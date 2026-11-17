package com.goodbp.ui.flow

import android.Manifest
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.goodbp.R
import com.goodbp.capture.PhotoReader
import com.goodbp.data.GoodBpDatabase
import com.goodbp.data.Reading
import com.goodbp.data.Settings
import com.goodbp.family.FamilyAlerts
import com.goodbp.rules.BpResult
import com.goodbp.speech.Speaker
import com.goodbp.speech.VoiceInput
import com.goodbp.ui.theme.Bp
import kotlinx.coroutines.launch
import java.io.File

/** Photo -> (voice backup) -> read-back 对/不对 -> spoken fixed-rule result. */
@Composable
fun ReadingScreen(alerts: FamilyAlerts, onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { Settings(context) }
    val speaker = remember { Speaker(context, settings.language) }
    val voice = remember { VoiceInput(context, settings.language) }
    val photoReader = remember { PhotoReader(context) }
    val db = remember { GoodBpDatabase.get(context) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown(); voice.destroy() } }

    var state by remember { mutableStateOf<FlowState>(FlowState.TakePhoto) }
    val flow = remember {
        ReadingFlow(settings.range()) { sys, dia, pulse, via, result ->
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
    val photoUri: Uri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.files", photoFile)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photoReader.read(photoUri) { flow.photoRead(it); sync() } else { flow.skipPhoto(); sync() }
    }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { mic.launch(Manifest.permission.RECORD_AUDIO) }

    // Strings resolved here so speech uses the current language.
    val sTakePhoto = stringResource(R.string.say_take_photo)
    val sSpeak = stringResource(R.string.say_speak_numbers)
    val sRetry = stringResource(R.string.say_retry)
    val sReadback = stringResource(R.string.say_readback)
    val sNormal = stringResource(R.string.say_result_normal)
    val sHigh = stringResource(R.string.say_result_high)
    val sLow = stringResource(R.string.say_result_low)

    LaunchedEffect(state) {
        when (val s = state) {
            FlowState.TakePhoto -> speaker.sayPrompt("say_take_photo", sTakePhoto)
            is FlowState.SpeakNumbers -> {
                val prompt = if (s.attempt == 1) sSpeak else "$sRetry $sSpeak"
                speaker.say(prompt) {
                    voice.listen(
                        onResult = { flow.voiceHeard(VoiceInput.pickReading(it)); sync() },
                        onError = { flow.voiceHeard(null); sync() },
                    )
                }
            }
            is FlowState.ReadBack -> speaker.say(String.format(sReadback, s.sys, s.dia, s.pulse)) {
                voice.listen(
                    onResult = { c -> VoiceInput.parseYesNo(c, settings.language)?.let { flow.confirm(it); sync() } },
                    onError = { /* buttons stay on screen */ },
                )
            }
            is FlowState.Result -> when (s.result) {
                BpResult.NORMAL -> speaker.sayPrompt("say_result_normal", sNormal)
                BpResult.HIGH -> speaker.sayPrompt("say_result_high", sHigh)
                BpResult.LOW -> speaker.sayPrompt("say_result_low", sLow)
            }
            FlowState.HandOffToFamily -> alerts.needsHelp("voice_failed")
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val s = state) {
            FlowState.TakePhoto -> {
                Big(stringResource(R.string.take_photo)) { camera.launch(photoUri) }
                Big(stringResource(R.string.speak_numbers), Color.DarkGray) { flow.skipPhoto(); sync() }
            }
            is FlowState.SpeakNumbers -> Say(stringResource(R.string.speak_numbers))
            is FlowState.ReadBack -> {
                Say("${s.sys} / ${s.dia}\n♥ ${s.pulse}")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Big(stringResource(R.string.yes), Bp.Normal, Modifier.weight(1f)) { flow.confirm(true); sync() }
                    Big(stringResource(R.string.no), Bp.High, Modifier.weight(1f)) { flow.confirm(false); sync() }
                }
            }
            is FlowState.Result -> {
                val (word, color) = when (s.result) {
                    BpResult.NORMAL -> stringResource(R.string.result_normal) to Bp.Normal
                    BpResult.HIGH -> stringResource(R.string.result_high) to Bp.High
                    BpResult.LOW -> stringResource(R.string.result_low) to Bp.Low
                }
                Text(word, style = MaterialTheme.typography.displayLarge, color = color)
                Say("${s.sys} / ${s.dia}")
                Big(stringResource(R.string.done), onClick = onFinished)
            }
            FlowState.HandOffToFamily -> Big(stringResource(R.string.done), onClick = onFinished)
        }
    }
}

@Composable
private fun Say(text: String) =
    Text(text, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)

@Composable
private fun Big(
    label: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
) = Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(containerColor = color),
    modifier = modifier.height(140.dp),
) { Text(label, style = MaterialTheme.typography.headlineLarge) }
