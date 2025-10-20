package com.devlabs.devlabsbackend.core.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class RuntimeLoggerDelegate : ReadOnlyProperty<Any, Logger> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Logger {
        return LoggerFactory.getLogger(thisRef::class.java)
    }
}

fun logger(): ReadOnlyProperty<Any, Logger> = RuntimeLoggerDelegate()

inline fun <reified T : Any> staticLogger(): Logger = LoggerFactory.getLogger(T::class.java)
