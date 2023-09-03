package com.example.audio_meter

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import android.content.Context
import android.widget.TextView
import androidx.annotation.WorkerThread
import kotlin.random.Random
import kotlin.math.pow
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
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: ValueDatabase? = null

        fun getDatabase(context: Context): ValueDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ValueDatabase::class.java,
                    "value_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}


// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class ValueRepository(private val valueDao: ValueDao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allValues: Flow<List<Value>> = valueDao.getValues()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(value: Value) {
        valueDao.insert(value)
    }

    @Suppress("RedundantSuspendModifier")
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


class WordViewModelFactory(private val repository: ValueRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ValueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ValueViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class DatabaseHandler(context: ComponentActivity, private val textView: TextView) {
    private val database = ValueDatabase.getDatabase(context)
    private val repository = ValueRepository(database.valueDao())
    private val viewModel = ValueViewModel(repository)
    // private val viewModel = ViewModelProvider(context)[ValueViewModel::class.java]

    var newestData = listOf<Value>()

    init {
        generateRandomData()
        addText("randomstuff")
        addText("randomstuff")
        viewModel.allValues.observe(context) { data ->
            newestData = data
            if (data.isNotEmpty()) {
                addText(data[0].value.toString())
                addText("size")
                addText(data.size.toString())
                //for (dat in data) {
                //    addText((dat.value.toString()))
                //}
                addText("added something")
            }
        }
        // Log.d("DatabaseHandler", x.toString())
        generateRandomData()
        generateRandomData()
    }

    fun insertData(value: Float) {
        val time: Long = System.currentTimeMillis() + Random.nextInt(-10000, 10000)
        viewModel.insert(Value(time = time, value = value))
    }

    fun deleteAll() {
        newestData = listOf()
        viewModel.deleteAll()
    }

    private fun generateRandomData() {
        for (i in 0 until 10) {
            val time: Long = System.currentTimeMillis() + Random.nextInt(-10000, 10000)
            val value: Float = Random.nextFloat() * 2f.pow(16)
            val testValue = Value(time = time, value = value)
            viewModel.insert(testValue)
        }
    }

    private fun addText(text: String) {
        textView.text = textView.text.toString() + text
    }
}