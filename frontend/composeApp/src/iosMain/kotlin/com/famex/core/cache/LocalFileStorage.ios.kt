package com.famex.core.cache

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.posix.FILE
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell

// Files under Library/Caches/<dir> via NSFileManager (dir/delete) + the posix FILE API
// (read/write — Kotlin/Native 1.9.23's Foundation bindings don't expose the NSString/NSData
// convenience class methods this file first reached for, so C stdio is the stable choice).
// Library/Caches is the iOS counterpart of Android filesDir's "can be cleared / not backed up"
// semantics — correct home for data that's a mirror of server state and safe to lose. Ops run on
// Dispatchers.Default so a multi-hundred-KB transaction list never blocks the main thread.
@OptIn(ExperimentalForeignApi::class)
private class IosLocalFileStorage : LocalFileStorage {
    private val cacheDirPath: String
        get() = NSHomeDirectory() + "/Library/Caches/" + DIR_NAME

    private fun filePathFor(key: String): String =
        "$cacheDirPath/${key.replace(Regex("[^A-Za-z0-9._-]"), "_")}"

    override suspend fun readString(key: String): String? = withContext(Dispatchers.Default) {
        ensureCacheDir()
        val path = filePathFor(key)
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) {
            return@withContext null
        }
        readFile(path)
    }

    override suspend fun writeString(key: String, value: String) = withContext(Dispatchers.Default) {
        ensureCacheDir()
        writeFile(filePathFor(key), value)
        Unit
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.Default) {
        ensureCacheDir()
        NSFileManager.defaultManager.removeItemAtPath(filePathFor(key), null)
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        NSFileManager.defaultManager.removeItemAtPath(cacheDirPath, null)
        Unit
    }

    private fun ensureCacheDir() {
        NSFileManager.defaultManager.createDirectoryAtPath(
            cacheDirPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    private fun readFile(path: String): String? = memScoped {
        val file = fopen(path, "r") ?: return@memScoped null
        try {
            if (fseek(file, 0L, SEEK_END) != 0) return@memScoped null
            val size = ftell(file)
            if (size <= 0L) return@memScoped null
            if (fseek(file, 0L, SEEK_SET) != 0) return@memScoped null
            val buffer = allocArray<ByteVar>(size + 1)
            // allocArray zero-initializes, so buffer[size] is already NUL and toKString stops there.
            fread(buffer, size.convert(), 1u.convert(), file)
            buffer.toKString()
        } finally {
            fclose(file)
        }
    }

    private fun writeFile(path: String, contents: String) = memScoped {
        val file = fopen(path, "w") ?: return@memScoped
        try {
            fputs(contents, file)
        } finally {
            fclose(file)
        }
    }

    companion object {
        private const val DIR_NAME = "famex_offline"
    }
}

actual fun createLocalFileStorage(): LocalFileStorage = IosLocalFileStorage()
