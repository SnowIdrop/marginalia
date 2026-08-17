package com.github.borgand.marginalia.ui.render

import com.github.borgand.marginalia.MarginaliaBundle
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class MarginaliaColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName() = "Marginalia"
    override fun getIcon(): Icon? = null
    override fun getHighlighter(): SyntaxHighlighter = PlainSyntaxHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor(MarginaliaBundle.message("color.heading.1"), MarginaliaTextAttributes.H1),
        AttributesDescriptor(MarginaliaBundle.message("color.heading.2"), MarginaliaTextAttributes.H2),
        AttributesDescriptor(MarginaliaBundle.message("color.heading.3"), MarginaliaTextAttributes.H3),
        AttributesDescriptor(MarginaliaBundle.message("color.heading.4.6"), MarginaliaTextAttributes.H4_6),
        AttributesDescriptor(MarginaliaBundle.message("color.heading.1.emphasis"), MarginaliaTextAttributes.H1_STYLE),
        AttributesDescriptor(MarginaliaBundle.message("color.heading.2.emphasis"), MarginaliaTextAttributes.H2_STYLE),
        AttributesDescriptor(MarginaliaBundle.message("color.blockquote"), MarginaliaTextAttributes.BLOCKQUOTE),
        AttributesDescriptor(MarginaliaBundle.message("color.list.marker"), MarginaliaTextAttributes.LIST_MARKER),
        AttributesDescriptor(MarginaliaBundle.message("color.dimmed.marker"), MarginaliaTextAttributes.DIMMED_MARKER),
        AttributesDescriptor(MarginaliaBundle.message("color.bold"), MarginaliaTextAttributes.BOLD),
        AttributesDescriptor(MarginaliaBundle.message("color.italic"), MarginaliaTextAttributes.ITALIC),
        AttributesDescriptor(MarginaliaBundle.message("color.bold.italic"), MarginaliaTextAttributes.BOLD_ITALIC),
        AttributesDescriptor(MarginaliaBundle.message("color.strikethrough"), MarginaliaTextAttributes.STRIKETHROUGH),
    )

    override fun getDemoText(): String =
        "# Heading 1\n## Heading 2\n### Heading 3\n#### Heading 4\n\n" +
        "> a blockquote\n\n- list item\n\n**bold** _italic_ ***both*** ~~struck out~~\n"
}
