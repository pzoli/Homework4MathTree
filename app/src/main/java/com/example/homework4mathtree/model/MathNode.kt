package com.example.homework4mathtree.model

import kotlin.math.pow

/**
 * Szülő osztály a matematikai kifejezésfához (Math Tree Node).
 * Ebben a szülő osztályban van deklarálva a getValue() metódus,
 * amit az egyes művelet és konstans osztályok megvalósítanak.
 */
sealed class MathNode {
    /**
     * Visszaadja a művelet vagy konstans kiszámított értékét.
     */
    abstract fun getValue(): Double

    /**
     * A művelet szimbóluma (pl. +, -, *, /, ^, √, vagy maga a szám).
     */
    abstract val symbol: String

    /**
     * A művelet leíró megnevezése magyarul.
     */
    abstract val description: String

    /**
     * A bal oldali gyermek elem (ha van).
     */
    open val left: MathNode? get() = null

    /**
     * A jobb oldali gyermek elem (ha van).
     */
    open val right: MathNode? get() = null

    /**
     * Ellenőrzi, hogy a csomópont levele-e (konstans szám).
     */
    val isLeaf: Boolean get() = (left == null) && (right == null)
}

/**
 * Absztrakt szülő osztály a bináris műveleteknek.
 */
abstract class OperationNode(
    override val left: MathNode,
    override val right: MathNode,
) : MathNode()

/**
 * Összeadás művelet (+)
 */
class AdditionNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "+"
    override val description: String = "Összeadás (+)"
    override fun getValue(): Double = left.getValue() + right.getValue()
}

/**
 * Kivonás művelet (-)
 */
class SubtractionNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "-"
    override val description: String = "Kivonás (-)"
    override fun getValue(): Double = left.getValue() - right.getValue()
}

/**
 * Szorzás művelet (*)
 */
class MultiplicationNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "*"
    override val description: String = "Szorzás (*)"
    override fun getValue(): Double = left.getValue() * right.getValue()
}

/**
 * Osztás művelet (/)
 */
class DivisionNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "/"
    override val description: String = "Osztás (/)"
    override fun getValue(): Double {
        val denom = right.getValue()
        if (denom == 0.0) {
            throw ArithmeticException("Nullával való osztás!")
        }
        return left.getValue() / denom
    }
}

/**
 * Hatványozás művelet (^)
 */
class ExponentiationNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "^"
    override val description: String = "Hatványozás (^)"
    override fun getValue(): Double = left.getValue().pow(right.getValue())
}

/**
 * Gyökvonás művelet (√ vagy root)
 * Bal oldali ág: a gyök foka (pl. 2 a négyzetgyöknél, 3 a köbgyöknél).
 * Jobb oldali ág: a gyökalatti kifejezés.
 */
class RootNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "√"
    override val description: String = "Gyökvonás (√)"
    override fun getValue(): Double {
        val degree = left.getValue()
        val radicand = right.getValue()
        if (degree == 0.0) {
            throw ArithmeticException("Nulladik gyök nem értelmezett!")
        }
        if ((radicand < 0) && (degree % 2.0 == 0.0)) {
            throw ArithmeticException("Páros fokú gyök negatív számból nem értelmezett a valós számok halmazán!")
        }
        return radicand.pow(1.0 / degree)
    }
}

/**
 * Konstans szám érték (fa levele)
 */
class NumberNode(val numberValue: Double) : MathNode() {
    override val symbol: String = if (numberValue % 1.0 == 0.0) {
        numberValue.toLong().toString()
    } else {
        numberValue.toString()
    }
    override val description: String = "Konstans szám ($symbol)"
    override fun getValue(): Double = numberValue
}

/**
 * Változó elem a kifejezésfában (fa levele, behelyettesített értékkel)
 */
class VariableNode(
    val name: String,
    val variableValue: Double
) : MathNode() {
    override val symbol: String = name
    override val description: String = if (variableValue % 1.0 == 0.0) {
        "Változó ($name = ${variableValue.toLong()})"
    } else {
        "Változó ($name = $variableValue)"
    }
    override fun getValue(): Double = variableValue
}
