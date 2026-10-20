package com.fintrack.kavindu.core.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

// Room stores a LocalDate as "yyyy-MM-dd" text. That text sorts in date order, so date range
// queries work. Enums need no converter: Room stores them by name.
class Converters {

    @TypeConverter
    fun localDateToText(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun textToLocalDate(text: String?): LocalDate? = text?.let(LocalDate::parse)
}
