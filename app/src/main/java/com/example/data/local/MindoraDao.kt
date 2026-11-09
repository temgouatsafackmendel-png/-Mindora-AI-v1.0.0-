package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MindoraDao {
    // --- USERS & AUTH ---
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // --- WORKSPACES ---
    @Query("SELECT * FROM workspaces WHERE ownerId = :userId ORDER BY createdAt ASC")
    fun observeWorkspaces(userId: String): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE ownerId = :userId ORDER BY createdAt ASC")
    suspend fun getWorkspacesOnce(userId: String): List<WorkspaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE id = :workspaceId AND ownerId = :userId")
    suspend fun deleteWorkspace(workspaceId: String, userId: String)

    // --- FOLDERS ---
    @Query("SELECT * FROM folders WHERE userId = :userId ORDER BY name ASC")
    fun observeFolders(userId: String): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId AND userId = :userId")
    suspend fun deleteFolder(folderId: String, userId: String)

    // --- NOTES ---
    @Query("SELECT * FROM notes WHERE userId = :userId AND isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActiveNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE userId = :userId AND isTrashed = 0 ORDER BY updatedAt DESC")
    suspend fun getActiveNotesOnce(userId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId AND isTrashed = 1 ORDER BY updatedAt DESC")
    fun observeTrashedNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId AND userId = :userId LIMIT 1")
    suspend fun getNoteById(noteId: String, userId: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId AND userId = :userId")
    suspend fun deleteNotePermanently(noteId: String, userId: String)

    @Query("DELETE FROM notes WHERE userId = :userId AND isTrashed = 1")
    suspend fun emptyTrash(userId: String)

    // --- TAGS & NOTE_TAGS ---
    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    fun observeTags(userId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    suspend fun getTagsOnce(userId: String): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId AND userId = :userId")
    suspend fun deleteTag(tagId: String, userId: String)

    @Query("SELECT * FROM note_tags")
    fun observeAllNoteTagCrossRefs(): Flow<List<NoteTagCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun removeNoteTagCrossRef(noteId: String, tagId: String)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: String)

    // --- TASKS ---
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC, createdAt DESC")
    fun observeTasks(userId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId AND userId = :userId")
    suspend fun deleteTask(taskId: String, userId: String)

    // --- ATTACHMENTS (VOICE, PHOTO, OCR, PDF, LINKS) ---
    @Query("SELECT * FROM attachments ORDER BY createdAt DESC")
    fun observeAllAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun observeAttachmentsForNote(noteId: String): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId: String)

    // --- EMBEDDINGS (SEMANTIC RAG) ---
    @Query("SELECT * FROM embeddings WHERE noteId IN (SELECT id FROM notes WHERE userId = :userId AND isTrashed = 0)")
    suspend fun getEmbeddingsForUser(userId: String): List<EmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: EmbeddingEntity)

    @Query("DELETE FROM embeddings WHERE noteId = :noteId")
    suspend fun deleteEmbeddingsForNote(noteId: String)

    // --- SHARES & COLLABORATION ---
    @Query("SELECT * FROM shares WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeShares(userId: String): Flow<List<ShareEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: ShareEntity)

    @Update
    suspend fun updateShare(share: ShareEntity)

    @Query("DELETE FROM shares WHERE id = :shareId AND userId = :userId")
    suspend fun deleteShare(shareId: String, userId: String)

    // --- AI CONVERSATIONS & MESSAGES ---
    @Query("SELECT * FROM ai_conversations WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeConversations(userId: String): Flow<List<AIConversationEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<AIMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AIConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AIMessageEntity)

    @Query("DELETE FROM ai_conversations WHERE id = :conversationId AND userId = :userId")
    suspend fun deleteConversation(conversationId: String, userId: String)

    // --- SECURITY LOGS ---
    @Query("SELECT * FROM security_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 100")
    fun observeSecurityLogs(userId: String): Flow<List<SecurityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityLog(log: SecurityLogEntity)
}
