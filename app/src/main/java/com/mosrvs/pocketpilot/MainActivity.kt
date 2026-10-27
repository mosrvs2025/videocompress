package com.mosrvs.pocketpilot

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.graphics.Color
import android.widget.*
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Composition
import java.io.File
import java.util.Locale

data class VideoFile(val uri: Uri, val name: String, val bytes: Long)

@androidx.annotation.OptIn(UnstableApi::class)
class MainActivity : Activity() {
    private val pickCode = 110
    private val ui = Handler(Looper.getMainLooper())
    private val videos = mutableListOf<VideoFile>()
    private lateinit var header: TextView
    private lateinit var results: LinearLayout
    private lateinit var command: EditText
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var compressButton: Button
    private var selected: VideoFile? = null
    private var transformer: Transformer? = null
    private var activeFile: File? = null
    private var busy = false
    private var quality = Quality.BALANCED
    private var minimumMb = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 20)
            setBackgroundColor(Color.rgb(13, 21, 29))
        }
        fun label(text: String, sp: Float) = TextView(this).apply {
            this.text = text
            textSize = sp
            setTextColor(Color.WHITE)
            setPadding(0, 7, 0, 8)
        }
        root.addView(label("PocketPilot", 30f))
        root.addView(label("Private video assistant · v0.2", 14f))
        root.addView(label("Commands currently use a local rule engine. Needle 3 is not connected yet.", 12f))
        command = EditText(this).apply {
            hint = "Find videos over 500 MB"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            setSingleLine(false)
            setText("Find my biggest videos")
        }
        root.addView(command)
        root.addView(Button(this).apply {
            text = "Run local command"
            setOnClickListener { runCommand() }
        })
        root.addView(Button(this).apply {
            text = "Select videos"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "video/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                startActivityForResult(intent, pickCode)
            }
        })
        header = label("Select videos to see sizes", 15f)
        root.addView(header)
        val scroller = ScrollView(this)
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroller.addView(results)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        compressButton = Button(this).apply {
            text = "Compress selected · Balanced 720p"
            isEnabled = false
            setOnClickListener { askBeforeCompress() }
        }
        root.addView(compressButton)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; visibility = View.GONE
        }
        root.addView(progress)
        root.addView(Button(this).apply {
            text = "Cancel processing"
            setOnClickListener { cancelExport() }
        })
        status = label("No files modified. Original videos are always kept.", 13f)
        root.addView(status)
        setContentView(root)
    }

    private fun runCommand() {
        when (val action = CommandRouter.parse(command.text.toString())) {
            is Command.Filter -> { minimumMb = action.minimumMb; render() }
            is Command.Compress -> { quality = action.quality; updateCompressButton(); status.text = "Selected preset: ${quality.name}. Choose a video and confirm." }
            Command.Help -> status.text = "Try: 'show videos over 500 MB', 'compress high quality', or 'compress smallest'."
        }
    }

    private fun render() {
        val showing = videos.filter { it.bytes >= minimumMb * 1048576 }.sortedByDescending { it.bytes }
        header.text = "${showing.size} videos · ${fmt(showing.sumOf { it.bytes })} selected · min ${minimumMb} MB"
        results.removeAllViews()
        showing.forEach { video ->
            val button = Button(this).apply {
                text = "${if (video == selected) "✓ " else ""}${video.name}\n${fmt(video.bytes)}"
                setAllCaps(false)
                setOnClickListener { selected = video; render() }
            }
            results.addView(button)
        }
        updateCompressButton()
    }

    private fun updateCompressButton() {
        compressButton.text = "Compress selected · ${quality.name.lowercase().replaceFirstChar { it.uppercase() }} ${quality.height}p"
        compressButton.isEnabled = selected != null && !busy
    }

    private fun askBeforeCompress() {
        val video = selected ?: return
        android.app.AlertDialog.Builder(this)
            .setTitle("Compress ${video.name}?")
            .setMessage("Create a separate MP4 at up to ${quality.height}p. The original remains untouched. Actual output size depends on the source and device encoder.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create copy") { _, _ -> compress(video) }
            .show()
    }

    private fun compress(video: VideoFile) {
        if (busy) return
        busy = true
        updateCompressButton()
        progress.visibility = View.VISIBLE
        progress.isIndeterminate = true
        status.text = "Preparing encoder..."
        val output = File(cacheDir, "pocketpilot-${System.currentTimeMillis()}.mp4")
        activeFile = output
        val edit = EditedMediaItem.Builder(MediaItem.fromUri(video.uri))
            .setEffects(Effects(emptyList(), listOf(Presentation.createForHeight(quality.height))))
            .build()
        transformer = Transformer.Builder(this)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    busy = false
                    progress.visibility = View.GONE
                    updateCompressButton()
                    if (copyToGallery(output, video.name)) {
                        status.text = "Saved compressed copy: ${fmt(output.length())} (original ${fmt(video.bytes)}). Look in Movies/PocketPilot."
                    } else status.text = "Export finished but couldn't save to gallery. Temporary file remains in app cache."
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    busy = false
                    progress.visibility = View.GONE
                    updateCompressButton()
                    status.text = "Compression failed: ${exportException.message}"
                    output.delete()
                }
            }).build()
        try {
            transformer?.start(edit, output.absolutePath)
            pollProgress()
        } catch (t: Exception) {
            busy = false; updateCompressButton()
            progress.visibility = View.GONE
            status.text = "Unable to start: ${t.message}"
            output.delete()
        }
    }

    private fun pollProgress() {
        if (!busy) return
        val holder = androidx.media3.transformer.ProgressHolder()
        val state = transformer?.getProgress(holder)
        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
            progress.isIndeterminate = false
            progress.progress = holder.progress
            status.text = "Compressing: ${holder.progress}%"
        }
        ui.postDelayed({ pollProgress() }, 400)
    }

    private fun copyToGallery(source: File, sourceName: String): Boolean {
        if (!source.exists() || source.length() == 0L) return false
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "PocketPilot-${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/PocketPilot")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            contentResolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
                ?: throw IllegalStateException("No output stream")
            val update = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            contentResolver.update(uri, update, null, null)
            source.delete()
            true
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            false
        }
    }

    private fun cancelExport() {
        if (!busy) { status.text = "Nothing running."; return }
        transformer?.cancel()
        activeFile?.delete()
        busy = false
        progress.visibility = View.GONE
        updateCompressButton()
        status.text = "Compression cancelled. Original unchanged."
    }

    @Deprecated("Simple system picker integration")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickCode || resultCode != RESULT_OK || data == null) return
        val incoming = mutableListOf<Uri>()
        data.clipData?.let { clip -> for (i in 0 until clip.itemCount) incoming.add(clip.getItemAt(i).uri) }
        if (incoming.isEmpty()) data.data?.let(incoming::add)
        incoming.forEach { uri ->
            var name = "video.mp4"; var bytes = 0L
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) ?: name }
                    cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { if (!cursor.isNull(it)) bytes = cursor.getLong(it) }
                }
            }
            videos.removeAll { it.uri == uri }
            videos.add(VideoFile(uri, name, bytes))
        }
        minimumMb = 0
        selected = videos.firstOrNull()
        render()
    }

    private fun fmt(bytes: Long) = String.format(Locale.US, "%.1f MB", bytes.toDouble() / 1048576.0)
}
