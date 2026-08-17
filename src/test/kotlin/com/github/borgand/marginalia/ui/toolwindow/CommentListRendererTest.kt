package com.github.borgand.marginalia.ui.toolwindow

import com.github.borgand.marginalia.core.CommentStatus
import com.github.borgand.marginalia.core.MarginaliaComment
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import java.awt.Component
import java.awt.Container

class CommentListRendererTest : BasePlatformTestCase() {

    fun testAddressedCommentShowsAgentFeedback() {
        val comment = MarginaliaComment().apply {
            status = CommentStatus.ADDRESSED
            filePath = "/project/Example.cs"
            body = "Use a guard clause"
            anchoredText = "if (value.HasValue)"
            resolutionNote = "Applied the guard clause and preserved the fallback behavior."
        }
        val list = JBList<SidecarRow>().apply { setSize(480, 600) }

        val component = CommentListRenderer().getListCellRendererComponent(
            list,
            SidecarRow.CommentRow(comment, line = 12),
            0,
            false,
            false,
        )

        val labels = descendants(component).filterIsInstance<JBLabel>()
        assertTrue(labels.any { it.text.contains(comment.resolutionNote!!) })
        val pills = descendants(component).filterIsInstance<RoundedPill>()
        assertTrue(pills.any { it.text == VisualStatus.ADDRESSED.label })
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) {
            component.components.forEach { yieldAll(descendants(it)) }
        }
    }
}
