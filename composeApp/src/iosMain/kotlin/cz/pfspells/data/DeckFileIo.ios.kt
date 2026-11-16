package cz.pfspells.data

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

actual val deckFileIo: DeckFileIo = object : DeckFileIo {
    override val saveLabel = "Share"
    override val canOpen = false // import přes schránku

    override suspend fun saveText(fileName: String, text: String): Boolean {
        @Suppress("DEPRECATION")
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
        while (top.presentedViewController != null) top = top.presentedViewController!!
        val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        vc.popoverPresentationController?.sourceView = top.view // nutné na iPadu
        top.presentViewController(vc, animated = true, completion = null)
        return true
    }

    override suspend fun openText(): String? = null
}
