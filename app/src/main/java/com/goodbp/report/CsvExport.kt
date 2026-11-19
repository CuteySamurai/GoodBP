package com.goodbp.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.goodbp.data.Reading
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** All readings as CSV (opens in Excel / Google Sheets), via the share sheet. */
object CsvExport {
    fun share(context: Context, readings: List<Reading>) {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val text = buildString {
            append("date_time,systolic,diastolic,pulse,result,method\n")
            readings.sortedBy { it.takenAt }.forEach {
                append("${fmt.format(Date(it.takenAt))},${it.systolic},${it.diastolic},${it.pulse},${it.result},${it.via}\n")
            }
        }
        val file = File(context.cacheDir, "bp_readings.csv").apply { writeText("﻿" + text) } // BOM so Excel reads UTF-8
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
