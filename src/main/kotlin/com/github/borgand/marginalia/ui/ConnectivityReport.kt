package com.github.borgand.marginalia.ui

import com.github.borgand.marginalia.MarginaliaBundle
import com.github.borgand.marginalia.core.DocRegistry
import com.github.borgand.marginalia.mcp.McpServerService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Assembles the "Test agent connectivity" report. Given the MCP model is pull-based
 * (the server cannot push to the agent), connectivity = "has an MCP client reached us,
 * and how recently". This confirms `claude mcp add` + a running agent are configured.
 */
object ConnectivityReport {

    fun build(project: Project): String {
        val server = service<McpServerService>()
        val port = server.port()
        val listening = server.probeListening()
        val coEdited = project.service<DocRegistry>().paths()

        val lines = mutableListOf<String>()

        lines += MarginaliaBundle.message("connectivity.server", server.status)
        lines += if (listening) {
            MarginaliaBundle.message("connectivity.port.accepting", port)
        } else {
            MarginaliaBundle.message("connectivity.port.not.accepting", port)
        }

        val connectedAt = server.lastClientConnectedAt
        lines += if (connectedAt != null) {
            MarginaliaBundle.message("connectivity.agent.connected", ago(connectedAt))
        } else {
            MarginaliaBundle.message("connectivity.agent.never")
        }

        val toolAt = server.lastToolCallAt
        lines += if (toolAt != null) {
            MarginaliaBundle.message("connectivity.last.tool", server.lastToolName ?: "", ago(toolAt))
        } else {
            MarginaliaBundle.message("connectivity.last.tool.never")
        }

        lines += MarginaliaBundle.message("connectivity.files", coEdited.size)

        lines += ""
        if (!listening) {
            lines += MarginaliaBundle.message("connectivity.server.down")
        } else if (connectedAt == null) {
            lines += MarginaliaBundle.message("connectivity.no.agent")
            lines += "    claude mcp add --transport http marginalia http://localhost:$port/mcp"
            lines += MarginaliaBundle.message("connectivity.confirm.mcp")
        } else {
            lines += MarginaliaBundle.message("connectivity.agent.reached")
            lines += MarginaliaBundle.message("connectivity.comments.delivered")
            lines += MarginaliaBundle.message("connectivity.check.comments")
        }

        return lines.joinToString("\n")
    }

    private fun ago(timestamp: Long): String {
        val seconds = (System.currentTimeMillis() - timestamp) / 1000
        return when {
            seconds < 5 -> MarginaliaBundle.message("time.just.now")
            seconds < 60 -> MarginaliaBundle.message("time.seconds.ago", seconds)
            seconds < 3600 -> MarginaliaBundle.message("time.minutes.ago", seconds / 60)
            else -> MarginaliaBundle.message("time.hours.ago", seconds / 3600)
        }
    }
}
