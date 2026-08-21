package com.github.borgand.marginalia.core

/**
 * Lifecycle of a review comment (PRD F2):
 * DRAFT (manual mode, not yet submitted) → QUEUED (visible to the agent)
 * → DISPATCHED (returned by get_pending_comments) → ADDRESSED (agent handled it)
 * → ARCHIVED (user closed it).
 */
enum class CommentStatus { DRAFT, QUEUED, DISPATCHED, ADDRESSED, ARCHIVED }

/**
 * Persistable comment bean (XmlSerializer-friendly: mutable fields + no-arg constructor).
 * Offsets and the anchored snippet are the persisted fallback for the transient
 * RangeMarker, which is re-created on file open via [CommentStore.ensureAnchored].
 */
class MarginaliaComment {
    var id: String = ""
    var filePath: String = ""
    var body: String = ""
    var status: CommentStatus = CommentStatus.QUEUED
    var anchoredText: String = ""
    var headingPath: MutableList<String> = mutableListOf()
    var startOffset: Int = 0
    var endOffset: Int = 0
    var createdAt: Long = 0
    var resolutionNote: String? = null
    var orphaned: Boolean = false
    var reviewRounds: MutableList<ReviewRound> = mutableListOf()

    fun currentReviewCycle(): Int = reviewRounds.lastOrNull()?.reviewCycle ?: 0

    fun currentInstruction(): String = reviewRounds.lastOrNull()?.reason ?: body
}

/** A follow-up review cycle on the original anchored comment. */
class ReviewRound {
    var reviewCycle: Int = 0
    var reason: String = ""
    var createdAt: Long = 0
    var agentNote: String? = null
    var addressedAt: Long = 0
}

fun canRequeue(comment: MarginaliaComment): Boolean =
    comment.orphaned || comment.status in setOf(CommentStatus.DISPATCHED, CommentStatus.ADDRESSED, CommentStatus.ARCHIVED)
