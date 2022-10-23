package com.mukul.jan.arc.delegation

import android.content.SharedPreferences
import kotlin.math.log
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface PreferenceHolder {
    val prefs: SharedPreferences
}

class BooleanPreference(
    private val key: String,
    private val default: Boolean
): ReadWriteProperty<PreferenceHolder, Boolean> {
    override fun getValue(thisRef: PreferenceHolder, property: KProperty<*>): Boolean {
        return thisRef.prefs.getBoolean(key,default)
    }
    override fun setValue(thisRef: PreferenceHolder, property: KProperty<*>, value: Boolean) {
        thisRef.prefs.edit().putBoolean(key,value).apply()
    }
}

fun booleanPreference(
    key: String,
    default: Boolean
): ReadWriteProperty<PreferenceHolder, Boolean> {
    return BooleanPreference(
        key = key,
        default = default
    )
}

//USE OF [ReadWriteProperty]
class PreferenceStore(
    override val prefs: SharedPreferences
) : PreferenceHolder {
    var loggedIn by booleanPreference(
        key = "",
        default = false
    )

    fun access() {
        //saving to prefs
        loggedIn = true
    }
}