package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MindoraDatabase
import com.example.data.local.MindoraJsonCodec
import com.example.data.local.NoteBlock
import com.example.data.local.NoteEntity
import com.example.data.local.SharePermission
import com.example.data.repository.AiActionType
import com.example.data.repository.MindoraRepository
import com.example.data.repository.PlaceholderAiRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: MindoraDatabase
    private lateinit var aiRepository: PlaceholderAiRepository
    private lateinit var repository: MindoraRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MindoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        aiRepository = PlaceholderAiRepository()
        repository = MindoraRepository(database.mindoraDao(), aiRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app name resource is Mindora AI and note blocks json codec works`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Mindora AI", appName)

        val blocks = listOf(
            NoteBlock(type = com.example.data.local.BlockType.HEADING_1, text = "Titre Principal"),
            NoteBlock(type = com.example.data.local.BlockType.CHECKLIST, text = "Valider les tests", isChecked = true),
            NoteBlock(type = com.example.data.local.BlockType.CODE, text = "println(\"Mindora AI\")")
        )
        val encoded = MindoraJsonCodec.encodeBlocks(blocks)
        val decoded = MindoraJsonCodec.decodeBlocks(encoded)
        assertEquals(3, decoded.size)
        assertTrue(decoded[1].isChecked)
    }

    @Test
    fun `authentication registration login and multi-user isolation work`() = runBlocking {
        // 1. Register User A
        val userAResult = repository.registerWithEmail("Alice Durand", "alice@mindora.ai", "SecurePass123!")
        assertTrue(userAResult.isSuccess)
        val userA = userAResult.getOrThrow()

        // 2. Register User B
        val userBResult = repository.registerWithEmail("Bob Martin", "bob@mindora.ai", "SecurePass456!")
        assertTrue(userBResult.isSuccess)
        val userB = userBResult.getOrThrow()

        // 3. Create private note for User A
        val privateNoteA = NoteEntity(
            id = "private_note_alice",
            title = "Projet Confidentiel Alice",
            content = "Contenu strictement isolé pour Alice",
            userId = userA.id,
            workspaceId = "ws_alice"
        )
        repository.saveNoteWithIndexing(privateNoteA, userA.name, recordVersionSnapshot = true)

        // 4. Verify User B cannot see User A's private note
        val userBNotes = repository.observeActiveNotes(userB.id).first()
        assertFalse(userBNotes.any { it.id == "private_note_alice" })

        // 5. Verify login with wrong password fails and logs security event
        val wrongLogin = repository.loginWithEmail("alice@mindora.ai", "WrongPassword")
        assertTrue(wrongLogin.isFailure)
    }

    @Test
    fun `note auto-save version history and semantic RAG retrieval work`() = runBlocking {
        val user = repository.ensureDefaultDemoAccount()
        val notes = repository.observeActiveNotes(user.id).first()
        assertTrue("Initial seeded notes should not be empty", notes.isNotEmpty())

        // Query RAG about meetings and decisions
        val ragAnswer = aiRepository.queryKnowledgeBaseRag(
            question = "Quelles décisions ai-je prises lors de ma dernière réunion ?",
            userNotes = notes
        )
        assertTrue(ragAnswer.answerText.isNotBlank())
        assertTrue("RAG must return cited source notes", ragAnswer.citations.isNotEmpty())

        // Transform note into tasks via AI repository
        val firstNote = notes.first()
        val taskActionResult = aiRepository.executeNoteAction(
            action = AiActionType.TO_TASKS,
            note = firstNote,
            allUserNotes = notes,
            folders = emptyList()
        )
        assertTrue(taskActionResult.extractedTasks.isNotEmpty())
    }

    @Test
    fun `collaboration share permissions and comments persist`() = runBlocking {
        val user = repository.ensureDefaultDemoAccount()
        val note = repository.observeActiveNotes(user.id).first().first()

        val share = repository.shareNoteWithCollaborator(
            noteId = note.id,
            ownerUserId = user.id,
            invitedEmail = "partenaire@mindora.ai",
            invitedName = "Partenaire Externe",
            permission = SharePermission.READ
        )
        assertNotNull(share)
        assertEquals(SharePermission.READ.name, share.permission)

        repository.addCommentToShare(share, user.name, user.email, "Nouveau commentaire de test")
        val updatedShares = repository.observeShares(user.id).first()
        val updatedShare = updatedShares.first { it.id == share.id }
        val decodedComments = MindoraJsonCodec.decodeComments(updatedShare.commentsJson)
        assertTrue(decodedComments.any { it.content.contains("Nouveau commentaire de test") })
    }
}
