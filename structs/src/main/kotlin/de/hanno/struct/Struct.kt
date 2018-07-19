package de.hanno.struct

import org.lwjgl.BufferUtils
import java.nio.ByteBuffer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Sizable {
    val sizeInBytes: Int
}

interface Bufferable: Sizable {
    val buffer: ByteBuffer
}

interface Struct: Bufferable {
    val memberStructs: MutableList<StructProperty<*>>
    fun getCurrentLocalByteOffset() = memberStructs.sumBy { it.sizeInBytes }
}

fun <T: Bufferable> T.copyTo(target: T) {
    val tempArray = ByteArray(sizeInBytes)
    this.buffer.rewind()
    this.buffer.get(tempArray, 0, sizeInBytes)
    target.buffer.put(tempArray, 0, sizeInBytes)
}
fun <T: Bufferable> T.copyFrom(target: T) {
    target.copyTo(this)
}

interface SimpleStruct : Struct {
    val parent: Struct?
    val baseByteOffset: Int

    operator fun Int.provideDelegate(thisRef: SimpleStruct, prop: KProperty<*>): IntProperty {
        return IntProperty(getCurrentLocalByteOffset()).apply { thisRef.register(this@apply) }
    }
    operator fun Float.provideDelegate(thisRef: SimpleStruct, prop: KProperty<*>): FloatProperty {
        return FloatProperty(getCurrentLocalByteOffset()).apply { thisRef.register(this@apply) }
    }
    operator fun Double.provideDelegate(thisRef: SimpleStruct, prop: KProperty<*>): DoubleProperty {
        return DoubleProperty(getCurrentLocalByteOffset()).apply { thisRef.register(this@apply) }
    }
    operator fun Long.provideDelegate(thisRef: SimpleStruct, prop: KProperty<*>): LongProperty {
        return LongProperty(getCurrentLocalByteOffset()).apply { thisRef.register(this@apply) }
    }

    operator fun <FIELD: SimpleStruct> FIELD.provideDelegate(thisRef: SimpleStruct, prop: KProperty<*>): StructProperty<FIELD> {
        return object : StructProperty<FIELD>(sizeInBytes) {
            var currentRef = this@provideDelegate

            override fun getValue(thisRef: SimpleStruct, property: KProperty<*>): FIELD {
                return currentRef
            }

            override fun setValue(thisRef: SimpleStruct, property: KProperty<*>, value: FIELD) {
                currentRef = value
            }

        }.apply { thisRef.register(this@apply) }
    }

    fun register(structProperty: StructProperty<*>) {
        memberStructs.add(structProperty)
    }
}

abstract class BaseStruct(override val parent: Struct? = null): SimpleStruct {
    override val memberStructs = mutableListOf<StructProperty<*>>()
    override val sizeInBytes by lazy {
        memberStructs.sumBy { it.sizeInBytes }
    }
//    override val sizeInBytes
//        get() = memberStructs.sumBy { it.sizeInBytes }
    override val baseByteOffset = parent?.getCurrentLocalByteOffset() ?: 0
    override val buffer by lazy {
        parent?.buffer ?: BufferUtils.createByteBuffer(sizeInBytes)
    }
}

abstract class SlidingWindow : BaseStruct() {
    override var parent: Struct? = null
        set(value) {
            field = value
            buffer = value?.buffer
            baseByteOffset = value?.buffer?.position() ?: 0
        }
    override val memberStructs = mutableListOf<StructProperty<*>>()
    override val sizeInBytes by lazy {
        memberStructs.sumBy { it.sizeInBytes }
    }
    override var baseByteOffset = 0
    override var buffer: ByteBuffer? = parent?.buffer
}

interface GenericStructProperty<OWNER_TYPE: SimpleStruct, FIELD_TYPE> : ReadWriteProperty<OWNER_TYPE, FIELD_TYPE>

abstract class StructProperty<FIELD_TYPE>(val sizeInBytes: Int): GenericStructProperty<SimpleStruct, FIELD_TYPE> {
    open var localByteOffset = 0
        protected set
}
class IntProperty(override var localByteOffset: Int): StructProperty<Int>(Integer.BYTES) {

    override fun setValue(thisRef: SimpleStruct, property: KProperty<*>, value: Int) {
        thisRef.buffer.putInt(thisRef.baseByteOffset + localByteOffset, value)
    }
    override fun getValue(thisRef: SimpleStruct, property: KProperty<*>) = thisRef.buffer.getInt(thisRef.baseByteOffset + localByteOffset)
}

class FloatProperty(override var localByteOffset: Int): StructProperty<Float>(java.lang.Float.BYTES) {

    override fun setValue(thisRef: SimpleStruct, property: KProperty<*>, value: Float) {
        thisRef.buffer.putFloat(thisRef.baseByteOffset + localByteOffset, value)
    }
    override fun getValue(thisRef: SimpleStruct, property: KProperty<*>) = thisRef.buffer.getFloat(thisRef.baseByteOffset + localByteOffset)
}

class DoubleProperty(override var localByteOffset: Int): StructProperty<Double>(java.lang.Double.BYTES) {

    override fun setValue(thisRef: SimpleStruct, property: KProperty<*>, value: Double) {
        thisRef.buffer.putDouble(thisRef.baseByteOffset + localByteOffset, value)
    }
    override fun getValue(thisRef: SimpleStruct, property: KProperty<*>) = thisRef.buffer.getDouble(thisRef.baseByteOffset + localByteOffset)
}
class LongProperty(override var localByteOffset: Int): StructProperty<Long>(java.lang.Long.BYTES) {

    override fun setValue(thisRef: SimpleStruct, property: KProperty<*>, value: Long) {
        thisRef.buffer.putLong(thisRef.baseByteOffset + localByteOffset, value)
    }
    override fun getValue(thisRef: SimpleStruct, property: KProperty<*>) = thisRef.buffer.getLong(thisRef.baseByteOffset + localByteOffset)
}