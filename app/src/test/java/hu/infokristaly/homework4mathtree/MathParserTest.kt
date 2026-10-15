package hu.infokristaly.homework4mathtree

import hu.infokristaly.homework4mathtree.model.*
import hu.infokristaly.homework4mathtree.parser.MathExpressionParser
import hu.infokristaly.homework4mathtree.parser.UserFunction
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
        val rightNode = (tree2 as AdditionNode).right
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
        val leftNode = (tree as AdditionNode).left
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
            "függvény1" to UserFunction("függvény1", listOf("x"), "2 * x"),
            "függvény2" to UserFunction("függvény2", listOf("b"), "b ^ 2")
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
            userFunctions = mapOf("függvény1" to UserFunction("függvény1", listOf("x"), "2 * x")),
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

        // Invoke rule parsing
        val r2 = RuleParser.parseRuleText(
            name = "Szabály 2",
            text = "if temp > 25 then invoke Klíma rules"
        )
        assertEquals("temp > 25", r2.conditionExpression)
        assertTrue(r2.isFocusRule)
        assertEquals("Klíma", r2.focusLabel)
        assertEquals("if temp > 25 then invoke Klíma rules", r2.toRuleText())

        // Unconditional rule parsing
        val r3 = RuleParser.parseRuleText(
            name = "Kezdőérték szabály",
            text = "unconditionally conclude that x1 = 10 + y"
        )
        assertTrue(r3.isUnconditional)
        assertEquals("x1", r3.targetVariable)
        assertEquals("10 + y", r3.valueExpression)
        assertEquals("unconditionally conclude that x1 = 10 + y", r3.toRuleText())

        // Test evaluation of valueExpression
        val vars = mapOf("y" to Value.Number(5.0))
        val valTree = parser.parse(r3.valueExpression, vars)
        assertEquals(Value.Number(15.0), valTree.getValue())
    }

    @Test
    fun testChainingAndReferencedVariables() {
        val ruleText = "if x1 <= 10 then conclude that x1 = x1 + 2"
        val rule = RuleParser.parseRuleText("Chaining Rule", ruleText)

        assertEquals("x1 <= 10", rule.conditionExpression)
        assertEquals("x1", rule.targetVariable)
        assertEquals("x1 + 2", rule.valueExpression)
        assertTrue(rule.mayCauseChaining)
        assertEquals(listOf("x1"), rule.referencedVariables)

        // Test step 1: x1 = 2 -> 2 <= 10 -> true -> x1 = 4
        var vars = mapOf("x1" to Value.Number(2.0))
        var condNode = parser.parse(rule.conditionExpression, vars)
        assertTrue(condNode.getValue().toBoolean())
        val valNode = parser.parse(rule.valueExpression, vars)
        assertEquals(4.0, valNode.getValue().toDouble(), 0.0001)

        // Test step 5: x1 = 12 -> 12 <= 10 -> false (stops chaining)
        vars = mapOf("x1" to Value.Number(12.0))
        condNode = parser.parse(rule.conditionExpression, vars)
        assertFalse(condNode.getValue().toBoolean())
    }

    @Test
    fun testTimerDataClass() {
        val timer = Timer(
            name = "Klíma Időzítő",
            targetLabel = "Klíma",
            intervalMillis = 2000L,
            isRunning = true
        )

        assertEquals("Klíma Időzítő", timer.name)
        assertEquals("Klíma", timer.targetLabel)
        assertEquals(2000L, timer.intervalMillis)
        assertTrue(timer.isRunning)

        // Test Workspace containing timers and JSON serialization roundtrip
        val ws = Workspace(
            name = "Teszt WS",
            timers = listOf(timer)
        )
        assertEquals(1, ws.timers.size)

        val jsonStr = ws.toJsonString()
        @Suppress("UNCHECKED_CAST")
        val parsedMap = Simulation.fromJsonString(
            "{\"activeWorkspaceName\":\"Teszt WS\",\"workspaces\":[$jsonStr]}"
        ).getActiveWorkspace()

        assertEquals(1, parsedMap.timers.size)
        assertEquals("Klíma Időzítő", parsedMap.timers[0].name)
        assertEquals("Klíma", parsedMap.timers[0].targetLabel)
        assertEquals(2000L, parsedMap.timers[0].intervalMillis)
        assertTrue(parsedMap.timers[0].isRunning)
    }

    @Test
    fun testTimerActionRuleParsing() {
        // start timer rule
        val r1 = RuleParser.parseRuleText(
            name = "Klíma Start",
            text = "if temp > 25 then start timer Klíma Időzítő"
        )
        assertTrue(r1.isTimerActionRule)
        assertEquals("start", r1.timerAction)
        assertEquals("Klíma Időzítő", r1.timerName)
        assertEquals("if temp > 25 then start timer Klíma Időzítő", r1.toRuleText())

        // stop timer rule
        val r2 = RuleParser.parseRuleText(
            name = "Klíma Stop",
            text = "if temp <= 20 then stop timer Klíma Időzítő"
        )
        assertTrue(r2.isTimerActionRule)
        assertEquals("stop", r2.timerAction)
        assertEquals("Klíma Időzítő", r2.timerName)
        assertEquals("if temp <= 20 then stop timer Klíma Időzítő", r2.toRuleText())

        // unconditionally start timer rule
        val r3 = RuleParser.parseRuleText(
            name = "Kezdő Timer Start",
            text = "unconditionally start timer Klíma Időzítő"
        )
        assertTrue(r3.isTimerActionRule)
        assertTrue(r3.isUnconditional)
        assertEquals("start", r3.timerAction)
        assertEquals("Klíma Időzítő", r3.timerName)
        assertEquals("unconditionally start timer Klíma Időzítő", r3.toRuleText())
    }

    @Test
    fun testUnconditionalRuleFocusedOnly() {
        val r = RuleParser.parseRuleText(
            name = "Feltétel nélküli szabály",
            text = "unconditionally conclude that x1 = 10 + y",
            defaultLabel = "init"
        )
        assertTrue(r.isUnconditional)

        // General evaluation should filter out unconditional rules
        val allRules = listOf(
            r,
            RuleParser.parseRuleText("Normál szabály", "if temp > 25 then conclude that status = \"Hűtés\"")
        )
        val generalRules = allRules.filter { !it.isUnconditional }
        assertEquals(1, generalRules.size)
        assertEquals("Normál szabály", generalRules[0].name)

        // Focused evaluation on label "init" includes the unconditional rule
        val initRules = allRules.filter { it.label.equals("init", ignoreCase = true) }
        assertEquals(1, initRules.size)
        assertEquals("Feltétel nélküli szabály", initRules[0].name)
    }

    @Test
    fun testExpressionNodeHierarchy() {
        val num5 = NumberNode(5.0)
        val num3 = NumberNode(3.0)

        // Binary operation node
        val addNode = AdditionNode(num5, num3)
        assertTrue(addNode is BinaryOperationNode)
        assertTrue(addNode is ExpressionNode)
        assertEquals(num5, addNode.left)
        assertEquals(num3, addNode.right)
        assertFalse(addNode.isLeaf)

        // Unary operation node
        val boolNode = BooleanNode(false)
        val notNode = NotNode(boolNode)
        assertTrue(notNode is UnaryOperationNode)
        assertTrue(notNode is ExpressionNode)
        assertEquals(boolNode, notNode.operand)
        assertFalse(notNode.isLeaf)

        // Leaf nodes
        assertTrue(num5.isLeaf)
        assertTrue(boolNode.isLeaf)
    }

    @Test
    fun testClassDefAndObjectPropertyPaths() {
        val sim = Simulation()
        val activeWs = sim.getActiveWorkspace()

        // Test 1: Direct object property lookup (zöldAlma.szín -> "zöld")
        val tree1 = parser.parse("zöldAlma.szín == \"zöld\"", simulation = sim)
        assertEquals(Value.Bool(true), tree1.getValue())

        // Test 2: Nested object property lookup (zöldAlmafa.almák.szín -> "zöld")
        val tree2 = parser.parse("zöldAlmafa.almák.szín == \"zöld\"", simulation = sim)
        assertEquals(Value.Bool(true), tree2.getValue())

        // Test 3: Workspace dot notation lookup (Fő_Munkaterület.zöldAlmafa.almák.szín -> "zöld")
        val tree3 = parser.parse("Fő_Munkaterület.zöldAlmafa.almák.szín == \"zöld\"", simulation = sim)
        assertEquals(Value.Bool(true), tree3.getValue())

        // Test 4: Object property assignment update
        val updatedSim = hu.infokristaly.homework4mathtree.parser.updateObjectPropertyPath(
            path = "zöldAlma.szín",
            newValueStr = "\"piros\"",
            currentWorkspace = activeWs,
            simulation = sim
        )
        val tree4 = parser.parse("zöldAlma.szín", simulation = updatedSim)
        assertEquals("piros", tree4.getValue().toDisplayString())
    }
}
