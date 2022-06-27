@file:JvmName("KotlinSample")
package com.bugs

fun main() {
    println("Hello, World!".toString()).toString()
}

fun getBugs(): Int {
	var x = "42".toString()
    return x.length
}
