package com.leslo.aether

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AetherClockWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = LocalDate.now()
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        // Fallback to Hardinxveld-Giessendam coordinates
        var lat = 51.84
        var lng = 4.84

        try {
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            bestLocation?.let {
                lat = it.latitude
                lng = it.longitude
            }
        } catch (e: SecurityException) {
            // No permission yet, use Hardinxveld-Giessendam fallback
        }

        val sunrise = SolarCalculator.calculate(now, lat, lng, true)
        val sunset = SolarCalculator.calculate(now, lat, lng, false)
        val currentTime = LocalTime.now()

        val (nextEvent, eventTime) = when {
            sunrise != null && currentTime.isBefore(sunrise) -> "SUNRISE" to sunrise
            sunset != null && currentTime.isBefore(sunset) -> "SUNSET" to sunset
            else -> {
                val tomorrowSunrise = SolarCalculator.calculate(now.plusDays(1), lat, lng, true)
                "SUNRISE" to (tomorrowSunrise ?: LocalTime.of(6, 0))
            }
        }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val eventTimeString = eventTime.format(timeFormatter)

        provideContent {
            GlanceTheme {
                MyContent(nextEvent, eventTimeString)
            }
        }
    }

    @Composable
    private fun MyContent(nextEvent: String, eventTime: String) {
        val context = LocalContext.current
        val onSurfaceColor = GlanceTheme.colors.onSurface.getColor(context)
        val primaryColor = GlanceTheme.colors.primary.getColor(context)
        val onSurfaceVariantColor = GlanceTheme.colors.onSurfaceVariant.getColor(context)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Time Section
            Box(modifier = GlanceModifier.wrapContentHeight().fillMaxWidth()) {
                val rv = RemoteViews(context.packageName, R.layout.text_clock_layout)
                rv.setTextColor(R.id.hours, onSurfaceColor.toArgb())
                rv.setTextColor(R.id.minutes, primaryColor.toArgb())
                rv.setTextColor(R.id.am_pm, onSurfaceVariantColor.toArgb())
                
                AndroidRemoteViews(remoteViews = rv)
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Date Section
            val dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())).uppercase()
            Text(
                text = dateString,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Divider Line
            Box(
                modifier = GlanceModifier
                    .width(30.dp)
                    .height(2.dp)
                    .background(GlanceTheme.colors.primary)
            ) {
                // Empty box for divider
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Celestial Info Section
            Text(
                text = "SOLAR CYCLE",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "$nextEvent > $eventTime",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class AetherClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AetherClockWidget()
}
