package com.famex.core.cache

import com.famex.core.persistence.AndroidAppContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// One file per key, under an app-private filesDir subdirectory (never backed up, per
// AndroidAutoBackup defaults for filesDir). Each op hops to Dispatchers.IO — the SharedPreferences
// actual's threading pattern, so file I/O never blocks the main thread.
private class AndroidLocalFileStorage : LocalFileStorage {
    private val cacheDir: File
        get() = File(AndroidAppContext.applicationContext.filesDir, DIR_NAME).apply { mkdirs() }

    private fun fileFor(key: String): File = File(cacheDir, key.replace(Regex("[^A-Za-z0-9._-]"), "_"))

    override suspend fun readString(key: String): String? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        if (file.exists()) file.readText() else null
    }

    override suspend fun writeString(key: String, value: String) = withContext(Dispatchers.IO) {
        fileFor(key).writeText(value)
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        fileFor(key).delete()
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
        Unit
    }

    companion object {
        private const val DIR_NAME = "famex_offline"
    }
}

actual fun createLocalFileStorage(): LocalFileStorage = AndroidLocalFileStorage()
