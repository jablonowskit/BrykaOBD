package app.brykaobd.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brykaobd.obd.ExtPidReading
import app.brykaobd.obd.GaugePids
import kotlin.math.min

private val ClusterBg = Color(0xFF10141A)
private val Face = Color(0xFF080B10)
private val Track = Color(0xFF2A3342)
private val Accent = Color(0xFF5CFFB0)
private val RpmFill = Color(0xFFFFCC33)
private val Redline = Color(0xFFFF4D4D)
private val OkGreen = Color(0xFF4CAF50)
private val WarnAmber = Color(0xFFFFB300)
private val Muted = Color(0xFF8B95A5)
private val ValueWhite = Color(0xFFF2F5F8)

/**
 * Compact motorcycle cluster for phone: RPM horseshoe + speed inside, dense strips below.
 * Does not stretch to fill empty vertical space.
 */
@Composable
fun CarDashboardCluster(
    readings: List<ExtPidReading>,
    instantL100: Double?,
    sootLoad: Double? = null,
    oilPressureBar: Double? = null,
    dtcWarn: Boolean = false,
    dtcCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    fun reading(request: String): ExtPidReading? =
        readings.firstOrNull { it.pid.request == request }

    val speed = reading(GaugePids.speed.request)?.value
    val rpm = reading(GaugePids.rpm.request)?.value
    val coolant = reading(GaugePids.coolant.request)?.value
    val oilTemp = reading(GaugePids.oilTemp.request)?.value
    val throttle = reading(GaugePids.throttle.request)?.value
    val voltage = reading(GaugePids.voltage.request)?.value
    val fuelRate = reading(GaugePids.fuelRate.request)?.value
    val odometer = reading(GaugePids.odometer.request)?.value

    Column(
        modifier
            .fillMaxSize()
            .background(ClusterBg, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .heightIn(max = 200.dp),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val w = maxWidth
            val h = minOf(maxHeight, w * 0.62f)
            val boxH = with(density) { h }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(boxH),
            ) {
            MotoSpeedo(
                speed = speed,
                rpm = rpm,
                odometer = odometer,
                maxRpm = 8000.0,
                redlineFrom = 6500.0,
                modifier = Modifier.fillMaxSize(),
            )
                if (dtcWarn) {
                    Text(
                        if (dtcCount > 0) "⚠ DTC $dtcCount" else "⚠ DTC",
                        color = Redline,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Face, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MiniBar(
                "COOL",
                formatNum(coolant, 0) + "°",
                coolant,
                scaleMin = 40.0,
                scaleMax = 120.0,
                okMin = 75.0,
                okMax = 105.0,
                warnLow = 60.0,
                warnHigh = 110.0,
                modifier = Modifier.weight(1f),
            )
            MiniBar(
                "OIL",
                formatNum(oilTemp, 0) + "°",
                oilTemp,
                scaleMin = 40.0,
                scaleMax = 150.0,
                okMin = 80.0,
                okMax = 120.0,
                warnLow = 60.0,
                warnHigh = 130.0,
                modifier = Modifier.weight(1f),
            )
            MiniBar(
                "THR",
                formatNum(throttle, 0) + "%",
                throttle,
                scaleMin = 0.0,
                scaleMax = 100.0,
                okMin = 0.0,
                okMax = 90.0,
                warnLow = null,
                warnHigh = 95.0,
                modifier = Modifier.weight(1f),
            )
            MiniBar(
                "BAR",
                formatNum(oilPressureBar, 1),
                oilPressureBar,
                scaleMin = 0.0,
                scaleMax = 5.0,
                okMin = 1.2,
                okMax = 4.5,
                warnLow = 0.8,
                warnHigh = 5.0,
                modifier = Modifier.weight(1f),
            )
            MiniBar(
                "DPF",
                formatNum(sootLoad, 0) + "%",
                sootLoad,
                scaleMin = 0.0,
                scaleMax = 100.0,
                okMin = 0.0,
                okMax = 55.0,
                warnLow = null,
                warnHigh = 75.0,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Pill(
                "BATT",
                formatNum(voltage, 1),
                "V",
                Modifier.weight(1f),
                valueColor = battColor(voltage),
            )
            Pill("FUEL", formatNum(fuelRate, 1), "L/h", Modifier.weight(1f))
            Pill("INST", formatNum(instantL100, 1), "L/100", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MotoSpeedo(
    speed: Double?,
    rpm: Double?,
    odometer: Double?,
    maxRpm: Double,
    redlineFrom: Double,
    modifier: Modifier = Modifier,
) {
    val frac = ((rpm ?: 0.0) / maxRpm).toFloat().coerceIn(0f, 1f)
    val redFrac = (redlineFrom / maxRpm).toFloat()
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = min(size.width, size.height) * 0.11f
            val pad = stroke * 0.85f
            // Horseshoe in the upper portion; leave room for digits in the open center.
            val diameter = min(size.width - pad * 2, size.height * 1.55f - pad)
            val left = (size.width - diameter) / 2f
            val top = pad * 0.4f
            val arcSize = Size(diameter, diameter)
            val start = 160f
            val sweep = 220f

            drawArc(
                color = Track,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(left, top),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Redline.copy(alpha = 0.7f),
                startAngle = start + sweep * redFrac,
                sweepAngle = sweep * (1f - redFrac),
                useCenter = false,
                topLeft = Offset(left, top),
                size = arcSize,
                style = Stroke(width = stroke * 0.55f, cap = StrokeCap.Butt),
            )
            if (frac > 0.005f) {
                drawArc(
                    color = if (frac >= redFrac) Redline else RpmFill,
                    startAngle = start,
                    sweepAngle = sweep * frac,
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Text(
                formatNum(speed, 0),
                color = ValueWhite,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                lineHeight = 52.sp,
            )
            Text("km/h", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("rpm ${formatNum(rpm, 0)}", color = Muted, fontSize = 11.sp)
            Text(
                "odo ${formatOdo(odometer)} km",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun MiniBar(
    label: String,
    text: String,
    value: Double?,
    scaleMin: Double,
    scaleMax: Double,
    okMin: Double,
    okMax: Double,
    warnLow: Double?,
    warnHigh: Double?,
    modifier: Modifier = Modifier,
) {
    val frac = if (value == null) {
        0f
    } else {
        ((value - scaleMin) / (scaleMax - scaleMin)).toFloat().coerceIn(0f, 1f)
    }
    val barColor = zoneColor(value, okMin, okMax, warnLow, warnHigh)
    Column(
        modifier
            .background(Face, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text,
                color = if (value == null) Muted else barColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().height(5.dp)) {
            val r = size.height / 2f
            drawRoundRect(color = Track, cornerRadius = CornerRadius(r, r))
            if (frac > 0.02f) {
                drawRoundRect(
                    color = barColor,
                    size = Size(size.width * frac, size.height),
                    cornerRadius = CornerRadius(r, r),
                )
            }
        }
    }
}

/** Green in normal band; amber in warn band; red outside. */
private fun zoneColor(
    value: Double?,
    okMin: Double,
    okMax: Double,
    warnLow: Double?,
    warnHigh: Double?,
): Color {
    if (value == null) return Track
    if (value in okMin..okMax) return OkGreen
    val inWarnLow = warnLow != null && value >= warnLow && value < okMin
    val inWarnHigh = warnHigh != null && value > okMax && value <= warnHigh
    if (inWarnLow || inWarnHigh) return WarnAmber
    return Redline
}

@Composable
private fun Pill(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Accent,
) {
    Column(
        modifier
            .background(Face, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            color = if (value == "—") Muted else valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(unit, color = Muted, fontSize = 9.sp)
    }
}

private fun battColor(voltage: Double?): Color {
    if (voltage == null) return Track
    // Charging / healthy running ~13.5–14.8 V; idle battery ~12.2–12.8; low <12.0
    return when {
        voltage in 13.2..14.8 -> OkGreen
        voltage in 12.2..13.2 -> WarnAmber
        voltage in 14.8..15.5 -> WarnAmber
        else -> Redline
    }
}

private fun formatNum(value: Double?, digits: Int): String {
    if (value == null) return "—"
    if (digits <= 0) return value.toLong().toString()
    var f = 1.0
    repeat(digits) { f *= 10.0 }
    return ((value * f).toLong() / f).toString()
}

private fun formatOdo(value: Double?): String {
    if (value == null) return "—"
    return value.toLong().toString()
}
