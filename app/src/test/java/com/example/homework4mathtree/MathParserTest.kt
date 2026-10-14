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
        assertEquals(8.0, addNode.getValue().toDouble(), 0.0001)

        val subNode: MathNode = SubtractionNode(num1, num2)
        assertEquals(2.0, subNode.getValue().toDouble(), 0.0001)

        val mulNode: MathNode = MultiplicationNode(num1, num2)
        assertEquals(15.0, mulNode.getValue().toDouble(), 0.0001)

        val divNode: MathNode = DivisionNode(num1, num2)
        assertEquals(1.666666, divNode.getValue().toDouble(), 0.001)

        val powNode: MathNode = ExponentiationNode(num1, num2)
        assertEquals(125.0, powNode.getValue().toDouble(), 0.0001)

        val rootNode: MathNode = RootNode(num2, NumberNode(27.0))
        assertEquals(3.0, rootNode.getValue().toDouble(), 0.0001)
    }

    @Test
    fun testOperatorPrecedence() {
        // 3 + 5 * 2 = 13 (not 16)
        val tree1 = parser.parse("3 + 5 * 2")
        assertTrue(tree1 is AdditionNode)
        assertEquals(13.0, tree1.getValue().toDouble(), 0.0001)

        // (3 + 5) * 2 = 16
        val tree2 = parser.parse("(3 + 5) * 2")
        assertTrue(tree2 is MultiplicationNode)
        assertEquals(16.0, tree2.getValue().toDouble(), 0.0001)
    }

    @Test
    fun testExponentiationAndRoots() {
        // 2 ^ 3 + 4 = 12
        val tree1 = parser.parse("2 ^ 3 + 4")
        assertEquals(12.0, tree1.getValue().toDouble(), 0.0001)

        // sqrt(16) + 5 = 9
        val tree2 = parser.parse("sqrt(16) + 5")
        assertEquals(9.0, tree2.getValue().toDouble(), 0.0001)

        // √16 + 5 = 9
        val tree3 = parser.parse("√16 + 5")
        assertEquals(9.0, tree3.getValue().toDouble(), 0.0001)

        // 3 √ 27 = 3
        val tree4 = parser.parse("3 √ 27")
        assertEquals(3.0, tree4.getValue().toDouble(), 0.0001)
    }

    @Test
    fun testComplexExpression() {
        // (10 - 2) / (1 + 3) = 2
        val tree = parser.parse("(10 - 2) / (1 + 3)")
        assertEquals(2.0, tree.getValue().toDouble(), 0.0001)
    }

    @Test
    fun testNegativeNumbers() {
        // -1 should be parsed directly as NumberNode(-1.0), not SubtractionNode
        val tree1 = parser.parse("-1")
        assertTrue(tree1 is NumberNode)
        assertEquals(-1.0, tree1.getValue().toDouble(), 0.0001)
        assertEquals("-1", tree1.symbol)

        // 3 + -1 = 2, right node is NumberNode(-1.0)
        val tree2 = parser.parse("3 + -1")
        assertTrue(tree2 is AdditionNode)
        val rightNode = tree2.right
        assertTrue(rightNode is NumberNode)
        assertEquals(-1.0, (rightNode as NumberNode).numberValue, 0.0001)
        assertEquals(2.0, tree2.getValue().toDouble(), 0.0001)

        // 5 * -2 = -10
        val tree3 = parser.parse("5 * -2")
        assertTrue(tree3 is MultiplicationNode)
        assertEquals(-10.0, tree3.getValue().toDouble(), 0.0001)

        // Subtraction vs Negative: 3 - 1 should be SubtractionNode
        val tree4 = parser.parse("3 - 1")
        assertTrue(tree4 is SubtractionNode)
        assertEquals(2.0, tree4.getValue().toDouble(), 0.0001)
    }

    @Test
    fun testVariableSubstitution() {
        // Direct VariableNode test
        val varNode: MathNode = VariableNode("x", Value.Number(10.0))
        assertEquals(10.0, varNode.getValue().toDouble(), 0.0001)
        assertEquals("x", varNode.symbol)

        // Parse x + 5 * y with x=10, y=2 -> 10 + 10 = 20
        val vars = mapOf("x" to Value.Number(10.0), "y" to Value.Number(2.0))
        val tree = parser.parse("x + 5 * y", vars)
        assertTrue(tree is AdditionNode)
        val leftNode = tree.left
        assertTrue(leftNode is VariableNode)
        assertEquals("x", (leftNode as VariableNode).name)
        assertEquals(10.0, leftNode.getValue().toDouble(), 0.0001)
        assertEquals(20.0, tree.getValue().toDouble(), 0.0001)

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
        val vars = mapOf("a" to Value.Number(1.0), "b" to Value.Number(3.0))

        // függvény2(3) = 9
        // 10 * a + 2 * 9 = 28
        // függvény1(28) = 2 * 28 = 56
        val tree = parser.parse("függvény1(10 * a + 2 * függvény2(b))", vars, funcs)
        assertTrue(tree is FunctionCallNode)
        assertEquals(56.0, tree.getValue().toDouble(), 0.0001)
    }

    @Test
    fun testTrigonometricFunctions() {
        // Degree mode
        val sin90 = parser.parse("sin(90)", isDegreeMode = true)
        assertTrue(sin90 is SinNode)
        assertEquals(1.0, sin90.getValue().toDouble(), 0.0001)

        val cos0 = parser.parse("cos(0)", isDegreeMode = true)
        assertTrue(cos0 is CosNode)
        assertEquals(1.0, cos0.getValue().toDouble(), 0.0001)

        val tan45 = parser.parse("tan(45)", isDegreeMode = true)
        assertTrue(tan45 is TanNode)
        assertEquals(1.0, tan45.getValue().toDouble(), 0.0001)

        // Radian mode with pi
        val sinPi2 = parser.parse("sin(pi / 2)", isDegreeMode = false)
        assertEquals(1.0, sinPi2.getValue().toDouble(), 0.0001)
    }

    @Test
    fun testLogicalAndStringOperations() {
        val vars = mapOf(
            "v1" to Value.Text("teszt"),
            "x" to Value.Number(8.0),
            "y" to Value.Number(2.0)
        )

        // String equality test: v1 == "teszt" -> true
        val tree1 = parser.parse("v1 == \"teszt\"", vars)
        assertTrue(tree1 is EqualNode)
        assertEquals(Value.Bool(true), tree1.getValue())

        // String inequality test: v1 != "mas" -> true
        val tree2 = parser.parse("v1 != \"mas\"", vars)
        assertTrue(tree2 is NotEqualNode)
        assertEquals(Value.Bool(true), tree2.getValue())

        // Logical AND + relational: x > 5 and y <= 10 -> true
        val tree3 = parser.parse("x > 5 and y <= 10", vars)
        assertTrue(tree3 is AndNode)
        assertEquals(Value.Bool(true), tree3.getValue())

        // Logical NOT: not (v1 == "mas") -> true
        val tree4 = parser.parse("not (v1 == \"mas\")", vars)
        assertTrue(tree4 is NotNode)
        assertEquals(Value.Bool(true), tree4.getValue())

        // Boolean display strings: true (igaz) / false (hamis)
        assertEquals("true (igaz)", Value.Bool(true).toDisplayString())
        assertEquals("false (hamis)", Value.Bool(false).toDisplayString())

        // Boolean constant keywords test
        val tree5 = parser.parse("true == igaz")
        assertEquals(Value.Bool(true), tree5.getValue())
    }

    @Test
    fun testSimulationAndWorkspaces() {
        val sim = Simulation()

        // Test JSON Export & Import roundtrip
        val jsonStr = sim.toJsonString()
        val importedSim = Simulation.fromJsonString(jsonStr)

        assertEquals(sim.activeWorkspaceName, importedSim.activeWorkspaceName)
        assertEquals(sim.workspaces.keys, importedSim.workspaces.keys)

        // Test cross-workspace expression evaluation: Fizika.f1(10 * a + 2 * Fizika.f2(b))
        // f2(3) = 9
        // 10*1 + 2*9 = 28
        // f1(28) = 3 * 28 = 84
        val tree = parser.parse(
            input = "Fizika.f1(10 * a + 2 * Fizika.f2(b))",
            variables = mapOf("a" to Value.Number(1.0), "b" to Value.Number(3.0)),
            userFunctions = mapOf("függvény1" to com.example.homework4mathtree.parser.UserFunction("függvény1", listOf("x"), "2 * x")),
            simulation = sim
        )

        assertEquals(84.0, tree.getValue().toDouble(), 0.0001)
    }

    @Test
    fun testRuleEngineAndConditionTree() {
        val rule = Rule(
            name = "Teszt Szabály",
            label = "Teszt",
            conditionExpression = "v1 == \"teszt\"",
            targetVariable = "x1",
            valueExpression = "10"
        )

        val vars = mapOf("v1" to Value.Text("teszt"))

        // Test condition tree evaluation
        val condTree = parser.parse(rule.conditionExpression, vars)
        assertTrue(condTree is EqualNode)
        assertEquals(Value.Bool(true), condTree.getValue())

        // Test value expression evaluation
        val valueTree = parser.parse(rule.valueExpression, vars)
        assertEquals(Value.Number(10.0), valueTree.getValue())
    }

    @Test
    fun testRuleTextParser() {
        // Conclude rule parsing
        val r1 = RuleParser.parseRuleText(
            name = "Szabály 1",
            text = "if v1 == \"teszt\" then conclude that x1 = 10"
        )
        assertEquals("v1 == \"teszt\"", r1.conditionExpression)
        assertEquals("x1", r1.targetVariable)
        assertEquals("10", r1.valueExpression)
        assertFalse(r1.isFocusRule)
        assertEquals("if v1 == \"teszt\" then conclude that x1 = 10", r1.toRuleText())

        // Focus rule parsing
        val r2 = RuleParser.parseRuleText(
            name = "Szabály 2",
            text = "if temp > 25 then focus on Klíma"
        )
        assertEquals("temp > 25", r2.conditionExpression)
        assertTrue(r2.isFocusRule)
        assertEquals("Klíma", r2.focusLabel)
        assertEquals("if temp > 25 then focus on Klíma", r2.toRuleText())
    }
}
