package com.github.borgand.marginalia.ui

import com.github.borgand.marginalia.MarginaliaBundle
import com.github.borgand.marginalia.mcp.McpServerService
import com.github.borgand.marginalia.ui.render.RenderSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings > Tools > Marginalia. Gives a UI path to change the MCP server port and
 * restart it — so a failed auto-start (e.g. port clash) is never a dead end.
 */
class MarginaliaConfigurable : Configurable {

    private val server get() = service<McpServerService>()
    private val settings get() = service<MarginaliaSettings>()
    private val render get() = RenderSettings.getInstance()

    private val portField = JBTextField(10)
    private val statusLabel = JBLabel()
    private val captureSurfaceCombo = JComboBox(DefaultComboBoxModel(CaptureSurface.entries.toTypedArray()))
    private val foldLinkUrlsBox = JCheckBox(MarginaliaBundle.message("settings.fold.link.urls"))
    private val foldFrontmatterBox = JCheckBox(MarginaliaBundle.message("settings.fold.frontmatter"))
    private val dimMarkersBox = JCheckBox(MarginaliaBundle.message("settings.dim.markers"))
    private val bigTitlesBox = JCheckBox(MarginaliaBundle.message("settings.big.titles"))
    private val renderTablesBox = JCheckBox(MarginaliaBundle.message("settings.render.tables"))
    private val inlineImagesBox = JCheckBox(MarginaliaBundle.message("settings.inline.images"))

    override fun getDisplayName(): String = "Marginalia"

    override fun createComponent(): JComponent {
        portField.text = server.port().toString()
        captureSurfaceCombo.selectedItem = settings.captureSurface
        captureSurfaceCombo.renderer = captureSurfaceRenderer()
        refreshStatus()
        foldLinkUrlsBox.isSelected = render.foldLinkUrls
        foldFrontmatterBox.isSelected = render.foldFrontmatter
        dimMarkersBox.isSelected = render.dimMarkers
        bigTitlesBox.isSelected = render.bigTitles
        renderTablesBox.isSelected = render.renderTables
        inlineImagesBox.isSelected = render.inlineImages
        val restartButton = JButton(MarginaliaBundle.message("settings.restart.server")).apply {
            addActionListener {
                applyPort()
                server.restart()
                refreshStatus()
            }
        }
        val panel: JPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(MarginaliaBundle.message("settings.server.port"), portField)
            .addComponent(restartButton)
            .addComponent(statusLabel)
            .addLabeledComponent(MarginaliaBundle.message("settings.capture.surface"), captureSurfaceCombo)
            .addComponent(JBLabel(MarginaliaBundle.message("settings.markdown.rendering")))
            .addComponent(foldLinkUrlsBox)
            .addComponent(foldFrontmatterBox)
            .addComponent(dimMarkersBox)
            .addComponent(bigTitlesBox)
            .addComponent(renderTablesBox)
            .addComponent(inlineImagesBox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        return panel
    }

    private fun captureSurfaceRenderer() =
        com.intellij.ui.SimpleListCellRenderer.create<CaptureSurface>("") {
            when (it) {
                CaptureSurface.INLINE -> MarginaliaBundle.message("settings.capture.inline")
                CaptureSurface.DIALOG -> MarginaliaBundle.message("settings.capture.dialog")
            }
        }

    private fun refreshStatus() {
        statusLabel.text = when (server.state) {
            McpServerService.State.STOPPED -> MarginaliaBundle.message("settings.status.stopped")
            McpServerService.State.RUNNING -> MarginaliaBundle.message("settings.status.running", server.port())
            McpServerService.State.FAILED -> MarginaliaBundle.message("settings.status.failed", server.status)
        }
    }

    private fun parsedPort(): Int? = portField.text.trim().toIntOrNull()?.takeIf { it in 1..65535 }

    private fun applyPort() {
        val port = parsedPort()
        if (port == null) {
            Messages.showErrorDialog(MarginaliaBundle.message("settings.invalid.port"), "Marginalia")
            return
        }
        server.setPort(port)
    }

    override fun isModified(): Boolean =
        parsedPort() != server.port() ||
            captureSurfaceCombo.selectedItem != settings.captureSurface ||
            foldLinkUrlsBox.isSelected != render.foldLinkUrls ||
            foldFrontmatterBox.isSelected != render.foldFrontmatter ||
            dimMarkersBox.isSelected != render.dimMarkers ||
            bigTitlesBox.isSelected != render.bigTitles ||
            renderTablesBox.isSelected != render.renderTables ||
            inlineImagesBox.isSelected != render.inlineImages

    override fun apply() {
        applyPort()
        settings.captureSurface = captureSurfaceCombo.selectedItem as CaptureSurface
        render.foldLinkUrls = foldLinkUrlsBox.isSelected
        render.foldFrontmatter = foldFrontmatterBox.isSelected
        render.dimMarkers = dimMarkersBox.isSelected
        render.bigTitles = bigTitlesBox.isSelected
        render.renderTables = renderTablesBox.isSelected
        render.inlineImages = inlineImagesBox.isSelected
        refreshOpenMarkdownEditors()
    }

    private fun refreshOpenMarkdownEditors() {
        for (project in com.intellij.openapi.project.ProjectManager.getInstance().openProjects) {
            if (project.isDisposed) continue
            val fem = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
            for (file in fem.openFiles) {
                if (!file.name.endsWith(".md", ignoreCase = true)) continue
                val editor = (fem.getSelectedEditor(file) as? com.intellij.openapi.fileEditor.TextEditor)?.editor ?: continue
                project.service<com.github.borgand.marginalia.ui.render.MarkdownLineDecorator>().refresh(editor)
                project.service<com.github.borgand.marginalia.ui.render.fold.CustomFoldController>().refresh(editor)
            }
            // re-run annotator + folding builder (they read RenderSettings) on all open files
            com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

    override fun reset() {
        portField.text = server.port().toString()
        captureSurfaceCombo.selectedItem = settings.captureSurface
        refreshStatus()
        foldLinkUrlsBox.isSelected = render.foldLinkUrls
        foldFrontmatterBox.isSelected = render.foldFrontmatter
        dimMarkersBox.isSelected = render.dimMarkers
        bigTitlesBox.isSelected = render.bigTitles
        renderTablesBox.isSelected = render.renderTables
        inlineImagesBox.isSelected = render.inlineImages
    }
}
