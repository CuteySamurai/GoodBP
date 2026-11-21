package com.goodbp.ui.meds

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.goodbp.R
import com.goodbp.data.GoodBpDatabase
import com.goodbp.data.Medicine
import com.goodbp.meds.MedSchedule
import kotlinx.coroutines.launch
import java.io.File

/** Family screen: name, how to describe it aloud, a photo of the pill or box, and times. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedEditor(original: Medicine, onClose: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { GoodBpDatabase.get(context).medicines() }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(original.name) }
    var desc by remember { mutableStateOf(original.spokenDescription) }
    var photo by remember { mutableStateOf(original.pillPhotoPath) }
    var photoVersion by remember { mutableIntStateOf(0) }
    var times by remember { mutableStateOf(MedSchedule.parse(original.timesOfDay)) }
    var picking by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    // New photo goes to a new file; the old one is removed only when saved.
    val newFile = remember { File(File(context.filesDir, "meds").apply { mkdirs() }, "med_${System.currentTimeMillis()}.jpg") }
    val newUri: Uri = remember { FileProvider.getUriForFile(context, "${context.packageName}.files", newFile) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && newFile.exists()) { photo = newFile.path; photoVersion++ }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(if (original.id == 0L) R.string.med_add else R.string.med_edit), style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.med_name)) }, singleLine = true)
                OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.med_desc)) })

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MedPhoto(photo, 110.dp, photoVersion)
                    OutlinedButton({ runCatching { camera.launch(newUri) } }) { Text("📷 " + stringResource(R.string.med_photo)) }
                }

                Text(stringResource(R.string.med_times), fontSize = 20.sp)
                times.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(MedSchedule.hhmm(m), fontSize = 28.sp, modifier = Modifier.weight(1f))
                        TextButton({ times = times - m }) { Text(stringResource(R.string.remove)) }
                    }
                }
                OutlinedButton({ picking = true }) { Text(stringResource(R.string.add_time)) }

                if (error) Text(stringResource(R.string.med_need_name_time), color = MaterialTheme.colorScheme.error)
                Button({
                    if (name.isBlank() || times.isEmpty()) { error = true; return@Button }
                    val updated = original.copy(name = name.trim(), spokenDescription = desc.trim(), pillPhotoPath = photo,
                        timesOfDay = MedSchedule.format(times))
                    scope.launch {
                        if (original.id == 0L) {
                            val id = dao.insert(updated)
                            MedSchedule.schedule(context, updated.copy(id = id))
                        } else {
                            MedSchedule.cancel(context, original)
                            dao.update(updated)
                            MedSchedule.schedule(context, updated)
                            if (original.pillPhotoPath != null && original.pillPhotoPath != photo) File(original.pillPhotoPath).delete()
                        }
                        onClose()
                    }
                }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.med_save), fontSize = 20.sp) }
                OutlinedButton({
                    if (photo != original.pillPhotoPath) newFile.delete()
                    onClose()
                }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
                if (original.id != 0L) TextButton({ askDelete = true }, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.med_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (picking) {
        val state = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton({ times = (times + (state.hour * 60 + state.minute)).distinct().sorted(); picking = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton({ picking = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state) },
        )
    }
    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            confirmButton = {
                TextButton({
                    scope.launch {
                        MedSchedule.cancel(context, original)
                        dao.delete(original.id)
                        original.pillPhotoPath?.let { File(it).delete() }
                        askDelete = false
                        onClose()
                    }
                }) { Text(stringResource(R.string.med_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ askDelete = false }) { Text(stringResource(R.string.cancel)) } },
            text = { Text(stringResource(R.string.med_delete_q), fontSize = 18.sp) },
        )
    }
}
