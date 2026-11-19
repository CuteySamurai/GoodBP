package com.goodbp.report

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.goodbp.data.Reading
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One-tap PDF for the doctor. Skeleton: a plain table of readings.
 * TODO (report step): chart image, medicine confirmations side by side, sharing.
 */
object DoctorReport {
    /** Writes the PDF and opens the share sheet (WhatsApp, email, print...). */
    fun share(context: Context, readings: List<Reading>) {
        val file = write(context, readings.sortedBy { it.takenAt })
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = android.content.Intent(android.content.Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(android.content.Intent.EXTRA_STREAM, uri)
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(android.content.Intent.createChooser(send, null)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun write(context: Context, readings: List<Reading>): File {
        val doc = PdfDocument()
        val paint = Paint().apply { textSize = 11f }
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        var y = 40f
        page.canvas.drawText("Blood pressure log / 血压记录", 40f, y, paint.apply { textSize = 16f }); paint.textSize = 11f
        y += 30f
        readings.forEachIndexed { i, r ->
            if (y > 800f) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, i + 2).create()); y = 40f
            }
            page.canvas.drawText("${fmt.format(Date(r.takenAt))}   ${r.systolic}/${r.diastolic}   pulse ${r.pulse}   ${r.result}", 40f, y, paint)
            y += 16f
        }
        doc.finishPage(page)
        val out = ExportName.file(context, "pdf")
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
        return out
    }
}
