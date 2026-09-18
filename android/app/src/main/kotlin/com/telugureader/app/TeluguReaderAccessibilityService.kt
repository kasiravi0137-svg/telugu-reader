package com.telugureader.app

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import java.util.Locale
import java.util.UUID

class TeluguReaderAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var bubble: ImageView
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val handler = Handler(Looper.getMainLooper())
    private var isReading = false
    private var lastReadText: String = ""
    private var consecutiveNoNewContent = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        tts = TextToSpeech(this, this)
        setupBubble()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val teluguLocale = Locale("te", "IN")
            val result = tts?.setLanguage(teluguLocale)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setSpeechRate(1.0f)
        }
    }

    // ---------- Floating bubble ----------

    private fun setupBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubble = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setBackgroundColor(0x33000000)
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        bubbleParams = WindowManager.LayoutParams(
            140, 140,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 800
            y = 600
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var isClick = true

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = bubbleParams.x
                    startY = bubbleParams.y
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > 12 || kotlin.math.abs(dy) > 12) isClick = false
                    bubbleParams.x = startX + dx
                    bubbleParams.y = startY + dy
                    windowManager.updateViewLayout(bubble, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) onBubbleTapped()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubble, bubbleParams)
    }

    private fun onBubbleTapped() {
        if (isReading) {
            stopReading()
        } else {
            consecutiveNoNewContent = 0
            lastReadText = ""
            readCurrentScreen()
        }
    }

    private fun stopReading() {
        isReading = false
        tts?.stop()
        bubble.setBackgroundColor(0x33000000)
    }

    // ---------- Reading + auto-scroll ----------

    private fun readCurrentScreen() {
        val root = rootInActiveWindow ?: return
        val text = extractVisibleText(root)
        if (text.isBlank()) return

        if (text == lastReadText) {
            consecutiveNoNewContent++
        } else {
            consecutiveNoNewContent = 0
        }

        if (consecutiveNoNewContent >= 2) {
            stopReading()
            return
        }

        isReading = true
        bubble.setBackgroundColor(0x66FF6B35)
        lastReadText = text
        speak(text)
    }

    private fun speak(text: String) {
        val id = UUID.randomUUID().toString()
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                handler.post {
                    if (isReading) attemptAutoScrollThenContinue()
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handler.post { if (isReading) attemptAutoScrollThenContinue() }
            }
        })
        // Split long text into TTS-safe chunks (~3800 chars) to avoid platform limits.
        val chunks = text.chunked(3800)
        for ((index, chunk) in chunks.withIndex()) {
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(chunk, queueMode, null, "$id-$index")
        }
    }

    private fun attemptAutoScrollThenContinue() {
        val root = rootInActiveWindow
        val scrollable = root?.let { findScrollableNode(it) }
        val scrolled = scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false

        if (!scrolled) {
            stopReading()
            return
        }

        // give the app time to render new content, then read it
        handler.postDelayed({
            if (isReading) readCurrentScreen()
        }, 700)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun extractVisibleText(node: AccessibilityNodeInfo, builder: StringBuilder = StringBuilder()): String {
        if (!node.isVisibleToUser) return builder.toString()

        val nodeText = node.text?.toString()?.trim()
        if (!nodeText.isNullOrEmpty()) {
            if (builder.isNotEmpty()) builder.append("\n")
            builder.append(nodeText)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractVisibleText(child, builder)
        }
        return builder.toString()
    }

    // ---------- Lifecycle ----------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Content is pulled on-demand (bubble tap / auto-scroll), so no per-event work needed.
    }

    override fun onInterrupt() {
        stopReading()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubble.isInitialized) {
            try { windowManager.removeView(bubble) } catch (_: Exception) {}
        }
        tts?.shutdown()
    }
}
