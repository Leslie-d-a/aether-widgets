package com.leslo.aether

import java.time.*
import kotlin.math.*

object SolarCalculator {
    private const val ZENITH = 90.83333333333333

    fun calculate(date: LocalDate, lat: Double, lng: Double, isSunrise: Boolean): LocalTime? {
        val n = date.dayOfYear.toDouble()
        val lngHour = lng / 15.0
        val t = if (isSunrise) n + ((6.0 - lngHour) / 24.0) else n + ((18.0 - lngHour) / 24.0)

        val m = (0.9856 * t) - 3.289
        var l = m + (1.916 * sin(Math.toRadians(m))) + (0.020 * sin(Math.toRadians(2 * m))) + 282.634
        l = (l + 360) % 360

        var ra = Math.toDegrees(atan(0.91764 * tan(Math.toRadians(l))))
        ra = (ra + 360) % 360
        
        val lQuadrant = floor(l / 90.0) * 90.0
        val raQuadrant = floor(ra / 90.0) * 90.0
        ra = (ra + (lQuadrant - raQuadrant)) / 15.0

        val sinDec = 0.39782 * sin(Math.toRadians(l))
        val cosDec = cos(asin(sinDec))

        val cosH = (cos(Math.toRadians(ZENITH)) - (sinDec * sin(Math.toRadians(lat)))) / (cosDec * cos(Math.toRadians(lat)))

        if (cosH > 1) return null 
        if (cosH < -1) return null 

        val hDeg = if (isSunrise) 360.0 - Math.toDegrees(acos(cosH)) else Math.toDegrees(acos(cosH))
        val h = hDeg / 15.0
        
        val localMeanTime = (h + ra - (0.06571 * t) - 6.622 + 24) % 24
        val utcTime = (localMeanTime - lngHour + 24) % 24
        
        val totalSeconds = (utcTime * 3600).toInt()
        val utcLocalTime = LocalTime.ofSecondOfDay((totalSeconds % 86400).toLong())
        
        val utcDateTime = ZonedDateTime.of(date.atTime(utcLocalTime), ZoneId.of("UTC"))
        return utcDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalTime()
    }
}