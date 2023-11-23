package de.tomcory.heimdall.telemetry

import androidx.datastore.dataStore
import de.tomcory.heimdall.persistence.database.dao.RequestDao
import de.tomcory.heimdall.persistence.database.entity.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect


class TelemetryExport(private val requestDao: RequestDao) {

    fun exportDataToServer() {


        CoroutineScope(Dispatchers.IO).launch {
            requestDao.getAllObservable().collect { listOfRequests ->
                listOfRequests.forEach { request ->
                    println("Request: $request")
                }
            }
        }
    }
}


