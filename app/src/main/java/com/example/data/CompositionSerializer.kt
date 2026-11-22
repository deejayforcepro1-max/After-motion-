package com.example.data

import com.example.model.CompositionProject
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object CompositionSerializer {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(CompositionProject::class.java)

    fun toJson(project: CompositionProject): String = adapter.toJson(project)

    fun fromJson(json: String): CompositionProject? = runCatching {
        adapter.fromJson(json)
    }.getOrNull()
}
