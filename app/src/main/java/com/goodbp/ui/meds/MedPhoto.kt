package com.goodbp.ui.meds

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import java.io.File

/** The family's photo of the pill or box (she recognises it by sight), or a 💊. */
@Composable
fun MedPhoto(path: String?, size: Dp, version: Int = 0) {
    val bmp = remember(path, version) { path?.let { decodeThumb(it, 400) } }
    if (bmp != null) {
        Image(bmp.asImageBitmap(), null, Modifier.size(size).clip(RoundedCornerShape(14)), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(size).background(com.goodbp.ui.theme.Palette.MintTint, RoundedCornerShape(14)), contentAlignment = Alignment.Center) {
            Text("💊", fontSize = (size.value / 2.2f).sp)
        }
    }
}

fun decodeThumb(path: String, target: Int): Bitmap? = runCatching {
    if (!File(path).exists()) return null
    val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, b)
    var s = 1
    while (maxOf(b.outWidth, b.outHeight) / (s * 2) >= target) s *= 2
    val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s }) ?: return null
    val deg = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (deg == 0f) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
}.getOrNull()
