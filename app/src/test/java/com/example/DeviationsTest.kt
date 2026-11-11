package com.example

import com.example.data.deviations.DeviationsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviationsTest {

    @Test
    fun `verify Dark Ranks mapping from 2000 down to 0`() {
        // At 2000.0 -> Rank 1: المتغير, gripProgress = 0.0
        val rank2000 = DeviationsManager.getDarkRank(2000.0)
        assertEquals(1, rank2000.index)
        assertEquals("المتغير", rank2000.title)
        assertEquals(0.0f, rank2000.gripProgress, 0.01f)

        // At 0.0 -> Rank 20: بافوميت, gripProgress = 1.0 (fully gripped)
        val rank0 = DeviationsManager.getDarkRank(0.0)
        assertEquals(20, rank0.index)
        assertEquals("بافوميت", rank0.title)
        assertEquals(1.0f, rank0.gripProgress, 0.01f)

        // Boundary checks
        assertTrue(DeviationsManager.isDarkTrack(0.0))
        assertTrue(DeviationsManager.isDarkTrack(1000.0))
        assertTrue(DeviationsManager.isDarkTrack(2000.0))
        assertFalse(DeviationsManager.isDarkTrack(2001.0))
    }

    @Test
    fun `verify Light Ranks mapping from 2001 up to 20000`() {
        // At 2001.0 -> Rank 1: التائب, approachProgress = 0.0
        val rank2001 = DeviationsManager.getLightRank(2001.0)
        assertEquals(1, rank2001.index)
        assertEquals("التائب", rank2001.title)
        assertEquals(0.0f, rank2001.approachProgress, 0.01f)

        // At 20000.0 -> Rank 20: جبريل, approachProgress = 1.0 (pure white angel touch)
        val rank20000 = DeviationsManager.getLightRank(20000.0)
        assertEquals(20, rank20000.index)
        assertEquals("جبريل", rank20000.title)
        assertEquals(1.0f, rank20000.approachProgress, 0.01f)
    }

    @Test
    fun `verify 50-Tier Biological System mapping`() {
        // Tier 1 at 0 PTS: الأنثى المطلقة
        val tier1 = DeviationsManager.getBiologicalTier(0.0)
        assertEquals(1, tier1.index)
        assertEquals("الأنثى المطلقة", tier1.title)
        assertEquals(1.0f, tier1.estrogenPercent, 0.01f)
        assertEquals(0.0f, tier1.testosteronePercent, 0.01f)

        // Tier 20 around 7900-8000 PTS: نقطة التعادل
        val tier20 = DeviationsManager.getBiologicalTier(7900.0)
        assertEquals(20, tier20.index)
        assertEquals("نقطة التعادل", tier20.title)

        // Tier 50 at 20000 PTS: هرقل
        val tier50 = DeviationsManager.getBiologicalTier(20000.0)
        assertEquals(50, tier50.index)
        assertEquals("هرقل", tier50.title)
        assertEquals(0.0f, tier50.estrogenPercent, 0.01f)
        assertEquals(1.0f, tier50.testosteronePercent, 0.01f)
    }

    @Test
    fun `verify all 20 Dark Ranks and 20 Light Ranks are complete and non-empty`() {
        assertEquals(20, DeviationsManager.DARK_RANKS.size)
        assertEquals(20, DeviationsManager.LIGHT_RANKS.size)
        assertEquals(50, DeviationsManager.BIOLOGICAL_50_TIERS.size)

        DeviationsManager.DARK_RANKS.forEach { assertTrue(it.isNotBlank()) }
        DeviationsManager.LIGHT_RANKS.forEach { assertTrue(it.isNotBlank()) }
        DeviationsManager.BIOLOGICAL_50_TIERS.forEach { assertTrue(it.isNotBlank()) }
    }
}
