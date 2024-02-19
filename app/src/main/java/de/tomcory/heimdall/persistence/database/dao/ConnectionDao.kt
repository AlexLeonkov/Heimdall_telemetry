package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.tomcory.heimdall.persistence.database.entity.Connection
import de.tomcory.heimdall.persistence.database.entity.Response
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg connections: Connection): List<Long>

    @Update
    suspend fun update(vararg connections: Connection)

    @Query("UPDATE Connection SET bytesOut = bytesIn + :delta WHERE id = :id")
    suspend fun updateBytesOut(id: Long, delta: Int)

    @Query("UPDATE Connection SET bytesIn = bytesIn + :delta WHERE id = :id")
    suspend fun updateBytesIn(id: Long, delta: Int)

    @Query("SELECT * FROM Connection")
    suspend fun getAll(): List<Connection>

    @Query("Select * FROM Connection")
    fun getAllObservable(): Flow<List<Connection>>
}