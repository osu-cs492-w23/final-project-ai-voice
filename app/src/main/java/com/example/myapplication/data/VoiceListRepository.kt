package com.example.myapplication.data

import com.example.myapplication.VoiceInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VoiceListRepository (
    private val service: VoiceInterface,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) {
        suspend fun loadRepositoriesSearch(): Result<List<Voice>> =
            withContext(dispatcher) {
                try {
                    val response = service.getVoices()
                    if (response.isSuccessful) {
                        Result.success(response.body()?.items() ?: listOf())
                    } else {
                        Result.failure(Exception(response.errorBody()?.string()))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
}