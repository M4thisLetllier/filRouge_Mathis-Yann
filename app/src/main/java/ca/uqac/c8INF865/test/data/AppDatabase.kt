package ca.uqac.c8INF865.test.data

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reveils")
data class ReveilEntity(
    @androidx.room.PrimaryKey val id: Int,
    val nom: String,
    val heure: String,
    val dateProchaine: String,
    val nbAmis: Int,
    val actif: Boolean
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @androidx.room.PrimaryKey val id: Int,
    val name: String,
    val phone: String
)

@Entity(
    tableName = "reveil_contacts",
    primaryKeys = ["reveilId", "contactId"]
)
data class ReveilContactEntity(
    val reveilId: Int,
    val contactId: Int
)

@Entity(tableName = "sons")
data class SonEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val chemin: String,
    val dateCreation: Long
)

@Dao
interface ReveilDao {
    @Query("SELECT * FROM reveils ORDER BY id")
    fun observerTous(): Flow<List<ReveilEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererTous(reveils: List<ReveilEntity>)

    @Query("UPDATE reveils SET actif = :actif WHERE id = :id")
    suspend fun modifierEtat(id: Int, actif: Boolean)

    @Query("UPDATE reveils SET nom = :nom, heure = :heure WHERE id = :id")
    suspend fun modifierDetails(id: Int, nom: String, heure: String)

    @Insert
    suspend fun inserer(reveil: ReveilEntity)

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM reveils")
    suspend fun prochainId(): Int

    @Query("DELETE FROM reveils WHERE id = :id")
    suspend fun supprimer(id: Int)

    @Query("SELECT COUNT(*) FROM reveils")
    suspend fun compter(): Int
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY id")
    fun observerTous(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererTous(contacts: List<ContactEntity>)

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun compter(): Int
}

@Dao
interface ReveilContactDao {
    @Query("SELECT * FROM reveil_contacts")
    fun observerToutesLesAssociations(): Flow<List<ReveilContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun associer(association: ReveilContactEntity)

    @Query("DELETE FROM reveil_contacts WHERE reveilId = :reveilId AND contactId = :contactId")
    suspend fun dissocier(reveilId: Int, contactId: Int)

    @Query("DELETE FROM reveil_contacts WHERE reveilId = :reveilId")
    suspend fun supprimerAssociations(reveilId: Int)
}

@Dao
interface SonDao {
    @Query("SELECT * FROM sons ORDER BY dateCreation DESC")
    fun observerTous(): Flow<List<SonEntity>>

    @Insert
    suspend fun inserer(son: SonEntity)

    @Delete
    suspend fun supprimer(son: SonEntity)
}

@Database(
    entities = [ReveilEntity::class, ContactEntity::class, ReveilContactEntity::class, SonEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reveilDao(): ReveilDao
    abstract fun contactDao(): ContactDao
    abstract fun reveilContactDao(): ReveilContactDao
    abstract fun sonDao(): SonDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reveils_contacts.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS reveil_contacts " +
                        "(reveilId INTEGER NOT NULL, contactId INTEGER NOT NULL, " +
                        "PRIMARY KEY(reveilId, contactId))"
                )
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS sons " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "nom TEXT NOT NULL, chemin TEXT NOT NULL, dateCreation INTEGER NOT NULL)"
                )
            }
        }
    }
}
