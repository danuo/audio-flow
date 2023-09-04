package com.example.audio_meter

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
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
    val value: Float
)


@Dao
interface ValueDao {
    @Query("SELECT COUNT(*) FROM data_table")
    fun getDataCount(): Flow<Int>

    @Query("SELECT * FROM data_table ORDER BY time ASC")
    fun getValuesAll(): List<Value>

    @Query("SELECT * FROM data_table WHERE time > :timeStamp ORDER BY time ASC")
    fun getValuesNewerThan(timeStamp: Long): Flow<List<Value>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: Value)

    @Query("DELETE FROM data_table")
    suspend fun deleteAll()

    @Query("DELETE FROM data_table WHERE time < :timeStamp")
    suspend fun deleteOlderThan(timeStamp: Long)
}


// Annotates class to be a Room Database with a table (entity)
@Database(entities = [Value::class], version = 1, exportSchema = false)
abstract class ValueDatabase : RoomDatabase() {

    abstract fun valueDao(): ValueDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: ValueDatabase? = null

        fun getDatabase(context: Context): ValueDatabase {
            // if the INSTANCE is not null, then return it, else create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ValueDatabase::class.java,
                    "value_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}


class ValueRepository(private val valueDao: ValueDao) {
    val getDataCount: Flow<Int> = valueDao.getDataCount()

    fun getValuesAll(): List<Value> {
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


class ValueViewModel(private val repository: ValueRepository) : ViewModel() {
    val getDataCount: LiveData<Int> = repository.getDataCount.asLiveData()

    fun getAllValues(): List<Value> {
        return repository.getValuesAll()
    }

    // Using LiveData and caching what allWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    fun getValuesNewerThan(timeStamp: Long): LiveData<List<Value>> {
        return repository.getValuesNewerThan(timeStamp).asLiveData()
    }

    // launching a new coroutine to insert the data in a non-blocking way
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
class ValueViewModelFactory(private val repository: ValueRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ValueViewModel(repository) as T
    }
}


class DatabaseHandler(
    context: MainActivity,
    private val uiHandler: UiHandler,
) {
    private val database = ValueDatabase.getDatabase(context)
    private val repository = ValueRepository(database.valueDao())
    private val factory = ValueViewModelFactory(repository)
    private val viewModel =
        ViewModelProvider(context, factory).get(modelClass = ValueViewModel::class.java)

    var dataCount: Int = 0
    var newestData = listOf<Value>()

    init {
        cleanupDatabase()
        viewModel.getDataCount.observe(context) { data ->
            dataCount = data
            uiHandler.updateUI(mapOf("nSamples" to dataCount))
        }
        val time = System.currentTimeMillis() - context.showMilliseconds
        viewModel.getValuesNewerThan(time).observe(context) { data ->
            newestData = data
            if (data.isNotEmpty()) {
                uiHandler.updateChart(data)
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

    fun cleanupDatabase() {
        // delete data that is older than 10 days
        var time: Long = System.currentTimeMillis() - 10 * 24 * 3600 * 1000
        viewModel.deleteOlderThan(time)
    }
}