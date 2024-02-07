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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.zip
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import timber.log.Timber
import java.util.UUID
import de.tomcory.heimdall.Preferences
import de.tomcory.heimdall.ui.main.preferencesStore
import kotlinx.coroutines.flow.first

enum class AnonymizationType {
    FULL, // Indicates full anonymization should be applied
    NONE  // Indicates no anonymization should be applied
}



class TelemetryService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val db: HeimdallDatabase? = HeimdallDatabase.instance

    private var lastExportedRequestTimestamp: Long = 0
    private var lastExportedResponseTimestamp: Long = 0

    private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("http://172.20.10.2:8080/") // Replace with your server's base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private val apiService: ApiService = retrofit.create(ApiService::class.java)

    private val exportedAppsIdentifiers = mutableSetOf<String>()

    private val deviceIdentifier = UUID.randomUUID().toString()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationCompat.Builder(this, HeimdallApplication.CHANNEL_ID)
                .setContentTitle("Telemetry Service")
                .setContentText("Automatically exporting telemetry data...")
                .setSmallIcon(R.drawable.ic_earth)
                .build()

        startForeground(1, notification)
        fetchPreferencesAndObserveData()
    }



    private fun fetchPreferencesAndObserveData() {
        serviceScope.launch {
            applicationContext.preferencesStore.data
                    .map { preferences ->
                        // Map Proto DataStore preferences to local variables
                        Triple(
                                preferences.isFullyAnonymized,
                                preferences.isNoIpTimestamps,
                                preferences.isDisclosedContent
                        )
                    }
                    .collect { (isFullyAnonymized, isNoIpTimestamps, isDisclosedContent) ->
                        // This log statement will be executed every time the preferences change
                        Timber.d("Preferences - Anonymized: $isFullyAnonymized, No IP: $isNoIpTimestamps, Disclosed: $isDisclosedContent")

                        // Call a method to handle the data export with the updated preferences
                        // Make sure this method can handle being called multiple times if that's the desired behavior
//                        observeAndExportData()





                            serviceScope.launch {
                                Timber.d("Starting to collect request data")
                                db?.requestDao?.getAllObservable()?.collect { requests ->
                                    requests.forEach { request ->
                                        exportRequestData(request, AnonymizationType.NONE)
                                    }
                                }
                                Timber.d("Finished collecting request data")
                            }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in request data collection") } }

                            serviceScope.launch {
                                Timber.d("Starting to collect response data")
                                db?.responseDao?.getAllObservable()?.collect { responses ->
                                    responses.forEach { response ->
                                        exportResponseData(response, AnonymizationType.NONE)
                                    }
                                }
                                Timber.d("Finished collecting response data")
                            }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in response data collection") } }

                            serviceScope.launch {
                                Timber.d("Starting to collect app data")
                                db?.appDao?.getAllObservable()?.collect { apps ->
                                    apps.forEach { app ->
                                        exportAppData(app, AnonymizationType.NONE)
                                    }
                                }
                                Timber.d("Finished collecting app data")
                            }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in app data collection") } }
                        }



                    }
        }



    //todo check if it works without
//    private fun observeAndExportData() {
//        serviceScope.launch {
//            Timber.d("Starting to collect request data")
//            db?.requestDao?.getAllObservable()?.collect { requests ->
//                requests.forEach { request ->
//                    exportRequestData(request, AnonymizationType.NONE)
//                }
//            }
//            Timber.d("Finished collecting request data")
//        }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in request data collection") } }
//
//        serviceScope.launch {
//            Timber.d("Starting to collect response data")
//            db?.responseDao?.getAllObservable()?.collect { responses ->
//                responses.forEach { response ->
//                    exportResponseData(response, AnonymizationType.NONE)
//                }
//            }
//            Timber.d("Finished collecting response data")
//        }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in response data collection") } }
//
//        serviceScope.launch {
//            Timber.d("Starting to collect app data")
//            db?.appDao?.getAllObservable()?.collect { apps ->
//                apps.forEach { app ->
//                    exportAppData(app, AnonymizationType.NONE)
//                }
//            }
//            Timber.d("Finished collecting app data")
//        }.invokeOnCompletion { it?.let { error -> Timber.e(error, "Error in app data collection") } }
//    }
//
//


    // Adjusted export function to include anonymization decision
    private suspend fun exportRequestData(request: EntityRequest, anonymizationType: AnonymizationType) {
        // Decide to anonymize based on the passed anonymization type
        val processedRequest = when (anonymizationType) {
            AnonymizationType.FULL -> anonymizeRequest(request)
            AnonymizationType.NONE -> request // No anonymization applied
        }

        // Proceed with sending the processed (potentially anonymized) request
        if (processedRequest.timestamp > lastExportedRequestTimestamp) {
            try {
                val response = apiService.sendRequestData(deviceIdentifier, processedRequest)
                if (response.isSuccessful) {
                    Timber.d("Request data exported successfully")
                    lastExportedRequestTimestamp = processedRequest.timestamp
                } else {
                    Timber.e("Failed to export request data: $deviceIdentifier")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while exporting request data")
            }
        }
    }

    // Anonymize function for requests
    private fun anonymizeRequest(request: EntityRequest): EntityRequest {
        // Implement the actual anonymization logic here
        // For now, return the request as is or modify according to your anonymization strategy
        return request
    }


    private suspend fun exportResponseData(response: EntityResponse, anonymizationType: AnonymizationType) {
        // Decide to anonymize based on the passed anonymization type
        val processedResponse = when (anonymizationType) {
            AnonymizationType.FULL -> anonymizeResponse(response)
            AnonymizationType.NONE -> response // No anonymization applied
        }

        // Export logic for response data
        if (processedResponse.timestamp > lastExportedResponseTimestamp) {
            try {
                val responseResult = apiService.sendResponseData(deviceIdentifier, processedResponse)
                if (responseResult.isSuccessful) {
                    Timber.d("Response data exported successfully")
                    lastExportedResponseTimestamp = processedResponse.timestamp
                } else {
                    Timber.e("Failed to export response data: ${responseResult.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while exporting response data")
            }
        }
    }

    private fun anonymizeResponse(response: EntityResponse): EntityResponse {
        // Apply response-specific anonymization logic here
        return response // Placeholder, replace with actual anonymization logic
    }



    private suspend fun exportAppData(app: EntityApp, anonymizationType: AnonymizationType) {
        // Decide to anonymize based on the passed anonymization type
        val processedApp = when (anonymizationType) {
            AnonymizationType.FULL -> anonymizeApp(app)
            AnonymizationType.NONE -> app // No anonymization applied
        }

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















//    private suspend fun exportResponseData(response: EntityResponse) {
//        if (response.timestamp > lastExportedResponseTimestamp) {
//            try {
//                val responseResult = apiService.sendResponseData(deviceIdentifier, response)
//                if (responseResult.isSuccessful) {
//                    Timber.d("Response data exported successfully")
//                    lastExportedResponseTimestamp = response.timestamp
//                } else {
//                    Timber.e("Failed to export response data: ${responseResult.errorBody()?.string()}")
//                }
//            } catch (e: Exception) {
//                Timber.e(e, "Exception while exporting response data")
//            }
//        }
//    }
//
//    //todo
//
//    private suspend fun exportAppData(app: EntityApp) {
//        val appIdentifier = "${app.packageName}_${app.versionName}_${app.versionCode}"
//
//        if (!exportedAppsIdentifiers.contains(appIdentifier)) {
//            try {
//                val appDataResult = apiService.sendAppData(app)
//                if (appDataResult.isSuccessful) {
//                    Timber.d("App data exported successfully")
//                    exportedAppsIdentifiers.add(appIdentifier)
//                } else {
//                    Timber.e("Failed to export app data: ${appDataResult.errorBody()?.string()}")
//                }
//            } catch (e: Exception) {
//                Timber.e(e, "Exception while exporting app data")
//            }
//        } else {
//            Timber.d("App data already exported: $appIdentifier")
//        }
//    }



    // Retrofit API service definitions
    interface ApiService {
        @POST("/request")
        suspend fun sendRequestData(@Header("Device-Identifier") deviceIdentifier: String, @Body requestData: EntityRequest): retrofit2.Response<Unit>

        @POST("/response")
        suspend fun sendResponseData(@Header("Device-Identifier") deviceIdentifier: String, @Body responseData: EntityResponse): retrofit2.Response<Unit>

        @POST("/app-data")
        suspend fun sendAppData(@Body appData: EntityApp): retrofit2.Response<Unit>
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("TelemetryService is being destroyed")
        // Perform any cleanup if necessary
    }
}
