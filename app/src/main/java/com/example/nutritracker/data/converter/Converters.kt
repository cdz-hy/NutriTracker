package com.example.nutritracker.data.converter

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter fun fromLocalDate(d: LocalDate?): String? = d?.toString()
    @TypeConverter fun toLocalDate(s: String?): LocalDate? = s?.let { LocalDate.parse(it) }
    @TypeConverter fun fromLocalDateTime(dt: LocalDateTime?): String? = dt?.toString()
    @TypeConverter fun toLocalDateTime(s: String?): LocalDateTime? = s?.let { LocalDateTime.parse(it) }
}
