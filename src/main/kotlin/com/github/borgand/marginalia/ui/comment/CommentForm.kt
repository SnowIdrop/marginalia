package com.github.borgand.marginalia.ui.comment

import com.github.borgand.marginalia.MarginaliaBundle
import com.github.borgand.marginalia.ui.theme.MarginaliaColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * The shared comment-capture body (redesign §03): a context strip naming the file + line,
 * the anchored line shown in the editor font inside a `status.pending`-ruled quote box, and
 * the text input. Reused by both [AddCommentDialog] and [InlineCommentPopup].
 */
class CommentForm(
    private val fileName: String,
    private val line: Int,
    private val snippet: String,
    recentComments: List<String> = emptyList(),
    private val prompt: String? = null,
) {

    val textArea = JBTextArea(4, 42).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    /** Trimmed body text. */
    val body: String get() = textArea.text.trim()

    internal val recentCommentsCombo: JComboBox<String>? = recentComments.takeIf { it.isNotEmpty() }?.let { comments ->
        JComboBox(DefaultComboBoxModel(comments.toTypedArray())).apply {
            selectedIndex = -1
            renderer = SimpleListCellRenderer.create<String>(MarginaliaBundle.message("comment.previous.placeholder")) {
                historyPreview(it)
            }
            toolTipText = MarginaliaBundle.message("comment.previous.tooltip")
            alignmentX = JComponent.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            addActionListener {
                val selected = selectedItem as? String ?: return@addActionListener
                textArea.text = selected
                textArea.caretPosition = selected.length
                textArea.requestFocusInWindow()
            }
        }
    }

    val component: JComponent by lazy { build() }

    // Width is driven by the text input (~42 cols), never by how much text the user
    // happened to select: clamp so a long selection can't stretch the dialog/popup
    // off-screen (the snippet itself is truncated below, with the full text in a tooltip).
    private fun build(): JComponent = object : JPanel() {
        override fun getPreferredSize(): Dimension {
            val pref = super.getPreferredSize()
            pref.width = pref.width.coerceIn(JBUI.scale(MIN_WIDTH), JBUI.scale(MAX_WIDTH))
            return pref
        }
    }.apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(10, 12)

        add(contextStrip())
        add(javax.swing.Box.createVerticalStrut(JBUI.scale(6)))
        add(quoteBox())
        add(javax.swing.Box.createVerticalStrut(JBUI.scale(8)))
        recentCommentsCombo?.let { combo ->
            add(
                JBLabel(MarginaliaBundle.message("comment.previous")).apply {
                    font = JBFont.small().asBold()
                    foreground = MarginaliaColors.textMuted
                    alignmentX = JComponent.LEFT_ALIGNMENT
                },
            )
            add(javax.swing.Box.createVerticalStrut(JBUI.scale(4)))
            add(combo)
            add(javax.swing.Box.createVerticalStrut(JBUI.scale(8)))
        }
        prompt?.let {
            add(
                JBLabel(it).apply {
                    font = JBFont.small().asBold()
                    foreground = MarginaliaColors.textMuted
                    alignmentX = JComponent.LEFT_ALIGNMENT
                },
            )
            add(javax.swing.Box.createVerticalStrut(JBUI.scale(4)))
        }
        add(JBScrollPane(textArea).apply { alignmentX = JComponent.LEFT_ALIGNMENT })
    }

    private fun contextStrip(): JComponent {
        val icon = FileTypeManager.getInstance().getFileTypeByFileName(fileName).icon
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            alignmentX = JComponent.LEFT_ALIGNMENT
            add(
                JBLabel(MarginaliaBundle.message("comment.context.line", fileName, line + 1), icon, SwingConstants.LEFT).apply {
                    foreground = MarginaliaColors.textMuted
                    iconTextGap = JBUI.scale(5)
                },
                BorderLayout.WEST,
            )
        }
    }

    private fun quoteBox(): JComponent {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val editorFont = Font(scheme.editorFontName, Font.PLAIN, JBUI.scale(12))
        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = MarginaliaColors.surfaceEditor
            alignmentX = JComponent.LEFT_ALIGNMENT
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(MarginaliaColors.statusPending, 0, 2, 0, 0),
                JBUI.Borders.empty(6, 8),
            )
            val oneLine = snippet.replace('\n', ' ').trim()
            val shown = oneLine.ifEmpty { MarginaliaBundle.message("comment.empty.selection") }
            add(
                JBLabel(truncate(shown, SNIPPET_MAX_CHARS)).apply {
                    font = editorFont
                    foreground = MarginaliaColors.textPrimary
                    if (oneLine.length > SNIPPET_MAX_CHARS) toolTipText = oneLine
                },
                BorderLayout.CENTER,
            )
        }
    }

    private fun truncate(s: String, max: Int): String =
        if (s.length <= max) s else s.take(max - 1) + "…"

    private fun historyPreview(body: String): String =
        truncate(body.replace(Regex("\\s+"), " ").trim(), HISTORY_PREVIEW_MAX_CHARS)

    companion object {
        private const val MIN_WIDTH = 360
        private const val MAX_WIDTH = 560
        private const val SNIPPET_MAX_CHARS = 72
        private const val HISTORY_PREVIEW_MAX_CHARS = 72
    }
}
