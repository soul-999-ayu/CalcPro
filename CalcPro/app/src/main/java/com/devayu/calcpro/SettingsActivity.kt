package com.devayu.calcpro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// Removed AppCompatDelegate import since we aren't forcing modes anymore

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DELETED: AppCompatDelegate.setDefaultNightMode(...)
        // Now it follows system automatically

        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("VaultPrefs", Context.MODE_PRIVATE)

        val etOldPass = findViewById<EditText>(R.id.etOldPassword)
        val etNewPass = findViewById<EditText>(R.id.etNewPassword)
        val btnSave = findViewById<Button>(R.id.btnSavePassword)

        btnSave.setOnClickListener {
            val oldPass = etOldPass.text.toString()
            val newPass = etNewPass.text.toString()
            val savedPass = prefs.getString("password", "1234")

            if (oldPass == savedPass) {
                if (newPass.isNotEmpty()) {
                    prefs.edit().putString("password", newPass).apply()
                    Toast.makeText(this, "Password Updated!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    etNewPass.error = "Cannot be empty"
                }
            } else {
                etOldPass.error = "Wrong Old Password"
            }
        }

        findViewById<TextView>(R.id.tvCredits).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DevAyu-Codes/")))
        }
    }
}