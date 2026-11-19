package com.goodbp.ui.flow

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.goodbp.R
import com.goodbp.capture.PhotoReader
import com.goodbp.data.GoodBpDatabase
import com.goodbp.data.Reading
import com.goodbp.data.Settings
import com.goodbp.family.FamilyAlerts
import com.goodbp.rules.BpResult
import com.goodbp.speech.Speaker
import com.goodbp.ui.theme.Bp
import kotlinx.coroutines.launch
import java.io.File

/** Photo or manual -> read-back (shown + spoken, 对/不对) -> fixed-rule result. */
@Composable
fun ReadingScreen(alerts: FamilyAlerts, startManual: Boolean, onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { Settings(context) }
    val speaker = remember { Speaker(context, settings.language) }
    val photoReader = remember { PhotoReader(context) }
    val db = remember { GoodBpDatabase.get(context) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    var state by remember { mutableStateOf<FlowState>(if (startManual) FlowState.ManualEntry() else FlowState.TakePhoto) }
    var reading by remember { mutableStateOf(false) } // OCR in progress
    val flow = remember {
        ReadingFlow(settings.range(), start = state) { sys, dia, pulse, via, result ->
            // Only reached after she confirms at read-back.
            scope.launch {
                db.readings().insert(Reading(takenAt = System.currentTimeMillis(), systolic = sys,
                    diastolic = dia, pulse = pulse, via = via.name, result = result.name))
            }
            if (result != BpResult.NORMAL) alerts.readingOutOfRange(sys, dia, pulse, result)
        }
    }
    fun sync() { state = flow.state }

    val photoFile = remember { File(context.cacheDir, "monitor.jpg") }
    val photoUri: Uri = remember { FileProvider.getUriForFile(context, "${context.packageName}.files", photoFile) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) { reading = true; photoReader.read(photoUri) { reading = false; flow.photoRead(it); sync() } }
    }
    fun openCamera() = runCatching { camera.launch(photoUri) }.onFailure { flow.skipPhoto(); sync() }
    // Photo mode opens the camera straight away (once).
    var autoOpened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!startManual && !autoOpened) { autoOpened = true; openCamera() } }

    val sReadback = stringResource(R.string.say_readback)
    val sNormal = stringResource(R.string.say_result_normal)
    val sHigh = stringResource(R.string.say_result_high)
    val sLow = stringResource(R.string.say_result_low)
    LaunchedEffect(state) {
        when (val s = state) {
            is FlowState.ReadBack -> speaker.say(String.format(sReadback, s.sys, s.dia, s.pulse))
            is FlowState.Result -> when (s.result) {
                BpResult.NORMAL -> speaker.sayPrompt("say_result_normal", sNormal)
                BpResult.HIGH -> speaker.sayPrompt("say_result_high", sHigh)
                BpResult.LOW -> speaker.sayPrompt("say_result_low", sLow)
            }
            else -> {}
        }
    }

    when (val s = state) {
        is FlowState.ManualEntry -> ManualEntry(s.prefill, s.invalid, onDone = { flow.manualEntered(it); sync() }, onCancel = onFinished)
        else -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (s) {
                FlowState.TakePhoto -> {
                    if (reading) Text(stringResource(R.string.reading_photo), fontSize = 32.sp)
                    else {
                        Big("📷  " + stringResource(R.string.take_photo)) { openCamera() }
                        Big("✏️  " + stringResource(R.string.manual), Color(0xFF455A64)) { flow.skipPhoto(); sync() }
                        Big(stringResource(R.string.back), Color(0xFFBDBDBD), height = 80) { onFinished() }
                    }
                }
                is FlowState.ReadBack -> {
                    Text(stringResource(R.string.is_this_right), fontSize = 30.sp)
                    Numbers(s.sys, s.dia, s.pulse)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Big("✓ " + stringResource(R.string.yes), Bp.Normal, Modifier.weight(1f)) { flow.confirm(true); sync() }
                        Big("✗ " + stringResource(R.string.no), Bp.High, Modifier.weight(1f)) { flow.confirm(false); sync() }
                    }
                }
                is FlowState.Result -> {
                    val (word, color) = when (s.result) {
                        BpResult.NORMAL -> stringResource(R.string.result_normal) to Bp.Normal
                        BpResult.HIGH -> stringResource(R.string.result_high) to Bp.High
                        BpResult.LOW -> stringResource(R.string.result_low) to Bp.Low
                    }
                    Text(word, style = MaterialTheme.typography.displayLarge, color = color)
                    Numbers(s.sys, s.dia, s.pulse)
                    com.goodbp.ui.home.BpIndicator(s.sys, s.dia, settings.range())
                    Big(stringResource(R.string.done), onClick = onFinished)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun Numbers(sys: Int, dia: Int, pulse: Int) = Column(horizontalAlignment = Alignment.End) {
    listOf("SYS" to sys, "DIA" to dia, "PULSE" to pulse).forEach { (l, v) ->
        Row(verticalAlignment = Alignment.Bottom) {
            Text(l, fontSize = 22.sp, color = Color.Gray, modifier = Modifier.padding(end = 16.dp, bottom = 10.dp))
            Text("$v", fontSize = 64.sp, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun Big(
    label: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier.fillMaxWidth(),
    height: Int = 130,
    onClick: () -> Unit,
) = Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(containerColor = color),
    modifier = modifier.height(height.dp),
) { Text(label, style = MaterialTheme.typography.headlineLarge) }
