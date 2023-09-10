package com.example.audio_meter

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.Job
import androidx.lifecycle.lifecycleScope


@Entity(tableName = "data_table")
class Value(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val time: Long,
    val value: Float
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

    fun getValuesAll(): Flow<List<Value>> {
        return valueDao.getValuesAll()
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


class ValueViewModel(
    private val repository: ValueRepository
) :
    ViewModel() {
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


@Suppress("UNCHECKED_CAST")
class ValueViewModelFactory(
    private val repository: ValueRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ValueViewModel(repository) as T
    }
}


class DatabaseHandler(
    private val context: MainActivity,
    private val uiHandler: UiHandler,
) {
    private val application: MainApplication = MainApplication.getInstance()
    private val database = ValueDatabase.getDatabase()
    private val repository = ValueRepository(database!!.valueDao())
    private val factory = ValueViewModelFactory(repository)
    private val viewModel =
        ViewModelProvider(context, factory).get(modelClass = ValueViewModel::class.java)

    var dataCount: Int = 10
    var newestData = listOf<Value>()

    private var job: Job? = null

    init {
        cleanupDatabase()
        context.lifecycleScope.launch {
            viewModel.getDataCount().collect() { data ->
                dataCount = data
                uiHandler.updateUI(mapOf("nSamples" to dataCount))
            }
        }
        renewDataQuery()
    }

    fun renewDataQuery() {
        val initTime = System.currentTimeMillis()
        val timeStamp = initTime - application.showMilliseconds
        job?.cancel()
        job = context.lifecycleScope.launch {
            viewModel.getValuesNewerThan(timeStamp).collect() { data ->
                newestData = data
                if (data.isNotEmpty()) {
                    uiHandler.uiChart.updateChart(data)
                    // send to server
                }
                if (System.currentTimeMillis() - initTime > 1000 * 10) {
                    renewDataQuery()
                }
            }
        }
    }

    fun insertData(time: Long, value: Float) {
        viewModel.insert(Value(time = time, value = value))
    }

    fun deleteAll() {
        newestData = listOf()
        viewModel.deleteAll()
    }

    private fun cleanupDatabase() {
        // delete data that is older than 10 days
        val time: Long = System.currentTimeMillis() - 10 * 24 * 3600 * 1000
        viewModel.deleteOlderThan(time)
    }
}