package com.github.borgand.marginalia.ui.comment

import com.github.borgand.marginalia.MarginaliaBundle
import com.github.borgand.marginalia.ui.theme.MarginaliaColors
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

class RequeueCommentDialog(
    project: Project,
    fileName: String,
    line: Int,
    snippet: String,
) : DialogWrapper(project) {
    private val form = CommentForm(
        fileName,
        line,
        snippet,
        prompt = MarginaliaBundle.message("comment.requeue.reason"),
    )

    init {
        title = MarginaliaBundle.message("comment.requeue.title")
        isResizable = true
        setOKButtonText(MarginaliaBundle.message("panel.requeue"))
        init()
    }

    override fun createCenterPanel(): JComponent = form.component
    override fun getPreferredFocusedComponent(): JComponent = form.textArea
    override fun createActions(): Array<Action> = arrayOf(okAction, cancelAction)
    override fun doValidate() = if (form.body.isEmpty()) {
        ValidationInfo(MarginaliaBundle.message("comment.requeue.required"), form.textArea)
    } else null

    override fun createSouthAdditionalPanel(): JPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.emptyLeft(8)
        add(JBLabel(MarginaliaBundle.message("comment.requeue.queues")).apply {
            font = JBFont.small()
            foreground = MarginaliaColors.statusPending
        }, BorderLayout.WEST)
    }

    val reason: String get() = form.body
}
