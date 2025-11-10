package com.miso.vinilos.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para ImageUrlHelper
 */
class ImageUrlHelperTest {

    @Test
    fun `normalizeImageUrl with null returns null`() {
        val result = ImageUrlHelper.normalizeImageUrl(null)
        assertNull(result)
    }

    @Test
    fun `normalizeImageUrl with blank string returns null`() {
        val result = ImageUrlHelper.normalizeImageUrl("   ")
        assertNull(result)
    }

    @Test
    fun `normalizeImageUrl with empty string returns null`() {
        val result = ImageUrlHelper.normalizeImageUrl("")
        assertNull(result)
    }

    @Test
    fun `normalizeImageUrl with valid https URL returns same URL`() {
        val url = "https://example.com/image.jpg"
        val result = ImageUrlHelper.normalizeImageUrl(url)
        assertEquals(url, result)
    }

    @Test
    fun `normalizeImageUrl with valid http URL returns same URL`() {
        val url = "http://example.com/image.jpg"
        val result = ImageUrlHelper.normalizeImageUrl(url)
        assertEquals(url, result)
    }

    @Test
    fun `normalizeImageUrl trims whitespace`() {
        val url = "  https://example.com/image.jpg  "
        val result = ImageUrlHelper.normalizeImageUrl(url)
        assertEquals("https://example.com/image.jpg", result)
    }

    @Test
    fun `normalizeImageUrl converts relative URL to absolute with baseUrl`() {
        val relativeUrl = "images/photo.jpg"
        val baseUrl = "https://example.com"
        val result = ImageUrlHelper.normalizeImageUrl(relativeUrl, baseUrl)
        assertEquals("https://example.com/images/photo.jpg", result)
    }

    @Test
    fun `normalizeImageUrl converts relative URL with leading slash`() {
        val relativeUrl = "/images/photo.jpg"
        val baseUrl = "https://example.com"
        val result = ImageUrlHelper.normalizeImageUrl(relativeUrl, baseUrl)
        assertEquals("https://example.com/images/photo.jpg", result)
    }

    @Test
    fun `normalizeImageUrl handles baseUrl with trailing slash`() {
        val relativeUrl = "images/photo.jpg"
        val baseUrl = "https://example.com/"
        val result = ImageUrlHelper.normalizeImageUrl(relativeUrl, baseUrl)
        assertEquals("https://example.com/images/photo.jpg", result)
    }

    @Test
    fun `normalizeImageUrl returns null for relative URL without baseUrl`() {
        val relativeUrl = "images/photo.jpg"
        val result = ImageUrlHelper.normalizeImageUrl(relativeUrl)
        assertNull(result)
    }

    @Test
    fun `normalizeImageUrl fixes spaces in URL`() {
        val url = "https://example.com/image with spaces.jpg"
        val result = ImageUrlHelper.normalizeImageUrl(url)
        assertEquals("https://example.com/image%20with%20spaces.jpg", result)
    }

    @Test
    fun `isValidImageUrl returns true for valid URL`() {
        val isValid = ImageUrlHelper.isValidImageUrl("https://example.com/image.jpg")
        assertTrue(isValid)
    }

    @Test
    fun `isValidImageUrl returns false for null`() {
        val isValid = ImageUrlHelper.isValidImageUrl(null)
        assertFalse(isValid)
    }

    @Test
    fun `isValidImageUrl returns false for blank string`() {
        val isValid = ImageUrlHelper.isValidImageUrl("   ")
        assertFalse(isValid)
    }

    @Test
    fun `normalizeImageUrl handles file protocol`() {
        val url = "file:///path/to/image.jpg"
        val result = ImageUrlHelper.normalizeImageUrl(url)
        assertNotNull(result)
        assertTrue(result!!.startsWith("file://"))
    }
}

