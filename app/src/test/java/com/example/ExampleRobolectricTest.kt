package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.HeaderFormat
import com.example.domain.SmaliParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smali Merger", appName)
    }

    @Test
    fun `verify demo samples have expected smali and extensionless files`() {
        val samples = SmaliParser.getDemoSamples()
        assertTrue(samples.isNotEmpty())

        val adsUtility = samples.find { it.fileName == "Adsutility" }
        assertTrue("Adsutility must be present without extension", adsUtility != null)
        assertTrue(adsUtility!!.hasSmaliDirectives)
        assertEquals(false, adsUtility.isSmaliExtension)

        val seqScope = samples.find { it.fileName == "SequenceScope.smali" }
        assertTrue(seqScope != null)
        assertEquals(true, seqScope!!.isSmaliExtension)
    }

    @Test
    fun `verify merged text generation with user format`() {
        val samples = SmaliParser.getDemoSamples()
        val merged = SmaliParser.generateMergedText(samples, HeaderFormat.PATH_THEN_NAME)

        assertTrue(merged.contains("===== Path kotlin/sequences/SequenceScope.smali ====="))
        assertTrue(merged.contains("SequenceScope.smali"))
        assertTrue(merged.contains(".class public abstract Lkotlin/sequences/SequenceScope;"))
        assertTrue(merged.contains("===== Path com/utility/Adsutility ====="))
        assertTrue(merged.contains("Adsutility"))
    }
}
