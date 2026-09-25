package com.movealarm.app.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.movealarm.app.data.PhotoOrder
import com.movealarm.app.data.SettingsStore
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** 사용자가 고른 사진을 앱 내부 저장소(filesDir/photos)에 복사해 보관한다. 원본이 지워져도 유지된다. */
class PhotoStore(context: Context) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "photos").apply { mkdirs() }

    fun list(): List<File> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".jpg") }?.sortedBy { it.name } ?: emptyList()

    /** 원본을 최대 MAX_STORED_PX 로 줄이고 회전을 바로잡아 JPEG로 저장. 성공하면 true. */
    fun import(uri: Uri): Boolean = runCatching {
        val bitmap = decodeFromUri(uri, MAX_STORED_PX)
        val name = "%013d_%04d.jpg".format(System.currentTimeMillis(), (0..9999).random())
        val tmp = File(dir, "$name.tmp")
        tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        tmp.renameTo(File(dir, name))
    }.getOrDefault(false)

    fun delete(file: File) {
        if (file.parentFile == dir) file.delete()
    }

    /** 알람 한 번에 보여줄 사진을 고르고, 다음 순서를 기록한다. 사진이 없으면 null. */
    fun pickForAlarm(settings: SettingsStore, order: PhotoOrder): File? {
        val photos = list()
        if (photos.isEmpty()) return null
        val index = PhotoSelector.choose(order, photos.size, settings.sequentialNextIndex, settings.lastShownIndex)
        settings.lastShownIndex = index
        settings.sequentialNextIndex = index + 1
        return photos[index]
    }

    private fun decodeFromUri(uri: Uri, maxPx: Int): Bitmap {
        val resolver = appContext.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = minOf(1f, maxPx.toFloat() / max(w, h))
                decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxPx)
        }
        val decoded = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("decode failed")
        val rotation = resolver.openInputStream(uri)!!.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
        val scale = minOf(1f, maxPx.toFloat() / max(decoded.width, decoded.height))
        if (rotation == 0f && scale == 1f) return decoded
        val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it !== decoded) decoded.recycle() }
    }

    companion object {
        private const val MAX_STORED_PX = 1440

        fun sampleSizeFor(width: Int, height: Int, maxPx: Int): Int {
            var sample = 1
            while (max(width, height) / (sample * 2) >= maxPx) sample *= 2
            return sample
        }

        /** 저장된 사진 파일을 화면/알림용으로 가볍게 읽어온다. */
        fun decodeFile(file: File, maxPx: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0) return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxPx)
                inPreferredConfig = config
            }
            val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val scale = maxPx.toFloat() / max(bmp.width, bmp.height)
            if (scale >= 1f) return bmp
            return Bitmap.createScaledBitmap(bmp, (bmp.width * scale).roundToInt(), (bmp.height * scale).roundToInt(), true)
                .also { if (it !== bmp) bmp.recycle() }
        }
    }
}
