package dev.kikugie.worldrenderer.property

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PropertyDelegate<T>(private val property: Property<T>) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T = this.property.get()

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.property.set(value)
    }
}