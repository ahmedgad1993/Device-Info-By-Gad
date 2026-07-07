package com.deviceinfo.gad

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * Runs a coroutine job only while at least one consumer has called [acquire].
 * Safe for multiple simultaneous consumers; the job is cancelled only when
 * the last consumer calls [release].
 */
class RefCountedMonitor(
    private val scope: CoroutineScope,
    private val onStart: CoroutineScope.() -> Job,
    private val onStop: () -> Unit = {}
) {
    private var refCount = 0
    private var job: Job? = null

    @Synchronized
    fun acquire() {
        refCount++
        if (job?.isActive != true) job = scope.onStart()
    }

    @Synchronized
    fun release() {
        refCount = (refCount - 1).coerceAtLeast(0)
        if (refCount == 0) {
            job?.cancel()
            job = null
            onStop()
        }
    }
}
