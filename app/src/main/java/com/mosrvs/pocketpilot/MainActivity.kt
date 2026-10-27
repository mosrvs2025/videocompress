package com.mosrvs.pocketpilot

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.util.Locale

/** Safe first vertical slice: system file picker + media inventory. No inference or compression yet. */
class MainActivity : Activity() {
    private lateinit var result: TextView
    private val PICK_VIDEOS = 110
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
            setBackgroundColor(Color.rgb(247, 249, 250))
        }
        layout.addView(TextView(this).apply {
            text = "PocketPilot"
            textSize = 30f
            setTextColor(Color.rgb(12, 21, 28))
        })
        layout.addView(TextView(this).apply {
            text = "Your private media assistant — first prototype"
            textSize = 15f
            setPadding(0, 12, 0, 26)
        })
        val command = EditText(this).apply {
            hint = "Try: find videos over 500 MB"
            setSingleLine(false)
            minLines = 2
        }
        layout.addView(command)
        layout.addView(Button(this).apply {
            text = "Choose videos (system picker)"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "video/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                startActivityForResult(intent, PICK_VIDEOS)
            }
        })
        result = TextView(this).apply {
            text = "Selected videos appear here. No files will be modified.\n\nNeedle 3 integration: not connected yet."
            textSize = 15f
            setTextColor(Color.rgb(32, 45, 54))
            setPadding(0, 24, 0, 0)
        }
        val scroll = ScrollView(this)
        scroll.addView(result)
        layout.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(layout)
    }
    @Deprecated("Uses startActivityForResult for dependency-free Android prototype.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_VIDEOS || resultCode != RESULT_OK || data == null) return
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
        }
        if (uris.isEmpty()) data.data?.let { uris.add(it) }
        val videos = uris.map { uri ->
            var name = "Selected video"
            var bytes = 0L
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) ?: name }
                    cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { if (!cursor.isNull(it)) bytes = cursor.getLong(it) }
                }
            }
            name to bytes
        }.sortedByDescending { it.second }
        val total = videos.sumOf { it.second }
        result.text = buildString {
            append("Selected ${videos.size} video(s) · ${fmt(total)} total\n\n")
            videos.forEachIndexed { index, (name, bytes) ->
                append("${index + 1}. $name\n   ${fmt(bytes)}\n\n")
            }
            append("Coming next: real Needle 3 tool routing, compression estimates, and transcoding.\nNo file has been changed.")
        }
    }
    private fun fmt(bytes: Long): String =
        String.format(Locale.US, "%.1f MB", bytes.toDouble() / 1048576.0)
}
