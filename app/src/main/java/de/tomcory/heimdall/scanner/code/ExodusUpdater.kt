package de.tomcory.heimdall.scanner.code

import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.Tracker
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import timber.log.Timber


object ExodusUpdater {

    suspend fun updateAll() {
        Timber.d("Querying Exodus API...")

        val apiInstance = Retrofit.Builder()
            .baseUrl(ExodusAPIInterface.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ExodusAPIInterface::class.java)

        val result = apiInstance.getAllTrackers()

        if (result.isSuccessful && result.body() != null) {
            Timber.d("Successfully queried Exodus API.")
            val trackersRaw = result.body()

            val trackerList = mutableListOf<Tracker>()

            Timber.d("Updating database...")

            trackersRaw?.trackers?.values?.forEach{
                trackerList.add(Tracker(
                    name = it.name,
                    categories = it.categories.joinToString(","),
                    codeSignature = it.code_signature,
                    networkSignature = it.network_signature,
                    creationDate = it.creation_date,
                    web = it.website
                ))
            }

            if(trackerList.isNotEmpty()) {
                HeimdallDatabase.instance?.trackerDao?.deleteAllTrackers()
                HeimdallDatabase.instance?.trackerDao?.insertTrackers(*trackerList.toTypedArray())
                Timber.d("Database updated, ${trackerList.size} trackers added.")
            } else {
                Timber.w("Database not updated, no trackers found.")
            }

        } else {
            Timber.w("Failed to query Exodus API, response code: ${result.code()}.")
        }
    }
}

data class ExodusTracker(
    val categories: List<String> = emptyList(),
    val code_signature: String = String(),
    val creation_date: String = String(),
    val description: String = String(),
    val name: String = String(),
    val network_signature: String = String(),
    val website: String = String()
)

data class ExodusTrackers(
    val trackers: Map<String, ExodusTracker> = emptyMap()
)

interface ExodusAPIInterface {
    companion object {
        const val BASE_URL = "https://reports.exodus-privacy.eu.org/api/"
    }

    @GET("trackers")
    suspend fun getAllTrackers(): Response<ExodusTrackers>
}