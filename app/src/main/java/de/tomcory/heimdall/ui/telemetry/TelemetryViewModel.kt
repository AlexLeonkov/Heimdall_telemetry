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

    interface ApiService {
        @POST("post")
        suspend fun sendRequest(@Body requestData: EntityRequest): Response<Unit>
    }

    private val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.0.219:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()


    private val apiService: ApiService = retrofit.create(ApiService::class.java)





    fun exportTelemetryData() {
        viewModelScope.launch {
            requests?.collect { listOfRequests ->
                listOfRequests.forEach { request ->
                    try {
                        val response = apiService.sendRequest(request)
                        if (response.isSuccessful) {
                            Timber.d("Data sent successfully")
                        } else {
                            Timber.e("Error sending data")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to send data")
                    }
                }
            }
        }
        Timber.d("it worked")
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


            exportTelemetryData()

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
