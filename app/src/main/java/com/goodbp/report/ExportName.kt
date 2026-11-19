package com.goodbp.report

import android.content.Context
import com.goodbp.data.Settings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Export file name: Name_YYYY-MMM-DD_BP.ext, e.g. Mary_2026-Nov-19_BP.pdf */
object ExportName {
    fun of(name: String, now: Date, ext: String): String {
        val clean = name.trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").trim('_').ifEmpty { "Reading" }
        val date = SimpleDateFormat("yyyy-MMM-dd", Locale.US).format(now)
        return "${clean}_${date}_BP.$ext"
    }

    /** A fresh file in cache/export/ (cleared each time so old exports don't pile up). */
    fun file(context: Context, ext: String): File {
        val dir = File(context.cacheDir, "export").apply { deleteRecursively(); mkdirs() }
        return File(dir, of(Settings(context).name, Date(), ext))
    }
}
