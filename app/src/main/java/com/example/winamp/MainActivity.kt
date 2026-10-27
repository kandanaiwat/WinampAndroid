package com.example.winamp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay

@UnstableApi
class MainActivity : ComponentActivity() {
    private lateinit var pc: PlayerController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pc = PlayerController(applicationContext)
        setContent { WinampApp(pc) }
    }
    override fun onDestroy() { super.onDestroy(); pc.release() }
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

// ====================== หน้าต่างหลัก ======================
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun WinampApp(pc: PlayerController) {
    val ctx = LocalContext.current
    var skinIndex by remember { mutableIntStateOf(Skins.indexOf(SkinStore.load(ctx)).coerceAtLeast(0)) }
    val skin = Skins[skinIndex]
    var showEq by remember { mutableStateOf(true) }
    var showPl by remember { mutableStateOf(true) }
    var showSkins by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { pc.add(it) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) pc.startVisualizer() }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            pc.startVisualizer() else perm.launch(Manifest.permission.RECORD_AUDIO)
    }
    LaunchedEffect(Unit) { while (true) { pc.tick(); delay(200) } }

    Column(
        Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ---------- MAIN ----------
        Win(skin, "WINAMP") {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // LCD: เวลา + spectrum
                Column(Modifier.width(120.dp).height(64.dp).clip(RoundedCornerShape(2.dp)).background(skin.lcd).padding(4.dp)) {
                    Text(fmt(pc.position), color = skin.lcdText, fontSize = 30.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spectrum(skin, pc.bars, Modifier.fillMaxWidth().weight(1f))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(skin.lcd).padding(horizontal = 4.dp, vertical = 3.dp)) {
                        val t = pc.tracks.getOrNull(pc.current)?.let { "${pc.current + 1}. ${it.title}  ***  " } ?: "WINAMP for Android  ***  "
                        Text(t, color = skin.lcdText, fontSize = 13.sp, maxLines = 1, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Lcd(skin, "${fmt(pc.duration)}")
                        Lcd(skin, if (pc.isPlaying) "PLAY" else "STOP")
                    }
                }
            }
            // seek bar
            val dur = pc.duration.coerceAtLeast(1)
            Slider(
                value = (pc.position.toFloat() / dur).coerceIn(0f, 1f),
                onValueChange = { pc.seekTo((it * dur).toLong()) },
                colors = sliderColors(skin), modifier = Modifier.padding(horizontal = 6.dp).height(24.dp)
            )
            // ปุ่มควบคุม
            Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Btn(skin, "⏮") { pc.prev() }
                Btn(skin, "▶") { pc.play() }
                Btn(skin, "⏸") { pc.pause() }
                Btn(skin, "⏹") { pc.stop() }
                Btn(skin, "⏭") { pc.next() }
                Btn(skin, "⏏") { picker.launch(arrayOf("audio/*")) }
                Spacer(Modifier.weight(1f))
                Btn(skin, "SHUF", pc.shuffle) { pc.toggleShuffle() }
                Btn(skin, when (pc.repeat) { Player.REPEAT_MODE_ONE -> "REP1"; else -> "REP" },
                    pc.repeat != Player.REPEAT_MODE_OFF) { pc.cycleRepeat() }
            }
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Btn(skin, "EQ", showEq) { showEq = !showEq }
                Btn(skin, "PL", showPl) { showPl = !showPl }
                Btn(skin, "SKINS", showSkins) { showSkins = !showSkins }
            }
        }

        // ---------- SKIN BROWSER ----------
        if (showSkins) Win(skin, "SKIN BROWSER") {
            Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Skins.forEachIndexed { i, s ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                            .background(if (i == skinIndex) skin.button else Color.Transparent)
                            .clickable { skinIndex = i; SkinStore.save(ctx, i) }.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ตัวอย่างสีของสกิน
                        Row(Modifier.clip(RoundedCornerShape(2.dp)).border(1.dp, s.light, RoundedCornerShape(2.dp))) {
                            listOf(s.frame, s.titleA, s.lcd, s.lcdText, s.slider).forEach {
                                Box(Modifier.size(width = 18.dp, height = 24.dp).background(it))
                            }
                        }
                        Text(s.name, color = skin.buttonText, fontSize = 14.sp)
                    }
                }
            }
        }

        // ---------- EQUALIZER ----------
        if (showEq) Win(skin, "WINAMP EQUALIZER") {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Btn(skin, "ON", pc.eqOn) { pc.setEqOn(!pc.eqOn) }
                Btn(skin, "RESET") { pc.eqReset() }
            }
            if (pc.eqLevels.isEmpty()) {
                Text("กดเล่นเพลงก่อน EQ จึงจะทำงาน", color = skin.lcdDim, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
            } else Row(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                pc.eqLevels.forEachIndexed { i, lv ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        VSlider(skin, lv.toFloat(), pc.eqMin.toFloat(), pc.eqMax.toFloat(), Modifier.weight(1f)) {
                            pc.setEqLevel(i, it.toInt())
                        }
                        Text(pc.eqLabels.getOrElse(i) { "" }, color = skin.buttonText, fontSize = 10.sp)
                    }
                }
            }
        }

        // ---------- PLAYLIST ----------
        if (showPl) Win(skin, "WINAMP PLAYLIST") {
            Column(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 280.dp).background(skin.listBg)
                .verticalScroll(rememberScrollState()).padding(4.dp)) {
                if (pc.tracks.isEmpty()) Text("กดปุ่ม ADD เพื่อเพิ่มเพลง", color = skin.lcdDim, fontSize = 13.sp)
                pc.tracks.forEachIndexed { i, t ->
                    Row(Modifier.fillMaxWidth().clickable { pc.playIndex(i) }.padding(vertical = 4.dp)) {
                        Text("${i + 1}. ${t.title}", maxLines = 1, fontSize = 14.sp, modifier = Modifier.weight(1f),
                            color = if (i == pc.current) skin.listCurrent else skin.listText)
                    }
                }
            }
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Btn(skin, "ADD") { picker.launch(arrayOf("audio/*")) }
                Btn(skin, "REM") { pc.remove(pc.current) }
                Btn(skin, "CLEAR") { pc.clear() }
                Spacer(Modifier.weight(1f))
                Lcd(skin, "${fmt(pc.position)}/${fmt(pc.duration)}")
            }
        }
    }
}

// ====================== ส่วนประกอบ UI ======================
@Composable
fun Win(skin: Skin, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(skin.frame).border(1.dp, skin.light)) {
        Row(Modifier.fillMaxWidth().height(18.dp).background(Brush.verticalGradient(listOf(skin.titleA, skin.titleB))),
            verticalAlignment = Alignment.CenterVertically) {
            TitleLines(skin, Modifier.weight(1f))
            Text(" $title ", color = skin.buttonText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            TitleLines(skin, Modifier.weight(1f))
        }
        content()
    }
}

@Composable
private fun TitleLines(skin: Skin, modifier: Modifier) {
    Column(modifier.padding(horizontal = 3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { Box(Modifier.fillMaxWidth().height(1.dp).background(skin.light.copy(alpha = 0.8f))) }
    }
}

@Composable
fun Btn(skin: Skin, label: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.height(30.dp).widthIn(min = 36.dp).clip(RoundedCornerShape(3.dp))
            .background(if (active) skin.slider.copy(alpha = 0.35f) else skin.button)
            .border(1.dp, skin.light, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (active) skin.lcdText else skin.buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun Lcd(skin: Skin, text: String) {
    Box(Modifier.clip(RoundedCornerShape(2.dp)).background(skin.lcd).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, color = skin.lcdText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun sliderColors(skin: Skin) = SliderDefaults.colors(
    thumbColor = skin.buttonText, activeTrackColor = skin.slider, inactiveTrackColor = skin.lcd,
    activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent
)

/** สไลด์เดอร์แนวตั้งสำหรับ EQ */
@Composable
fun VSlider(skin: Skin, value: Float, min: Float, max: Float, modifier: Modifier, onChange: (Float) -> Unit) {
    BoxWithConstraints(
        modifier.width(28.dp).pointerInputDrag { frac -> onChange(max - frac * (max - min)) }
    ) {
        val h = constraints.maxHeight.toFloat()
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            drawRect(skin.lcd, Offset(cx - 4.dp.toPx(), 0f), Size(8.dp.toPx(), size.height))
            val y = (1f - (value - min) / (max - min)) * size.height
            val mid = size.height / 2
            drawRect(skin.slider, Offset(cx - 4.dp.toPx(), minOf(y, mid)), Size(8.dp.toPx(), kotlin.math.abs(y - mid)))
            drawRect(skin.buttonText, Offset(cx - 11.dp.toPx(), y - 5.dp.toPx()), Size(22.dp.toPx(), 10.dp.toPx()))
            drawLine(skin.light, Offset(0f, mid), Offset(size.width, mid), 1f)
        }
    }
}

private fun Modifier.pointerInputDrag(onFrac: (Float) -> Unit): Modifier =
    this.then(Modifier.pointerInput(Unit) {
        detectTapGestures { onFrac((it.y / size.height).coerceIn(0f, 1f)) }
    }).pointerInput(Unit) {
        detectDragGestures { change, _ -> onFrac((change.position.y / size.height).coerceIn(0f, 1f)) }
    }

/** Spectrum analyzer แบบแท่ง เหมือน Winamp */
@Composable
fun Spectrum(skin: Skin, bars: FloatArray, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val n = bars.size
        val gap = 1.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        val seg = 3.dp.toPx()
        bars.forEachIndexed { i, v ->
            val h = v * size.height
            var y = size.height
            var k = 0
            while (size.height - y < h) {
                val frac = (size.height - y) / size.height
                drawRect(lerp(skin.barLow, skin.barHigh, frac), Offset(i * (w + gap), y - seg), Size(w, seg - 1f))
                y -= seg; k++
            }
        }
    }
}
