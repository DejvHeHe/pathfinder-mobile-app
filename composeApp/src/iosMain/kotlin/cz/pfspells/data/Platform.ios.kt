package cz.pfspells.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object AppFiles {
    private val dir: String by lazy {
        NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
    }

    private fun path(name: String) = "$dir/$name"

    actual suspend fun readText(name: String): String? {
        val p = path(name)
        if (!NSFileManager.defaultManager.fileExistsAtPath(p)) return null
        return NSString.create(contentsOfFile = p, encoding = NSUTF8StringEncoding, error = null)?.toString()
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    actual suspend fun writeText(name: String, text: String) {
        (text as NSString).writeToFile(path(name), atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual suspend fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath(path(name), error = null)
    }
}

actual fun createHttpClient(): HttpClient = HttpClient(Darwin)

actual fun nowDisplayText(): String {
    val f = NSDateFormatter()
    f.dateFormat = "d. M. yyyy HH:mm"
    return f.stringFromDate(NSDate())
}
