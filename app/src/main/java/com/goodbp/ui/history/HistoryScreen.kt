package com.goodbp.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.goodbp.R
import com.goodbp.data.GoodBpDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Family-facing history. Plain list for now.
 * TODO (report step): Vico line chart + one-tap PDF via [com.goodbp.report.DoctorReport].
 */
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val readings by remember { GoodBpDatabase.get(context).readings().all() }.collectAsState(emptyList())
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineLarge)
        LazyColumn {
            items(readings) { r ->
                Text("${fmt.format(Date(r.takenAt))}  ${r.systolic}/${r.diastolic}  ♥${r.pulse}  ${r.result}",
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
