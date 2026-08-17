package com.github.borgand.marginalia.ui.toolwindow

import com.github.borgand.marginalia.MarginaliaBundle
import com.github.borgand.marginalia.core.CommentStatus
import com.github.borgand.marginalia.core.MarginaliaComment
import com.github.borgand.marginalia.ui.theme.MarginaliaColors
import java.awt.Color

/**
 * The five statuses surfaced from the [CommentStatus] model plus the orphaned flag. This
 * is the single mapping from comment state to appearance — the list, the gutter, the ribbon
 * and the badge all read it.
 */
enum class VisualStatus {
    /** Written, not yet delivered to the agent. */
    QUEUED,

    /** Agent pulled it via get_pending_comments. */
    DELIVERED,

    /** Agent reported the comment as addressed; human confirmation is still pending. */
    ADDRESSED,

    /** Human confirmed the comment is closed. */
    RESOLVED,

    /** Delivery/processing error — in Marginalia, the comment's anchor was lost. */
    FAILED,
    ;

    val label: String
        get() = MarginaliaBundle.message(
            when (this) {
                QUEUED -> "status.queued"
                DELIVERED -> "status.delivered"
                ADDRESSED -> "status.addressed"
                RESOLVED -> "status.resolved"
                FAILED -> "status.failed"
            },
        )

    val color: Color
        get() = when (this) {
            QUEUED -> MarginaliaColors.statusPending // amber — pending, not yet sent
            DELIVERED -> MarginaliaColors.statusDelivered // green — in flight / positive progress
            ADDRESSED -> MarginaliaColors.accent // agent response is ready for human review
            RESOLVED -> MarginaliaColors.statusInfo // blue — closed
            FAILED -> MarginaliaColors.statusConflict
        }

    /** Soft pill background = [color] @ 13%. */
    val softColor: Color get() = MarginaliaColors.soft(color)
}

/**
 * Maps a comment to its visual status. Dispatch state wins over the orphaned flag: once a
 * comment has reached the agent (DISPATCHED/ADDRESSED) or been closed (RESOLVED), a lost
 * anchor is the *expected* outcome of the text being rewritten, not a failure — re-anchoring
 * the original snippet legitimately fails after restart (markers are transient, see
 * [com.github.borgand.marginalia.core.CommentStore.ensureAnchored]). Orphaned only reads as
 * [VisualStatus.FAILED] for a comment that was never delivered: there a missing anchor means
 * it can no longer be acted on.
 */
fun visualStatus(comment: MarginaliaComment): VisualStatus = when {
    comment.status == CommentStatus.RESOLVED -> VisualStatus.RESOLVED
    comment.status == CommentStatus.ADDRESSED -> VisualStatus.ADDRESSED
    comment.status == CommentStatus.DISPATCHED -> VisualStatus.DELIVERED
    comment.orphaned -> VisualStatus.FAILED // DRAFT/QUEUED with a lost anchor
    else -> VisualStatus.QUEUED // DRAFT, QUEUED
}
