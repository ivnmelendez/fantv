package com.primetv.app.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromList(list: List<String>?): String = list?.joinToString("|") ?: ""
    @TypeConverter fun toList(s: String): List<String> = if (s.isBlank()) emptyList() else s.split("|")
}
