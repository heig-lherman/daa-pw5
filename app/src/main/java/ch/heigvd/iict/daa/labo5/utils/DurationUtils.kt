package ch.heigvd.iict.daa.labo5.utils

import kotlin.time.Duration.Companion.milliseconds

/**
 * Extension property to calculate the duration between a given time in milliseconds and now.
 * Returns a [kotlin.time.Duration] object.
 */
inline val Long.millisecondsUntilNow get() = (System.currentTimeMillis() - this).milliseconds