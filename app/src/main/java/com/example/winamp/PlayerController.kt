package com.example.winamp

import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import kotlin.math.hypot
import kotlin.math.max

data class Track(val uri: Uri, val title: String)

@UnstableApi
class PlayerController(private val ctx: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(ctx).build()
    val tracks = mutableStateListOf<Track>()
    var current by mutableIntStateOf(-1)
    var isPlaying by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var bars by mutableStateOf(FloatArray(BAR_COUNT))

    // Equalizer
    var eqOn by mutableStateOf(true)
    val eqLevels = mutableStateListOf<Int>()
    var eqMin = -1500; var eqMax = 1500
    val eqLabels = mutableStateListOf<String>()
    private var eq: Equalizer? = null
    private var vis: Visualizer? = null
    private var visAllowed = false

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                current = player.currentMediaItemIndex
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) duration = player.duration.coerceAtLeast(0)
            }
        })
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(t: AnalyticsListener.EventTime, id: Int) = attachEffects(id)
        })
        attachEffects(player.audioSessionId)
    }

    companion object { const val BAR_COUNT = 19 }

    fun add(uris: List<Uri>) {
        uris.forEach { uri ->
            tracks.add(Track(uri, displayName(uri)))
            player.addMediaItem(MediaItem.fromUri(uri))
        }
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        if (current < 0 && tracks.isNotEmpty()) current = 0
    }

    private fun displayName(uri: Uri): String {
        var name: String? = null
        ctx.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) name = it.getString(i)
            }
        }
        return (name ?: uri.lastPathSegment ?: "Unknown").substringBeforeLast('.')
    }

    fun remove(index: Int) {
        if (index !in tracks.indices) return
        tracks.removeAt(index); player.removeMediaItem(index)
        current = if (tracks.isEmpty()) -1 else player.currentMediaItemIndex
    }
    fun clear() { player.stop(); player.clearMediaItems(); tracks.clear(); current = -1; position = 0; duration = 0 }

    fun playIndex(i: Int) {
        if (i !in tracks.indices) return
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.seekTo(i, 0); player.play(); current = i
    }
    fun play() { if (tracks.isEmpty()) return; if (player.playbackState == Player.STATE_IDLE) player.prepare(); player.play() }
    fun pause() = player.pause()
    fun stop() { player.pause(); player.seekTo(0); position = 0 }
    fun next() { if (player.hasNextMediaItem()) player.seekToNextMediaItem() else if (repeat == Player.REPEAT_MODE_ALL) player.seekTo(0, 0) }
    fun prev() { if (player.currentPosition > 3000 || !player.hasPreviousMediaItem()) player.seekTo(0) else player.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { player.seekTo(ms); position = ms }
    fun toggleShuffle() { shuffle = !shuffle; player.shuffleModeEnabled = shuffle }
    fun cycleRepeat() {
        repeat = when (repeat) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE else -> Player.REPEAT_MODE_OFF }
        player.repeatMode = repeat
    }
    fun tick() {
        position = player.currentPosition
        if (player.duration > 0) duration = player.duration
        if (player.currentMediaItemIndex in tracks.indices) current = player.currentMediaItemIndex
    }

    // ---------- Equalizer ----------
    private fun attachEffects(sessionId: Int) {
        eq?.release(); eq = null
        if (sessionId == 0) return
        try {
            eq = Equalizer(0, sessionId).apply {
                enabled = eqOn
                eqMin = bandLevelRange[0].toInt(); eqMax = bandLevelRange[1].toInt()
                eqLevels.clear(); eqLabels.clear()
                for (b in 0 until numberOfBands) {
                    eqLevels.add(getBandLevel(b.toShort()).toInt())
                    val hz = getCenterFreq(b.toShort()) / 1000
                    eqLabels.add(if (hz >= 1000) "${hz / 1000}K" else "$hz")
                }
            }
        } catch (_: Exception) {}
        if (visAllowed) startVisualizer()
    }
    fun setEqLevel(band: Int, level: Int) {
        eqLevels[band] = level
        try { eq?.setBandLevel(band.toShort(), level.toShort()) } catch (_: Exception) {}
    }
    fun setEqOn(on: Boolean) { eqOn = on; try { eq?.enabled = on } catch (_: Exception) {} }
    fun eqReset() { for (i in eqLevels.indices) setEqLevel(i, 0) }

    // ---------- Spectrum ----------
    fun startVisualizer() {
        visAllowed = true
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        val id = player.audioSessionId
        if (id == 0) return
        try {
            vis?.release()
            vis = Visualizer(id).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(512)
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, w: ByteArray?, r: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, r: Int) {
                        if (fft != null) bars = computeBars(fft)
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (_: Exception) {}
    }

    private fun computeBars(fft: ByteArray): FloatArray {
        val bins = fft.size / 2
        val usable = (bins * 0.55f).toInt().coerceAtLeast(BAR_COUNT)
        val out = FloatArray(BAR_COUNT)
        for (i in 0 until BAR_COUNT) {
            val from = 1 + (usable * i / BAR_COUNT)
            val to = max(from + 1, 1 + usable * (i + 1) / BAR_COUNT)
            var peak = 0f
            for (k in from until to.coerceAtMost(bins)) {
                val re = fft[2 * k].toFloat(); val im = fft[2 * k + 1].toFloat()
                peak = max(peak, hypot(re, im))
            }
            val target = (peak / 110f).coerceIn(0f, 1f)
            out[i] = max(target, bars[i] - 0.06f)   // ค่อยๆ ตกลงแบบ Winamp
        }
        return out
    }

    fun release() { vis?.release(); eq?.release(); player.release() }
}
