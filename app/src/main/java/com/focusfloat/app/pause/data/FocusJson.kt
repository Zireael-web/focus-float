package com.focusfloat.app.pause.data

import org.json.JSONArray

fun List<String>.toJsonArrayString(): String {
    val array = JSONArray()
    forEach { array.put(it) }
    return array.toString()
}

fun String.toStringListFromJsonArray(): List<String> {
    return toStringListFromJsonArrayOrNull().orEmpty()
}

fun String.toStringListFromJsonArrayOrNull(): List<String>? {
    if (isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }.getOrNull()
}
