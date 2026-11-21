package com.goodbp.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.data.AppLanguage
import com.goodbp.data.Settings
import com.goodbp.reminders.ReminderScheduler
import com.goodbp.rules.BpRange
import com.goodbp.speech.Speaker
import kotlinx.coroutines.delay

/** Settings tab: profile, reminders, export, language, voice, BP range, phone setup. */
@Composable
fun SettingsTab(onLanguageChanged: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineLarge)
        ProfileSection(settings)
        AlarmSection(settings)
        ExportSection()
        LanguageSection(settings, onLanguageChanged)
        VoiceSection(settings)
        RangeSection(settings)
        PhoneSetupSection()
        PrivacySection()
    }
}

@Composable
private fun ProfileSection(settings: Settings) = Section(stringResource(R.string.profile)) {
    var name by remember { mutableStateOf(settings.name) }
    var age by remember { mutableStateOf(settings.age.toString()) }
    var weight by remember { mutableStateOf(if (settings.weightKg > 0) "%.1f".format(settings.weightKg) else "") }
    var height by remember { mutableStateOf(if (settings.heightCm > 0) settings.heightCm.toString() else "") }
    var saved by remember { mutableStateOf(false) }
    OutlinedTextField(name, { name = it; saved = false }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.name)) }, singleLine = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(age, { age = it.filter(Char::isDigit).take(3); saved = false }, Modifier.weight(1f),
            label = { Text(stringResource(R.string.age)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(weight, { weight = it.filter { c -> c.isDigit() || c == '.' }.take(5); saved = false }, Modifier.weight(1f),
            label = { Text(stringResource(R.string.weight_kg)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(height, { height = it.filter(Char::isDigit).take(3); saved = false }, Modifier.weight(1f),
            label = { Text(stringResource(R.string.height_cm)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
    val w = weight.toFloatOrNull(); val h = height.toIntOrNull()
    if (w != null && w > 0 && h != null && h in 100..230) {
        val bmi = w / ((h / 100f) * (h / 100f))
        Text("BMI %.1f".format(bmi), fontSize = Small)
    }
    Button({
        settings.name = name.trim()
        age.toIntOrNull()?.let { settings.age = it }
        settings.weightKg = weight.toFloatOrNull() ?: 0f
        settings.heightCm = height.toIntOrNull() ?: 0
        saved = true
    }, Modifier.fillMaxWidth()) { Text(stringResource(if (saved) R.string.saved else R.string.save)) }
}

@Composable
private fun ExportSection() = Section(stringResource(R.string.export)) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { com.goodbp.data.GoodBpDatabase.get(context).readings() }
    Text(stringResource(R.string.export_help), fontSize = Small)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ scope.launch { com.goodbp.report.DoctorReport.share(context, dao.since(0)) } }, Modifier.weight(1f)) { Text("PDF") }
        OutlinedButton({ scope.launch { com.goodbp.report.CsvExport.share(context, dao.since(0)) } }, Modifier.weight(1f)) { Text("CSV (Excel)") }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 22.sp, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

private val Small = 17.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSection(settings: Settings, onChanged: () -> Unit) =
    Section(stringResource(R.string.language)) {
        val names = mapOf(AppLanguage.ZH to "中文", AppLanguage.EN to "English", AppLanguage.ID to "Bahasa Indonesia")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { lang ->
                FilterChip(selected = settings.language == lang,
                    onClick = { if (settings.language != lang) { settings.language = lang; onChanged() } },
                    label = { Text(names.getValue(lang)) })
            }
        }
        val ctx = LocalContext.current
        val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
        Text("v$version", fontSize = Small, color = androidx.compose.ui.graphics.Color.Gray)
    }

@Composable
private fun VoiceSection(settings: Settings) = Section(stringResource(R.string.voice)) {
    val context = LocalContext.current
    val speaker = remember { Speaker(context, settings.language) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    var status by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { delay(1500); status = speaker.status }
    val sample = stringResource(R.string.say_readback, 128, 79, 66)
    Text(status, fontSize = 14.sp)
    Button({ speaker.say(sample); status = speaker.status }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.test_voice)) }
    Text(stringResource(R.string.voice_help), fontSize = Small)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ Speaker.openInstallVoiceData(context) }) { Text(stringResource(R.string.install_voice)) }
        OutlinedButton({ Speaker.openGoogleTtsInStore(context) }) { Text(stringResource(R.string.get_google_tts)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmSection(settings: Settings) = Section(stringResource(R.string.reminder_times)) {
    val context = LocalContext.current
    var times by remember { mutableStateOf(settings.bpTimes.sorted()) }
    var picking by remember { mutableStateOf(false) }
    var testSet by remember { mutableStateOf(false) }

    fun save(new: List<Int>) {
        val clean = new.distinct().sorted()
        ReminderScheduler.replaceBp(context, settings.bpTimes, if (settings.remindersOn) clean else emptyList())
        settings.bpTimes = clean
        times = clean
    }

    var on by remember { mutableStateOf(settings.remindersOn) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.reminders_on), fontSize = 20.sp, modifier = Modifier.weight(1f))
        Switch(on, {
            on = it; settings.remindersOn = it
            if (it) ReminderScheduler.scheduleBp(context, times) else ReminderScheduler.replaceBp(context, times, emptyList())
        })
    }
    if (times.isEmpty()) Text(stringResource(R.string.no_reminders), fontSize = Small)
    times.forEach { m ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("%02d:%02d".format(m / 60, m % 60), fontSize = 28.sp, modifier = Modifier.weight(1f))
            TextButton({ save(times - m) }) { Text(stringResource(R.string.remove)) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ picking = true }) { Text(stringResource(R.string.add_time)) }
        OutlinedButton({ ReminderScheduler.scheduleTest(context, 60); testSet = true }) { Text(stringResource(R.string.test_alarm)) }
    }
    if (testSet) Text(stringResource(R.string.test_alarm_set), fontSize = Small)

    if (picking) {
        val state = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton({ save(times + (state.hour * 60 + state.minute)); picking = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton({ picking = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state) },
        )
    }
}

@Composable
private fun RangeSection(settings: Settings) = Section(stringResource(R.string.bp_range)) {
    val r = settings.range()
    var sysLow by remember { mutableStateOf(r.sysLow.toString()) }
    var sysHigh by remember { mutableStateOf(r.sysHigh.toString()) }
    var diaLow by remember { mutableStateOf(r.diaLow.toString()) }
    var diaHigh by remember { mutableStateOf(r.diaHigh.toString()) }
    var source by remember { mutableStateOf(r.source) }
    var error by remember { mutableStateOf(false) }

    Text(stringResource(if (source == BpRange.Source.DOCTOR) R.string.range_from_doctor else R.string.range_placeholder), fontSize = Small)
    Text(stringResource(R.string.sys), fontSize = Small)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumField(stringResource(R.string.low_below), sysLow, { sysLow = it }, Modifier.weight(1f))
        NumField(stringResource(R.string.high_above), sysHigh, { sysHigh = it }, Modifier.weight(1f))
    }
    Text(stringResource(R.string.dia), fontSize = Small)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumField(stringResource(R.string.low_below), diaLow, { diaLow = it }, Modifier.weight(1f))
        NumField(stringResource(R.string.high_above), diaHigh, { diaHigh = it }, Modifier.weight(1f))
    }
    if (error) Text(stringResource(R.string.range_invalid), color = MaterialTheme.colorScheme.error, fontSize = Small)
    Button({
        val v = listOf(sysLow, sysHigh, diaLow, diaHigh).map { it.toIntOrNull() }
        val ok = v.none { it == null } && v[0]!! < v[1]!! && v[2]!! < v[3]!! && v[0]!! in 60..200 && v[3]!! in 40..150
        error = !ok
        if (ok) { settings.setDoctorRange(v[0]!!, v[1]!!, v[2]!!, v[3]!!); source = BpRange.Source.DOCTOR }
    }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_doctor_range)) }
}

@Composable
private fun NumField(label: String, v: String, set: (String) -> Unit, m: Modifier) = OutlinedTextField(
    v, { set(it.filter(Char::isDigit).take(3)) }, m, label = { Text(label) }, singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

@Composable
private fun PhoneSetupSection() = Section(stringResource(R.string.phone_setup)) {
    val context = LocalContext.current
    Text(stringResource(R.string.setup_intro), fontSize = Small)
    val ok = OemSetupGuide.isIgnoringBatteryOptimizations(context)
    Text(stringResource(R.string.setup_battery_status, stringResource(if (ok) R.string.yes_mark else R.string.no_mark)), fontSize = Small)
    Button({ OemSetupGuide.requestIgnoreBatteryOptimizations(context) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.setup_battery_btn)) }
    Button({ OemSetupGuide.openAutoStartSettings(context) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.setup_autostart_btn)) }
    Button({ OemSetupGuide.openNotificationSettings(context) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.setup_notif_btn)) }
    if (android.os.Build.VERSION.SDK_INT >= 34) {
        val full = OemSetupGuide.canUseFullScreen(context)
        Text(stringResource(R.string.setup_fullscreen_status, stringResource(if (full) R.string.yes_mark else R.string.no_mark)), fontSize = Small,
            color = if (full) androidx.compose.ui.graphics.Color.Unspecified else MaterialTheme.colorScheme.error)
        Button({ OemSetupGuide.openFullScreenSettings(context) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.setup_fullscreen_btn)) }
    }
    Text(stringResource(R.string.setup_steps), fontSize = 15.sp)
}

@Composable
private fun PrivacySection() = Section(stringResource(R.string.privacy_policy)) {
    var open by remember { mutableStateOf(false) }
    Text(stringResource(R.string.privacy_summary), fontSize = Small)
    Button({ open = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.privacy_read_full)) }
    if (open) com.goodbp.ui.privacy.PrivacyPolicyDialog { open = false }
}
