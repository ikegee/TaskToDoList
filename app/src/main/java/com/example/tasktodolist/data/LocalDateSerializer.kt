package com.example.tasktodolist.data

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Gson adapter + helpers for [LocalDate].
 * Uses ISO-8601 (`yyyy-MM-dd`) as the canonical string form for UI and JSON.
 *
 * Ported from TaskLinkedList.
 */
class LocalDateSerializer : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

    override fun serialize(
        date: LocalDate,
        type: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        return JsonPrimitive(format(date))
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): LocalDate {
        return parse(json.asString)
    }

    companion object {
        private val TAG = LocalDateSerializer::class.java.simpleName

        fun format(date: LocalDate): String = date.toString()

        /**
         * Parse button / intent / JSON text into [LocalDate].
         * Accepts ISO-8601 and legacy `Date.toString()` formats.
         */
        fun parse(text: String?): LocalDate {
            if (text.isNullOrBlank()) return LocalDate.now()
            val trimmed = text.trim()

            try {
                return LocalDate.parse(trimmed)
            } catch (_: Exception) {
                // fall through
            }

            // Legacy: java.util.Date().toString() → "EEE MMM dd HH:mm:ss zzz yyyy" (US locale)
            val patterns = arrayOf(
                "EEE MMM dd HH:mm:ss zzz yyyy",
                "EEE MMM dd HH:mm:ss Z yyyy",
            )
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    sdf.isLenient = true
                    val parsed = sdf.parse(trimmed)
                    if (parsed != null) {
                        return parsed.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                } catch (_: ParseException) {
                    // try next
                }
            }

            Log.w(TAG, "Unparseable date '$trimmed', using today")
            return LocalDate.now()
        }

    }
}
