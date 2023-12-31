package dev.kikugie.worldrenderer.property
abstract class Property<T>(defaultValue: T) {
    protected var value: T = defaultValue
    abstract fun set(newValue: T)
    abstract fun get(): T
}