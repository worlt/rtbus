package com.trackr.ui.screens.me

import com.trackr.core.data.repository.MAX_FAVORITES_BACKUP_BYTES
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.test.*
import org.junit.Test

class FavoritesFileTransferIoTest {
    @Test fun readsUtf8AndAcceptsAUtf8Bom() {
        val json = "{\"name\":\"广州收藏\"}"
        assertEquals(json, readFavoritesBackup(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))))
        assertEquals(json, readFavoritesBackup(ByteArrayInputStream(("\uFEFF" + json).toByteArray(Charsets.UTF_8))))
    }

    @Test fun rejectsOversizedStreamsWithoutTrustingAvailableOrFileMetadata() {
        val stream = object : InputStream() {
            var remaining = MAX_FAVORITES_BACKUP_BYTES + 1
            override fun available() = 0
            override fun read(): Int = if (remaining-- > 0) 'x'.code else -1
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (remaining <= 0) return -1
                val count = minOf(remaining, length)
                buffer.fill('x'.code.toByte(), offset, offset + count);remaining -= count
                return count
            }
        }
        assertFailsWith<IllegalArgumentException> { readFavoritesBackup(stream) }
    }

    @Test fun readFailuresAreReportedInsteadOfReturningPartialJson() {
        val stream = object : InputStream() { override fun read(): Int = throw IOException("provider unavailable") }
        assertFailsWith<IOException> { readFavoritesBackup(stream) }
    }
}
