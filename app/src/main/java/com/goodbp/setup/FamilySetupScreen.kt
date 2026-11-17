package com.goodbp.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.data.Settings

/**
 * Family-only setup (long-press top-right corner of home). Normal-size text, it's for the caregiver.
 * TODO: editors for reminder times, doctor's range, age, language, medicines, family link.
 */
@Composable
fun FamilySetupScreen(onHistory: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val range = settings.range()
    val small = TextStyle(fontSize = 18.sp)
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.family_settings), style = MaterialTheme.typography.headlineLarge)
        Text("Range (${range.source}): SYS ${range.sysLow}–${range.sysHigh}, DIA ${range.diaLow}–${range.diaHigh}", style = small)
        Text("Reminders: " + settings.bpTimes.joinToString { "%02d:%02d".format(it / 60, it % 60) }, style = small)
        Text("Battery optimisation off: ${OemSetupGuide.isIgnoringBatteryOptimizations(context)}", style = small)
        Button({ OemSetupGuide.requestIgnoreBatteryOptimizations(context) }, Modifier.fillMaxWidth()) { Text("1. 电池：不优化") }
        Button({ OemSetupGuide.openAutoStartSettings(context) }, Modifier.fillMaxWidth()) { Text("2. 允许自动启动") }
        Text(OemSetupGuide.MANUAL_STEPS_ZH, style = small)
        OutlinedButton(onHistory, Modifier.fillMaxWidth()) { Text(stringResource(R.string.history)) }
    }
}
