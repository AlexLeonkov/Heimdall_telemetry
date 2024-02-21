package de.tomcory.heimdall.telemetry

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.tomcory.heimdall.R
import de.tomcory.heimdall.application.HeimdallApplication
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.App as EntityApp
import de.tomcory.heimdall.persistence.database.entity.Request as EntityRequest
import de.tomcory.heimdall.persistence.database.entity.Response as EntityResponse
import de.tomcory.heimdall.persistence.database.entity.Connection as EntityConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import timber.log.Timber
import java.util.UUID
import de.tomcory.heimdall.ui.main.preferencesStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json





class TelemetryService : Service() {


    object AnonymizationFlags {
        const val NOT_ANONYMIZED = 1 shl 0
        const val ESSENTIAL_DATA_ONLY = 1 shl 1
        const val INCLUDE_HEADERS = 1 shl 2
        const val INCLUDE_CONTENT = 1 shl 3
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val db: HeimdallDatabase? = HeimdallDatabase.instance

    //sorting out already sent data
    private var lastExportedRequestTimestamp: Long = 0
    private var lastExportedResponseTimestamp: Long = 0
    private var lastExportedConnectionTimestamp: Long = 0

    private val deviceIdentifier = UUID.randomUUID().toString()

    //creating connecting with the server, change for the right ip
    private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("http://172.20.10.2:8080/") // Replace with your server's base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private val apiService: ApiService = retrofit.create(ApiService::class.java)


    private fun saveLastExportedTimestamps() {
        val sharedPreferences = getSharedPreferences("TelemetryServicePrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong("lastExportedRequestTimestamp", lastExportedRequestTimestamp)
            putLong("lastExportedResponseTimestamp", lastExportedResponseTimestamp)
            putLong("lastExportedConnectionTimestamp", lastExportedConnectionTimestamp)
            apply()
        }
    }

    private fun loadLastExportedTimestamps() {
        val sharedPreferences = getSharedPreferences("TelemetryServicePrefs", MODE_PRIVATE)
        lastExportedRequestTimestamp = sharedPreferences.getLong("lastExportedRequestTimestamp", 0)
        lastExportedResponseTimestamp = sharedPreferences.getLong("lastExportedResponseTimestamp", 0)
        lastExportedConnectionTimestamp = sharedPreferences.getLong("lastExportedConnectionTimestamp", 0)
    }

    //sorting out already sent apps data
    private val exportedAppsIdentifiers = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        loadLastExportedTimestamps()
        val notification = NotificationCompat.Builder(this, HeimdallApplication.CHANNEL_ID)
                .setContentTitle("Telemetry Service")
                .setContentText("Automatically exporting telemetry data...")
                .setSmallIcon(R.drawable.ic_earth)
                .build()

        startForeground(1, notification)
        fetchPreferencesAndObserveData()
    }




    private var currentAnonymizationType = 1
    private var isDataCollectionInitialized = false

    private fun fetchPreferencesAndObserveData() {
        serviceScope.launch {
            applicationContext.preferencesStore.data
                    .map { it.anonymizationFlags}

                    .distinctUntilChanged() // Only emit when the actual value changes
                    .collect { anonymizationType ->
                        currentAnonymizationType =  anonymizationType
                        if (!isDataCollectionInitialized) {
                            startDataCollection()
                            isDataCollectionInitialized = true
                        } else {
                            // If data collection is already initialized, you can optionally adjust its behavior here
                            Timber.d("Anonymization Type Updated: $anonymizationType - Adjust collection process accordingly")
                        }
                    }
        }
    }



//start listening on changes in the database
    private fun startDataCollection() {
        // Request data collection and export
        serviceScope.launch {
            Timber.d("Starting to collect request data")
            db?.requestDao?.getAllObservable()?.collect { requests ->
                requests.forEach { request ->
                    exportRequestData(request)
                }
            }
            Timber.d("Finished collecting request data")
        }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in request data collection") } }

        // Response data collection and export
        serviceScope.launch {
            Timber.d("Starting to collect response data")
            db?.responseDao?.getAllObservable()?.collect { responses ->
                responses.forEach { response ->
                    exportResponseData(response)
                }
            }
            Timber.d("Finished collecting response data")
        }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in response data collection") } }

        // App data collection and export
        serviceScope.launch {
            Timber.d("Starting to collect app data")
            db?.appDao?.getAllObservable()?.collect { apps ->
                apps.forEach { app ->
                    exportAppData(app)
                }
            }
            Timber.d("Finished collecting app data")
        }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in app data collection") } }

        // Connection data collection and export
        serviceScope.launch {
            Timber.d("Starting to collect connection data")
            db?.connectionDao?.getAllObservable()?.collect { connections ->
                connections.forEach { connection ->
                    exportConnectionData(connection)
                }
            }
            Timber.d("Finished collecting connection data")
        }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in connection data collection") } }
    }



    // export function with anonymization decision
    private suspend fun exportRequestData(request: EntityRequest) {

        // Proceed with sending the processed  request if wasn't sent
        if (request.timestamp > lastExportedRequestTimestamp) {
            //anonymize the data
            val processedRequest = anonymizeRequest(request)
            try {
                val response = apiService.sendRequestData(deviceIdentifier, currentAnonymizationType, processedRequest)
                if (response.isSuccessful) {
                    Timber.d("Request data exported successfully")
                    lastExportedRequestTimestamp = request.timestamp
                    saveLastExportedTimestamps()
                } else {
                    Timber.e("Failed to export request data: $deviceIdentifier")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while exporting request data")
            }
        }
    }

    private suspend fun exportResponseData(response: EntityResponse) {
        if (response.timestamp > lastExportedResponseTimestamp) {
            val processedResponse = anonymizeResponse(response)
            try {
                val responseAPI = apiService.sendResponseData(deviceIdentifier, currentAnonymizationType,  processedResponse)
                if (responseAPI.isSuccessful) {
                    Timber.d("Response data exported successfully")
                    lastExportedResponseTimestamp = response.timestamp
                    saveLastExportedTimestamps()
                } else {
                    Timber.e("Failed to export request data: $deviceIdentifier")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while exporting request data")
            }
        }
    }

    private suspend fun exportConnectionData(connection: EntityConnection) {
        // Decide to anonymize based on the passed anonymization type


        // Proceed with sending the processed (potentially anonymized) connection
        if (connection.initialTimestamp > lastExportedConnectionTimestamp) {
            val processedConnection = anonymizeConnection(connection)
            try {
                val response = apiService.sendConnectionData(deviceIdentifier, processedConnection)
                if (response.isSuccessful) {
                    Timber.d("Connection data exported successfully")
                    lastExportedConnectionTimestamp = processedConnection.initialTimestamp
                    saveLastExportedTimestamps()
                } else {
                    Timber.e("Failed to export connection data: $deviceIdentifier")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while exporting connection data")
            }
        }
    }

    //anonymize the data according to user preferences
    private fun anonymizeRequest(request: EntityRequest): EntityRequest {
        var anonymizedRequest = request

        if (currentAnonymizationType and AnonymizationFlags.ESSENTIAL_DATA_ONLY != 0) {
            // Logic for essential data only
            anonymizedRequest = anonymizedRequest.copy(
                    // Assume transformToEssentialData is a function that modifies the request to include only essential data
                    headers = "Essential-Header-Data",
                    content = "Essential content has been anonymized",
                    localIp = "randomised",
                    localPort = 0
                    // Potentially more transformations
            )
        }

        if (currentAnonymizationType and AnonymizationFlags.INCLUDE_CONTENT != 0) {
            // Include the preprocessed  content

            val preproccesedContent = preprocessContentToJson(request.content)
            Timber.d(preproccesedContent)
            anonymizedRequest = anonymizedRequest.copy(content = preproccesedContent)
        } else if (currentAnonymizationType and AnonymizationFlags.INCLUDE_HEADERS != 0) {
            //handle header inclusion separately if needed
            anonymizedRequest = anonymizedRequest.copy(headers = request.headers)
        }

        // Assuming NOT_ANONYMIZED means returning the request unchanged,
        // there's no need to explicitly handle it here

        return anonymizedRequest
    }

//example of regex preprocessing module for the content field
    fun preprocessContentToJson(jsonContent: String): String {
        // Existing patterns
        val emailPattern = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b".toRegex()
        val yearPattern = "\\b(19|20)\\d{2}\\b".toRegex()
        val genderPattern = "\\b(male|female|WOMENS|MENS)\\b".toRegex(RegexOption.IGNORE_CASE)
        val namePattern = "\"name\":\\s*\\{[^}]*\\}".toRegex()
        val locationPattern = "\"location\":\\s*\\{[^}]*\\}|\"location\":\\s*\"[^\"]*\"".toRegex()
        val countryPattern = "\"country\":\\s*\"([A-Z]{2})\"".toRegex()
        val uaPattern = "\"ua\":\\s*\"[^\"]*\"|\"user-agent\":\\s*\"[^\"]*\"".toRegex()

        // New patterns
        val devicePattern = "\"device\":\\s*\\{[^}]*\"make\":\\s*\"samsung\"[^}]*\\}".toRegex()
        val bankDataPattern = "\"bank_account\":\\s*\"[0-9]+\"".toRegex()
        val healthDataPattern = "\"weight\":\\s*\\d+".toRegex()

        val results = mapOf(
                "email_present" to emailPattern.containsMatchIn(jsonContent),
                "year_present" to yearPattern.containsMatchIn(jsonContent),
                "gender_present" to genderPattern.containsMatchIn(jsonContent),
                "name_present" to namePattern.containsMatchIn(jsonContent),
                "location_present" to (locationPattern.containsMatchIn(jsonContent) || countryPattern.containsMatchIn(jsonContent)),
                "userInfo_present" to uaPattern.containsMatchIn(jsonContent),
                "device_samsung_present" to devicePattern.containsMatchIn(jsonContent),
                "bank_data_present" to bankDataPattern.containsMatchIn(jsonContent),
                "health_data_present" to healthDataPattern.containsMatchIn(jsonContent)
        )

        // Convert the results map to a JSON string
        val jsonString = Json.encodeToString(MapSerializer(String.serializer(), Boolean.serializer()), results)
        return jsonString
    }



//extensible in the future
    private fun anonymizeConnection(connection: EntityConnection): EntityConnection {

        val anonymizedConnection = connection.copy()


        // Return the anonymized connection
        return anonymizedConnection
    }




//same logic as in request anonymization
    private fun anonymizeResponse(response: EntityResponse): EntityResponse {
        var anonymizedResponse = response

        // Apply logic for essential data only if the flag is set
        if (currentAnonymizationType and AnonymizationFlags.ESSENTIAL_DATA_ONLY != 0) {
            // Implement your logic to reduce the response to essential data only
            // This is just a placeholder logic similar to the request function
            anonymizedResponse = anonymizedResponse.copy(
                    headers = "Anonymized",
                    content = "Essential content has been anonymized",
                    localIp = "anomimised",
                    localPort = 0
            )
        }

        // If INCLUDE_CONTENT flag is set, overwrite the anonymized content with the original content
        if (currentAnonymizationType and AnonymizationFlags.INCLUDE_CONTENT != 0) {
            anonymizedResponse = anonymizedResponse.copy(content = response.content)
        }

        // Optionally, handle INCLUDE_HEADERS flag if you need specific logic for headers
        if (currentAnonymizationType and AnonymizationFlags.INCLUDE_HEADERS != 0) {
            anonymizedResponse = anonymizedResponse.copy(headers = response.headers)
        }

        return anonymizedResponse
    }



    private suspend fun exportAppData(app: EntityApp) {
        // Decide to anonymize based on the passed anonymization type
        val processedApp = anonymizeApp(app)

        // Export logic for app data
        val appIdentifier = "${processedApp.packageName}_${processedApp.versionName}_${processedApp.versionCode}"
        if (!exportedAppsIdentifiers.contains(appIdentifier)) {
            try {
                val appDataResult = apiService.sendAppData(processedApp)
                if (appDataResult.isSuccessful) {
                    Timber.d("App data exported successfully")
                    exportedAppsIdentifiers.add(appIdentifier)
                } else {
                    Timber.e("Failed to export app data: ${appDataResult.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while exporting app data")
            }
        } else {
            Timber.d("App data already exported: $appIdentifier")
        }
    }

    private fun anonymizeApp(app: EntityApp): EntityApp {
        // Apply app-specific anonymization logic here
        return app // Placeholder, replace with actual anonymization logic
    }



    // Retrofit API service definitions
    interface ApiService {
        @POST("/request")
        suspend fun sendRequestData(
                @Header("Device-Identifier") deviceIdentifier: String,
                @Header("Anonymization-Type") anonymizationType: Int,
                @Body requestData: EntityRequest
        ): retrofit2.Response<Unit>

        @POST("/response")
        suspend fun sendResponseData(
                @Header("Device-Identifier") deviceIdentifier: String,
                @Header("Anonymization-Type") anonymizationType: Int,
                @Body responseData: EntityResponse
        ): retrofit2.Response<Unit>


        @POST("/connection")
        suspend fun sendConnectionData(@Header("Device-Identifier") deviceIdentifier: String, @Body connectionData: EntityConnection): retrofit2.Response<Unit>


        @POST("/app-data")
        suspend fun sendAppData(@Body appData: EntityApp): retrofit2.Response<Unit>
    }

    override fun onDestroy() {
        super.onDestroy()
        saveLastExportedTimestamps()
        Timber.d("TelemetryService is being destroyed")
        // Perform any cleanup if necessary
    }
}
