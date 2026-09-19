package com.telugureader.telugu_reader

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.TextView
import java.util.Locale
import java.util.UUID

class TeluguReaderAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var bubble: ImageView
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var captionView: TextView? = null
    private var captionParams: WindowManager.LayoutParams? = null
    private var tts: TextToSpeech? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isReading = false
    private var lastReadText: String = ""
    private var consecutiveNoNewContent = 0
    private var sessionId = 0
    private var scrollStepsThisSession = 0
    private val maxScrollSteps = 400

    private val adKeywords = listOf(
        "advertisement", "sponsored", "ad ·", "ప్రకటన"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        tts = TextToSpeech(this, this)
        setupBubble()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale("te", "IN"))
            tts?.setSpeechRate(0.8f)
        }
    }

    // ---------- Floating bubble + caption ----------

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
            140, 140, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 800
            y = 600
        }

        var downX = 0f; var downY = 0f
        var startX = 0; var startY = 0
        var isClick = true

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = bubbleParams.x; startY = bubbleParams.y
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
        setupCaption(overlayType)
    }

    private fun setupCaption(overlayType: Int) {
        val caption = TextView(this).apply {
            setBackgroundColor(0xCC000000.toInt())
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(24, 16, 24, 16)
            maxLines = 3
            visibility = android.view.View.GONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 60
        }
        windowManager.addView(caption, params)
        captionView = caption
        captionParams = params
    }

    private fun showCaption(text: String) {
        handler.post {
            captionView?.text = text
            captionView?.visibility = android.view.View.VISIBLE
        }
    }

    private fun hideCaption() {
        handler.post { captionView?.visibility = android.view.View.GONE }
    }

    private fun onBubbleTapped() {
        if (isReading) {
            stopReading()
        } else {
            sessionId++
            scrollStepsThisSession = 0
            consecutiveNoNewContent = 0
            lastReadText = ""
            readCurrentScreen(sessionId)
        }
    }

    private fun stopReading() {
        isReading = false
        sessionId++ // invalidates any pending callbacks from the old session
        tts?.stop()
        bubble.setBackgroundColor(0x33000000)
        hideCaption()
    }

    // ---------- Reading + auto-scroll ----------

    private fun readCurrentScreen(mySession: Int) {
        if (mySession != sessionId) return
        val root = rootInActiveWindow ?: return
        val text = extractVisibleText(root)
        if (text.isBlank()) return

        if (text == lastReadText) {
            consecutiveNoNewContent++
        } else {
            consecutiveNoNewContent = 0
        }

        if (consecutiveNoNewContent >= 2 || scrollStepsThisSession > maxScrollSteps) {
            stopReading()
            return
        }

        isReading = true
        bubble.setBackgroundColor(0x66FF6B35)
        lastReadText = text
        speakSentences(text, mySession)
    }

    private fun splitSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.?!।॥\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun speakSentences(text: String, mySession: Int) {
        val sentences = splitSentences(text)
        if (sentences.isEmpty()) {
            attemptAutoScrollThenContinue(mySession)
            return
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val idx = utteranceId?.substringAfterLast("-")?.toIntOrNull() ?: return
                if (mySession == sessionId && idx < sentences.size) {
                    showCaption(sentences[idx])
                }
            }
            override fun onDone(utteranceId: String?) {
                val idx = utteranceId?.substringAfterLast("-")?.toIntOrNull() ?: return
                if (mySession == sessionId && idx == sentences.size - 1) {
                    handler.post { if (isReading && mySession == sessionId) attemptAutoScrollThenContinue(mySession) }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handler.post { if (isReading && mySession == sessionId) attemptAutoScrollThenContinue(mySession) }
            }
        })

        val id = UUID.randomUUID().toString()
        sentences.forEachIndexed { index, sentence ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(sentence, queueMode, null, "$id-$index")
        }
    }

    private fun attemptAutoScrollThenContinue(mySession: Int) {
        if (mySession != sessionId) return
        val root = rootInActiveWindow
        val scrollable = root?.let { findScrollableNode(it) }
        val scrolled = scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
        scrollStepsThisSession++

        if (!scrolled) {
            stopReading()
            return
        }

        handler.postDelayed({
            if (isReading && mySession == sessionId) readCurrentScreen(mySession)
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

    private fun isAdText(text: String): Boolean {
        val lower = text.lowercase()
        return adKeywords.any { lower.contains(it) } && text.length < 40
    }

    private fun extractVisibleText(node: AccessibilityNodeInfo, builder: StringBuilder = StringBuilder()): String {
        if (!node.isVisibleToUser) return builder.toString()

        val nodeText = node.text?.toString()?.trim()
        if (!nodeText.isNullOrEmpty() && !isAdText(nodeText)) {
            if (builder.isNotEmpty()) builder.append("\n")
            builder.append(nodeText)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractVisibleText(child, builder)
        }
        return builder.toString()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        stopReading()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubble.isInitialized) {
            try { windowManager.removeView(bubble) } catch (_: Exception) {}
        }
        captionView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        tts?.shutdown()
    }
}
