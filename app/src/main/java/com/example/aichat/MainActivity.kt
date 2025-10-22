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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aichat.messageadapter.Message
import com.example.aichat.messageadapter.MessagesAdapter

import com.example.aichat.utils.OpenAIClient



class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var et: EditText
    private lateinit var btnCapture: ImageButton
    private lateinit var adapter: MessagesAdapter
    private val messages = mutableListOf<Message>()

    // Broadcast action names (must match service)
    companion object {
        const val ACTION_REQUEST_CAPTURE = "com.example.aichat.ACTION_REQUEST_CAPTURE"
        const val ACTION_CAPTURE_RESULT = "com.example.aichat.ACTION_CAPTURE_RESULT"
        const val EXTRA_CAPTURE_TEXT = "capture_text"
        const val APP_NAME = "app_name"
    }

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            if (intent.action == ACTION_CAPTURE_RESULT) {
                val text = intent.getStringExtra(EXTRA_CAPTURE_TEXT) ?: "(no text)"
                val app = intent.getStringExtra(APP_NAME) ?: "(no app)"
                Log.i(app, "Capture result received: $text")
                // et.text = text
                et.setText(text)
                bringAppToForeground()
                // Toast.makeText(this@MainActivity, "Captured text received (check app)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bringAppToForeground() {
        val intent = Intent(this@MainActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        et = findViewById(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        btnCapture = findViewById(R.id.btnCapture)

        adapter = MessagesAdapter(messages)
        val rv = findViewById<RecyclerView>(R.id.rvMessages)
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rv.adapter = adapter

        btnSend.setOnClickListener {
            val text = et.text.toString().trim()
            if (text.isNotEmpty()) {
                // add local user message
                val msg = Message(text, isSentByUser = true)
                adapter.addMessage(msg)
                rv.scrollToPosition(adapter.itemCount - 1)
                et.setText("")

                // Call OpenAI with the entered text and append the response to the chat
                val apiKey = BuildConfig.OPENAI_API_KEY
                if (apiKey.isBlank()) {
                    Toast.makeText(this, "OpenAI API key missing. Add OPENAI_API_KEY in local.properties", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Add typing indicator on the left while waiting for response
                val typingMsg = Message("Typing…", isSentByUser = false)
                adapter.addMessage(typingMsg)
                val typingIndex = adapter.itemCount - 1
                rv.scrollToPosition(typingIndex)

                Thread {
                    try {
                        val aiText = OpenAIClient.chatCompletion(text, apiKey)
                        runOnUiThread {
                            adapter.updateMessageText(typingIndex, if (aiText.isNotBlank()) aiText else "(No response)")
                            rv.scrollToPosition(adapter.itemCount - 1)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error calling OpenAI", e)
                        runOnUiThread {
                            adapter.updateMessageText(typingIndex, "(Error: ${e.localizedMessage})")
                            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
        }

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


        // Optional: prefill with sample messages
        adapter.addMessage(Message("Sure, that sounds like a plan! When do you want to meet?", false))
        adapter.addMessage(Message("You could respond with something like: \"Sure...\"", true))
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

