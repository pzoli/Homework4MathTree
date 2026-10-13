package com.example.homework4mathtree

import com.example.homework4mathtree.model.*
import com.example.homework4mathtree.parser.MathExpressionParser
import org.junit.Assert.*
import org.junit.Test

class MathParserTest {

    private val parser = MathExpressionParser()

    @Test
    fun testDirectNodeValues() {
        // OOP test: parent MathNode class getValue() calls on subclasses
        val num1: MathNode = NumberNode(5.0)
        val num2: MathNode = NumberNode(3.0)

        val addNode: MathNode = AdditionNode(num1, num2)
        assertEquals(8.0, addNode.getValue(), 0.0001)

        val subNode: MathNode = SubtractionNode(num1, num2)
        assertEquals(2.0, subNode.getValue(), 0.0001)

        val mulNode: MathNode = MultiplicationNode(num1, num2)
        assertEquals(15.0, mulNode.getValue(), 0.0001)

        val divNode: MathNode = DivisionNode(num1, num2)
        assertEquals(1.666666, divNode.getValue(), 0.001)

        val powNode: MathNode = ExponentiationNode(num1, num2)
        assertEquals(125.0, powNode.getValue(), 0.0001)

        val rootNode: MathNode = RootNode(num2, NumberNode(27.0))
        assertEquals(3.0, rootNode.getValue(), 0.0001)
    }

    @Test
    fun testOperatorPrecedence() {
        // 3 + 5 * 2 = 13 (not 16)
        val tree1 = parser.parse("3 + 5 * 2")
        assertTrue(tree1 is AdditionNode)
        assertEquals(13.0, tree1.getValue(), 0.0001)

        // (3 + 5) * 2 = 16
        val tree2 = parser.parse("(3 + 5) * 2")
        assertTrue(tree2 is MultiplicationNode)
        assertEquals(16.0, tree2.getValue(), 0.0001)
    }

    @Test
    fun testExponentiationAndRoots() {
        // 2 ^ 3 + 4 = 12
        val tree1 = parser.parse("2 ^ 3 + 4")
        assertEquals(12.0, tree1.getValue(), 0.0001)

        // sqrt(16) + 5 = 9
        val tree2 = parser.parse("sqrt(16) + 5")
        assertEquals(9.0, tree2.getValue(), 0.0001)

        // √16 + 5 = 9
        val tree3 = parser.parse("√16 + 5")
        assertEquals(9.0, tree3.getValue(), 0.0001)

        // 3 √ 27 = 3
        val tree4 = parser.parse("3 √ 27")
        assertEquals(3.0, tree4.getValue(), 0.0001)
    }

    @Test
    fun testComplexExpression() {
        // (10 - 2) / (1 + 3) = 2
        val tree = parser.parse("(10 - 2) / (1 + 3)")
        assertEquals(2.0, tree.getValue(), 0.0001)
    }

    @Test
    fun testNegativeNumbers() {
        // -1 should be parsed directly as NumberNode(-1.0), not SubtractionNode
        val tree1 = parser.parse("-1")
        assertTrue(tree1 is NumberNode)
        assertEquals(-1.0, tree1.getValue(), 0.0001)
        assertEquals("-1", tree1.symbol)

        // 3 + -1 = 2, right node is NumberNode(-1.0)
        val tree2 = parser.parse("3 + -1")
        assertTrue(tree2 is AdditionNode)
        val rightNode = tree2.right
        assertTrue(rightNode is NumberNode)
        assertEquals(-1.0, (rightNode as NumberNode).numberValue, 0.0001)
        assertEquals(2.0, tree2.getValue(), 0.0001)

        // 5 * -2 = -10
        val tree3 = parser.parse("5 * -2")
        assertTrue(tree3 is MultiplicationNode)
        assertEquals(-10.0, tree3.getValue(), 0.0001)

        // Subtraction vs Negative: 3 - 1 should be SubtractionNode
        val tree4 = parser.parse("3 - 1")
        assertTrue(tree4 is SubtractionNode)
        assertEquals(2.0, tree4.getValue(), 0.0001)
    }

    @Test
    fun testVariableSubstitution() {
        // Direct VariableNode test
        val varNode: MathNode = VariableNode("x", 10.0)
        assertEquals(10.0, varNode.getValue(), 0.0001)
        assertEquals("x", varNode.symbol)

        // Parse x + 5 * y with x=10, y=2 -> 10 + 10 = 20
        val vars = mapOf("x" to 10.0, "y" to 2.0)
        val tree = parser.parse("x + 5 * y", vars)
        assertTrue(tree is AdditionNode)
        val leftNode = tree.left
        assertTrue(leftNode is VariableNode)
        assertEquals("x", (leftNode as VariableNode).name)
        assertEquals(10.0, leftNode.getValue(), 0.0001)
        assertEquals(20.0, tree.getValue(), 0.0001)

        // Extract variables list
        val extracted = parser.extractVariables("a ^ 2 + b ^ 2 - sqrt(a)")
        assertEquals(listOf("a", "b"), extracted)
    }

    @Test
    fun testUserFunctionsAndNesting() {
        val funcs = mapOf(
            "függvény1" to com.example.homework4mathtree.parser.UserFunction("függvény1", listOf("x"), "2 * x"),
            "függvény2" to com.example.homework4mathtree.parser.UserFunction("függvény2", listOf("b"), "b ^ 2")
        )
        val vars = mapOf("a" to 1.0, "b" to 3.0)

        // függvény2(3) = 9
        // 10 * a + 2 * 9 = 28
        // függvény1(28) = 2 * 28 = 56
        val tree = parser.parse("függvény1(10 * a + 2 * függvény2(b))", vars, funcs)
        assertTrue(tree is FunctionCallNode)
        assertEquals(56.0, tree.getValue(), 0.0001)
    }
}
