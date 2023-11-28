package de.tomcory.heimdall.ui.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.PrimaryKey
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.Request
import de.tomcory.heimdall.persistence.database.entity.Response
import de.tomcory.heimdall.telemetry.TelemetryExport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class TelemetryViewModel() : ViewModel() {

    private val db: HeimdallDatabase? = HeimdallDatabase.instance
    val requests: Flow<List<Request>>? = db?.requestDao?.getAllObservable()
    val responses: Flow<List<Response>>? = db?.responseDao?.getAllObservable()


    fun exportTelemetryData() {
        viewModelScope.launch {

          requests?.collect { listOfRequests ->
              Timber.d("not enteries")
            listOfRequests.forEach { request ->

                Timber.d(request.toString())
            }
        }


//            Timber.d("corrutin=ne")
//            Timber.d(requests?.collect().toString() )
        }
        Timber.d("it worked")
    }


    fun createFakeData() {
        viewModelScope.launch {
            // Create a fake request
            val fakeRequest = Request(
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

            // Create a fake response corresponding to the fake request
            val fakeResponse = Response(
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
