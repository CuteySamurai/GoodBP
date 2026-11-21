package com.goodbp.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goodbp.R
import com.goodbp.data.Settings

/**
 * One-time privacy notice on first launch. The box must be ticked before Agree works,
 * and the dialog can't be dismissed with Back or an outside tap.
 */
@Composable
fun PrivacyConsentDialog(onAgree: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var ticked by remember { mutableStateOf(false) }
    var showFull by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.privacy_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.privacy_summary), fontSize = 18.sp, lineHeight = 26.sp)
                TextButton({ showFull = true }) { Text(stringResource(R.string.privacy_read_full), fontSize = 17.sp) }
                Row(
                    Modifier.fillMaxWidth().toggleable(ticked, role = Role.Checkbox) { ticked = it },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(ticked, onCheckedChange = null)
                    Text(stringResource(R.string.privacy_tick), fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button({ settings.privacyAccepted = true; onAgree() }, enabled = ticked) {
                Text(stringResource(R.string.privacy_agree), fontSize = 20.sp)
            }
        },
    )
    if (showFull) PrivacyPolicyDialog { showFull = false }
}

/** Full policy, bundled in the app (it has no internet permission). */
@Composable
fun PrivacyPolicyDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text(stringResource(R.string.privacy_policy), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.privacy_full),
                    fontSize = 18.sp, lineHeight = 26.sp,
                    modifier = Modifier.weight(1f).padding(vertical = 16.dp).verticalScroll(rememberScrollState()),
                )
                Button(onClose, Modifier.fillMaxWidth().height(64.dp)) { Text(stringResource(R.string.ok), fontSize = 22.sp) }
            }
        }
    }
}
