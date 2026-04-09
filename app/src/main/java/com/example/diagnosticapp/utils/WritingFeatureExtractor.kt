package com.example.diagnosticapp.utils

import android.util.Log
import kotlin.math.*

object WritingFeatureExtractor {

    private const val TAG = "WFE"

    data class SVCData(
        val x: FloatArray,
        val y: FloatArray,
        val t: FloatArray,
        val b: FloatArray,
        val az: FloatArray,
        val alt: FloatArray,
        val p: FloatArray
    )

    // ============================================================
    // Public API
    // ============================================================
    fun extractFromSVC(path: String): FloatArray {
        val file = java.io.File(path)
        if (!file.exists()) {
            Log.e(TAG, "SVC not found")
            return FloatArray(72)
        }

        val lines = file.readLines()
        if (lines.size <= 1) {
            Log.e(TAG, "Invalid SVC")
            return FloatArray(72)
        }

        val d = parseSVC(lines)
        return computeFeatures(d)
    }

    // ============================================================
    // Parse SVC (PaHaW format: Y X t b az alt p)
    // ============================================================
    private fun parseSVC(lines: List<String>): SVCData {
        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        val ts = ArrayList<Float>()
        val bs = ArrayList<Float>()
        val azs = ArrayList<Float>()
        val alts = ArrayList<Float>()
        val ps = ArrayList<Float>()

        for (i in 1 until lines.size) {
            val parts = lines[i].trim().split(" ")
            if (parts.size < 7) continue

            // PaHaW format: Y X timestamp button azimuth altitude pressure
            ys.add(parts[0].toFloat())
            xs.add(parts[1].toFloat())
            ts.add(parts[2].toFloat())
            bs.add(parts[3].toFloat())
            azs.add(parts[4].toFloat())
            alts.add(parts[5].toFloat())
            ps.add(parts[6].toFloat())
        }

        return SVCData(
            x = xs.toFloatArray(),
            y = ys.toFloatArray(),
            t = ts.toFloatArray(),
            b = bs.toFloatArray(),
            az = azs.toFloatArray(),
            alt = alts.toFloatArray(),
            p = ps.toFloatArray()
        )
    }

    // ============================================================
    // UTILITIES
    // ============================================================
    private fun diff(a: FloatArray): FloatArray {
        if (a.size < 2) return FloatArray(0)
        val out = FloatArray(a.size - 1)
        for (i in out.indices) out[i] = a[i + 1] - a[i]
        return out
    }

    private fun mean(a: FloatArray): Float {
        if (a.isEmpty()) return 0f
        var s = 0f
        for (v in a) s += v
        return s / a.size
    }

    private fun std(a: FloatArray): Float {
        if (a.size < 2) return 0f
        val m = mean(a)
        var s = 0f
        for (v in a) s += (v - m).pow(2)
        return sqrt(s / a.size)
    }

    private fun percentile(a: FloatArray, perc: Int): Float {
        if (a.isEmpty()) return 0f
        val s = a.sorted()
        val idx = ((perc / 100.0) * (s.size - 1)).toInt()
        return s[idx]
    }

    private fun zeroCrossings(a: FloatArray): Int {
        if (a.size < 2) return 0
        var c = 0
        for (i in 1 until a.size) {
            if (a[i] * a[i - 1] < 0) c++
        }
        return c
    }

    // ============================================================
    // Curvature — Python-accurate
    // ============================================================
    private fun computeCurvature(dx: FloatArray, dy: FloatArray): FloatArray {
        if (dx.size < 3) return FloatArray(0)

        val ddx = diff(dx)
        val ddy = diff(dy)
        val L = minOf(dx.size, dy.size, ddx.size + 1, ddy.size + 1) - 1
        if (L <= 2) return FloatArray(0)

        val out = FloatArray(L - 1)
        for (i in 1 until L) {
            val dx1 = dx[i]
            val dy1 = dy[i]
            val ddx1 = ddx[i - 1]
            val ddy1 = ddy[i - 1]

            val num = abs(dx1 * ddy1 - dy1 * ddx1)
            val denom = (dx1 * dx1 + dy1 * dy1).toDouble().pow(1.5) + 1e-9
            out[i - 1] = (num / denom).toFloat()
        }
        return out
    }

    // ============================================================
    // Stroke segmentation
    // ============================================================
    private fun extractStrokes(b: FloatArray, t: FloatArray): Pair<FloatArray, FloatArray> {
        val dur = ArrayList<Float>()
        val len = ArrayList<Float>()

        var i = 0
        while (i < b.size) {
            if (b[i] == 1f) {
                val start = i
                while (i < b.size && b[i] == 1f) i++
                val end = i - 1
                dur.add(t[end] - t[start])
                len.add((end - start).toFloat())
            } else i++
        }

        return dur.toFloatArray() to len.toFloatArray()
    }

    private fun segmentPressure(p: FloatArray): Triple<FloatArray, FloatArray, FloatArray> {
        if (p.size < 20) return Triple(p, p, p)
        val i1 = (0.2 * p.size).toInt()
        val i2 = (0.8 * p.size).toInt()
        return Triple(
            p.copyOfRange(0, i1),
            p.copyOfRange(i1, i2),
            p.copyOfRange(i2, p.size)
        )
    }

    // ============================================================
    // FINAL FEATURE EXTRACTION — UPDATED WITH AZIMUTH & ALTITUDE (72 FEATURES)
    // ============================================================
    private fun computeFeatures(d: SVCData): FloatArray {

        val x = d.x
        val y = d.y
        val t = d.t
        val b = d.b
        val az = d.az
        val alt = d.alt
        val p = d.p

        val n = x.size
        if (n < 5) return FloatArray(72)

        val dx = diff(x)
        val dy = diff(y)
        val dt = diff(t).map { if (it <= 0f) 1e-6f else it }.toFloatArray()

        // ================= Velocities =======================
        val vel = FloatArray(dx.size) { i ->
            sqrt(dx[i] * dx[i] + dy[i] * dy[i]) / dt[i]
        }
        val velX = FloatArray(dx.size) { i -> dx[i] / dt[i] }
        val velY = FloatArray(dx.size) { i -> dy[i] / dt[i] }

        // ================= Accelerations =======================
        val acc = diff(vel)
        val accX = diff(velX)
        val accY = diff(velY)

        // ================= Jerks =======================
        val jerk = diff(acc)
        val jerkX = diff(accX)
        val jerkY = diff(accY)

        // ================= Curvature =======================
        val curvature = computeCurvature(dx, dy)

        // ================= Pressure =======================
        val dp = diff(p)
        val dpDt = FloatArray(dp.size) { i -> dp[i] / dt[i] }

        // ================= Azimuth & Altitude derivatives =======================
        val daz = diff(az)
        val dalt = diff(alt)
        val dazDt = FloatArray(daz.size) { i -> daz[i] / dt[i] }
        val daltDt = FloatArray(dalt.size) { i -> dalt[i] / dt[i] }

        // ================= Duration =======================
        val duration = (t.last() - t.first()).coerceAtLeast(1f)

        // ================= Strokes =======================
        val (strokeDur, strokeLen) = extractStrokes(b, t)

        // Pressure segmentation
        val (pRise, pMain, pFall) = segmentPressure(p)

        val out = ArrayList<Float>()

        // ============================================================
        // FEATURE ORDER (72 FEATURES)
        // ============================================================

        // 1) Basic kinematics (5)
        out += mean(vel)
        out += std(vel)
        out += percentile(vel, 99)
        out += percentile(vel, 1)
        out += (percentile(vel, 99) - percentile(vel, 1))

        // 2) Vel_x, Vel_y (4)
        out += mean(velX)
        out += std(velX)
        out += mean(velY)
        out += std(velY)

        // 3) Acceleration (6)
        out += mean(acc)
        out += std(acc)
        out += mean(accX)
        out += std(accX)
        out += mean(accY)
        out += std(accY)

        // 4) Jerk (6)
        out += mean(jerk)
        out += std(jerk)
        out += mean(jerkX)
        out += std(jerkX)
        out += mean(jerkY)
        out += std(jerkY)

        // 5) Curvature (2)
        out += mean(curvature)
        out += std(curvature)

        // 6) NCV/NCA/NCP (6)
        val NCV = zeroCrossings(diff(vel))
        val NCA = zeroCrossings(acc)
        val NCP = zeroCrossings(dpDt)
        out += NCV.toFloat()
        out += NCA.toFloat()
        out += NCP.toFloat()
        out += NCV / duration
        out += NCA / duration
        out += NCP / duration

        // 7) Pressure stats (6)
        out += mean(p)
        out += std(p)
        out += percentile(p, 50)
        out += percentile(p, 99)
        out += percentile(p, 1)
        out += (percentile(p, 99) - percentile(p, 1))

        // 8) Pressure rise/main/fall (12)
        fun addSeg(seg: FloatArray) {
            out += mean(seg)
            out += std(seg)
            out += percentile(seg, 99)
            out += percentile(seg, 1)
        }
        addSeg(pRise)
        addSeg(pMain)
        addSeg(pFall)

        // 9) Overshoot (1)
        out += (p.max() - percentile(p, 50))

        // 10) Stroke features (5)
        out += strokeLen.size.toFloat()
        out += mean(strokeLen)
        out += std(strokeLen)
        out += mean(strokeDur)
        out += std(strokeDur)

        // 11) Timing (2)
        out += duration
        out += (b.count { it == 1f }.toFloat() / b.size.toFloat())

        // 12) Pressure correlations (3)
        val L = minOf(p.size - 1, velX.size)
        val L2 = minOf(p.size - 2, acc.size)

        fun corr(a: FloatArray, b: FloatArray, len: Int): Float {
            if (len <= 5) return 0f
            val aa = a.copyOfRange(0, len)
            val bb = b.copyOfRange(0, len)
            val ma = mean(aa)
            val mb = mean(bb)
            var num = 0f
            var da = 0f
            var db = 0f
            for (i in 0 until len) {
                val xa = aa[i] - ma
                val xb = bb[i] - mb
                num += xa * xb
                da += xa * xa
                db += xb * xb
            }
            return if (da == 0f || db == 0f) 0f else num / sqrt(da * db)
        }

        out += corr(p, velX, L)
        out += corr(p, velY, L)
        out += corr(p, acc, L2)

        // 13) Azimuth features (7)
        out += mean(az)
        out += std(az)
        out += percentile(az, 99)
        out += percentile(az, 1)
        out += (percentile(az, 99) - percentile(az, 1))
        out += mean(dazDt)
        out += std(dazDt)

        // 14) Altitude features (7)
        out += mean(alt)
        out += std(alt)
        out += percentile(alt, 99)
        out += percentile(alt, 1)
        out += (percentile(alt, 99) - percentile(alt, 1))
        out += mean(daltDt)
        out += std(daltDt)

        // Final result (72 features)
        val arr = out.toFloatArray()
        for (i in arr.indices) if (!arr[i].isFinite()) arr[i] = 0f

        Log.d(TAG, "Generated ${arr.size} features")
        return arr
    }
}