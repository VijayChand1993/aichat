package com.example.aichat

import android.annotation.SuppressLint
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var tvResult: TextView
    private lateinit var btnCapture: Button

    // Broadcast action names (must match service)
    companion object {
        const val ACTION_REQUEST_CAPTURE = "com.example.aichat.ACTION_REQUEST_CAPTURE"
        const val ACTION_CAPTURE_RESULT = "com.example.aichat.ACTION_CAPTURE_RESULT"
        const val EXTRA_CAPTURE_TEXT = "capture_text"
    }

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            if (intent.action == ACTION_CAPTURE_RESULT) {
                val text = intent.getStringExtra(EXTRA_CAPTURE_TEXT) ?: "(no text)"
                Log.i(TAG, "Capture result received: $text")
                tvResult.text = text
                Toast.makeText(this@MainActivity, "Captured text received (check app)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        btnCapture = findViewById(R.id.btnCapture)

        btnCapture.setOnClickListener {
            // If accessibility service is not enabled, send user to settings
            if (!ChatAccessibilityService.isAccessibilityServiceEnabled(this)) {
                Toast.makeText(this, "Please enable Accessibility service for this app", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }

            // Move this app to background (minimize current app - this one)
            moveTaskToBack(true)

            var count = 1
            // Give the system a small delay to switch foreground app (adjustable)
            Handler(Looper.getMainLooper()).postDelayed({
                Log.i(TAG, "Called Loop: $count++ ")
                // Send broadcast to AccessibilityService to capture the current foreground window
                val broadcast = Intent(ACTION_REQUEST_CAPTURE)
                broadcast.setPackage(packageName) // ensure only our app receives it
                sendBroadcast(broadcast)
                // Note: response will be via ACTION_CAPTURE_RESULT broadcast
            }, 5000L) // 350ms should be enough; increase if you see misses
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val f = IntentFilter(ACTION_CAPTURE_RESULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ has the flags overload
            registerReceiver(resultReceiver, f, Context.RECEIVER_NOT_EXPORTED)
        } else {
            // older devices: use old overload
            registerReceiver(resultReceiver, f)
        }
    }


    override fun onPause() {
        // try { unregisterReceiver(resultReceiver) } catch (ex: Exception) {}
        super.onPause()
    }
}
