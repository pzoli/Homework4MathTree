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
 * Fő ősosztály a kifejezésfához (Expression Tree Node).
 * Nyújtja a kiszámított getValue() metódust, a szimbólumot, leírást és a levelek azonosítását.
 */
abstract class ExpressionNode {
    abstract fun getValue(): Value
    abstract val symbol: String
    abstract val description: String
    open val isLeaf: Boolean get() = false
}

/** Alias a visszamenőleges kompatibilitás érdekében */
typealias MathNode = ExpressionNode

/**
 * Absztrakt ősosztály az egy-operandusú (Unáris) műveletekhez.
 * Nem rendelkezik 'left' és 'right' mezőkkel, kizárólag egyetlen 'operand' kifejezéssel.
 */
abstract class UnaryOperationNode(
    val operand: ExpressionNode
) : ExpressionNode() {
    override val isLeaf: Boolean get() = false
}

/**
 * Absztrakt ősosztály a két-operandusú (Bináris) műveletekhez.
 * Rendelkezik 'left' és 'right' gyermek kifejezésekkel.
 */
abstract class BinaryOperationNode(
    val left: ExpressionNode,
    val right: ExpressionNode
) : ExpressionNode() {
    override val isLeaf: Boolean get() = false
}

/** Korábbi név-kompatibilitási alias */
typealias OperationNode = BinaryOperationNode

// ================= BINÁRIS MŰVELETEK =================

class AdditionNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
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

class SubtractionNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
    override val symbol: String = "-"
    override val description: String = "Kivonás (-)"
    override fun getValue(): Value = Value.Number(left.getValue().toDouble() - right.getValue().toDouble())
}

class MultiplicationNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
    override val symbol: String = "*"
    override val description: String = "Szorzás (*)"
    override fun getValue(): Value = Value.Number(left.getValue().toDouble() * right.getValue().toDouble())
}

class DivisionNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
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

class ExponentiationNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
    override val symbol: String = "^"
    override val description: String = "Hatványozás (^)"
    override fun getValue(): Value = Value.Number(left.getValue().toDouble().pow(right.getValue().toDouble()))
}

class RootNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
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

class EqualNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
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

class NotEqualNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
    override val symbol: String = "!="
    override val description: String = "Nem egyenlő (!=)"
    override fun getValue(): Value {
        val eq = EqualNode(left, right).getValue() as Value.Bool
        return Value.Bool(!eq.value)
    }
}

class LessThanNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
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

class GreaterThanNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
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

class LessOrEqualNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
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

class GreaterOrEqualNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
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

class AndNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
    override val symbol: String = "and"
    override val description: String = "Logikai És (and/&&)"
    override fun getValue(): Value = Value.Bool(left.getValue().toBoolean() && right.getValue().toBoolean())
}

class OrNode(left: ExpressionNode, right: ExpressionNode) : BinaryOperationNode(left, right) {
    override val symbol: String = "or"
    override val description: String = "Logikai Vagy (or/||)"
    override fun getValue(): Value = Value.Bool(left.getValue().toBoolean() || right.getValue().toBoolean())
}

// ================= UNÁRIS MŰVELETEK =================

class NotNode(operand: ExpressionNode) : UnaryOperationNode(operand) {
    override val symbol: String = "not"
    override val description: String = "Logikai Tagadás (not/!)"
    override fun getValue(): Value = Value.Bool(!operand.getValue().toBoolean())
}

abstract class TrigonometricNode(
    operand: ExpressionNode,
    val isDegreeMode: Boolean = false,
) : UnaryOperationNode(operand) {
    protected fun toRadians(angle: Double): Double {
        return if (isDegreeMode) Math.toRadians(angle) else angle
    }
}

class SinNode(
    operand: ExpressionNode,
    isDegreeMode: Boolean = false,
) : TrigonometricNode(operand, isDegreeMode) {
    override val symbol: String = "sin"
    override val description: String = if (isDegreeMode) "Szinusz (sin, Fok)" else "Szinusz (sin, Rad)"
    override fun getValue(): Value = Value.Number(kotlin.math.sin(toRadians(operand.getValue().toDouble())))
}

class CosNode(
    operand: ExpressionNode,
    isDegreeMode: Boolean = false,
) : TrigonometricNode(operand, isDegreeMode) {
    override val symbol: String = "cos"
    override val description: String = if (isDegreeMode) "Koszinusz (cos, Fok)" else "Koszinusz (cos, Rad)"
    override fun getValue(): Value = Value.Number(kotlin.math.cos(toRadians(operand.getValue().toDouble())))
}

class TanNode(
    operand: ExpressionNode,
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
    operand: ExpressionNode,
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

// ================= LEVELEK, VÁLTOZÓK ÉS FÜGGVÉNYHÍVÁSOK (KÖZVETLENÜL ExpressionNode) =================

class NumberNode(val numberValue: Double) : ExpressionNode() {
    override val isLeaf: Boolean get() = true
    override val symbol: String = if (numberValue % 1.0 == 0.0) {
        numberValue.toLong().toString()
    } else {
        numberValue.toString()
    }
    override val description: String = "Konstans szám ($symbol)"
    override fun getValue(): Value = Value.Number(numberValue)
}

class BooleanNode(val booleanValue: Boolean) : ExpressionNode() {
    override val isLeaf: Boolean get() = true
    override val symbol: String = if (booleanValue) "true" else "false"
    override val description: String = if (booleanValue) "Konstans logikai érték (true / igaz)" else "Konstans logikai érték (false / hamis)"
    override fun getValue(): Value = Value.Bool(booleanValue)
}

class StringNode(val stringValue: String) : ExpressionNode() {
    override val isLeaf: Boolean get() = true
    override val symbol: String = "\"$stringValue\""
    override val description: String = "Konstans szöveg (\"$stringValue\")"
    override fun getValue(): Value = Value.Text(stringValue)
}

class VariableNode(
    val name: String,
    val variableValue: Value,
) : ExpressionNode() {
    override val isLeaf: Boolean get() = true
    override val symbol: String = name
    override val description: String = "Változó ($name = \"${variableValue.toDisplayString()}\")"
    override fun getValue(): Value = variableValue
}

class FunctionCallNode(
    val functionName: String,
    val argumentNodes: List<ExpressionNode>,
    val functionBodyTree: ExpressionNode,
) : ExpressionNode() {
    override val symbol: String = if (argumentNodes.isEmpty()) {
        functionName
    } else {
        "$functionName(${argumentNodes.joinToString(", ") { it.symbol }})"
    }
    override val description: String = "Függvényhívás: $functionName"
    override fun getValue(): Value = functionBodyTree.getValue()
}
