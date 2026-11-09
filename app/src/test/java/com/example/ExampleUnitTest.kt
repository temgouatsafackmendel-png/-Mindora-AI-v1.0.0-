package com.example

import com.example.data.local.PlanTier
import com.example.data.repository.PlaceholderAiRepository
import com.example.ui.viewmodel.NoteTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `embedding cosine similarity ranks semantically related texts higher`() {
        val aiRepo = PlaceholderAiRepository()
        val vecSaas1 = aiRepo.computeEmbeddingVector("Architecture projet SaaS roadmap et lancement produit")
        val vecSaas2 = aiRepo.computeEmbeddingVector("Développement MVP produit SaaS et architecture technique")
        val vecAudio = aiRepo.computeEmbeddingVector("Enregistrement audio vocal et transcription microphone")

        val simRelated = aiRepo.cosineSimilarity(vecSaas1, vecSaas2)
        val simUnrelated = aiRepo.cosineSimilarity(vecSaas1, vecAudio)

        assertTrue("Related SaaS vectors must have higher similarity ($simRelated vs $simUnrelated)", simRelated > simUnrelated)
    }

    @Test
    fun `saas subscription tiers enforce progressive quotas`() {
        assertTrue(PlanTier.PRO.maxNotes > PlanTier.FREE.maxNotes)
        assertTrue(PlanTier.BUSINESS.maxNotes > PlanTier.PRO.maxNotes)
        assertTrue(PlanTier.PRO.maxAiQueriesPerMonth > PlanTier.FREE.maxAiQueriesPerMonth)
        assertEquals("FREE", PlanTier.FREE.displayName)
    }

    @Test
    fun `v2 smart note templates provide structured blocks and summaries`() {
        assertEquals(5, NoteTemplate.entries.size)
        NoteTemplate.entries.forEach { template ->
            assertTrue(template.initialBlocks.isNotEmpty())
            assertTrue(template.summary.isNotEmpty())
        }
    }
}
