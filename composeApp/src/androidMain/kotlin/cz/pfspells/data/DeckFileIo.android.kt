package cz.pfspells.data

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

actual val deckFileIo: DeckFileIo = AndroidDeckFileIo

object AndroidDeckFileIo : DeckFileIo {
    private var activityRef: WeakReference<ComponentActivity>? = null
    private var openLauncher: ActivityResultLauncher<Array<String>>? = null
    private var pending: CompletableDeferred<Uri?>? = null

    /** Volá MainActivity v onCreate (registerForActivityResult musí být před onStart). */
    fun register(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
        openLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pending?.complete(uri)
            pending = null
        }
    }

    override val saveLabel = "Share"
    override val canOpen = true

    override suspend fun saveText(fileName: String, text: String): Boolean {
        val activity = activityRef?.get() ?: return false
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        activity.startActivity(Intent.createChooser(send, "Share deck"))
        return true
    }

    override suspend fun openText(): String? {
        val launcher = openLauncher ?: return null
        pending?.complete(null)
        val result = CompletableDeferred<Uri?>()
        pending = result
        launcher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
        val uri = result.await() ?: return null
        val activity = activityRef?.get() ?: return null
        return withContext(Dispatchers.IO) {
            activity.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }
    }
}
