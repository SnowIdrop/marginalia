package com.github.borgand.marginalia.ui.toolwindow

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import java.awt.Component
import java.awt.Container

class ProgressRibbonTest : BasePlatformTestCase() {

    fun testLegendIncludesAllUnfilteredStatusesIncludingFailed() {
        val ribbon = ProgressRibbon()
        ribbon.update(archived = 6, addressed = 5, delivered = 4, queued = 3, draft = 2, failed = 1)

        val text = descendants(ribbon).filterIsInstance<JBLabel>().joinToString(" ") { it.text }
        assertTrue(text.contains("6 ${VisualStatus.ARCHIVED.label}"))
        assertTrue(text.contains("1 ${VisualStatus.FAILED.label}"))
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }
}
