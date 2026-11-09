package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        WorkspaceEntity::class,
        FolderEntity::class,
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        TaskEntity::class,
        AttachmentEntity::class,
        EmbeddingEntity::class,
        ShareEntity::class,
        AIConversationEntity::class,
        AIMessageEntity::class,
        SecurityLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MindoraDatabase : RoomDatabase() {
    abstract fun mindoraDao(): MindoraDao

    companion object {
        @Volatile
        private var INSTANCE: MindoraDatabase? = null

        fun getInstance(context: Context): MindoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MindoraDatabase::class.java,
                    "mindora_ai_workspace.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
