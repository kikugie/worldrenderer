@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CanBeParameter")

package dev.kikugie.worldrenderer.property

class IntProperty(defaultValue: Int, val min: Int, val max: Int, private val rollover: Boolean = false) :
    Property<Int>(defaultValue) {
    val span = max - min
    val range = min..max

    init {
        require(min <= max)
    }
    override fun set(newValue: Int) {
        value = when {
            newValue in range -> newValue
            rollover -> newValue.shift()
            else -> value.coerceIn(range)
        }
    }

    override fun get() = value

    private fun Int.shift(): Int {
        val diff = (this - min) / span
        return this - diff * span
    }
}

class DoubleProperty(defaultValue: Double, val min: Double, val max: Double, private val rollover: Boolean = false) :
    Property<Double>(defaultValue) {
    val span = max - min
    val range = min..max

    init {
        require(min <= max)
    }

    override fun set(newValue: Double) {
        value = when {
            newValue in range -> newValue
            rollover -> newValue.shift()
            else -> value.coerceIn(range)
        }
    }

    override fun get() = value

    private fun Double.shift(): Double {
        val diff = (this - min) / span
        return this - diff * span
    }
}