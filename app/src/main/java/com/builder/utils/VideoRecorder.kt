package com.builder.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class VideoRecorder(private val context: Context) {
    private var recording: Recording? = null
    val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()
    val videoCapture = VideoCapture.withOutput(recorder)

    fun startRecording(onVideoSaved: (Uri?) -> Unit) {
        val name = "CamRU_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CamRU-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                if (recordEvent is VideoRecordEvent.Finalize) {
                    val uri = recordEvent.outputResults.outputUri
                    // Paksa galeri scan file baru
                    MediaScannerConnection.scanFile(context, arrayOf(uri.path), null, null)
                    onVideoSaved(uri)
                }
            }
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }
}
