package de.tomcory.heimdall.ui.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.PrimaryKey
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.Request as EntityRequest
import de.tomcory.heimdall.persistence.database.entity.Response as EntityResponse


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import timber.log.Timber



import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.Response


import retrofit2.converter.gson.GsonConverterFactory


class TelemetryViewModel() : ViewModel() {

    private val db: HeimdallDatabase? = HeimdallDatabase.instance
    val requests: Flow<List<EntityRequest>>? = db?.requestDao?.getAllObservable()
    val responses: Flow<List<EntityResponse>>? = db?.responseDao?.getAllObservable()


    companion object {
        const val FULLY_ANONYMIZED = 1 shl 0
        const val NO_IP_TIMESTAMPS = 1 shl 1
        const val DISCLOSED_CONTENT = 1 shl 2
    }

    interface ApiService {
        @POST("post")
        suspend fun sendRequest(@Body requestData: EntityRequest): Response<Unit>
    }

    private val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.2.164:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()


    private val apiService: ApiService = retrofit.create(ApiService::class.java)



    fun anonymiseData(request: EntityRequest, options: Int): EntityRequest {
        // Clone the original request to avoid modifying the original object
        var anonymisedRequest = request.copy()

        if (options and FULLY_ANONYMIZED != 0) {
            // Implement FULLY_ANONYMIZED logic
            anonymisedRequest = anonymisedRequest.copy(
                    remoteIp = "0.0.0.0", // Use a dummy IP address
                    remotePort = -1,       // Use a special value to indicate anonymization
                    localIp = "0.0.0.0",   // Use a dummy IP address
                    localPort = -1         // Use a special value to indicate anonymization
                    // Set other fields to anonymized values as necessary
            )
        }

        if (options and NO_IP_TIMESTAMPS != 0) {
            // Implement NO_IP_TIMESTAMPS logic
            anonymisedRequest = anonymisedRequest.copy(
                    remoteIp = "0.0.0.0", // Use a dummy IP address
                    remotePort = -1,       // Use a special value to indicate anonymization
                    timestamp = -1         // Use a special value to indicate anonymization
            )
        }


        if (options and DISCLOSED_CONTENT != 0) {
            // Implement DISCLOSED_CONTENT logic
            anonymisedRequest = anonymisedRequest.copy(
                    content = "Content is disclosed"
                    // Modify as necessary for disclosed content
            )
        }

        return anonymisedRequest
    }


    private var lastSentTimestamp: Long = 0
    fun exportTelemetryData(options: Int) {
        viewModelScope.launch {
            // Retrieve unsent requests that are newer than the last sent timestamp
            val unsentRequests = db?.requestDao?.getUnsentRequests(lastSentTimestamp)

            // Track the timestamp of the most recently sent request
            var mostRecentSentTimestamp = lastSentTimestamp

            unsentRequests?.forEach { request ->
                val anonymisedRequest = anonymiseData(request, options)
                try {
                    val response = apiService.sendRequest(anonymisedRequest)
                    if (response.isSuccessful) {
                        Timber.d("Data sent successfully")
                        // Update the most recent sent timestamp
                        mostRecentSentTimestamp = maxOf(mostRecentSentTimestamp, request.timestamp)
                    } else {
                        Timber.e("Error sending data")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to send data")
                }
            }

            // After all requests have been processed, update the last sent timestamp
            if (mostRecentSentTimestamp != lastSentTimestamp) {
                lastSentTimestamp = mostRecentSentTimestamp
                // Optionally, persist the lastSentTimestamp to storage to maintain state across app restarts
                // saveLastSentTimestamp(lastSentTimestamp)
            }
        }
    }




    fun createFakeData() {
        viewModelScope.launch {
            // Create a fake request
            val fakeRequest = EntityRequest(
                    id = 1,
                    timestamp = 1633036800000,
                    reqResId = 101,
                    headers = "Content-Type: application/json",
                    content = "{\"key\":\"value\"}",
                    contentLength = 15,
                    method = "GET",
                    remoteHost = "example.com",
                    remotePath = "/api/data",
                    remoteIp = "192.168.1.1",
                    remotePort = 8080,
                    localIp = "10.0.0.1",
                    localPort = 8081,
                    initiatorId = 200,
                    initiatorPkg = "com.example.app",
                    isTracker = false
            )

            // Insert the fake request into the database
            db?.requestDao?.insert(fakeRequest)

//todo
            val options = FULLY_ANONYMIZED or NO_IP_TIMESTAMPS
            exportTelemetryData(options)

            // Create a fake response corresponding to the fake request
            val fakeResponse = EntityResponse(
                    id = 1,
                    timestamp = 1633036800000,
                    reqResId = 101,
                    headers = "Content-Type: application/json",
                    content = "{\"response\":\"success\"}",
                    contentLength = 20,
                    statusCode = 200,
                    statusMsg = "OK",
                    remoteHost = "example.com",
                    remoteIp = "192.168.1.1",
                    remotePort = 8080,
                    localIp = "10.0.0.1",
                    localPort = 8081,
                    initiatorId = 200,
                    initiatorPkg = "com.example.app",
                    isTracker = false
            )

            // Insert the fake response into the database
            db?.responseDao?.insert(fakeResponse)
        }
    }


}
