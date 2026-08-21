package com.github.borgand.marginalia.ui.toolwindow

import com.intellij.ide.util.PropertiesComponent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CommentFilterStateTest : BasePlatformTestCase() {

    private val properties get() = PropertiesComponent.getInstance(project)

    override fun tearDown() {
        try {
            properties.unsetValue(CommentFilterState.KEY)
        } finally {
            super.tearDown()
        }
    }

    fun testMissingAndInvalidValuesUseActiveDefault() {
        assertEquals(CommentFilterState.DEFAULT_VISIBLE_STATUSES, CommentFilterState.decode(null))
        assertEquals(CommentFilterState.DEFAULT_VISIBLE_STATUSES, CommentFilterState.decode("UNKNOWN"))
        assertFalse(CommentFilterState.decode(null).contains(VisualStatus.ARCHIVED))
    }

    fun testProjectStateRoundTripsSelectedStatuses() {
        val state = CommentFilterState(properties)
        state.visibleStatuses = setOf(VisualStatus.DRAFT, VisualStatus.FAILED, VisualStatus.ARCHIVED)

        assertEquals(
            setOf(VisualStatus.DRAFT, VisualStatus.FAILED, VisualStatus.ARCHIVED),
            CommentFilterState(properties).visibleStatuses,
        )
    }

    fun testEmptySelectionIsPersistedExplicitly() {
        val state = CommentFilterState(properties)
        state.visibleStatuses = emptySet()

        assertTrue(CommentFilterState(properties).visibleStatuses.isEmpty())
    }

    fun testUnknownEntriesAreIgnoredWhenKnownEntriesRemain() {
        assertEquals(setOf(VisualStatus.QUEUED), CommentFilterState.decode("QUEUED,FUTURE_STATUS"))
    }
}
