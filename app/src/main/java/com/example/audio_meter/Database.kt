package com.example.audio_meter

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


@Entity(tableName = "data_table")
class Value(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val time: Long,
    val maxAmpDbu: Float,
    val rmsAmpDbu: Float
)


@Dao
interface ValueDao {
    @Query("SELECT COUNT(*) FROM data_table")
    fun getDataCount(): Flow<Int>

    @Query("SELECT * FROM data_table ORDER BY time ASC")
    fun getValuesAll(): Flow<List<Value>>

    @Query("SELECT * FROM data_table WHERE time > :timeStamp ORDER BY time ASC")
    fun getValuesNewerThan(timeStamp: Long): Flow<List<Value>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: Value)

    @Query("DELETE FROM data_table")
    suspend fun deleteAll()

    @Query("DELETE FROM data_table WHERE time < :timeStamp")
    suspend fun deleteOlderThan(timeStamp: Long)
}


@Database(entities = [Value::class], version = 1, exportSchema = false)
abstract class ValueDatabase : RoomDatabase() {

    abstract fun valueDao(): ValueDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: ValueDatabase? = null

        fun getDatabase(): ValueDatabase? {
            return INSTANCE
        }

        fun loadDatabase(context: Context) {
            // if the INSTANCE is not null, then return it, else create the database
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ValueDatabase::class.java,
                    "value_database"
                ).build()
                INSTANCE = instance
            }
        }
    }
}


class ValueRepository(private val valueDao: ValueDao) {
    fun getDataCount(): Flow<Int> {
        return valueDao.getDataCount()
    }

    fun getValuesNewerThan(timeStamp: Long): Flow<List<Value>> {
        return valueDao.getValuesNewerThan(timeStamp)
    }

    @WorkerThread
    suspend fun insert(value: Value) {
        valueDao.insert(value)
    }

    @WorkerThread
    suspend fun deleteAll() {
        valueDao.deleteAll()
    }

    @WorkerThread
    suspend fun deleteOlderThan(timeStamp: Long) {
        valueDao.deleteOlderThan(timeStamp)
    }
}


class ValueViewModel() :
    ViewModel() {
    private val database = ValueDatabase.getDatabase()
    private val repository = ValueRepository(database!!.valueDao())

    fun getDataCount(): Flow<Int> {
        return repository.getDataCount()
    }

    fun getValuesNewerThan(timeStamp: Long): Flow<List<Value>> {
        return repository.getValuesNewerThan(timeStamp)
    }

    fun insert(value: Value) = viewModelScope.launch {
        repository.insert(value)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

    fun deleteOlderThan(timeStamp: Long) = viewModelScope.launch {
        repository.deleteOlderThan(timeStamp)
    }
}
