package ru.bgtu.schedule.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayInputStream
import java.io.File

fun avatarFile(context: Context): File = File(context.filesDir, "avatar.jpg")

fun libraryDir(context: Context): File = File(context.filesDir, "library").apply { mkdirs() }

fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
    val decoded = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return null
    val rotation = runCatching {
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)
    if (rotation == 0f) return decoded
    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
        if (it != decoded) decoded.recycle()
    }
}

fun saveAvatar(context: Context, bitmap: Bitmap): Boolean {
    return runCatching {
        avatarFile(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    }.isSuccess
}

data class StoredFile(val file: File, val size: Long)

fun listLibrary(context: Context): List<StoredFile> {
    return libraryDir(context).listFiles()
        ?.filter { it.isFile }
        ?.sortedByDescending { it.lastModified() }
        ?.map { StoredFile(it, it.length()) }
        .orEmpty()
}

fun importToLibrary(context: Context, uri: Uri): File? {
    val rawName = displayName(context, uri) ?: "file"
    val safe = rawName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().take(80).ifBlank { "file" }
    val dest = uniqueFile(libraryDir(context), safe)
    val input = context.contentResolver.openInputStream(uri) ?: return null
    input.use { source -> dest.outputStream().use { source.copyTo(it) } }
    return dest
}

fun formatBytes(size: Long): String = when {
    size < 1024 -> "$size Б"
    size < 1024 * 1024 -> "${size / 1024} КБ"
    else -> String.format("%.1f МБ", size / 1048576.0)
}

private fun displayName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return uri.lastPathSegment?.substringAfterLast('/')
}

class FileTags(context: Context) {
    private val sp = context.getSharedPreferences("bgtu_file_tags", Context.MODE_PRIVATE)

    fun lessonKeys(fileName: String): Set<String> = sp.getStringSet(fileKey(fileName), emptySet())?.toSet().orEmpty()

    fun link(fileName: String, lessonKey: String, attached: Boolean) {
        val next = lessonKeys(fileName).toMutableSet()
        if (attached) next += lessonKey else next -= lessonKey
        sp.edit().putStringSet(fileKey(fileName), next).apply()
    }

    fun forget(fileName: String) {
        sp.edit().remove(fileKey(fileName)).apply()
    }

    fun filesFor(context: Context, lessonKey: String): List<StoredFile> {
        return listLibrary(context).filter { lessonKey in lessonKeys(it.file.name) }
    }

    fun has(lessonKey: String): Boolean {
        return sp.all.values.any { value -> value is Set<*> && lessonKey in value }
    }

    private fun fileKey(name: String) = "f:$name"
}

private fun uniqueFile(dir: File, name: String): File {
    val dot = name.lastIndexOf('.')
    val base = if (dot > 0) name.substring(0, dot) else name
    val ext = if (dot > 0) name.substring(dot) else ""
    var file = File(dir, name)
    var index = 2
    while (file.exists()) {
        file = File(dir, "$base ($index)$ext")
        index++
    }
    return file
}
