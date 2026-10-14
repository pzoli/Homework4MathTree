package com.example.homework4mathtree.model

import java.util.UUID

/**
 * Szabály (Rule) adatmodell:
 * 1. Feltételes: "if <feltétel> then conclude that <változó> = <érték>"
 * 2. Fókuszáló: "if <feltétel> then focus on <címke>"
 * 3. Feltétel nélküli: "unconditionally conclude that <változó> = <érték>"
 */
data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                            // Szabály neve (pl. "Szabály 1")
    val label: String = "Alapértelmezett",        // Címke csoportos fókuszhoz (pl. "Teszt", "Klíma")
    val conditionExpression: String,             // Feltétel kifejezés (pl. "v1 == \"teszt\"" vagy "true")
    val targetVariable: String = "",             // Cél változó neve (pl. "x1" vagy "status")
    val valueExpression: String = "",            // Beállítandó érték kifejezése (pl. "10 + y" vagy "\"Hűtés\"")
    val focusLabel: String? = null,              // Ha "focus on <címke>" típusú szabály
    val isUnconditional: Boolean = false,        // true esetén feltétel nélküli szabály
    val isActive: Boolean = true
) {
    val isFocusRule: Boolean get() = focusLabel != null

    /**
     * Visszaadja a szabály teljes szöveges formátumát.
     */
    fun toRuleText(): String {
        return when {
            isUnconditional -> "unconditionally conclude that $targetVariable = $valueExpression"
            isFocusRule -> "if $conditionExpression then focus on $focusLabel"
            else -> "if $conditionExpression then conclude that $targetVariable = $valueExpression"
        }
    }

    fun toJsonString(): String {
        val focusStr = if (focusLabel != null) ",\"focusLabel\":\"${escapeJson(focusLabel)}\"" else ""
        return "{" +
            "\"id\":\"${escapeJson(id)}\"," +
            "\"name\":\"${escapeJson(name)}\"," +
            "\"label\":\"${escapeJson(label)}\"," +
            "\"conditionExpression\":\"${escapeJson(conditionExpression)}\"," +
            "\"targetVariable\":\"${escapeJson(targetVariable)}\"," +
            "\"valueExpression\":\"${escapeJson(valueExpression)}\"," +
            "\"isUnconditional\":$isUnconditional," +
            "\"isActive\":$isActive" +
            focusStr +
            "}"
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Rule {
            return Rule(
                id = map["id"]?.toString() ?: UUID.randomUUID().toString(),
                name = map["name"]?.toString() ?: "Szabály",
                label = map["label"]?.toString() ?: "Alapértelmezett",
                conditionExpression = map["conditionExpression"]?.toString() ?: "true",
                targetVariable = map["targetVariable"]?.toString() ?: "",
                valueExpression = map["valueExpression"]?.toString() ?: "",
                focusLabel = map["focusLabel"]?.toString(),
                isUnconditional = map["isUnconditional"] as? Boolean ?: false,
                isActive = map["isActive"] as? Boolean ?: true
            )
        }
    }
}

/**
 * Szöveges Rule Parser: leképezi a szöveges szabályokat Rule objektummá.
 */
object RuleParser {
    fun parseRuleText(
        name: String,
        text: String,
        defaultLabel: String = "Alapértelmezett",
        id: String = UUID.randomUUID().toString()
    ): Rule {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()

        // 1. Feltétel nélküli szabály: "unconditionally conclude that x1 = 10 + y"
        if (lower.startsWith("unconditionally")) {
            var rest = trimmed.substring(15).trim()
            val restLower = rest.lowercase()
            if (restLower.startsWith("conclude that")) {
                rest = rest.substring(13).trim()
            } else if (restLower.startsWith("conclude")) {
                rest = rest.substring(8).trim()
            }

            val eqIndex = rest.indexOf('=')
            if (eqIndex == -1) {
                throw IllegalArgumentException("Feltétel nélküli szabály esetén is meg kell adni egy 'változó = kifejezés' értékadást! Pl.: unconditionally conclude that x1 = 10 + y")
            }

            val targetVar = rest.substring(0, eqIndex).trim()
            val valExpr = rest.substring(eqIndex + 1).trim()

            if (targetVar.isEmpty()) {
                throw IllegalArgumentException("Hiányzó cél változó név az '=' előtt!")
            }
            if (valExpr.isEmpty()) {
                throw IllegalArgumentException("Hiányzó érték kifejezés az '=' után!")
            }

            return Rule(
                id = id,
                name = name,
                label = defaultLabel,
                conditionExpression = "true",
                targetVariable = targetVar,
                valueExpression = valExpr,
                focusLabel = null,
                isUnconditional = true
            )
        }

        // 2. Feltételes szabály "if...then..."
        val ifIndex = lower.indexOf("if")
        if (ifIndex == -1) {
            throw IllegalArgumentException("A szabálynak 'if...then...', 'if...focus on' vagy 'unconditionally conclude that' formátumúnak kell lennie!")
        }

        val thenIndex = lower.indexOf("then", ifIndex + 2)
        if (thenIndex == -1) {
            throw IllegalArgumentException("A szabálynak tartalmaznia kell a 'then' kulcsszót az 'if' feltétel után!")
        }

        val condition = trimmed.substring(ifIndex + 2, thenIndex).trim()
        if (condition.isEmpty()) {
            throw IllegalArgumentException("Az 'if' és 'then' közötti feltétel nem lehet üres!")
        }

        val afterThen = trimmed.substring(thenIndex + 4).trim()
        val afterThenLower = afterThen.lowercase()

        // 2a. "focus on <címke>"
        if (afterThenLower.startsWith("focus on") || afterThenLower.startsWith("focuse on")) {
            val focusTarget = afterThen.substring(afterThen.indexOf("on", ignoreCase = true) + 2).trim()
            if (focusTarget.isEmpty()) {
                throw IllegalArgumentException("A 'focus on' után meg kell adni a fókuszálandó címkét! Pl.: focus on Klíma")
            }
            return Rule(
                id = id,
                name = name,
                label = defaultLabel,
                conditionExpression = condition,
                targetVariable = "",
                valueExpression = "",
                focusLabel = focusTarget,
                isUnconditional = false
            )
        }

        // 2b. "conclude that <változó> = <kifejezés>"
        var concludePart = afterThen
        if (afterThenLower.startsWith("conclude that")) {
            concludePart = afterThen.substring(13).trim()
        } else if (afterThenLower.startsWith("conclude")) {
            concludePart = afterThen.substring(8).trim()
        }

        val eqIndex = concludePart.indexOf('=')
        if (eqIndex == -1) {
            throw IllegalArgumentException("A 'then conclude that' után meg kell adni egy változó értékadást! Pl.: conclude that status = \"Hűtés\"")
        }

        val targetVar = concludePart.substring(0, eqIndex).trim()
        val valExpr = concludePart.substring(eqIndex + 1).trim()

        if (targetVar.isEmpty()) {
            throw IllegalArgumentException("Hiányzó cél változó név az '=' előtt!")
        }
        if (valExpr.isEmpty()) {
            throw IllegalArgumentException("Hiányzó érték kifejezés az '=' után!")
        }

        return Rule(
            id = id,
            name = name,
            label = defaultLabel,
            conditionExpression = condition,
            targetVariable = targetVar,
            valueExpression = valExpr,
            focusLabel = null,
            isUnconditional = false
        )
    }
}

/**
 * Felhasználó által definiált függvény adatai (szerializálható formátumban).
 */
data class UserFunctionData(
    val name: String,
    val parameters: List<String> = emptyList(),
    val expressionBody: String = ""
)

/**
 * Egyedi névvel rendelkező Munkaterület (Workspace),
 * amely szabályokat (Rules), változókat és függvényeket tárol.
 */
data class Workspace(
    val name: String,
    val variables: Map<String, String> = emptyMap(),
    val functions: List<UserFunctionData> = emptyList(),
    val rules: List<Rule> = emptyList()
) {
    fun toJsonString(): String {
        val varsStr = variables.entries.joinToString(",") { (k, v) ->
            "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
        }
        val funcsStr = functions.joinToString(",") { f ->
            val paramsStr = f.parameters.joinToString(",") { "\"${escapeJson(it)}\"" }
            "{\"name\":\"${escapeJson(f.name)}\",\"parameters\":[$paramsStr],\"expressionBody\":\"${escapeJson(f.expressionBody)}\"}"
        }
        val rulesStr = rules.joinToString(",") { it.toJsonString() }
        return "{\"name\":\"${escapeJson(name)}\",\"variables\":{$varsStr},\"functions\":[$funcsStr],\"rules\":[$rulesStr]}"
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Workspace {
            val name = map["name"] as? String ?: "Workspace"

            val varsMap = mutableMapOf<String, String>()
            (map["variables"] as? Map<String, Any?>)?.forEach { (k, v) ->
                varsMap[k] = v?.toString() ?: ""
            }

            val funcsList = mutableListOf<UserFunctionData>()
            (map["functions"] as? List<Map<String, Any?>>)?.forEach { fMap ->
                val fName = fMap["name"] as? String ?: ""
                val fBody = fMap["expressionBody"] as? String ?: ""
                val params = (fMap["parameters"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                funcsList.add(UserFunctionData(fName, params, fBody))
            }

            val rulesList = mutableListOf<Rule>()
            (map["rules"] as? List<Map<String, Any?>>)?.forEach { rMap ->
                rulesList.add(Rule.fromMap(rMap))
            }

            return Workspace(name, varsMap, funcsList, rulesList)
        }
    }
}

/**
 * Szimuláció objektum, amely a Munkaterületeket (Workspaces) tárolja egy Map-ben.
 * Támogatja a teljes tartalom JSON formátumba történő exportálását és importálását.
 */
data class Simulation(
    val activeWorkspaceName: String = "Fő_Munkaterület",
    val workspaces: Map<String, Workspace> = mapOf(
        "Fő_Munkaterület" to Workspace(
            name = "Fő_Munkaterület",
            variables = mapOf("v1" to "teszt", "a" to "1", "b" to "3", "temp" to "28", "isHot" to "true", "y" to "5"),
            functions = listOf(
                UserFunctionData("függvény1", listOf("x"), "2 * x")
            ),
            rules = listOf(
                Rule(
                    name = "Szabály 0 - Feltétel nélküli kezdőérték",
                    label = "Kezdőértékek",
                    conditionExpression = "true",
                    targetVariable = "x1",
                    valueExpression = "10 + y",
                    isUnconditional = true
                ),
                Rule(
                    name = "Szabály 1 - Teszt ellenőrzés",
                    label = "Teszt",
                    conditionExpression = "v1 == \"teszt\"",
                    targetVariable = "x1",
                    valueExpression = "10"
                ),
                Rule(
                    name = "Szabály 2 - Kánikula fókusz",
                    label = "Klíma",
                    conditionExpression = "isHot == true",
                    focusLabel = "Klíma"
                ),
                Rule(
                    name = "Szabály 3 - Hőmérséklet riasztás",
                    label = "Klíma",
                    conditionExpression = "temp > 25",
                    targetVariable = "status",
                    valueExpression = "\"Hűtés_szükséges\""
                )
            )
        ),
        "Fizika" to Workspace(
            name = "Fizika",
            variables = mapOf("g" to "9.81", "m" to "5"),
            functions = listOf(
                UserFunctionData("f1", listOf("x"), "3 * x"),
                UserFunctionData("f2", listOf("b"), "b ^ 2")
            ),
            rules = listOf(
                Rule(
                    name = "Erő számítás",
                    label = "Fizika",
                    conditionExpression = "m > 0",
                    targetVariable = "F",
                    valueExpression = "m * g"
                )
            )
        )
    )
) {
    fun getActiveWorkspace(): Workspace {
        return workspaces[activeWorkspaceName] ?: workspaces.values.firstOrNull() ?: Workspace("Fő_Munkaterület")
    }

    /**
     * Exportálja a szimuláció teljes tartalmát JSON string formátumba.
     */
    fun toJsonString(): String {
        val wsStr = workspaces.values.joinToString(",") { it.toJsonString() }
        return "{\"activeWorkspaceName\":\"${escapeJson(activeWorkspaceName)}\",\"workspaces\":[$wsStr]}"
    }

    companion object {
        /**
         * Importálja a szimulációt egy JSON stringből.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromJsonString(jsonString: String): Simulation {
            val parsed = SimpleJsonParser(jsonString).parseValue() as? Map<String, Any?>
                ?: throw IllegalArgumentException("Érvénytelen JSON tartalom!")

            val activeName = parsed["activeWorkspaceName"] as? String ?: "Fő_Munkaterület"
            val wsList = parsed["workspaces"] as? List<Map<String, Any?>> ?: emptyList()

            val wsMap = mutableMapOf<String, Workspace>()
            wsList.forEach { wsMapData ->
                val ws = Workspace.fromMap(wsMapData)
                wsMap[ws.name] = ws
            }

            if (wsMap.isEmpty()) {
                val defaultWs = Workspace("Fő_Munkaterület")
                wsMap[defaultWs.name] = defaultWs
            }

            val finalActiveName = if (wsMap.containsKey(activeName)) activeName else wsMap.keys.first()

            return Simulation(activeWorkspaceName = finalActiveName, workspaces = wsMap)
        }
    }
}

private fun escapeJson(str: String): String = str
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")

private class SimpleJsonParser(private val json: String) {
    private var pos = 0

    fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= json.length) return null
        return when (val c = json[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> if (c.isDigit() || c == '-') parseNumber() else null
        }
    }

    private fun skipWhitespace() {
        while (pos < json.length && json[pos].isWhitespace()) pos++
    }

    private fun parseObject(): Map<String, Any?> {
        pos++ // skip '{'
        val map = mutableMapOf<String, Any?>()
        while (pos < json.length) {
            skipWhitespace()
            if (pos < json.length && json[pos] == '}') {
                pos++
                return map
            }
            val key = parseString()
            skipWhitespace()
            if (pos < json.length && json[pos] == ':') pos++
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            if (pos < json.length && json[pos] == ',') {
                pos++
            } else if (pos < json.length && json[pos] == '}') {
                pos++
                return map
            }
        }
        return map
    }

    private fun parseArray(): List<Any?> {
        pos++ // skip '['
        val list = mutableListOf<Any?>()
        while (pos < json.length) {
            skipWhitespace()
            if (pos < json.length && json[pos] == ']') {
                pos++
                return list
            }
            val item = parseValue()
            list.add(item)
            skipWhitespace()
            if (pos < json.length && json[pos] == ',') {
                pos++
            } else if (pos < json.length && json[pos] == ']') {
                pos++
                return list
            }
        }
        return list
    }

    private fun parseString(): String {
        pos++ // skip '"'
        val sb = StringBuilder()
        while (pos < json.length) {
            val c = json[pos]
            if (c == '"') {
                pos++
                return sb.toString()
            }
            if (c == '\\' && pos + 1 < json.length) {
                pos++
                when (val esc = json[pos]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else -> sb.append(esc)
                }
            } else {
                sb.append(c)
            }
            pos++
        }
        return sb.toString()
    }

    private fun parseBoolean(): Boolean {
        if (json.startsWith("true", pos)) {
            pos += 4
            return true
        }
        if (json.startsWith("false", pos)) {
            pos += 5
            return false
        }
        return false
    }

    private fun parseNull(): Any? {
        if (json.startsWith("null", pos)) pos += 4
        return null
    }

    private fun parseNumber(): Number {
        val start = pos
        if (json[pos] == '-') pos++
        while (pos < json.length && (json[pos].isDigit() || json[pos] == '.')) pos++
        val numStr = json.substring(start, pos)
        return numStr.toDoubleOrNull() ?: 0.0
    }
}
