package com.example.homework4mathtree.parser

import com.example.homework4mathtree.model.*

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
 */
class MathExpressionParser {

    /**
     * Szöveges kifejezést alakít bináris MathNode fa struktúrává.
     */
    fun parse(
        input: String,
        variables: Map<String, Value> = emptyMap(),
        userFunctions: Map<String, UserFunction> = emptyMap(),
        isDegreeMode: Boolean = false
    ): MathNode {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Kérjük, adjon meg egy kifejezést!")
        }
        val tokens = tokenize(trimmed)
        if (tokens.isEmpty()) {
            throw IllegalArgumentException("A megadott kifejezés üres vagy érvénytelen!")
        }
        val parserInstance = ParserInstance(tokens, variables, userFunctions, isDegreeMode)
        val rootNode = parserInstance.parseExpression()
        if (!parserInstance.isAtEnd()) {
            throw IllegalArgumentException("Érvénytelen kifejezés a következő szimbólumnál: ${parserInstance.peek()}")
        }
        return rootNode
    }

    /**
     * Kinyeri a kifejezésben található összes szabad változó nevét egyedi listaként.
     */
    fun extractVariables(
        input: String,
        userFunctions: Map<String, UserFunction> = emptyMap()
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
            val funcNames = userFunctions.keys.map { it.lowercase() }.toSet()
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
                    val start = i
                    while (i < len && input[i] != quote) {
                        i++
                    }
                    val strVal = input.substring(start, i)
                    if (i < len && input[i] == quote) {
                        i++
                    }
                    tokens.add(Token.StringLiteral(strVal))
                }
                c == '=' -> {
                    if (i + 1 < len && input[i + 1] == '=') {
                        i += 2
                    } else {
                        i++
                    }
                    tokens.add(Token.Equal)
                }
                c == '!' -> {
                    if (i + 1 < len && input[i + 1] == '=') {
                        i += 2
                        tokens.add(Token.NotEqual)
                    } else {
                        i++
                        tokens.add(Token.Not)
                    }
                }
                c == '<' -> {
                    if (i + 1 < len && input[i + 1] == '=') {
                        i += 2
                        tokens.add(Token.LessOrEqual)
                    } else {
                        i++
                        tokens.add(Token.LessThan)
                    }
                }
                c == '>' -> {
                    if (i + 1 < len && input[i + 1] == '=') {
                        i += 2
                        tokens.add(Token.GreaterOrEqual)
                    } else {
                        i++
                        tokens.add(Token.GreaterThan)
                    }
                }
                c == '&' -> {
                    if (i + 1 < len && input[i + 1] == '&') {
                        i += 2
                    } else {
                        i++
                    }
                    tokens.add(Token.And)
                }
                c == '|' -> {
                    if (i + 1 < len && input[i + 1] == '|') {
                        i += 2
                    } else {
                        i++
                    }
                    tokens.add(Token.Or)
                }
                c == '+' -> {
                    tokens.add(Token.Plus)
                    i++
                }
                c == '-' -> {
                    val lastToken = tokens.lastOrNull()
                    val isUnary = lastToken == null || (
                        lastToken !is Token.Number &&
                        lastToken !is Token.Variable &&
                        lastToken !is Token.StringLiteral &&
                        lastToken !is Token.RParen
                    )
                    if (isUnary && i + 1 < len && (input[i + 1].isDigit() || input[i + 1] == '.')) {
                        val start = i
                        i++ // Skip '-'
                        var hasDot = input[i] == '.'
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
                    } else {
                        tokens.add(Token.Minus)
                        i++
                    }
                }
                c == '*' -> {
                    tokens.add(Token.Multiply)
                    i++
                }
                c == '/' || c == ':' -> {
                    tokens.add(Token.Divide)
                    i++
                }
                c == '^' -> {
                    tokens.add(Token.Power)
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
                c.isDigit() || c == '.' -> {
                    val start = i
                    var hasDot = c == '.'
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
                    while (i < len && (input[i].isLetterOrDigit() || input[i] == '_')) {
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
        private val isDegreeMode: Boolean
    ) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= tokens.size

        fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null

        private fun advance(): Token {
            val token = tokens[pos]
            pos++
            return token
        }

        // Expression: Or
        fun parseExpression(): MathNode = parseOr()

        // Or: And ( 'or' And )*
        private fun parseOr(): MathNode {
            var node = parseAnd()
            while (peek() is Token.Or) {
                advance()
                val right = parseAnd()
                node = OrNode(node, right)
            }
            return node
        }

        // And: Equality ( 'and' Equality )*
        private fun parseAnd(): MathNode {
            var node = parseEquality()
            while (peek() is Token.And) {
                advance()
                val right = parseEquality()
                node = AndNode(node, right)
            }
            return node
        }

        // Equality: Relational ( ( '==' | '!=' ) Relational )*
        private fun parseEquality(): MathNode {
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

        // Relational: Additive ( ( '<' | '>' | '<=' | '>=' ) Additive )*
        private fun parseRelational(): MathNode {
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

        // Additive: Term ( ( '+' | '-' ) Term )*
        private fun parseAdditive(): MathNode {
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

        // Term: PowerAndRoot (( '*' | '/' ) PowerAndRoot)*
        private fun parseTerm(): MathNode {
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

        // PowerAndRoot: Unary (( '^' | 'root' ) Unary)*
        private fun parsePowerAndRoot(): MathNode {
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

        // Unary: ('+' | '-' | 'not' | '√' | 'sqrt' | trig) Unary | Primary
        private fun parseUnary(): MathNode {
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

        // Primary: Number | StringLiteral | Variable | FunctionCall | '(' Expression ')'
        private fun parsePrimary(): MathNode {
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

                    // Beépített konstansok (pi, e, true, false)
                    when (varName.lowercase()) {
                        "true", "igaz" -> return BooleanNode(true)
                        "false", "hamis" -> return BooleanNode(false)
                        "pi", "π" -> return NumberNode(Math.PI)
                        "e" -> return NumberNode(Math.E)
                    }

                    if (peek() is Token.LParen) {
                        // Ez egy függvényhívás!
                        advance() // '('
                        val argNodes = mutableListOf<MathNode>()
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

                        val userFunc = userFunctions[varName]
                            ?: userFunctions[varName.lowercase()]
                            ?: userFunctions[varName.uppercase()]

                        if (userFunc != null) {
                            val localCallVariables = variables.toMutableMap()
                            userFunc.parameters.forEachIndexed { index, paramName ->
                                val argVal = if (index < argNodes.size) argNodes[index].getValue() else Value.Number(0.0)
                                localCallVariables[paramName] = argVal
                            }

                            val bodyParser = MathExpressionParser()
                            val bodyTree = bodyParser.parse(userFunc.expressionBody, localCallVariables, userFunctions, isDegreeMode)

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
                        // Normál változó vagy paraméter nélküli elnevezett kifejezés
                        val userFunc = userFunctions[varName]
                            ?: userFunctions[varName.lowercase()]
                            ?: userFunctions[varName.uppercase()]

                        if (userFunc != null && userFunc.parameters.isEmpty()) {
                            val bodyParser = MathExpressionParser()
                            val bodyTree = bodyParser.parse(userFunc.expressionBody, variables, userFunctions, isDegreeMode)
                            FunctionCallNode(
                                functionName = varName,
                                argumentNodes = emptyList(),
                                functionBodyTree = bodyTree
                            )
                        } else {
                            val varValue = variables[varName]
                                ?: variables[varName.lowercase()]
                                ?: variables[varName.uppercase()]
                                ?: Value.Number(0.0)
                            VariableNode(varName, varValue)
                        }
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
                else -> {
                    throw IllegalArgumentException("Érvénytelen szimbólum a kifejezésben: $token")
                }
            }
        }
    }
}
