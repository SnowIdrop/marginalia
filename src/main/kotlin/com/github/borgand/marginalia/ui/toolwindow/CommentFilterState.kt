package com.github.borgand.marginalia.ui.toolwindow

import com.intellij.ide.util.PropertiesComponent

/** Project-scoped persistence for the statuses visible in the sidecar list. */
class CommentFilterState(private val properties: PropertiesComponent) {

    var visibleStatuses: Set<VisualStatus>
        get() = decode(properties.getValue(KEY))
        set(value) {
            properties.setValue(KEY, encode(value))
        }

    companion object {
        const val KEY = "marginalia.comment.filter.archived-statuses"
        private const val EMPTY = "-"

        val DEFAULT_VISIBLE_STATUSES: Set<VisualStatus> =
            VisualStatus.entries.toSet() - VisualStatus.ARCHIVED

        fun encode(statuses: Set<VisualStatus>): String =
            if (statuses.isEmpty()) EMPTY else statuses.sortedBy { it.ordinal }.joinToString(",") { it.name }

        fun decode(value: String?): Set<VisualStatus> {
            if (value == null) return DEFAULT_VISIBLE_STATUSES
            if (value == EMPTY) return emptySet()
            val statuses = value.split(',')
                .mapNotNull { name -> VisualStatus.entries.find { it.name == name.trim() } }
                .toSet()
            return statuses.ifEmpty { DEFAULT_VISIBLE_STATUSES }
        }
    }
}
