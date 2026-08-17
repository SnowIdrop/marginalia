package com.github.borgand.marginalia.ui.toolwindow

import com.github.borgand.marginalia.core.CommentStatus
import com.github.borgand.marginalia.core.MarginaliaComment
import com.github.borgand.marginalia.core.canRequeue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualStatusTest {

    private fun comment(status: CommentStatus, orphaned: Boolean = false) =
        MarginaliaComment().apply {
            this.status = status
            this.orphaned = orphaned
        }

    @Test
    fun draftAndQueuedHaveDistinctVisualStates() {
        assertEquals(VisualStatus.DRAFT, visualStatus(comment(CommentStatus.DRAFT)))
        assertEquals(VisualStatus.QUEUED, visualStatus(comment(CommentStatus.QUEUED)))
    }

    @Test
    fun dispatchedIsDeliveredAndAddressedWaitsForHumanReview() {
        assertEquals(VisualStatus.DELIVERED, visualStatus(comment(CommentStatus.DISPATCHED)))
        assertEquals(VisualStatus.ADDRESSED, visualStatus(comment(CommentStatus.ADDRESSED)))
    }

    @Test
    fun resolvedIsResolved() {
        assertEquals(VisualStatus.RESOLVED, visualStatus(comment(CommentStatus.RESOLVED)))
    }

    @Test
    fun orphanedUndeliveredCommentIsFailed() {
        // A lost anchor only signals failure while the comment was never delivered.
        assertEquals(VisualStatus.FAILED, visualStatus(comment(CommentStatus.DRAFT, orphaned = true)))
        assertEquals(VisualStatus.FAILED, visualStatus(comment(CommentStatus.QUEUED, orphaned = true)))
    }

    @Test
    fun deliveredOrResolvedCommentKeepsStatusWhenOrphaned() {
        // After the agent rewrites the anchored text, the original snippet no longer matches
        // on restart and the comment orphans — but it was delivered/closed, not failed.
        assertEquals(VisualStatus.DELIVERED, visualStatus(comment(CommentStatus.DISPATCHED, orphaned = true)))
        assertEquals(VisualStatus.ADDRESSED, visualStatus(comment(CommentStatus.ADDRESSED, orphaned = true)))
        assertEquals(VisualStatus.RESOLVED, visualStatus(comment(CommentStatus.RESOLVED, orphaned = true)))
    }

    @Test
    fun requeueIsLimitedToCompletedDeliveredOrFailedComments() {
        assertFalse(canRequeue(comment(CommentStatus.DRAFT)))
        assertFalse(canRequeue(comment(CommentStatus.QUEUED)))
        assertTrue(canRequeue(comment(CommentStatus.DISPATCHED)))
        assertTrue(canRequeue(comment(CommentStatus.ADDRESSED)))
        assertTrue(canRequeue(comment(CommentStatus.RESOLVED)))
        assertTrue(canRequeue(comment(CommentStatus.QUEUED, orphaned = true)))
    }
}
