package dev.kikugie.worldrenderer.property

abstract class Property<T>(val defaultValue: T) : () -> T {
    private val listeners = mutableListOf<(Property<T>, T) -> Unit>()
    var value = defaultValue
        set(new) {
            field = new
            listeners.forEach { it(this, new) }
        }

    fun listen(consumer: (Property<T>, T) -> Unit) {
        listeners += consumer
    }

    override fun invoke() = value
}

class IntProperty(defaultValue: Int, val min: Int, val max: Int, private val rollover: Boolean = false) :
    Property<Int>(defaultValue) {
    val span = max - min

    init {
        require(min <= max)
    }

    fun modify(by: Int) {
        value = if (rollover) {
            val rolled = value + by
            when {
                rolled > max -> rolled - span
                rolled < min -> rolled + span
                else -> rolled
            }
        } else value.coerceIn(min..max)
    }

    operator fun plusAssign(by: Int) = modify(by)
}

class DoubleProperty(defaultValue: Double, val min: Double, val max: Double, private val rollover: Boolean = false) :
    Property<Double>(defaultValue) {
    val span = max - min

    init {
        require(min <= max)
    }

    fun modify(by: Double) {
        value = if (rollover) {
            val rolled = value + by
            when {
                rolled > max -> rolled - span
                rolled < min -> rolled + span
                else -> rolled
            }
        } else value.coerceIn(min..max)
    }

    operator fun plusAssign(by: Double) = modify(by)
}