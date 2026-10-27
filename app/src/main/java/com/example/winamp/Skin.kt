package com.example.winamp

import android.content.Context
import androidx.compose.ui.graphics.Color

/** สกินหนึ่งชุด = ชุดสีทั้งหมดของหน้าต่าง Winamp */
data class Skin(
    val name: String,
    val frame: Color,      // พื้นหน้าต่าง
    val light: Color,      // ขอบสว่าง
    val titleA: Color,     // แถบ title (บน)
    val titleB: Color,     // แถบ title (ล่าง)
    val lcd: Color,        // พื้นจอ LCD
    val lcdText: Color,    // ตัวเลข/ข้อความบนจอ
    val lcdDim: Color,     // ข้อความจาง
    val button: Color,     // ปุ่ม
    val buttonText: Color,
    val slider: Color,     // แถบเลื่อน/EQ
    val listBg: Color,     // พื้น playlist
    val listText: Color,
    val listCurrent: Color,
    val barLow: Color,     // สี spectrum ล่าง
    val barHigh: Color     // สี spectrum บน
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Skins = listOf(
    Skin("Midnight Purple (ตามรูป)", c(0x2B2A4A), c(0x6E6C9A), c(0x5A5888), c(0x2E2D50), c(0x000000),
        c(0x00E000), c(0x0A5A0A), c(0x3C3B66), c(0xC8C8F0), c(0xE6D020), c(0x000000), c(0xFFFFFF), c(0x00FF40),
        c(0x20D020), c(0xE0C020)),
    Skin("Classic Winamp", c(0x3A3A3A), c(0x9A9A9A), c(0x707070), c(0x303030), c(0x000000),
        c(0x00FF00), c(0x004400), c(0x505050), c(0xE0E0E0), c(0xD0B000), c(0x000000), c(0x00FF00), c(0xFFFFFF),
        c(0x00C800), c(0xF0F000)),
    Skin("Neon Cyber", c(0x120A24), c(0xFF2BD6), c(0x2A0F5C), c(0x120A24), c(0x05010F),
        c(0x00F5FF), c(0x064A52), c(0x2A0F5C), c(0x00F5FF), c(0xFF2BD6), c(0x05010F), c(0x9FFBFF), c(0xFF2BD6),
        c(0x00F5FF), c(0xFF2BD6)),
    Skin("Amber Terminal", c(0x1E1408), c(0x8A5A10), c(0x4A3208), c(0x1E1408), c(0x0A0600),
        c(0xFFB000), c(0x4A3200), c(0x3A2A10), c(0xFFC850), c(0xFFB000), c(0x0A0600), c(0xFFB000), c(0xFFFFFF),
        c(0xCC7A00), c(0xFFE070)),
    Skin("Ocean Blue", c(0x0B2A4A), c(0x5AA8E8), c(0x1B5C9A), c(0x0B2A4A), c(0x00101E),
        c(0x5CE1FF), c(0x0B3A52), c(0x16406E), c(0xD8F2FF), c(0x5CE1FF), c(0x00101E), c(0xD8F2FF), c(0x5CFFB0),
        c(0x1E90FF), c(0x5CFFE0)),
    Skin("Forest", c(0x1C2B1C), c(0x7AB07A), c(0x3A5A3A), c(0x1C2B1C), c(0x020A02),
        c(0x9CFF6A), c(0x1E4A10), c(0x2E452E), c(0xE0FFD8), c(0xC8E040), c(0x020A02), c(0xC8FFB0), c(0xFFFFFF),
        c(0x38B000), c(0xD8F030)),
    Skin("Blood Red", c(0x2A0A0A), c(0xB04040), c(0x6A1818), c(0x2A0A0A), c(0x080000),
        c(0xFF3030), c(0x4A0A0A), c(0x4A1414), c(0xFFD0D0), c(0xFF5050), c(0x080000), c(0xFFB0B0), c(0xFFFFFF),
        c(0xC00000), c(0xFFA040)),
    Skin("Sakura Light", c(0xF3DDE6), c(0xFFFFFF), c(0xE59AB8), c(0xF3DDE6), c(0xFFF8FB),
        c(0xC2185B), c(0xEBC4D4), c(0xE8B8CC), c(0x5A1E38), c(0xC2185B), c(0xFFF8FB), c(0x5A1E38), c(0xC2185B),
        c(0xE57399), c(0x8E24AA))
)

object SkinStore {
    private const val PREF = "winamp"
    fun load(ctx: Context): Skin {
        val i = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("skin", 0)
        return Skins.getOrElse(i) { Skins[0] }
    }
    fun save(ctx: Context, index: Int) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("skin", index).apply()
}
