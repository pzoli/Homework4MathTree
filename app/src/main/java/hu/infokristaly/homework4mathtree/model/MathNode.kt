package hu.infokristaly.homework4mathtree.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/**
 * Egységes érték típus a fa csomópontok kiértékeléséhez (Szám, Szöveg, Boole-érték).
 */
sealed class Value {
    data class Number(val value: Double) : Value()
    data class Text(val value: String) : Value()
    data class Bool(val value: Boolean) : Value()

    fun toDouble(): Double = when (this) {
        is Number -> value
        is Bool -> if (value) 1.0 else 0.0
        is Text -> value.toDoubleOrNull() ?: 0.0
    }

    fun toBoolean(): Boolean = when (this) {
        is Bool -> value
        is Number -> value != 0.0
        is Text -> value.isNotEmpty() && value.lowercase() != "false" && value.lowercase() != "hamis" && value != "0"
    }

    fun toDisplayString(): String = when (this) {
        is Number -> if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.4f", value)
        is Text -> value
        is Bool -> if (value) "true (igaz)" else "false (hamis)"
    }
}

/**
 * Szülő osztály a matematikai kifejezésfához (Math Tree Node).
 * Ebben a szülő osztályban van deklarálva a getValue() metódus,
 * amit az egyes művelet és konstans osztályok megvalósítanak.
 */
sealed class MathNode {
    /**
     * Visszaadja a művelet vagy konstans kiszámított értékét.
     */
    abstract fun getValue(): Value

    /**
     * A művelet szimbóluma (pl. +, -, *, /, ^, √, vagy maga a szám/szöveg).
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
     * Ellenőrzi, hogy a csomópont levele-e.
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
    override fun getValue(): Value {
        val l = left.getValue()
        val r = right.getValue()
        return if (l is Value.Text || r is Value.Text) {
            Value.Text(l.toDisplayString() + r.toDisplayString())
        } else {
            Value.Number(l.toDouble() + r.toDouble())
        }
    }
}

/**
 * Kivonás művelet (-)
 */
class SubtractionNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "-"
    override val description: String = "Kivonás (-)"
    override fun getValue(): Value = Value.Number(left.getValue().toDouble() - right.getValue().toDouble())
}

/**
 * Szorzás művelet (*)
 */
class MultiplicationNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "*"
    override val description: String = "Szorzás (*)"
    override fun getValue(): Value = Value.Number(left.getValue().toDouble() * right.getValue().toDouble())
}

/**
 * Osztás művelet (/)
 */
class DivisionNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "/"
    override val description: String = "Osztás (/)"
    override fun getValue(): Value {
        val denom = right.getValue().toDouble()
        if (denom == 0.0) {
            throw ArithmeticException("Nullával való osztás!")
        }
        return Value.Number(left.getValue().toDouble() / denom)
    }
}

/**
 * Hatványozás művelet (^)
 */
class ExponentiationNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "^"
    override val description: String = "Hatványozás (^)"
    override fun getValue(): Value = Value.Number(left.getValue().toDouble().pow(right.getValue().toDouble()))
}

/**
 * Gyökvonás művelet (√ vagy root)
 */
class RootNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "√"
    override val description: String = "Gyökvonás (√)"
    override fun getValue(): Value {
        val degree = left.getValue().toDouble()
        val radicand = right.getValue().toDouble()
        if (degree == 0.0) {
            throw ArithmeticException("Nulladik gyök nem értelmezett!")
        }
        if ((radicand < 0) && (degree % 2.0 == 0.0)) {
            throw ArithmeticException("Páros fokú gyök negatív számból nem értelmezett a valós számok halmazán!")
        }
        return Value.Number(radicand.pow(1.0 / degree))
    }
}

// ================= LOGIKAI ÉS ÖSSZEHASONLÍTÓ MŰVELETEK =================

class EqualNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "=="
    override val description: String = "Egyenlő (==)"
    override fun getValue(): Value {
        val l = left.getValue()
        val r = right.getValue()
        val isEqual = when {
            l is Value.Text || r is Value.Text -> l.toDisplayString() == r.toDisplayString()
            l is Value.Bool || r is Value.Bool -> l.toBoolean() == r.toBoolean()
            else -> abs(l.toDouble() - r.toDouble()) < 1e-9
        }
        return Value.Bool(isEqual)
    }
}

class NotEqualNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "!="
    override val description: String = "Nem egyenlő (!=)"
    override fun getValue(): Value {
        val eq = EqualNode(left, right).getValue() as Value.Bool
        return Value.Bool(!eq.value)
    }
}

class LessThanNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "<"
    override val description: String = "Kisebb (<)"
    override fun getValue(): Value {
        val l = left.getValue()
        val r = right.getValue()
        val res = if (l is Value.Text && r is Value.Text) {
            l.value < r.value
        } else {
            l.toDouble() < r.toDouble()
        }
        return Value.Bool(res)
    }
}

class GreaterThanNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = ">"
    override val description: String = "Nagyobb (>)"
    override fun getValue(): Value {
        val l = left.getValue()
        val r = right.getValue()
        val res = if (l is Value.Text && r is Value.Text) {
            l.value > r.value
        } else {
            l.toDouble() > r.toDouble()
        }
        return Value.Bool(res)
    }
}

class LessOrEqualNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "<="
    override val description: String = "Kisebb vagy egyenlő (<=)"
    override fun getValue(): Value {
        val l = left.getValue()
        val r = right.getValue()
        val res = if (l is Value.Text && r is Value.Text) {
            l.value <= r.value
        } else {
            l.toDouble() <= r.toDouble()
        }
        return Value.Bool(res)
    }
}

class GreaterOrEqualNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = ">="
    override val description: String = "Nagyobb vagy egyenlő (>=)"
    override fun getValue(): Value {
        val l = left.getValue()
        val r = right.getValue()
        val res = if (l is Value.Text && r is Value.Text) {
            l.value >= r.value
        } else {
            l.toDouble() >= r.toDouble()
        }
        return Value.Bool(res)
    }
}

class AndNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "and"
    override val description: String = "Logikai És (and/&&)"
    override fun getValue(): Value = Value.Bool(left.getValue().toBoolean() && right.getValue().toBoolean())
}

class OrNode(left: MathNode, right: MathNode) : OperationNode(left, right) {
    override val symbol: String = "or"
    override val description: String = "Logikai Vagy (or/||)"
    override fun getValue(): Value = Value.Bool(left.getValue().toBoolean() || right.getValue().toBoolean())
}

class NotNode(val operand: MathNode) : MathNode() {
    override val symbol: String = "not"
    override val description: String = "Logikai Tagadás (not/!)"
    override val left: MathNode get() = operand
    override val right: MathNode? get() = null
    override fun getValue(): Value = Value.Bool(!operand.getValue().toBoolean())
}

// ================= LEVELEK (SZÁM, SZÖVEG, VÁLTOZÓ, FÜGGVÉNYHÍVÁS) =================

class NumberNode(val numberValue: Double) : MathNode() {
    override val symbol: String = if (numberValue % 1.0 == 0.0) {
        numberValue.toLong().toString()
    } else {
        numberValue.toString()
    }
    override val description: String = "Konstans szám ($symbol)"
    override fun getValue(): Value = Value.Number(numberValue)
}

class BooleanNode(val booleanValue: Boolean) : MathNode() {
    override val symbol: String = if (booleanValue) "true" else "false"
    override val description: String = if (booleanValue) "Konstans logikai érték (true / igaz)" else "Konstans logikai érték (false / hamis)"
    override fun getValue(): Value = Value.Bool(booleanValue)
}

class StringNode(val stringValue: String) : MathNode() {
    override val symbol: String = "\"$stringValue\""
    override val description: String = "Konstans szöveg (\"$stringValue\")"
    override fun getValue(): Value = Value.Text(stringValue)
}

class VariableNode(
    val name: String,
    val variableValue: Value,
) : MathNode() {
    override val symbol: String = name
    override val description: String = "Változó ($name = \"${variableValue.toDisplayString()}\")"
    override fun getValue(): Value = variableValue
}

class FunctionCallNode(
    val functionName: String,
    val argumentNodes: List<MathNode>,
    val functionBodyTree: MathNode,
) : MathNode() {
    override val symbol: String = if (argumentNodes.isEmpty()) {
        functionName
    } else {
        "$functionName(${argumentNodes.joinToString(", ") { it.symbol }})"
    }
    override val description: String = "Függvényhívás: $functionName"
    override val left: MathNode? get() = argumentNodes.firstOrNull()
    override val right: MathNode get() = functionBodyTree
    override fun getValue(): Value = functionBodyTree.getValue()
}

abstract class TrigonometricNode(
    val operand: MathNode,
    val isDegreeMode: Boolean = false,
) : MathNode() {
    override val left: MathNode get() = operand
    override val right: MathNode? get() = null

    protected fun toRadians(angle: Double): Double {
        return if (isDegreeMode) Math.toRadians(angle) else angle
    }
}

class SinNode(
    operand: MathNode,
    isDegreeMode: Boolean = false,
) : TrigonometricNode(operand, isDegreeMode) {
    override val symbol: String = "sin"
    override val description: String = if (isDegreeMode) "Szinusz (sin, Fok)" else "Szinusz (sin, Rad)"
    override fun getValue(): Value = Value.Number(kotlin.math.sin(toRadians(operand.getValue().toDouble())))
}

class CosNode(
    operand: MathNode,
    isDegreeMode: Boolean = false,
) : TrigonometricNode(operand, isDegreeMode) {
    override val symbol: String = "cos"
    override val description: String = if (isDegreeMode) "Koszinusz (cos, Fok)" else "Koszinusz (cos, Rad)"
    override fun getValue(): Value = Value.Number(kotlin.math.cos(toRadians(operand.getValue().toDouble())))
}

class TanNode(
    operand: MathNode,
    isDegreeMode: Boolean = false,
) : TrigonometricNode(operand, isDegreeMode) {
    override val symbol: String = "tan"
    override val description: String = if (isDegreeMode) "Tangens (tan, Fok)" else "Tangens (tan, Rad)"
    override fun getValue(): Value {
        val rad = toRadians(operand.getValue().toDouble())
        val cosVal = kotlin.math.cos(rad)
        if (kotlin.math.abs(cosVal) < 1e-12) {
            throw ArithmeticException("Tangens nem értelmezett kπ + π/2 pontokban!")
        }
        return Value.Number(kotlin.math.tan(rad))
    }
}

class CotNode(
    operand: MathNode,
    isDegreeMode: Boolean = false,
) : TrigonometricNode(operand, isDegreeMode) {
    override val symbol: String = "cot"
    override val description: String = if (isDegreeMode) "Kotangens (cot, Fok)" else "Kotangens (cot, Rad)"
    override fun getValue(): Value {
        val rad = toRadians(operand.getValue().toDouble())
        val sinVal = kotlin.math.sin(rad)
        if (kotlin.math.abs(sinVal) < 1e-12) {
            throw ArithmeticException("Kotangens nem értelmezett kπ pontokban!")
        }
        return Value.Number(1.0 / kotlin.math.tan(rad))
    }
}
