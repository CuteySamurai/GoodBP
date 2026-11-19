package com.goodbp.capture

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Reads the seven-segment LCD numbers on any home BP monitor that stacks
 * SYS, DIA and PULSE top to bottom (Omron, Guardian, most others).
 *
 * Pure Kotlin (no Android) so it can be tested on real photos off-device.
 * Steps: dark pixels vs local background -> blobs -> group blobs into digits ->
 * undo the italic slant -> check the 7 segment spots -> digits into rows ->
 * pick the two big rows (SYS above DIA) and the row below them (PULSE).
 */
object SegmentReader {

    /** 8-bit grayscale image, row-major. */
    class Gray(val w: Int, val h: Int, val px: IntArray)

    data class Num(val value: Int, val digits: Int, val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val height get() = bottom - top + 1
    }

    private const val MAX_SIDE = 1000
    /** Give up (and offer the keypad) rather than keep her waiting. */
    private const val TIME_BUDGET_MS = 9000L

    fun read(src: Gray): Triple<Int, Int, Int>? {
        val img = resize(src, MAX_SIDE)
        val deadline = System.currentTimeMillis() + TIME_BUDGET_MS
        // A photo held a little crooked: try small turns if straight fails.
        for (deg in listOf(0.0, -4.0, 4.0)) {
            if (System.currentTimeMillis() > deadline) break
            val r = readUpright(if (deg == 0.0) img else rotate(img, deg), deadline)
            if (r != null) return r
        }
        return null
    }

    /**
     * Many settings each read the photo: ink level, background window (small windows
     * follow shadow edges, large ones ignore ghost segments) and grouping distance.
     * The answer must win clearly: at least 3 votes and twice any other answer.
     * Otherwise we ask her to type it, rather than risk wrong numbers.
     */
    private fun readUpright(img: Gray, deadline: Long): Triple<Int, Int, Int>? {
        val votes = HashMap<Triple<Int, Int, Int>, Int>()
        for (rd in listOf(40, 60, 80)) for (t in listOf(0.5, 0.4, 0.6, 0.3, -0.6, -0.7, -0.8)) for (rf in listOf(0.5, 0.8)) {
            if (System.currentTimeMillis() > deadline) break
            val r = pick(numbers(img, t, rf, rd)) ?: continue
            votes[r] = (votes[r] ?: 0) + 1
            val sorted = votes.values.sortedDescending()
            if (sorted[0] >= 5 && sorted.size == 1) return r
        }
        val ranked = votes.entries.sortedByDescending { it.value }
        val top = ranked.firstOrNull() ?: return null
        val second = ranked.getOrNull(1)?.value ?: 0
        return if (top.value >= 3 && top.value >= 2 * second) top.key else null
    }

    // ---------------------------------------------------------------- image prep

    /** Scale so the long side is [side] px (box average down, bilinear up), so all thresholds see one size. */
    fun resize(src: Gray, side: Int): Gray {
        val f = max(src.w, src.h).toDouble() / side
        if (f > 1.0) return downscale(src, side)
        if (f > 0.95) return src
        val w = (src.w / f).toInt(); val h = (src.h / f).toInt()
        val out = IntArray(w * h)
        for (y in 0 until h) {
            val sy = (y * f).coerceAtMost(src.h - 1.001); val y0 = sy.toInt(); val fy = sy - y0
            for (x in 0 until w) {
                val sx = (x * f).coerceAtMost(src.w - 1.001); val x0 = sx.toInt(); val fx = sx - x0
                val i = y0 * src.w + x0
                val top = src.px[i] * (1 - fx) + src.px[i + 1] * fx
                val bot = src.px[i + src.w] * (1 - fx) + src.px[i + src.w + 1] * fx
                out[y * w + x] = (top * (1 - fy) + bot * fy).toInt()
            }
        }
        return Gray(w, h, out)
    }

    /** Turn by a few degrees around the centre; uncovered corners take the mean grey. */
    fun rotate(src: Gray, degrees: Double): Gray {
        val a = Math.toRadians(degrees); val c = kotlin.math.cos(a); val s = kotlin.math.sin(a)
        val cx = src.w / 2.0; val cy = src.h / 2.0
        val fillGrey = src.px.average().toInt()
        val out = IntArray(src.w * src.h)
        for (y in 0 until src.h) for (x in 0 until src.w) {
            val dx = x - cx; val dy = y - cy
            val sx = (c * dx + s * dy + cx).roundToInt(); val sy = (-s * dx + c * dy + cy).roundToInt()
            out[y * src.w + x] = if (sx in 0 until src.w && sy in 0 until src.h) src.px[sy * src.w + sx] else fillGrey
        }
        return Gray(src.w, src.h, out)
    }

    fun downscale(src: Gray, maxSide: Int): Gray {
        val f = max(src.w, src.h).toDouble() / maxSide
        if (f <= 1.0) return src
        val w = (src.w / f).toInt(); val h = (src.h / f).toInt()
        val out = IntArray(w * h)
        for (y in 0 until h) {
            val sy0 = (y * f).toInt(); val sy1 = min(src.h, ((y + 1) * f).toInt().coerceAtLeast(sy0 + 1))
            for (x in 0 until w) {
                val sx0 = (x * f).toInt(); val sx1 = min(src.w, ((x + 1) * f).toInt().coerceAtLeast(sx0 + 1))
                var s = 0; var n = 0
                for (yy in sy0 until sy1) { val row = yy * src.w; for (xx in sx0 until sx1) { s += src.px[row + xx]; n++ } }
                out[y * w + x] = s / n
            }
        }
        return Gray(w, h, out)
    }

    /**
     * Ink pixels. Background = brightest nearby value after a closing (thin dark
     * strokes filled in, shade steps kept); ink level = darkest nearby value.
     * A pixel is ink when it sits in the lower `level` part of that local range,
     * so dim, shadowed or glared parts of the screen all work, and the faint
     * "ghost" segments of an LCD stay out when real digits are near.
     */
    internal fun darkMask(img: Gray, level: Double, rDiv: Int = 40): BooleanArray {
        val w = img.w; val h = img.h
        val (bg, lo) = backgrounds(img, rDiv)
        val m = BooleanArray(w * h)
        if (level < 0) {
            // Shadow mode: compare each pixel with the screen brightness right there.
            // A shadow dims ink and background alike, so lit segments stay at about the
            // same fraction of the background while faint unlit "ghost" segments don't.
            val k = -level
            for (i in m.indices) { val b = bg[i]; val p = img.px[i]; m[i] = p < b * k && b - p >= 15 }
            return m
        }
        for (i in m.indices) {
            val b = bg[i]; val d = lo[i]; val p = img.px[i]
            m[i] = b - d >= 18 && p < d + level * (b - d) && p < b - 10
        }
        return m
    }

    private var cacheKey: Pair<Gray, Int>? = null
    private var cacheVal: Pair<IntArray, IntArray>? = null

    /** Background (closing) and local darkest value; the same for every ink level, so kept. */
    @Synchronized
    private fun backgrounds(img: Gray, rDiv: Int): Pair<IntArray, IntArray> {
        val key = img to rDiv
        if (cacheKey?.first === img && cacheKey?.second == rDiv) return cacheVal!!
        val w = img.w; val h = img.h
        val r = max(6, max(w, h) / rDiv)
        val tmp = IntArray(w * h); val bg = IntArray(w * h); val lo = IntArray(w * h)
        for (y in 0 until h) slidingMax(img.px, y * w, 1, w, r, tmp, 1)
        for (x in 0 until w) slidingMax(tmp, x, w, h, r, bg, 1)
        for (y in 0 until h) slidingMax(bg, y * w, 1, w, r, tmp, -1)
        for (x in 0 until w) slidingMax(tmp, x, w, h, r, bg, -1)
        for (y in 0 until h) slidingMax(img.px, y * w, 1, w, r, tmp, -1)
        for (x in 0 until w) slidingMax(tmp, x, w, h, r, lo, -1)
        cacheKey = key; cacheVal = bg to lo
        return cacheVal!!
    }

    /** Running max (sign 1) or min (sign -1) over +-r along one row or column. */
    private fun slidingMax(src: IntArray, start: Int, step: Int, n: Int, r: Int, dst: IntArray, sign: Int) {
        val dq = IntArray(n); var head = 0; var tail = 0
        var next = 0
        for (i in 0 until n) {
            val hi = min(n - 1, i + r)
            while (next <= hi) {
                val v = sign * src[start + next * step]
                while (tail > head && sign * src[start + dq[tail - 1] * step] <= v) tail--
                dq[tail++] = next; next++
            }
            while (dq[head] < i - r) head++
            dst[start + i * step] = src[start + dq[head] * step]
        }
    }

    // ---------------------------------------------------------------- blobs

    private class Blob(var x0: Int, var y0: Int, var x1: Int, var y1: Int, val pix: IntArray) {
        val w get() = x1 - x0 + 1
        val h get() = y1 - y0 + 1
    }

    private fun blobs(m: BooleanArray, w: Int, h: Int): List<Blob> {
        val seen = BooleanArray(m.size)
        val stack = IntArray(m.size)
        val out = ArrayList<Blob>()
        val buf = IntArray(m.size)
        for (start in m.indices) {
            if (!m[start] || seen[start]) continue
            var sp = 0; var n = 0
            stack[sp++] = start; seen[start] = true
            var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
            while (sp > 0) {
                val p = stack[--sp]; buf[n++] = p
                val x = p % w; val y = p / w
                if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx; val ny = y + dy
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                    val q = ny * w + nx
                    if (m[q] && !seen[q]) { seen[q] = true; stack[sp++] = q }
                }
            }
            if (n < 12) continue
            if (y1 - y0 > h * 0.3 || x1 - x0 > w * 0.25) continue // frames, buttons, shadows
            if (x0 == 0 || y0 == 0 || x1 == w - 1 || y1 == h - 1) continue // cut off by the photo edge
            // Hollow rings (button outlines) are not LCD digits: digits fill a fair part of their box.
            if (n < 0.08 * (x1 - x0 + 1) * (y1 - y0 + 1)) continue
            out.add(Blob(x0, y0, x1, y1, buf.copyOf(n)))
        }
        return out
    }

    /** Pieces close to each other (segments, digits, rows of one display) become one cluster. */
    private fun cluster(bs: List<Blob>, reachFactor: Double): List<Blob> {
        val n = bs.size
        if (n == 0 || n > 4000) return emptyList()
        val parent = IntArray(n) { it }
        fun find(i: Int): Int { var a = i; while (parent[a] != a) { parent[a] = parent[parent[a]]; a = parent[a] }; return a }
        val reach = DoubleArray(n) { val b = bs[it]; reachFactor * b.pix.size.toDouble() / max(b.w, b.h) }
        for (i in 0 until n) for (j in i + 1 until n) {
            val a = bs[i]; val b = bs[j]
            val e = max(reach[i], reach[j])
            val gx = max(0, max(a.x0, b.x0) - min(a.x1, b.x1))
            val gy = max(0, max(a.y0, b.y0) - min(a.y1, b.y1))
            if (gx <= e && gy <= e) parent[find(i)] = find(j)
        }
        val groups = HashMap<Int, MutableList<Blob>>()
        for (i in 0 until n) groups.getOrPut(find(i)) { ArrayList() }.add(bs[i])
        return groups.values.map { g ->
            Blob(g.minOf { it.x0 }, g.minOf { it.y0 }, g.maxOf { it.x1 }, g.maxOf { it.y1 },
                g.flatMap { it.pix.asIterable() }.toIntArray())
        }
    }

    private fun blobOf(pix: IntArray, w: Int): Blob {
        var x0 = Int.MAX_VALUE; var y0 = Int.MAX_VALUE; var x1 = -1; var y1 = -1
        for (p in pix) { val x = p % w; val y = p / w; if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y }
        return Blob(x0, y0, x1, y1, pix)
    }

    /** Split a cluster into text lines where the row-ink count nearly vanishes. */
    private fun lines(c: Blob, w: Int): List<Blob> {
        val prof = IntArray(c.h)
        for (p in c.pix) prof[p / w - c.y0]++
        val thr = max(2, (0.03 * prof.max()).toInt())
        val out = ArrayList<Blob>()
        var y = 0
        while (y < c.h) {
            if (prof[y] <= thr) { y++; continue }
            val s = y
            while (y < c.h && prof[y] > thr) y++
            val y0 = c.y0 + s; val y1 = c.y0 + y - 1
            if (y1 - y0 + 1 < 6) continue
            // Keep the faint tips just outside the cut too.
            val lo = y0 - 2; val hi = y1 + 2
            out.add(blobOf(c.pix.filter { val py = it / w; py in lo..hi }.toIntArray(), w))
        }
        return out
    }

    // ---------------------------------------------------------------- digits

    // Segment bits: a=1 top, b=2 upper right, c=4 lower right, d=8 bottom, e=16 lower left, f=32 upper left, g=64 middle
    private val patterns: Map<Int, Int> = mapOf(
        0b0111111 to 0, 0b0000110 to 1, 0b1011011 to 2, 0b1001111 to 3, 0b1100110 to 4,
        0b1101101 to 5, 0b1111101 to 6, 0b1111100 to 6, 0b0000111 to 7, 0b0100111 to 7,
        0b1111111 to 8, 0b1101111 to 9, 0b1100111 to 9,
    )

    /** Italic LCD digits lean right; pick the shear that opens the most blank columns. */
    private fun shearOf(line: Blob, w: Int): Double {
        val cy = (line.y0 + line.y1) / 2.0
        var best = 0.0; var bestBlank = -1
        val pad = line.h
        val cols = IntArray(line.w + 2 * pad)
        var s = -0.1
        while (s <= 0.45) {
            cols.fill(0)
            for (p in line.pix) {
                val x = (p % w - line.x0 + pad + s * (p / w - cy)).roundToInt().coerceIn(0, cols.size - 1)
                cols[x]++
            }
            val first = cols.indexOfFirst { it > 0 }; val last = cols.indexOfLast { it > 0 }
            var blank = 0
            for (i in first..last) if (cols[i] <= 1) blank++
            if (blank > bestBlank) { bestBlank = blank; best = s }
            s += 0.02
        }
        return best
    }

    /** Cut a line into glyphs at blank (sheared) columns. */
    private fun glyphs(line: Blob, w: Int, shear: Double): List<Blob> {
        val cy = (line.y0 + line.y1) / 2.0
        val pad = line.h
        val n = line.w + 2 * pad
        val col = IntArray(line.pix.size)
        val cols = IntArray(n)
        line.pix.forEachIndexed { i, p ->
            val x = (p % w - line.x0 + pad + shear * (p / w - cy)).roundToInt().coerceIn(0, n - 1)
            col[i] = x; cols[x]++
        }
        val out = ArrayList<Blob>()
        var x = 0
        while (x < n) {
            if (cols[x] <= 1) { x++; continue }
            val s = x
            while (x < n && cols[x] > 1) x++
            val e = x - 1
            val pix = line.pix.filterIndexed { i, _ -> col[i] in s..e }.toIntArray()
            out.add(blobOf(pix, w))
        }
        return out
    }

    private fun decodeBits(g: Blob, w: Int, shear: Double): Pair<Int, Int?> {
        val cy = (g.y0 + g.y1) / 2.0
        var lo = Double.MAX_VALUE; var hi = -Double.MAX_VALUE
        for (p in g.pix) { val x = p % w + shear * (p / w - cy); if (x < lo) lo = x; if (x > hi) hi = x }
        val gw = hi - lo + 1; val gh = g.h.toDouble()
        if (gw < 0.42 * gh) {
            // Narrow: a "1" if ink runs in both halves.
            var top = 0; var bot = 0
            for (p in g.pix) if (p / w < cy) top++ else bot++
            return -1 to (if (top > 0 && bot > 0 && min(top, bot) > 0.25 * max(top, bot)) 1 else null)
        }
        val cols = 10; val rows = 20
        val grid = IntArray(cols * rows)
        for (p in g.pix) {
            val u = ((p % w + shear * (p / w - cy) - lo) / gw * cols).toInt().coerceIn(0, cols - 1)
            val v = ((p / w - g.y0) / gh * rows).toInt().coerceIn(0, rows - 1)
            grid[v * cols + u]++
        }
        val cell = gw * gh / (cols * rows)
        fun fill(u0: Int, u1: Int, v0: Int, v1: Int): Double {
            var s = 0
            for (v in v0 until v1) for (u in u0 until u1) s += grid[v * cols + u]
            return s / (cell * (u1 - u0) * (v1 - v0))
        }
        val segs = doubleArrayOf(
            fill(3, 7, 0, 3),    // a
            fill(7, 10, 3, 8),   // b
            fill(7, 10, 12, 17), // c
            fill(3, 7, 17, 20),  // d
            fill(0, 3, 12, 17),  // e
            fill(0, 3, 3, 8),    // f
            fill(3, 7, 8, 12),   // g
        )
        var bits = 0
        segs.forEachIndexed { i, f -> if (f > 0.22) bits = bits or (1 shl i) }
        // A segment that is neither clearly on nor clearly off (glare, blur) makes the digit unsure.
        if (segs.any { it in 0.12..0.36 }) return bits to null
        patterns[bits]?.let { return bits to it }
        // A shadow edge can hide one segment. Accept the digit only if exactly one
        // digit matches with one extra segment (never by removing ink).
        val near = patterns.filter { (p, d) -> d != 1 && p and bits == bits && Integer.bitCount(p xor bits) == 1 }
            .values.toSet()
        return bits to near.singleOrNull()
    }

    // ---------------------------------------------------------------- numbers

    private class Read(val g: Blob, val bits: Int, val digit: Int?)

    private fun readLines(img: Gray, ratio: Double, reachFactor: Double, rDiv: Int): List<List<Read>> {
        val m = darkMask(img, ratio, rDiv)
        val w = img.w
        val kept = blobs(m, w, img.h)
        // Ink that survived the blob filters, for re-reading whole bands below.
        val ink = BooleanArray(m.size)
        for (b in kept) for (p in b.pix) ink[p] = true
        val all = ArrayList<Blob>()
        for (c in cluster(kept, reachFactor).filter { it.h >= 14 }) {
            val ls = lines(c, w)
            all.addAll(ls.filter { it.h >= 12 })
            // Digits without a middle bar (0, 1, 7) have a blank row halfway down,
            // so a line may come out as two halves: also try each pair joined.
            for (i in 0 until ls.size - 1) {
                val a = ls[i]; val b = ls[i + 1]
                val ratioH = a.h.toDouble() / b.h
                if (b.y0 - a.y1 <= 0.35 * min(a.h, b.h) && ratioH in 0.6..1.6)
                    all.add(Blob(min(a.x0, b.x0), a.y0, max(a.x1, b.x1), b.y1, a.pix + b.pix))
            }
        }
        fun read(line: Blob): List<Read> {
            val sh = shearOf(line, w)
            return glyphs(line, w, sh)
                .filter { it.h >= 0.6 * line.h && it.pix.size >= 10 }
                .map { g ->
                    val (bits, d) = decodeBits(g, w, sh)
                    // Real digits other than 1 are wide; thin bars (charge or level gauges) are not.
                    Read(g, bits, if (d != null && d != 1 && g.w < 0.4 * g.h) null else d)
                }
        }
        val first = all.map { read(it) }
        // A digit's strokes can land in different clusters or line halves (a "7"
        // whose lower stroke stands apart). Re-read the full band of ink at the
        // height of every tall glyph found so far.
        val seen = HashSet<Long>()
        val second = first.flatten().filter { it.g.h >= 24 }.mapNotNull { r ->
            val g = r.g
            if (!seen.add((g.y0 / 3).toLong() * 100000 + g.y1 / 3)) return@mapNotNull null
            val x0 = max(0, g.x0 - 3 * g.h); val x1 = min(w - 1, g.x1 + 3 * g.h)
            val pix = ArrayList<Int>()
            for (y in g.y0..g.y1) for (x in x0..x1) if (ink[y * w + x]) pix.add(y * w + x)
            read(blobOf(pix.toIntArray(), w))
        }
        return first + second
    }

    fun numbers(img: Gray, ratio: Double = 0.5, reachFactor: Double = 0.5, rDiv: Int = 40): List<Num> {
        // Chain readable digits of the same size on the same line, left to right
        // (across clusters: a lone "1" often stands apart from the rest).
        val reads = readLines(img, ratio, reachFactor, rDiv).flatten().filter { it.digit != null }.sortedBy { it.g.x0 }
        val chains = ArrayList<MutableList<Read>>()
        for (r in reads) {
            val chain = chains.filter { c ->
                val last = c.last().g; val h = last.h.toDouble()
                // Same top line (a "7" may end higher than its neighbours), similar size, close.
                abs(r.g.y0 - last.y0) < 0.15 * h && r.g.h / h in 0.75..1.33 &&
                    r.g.x0 - last.x1 in (-0.1 * h).toInt()..(0.9 * h).toInt()
            }.minByOrNull { c -> abs(r.g.y0 - c.last().g.y0) }
            if (chain != null) chain.add(r) else chains.add(arrayListOf(r))
        }
        // Digits sit on a fixed pitch (right edges evenly spaced). Bars or marks
        // next to a number break that rhythm: cut there. Numbers are right-aligned,
        // so a longer run keeps its last 3 digits.
        val runs = ArrayList<List<Read>>()
        for (c in chains) {
            if (c.size < 3) { runs.add(c); continue }
            val steps = (1 until c.size).map { c[it].g.x1 - c[it - 1].g.x1 }
            val pitch = steps.sorted()[steps.size / 2].toDouble()
            var cur = arrayListOf(c[0])
            for (i in 1 until c.size) {
                if (steps[i - 1] / pitch !in 0.7..1.3) { runs.add(cur); cur = arrayListOf() }
                cur.add(c[i])
            }
            runs.add(cur)
        }
        return runs.map { if (it.size > 3) it.takeLast(3) else it }.filter { it.size in 2..3 }.mapNotNull { run ->
            val v = run.fold(0) { acc, r -> acc * 10 + r.digit!! }
            // All-"1" rows are usually bars or gauge marks, not numbers.
            if (run.all { it.digit == 1 }) null
            else if (run.size == 2 && run[0].digit != 0 || run.size == 3 && v in 100..299)
                Num(v, run.size, run.minOf { it.g.x0 }, run.minOf { it.g.y0 }, run.maxOf { it.g.x1 }, run.maxOf { it.g.y1 })
            else null
        }
    }

    /**
     * SYS and DIA: two rows of the same size, DIA right under SYS, right edges lined up.
     * PULSE: the next row under DIA, not bigger than it, right edge lined up too.
     * Among candidates the biggest digits win (the main readout of the screen).
     */
    fun pick(nums: List<Num>): Triple<Int, Int, Int>? {
        fun overlapsX(a: Num, b: Num) = min(a.right, b.right) - max(a.left, b.left) > 0.3 * min(a.right - a.left, b.right - b.left)
        var best: Triple<Int, Int, Int>? = null; var bestScore = -1
        for (s in nums) for (d in nums) {
            if (s === d || d.top < s.bottom - 0.2 * s.height) continue
            if (d.height.toDouble() / s.height !in 0.85..1.18 || !overlapsX(s, d)) continue
            if (d.top - s.bottom > 0.6 * s.height) continue
            if (abs(d.right - s.right) > 0.35 * s.height) continue
            if (s.value !in 60..260 || d.value !in 30..160 || s.value <= d.value) continue
            val p = nums.filter {
                it !== s && it !== d && it.top >= d.bottom - 0.15 * d.height && it.top - d.bottom < 1.0 * d.height &&
                    it.height.toDouble() / d.height in 0.3..1.05 && abs(it.right - d.right) < 0.5 * d.height &&
                    it.value in 30..220
            }.minByOrNull { it.top } ?: continue
            val score = s.height + d.height
            if (score > bestScore) { bestScore = score; best = Triple(s.value, d.value, p.value) }
        }
        return best
    }

    internal fun inspectClusters(img: Gray, ratio: Double): List<String> {
        val m = darkMask(img, ratio)
        return cluster(blobs(m, img.w, img.h), 0.5).filter { it.h >= 14 }.map { c ->
            "C[${c.x0},${c.y0} ${c.w}x${c.h}] lines=" + lines(c, img.w).joinToString { "[${it.x0},${it.y0} ${it.w}x${it.h}]" }
        }
    }

    /** Test hook: every glyph per line with its box, segment bits and digit. */
    internal fun inspect(img: Gray, ratio: Double): List<String> =
        readLines(img, ratio, 0.5, 40).filter { it.isNotEmpty() }.map { line ->
            line.joinToString("  ") { r -> "[${r.g.x0},${r.g.y0} ${r.g.w}x${r.g.h} ${Integer.toBinaryString(r.bits)}=${r.digit}]" }
        }

}
