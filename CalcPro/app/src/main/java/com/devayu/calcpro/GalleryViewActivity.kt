package com.devayu.calcpro

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.github.chrisbanes.photoview.PhotoView // IMPORT ADDED
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class GalleryViewActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var vaultFiles = mutableListOf<VaultManager.VaultFile>()
    private var isLocked = false
    private var isWaitingForAuth = false
    private lateinit var mainBottomBar: View

    private val unlockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isWaitingForAuth = false
        if (result.resultCode == Activity.RESULT_OK) {
            isLocked = false
            // Unlock successful: Show content again
            viewPager.visibility = View.VISIBLE
            mainBottomBar.visibility = View.VISIBLE
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Security Flags
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)

        setContentView(R.layout.activity_gallery_view)

        vaultFiles = VaultManager.getVaultFiles(this).toMutableList()
        val startIndex = intent.getIntExtra("START_INDEX", 0)

        viewPager = findViewById(R.id.viewPager)
        mainBottomBar = findViewById(R.id.bottomBar)

        // Pass the UI Toggle callback
        val adapter = GalleryPagerAdapter(this, vaultFiles) { showUI ->
            toggleMainUI(showUI)
        }

        viewPager.adapter = adapter
        viewPager.setCurrentItem(startIndex, false)

        findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            val pos = viewPager.currentItem
            if (pos in vaultFiles.indices) {
                vaultFiles[pos].encryptedFile.delete()
                vaultFiles[pos].thumbFile.delete()
                vaultFiles.removeAt(pos)
                adapter.notifyItemRemoved(pos)
                if (vaultFiles.isEmpty()) finish()
            }
        }

        findViewById<ImageButton>(R.id.btnUnhide).setOnClickListener {
            val pos = viewPager.currentItem
            if (pos in vaultFiles.indices) unhideFile(vaultFiles[pos])
        }

        findViewById<ImageButton>(R.id.btnShare).setOnClickListener {
            val pos = viewPager.currentItem
            if (pos in vaultFiles.indices) shareFile(vaultFiles[pos])
        }
    }

    // --- FIX: INSTANTLY HIDE CONTENT ON LEAVE ---
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        hideContent()
    }

    override fun onPause() {
        super.onPause()
        // Double protection: Hide on pause too (covers swiping home)
        hideContent()
    }

    private fun hideContent() {
        viewPager.visibility = View.INVISIBLE
        mainBottomBar.visibility = View.INVISIBLE
    }
    // --------------------------------------------

    fun toggleMainUI(show: Boolean) {
        if (show) {
            mainBottomBar.visibility = View.VISIBLE
            mainBottomBar.animate().alpha(1f).setDuration(300).start()
        } else {
            mainBottomBar.animate().alpha(0f).setDuration(300).withEndAction {
                mainBottomBar.visibility = View.GONE
            }.start()
        }
    }

    override fun onResume() {
        super.onResume()
        // Only show content if we are NOT waiting for the password screen
        if (!isWaitingForAuth) {
            viewPager.visibility = View.VISIBLE
            mainBottomBar.visibility = View.VISIBLE
        }
    }

    private fun shareFile(file: VaultManager.VaultFile) {
        val rawDecrypted = VaultManager.getDecryptedFile(this, file.encryptedFile) ?: return
        try {
            val isVideo = file.encryptedFile.name.contains("_vid")
            val ext = if (isVideo) "mp4" else "jpg"
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

            val shareDir = File(cacheDir, "share_temp").apply { mkdirs() }
            val shareFile = File(shareDir, "shared_file.$ext")

            FileInputStream(rawDecrypted).use { input ->
                FileOutputStream(shareFile).use { output -> input.copyTo(output) }
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.provider", shareFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unhideFile(file: VaultManager.VaultFile) {
        val decrypted = VaultManager.getDecryptedFile(this, file.encryptedFile) ?: return
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val ext = if(file.encryptedFile.name.contains("_vid")) "mp4" else "jpg"
        val destFile = File(downloadsDir, "Unhidden_${System.currentTimeMillis()}.$ext")
        try {
            FileInputStream(decrypted).use { input -> FileOutputStream(destFile).use { output -> input.copyTo(output) } }
            Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show()
            file.encryptedFile.delete()
            file.thumbFile.delete()
            val pos = viewPager.currentItem
            vaultFiles.removeAt(pos)
            viewPager.adapter?.notifyItemRemoved(pos)
            if (vaultFiles.isEmpty()) finish()
        } catch (e: Exception) { }
    }

    override fun onStop() { super.onStop(); if (!isWaitingForAuth) isLocked = true }
    override fun onStart() {
        super.onStart()
        if (isLocked && !isWaitingForAuth) {
            isWaitingForAuth = true
            val intent = Intent(this, CalculatorActivity::class.java)
            intent.putExtra("UNLOCK_MODE", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            unlockLauncher.launch(intent)
        }
    }
}

// --- ADAPTER CLASS (Must be outside the Activity Class) ---
class GalleryPagerAdapter(
    val context: AppCompatActivity,
    val files: List<VaultManager.VaultFile>,
    val onUIToggle: (Boolean) -> Unit
) : RecyclerView.Adapter<GalleryPagerAdapter.PagerVH>() {

    class PagerVH(view: View) : RecyclerView.ViewHolder(view) {
        val img: PhotoView = view.findViewById(R.id.imgFull) // CHANGED: Now PhotoView
        val playIcon: ImageView = view.findViewById(R.id.iconPlay)
        val videoView: VideoView = view.findViewById(R.id.videoView)
        val controls: LinearLayout = view.findViewById(R.id.videoControls)
        val btnPlayPause: ImageView = view.findViewById(R.id.btnPlayPause)
        val btnFullscreen: ImageView = view.findViewById(R.id.btnFullscreen)
        val seekBar: SeekBar = view.findViewById(R.id.videoSeekBar)
        val touchOverlay: View = view.findViewById(R.id.touchOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerVH {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_page, parent, false)
        return PagerVH(view)
    }

    override fun onBindViewHolder(holder: PagerVH, position: Int) {
        val item = files[position]
        val isVideo = item.encryptedFile.name.contains("_vid")

        // Reset UI
        holder.videoView.visibility = View.GONE
        holder.controls.visibility = View.GONE
        holder.touchOverlay.visibility = View.GONE
        holder.img.visibility = View.VISIBLE
        holder.playIcon.visibility = if (isVideo) View.VISIBLE else View.GONE
        onUIToggle(true)

        holder.img.setOnClickListener(null)
        holder.img.setOnPhotoTapListener(null) // Clear previous listeners
        holder.touchOverlay.setOnClickListener(null)

        if (isVideo) {
            holder.img.setZoomable(false) // CHANGED: Disable zoom for video thumbs
            holder.img.load(item.thumbFile)
            val startListener = View.OnClickListener { startVideo(holder, item) }
            holder.img.setOnClickListener(startListener)
            holder.playIcon.setOnClickListener(startListener)
        } else {
            holder.img.setZoomable(true) // CHANGED: Enable zoom for images
            val decrypted = VaultManager.getDecryptedFile(context, item.encryptedFile)
            holder.img.load(decrypted ?: item.thumbFile)

            // Optional: Toggle UI if image is tapped (PhotoView consumes standard clicks)
            holder.img.setOnPhotoTapListener { _, _, _ ->
                // You can add logic here if you want to toggle the bottom bar on tap
                // For now, leaving empty to match standard behavior or can call logic if needed
            }
        }
    }

    private fun startVideo(holder: PagerVH, item: VaultManager.VaultFile) {
        val decrypted = VaultManager.getDecryptedFile(context, item.encryptedFile) ?: return

        holder.img.visibility = View.GONE
        holder.playIcon.visibility = View.GONE
        holder.videoView.visibility = View.VISIBLE
        holder.touchOverlay.visibility = View.VISIBLE

        showControls(holder)

        holder.videoView.setVideoURI(Uri.fromFile(decrypted))

        holder.videoView.setOnPreparedListener { mp ->
            holder.seekBar.max = holder.videoView.duration
            holder.videoView.start()
            holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            updateSeekBar(holder)
        }

        holder.videoView.setOnCompletionListener {
            holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            holder.videoView.seekTo(0)
            showControls(holder)
        }

        holder.touchOverlay.setOnClickListener {
            if (holder.controls.visibility == View.VISIBLE) hideControls(holder) else showControls(holder)
        }

        holder.btnPlayPause.setOnClickListener {
            if (holder.videoView.isPlaying) {
                holder.videoView.pause()
                holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                showControls(holder)
                cancelAutoHide()
            } else {
                holder.videoView.start()
                holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                showControls(holder)
                updateSeekBar(holder)
            }
        }

        holder.btnFullscreen.setOnClickListener {
            if (context.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }

        holder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2) holder.videoView.seekTo(p1)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { cancelAutoHide() }
            override fun onStopTrackingTouch(p0: SeekBar?) { showControls(holder) }
        })
    }

    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    private fun showControls(holder: PagerVH) {
        holder.controls.visibility = View.VISIBLE
        onUIToggle(false)
        cancelAutoHide()

        hideRunnable = Runnable {
            if (holder.videoView.isPlaying) hideControls(holder)
        }
        handler.postDelayed(hideRunnable!!, 3000)
    }

    private fun hideControls(holder: PagerVH) {
        holder.controls.visibility = View.GONE
        if (!holder.videoView.isPlaying) onUIToggle(true)
        cancelAutoHide()
    }

    private fun cancelAutoHide() {
        hideRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateSeekBar(holder: PagerVH) {
        val runnable = object : Runnable {
            override fun run() {
                try {
                    if (holder.videoView.isPlaying) {
                        holder.seekBar.progress = holder.videoView.currentPosition
                        handler.postDelayed(this, 1000)
                    }
                } catch (e: Exception) {}
            }
        }
        handler.post(runnable)
    }

    override fun getItemCount() = files.size
}