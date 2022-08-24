package com.avito.emcee.queue

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
public data class JobResultsBody(
    @Json(name = "jobId")
    val id: String
)