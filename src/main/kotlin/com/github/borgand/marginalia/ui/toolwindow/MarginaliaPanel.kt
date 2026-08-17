package com.github.borgand.marginalia.ui.toolwindow

import com.github.borgand.marginalia.MarginaliaBundle
import com.github.borgand.marginalia.core.ActivityLog
import com.github.borgand.marginalia.core.CommentQueue
import com.github.borgand.marginalia.core.CommentStatus
import com.github.borgand.marginalia.core.CommentStore
import com.github.borgand.marginalia.core.DocRegistry
import com.github.borgand.marginalia.core.MarginaliaComment
import com.github.borgand.marginalia.core.canRequeue
import com.github.borgand.marginalia.mcp.McpServerService
import com.github.borgand.marginalia.ui.ConnectivityReport
import com.github.borgand.marginalia.ui.CaptureSurface
import com.github.borgand.marginalia.ui.MarginaliaSettings
import com.github.borgand.marginalia.ui.comment.InlineCommentPopup
import com.github.borgand.marginalia.ui.comment.RequeueCommentDialog
import com.github.borgand.marginalia.ui.theme.MarginaliaColors
import com.github.borgand.marginalia.ui.theme.MarginaliaIcons
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.BadgeIconSupplier
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.ListSelectionModel
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

/**
 * The sidecar body. A [SimpleToolWindowPanel] with an action toolbar + connection chip on
 * top, a progress ribbon, the grouped comment list, and the footer status panel.
 */
class MarginaliaPanel(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : SimpleToolWindowPanel(true, true), Disposable {

    private val store = project.service<CommentStore>()
    private val queue = project.service<CommentQueue>()

    private val listModel = CommentListModel()
    private val list = JBList(listModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = CommentListRenderer()
        background = MarginaliaColors.surfaceToolWindow
        // Cards already wrap their own content to the list width; the platform's
        // expandable-items hover popup would just repaint the card overflowing past
        // the tool-window edge, adding no detail. Disable it.
        setExpandableItemsEnabled(false)
    }
    private val ribbon = ProgressRibbon()
    private val connectionChip = ConnectionChip()
    private val footer = FooterStatusPanel(this)
    private val baseBadge = BadgeIconSupplier(MarginaliaIcons.ToolWindow)
    private var clearButton: JButton? = null
    private var submitButton: JButton? = null
    private var requeueItem: JMenuItem? = null
    private var historyItem: JMenuItem? = null

    init {
        toolbar = buildToolbar()
        setContent(buildContent())

        list.componentPopupMenu = buildPopupMenu()
        installListInteractions()

        store.addChangeListener(this) { onEdt { refresh() } }
        refresh()
    }

    // ── construction ────────────────────────────────────────────────────────────
    private fun buildToolbar(): JComponent {
        val group = DefaultActionGroup().apply {
            add(AutoQueueToggle())
            val more = DefaultActionGroup(MarginaliaBundle.message("panel.more"), true).apply {
                templatePresentation.icon = AllIcons.General.GearPlain
                add(RestartServerAction())
                add(TestConnectivityAction())
            }
            add(more)
        }
        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar("MarginaliaToolbar", group, true)
        actionToolbar.targetComponent = this

        val submit = JButton(MarginaliaBundle.message("panel.submit.review")).apply {
            putClientProperty("JButton.buttonType", "default")
            toolTipText = MarginaliaBundle.message("panel.submit.review.tooltip")
            addActionListener {
                val n = queue.submitReview()
                service<ActivityLog>().log("submit review: $n comment(s) queued")
            }
        }
        submitButton = submit

        val clear = JButton(MarginaliaBundle.message("panel.clear"), AllIcons.Actions.GC).apply {
            toolTipText = MarginaliaBundle.message("panel.clear.tooltip")
            addActionListener { showClearMenu(this) }
        }
        clearButton = clear

        val right = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), JBUI.scale(2))).apply {
            isOpaque = false
            add(clear)
            add(submit)
            add(connectionChip)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            add(actionToolbar.component, BorderLayout.WEST)
            add(right, BorderLayout.EAST)
        }
    }

    private fun buildContent(): JComponent = JPanel(BorderLayout()).apply {
        background = MarginaliaColors.surfaceToolWindow
        add(ribbon, BorderLayout.NORTH)
        add(JBScrollPane(list).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    private fun installListInteractions() {
        // single click on a file header toggles its fold
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val index = list.locationToIndex(e.point).takeIf { it >= 0 } ?: return
                if (!list.getCellBounds(index, index).contains(e.point)) return
                val row = listModel.getElementAt(index)
                if (row is SidecarRow.FileHeaderRow) listModel.toggleCollapsed(row.path)
            }
        })
        // double click / Enter on a comment jumps to its anchored line
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                jumpToSelected(); return true
            }
        }.installOn(list)
    }

    private fun buildPopupMenu(): JPopupMenu = JPopupMenu().apply {
        add(JMenuItem(MarginaliaBundle.message("panel.jump.to.line")).apply { addActionListener { jumpToSelected() } })
        add(JMenuItem(MarginaliaBundle.message("panel.resolve")).apply {
            addActionListener { selectedComment()?.let { store.setStatus(it.id, CommentStatus.RESOLVED) } }
        })
        add(JMenuItem(MarginaliaBundle.message("panel.requeue")).apply {
            addActionListener { requeueSelected() }
            requeueItem = this
        })
        add(JMenuItem(MarginaliaBundle.message("panel.review.history")).apply {
            addActionListener { selectedComment()?.let { ReviewHistoryDialog(project, it).show() } }
            historyItem = this
        })
        add(JMenuItem(MarginaliaBundle.message("panel.delete")).apply {
            addActionListener { selectedComment()?.let { store.remove(it.id) } }
        })
        add(JMenuItem(MarginaliaBundle.message("panel.stop.coediting")).apply {
            addActionListener {
                selectedComment()?.let { project.service<DocRegistry>().unregister(it.filePath) }
            }
        })
        addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                val comment = selectedComment()
                requeueItem?.isEnabled = comment?.let(::canRequeue) == true
                historyItem?.isEnabled = comment?.reviewRounds?.isNotEmpty() == true
            }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) = Unit
            override fun popupMenuCanceled(e: PopupMenuEvent) = Unit
        })
    }

    /** Pops the Clear dropdown; each item shows a live count and disables when empty. */
    private fun showClearMenu(anchor: JComponent) {
        val comments = store.comments()
        val resolved = comments.filter { visualStatus(it) == VisualStatus.RESOLVED }
        val failed = comments.filter { visualStatus(it) == VisualStatus.FAILED }

        val menu = JPopupMenu()
        menu.add(JMenuItem(MarginaliaBundle.message("panel.clear.resolved", resolved.size)).apply {
            isEnabled = resolved.isNotEmpty()
            addActionListener { clear("resolved") { visualStatus(it) == VisualStatus.RESOLVED } }
        })
        menu.add(JMenuItem(MarginaliaBundle.message("panel.clear.failed", failed.size)).apply {
            isEnabled = failed.isNotEmpty()
            addActionListener { clear("failed") { visualStatus(it) == VisualStatus.FAILED } }
        })
        menu.add(JMenuItem(MarginaliaBundle.message("panel.clear.all", comments.size)).apply {
            isEnabled = comments.isNotEmpty()
            addActionListener {
                val confirmed = Messages.showYesNoDialog(
                    project,
                    MarginaliaBundle.message("panel.clear.all.message", comments.size),
                    MarginaliaBundle.message("panel.clear.all.title"),
                    Messages.getWarningIcon(),
                ) == Messages.YES
                if (confirmed) clear("all") { true }
            }
        })
        menu.show(anchor, 0, anchor.height)
    }

    private fun clear(label: String, predicate: (MarginaliaComment) -> Boolean) {
        val n = store.removeWhere(predicate)
        service<ActivityLog>().log("clear $label: $n comment(s) removed")
    }

    // ── behavior ──────────────────────────────────────────────────────────────
    private fun selectedComment(): MarginaliaComment? =
        (list.selectedValue as? SidecarRow.CommentRow)?.comment

    private fun requeueSelected() {
        val comment = selectedComment()?.takeIf(::canRequeue) ?: return
        val file = LocalFileSystem.getInstance().findFileByPath(comment.filePath)
        if (file == null) {
            Messages.showErrorDialog(project, MarginaliaBundle.message("comment.requeue.file.missing"), MarginaliaBundle.message("comment.requeue.title"))
            return
        }
        val line = lineOf(comment)
        val descriptor = if (line != null) {
            OpenFileDescriptor(project, file, line, 0)
        } else {
            OpenFileDescriptor(project, file, comment.startOffset.coerceAtLeast(0))
        }
        val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
        if (editor == null) {
            Messages.showErrorDialog(project, MarginaliaBundle.message("comment.requeue.editor.missing"), MarginaliaBundle.message("comment.requeue.title"))
            return
        }
        val document = editor.document
        val marker = store.markerFor(comment.id)
        val needsReanchor = marker == null || !marker.isValid || comment.orphaned
        val (start, end) = ReadAction.compute<Pair<Int, Int>, RuntimeException> {
            if (!needsReanchor) {
                marker!!.startOffset to marker.endOffset
            } else {
                val offset = comment.startOffset.coerceIn(0, document.textLength)
                val line = document.getLineNumber(offset)
                document.getLineStartOffset(line) to document.getLineEndOffset(line)
            }
        }
        val anchorLine = document.getLineNumber(start)
        val snippet = document.getText(com.intellij.openapi.util.TextRange(start, end))
        val submit: (String) -> Unit = { reason ->
            val ok = store.requeue(
                comment.id,
                reason,
                if (needsReanchor) document else null,
                start,
                end,
            )
            if (!ok) Messages.showErrorDialog(project, MarginaliaBundle.message("comment.requeue.failed"), MarginaliaBundle.message("comment.requeue.title"))
        }
        when (service<MarginaliaSettings>().captureSurface) {
            CaptureSurface.INLINE -> InlineCommentPopup(
                editor,
                file.name,
                anchorLine,
                snippet,
                headerText = MarginaliaBundle.message("comment.requeue.title"),
                statusText = MarginaliaBundle.message("status.queued"),
                submitText = MarginaliaBundle.message("panel.requeue"),
                submitTooltip = MarginaliaBundle.message("comment.requeue.submit.tooltip", InlineCommentPopup.shortcutLabel()),
                prompt = MarginaliaBundle.message("comment.requeue.reason"),
                onSubmit = submit,
            ).show()
            CaptureSurface.DIALOG -> {
                val dialog = RequeueCommentDialog(project, file.name, anchorLine, snippet)
                if (dialog.showAndGet()) submit(dialog.reason)
            }
        }
    }

    private fun jumpToSelected() {
        val comment = selectedComment() ?: return
        val file = LocalFileSystem.getInstance().findFileByPath(comment.filePath) ?: return
        val line = lineOf(comment)
        val descriptor = if (line != null) {
            OpenFileDescriptor(project, file, line, 0)
        } else {
            OpenFileDescriptor(project, file, comment.startOffset)
        }
        descriptor.navigate(true)
    }

    /** 0-based anchor line from a live marker, or null when no valid marker exists. */
    private fun lineOf(comment: MarginaliaComment): Int? {
        val marker = store.markerFor(comment.id) ?: return null
        return ReadAction.compute<Int?, RuntimeException> {
            if (marker.isValid) marker.document.getLineNumber(marker.startOffset) else null
        }
    }

    private fun refresh() {
        store.syncOffsetsFromMarkers()
        val comments = store.comments()
        listModel.setComments(comments) { lineOf(it) }

        val counts = comments.groupingBy { visualStatus(it) }.eachCount()
        ribbon.update(
            resolved = counts[VisualStatus.RESOLVED] ?: 0,
            addressed = counts[VisualStatus.ADDRESSED] ?: 0,
            delivered = counts[VisualStatus.DELIVERED] ?: 0,
            queued = counts[VisualStatus.QUEUED] ?: 0,
            draft = counts[VisualStatus.DRAFT] ?: 0,
        )

        val view = connectionView(
            service<McpServerService>().state,
            service<McpServerService>().lastClientConnectedAt != null,
            service<McpServerService>().lastToolCallAt,
        )
        connectionChip.update(view)
        footer.refresh()

        clearButton?.isEnabled = comments.isNotEmpty()
        submitButton?.isEnabled = comments.any { it.status == CommentStatus.DRAFT }

        val pending = (counts[VisualStatus.DRAFT] ?: 0) + (counts[VisualStatus.QUEUED] ?: 0)
        toolWindow.setIcon(baseBadge.getWarningIcon(pending > 0))
    }

    private fun onEdt(action: () -> Unit) {
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) action() else app.invokeLater(action)
    }

    override fun dispose() {}

    // ── toolbar actions ─────────────────────────────────────────────────────────
    private inner class AutoQueueToggle : ToggleAction(
        MarginaliaBundle.message("panel.auto.queue"),
        MarginaliaBundle.message("panel.auto.queue.tooltip"),
        AllIcons.Actions.Lightning,
    ) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isSelected(e: AnActionEvent) = queue.autoDispatch
        override fun setSelected(e: AnActionEvent, state: Boolean) { queue.autoDispatch = state }
    }

    private inner class RestartServerAction : AnAction(
        MarginaliaBundle.message("panel.restart.server"),
        MarginaliaBundle.message("panel.restart.server.tooltip"),
        AllIcons.Actions.Restart,
    ) {
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun actionPerformed(e: AnActionEvent) {
            ApplicationManager.getApplication().executeOnPooledThread {
                service<McpServerService>().restart()
                onEdt { refresh() }
            }
        }
    }

    private inner class TestConnectivityAction : AnAction(
        MarginaliaBundle.message("panel.test.connectivity"),
        MarginaliaBundle.message("panel.test.connectivity.tooltip"),
        AllIcons.Actions.Lightning,
    ) {
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun actionPerformed(e: AnActionEvent) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val report = ConnectivityReport.build(project)
                onEdt {
                    service<ActivityLog>().log("connectivity test:\n$report")
                    Messages.showMessageDialog(
                        project, report, MarginaliaBundle.message("panel.connectivity.title"), Messages.getInformationIcon(),
                    )
                }
            }
        }
    }

}
