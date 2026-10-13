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
}

/**
 * Matematikai kifejezéseket kielemző és bináris kifejezésfát építő osztály.
 */
class MathExpressionParser {

    /**
     * Szöveges matematikai kifejezést alakít bináris MathNode fa struktúrává
     * a megadott változó behelyettesítési értékekkel és felhasználói függvényekkel.
     */
    fun parse(
        input: String,
        variables: Map<String, Double> = emptyMap(),
        userFunctions: Map<String, UserFunction> = emptyMap()
    ): MathNode {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Kérjük, adjon meg egy matematikai kifejezést!")
        }
        val tokens = tokenize(trimmed)
        if (tokens.isEmpty()) {
            throw IllegalArgumentException("A megadott kifejezés üres vagy érvénytelen!")
        }
        val parserInstance = ParserInstance(tokens, variables, userFunctions)
        val rootNode = parserInstance.parseExpression()
        if (!parserInstance.isAtEnd()) {
            throw IllegalArgumentException("Érvénytelen kifejezés a következő szimbólumnál: ${parserInstance.peek()}")
        }
        return rootNode
    }

    /**
     * Kinyeri a kifejezésben található összes szabad változó nevét egyedi listaként.
     * Kiszűri a megadott felhasználói függvényneveket.
     */
    fun extractVariables(
        input: String,
        userFunctions: Map<String, UserFunction> = emptyMap()
    ): List<String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            val tokens = tokenize(trimmed)
            val funcNames = userFunctions.keys.map { it.lowercase() }.toSet()
            tokens.filterIsInstance<Token.Variable>()
                .map { it.name }
                .filterNot { funcNames.contains(it.lowercase()) }
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
                c == '+' -> {
                    tokens.add(Token.Plus)
                    i++
                }
                c == '-' -> {
                    val lastToken = tokens.lastOrNull()
                    val isUnary = lastToken == null || (
                        lastToken !is Token.Number &&
                        lastToken !is Token.Variable &&
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
        private val variables: Map<String, Double>,
        private val userFunctions: Map<String, UserFunction>
    ) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= tokens.size

        fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null

        private fun advance(): Token {
            val token = tokens[pos]
            pos++
            return token
        }

        // Expression: Term (( '+' | '-' ) Term)*
        fun parseExpression(): MathNode {
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

        // Unary: ('+' | '-' | '√' | 'sqrt') Unary | Primary
        private fun parseUnary(): MathNode {
            when (peek()) {
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
                is Token.Root -> {
                    advance()
                    val operand = parseUnary()
                    // Unary root defaults degree to 2.0 (Square root)
                    return RootNode(NumberNode(2.0), operand)
                }
                else -> return parsePrimary()
            }
        }

        // Primary: Number | Variable | FunctionCall | '(' Expression ')'
        private fun parsePrimary(): MathNode {
            val token = peek()
                ?: throw IllegalArgumentException("Kifejezés korai vége. Hiányzó operandus!")

            return when (token) {
                is Token.Number -> {
                    advance()
                    NumberNode(token.value)
                }
                is Token.Variable -> {
                    val varToken = advance() as Token.Variable
                    val varName = varToken.name

                    if (peek() is Token.LParen) {
                        // Ez egy függvényhívás! (pl. függvény1(arg1, arg2))
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
                                val argVal = if (index < argNodes.size) argNodes[index].getValue() else 0.0
                                localCallVariables[paramName] = argVal
                            }

                            val bodyParser = MathExpressionParser()
                            val bodyTree = bodyParser.parse(userFunc.expressionBody, localCallVariables, userFunctions)

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
                            val bodyTree = bodyParser.parse(userFunc.expressionBody, variables, userFunctions)
                            FunctionCallNode(
                                functionName = varName,
                                argumentNodes = emptyList(),
                                functionBodyTree = bodyTree
                            )
                        } else {
                            val varValue = variables[varName]
                                ?: variables[varName.lowercase()]
                                ?: variables[varName.uppercase()]
                                ?: 0.0
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
