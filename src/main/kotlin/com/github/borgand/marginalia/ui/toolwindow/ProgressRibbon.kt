package com.github.borgand.marginalia.ui.toolwindow

import com.github.borgand.marginalia.MarginaliaBundle
import com.github.borgand.marginalia.ui.theme.MarginaliaColors
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridLayout
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Review-session progress (redesign §02): a thin bar of all status counts plus a compact,
 * two-column legend. Counts always describe the full store, independent of list filtering.
 */
class ProgressRibbon : JPanel(BorderLayout(0, JBUI.scale(4))) {

    private val bar = Bar()
    private val archivedLabel = legendLabel(VisualStatus.ARCHIVED)
    private val addressedLabel = legendLabel(VisualStatus.ADDRESSED)
    private val deliveredLabel = legendLabel(VisualStatus.DELIVERED)
    private val queuedLabel = legendLabel(VisualStatus.QUEUED)
    private val draftLabel = legendLabel(VisualStatus.DRAFT)
    private val failedLabel = legendLabel(VisualStatus.FAILED)

    init {
        isOpaque = false
        border = JBUI.Borders.empty(6, 12, 8, 12)
        add(bar, BorderLayout.NORTH)
        val legend = JPanel(GridLayout(0, 2, JBUI.scale(12), JBUI.scale(2))).apply {
            isOpaque = false
        }
        legend.add(archivedLabel)
        legend.add(addressedLabel)
        legend.add(deliveredLabel)
        legend.add(queuedLabel)
        legend.add(draftLabel)
        legend.add(failedLabel)
        add(legend, BorderLayout.CENTER)
    }

    fun update(archived: Int, addressed: Int, delivered: Int, queued: Int, draft: Int, failed: Int) {
        bar.set(archived, addressed, delivered, queued, draft, failed)
        archivedLabel.text = MarginaliaBundle.message("progress.legend", archived, VisualStatus.ARCHIVED.label)
        addressedLabel.text = MarginaliaBundle.message("progress.legend", addressed, VisualStatus.ADDRESSED.label)
        deliveredLabel.text = MarginaliaBundle.message("progress.legend", delivered, VisualStatus.DELIVERED.label)
        queuedLabel.text = MarginaliaBundle.message("progress.legend", queued, VisualStatus.QUEUED.label)
        draftLabel.text = MarginaliaBundle.message("progress.legend", draft, VisualStatus.DRAFT.label)
        failedLabel.text = MarginaliaBundle.message("progress.legend", failed, VisualStatus.FAILED.label)
        revalidate(); repaint()
    }

    private fun legendLabel(status: VisualStatus) = JBLabel().apply {
        font = JBFont.small()
        foreground = status.color
    }

    /**
     * The bar. Counts ease from their current to their target value over ~180ms (redesign
     * §06 — animate only state transitions), so a segment visibly grows on archive/deliver.
     */
    private class Bar : JComponent() {
        private var curR = 0f
        private var curA = 0f
        private var curD = 0f
        private var curQ = 0f
        private var curDraft = 0f
        private var curFailed = 0f
        private var tgtR = 0
        private var tgtA = 0
        private var tgtD = 0
        private var tgtQ = 0
        private var tgtDraft = 0
        private var tgtFailed = 0
        private var initialized = false

        private val animator = javax.swing.Timer(16) { tick() }

        fun set(r: Int, a: Int, d: Int, q: Int, draft: Int, failed: Int) {
            tgtR = r; tgtA = a; tgtD = d; tgtQ = q; tgtDraft = draft; tgtFailed = failed
            if (!initialized) {
                curR = r.toFloat(); curA = a.toFloat(); curD = d.toFloat(); curQ = q.toFloat()
                curDraft = draft.toFloat(); curFailed = failed.toFloat()
                initialized = true
                repaint()
            } else if (!animator.isRunning) {
                animator.start()
            }
        }

        private fun tick() {
            curR += (tgtR - curR) * 0.25f
            curA += (tgtA - curA) * 0.25f
            curD += (tgtD - curD) * 0.25f
            curQ += (tgtQ - curQ) * 0.25f
            curDraft += (tgtDraft - curDraft) * 0.25f
            curFailed += (tgtFailed - curFailed) * 0.25f
            val settled = listOf(
                tgtR - curR,
                tgtA - curA,
                tgtD - curD,
                tgtQ - curQ,
                tgtDraft - curDraft,
                tgtFailed - curFailed,
            )
                .all { kotlin.math.abs(it) < 0.5f }
            if (settled) {
                curR = tgtR.toFloat(); curA = tgtA.toFloat(); curD = tgtD.toFloat(); curQ = tgtQ.toFloat()
                curDraft = tgtDraft.toFloat(); curFailed = tgtFailed.toFloat()
                animator.stop()
            }
            repaint()
        }

        override fun removeNotify() {
            animator.stop()
            super.removeNotify()
        }

        override fun getPreferredSize() = Dimension(JBUI.scale(120), JBUI.scale(5))
        override fun getMaximumSize() = Dimension(Int.MAX_VALUE, JBUI.scale(5))

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val h = height.toFloat()
                val w = width.toFloat()
                // track
                g2.color = MarginaliaColors.soft(MarginaliaColors.textMuted)
                g2.fill(RoundRectangle2D.Float(0f, 0f, w, h, h, h))

                val total = curR + curA + curD + curQ + curDraft + curFailed
                if (total <= 0f) return
                var x = 0f
                for ((value, color) in listOf(
                    curR to VisualStatus.ARCHIVED.color,
                    curA to VisualStatus.ADDRESSED.color,
                    curD to VisualStatus.DELIVERED.color,
                    curQ to VisualStatus.QUEUED.color,
                    curDraft to VisualStatus.DRAFT.color,
                    curFailed to VisualStatus.FAILED.color,
                )) {
                    if (value <= 0f) continue
                    val segW = w * value / total
                    g2.color = color
                    g2.fill(RoundRectangle2D.Float(x, 0f, segW, h, h, h))
                    x += segW
                }
            } finally {
                g2.dispose()
            }
        }
    }
}
