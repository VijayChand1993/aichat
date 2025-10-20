package com.example.aichat


import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.example.aichat.utils.TextProcessorFactory
import java.security.MessageDigest

@SuppressLint("AccessibilityPolicy")
class ChatAccessibilityService : AccessibilityService() {
    private val TAG = "ChatA11yService"

    private val requestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MainActivity.ACTION_REQUEST_CAPTURE) {
                Log.i(TAG, "Received capture request broadcast")
                // perform capture asynchronously
                Handler(Looper.getMainLooper()).post {
                    captureActiveWindowAndSendResult()
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")

        // Register the receiver in a backwards-compatible way
        val filter = IntentFilter(MainActivity.ACTION_REQUEST_CAPTURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(requestReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(requestReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for capture flow, but we keep it for potential future enhancements
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        try { unregisterReceiver(requestReceiver) } catch (e: Exception) {}
        super.onDestroy()
    }

    private fun captureActiveWindowAndSendResult() {
        try {
            val root = rootInActiveWindow
            val appName = getForegroundAppPackageName()
            if (root == null) {
                sendResult("No App Found", "(no root window available)")
                return
            }
            val texts = gatherVisibleText(root)
            val joined = if (texts.isEmpty()) "(no visible text found)" else texts.joinToString("\n\n---\n\n")
            // Log.i(TAG, "Captured text:$joined")
            sendResult(appName, joined)
        } catch (ex: Exception) {
            Log.e(TAG, "Error during capture: ${ex.message}", ex)
            sendResult("No App Found","Error during capture: ${ex.message}")
        }
    }

    private fun sendResult(app: String, text: String) {
        val processor = TextProcessorFactory.getProcessor(app)
        // val processedText = processor?.processText(text) ?: text
        val processedText = text

        val out = Intent(MainActivity.ACTION_CAPTURE_RESULT)
        out.putExtra(MainActivity.EXTRA_CAPTURE_TEXT, processedText)
        out.putExtra(MainActivity.APP_NAME, app)
        out.setPackage(packageName)
        sendBroadcast(out)
    }

    private fun gatherVisibleText(node: AccessibilityNodeInfo?): List<String> {
        val results = mutableListOf<String>()
        if (node == null) return results

        fun traverse(n: AccessibilityNodeInfo?) {
            if (n == null) return

            try {
                // only include nodes visible to user and with text/contentDescription
                val isVisible = try { n.isVisibleToUser } catch (e: Exception) { true }
                if (isVisible) {
                    val t = n.text
                    val d = n.contentDescription
                    if (!t.isNullOrBlank()) results.add(t.toString().trim())
                    else if (!d.isNullOrBlank()) results.add(d.toString().trim())
                }
                val childCount = n.childCount
                for (i in 0 until childCount) {
                    traverse(n.getChild(i))
                }
            } catch (e: Exception) {
                // ignore children access errors
            } finally {
                // Do NOT recycle root node. It's managed by framework.
            }
        }

        traverse(node)
        // simple dedupe and cleanup
        val cleaned = results.map { it.replace("\\s+".toRegex(), " ").trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        return cleaned
    }

    companion object {
        fun isAccessibilityServiceEnabled(ctx: Context): Boolean {
            // quick helper to check if our service is enabled
            val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
            val enabled = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { info -> info.resolveInfo.serviceInfo.packageName == ctx.packageName }
            return enabled
        }
    }

    private fun getForegroundAppPackageName(): String {
        // Get the package name of the foreground app
        return rootInActiveWindow?.packageName?.toString() ?: ""
    }
}
