package com.github.borgand.marginalia.ui.toolwindow

import com.github.borgand.marginalia.core.CommentStatus
import com.github.borgand.marginalia.core.MarginaliaComment
import com.github.borgand.marginalia.core.ReviewRound
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

    fun testLatestReviewRoundIsExpandedAndEarlierRoundsAreSummarized() {
        val comment = MarginaliaComment().apply {
            status = CommentStatus.ADDRESSED
            filePath = "/project/Example.cs"
            body = "Original"
            anchoredText = "target"
            reviewRounds.add(ReviewRound().apply { reviewCycle = 1; reason = "first follow-up"; agentNote = "first reply" })
            reviewRounds.add(ReviewRound().apply { reviewCycle = 2; reason = "latest follow-up"; agentNote = "latest reply" })
        }
        val component = CommentListRenderer().getListCellRendererComponent(
            JBList<SidecarRow>().apply { setSize(480, 600) },
            SidecarRow.CommentRow(comment, line = 3),
            0,
            false,
            false,
        )
        val text = descendants(component).filterIsInstance<JBLabel>().joinToString(" ") { it.text }
        assertTrue(text.contains("latest follow-up"))
        assertTrue(text.contains("latest reply"))
        assertFalse(text.contains("first follow-up"))
        assertTrue(text.contains("1"))
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) {
            component.components.forEach { yieldAll(descendants(it)) }
        }
    }
}
