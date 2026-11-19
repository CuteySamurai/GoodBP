package com.goodbp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.data.GoodBpDatabase
import com.goodbp.data.Reading
import com.goodbp.data.Settings
import com.goodbp.speech.Speaker
import com.goodbp.ui.common.ReadingCard
import com.goodbp.ui.common.resultStyle
import com.goodbp.ui.common.resultWord
import com.goodbp.ui.common.spoken
import com.goodbp.ui.common.whenLabel

/** Home: latest reading (big), Photo / Manual buttons, then the 3 readings before it. */
@Composable
fun HomeTab(onPhoto: () -> Unit, onManual: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val speaker = remember { Speaker(context, settings.language) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    val readings by remember { GoodBpDatabase.get(context).readings().all() }.collectAsState(emptyList())

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val name = settings.name
        Text(if (name.isBlank()) stringResource(R.string.app_name) else name, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        val last = readings.firstOrNull()
        if (last == null) {
            Text(stringResource(R.string.no_readings), fontSize = 24.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
        } else {
            LatestCard(last, settings.range()) { speaker.say(spoken(context, last)) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onPhoto, Modifier.weight(1.4f).height(140.dp), shape = RoundedCornerShape(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 44.sp)
                    Text(stringResource(R.string.take_photo), fontSize = 26.sp)
                }
            }
            Button(onManual, Modifier.weight(1f).height(140.dp), shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✏️", fontSize = 44.sp)
                    Text(stringResource(R.string.manual), fontSize = 26.sp)
                }
            }
        }

        val earlier = readings.drop(1).take(3)
        if (earlier.isNotEmpty()) {
            Text(stringResource(R.string.recent), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            earlier.forEach { r -> ReadingCard(r, onTap = { speaker.say(spoken(context, r)) }) }
        }
    }
}

@Composable
private fun LatestCard(r: Reading, range: com.goodbp.rules.BpRange, onTap: () -> Unit) {
    val context = LocalContext.current
    val (color, mark) = resultStyle(r.result)
    Column(
        Modifier.fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .clickable(onClick = onTap)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$mark ${resultWord(context, r.result)}", fontSize = 30.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("🔊", fontSize = 32.sp)
        }
        Text(whenLabel(context, r.takenAt), fontSize = 20.sp, color = Color.DarkGray)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text("${r.systolic}", fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text(" / ", fontSize = 48.sp, color = Color.Gray)
            Text("${r.diastolic}", fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text(" mmHg", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 14.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("♥", fontSize = 36.sp, color = Color(0xFFC62828))
            Spacer(Modifier.width(8.dp))
            Text("${r.pulse}", fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(" bpm", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        BpIndicator(r.systolic, r.diastolic, range)
    }
}
