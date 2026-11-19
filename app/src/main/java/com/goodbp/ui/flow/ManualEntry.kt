package com.goodbp.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.ui.trend.DiaColor
import com.goodbp.ui.trend.PulseColor
import com.goodbp.ui.trend.SysColor

/**
 * Manual entry laid out like the monitor screen: SYS on top, DIA, then PULSE.
 * Own big keypad (no phone keyboard). Moves to the next row after 3 digits, or on ✓.
 */
@Composable
fun ManualEntry(
    prefill: Triple<Int, Int, Int>?,
    invalid: Boolean,
    photoFailed: Boolean = false,
    onDone: (Triple<Int, Int, Int>) -> Unit,
    onCancel: () -> Unit,
) {
    val values = remember(prefill) {
        mutableStateListOf(prefill?.first?.toString() ?: "", prefill?.second?.toString() ?: "", prefill?.third?.toString() ?: "")
    }
    var active by remember(prefill) { mutableIntStateOf(0) }
    val rows = listOf(
        Triple("SYS", stringResource(R.string.sys), SysColor) to "mmHg",
        Triple("DIA", stringResource(R.string.dia), DiaColor) to "mmHg",
        Triple("PULSE", stringResource(R.string.pulse), PulseColor) to "/min",
    )

    fun submitOrNext() {
        if (active < 2) { active++; return }
        val n = values.map { it.toIntOrNull() }
        val first = n.indexOfFirst { it == null }
        if (first >= 0) active = first else onDone(Triple(n[0]!!, n[1]!!, n[2]!!))
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (photoFailed && prefill == null) Text(stringResource(R.string.photo_unreadable), color = MaterialTheme.colorScheme.error, fontSize = 20.sp)
        if (invalid || (photoFailed && prefill != null)) Text(stringResource(R.string.check_numbers), color = MaterialTheme.colorScheme.error, fontSize = 20.sp)
        rows.forEachIndexed { i, (meta, unit) ->
            val (code, name, color) = meta
            Row(
                Modifier.fillMaxWidth().height(96.dp)
                    .background(if (active == i) color.copy(alpha = 0.10f) else Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                    .border(if (active == i) 4.dp else 1.dp, if (active == i) color else Color.LightGray, RoundedCornerShape(16.dp))
                    .clickable { active = i }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(110.dp)) {
                    Text(code, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                    Text(name, fontSize = 16.sp, color = Color.DarkGray)
                }
                Text(
                    values[i].ifEmpty { "– – –" }, Modifier.weight(1f), fontSize = 60.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    color = if (values[i].isEmpty()) Color.LightGray else Color.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
                Text(unit, fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }
        }
        // Keypad
        val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "✓"))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        val isOk = k == "✓"
                        Button(
                            onClick = {
                                when (k) {
                                    "⌫" -> values[active] = values[active].dropLast(1)
                                    "✓" -> submitOrNext()
                                    else -> if (values[active].length < 3) {
                                        values[active] = values[active] + k
                                        if (values[active].length == 3) submitOrNext()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when { isOk -> Color(0xFF1B7F3B); k == "⌫" -> Color(0xFF9E9E9E); else -> Color(0xFF37474F) }),
                        ) { Text(k, fontSize = 36.sp) }
                    }
                }
            }
        }
        Button(onCancel, Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD), contentColor = Color.Black)) {
            Text(stringResource(R.string.back), fontSize = 24.sp)
        }
    }
}
