package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.tomcory.heimdall.persistence.database.entity.App
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(vararg app: App)

    @Query("UPDATE App SET isInstalled = 0 WHERE packageName = :packageName")
    suspend fun updateIsInstalled(packageName: String)

    @Query("SELECT * FROM App")
    suspend fun getAll(): List<App>


    @Query("SELECT * FROM App")
    fun getAllObservable(): Flow<List<App>>
    //todo rap to the flow

    @Query("SELECT * FROM App WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackageName(packageName: String): App?
}