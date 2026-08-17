package com.github.borgand.marginalia.ui.toolwindow

import com.github.borgand.marginalia.MarginaliaBundle
import com.github.borgand.marginalia.core.MarginaliaComment
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.Dimension
import javax.swing.JComponent

class ReviewHistoryDialog(project: Project, comment: MarginaliaComment) : DialogWrapper(project) {
    private val text = buildString {
        appendLine(MarginaliaBundle.message("comment.review.original"))
        appendLine(comment.body)
        comment.resolutionNote?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine(MarginaliaBundle.message("comment.agent.feedback"))
            appendLine(it)
        }
        comment.reviewRounds.forEach { round ->
            appendLine()
            appendLine(MarginaliaBundle.message("comment.review.cycle", round.reviewCycle))
            appendLine(round.reason)
            round.agentNote?.takeIf { it.isNotBlank() }?.let {
                appendLine()
                appendLine(MarginaliaBundle.message("comment.agent.feedback"))
                appendLine(it)
            }
        }
    }

    init {
        title = MarginaliaBundle.message("comment.review.history.title")
        setOKButtonText(MarginaliaBundle.message("comment.review.history.close"))
        init()
    }

    override fun createCenterPanel(): JComponent = JBScrollPane(JBTextArea(text).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }).apply { preferredSize = Dimension(560, 420) }

    override fun createActions() = arrayOf(okAction)
}
