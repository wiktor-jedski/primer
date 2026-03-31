package com.example.primer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalParserTest {

    @Test
    fun `single marker with multiple items produces correct section`() {
        val text = "## Goals\nitem1\nitem2\n"
        val result = parseJournal(text, listOf("## Goals"))
        assertEquals(1, result.size)
        assertEquals("## Goals", result[0].title)
        assertEquals(listOf("item1", "item2"), result[0].items)
    }

    @Test
    fun `multiple markers returned in configured order not file order`() {
        val text = "## B\nb1\n\n## A\na1\n"
        val result = parseJournal(text, listOf("## A", "## B"))
        assertEquals(2, result.size)
        assertEquals("## A", result[0].title)
        assertEquals("## B", result[1].title)
    }

    @Test
    fun `marker in file but not in configured list is ignored`() {
        val text = "## Goals\nitem1\n\n## Ignored\nignored\n"
        val result = parseJournal(text, listOf("## Goals"))
        assertEquals(1, result.size)
        assertEquals("## Goals", result[0].title)
    }

    @Test
    fun `marker in configured list but absent from file is skipped`() {
        val text = "## Goals\nitem1\n"
        val result = parseJournal(text, listOf("## Goals", "## Missing"))
        assertEquals(1, result.size)
        assertEquals("## Goals", result[0].title)
    }

    @Test
    fun `items end at EOF with no trailing blank line are still captured`() {
        val text = "## Goals\nitem1\nitem2"
        val result = parseJournal(text, listOf("## Goals"))
        assertEquals(1, result.size)
        assertEquals(listOf("item1", "item2"), result[0].items)
    }

    @Test
    fun `multiple consecutive blank lines between sections are handled`() {
        val text = "## A\na1\n\n\n## B\nb1\n"
        val result = parseJournal(text, listOf("## A", "## B"))
        assertEquals(2, result.size)
        assertEquals(listOf("a1"), result[0].items)
        assertEquals(listOf("b1"), result[1].items)
    }

    @Test
    fun `empty file returns empty result`() {
        val result = parseJournal("", listOf("## Goals"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `marker with zero items before next blank is omitted from result`() {
        val text = "## Goals\n\n## Other\nitem\n"
        val result = parseJournal(text, listOf("## Goals", "## Other"))
        assertEquals(1, result.size)
        assertEquals("## Other", result[0].title)
    }

    @Test
    fun `whitespace only lines between items are treated as blank and end section`() {
        val text = "## Goals\nitem1\n   \nitem2"
        val result = parseJournal(text, listOf("## Goals"))
        assertEquals(1, result.size)
        assertEquals(listOf("item1"), result[0].items)
    }

    @Test
    fun `marker line with leading and trailing spaces in file is matched`() {
        val text = "  ## Goals  \nitem1\n"
        val result = parseJournal(text, listOf("## Goals"))
        assertEquals(1, result.size)
        assertEquals(listOf("item1"), result[0].items)
    }
}
