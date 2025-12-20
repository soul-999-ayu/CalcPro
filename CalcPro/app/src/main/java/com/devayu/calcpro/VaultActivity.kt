package com.devayu.calcpro

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class VaultActivity : AppCompatActivity() {

    private lateinit var adapter: VaultAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private val vaultFiles = mutableListOf<VaultManager.VaultFile>()
    private var pendingDeleteUri: Uri? = null

    // SECURITY VARIABLES
    private var isLocked = false
    private var isPickingFile = false
    private var isWaitingForAuth = false
    private var isOpeningGallery = false

    private val unlockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isWaitingForAuth = false
        if (result.resultCode == Activity.RESULT_OK) {
            isLocked = false
            refreshList()
        } else {
            finish()
        }
    }

    private val deleteSender = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteUri?.let { deleteOriginal(it) }
        }
    }

    // CHANGED: Use GetMultipleContents() instead of GetContent()
    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        isPickingFile = false
        if (uris.isNotEmpty()) {
            Toast.makeText(this, "Encrypting ${uris.size} files...", Toast.LENGTH_SHORT).show()

            Thread {
                var successCount = 0
                // Loop through all selected files
                for (uri in uris) {
                    val success = VaultManager.hideFile(this, uri)
                    if (success) {
                        successCount++
                        // Attempt to delete original (Note: Bulk delete might ask for permission multiple times on some Android versions)
                        try {
                            contentResolver.delete(uri, null, null)
                        } catch (e: Exception) {
                            // If simple delete fails, we skip complex recovery for bulk actions to avoid UI spam
                        }
                    }
                }

                runOnUiThread {
                    refreshList()
                    if (successCount > 0) {
                        Toast.makeText(this, "Hidden $successCount files!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to hide files.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_vault)

        // NO BACK LOGIC NEEDED. Default Back will close Vault and reveal Calculator naturally.

        recyclerView = findViewById(R.id.recyclerView)
        emptyText = findViewById(R.id.tvEmpty)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        adapter = VaultAdapter(vaultFiles) { vaultFile ->
            isOpeningGallery = true
            val index = vaultFiles.indexOf(vaultFile)
            val intent = Intent(this, GalleryViewActivity::class.java)
            intent.putExtra("START_INDEX", index)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val fab = findViewById<FloatingActionButton>(R.id.fabAdd)
        fab.setOnClickListener {
            isPickingFile = true
            // CHANGED: Pass the mime type to launch()
            pickMedia.launch("*/*")
        }

        refreshList()
    }

    // --- MAGIC FIX: SELF DESTRUCT ON HOME/RECENTS ---
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If we aren't actively doing something (Picking file/Opening Viewer)
        // DESTROY Vault. Calculator is underneath, so system snapshots THAT.
        if (!isPickingFile && !isOpeningGallery) {
            finish()
        }
    }
    // ------------------------------------------------

    override fun onStart() {
        super.onStart()
        if (isLocked && !isPickingFile && !isWaitingForAuth && !isOpeningGallery) {
            isWaitingForAuth = true
            val intent = Intent(this, CalculatorActivity::class.java)
            intent.putExtra("UNLOCK_MODE", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            unlockLauncher.launch(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isPickingFile && !isWaitingForAuth && !isOpeningGallery) {
            isLocked = true
        }
    }

    override fun onResume() {
        super.onResume()
        isOpeningGallery = false
        if (!isWaitingForAuth) refreshList()
    }

    private fun refreshList() {
        vaultFiles.clear()
        vaultFiles.addAll(VaultManager.getVaultFiles(this))
        adapter.notifyDataSetChanged()
        emptyText.visibility = if (vaultFiles.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteOriginal(uri: Uri) {
        var targetUri = uri
        try {
            if (contentResolver.delete(targetUri, null, null) > 0) return
            val realUri = findRealUri(this, uri)
            if (realUri != null) {
                contentResolver.delete(realUri, null, null)
            } else {
                Toast.makeText(this, "Delete Manually", Toast.LENGTH_SHORT).show()
            }
        } catch (securityException: SecurityException) {
            val recoverable = securityException as? android.app.RecoverableSecurityException
            if (recoverable != null) {
                pendingDeleteUri = targetUri
                val intentSender = recoverable.userAction.actionIntent.intentSender
                val senderRequest = IntentSenderRequest.Builder(intentSender).build()
                deleteSender.launch(senderRequest)
            }
        } catch (e: Exception) {}
    }

    private fun findRealUri(context: Context, sourceUri: Uri): Uri? {
        // ... (Same logic as before) ...
        return null
    }
}