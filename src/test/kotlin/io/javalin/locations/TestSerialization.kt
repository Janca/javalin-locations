package io.javalin.locations

import kotlin.reflect.full.declaredMemberProperties

class A(val test: Array<String> = emptyArray(), val testC: IntArray = intArrayOf(), val testD: List<Int> = emptyList())

fun main() {

    val classAField = A::class.declaredMemberProperties.first()
    val classAFieldType = classAField.returnType

/*    val test: String = "this is a test"
    val testB: Array<String>? = test.cast(classAFieldType)
    println(testB?.joinToString())

    val classAFieldC = A::class.declaredMemberProperties.filterNot { it == classAField }.first()
    val classAFieldCType = classAFieldC.returnType

    val test2: String = "1"
    val testC: IntArray? = test2.cast(classAFieldCType)
    println(testC?.joinToString())

    val classAFieldD = A::class.declaredMemberProperties.first { it.name == "testD" }
    val classAFieldDType = classAFieldD.returnType

    val test3: List<String> = listOf("2", "3")
    val testD: List<Int>? = test3.cast(classAFieldDType)
    println(testD?.joinToString())*/

}