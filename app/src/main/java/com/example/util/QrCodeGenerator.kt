package com.example.util

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor

/**
 * Pure Kotlin QR Code generator (Byte mode, Version 2 / 25x25 or Version 3 / 29x29, ECC Level M)
 * with Reed-Solomon GF(256) error correction so pickup QR tokens render as real scannable QR codes
 * without external binary/network dependencies.
 */
object QrCodeGenerator {

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11d
            }
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun rsGeneratorPoly(ecCount: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until ecCount) {
            val next = IntArray(poly.size + 1)
            val factor = GF_EXP[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], factor)
            }
            poly = next
        }
        return poly
    }

    private fun rsEncode(data: IntArray, ecCount: Int): IntArray {
        val gen = rsGeneratorPoly(ecCount)
        val res = IntArray(data.size + ecCount)
        data.copyInto(res)
        for (i in data.indices) {
            val coef = res[i]
            if (coef != 0) {
                for (j in 1 until gen.size) {
                    res[i + j] = res[i + j] xor gfMul(gen[j], coef)
                }
            }
        }
        return res.copyOfRange(data.size, res.size)
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val bytes = content.take(40).toByteArray(Charsets.UTF_8)
        // Version 3-M: 29x29 modules, 44 data codewords, 26 EC codewords
        val version = 3
        val size = 17 + version * 4 // 29
        val totalDataCodewords = 44
        val ecCodewords = 26

        val bits = ArrayList<Int>(totalDataCodewords * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }
        // Mode: Byte (0100)
        appendBits(0b0100, 4)
        appendBits(bytes.size, 8)
        for (b in bytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }
        val maxBits = totalDataCodewords * 8
        val terminator = minOf(4, maxBits - bits.size)
        appendBits(0, terminator)
        while (bits.size % 8 != 0) bits.add(0)
        var padByte = 0xEC
        while (bits.size < maxBits) {
            appendBits(padByte, 8)
            padByte = if (padByte == 0xEC) 0x11 else 0xEC
        }

        val dataCodewords = IntArray(totalDataCodewords) { i ->
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[i * 8 + b]
            }
            v
        }
        val ec = rsEncode(dataCodewords, ecCodewords)
        val allCodewords = dataCodewords + ec
        val allBits = ArrayList<Int>(allCodewords.size * 8 + 7)
        for (cw in allCodewords) {
            for (i in 7 downTo 0) allBits.add((cw ushr i) and 1)
        }
        repeat(7) { allBits.add(0) } // remainder bits for V3

        val modules = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        fun setModule(r: Int, c: Int, black: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                modules[r][c] = black
                reserved[r][c] = true
            }
        }

        fun drawFinder(row: Int, col: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = row + r
                    val cc = col + c
                    if (rr in 0 until size && cc in 0 until size) {
                        val isBorder = r == -1 || r == 7 || c == -1 || c == 7
                        val isBlack = !isBorder && (
                            r == 0 || r == 6 || c == 0 || c == 6 ||
                                (r in 2..4 && c in 2..4)
                            )
                        setModule(rr, cc, isBlack)
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Alignment pattern for V3 at (22, 22) -> center (22, 22)
        for (r in -2..2) {
            for (c in -2..2) {
                val black = kotlin.math.max(kotlin.math.abs(r), kotlin.math.abs(c)) != 1
                setModule(22 + r, 22 + c, black)
            }
        }

        // Timing patterns
        for (i in 8 until size - 8) {
            setModule(6, i, i % 2 == 0)
            setModule(i, 6, i % 2 == 0)
        }

        // Dark module
        setModule(size - 8, 8, true)

        // Reserve format info areas
        for (i in 0 until 9) {
            if (i != 6) {
                reserved[8][i] = true
                reserved[i][8] = true
            }
        }
        for (i in 0 until 8) {
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }

        // Place data bits with mask 0 ((r + c) % 2 == 0)
        var bitIndex = 0
        var upward = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col--
            for (step in 0 until size) {
                val r = if (upward) size - 1 - step else step
                for (dx in 0..1) {
                    val c = col - dx
                    if (!reserved[r][c]) {
                        val rawBit = if (bitIndex < allBits.size) allBits[bitIndex++] == 1 else false
                        val masked = if ((r + c) % 2 == 0) !rawBit else rawBit
                        modules[r][c] = masked
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // Format info for ECC M (00) + Mask 0 (000) -> 00000 -> BCH + mask 101010000010010 = 0x5412 -> 0x5412 xor 0 = 0x5412 (actually M=00, mask 0 => 101010000010010)
        val formatBits = 0b101010000010010
        for (i in 0..14) {
            val bit = ((formatBits ushr i) and 1) == 1
            if (i < 6) modules[i][8] = bit
            else if (i < 8) modules[i + 1][8] = bit
            else modules[size - 15 + i][8] = bit

            if (i < 8) modules[8][size - 1 - i] = bit
            else if (i < 9) modules[8][15 - i] = bit
            else modules[8][15 - i - 1] = bit
        }

        val quiet = 2
        val totalModules = size + quiet * 2
        val scale = maxOf(1, sizePx / totalModules)
        val bmpSize = totalModules * scale
        val bitmap = Bitmap.createBitmap(bmpSize, bmpSize, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bmpSize * bmpSize) { AndroidColor.WHITE }

        for (r in 0 until size) {
            for (c in 0 until size) {
                if (modules[r][c]) {
                    val startY = (r + quiet) * scale
                    val startX = (c + quiet) * scale
                    for (y in startY until startY + scale) {
                        val rowOffset = y * bmpSize
                        for (x in startX until startX + scale) {
                            pixels[rowOffset + x] = AndroidColor.BLACK
                        }
                    }
                }
            }
        }
        bitmap.setPixels(pixels, 0, bmpSize, 0, 0, bmpSize, bmpSize)
        return bitmap
    }
}
