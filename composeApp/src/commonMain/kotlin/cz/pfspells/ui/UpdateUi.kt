package cz.pfspells.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cz.pfspells.data.SpellDatabase
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.APP_VERSION
import cz.pfspells.AppState
import cz.pfspells.IS_RELEASE_BUILD
import cz.pfspells.Strings
import cz.pfspells.UpdateState
import cz.pfspells.data.UpdateInfo
import cz.pfspells.data.appUpdater
import cz.pfspells.str

/** Tlačítko podle platformy: Android stáhne a nainstaluje APK, desktop stáhne instalátor v prohlížeči. */
private fun installLabel(s: Strings, info: UpdateInfo): String = when {
    info.asset == null -> s.releaseNotes
    ".apk" in appUpdater.assetExtensions -> s.downloadAndInstall
    else -> s.downloadInstaller
}

/** Sekce „Aktualizace appky“ na obrazovce Databáze – jen na platformách, které aktualizace nabízejí (Android, desktop). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppUpdateSection(state: AppState) {
    if (!state.updatesSupported) return
    val s = str
    val fonts = LocalNbFonts.current
    SectionHeader(s.appUpdatesHeader, size = 22.sp)
    NbPanel(Modifier.fillMaxWidth(), color = Nb.inset) {
        Text(
            s.installedVersion(APP_VERSION),
            fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Nb.ink,
        )
        if (!IS_RELEASE_BUILD) Text(s.devBuild, fontSize = 13.sp, color = Nb.inkMuted)
    }
    when (val u = state.update) {
        UpdateState.Idle, UpdateState.UpToDate, is UpdateState.Failed -> {
            NbPrimaryButton(s.checkForUpdates, { state.checkForUpdates() }, icon = NbIcons.Refresh)
            when (u) {
                UpdateState.UpToDate -> Text(s.upToDate, color = Nb.accent, fontSize = 14.sp)
                is UpdateState.Failed -> Text(s.updateFailed(u.message), color = Nb.error, fontSize = 14.sp)
                else -> {}
            }
        }
        UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Nb.accent, strokeWidth = 2.dp)
            Text("  " + s.checkingUpdates, color = Nb.ink)
        }
        is UpdateState.Available, is UpdateState.ReadyToInstall -> {
            val info = if (u is UpdateState.Available) u.info else (u as UpdateState.ReadyToInstall).info
            Text(s.updateAvailable(info.version), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Nb.accent)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NbPrimaryButton(installLabel(s, info), { state.installUpdate(info) }, icon = NbIcons.Refresh)
                if (info.asset != null) NbDashedButton(s.releaseNotes, { state.openReleasePage(info) })
            }
        }
        is UpdateState.Downloading -> {
            Text(s.updateDownloading(u.progress?.let { (it * 100).toInt() }), color = Nb.ink)
            val p = u.progress
            if (p != null) LinearProgressIndicator(progress = { p }, Modifier.fillMaxWidth(), color = Nb.accent, trackColor = Nb.inset)
            else LinearProgressIndicator(Modifier.fillMaxWidth(), color = Nb.accent, trackColor = Nb.inset)
        }
    }
    Text(
        if (".apk" in appUpdater.assetExtensions) s.updateHintApk else s.updateHintInstaller,
        fontSize = 13.sp, color = Nb.inkMuted,
    )
}

/** Banner po automatické kontrole: nová verze → stáhnout / co je nového / později / přeskočit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpdateBanner(state: AppState, modifier: Modifier = Modifier) {
    val info = state.updateBanner ?: return
    val s = str
    NbPanel(modifier.widthIn(max = 520.dp)) {
        Text(s.updateAvailable(info.version), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink)
        FlowRow(
            Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NbPrimaryButton(installLabel(s, info), { state.installUpdate(info) }, icon = null, height = 38.dp)
            if (info.asset != null) TextButton(onClick = { state.openReleasePage(info) }) { Text(s.releaseNotes, color = Nb.accent) }
            TextButton(onClick = { state.dismissUpdateBanner(skip = false) }) { Text(s.updateLater, color = Nb.ink) }
            TextButton(onClick = { state.dismissUpdateBanner(skip = true) }) { Text(s.updateSkip, color = Nb.inkMuted) }
        }
    }
}

/**
 * Dotaz před instalací (po stažení APK / před stažením instalátoru): nabídne export postav a inventářů.
 * Když podpis staženého APK nesedí s nainstalovanou verzí, Android ho přes ni nenainstaluje – pak důrazné varování
 * s postupem (export → stažení APK v prohlížeči → odinstalace → instalace → import) místo tlačítka Instalovat.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpdateConfirmDialog(state: AppState, db: SpellDatabase) {
    val ready = state.update as? UpdateState.ReadyToInstall ?: return
    val s = str
    val mismatch = ready.prepared.signatureMatches == false
    var exportCharacters by remember { mutableStateOf(false) }
    var exportInventories by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { state.cancelInstall() },
        containerColor = Nb.panel,
        title = { Text(s.updateConfirmTitle(ready.info.version), color = Nb.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (mismatch) s.updateSignatureMismatch else s.updateBackupHint,
                    color = if (mismatch) Nb.error else Nb.ink, fontSize = 14.sp, lineHeight = 20.sp,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.characters.isNotEmpty()) NbDashedButton(s.exportAllCharacters, { exportCharacters = true })
                    if (state.inventories.isNotEmpty()) NbDashedButton(s.exportAllInventories, { exportInventories = true })
                }
            }
        },
        confirmButton = {
            if (mismatch) NbPrimaryButton(s.downloadApkInBrowser, { state.openInBrowser(ready.prepared.asset.downloadUrl) }, icon = null, height = 40.dp)
            else NbPrimaryButton(if (".apk" in appUpdater.assetExtensions) s.installNow else s.downloadInstaller, { state.confirmInstall() }, icon = null, height = 40.dp)
        },
        dismissButton = { TextButton(onClick = { state.cancelInstall() }) { Text(s.cancel, color = Nb.ink) } },
    )
    if (exportCharacters) ExportDialog(state, state.characters.toList(), db) { exportCharacters = false }
    if (exportInventories) InventoryExportDialog(state.inventories.toList()) { exportInventories = false }
}
