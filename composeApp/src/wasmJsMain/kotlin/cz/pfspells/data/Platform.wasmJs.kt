package cz.pfspells.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import kotlinx.coroutines.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.js.Promise

/*
 * Web (Kotlin/Wasm). Velké soubory (stažené databáze, až ~10 MB) se ukládají do IndexedDB –
 * localStorage má limit ~5 MB a ten používají decky a inventáře (multiplatform-settings).
 */

private fun idbGet(key: String): Promise<JsString?> = js(
    """new Promise((resolve, reject) => {
        const open = indexedDB.open('pfspells', 1);
        open.onupgradeneeded = () => open.result.createObjectStore('files');
        open.onerror = () => reject(open.error);
        open.onsuccess = () => {
            const req = open.result.transaction('files', 'readonly').objectStore('files').get(key);
            req.onsuccess = () => resolve(req.result === undefined ? null : req.result);
            req.onerror = () => reject(req.error);
        };
    })"""
)

private fun idbPut(key: String, value: String?): Promise<JsAny?> = js(
    """new Promise((resolve, reject) => {
        const open = indexedDB.open('pfspells', 1);
        open.onupgradeneeded = () => open.result.createObjectStore('files');
        open.onerror = () => reject(open.error);
        open.onsuccess = () => {
            const tx = open.result.transaction('files', 'readwrite');
            const store = tx.objectStore('files');
            if (value === null) store.delete(key); else store.put(value, key);
            tx.oncomplete = () => resolve(null);
            tx.onerror = () => reject(tx.error);
        };
    })"""
)

actual object AppFiles {
    actual suspend fun readText(name: String): String? =
        try { idbGet(name).await<JsString?>()?.toString() } catch (e: Throwable) { null }

    actual suspend fun writeText(name: String, text: String) {
        idbPut(name, text).await<JsAny?>()
    }

    actual suspend fun delete(name: String) {
        try { idbPut(name, null).await<JsAny?>() } catch (e: Throwable) { }
    }
}

actual fun createHttpClient(): HttpClient = HttpClient(Js)

private fun jsNow(): JsString = js(
    """new Date().toLocaleString('cs-CZ', { day: 'numeric', month: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' })"""
)

actual fun nowDisplayText(): String = jsNow().toString()

// ---------- export / import souborů ----------

private fun downloadText(fileName: String, text: String): Unit = js(
    """{
        const blob = new Blob([text], { type: 'application/json' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(a.href), 2000);
    }"""
)

/** Otevře systémový výběr souboru; při zrušení vrátí null (pokud to prohlížeč ohlásí). */
private fun pickTextFile(): Promise<JsString?> = js(
    """new Promise((resolve) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.json,application/json,text/plain';
        input.onchange = () => {
            const f = input.files && input.files[0];
            if (!f) { resolve(null); return; }
            f.text().then((t) => resolve(t), () => resolve(null));
        };
        input.addEventListener('cancel', () => resolve(null));
        input.click();
    })"""
)

actual val deckFileIo: DeckFileIo = object : DeckFileIo {
    override val saveAction = SaveAction.DOWNLOAD
    override val canOpen = true

    override suspend fun saveText(fileName: String, text: String): Boolean {
        downloadText(fileName, text)
        return true
    }

    override suspend fun openText(): String? = pickTextFile().await<JsString?>()?.toString()
}

/** Android má systémové gesto Zpět, desktop a web Esc / tlačítko prohlížeče. */
actual val edgeSwipeBack: Boolean = false

// ---------- analytika (Umami) ----------

/** `umami.track` jen když skript Umami na stránce je (vkládá ho `docker/40-umami.sh`); chyby se ignorují. */
private fun umamiTrack(name: String, json: String?): Unit = js(
    """{
        try {
            const u = window.umami;
            if (u && typeof u.track === 'function') {
                if (json === null) u.track(name); else u.track(name, JSON.parse(json));
            }
        } catch (e) { }
    }"""
)

actual fun trackEvent(name: String, data: Map<String, String>) {
    val json = if (data.isEmpty()) null else JsonObject(data.mapValues { JsonPrimitive(it.value) }).toString()
    umamiTrack(name, json)
}
