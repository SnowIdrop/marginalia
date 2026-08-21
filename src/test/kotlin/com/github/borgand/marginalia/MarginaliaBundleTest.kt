package com.github.borgand.marginalia

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.ResourceBundle

class MarginaliaBundleTest {

    @Test
    fun simplifiedChineseBundleHasSameKeysAsDefaultBundle() {
        val english = ResourceBundle.getBundle("messages.MarginaliaBundle", Locale.ENGLISH)
        val chinese = ResourceBundle.getBundle("messages.MarginaliaBundle", Locale.SIMPLIFIED_CHINESE)

        assertEquals(english.keySet(), chinese.keySet())
        assertEquals("添加评论", chinese.getString("comment.add.title"))
        assertEquals("已归档", chinese.getString("status.archived"))
    }
}
