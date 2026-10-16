package hu.infokristaly.homework4mathtree.parser

import hu.infokristaly.homework4mathtree.model.*

/**
 * Felhasználó által definiált elnevezett kifejezés vagy függvény.
 */
data class UserFunction(
    val name: String,
    val parameters: List<String> = emptyList(), // pl. ["x"] vagy ["x", "y"]
    val expressionBody: String                   // pl. "10 * x + 2"
)

/**
 * Tokenek a lexikális elemzéshez.
 */
sealed class Token {
    data class Number(val value: Double) : Token()
    data class StringLiteral(val value: String) : Token()
    data class Variable(val name: String) : Token()
    object Plus : Token()
    object Minus : Token()
    object Multiply : Token()
    object Divide : Token()
    object Power : Token()
    object Root : Token()
    object LParen : Token()
    object RParen : Token()
    object Comma : Token()
    object Equal : Token()          // ==
    object NotEqual : Token()       // !=
    object LessThan : Token()       // <
    object GreaterThan : Token()    // >
    object LessOrEqual : Token()    // <=
    object GreaterOrEqual : Token() // >=
    object And : Token()            // and / &&
    object Or : Token()             // or / ||
    object Not : Token()            // not / !
}

/**
 * Matematikai és logikai kifejezéseket kielemző és bináris kifejezésfát építő osztály.
 * Támogatja a Munkaterületek (Workspaces), Objektum tulajdonságok (zöldAlma.szín / Kert.zöldAlmafa.almák.szín)
 * és Függvények közötti hivatkozásokat.
 */
class MathExpressionParser {

    /**
     * Szöveges kifejezést alakít bináris ExpressionNode fa struktúrává.
     */
    fun parse(
        input: String,
        variables: Map<String, Value> = emptyMap(),
        userFunctions: Map<String, UserFunction> = emptyMap(),
        isDegreeMode: Boolean = false,
        simulation: Simulation? = null
    ): ExpressionNode {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Kérjük, adjon meg egy kifejezést!")
        }
        val tokens = tokenize(trimmed)
        if (tokens.isEmpty()) {
            throw IllegalArgumentException("A megadott kifejezés üres vagy érvénytelen!")
        }
        val parserInstance = ParserInstance(tokens, variables, userFunctions, isDegreeMode, simulation)
        val rootNode = parserInstance.parseExpression()
        if (!parserInstance.isAtEnd()) {
            throw IllegalArgumentException("Érvénytelen kifejezés a következő szimbólnál: ${parserInstance.peek()}")
        }
        return rootNode
    }

    /**
     * Kinyeri a kifejezésben található összes szabad változó és objektum tulajdonság nevét egyedi listaként.
     */
    fun extractVariables(
        input: String,
        userFunctions: Map<String, UserFunction> = emptyMap(),
        simulation: Simulation? = null
    ): List<String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return emptyList()
        val builtInNames = setOf(
            "sqrt", "root", "gyok", "gyök",
            "sin", "szinusz", "cos", "koszinusz",
            "tan", "tg", "tangens", "cot", "ctg", "kotangens",
            "pi", "π", "e",
            "and", "or", "not",
            "true", "igaz", "false", "hamis"
        )
        return try {
            val tokens = tokenize(trimmed)
            val funcNames = userFunctions.keys.map { it.lowercase() }.toMutableSet()
            simulation?.workspaces?.values?.forEach { ws ->
                ws.functions.forEach { f -> funcNames.add(f.name.lowercase()) }
            }

            tokens.filterIsInstance<Token.Variable>()
                .map { it.name }
                .filterNot { funcNames.contains(it.lowercase()) || builtInNames.contains(it.lowercase()) }
                .distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = input.length

        while (i < len) {
            val c = input[i]
            when {
                c.isWhitespace() -> {
                    i++
                }
                c == '"' || c == '\'' -> {
                    val quote = c
                    i++
                    val sb = StringBuilder()
                    while (i < len && input[i] != quote) {
                        if (input[i] == '\\' && i + 1 < len) {
                            i++
                            sb.append(input[i])
                        } else {
                            sb.append(input[i])
                        }
                        i++
                    }
                    if (i < len && input[i] == quote) {
                        i++
                    }
                    tokens.add(Token.StringLiteral(sb.toString()))
                }
                c == '+' -> {
                    tokens.add(Token.Plus)
                    i++
                }
                c == '-' -> {
                    tokens.add(Token.Minus)
                    i++
                }
                c == '*' -> {
                    tokens.add(Token.Multiply)
                    i++
                }
                c == '/' -> {
                    tokens.add(Token.Divide)
                    i++
                }
                c == '^' -> {
                    tokens.add(Token.Power)
                    i++
                }
                c == '=' && i + 1 < len && input[i + 1] == '=' -> {
                    tokens.add(Token.Equal)
                    i += 2
                }
                c == '!' && i + 1 < len && input[i + 1] == '=' -> {
                    tokens.add(Token.NotEqual)
                    i += 2
                }
                c == '<' && i + 1 < len && input[i + 1] == '=' -> {
                    tokens.add(Token.LessOrEqual)
                    i += 2
                }
                c == '>' && i + 1 < len && input[i + 1] == '=' -> {
                    tokens.add(Token.GreaterOrEqual)
                    i += 2
                }
                c == '<' -> {
                    tokens.add(Token.LessThan)
                    i++
                }
                c == '>' -> {
                    tokens.add(Token.GreaterThan)
                    i++
                }
                c == '&' && i + 1 < len && input[i + 1] == '&' -> {
                    tokens.add(Token.And)
                    i += 2
                }
                c == '|' && i + 1 < len && input[i + 1] == '|' -> {
                    tokens.add(Token.Or)
                    i += 2
                }
                c == '!' -> {
                    tokens.add(Token.Not)
                    i++
                }
                c == '√' -> {
                    tokens.add(Token.Root)
                    i++
                }
                c == 'π' -> {
                    tokens.add(Token.Variable("π"))
                    i++
                }
                c == '(' -> {
                    tokens.add(Token.LParen)
                    i++
                }
                c == ')' -> {
                    tokens.add(Token.RParen)
                    i++
                }
                c == ',' -> {
                    tokens.add(Token.Comma)
                    i++
                }
                c.isDigit() -> {
                    val start = i
                    var hasDot = false
                    i++
                    while (i < len) {
                        val ch = input[i]
                        if (ch.isDigit()) {
                            i++
                        } else if (ch == '.' && !hasDot) {
                            hasDot = true
                            i++
                        } else {
                            break
                        }
                    }
                    val numStr = input.substring(start, i)
                    val value = numStr.toDoubleOrNull()
                        ?: throw IllegalArgumentException("Érvénytelen szám: $numStr")
                    tokens.add(Token.Number(value))
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    i++
                    while (i < len && (input[i].isLetterOrDigit() || input[i] == '_' || input[i] == '.')) {
                        i++
                    }
                    val name = input.substring(start, i)
                    when (name.lowercase()) {
                        "sqrt", "root", "gyok", "gyök" -> tokens.add(Token.Root)
                        "and" -> tokens.add(Token.And)
                        "or" -> tokens.add(Token.Or)
                        "not" -> tokens.add(Token.Not)
                        else -> tokens.add(Token.Variable(name))
                    }
                }
                else -> {
                    throw IllegalArgumentException("Ismeretlen karakter: '$c'")
                }
            }
        }
        return tokens
    }

    private class ParserInstance(
        private val tokens: List<Token>,
        private val variables: Map<String, Value>,
        private val userFunctions: Map<String, UserFunction>,
        private val isDegreeMode: Boolean,
        private val simulation: Simulation?
    ) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= tokens.size

        fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null

        private fun advance(): Token {
            val token = tokens[pos]
            pos++
            return token
        }

        fun parseExpression(): ExpressionNode = parseOr()

        private fun parseOr(): ExpressionNode {
            var node = parseAnd()
            while (peek() is Token.Or) {
                advance()
                val right = parseAnd()
                node = OrNode(node, right)
            }
            return node
        }

        private fun parseAnd(): ExpressionNode {
            var node = parseEquality()
            while (peek() is Token.And) {
                advance()
                val right = parseEquality()
                node = AndNode(node, right)
            }
            return node
        }

        private fun parseEquality(): ExpressionNode {
            var node = parseRelational()
            while (true) {
                when (peek()) {
                    is Token.Equal -> {
                        advance()
                        val right = parseRelational()
                        node = EqualNode(node, right)
                    }
                    is Token.NotEqual -> {
                        advance()
                        val right = parseRelational()
                        node = NotEqualNode(node, right)
                    }
                    else -> break
                }
            }
            return node
        }

        private fun parseRelational(): ExpressionNode {
            var node = parseAdditive()
            while (true) {
                when (peek()) {
                    is Token.LessThan -> {
                        advance()
                        val right = parseAdditive()
                        node = LessThanNode(node, right)
                    }
                    is Token.GreaterThan -> {
                        advance()
                        val right = parseAdditive()
                        node = GreaterThanNode(node, right)
                    }
                    is Token.LessOrEqual -> {
                        advance()
                        val right = parseAdditive()
                        node = LessOrEqualNode(node, right)
                    }
                    is Token.GreaterOrEqual -> {
                        advance()
                        val right = parseAdditive()
                        node = GreaterOrEqualNode(node, right)
                    }
                    else -> break
                }
            }
            return node
        }

        private fun parseAdditive(): ExpressionNode {
            var node = parseTerm()
            while (true) {
                when (peek()) {
                    is Token.Plus -> {
                        advance()
                        val right = parseTerm()
                        node = AdditionNode(node, right)
                    }
                    is Token.Minus -> {
                        advance()
                        val right = parseTerm()
                        node = SubtractionNode(node, right)
                    }
                    else -> break
                }
            }
            return node
        }

        private fun parseTerm(): ExpressionNode {
            var node = parsePowerAndRoot()
            while (true) {
                when (peek()) {
                    is Token.Multiply -> {
                        advance()
                        val right = parsePowerAndRoot()
                        node = MultiplicationNode(node, right)
                    }
                    is Token.Divide -> {
                        advance()
                        val right = parsePowerAndRoot()
                        node = DivisionNode(node, right)
                    }
                    else -> break
                }
            }
            return node
        }

        private fun parsePowerAndRoot(): ExpressionNode {
            var node = parseUnary()
            while (true) {
                when (peek()) {
                    is Token.Power -> {
                        advance()
                        val right = parseUnary()
                        node = ExponentiationNode(node, right)
                    }
                    is Token.Root -> {
                        advance()
                        val right = parseUnary()
                        node = RootNode(node, right)
                    }
                    else -> break
                }
            }
            return node
        }

        private fun parseUnary(): ExpressionNode {
            when (val token = peek()) {
                is Token.Plus -> {
                    advance()
                    return parseUnary()
                }
                is Token.Minus -> {
                    advance()
                    val operand = parseUnary()
                    return if (operand is NumberNode) {
                        NumberNode(-operand.numberValue)
                    } else {
                        SubtractionNode(NumberNode(0.0), operand)
                    }
                }
                is Token.Not -> {
                    advance()
                    val operand = parseUnary()
                    return NotNode(operand)
                }
                is Token.Root -> {
                    advance()
                    val operand = parseUnary()
                    return RootNode(NumberNode(2.0), operand)
                }
                is Token.Variable -> {
                    val nameLower = token.name.lowercase()
                    val isTrig = nameLower in setOf("sin", "cos", "tan", "tg", "cot", "ctg")
                    if (isTrig && pos + 1 < tokens.size && tokens[pos + 1] !is Token.LParen) {
                        advance()
                        val operand = parseUnary()
                        return when (nameLower) {
                            "sin" -> SinNode(operand, isDegreeMode)
                            "cos" -> CosNode(operand, isDegreeMode)
                            "tan", "tg" -> TanNode(operand, isDegreeMode)
                            "cot", "ctg" -> CotNode(operand, isDegreeMode)
                            else -> parsePrimary()
                        }
                    } else {
                        return parsePrimary()
                    }
                }
                else -> return parsePrimary()
            }
        }

        private fun parsePrimary(): ExpressionNode {
            val token = peek()
                ?: throw IllegalArgumentException("Kifejezés korai vége. Hiányzó operandus!")

            return when (token) {
                is Token.Number -> {
                    advance()
                    NumberNode(token.value)
                }
                is Token.StringLiteral -> {
                    advance()
                    StringNode(token.value)
                }
                is Token.Variable -> {
                    val varToken = advance() as Token.Variable
                    val varName = varToken.name

                    // Beépített konstansok
                    when (varName.lowercase()) {
                        "true", "igaz" -> return BooleanNode(true)
                        "false", "hamis" -> return BooleanNode(false)
                        "pi", "π" -> return NumberNode(Math.PI)
                        "e" -> return NumberNode(Math.E)
                    }

                    if (peek() is Token.LParen) {
                        advance() // '('
                        val argNodes = mutableListOf<ExpressionNode>()
                        if (peek() !is Token.RParen) {
                            while (true) {
                                val argExpr = parseExpression()
                                argNodes.add(argExpr)
                                if (peek() is Token.Comma) {
                                    advance() // ','
                                } else {
                                    break
                                }
                            }
                        }
                        if (peek() !is Token.RParen) {
                            throw IllegalArgumentException("Hiányzó zárójel a '$varName' függvényhívás végén: ')'")
                        }
                        advance() // ')'

                        val resolvedUserFunc = resolveUserFunction(varName)

                        if (resolvedUserFunc != null) {
                            val localCallVariables = variables.toMutableMap()
                            resolvedUserFunc.parameters.forEachIndexed { index, paramName ->
                                val argVal = if (index < argNodes.size) argNodes[index].getValue() else Value.Number(0.0)
                                localCallVariables[paramName] = argVal
                            }

                            val bodyParser = MathExpressionParser()
                            val bodyTree = bodyParser.parse(
                                input = resolvedUserFunc.expressionBody,
                                variables = localCallVariables,
                                userFunctions = userFunctions,
                                isDegreeMode = isDegreeMode,
                                simulation = simulation
                            )

                            FunctionCallNode(
                                functionName = varName,
                                argumentNodes = argNodes,
                                functionBodyTree = bodyTree
                            )
                        } else {
                            when (varName.lowercase()) {
                                "sqrt", "root", "gyok", "gyök" -> {
                                    val arg = argNodes.firstOrNull()
                                        ?: throw IllegalArgumentException("A gyökvonáshoz meg kell adni egy kifejezést!")
                                    RootNode(NumberNode(2.0), arg)
                                }
                                "sin", "szinusz" -> {
                                    val arg = argNodes.firstOrNull()
                                        ?: throw IllegalArgumentException("A szinusz függvényhez meg kell adni egy kifejezést!")
                                    SinNode(arg, isDegreeMode)
                                }
                                "cos", "koszinusz" -> {
                                    val arg = argNodes.firstOrNull()
                                        ?: throw IllegalArgumentException("A koszinusz függvényhez meg kell adni egy kifejezést!")
                                    CosNode(arg, isDegreeMode)
                                }
                                "tan", "tg", "tangens" -> {
                                    val arg = argNodes.firstOrNull()
                                        ?: throw IllegalArgumentException("A tangens függvényhez meg kell adni egy kifejezést!")
                                    TanNode(arg, isDegreeMode)
                                }
                                "cot", "ctg", "kotangens" -> {
                                    val arg = argNodes.firstOrNull()
                                        ?: throw IllegalArgumentException("A kotangens függvényhez meg kell adni egy kifejezést!")
                                    CotNode(arg, isDegreeMode)
                                }
                                else -> throw IllegalArgumentException("Ismeretlen függvény: '$varName'")
                            }
                        }
                    } else {
                        val resolvedValue = resolveVariableValue(varName)
                        VariableNode(varName, resolvedValue)
                    }
                }
                is Token.LParen -> {
                    advance()
                    val expr = parseExpression()
                    if (peek() !is Token.RParen) {
                        throw IllegalArgumentException("Hiányzó zárójel: ')'")
                    }
                    advance()
                    expr
                }
                else -> throw IllegalArgumentException("Váratlan szimbólum: $token")
            }
        }

        private fun resolveUserFunction(funcName: String): UserFunction? {
            if (userFunctions.containsKey(funcName)) return userFunctions[funcName]
            if (userFunctions.containsKey(funcName.lowercase())) return userFunctions[funcName.lowercase()]

            if (funcName.contains(".") && simulation != null) {
                val parts = funcName.split(".")
                if (parts.size == 2) {
                    val wsName = parts[0]
                    val fName = parts[1]
                    val targetWs = simulation.workspaces[wsName]
                    if (targetWs != null) {
                        val fData = targetWs.functions.find { it.name.equals(fName, ignoreCase = true) }
                        if (fData != null) {
                            return UserFunction(fData.name, fData.parameters, fData.expressionBody)
                        }
                    }
                }
            }

            if (simulation != null) {
                val activeWs = simulation.getActiveWorkspace()
                val fData = activeWs.functions.find { it.name.equals(funcName, ignoreCase = true) }
                if (fData != null) {
                    return UserFunction(fData.name, fData.parameters, fData.expressionBody)
                }
            }

            return null
        }

        private fun resolveVariableValue(varName: String): Value {
            val valFromMap = variables[varName] ?: variables[varName.lowercase()]
            if (valFromMap != null) {
                return valFromMap
            }

            if (varName.contains(".")) {
                val currentWs = simulation?.getActiveWorkspace() ?: Workspace("Fő_Munkaterület")
                val resolvedDotVal = resolveObjectPropertyPath(varName, currentWs, simulation)
                if (resolvedDotVal != null) {
                    return resolvedDotVal
                }
            }

            return Value.Text(varName)
        }
    }
}

/**
 * Feloldja az objektum tulajdonság elérési útvonalat (pl. zöldAlma.szín vagy Kert.zöldAlmafa.almák.szín).
 */
fun resolveObjectPropertyPath(
    path: String,
    currentWorkspace: Workspace,
    simulation: Simulation?
): Value? {
    val parts = path.split(".")
    if (parts.size < 2) return null

    var ws = currentWorkspace
    var partIdx = 0

    if (simulation != null && simulation.workspaces.containsKey(parts[0])) {
        ws = simulation.workspaces[parts[0]]!!
        partIdx = 1
    }

    if (partIdx >= parts.size) return null

    val currentObjName = parts[partIdx]
    var currentObj = ws.objects[currentObjName]

    if (currentObj == null) {
        val rawVarVal = ws.variables[currentObjName] ?: ws.variables[currentObjName.lowercase()]
        if (rawVarVal != null && partIdx == parts.size - 1) {
            return parseVariableValue(rawVarVal)
        }
        return null
    }

    partIdx++

    while (partIdx < parts.size) {
        val obj = currentObj ?: return null
        val propName = parts[partIdx]
        val propValueAny = obj.properties[propName] ?: return null

        if (partIdx == parts.size - 1) {
            return getPropertyValueAsValue(propValueAny)
        } else {
            val nextObjName = getPropertyValueAsString(propValueAny).trim().removeSurrounding("\"").removeSurrounding("'")
            val nextObj = ws.objects[nextObjName] ?: return null
            currentObj = nextObj
            partIdx++
        }
    }

    return null
}

/**
 * Frissíti a megadott objektum tulajdonság elérési útvonalát a szimulációban (pl. zöldAlma.szín = "piros").
 */
fun updateObjectPropertyPath(
    path: String,
    newValueStr: String,
    currentWorkspace: Workspace,
    simulation: Simulation
): Simulation {
    val parts = path.split(".")
    if (parts.size < 2) return simulation

    var wsName = currentWorkspace.name
    var ws = currentWorkspace
    var partIdx = 0

    if (simulation.workspaces.containsKey(parts[0])) {
        wsName = parts[0]
        ws = simulation.workspaces[wsName]!!
        partIdx = 1
    }

    if (partIdx >= parts.size) return simulation

    val currentObjName = parts[partIdx]
    var currentObj = ws.objects[currentObjName] ?: return simulation
    partIdx++

    val objChain = mutableListOf<ObjectInstance>()
    objChain.add(currentObj)

    while (partIdx < parts.size - 1) {
        val propName = parts[partIdx]
        val propValueAny = currentObj.properties[propName] ?: return simulation
        val nextObjName = getPropertyValueAsString(propValueAny).trim().removeSurrounding("\"").removeSurrounding("'")
        val nextObj = ws.objects[nextObjName] ?: return simulation
        currentObj = nextObj
        objChain.add(currentObj)
        partIdx++
    }

    val finalPropName = parts[partIdx]
    val targetObj = objChain.last()
    val propType = ws.classDefs[targetObj.className]?.properties?.get(finalPropName) ?: "StringNode"
    val typedVal = createTypedPropertyValue(propType, newValueStr)
    val updatedProps = targetObj.properties + (finalPropName to typedVal)
    val updatedObj = targetObj.copy(properties = updatedProps)

    val updatedObjects = ws.objects + (updatedObj.name to updatedObj)
    val updatedWs = ws.copy(objects = updatedObjects)
    val updatedWorkspaces = simulation.workspaces + (wsName to updatedWs)

    return simulation.copy(workspaces = updatedWorkspaces)
}
