package ch.heigvd.iict.daa.labo5.utils

import android.graphics.Bitmap
import java.io.File

/**
 * Extension function to write bitmaps to a file, it uses the JPEG format by default.
 * @param bitmap the bitmap to write
 * @param format the format to use
 * @param quality the quality of the image
 */
fun File.writeBitmap(
    bitmap: Bitmap,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 100
) {
    parentFile?.mkdirs()
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}