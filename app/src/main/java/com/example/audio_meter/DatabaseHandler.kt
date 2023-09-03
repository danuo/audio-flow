package com.example.audio_meter

import android.annotation.SuppressLint
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.annotation.WorkerThread
import android.content.Context
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity


@Entity(tableName = "data_table")
class Value(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val time: Long,
    val value: Float
)


@Dao
interface ValueDao {

    @Query("SELECT * FROM data_table ORDER BY time ASC")
    fun getValues(): Flow<List<Value>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: Value)

    @Query("DELETE FROM data_table")
    suspend fun deleteAll()
}


// Annotates class to be a Room Database with a table (entity) of the Word class
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
    val allValues: Flow<List<Value>> = valueDao.getValues()

    @WorkerThread
    suspend fun insert(value: Value) {
        valueDao.insert(value)
    }

    @WorkerThread
    suspend fun deleteAll() {
        valueDao.deleteAll()
    }
}


class ValueViewModel(private val repository: ValueRepository) : ViewModel() {

    // Using LiveData and caching what allWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allValues: LiveData<List<Value>> = repository.allValues.asLiveData()


    // launching a new coroutine to insert the data in a non-blocking way
    fun insert(value: Value) = viewModelScope.launch {
        repository.insert(value)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }
}


class ValueViewModelFactory(private val repository: ValueRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ValueViewModel(repository) as T
    }
}


class DatabaseHandler(
    context: ComponentActivity,
    private val uiHandler: UiHandler,
) {
    private val database = ValueDatabase.getDatabase(context)
    private val repository = ValueRepository(database.valueDao())
    private val factory = ValueViewModelFactory(repository)
    private val viewModel =
        ViewModelProvider(context, factory).get(modelClass = ValueViewModel::class.java)

    var newestData = listOf<Value>()

    init {
        viewModel.allValues.observe(context) { data ->
            newestData = data
            if (data.isNotEmpty()) {
                uiHandler.updateChart(data)
                uiHandler.updateUI(mapOf("nSamples" to data.size))
            }
        }
        addText("some text")
    }

    fun insertData(value: Float) {
        val time: Long = System.currentTimeMillis() + Random.nextInt(-10000, 10000)
        viewModel.insert(Value(time = time, value = value))
    }

    fun deleteAll() {
        newestData = listOf()
        viewModel.deleteAll()
    }

    @SuppressLint("SetTextI18n")
    private fun addText(text: String) {
        uiHandler.tempTextView.text = uiHandler.tempTextView.text.toString() + text
    }
}