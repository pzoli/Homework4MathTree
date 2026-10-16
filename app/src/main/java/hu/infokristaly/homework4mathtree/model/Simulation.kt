package hu.infokristaly.homework4mathtree.model

import hu.infokristaly.homework4mathtree.parser.MathExpressionParser
import java.util.UUID

/**
 * Osztály Definíció (ClassDef) adatmodell:
 * Mezők neve és típusa (pl. "szín" -> "StringNode", "almák" -> "Alma").
 */
data class ClassDef(
    val name: String,                            // Osztály neve (pl. "Alma", "Almafa")
    val properties: Map<String, String> = emptyMap() // Mezőnév -> Típusnév (pl. "szín" -> "StringNode", "súly" -> "NumberNode", "almák" -> "Alma")
) {
    fun toJsonString(): String {
        val propsStr = properties.toMap().entries.joinToString(",") { (k, v) ->
            "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
        }
        return "{\"name\":\"${escapeJson(name)}\",\"properties\":{$propsStr}}"
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ClassDef {
            val name = map["name"]?.toString() ?: "ClassDef"
            val props = mutableMapOf<String, String>()
            (map["properties"] as? Map<String, Any?>)?.forEach { (k, v) ->
                props[k] = v?.toString() ?: "StringNode"
            }
            return ClassDef(name, props)
        }
    }
}

/**
 * Objektum Példány (ObjectInstance) adatmodell:
 * Egy ClassDef alapján létrejött példány a mezők konkrét gépelt értékeivel (NumberNode, BooleanNode, StringNode, beágyazott ObjectInstance).
 */
data class ObjectInstance(
    val name: String,                            // Objektum neve (pl. "zöldAlma", "zöldAlmafa")
    val className: String,                       // Az Osztály (ClassDef) neve (pl. "Alma")
    val properties: Map<String, Any> = emptyMap() // Mezőnév -> Típusos Érték (NumberNode, StringNode, beágyazott ObjectInstance, etc.)
) {
    fun toJsonString(): String {
        val propsStr = properties.toMap().entries.joinToString(",") { (k, v) ->
            val valStr = when (v) {
                is ObjectInstance -> v.toJsonString()
                is ExpressionNode -> "\"${escapeJson(v.getValue().toDisplayString())}\""
                is Value -> "\"${escapeJson(v.toDisplayString())}\""
                else -> "\"${escapeJson(v.toString())}\""
            }
            "\"${escapeJson(k)}\":$valStr"
        }
        return "{\"name\":\"${escapeJson(name)}\",\"className\":\"${escapeJson(className)}\",\"properties\":{$propsStr}}"
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ObjectInstance {
            val name = map["name"]?.toString() ?: "Object"
            val className = map["className"]?.toString() ?: "ClassDef"
            val props = mutableMapOf<String, Any>()
            (map["properties"] as? Map<String, Any?>)?.forEach { (k, v) ->
                if (v is Map<*, *>) {
                    val subObjMap = v as Map<String, Any?>
                    if (subObjMap.containsKey("className") && subObjMap.containsKey("properties")) {
                        props[k] = ObjectInstance.fromMap(subObjMap)
                    } else {
                        props[k] = v
                    }
                } else {
                    props[k] = v ?: ""
                }
            }
            return ObjectInstance(name, className, props)
        }
    }
}

/**
 * Segédfüggvények a típusos tulajdonság értékek példányosításához és megjelenítéséhez.
 */
fun createTypedPropertyValue(propType: String, inputStr: String): Any {
    val trimmed = inputStr.trim()
    return when (propType) {
        "NumberNode" -> {
            val num = trimmed.toDoubleOrNull() ?: 0.0
            NumberNode(num)
        }
        "BooleanNode" -> {
            val b = trimmed.lowercase() == "true" || trimmed.lowercase() == "igaz" || trimmed == "1"
            BooleanNode(b)
        }
        "StringNode" -> {
            val str = trimmed.removeSurrounding("\"").removeSurrounding("'")
            StringNode(str)
        }
        "VariableNode" -> {
            VariableNode(trimmed, Value.Text(trimmed))
        }
        else -> {
            StringNode(trimmed.removeSurrounding("\"").removeSurrounding("'"))
        }
    }
}

fun parseVariableValue(inputStr: String): Value {
    val trimmed = inputStr.trim()
    if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
        (trimmed.startsWith("'") && trimmed.endsWith("'"))
    ) {
        return Value.Text(trimmed.substring(1, trimmed.length - 1))
    }
    val num = trimmed.toDoubleOrNull()
    if (num != null) {
        return Value.Number(num)
    }
    if (trimmed.equals("true", ignoreCase = true) || trimmed.equals("igaz", ignoreCase = true)) {
        return Value.Bool(true)
    }
    if (trimmed.equals("false", ignoreCase = true) || trimmed.equals("hamis", ignoreCase = true)) {
        return Value.Bool(false)
    }
    return Value.Text(trimmed)
}

fun getPropertyValueAsString(propValue: Any?): String {
    return when (propValue) {
        is ObjectInstance -> propValue.name
        is ExpressionNode -> propValue.getValue().toDisplayString()
        is Value -> propValue.toDisplayString()
        is Number -> propValue.toString()
        is Boolean -> propValue.toString()
        else -> propValue?.toString() ?: ""
    }
}

fun getPropertyValueAsValue(propValue: Any?): Value {
    return when (propValue) {
        is ObjectInstance -> Value.Text(propValue.name)
        is ExpressionNode -> propValue.getValue()
        is Value -> propValue
        is Number -> Value.Number(propValue.toDouble())
        is Boolean -> Value.Bool(propValue)
        is String -> parseVariableValue(propValue)
        else -> Value.Text(propValue?.toString() ?: "")
    }
}

/**
 * Megnevezett Időzítő (Timer) osztály:
 * Meghatározott időközönként (intervalMillis) beteszi a megadott címkével (targetLabel) ellátott szabályokat a Process Queue-ba.
 */
data class Timer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                            // Időzítő neve (pl. "Klíma Időzítő")
    val targetLabel: String,                     // Cél szabály címke (pl. "Klíma")
    val intervalMillis: Long = 2000L,            // Időköz milliszekundumban (pl. 2000 ms = 2 mp)
    val isRunning: Boolean = false               // Aktív-e az időzítő
) {
    fun toJsonString(): String {
        return "{" +
            "\"id\":\"${escapeJson(id)}\"," +
            "\"name\":\"${escapeJson(name)}\"," +
            "\"targetLabel\":\"${escapeJson(targetLabel)}\"," +
            "\"intervalMillis\":$intervalMillis," +
            "\"isRunning\":$isRunning" +
            "}"
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Timer {
            return Timer(
                id = map["id"]?.toString() ?: UUID.randomUUID().toString(),
                name = map["name"]?.toString() ?: "Időzítő",
                targetLabel = map["targetLabel"]?.toString() ?: "Klíma",
                intervalMillis = (map["intervalMillis"] as? Number)?.toLong() ?: 2000L,
                isRunning = map["isRunning"] as? Boolean ?: false
            )
        }
    }
}

/**
 * Szabály (Rule) adatmodell:
 * 1. Értékadó: "if <feltétel> then conclude that <változó> = <érték>"
 * 2. Meghívó/Fókuszáló: "if <feltétel> then invoke <címke> rules"
 * 3. Időzítő vezérlő: "if <feltétel> then start/stop timer <időzítő_név>"
 * 4. Feltétel nélküli: "unconditionally ..."
 */
data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                            // Szabály neve (pl. "Szabály 1")
    val label: String = "Alapértelmezett",        // Címke (pl. "init", "Teszt", "Klíma")
    val conditionExpression: String,             // Feltétel kifejezés (pl. "x1 <= 10")
    val targetVariable: String = "",             // Cél változó neve (pl. "x1" vagy "status")
    val valueExpression: String = "",            // Beállítandó érték kifejezése (pl. "x1 + 2")
    val focusLabel: String? = null,              // Ha "invoke <címke> rules" típusú szabály
    val timerAction: String? = null,             // "start" vagy "stop" ha időzítőt indít/állít le
    val timerName: String? = null,               // Az elindítandó vagy leállítandó időzítő neve
    val isUnconditional: Boolean = false,        // true esetén feltétel nélküli szabály
    val mayCauseChaining: Boolean = true,        // Szabályozza, hogy az értékadás indítson-e újabb szabály kiértékelést
    val referencedVariables: List<String> = emptyList(), // A feltételben (condition) hivatkozott változók
    val isActive: Boolean = true
) {
    val isFocusRule: Boolean get() = focusLabel != null
    val isTimerActionRule: Boolean get() = !timerAction.isNullOrEmpty() && !timerName.isNullOrEmpty()

    /**
     * Visszaadja a szabály teljes szöveges formátumát.
     */
    fun toRuleText(): String {
        return when {
            isTimerActionRule && isUnconditional -> "unconditionally $timerAction timer $timerName"
            isTimerActionRule -> "if $conditionExpression then $timerAction timer $timerName"
            isUnconditional && isFocusRule -> "unconditionally invoke $focusLabel rules"
            isUnconditional -> "unconditionally conclude that $targetVariable = $valueExpression"
            isFocusRule -> "if $conditionExpression then invoke $focusLabel rules"
            else -> "if $conditionExpression then conclude that $targetVariable = $valueExpression"
        }
    }

    fun toJsonString(): String {
        val focusStr = if (focusLabel != null) ",\"focusLabel\":\"${escapeJson(focusLabel)}\"" else ""
        val timerActStr = if (timerAction != null) ",\"timerAction\":\"${escapeJson(timerAction)}\"" else ""
        val timerNameStr = if (timerName != null) ",\"timerName\":\"${escapeJson(timerName)}\"" else ""
        val refVarsStr = referencedVariables.toList().joinToString(",") { "\"${escapeJson(it)}\"" }

        return "{" +
            "\"id\":\"${escapeJson(id)}\"," +
            "\"name\":\"${escapeJson(name)}\"," +
            "\"label\":\"${escapeJson(label)}\"," +
            "\"conditionExpression\":\"${escapeJson(conditionExpression)}\"," +
            "\"targetVariable\":\"${escapeJson(targetVariable)}\"," +
            "\"valueExpression\":\"${escapeJson(valueExpression)}\"," +
            "\"isUnconditional\":$isUnconditional," +
            "\"mayCauseChaining\":$mayCauseChaining," +
            "\"referencedVariables\":[$refVarsStr]," +
            "\"isActive\":$isActive" +
            focusStr +
            timerActStr +
            timerNameStr +
            "}"
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Rule {
            val cond = map["conditionExpression"]?.toString() ?: "true"
            val refVars = (map["referencedVariables"] as? List<*>)?.mapNotNull { it?.toString() }
                ?: MathExpressionParser().extractVariables(cond)

            return Rule(
                id = map["id"]?.toString() ?: UUID.randomUUID().toString(),
                name = map["name"]?.toString() ?: "Szabály",
                label = map["label"]?.toString() ?: "Alapértelmezett",
                conditionExpression = cond,
                targetVariable = map["targetVariable"]?.toString() ?: "",
                valueExpression = map["valueExpression"]?.toString() ?: "",
                focusLabel = map["focusLabel"]?.toString(),
                timerAction = map["timerAction"]?.toString(),
                timerName = map["timerName"]?.toString(),
                isUnconditional = map["isUnconditional"] as? Boolean ?: false,
                mayCauseChaining = map["mayCauseChaining"] as? Boolean ?: true,
                referencedVariables = refVars,
                isActive = map["isActive"] as? Boolean ?: true
            )
        }
    }
}

/**
 * Szöveges Rule Parser: leképezi a szöveges szabályokat Rule objektummá.
 * Automatikusan kinyeri a feltételben (condition) hivatkozott változók listáját.
 */
object RuleParser {
    fun parseRuleText(
        name: String,
        text: String,
        defaultLabel: String = "Alapértelmezett",
        mayCauseChaining: Boolean = true,
        id: String = UUID.randomUUID().toString()
    ): Rule {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()
        val parser = MathExpressionParser()

        // 1. Feltétel nélküli szabály: "unconditionally ..."
        if (lower.startsWith("unconditionally")) {
            var rest = trimmed.substring(15).trim()
            val restLower = rest.lowercase()

            // 1a. "unconditionally invoke <címke> rules" / "unconditionally focus on <címke>"
            if (restLower.startsWith("invoke") || restLower.startsWith("focus on") || restLower.startsWith("focuse on")) {
                var labelPart = if (restLower.startsWith("invoke")) {
                    rest.substring(6).trim()
                } else if (restLower.startsWith("focus on")) {
                    rest.substring(8).trim()
                } else {
                    rest.substring(9).trim()
                }
                if (labelPart.lowercase().endsWith("rules")) {
                    labelPart = labelPart.substring(0, labelPart.length - 5).trim()
                }
                if (labelPart.isEmpty()) {
                    throw IllegalArgumentException("Meg kell adni a meghívandó címkét! Pl.: unconditionally invoke Klíma rules")
                }
                return Rule(
                    id = id,
                    name = name,
                    label = defaultLabel,
                    conditionExpression = "true",
                    focusLabel = labelPart,
                    isUnconditional = true,
                    mayCauseChaining = mayCauseChaining,
                    referencedVariables = emptyList()
                )
            }

            // 1b. "unconditionally start timer <név>" / "unconditionally stop timer <név>"
            if (restLower.startsWith("start timer") || restLower.startsWith("stop timer") ||
                restLower.startsWith("start") || restLower.startsWith("stop")
            ) {
                val action = if (restLower.contains("start")) "start" else "stop"
                val timerIndex = restLower.indexOf("timer")
                val tName = if (timerIndex != -1) {
                    rest.substring(timerIndex + 5).trim().removeSurrounding("\"").removeSurrounding("'")
                } else {
                    rest.substring(action.length).trim().removeSurrounding("\"").removeSurrounding("'")
                }
                if (tName.isEmpty()) {
                    throw IllegalArgumentException("Meg kell adni az időzítő nevét! Pl.: unconditionally start timer Klíma Időzítő")
                }
                return Rule(
                    id = id,
                    name = name,
                    label = defaultLabel,
                    conditionExpression = "true",
                    timerAction = action,
                    timerName = tName,
                    isUnconditional = true,
                    mayCauseChaining = mayCauseChaining,
                    referencedVariables = emptyList()
                )
            }

            // 1c. "unconditionally conclude that x1 = 10 + y"
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
                isUnconditional = true,
                mayCauseChaining = mayCauseChaining,
                referencedVariables = emptyList()
            )
        }

        // 2. Feltételes szabály "if...then..."
        val ifIndex = lower.indexOf("if")
        if (ifIndex == -1) {
            throw IllegalArgumentException("A szabálynak 'if...then...', 'if...invoke...', 'if...start/stop timer' vagy 'unconditionally...' formátumúnak kell lennie!")
        }

        val thenIndex = lower.indexOf("then", ifIndex + 2)
        if (thenIndex == -1) {
            throw IllegalArgumentException("A szabálynak tartalmaznia kell a 'then' kulcsszót az 'if' feltétel után!")
        }

        val condition = trimmed.substring(ifIndex + 2, thenIndex).trim()
        if (condition.isEmpty()) {
            throw IllegalArgumentException("Az 'if' és 'then' közötti feltétel nem lehet üres!")
        }

        val refVars = parser.extractVariables(condition)

        val afterThen = trimmed.substring(thenIndex + 4).trim()
        val afterThenLower = afterThen.lowercase()

        // 2a. "invoke <címke> rules" / "focus on <címke>"
        if (afterThenLower.startsWith("invoke") || afterThenLower.startsWith("focus on") || afterThenLower.startsWith("focuse on")) {
            var labelPart = if (afterThenLower.startsWith("invoke")) {
                afterThen.substring(6).trim()
            } else if (afterThenLower.startsWith("focus on")) {
                afterThen.substring(8).trim()
            } else {
                afterThen.substring(9).trim()
            }
            if (labelPart.lowercase().endsWith("rules")) {
                labelPart = labelPart.substring(0, labelPart.length - 5).trim()
            }
            if (labelPart.isEmpty()) {
                throw IllegalArgumentException("Meg kell adni a meghívandó címkét! Pl.: invoke Klíma rules")
            }
            return Rule(
                id = id,
                name = name,
                label = defaultLabel,
                conditionExpression = condition,
                targetVariable = "",
                valueExpression = "",
                focusLabel = labelPart,
                isUnconditional = false,
                mayCauseChaining = mayCauseChaining,
                referencedVariables = refVars
            )
        }

        // 2b. "start timer <időzítő_név>" / "stop timer <időzítő_név>"
        if (afterThenLower.startsWith("start timer") || afterThenLower.startsWith("stop timer") ||
            afterThenLower.startsWith("start") || afterThenLower.startsWith("stop")
        ) {
            val action = if (afterThenLower.contains("start")) "start" else "stop"
            val timerIndex = afterThenLower.indexOf("timer")
            val tName = if (timerIndex != -1) {
                afterThen.substring(timerIndex + 5).trim().removeSurrounding("\"").removeSurrounding("'")
            } else {
                afterThen.substring(action.length).trim().removeSurrounding("\"").removeSurrounding("'")
            }
            if (tName.isEmpty()) {
                throw IllegalArgumentException("Meg kell adni az időzítő nevét! Pl.: start timer Klíma Időzítő")
            }
            return Rule(
                id = id,
                name = name,
                label = defaultLabel,
                conditionExpression = condition,
                timerAction = action,
                timerName = tName,
                isUnconditional = false,
                mayCauseChaining = mayCauseChaining,
                referencedVariables = refVars
            )
        }

        // 2c. "conclude that <változó> = <kifejezés>"
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
            isUnconditional = false,
            mayCauseChaining = mayCauseChaining,
            referencedVariables = refVars
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
 * amely szabályokat (Rules), változókat, függvényeket, időzítőket (Timers), osztályokat (ClassDef) és objektumokat (ObjectInstance) tárol.
 */
data class Workspace(
    val name: String,
    val variables: Map<String, String> = emptyMap(),
    val functions: List<UserFunctionData> = emptyList(),
    val rules: List<Rule> = emptyList(),
    val timers: List<Timer> = emptyList(),
    val classDefs: Map<String, ClassDef> = emptyMap(),
    val objects: Map<String, ObjectInstance> = emptyMap()
) {
    fun toJsonString(): String {
        val varsStr = variables.toMap().entries.joinToString(",") { (k, v) ->
            "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
        }
        val funcsStr = functions.toList().joinToString(",") { f ->
            val paramsStr = f.parameters.toList().joinToString(",") { "\"${escapeJson(it)}\"" }
            "{\"name\":\"${escapeJson(f.name)}\",\"parameters\":[$paramsStr],\"expressionBody\":\"${escapeJson(f.expressionBody)}\"}"
        }
        val rulesStr = rules.toList().joinToString(",") { it.toJsonString() }
        val timersStr = timers.toList().joinToString(",") { it.toJsonString() }
        val classDefsStr = classDefs.toMap().values.joinToString(",") { it.toJsonString() }
        val objectsStr = objects.toMap().values.joinToString(",") { it.toJsonString() }
        return "{\"name\":\"${escapeJson(name)}\",\"variables\":{$varsStr},\"functions\":[$funcsStr],\"rules\":[$rulesStr],\"timers\":[$timersStr],\"classDefs\":[$classDefsStr],\"objects\":[$objectsStr]}"
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

            val timersList = mutableListOf<Timer>()
            (map["timers"] as? List<Map<String, Any?>>)?.forEach { tMap ->
                timersList.add(Timer.fromMap(tMap))
            }

            val classDefsMap = mutableMapOf<String, ClassDef>()
            (map["classDefs"] as? List<Map<String, Any?>>)?.forEach { cdMap ->
                val cd = ClassDef.fromMap(cdMap)
                classDefsMap[cd.name] = cd
            }

            val objectsMap = mutableMapOf<String, ObjectInstance>()
            (map["objects"] as? List<Map<String, Any?>>)?.forEach { objMap ->
                val obj = ObjectInstance.fromMap(objMap)
                objectsMap[obj.name] = obj
            }

            return Workspace(name, varsMap, funcsList, rulesList, timersList, classDefsMap, objectsMap)
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
            variables = mapOf("v1" to "teszt", "a" to "1", "b" to "3", "temp" to "28", "isHot" to "true", "x1" to "2"),
            functions = listOf(
                UserFunctionData("függvény1", listOf("x"), "2 * x")
            ),
            rules = listOf(
                Rule(
                    name = "Init Szabály - x1 kezdőérték",
                    label = "init",
                    conditionExpression = "true",
                    targetVariable = "x1",
                    valueExpression = "2",
                    isUnconditional = true
                ),
                Rule(
                    name = "Chaining Szabály - x1 növelése",
                    label = "Teszt",
                    conditionExpression = "x1 <= 10",
                    targetVariable = "x1",
                    valueExpression = "x1 + 2",
                    mayCauseChaining = true,
                    referencedVariables = listOf("x1")
                ),
                Rule(
                    name = "Kánikula Fókusz",
                    label = "init",
                    conditionExpression = "isHot == true",
                    focusLabel = "Klíma",
                    referencedVariables = listOf("isHot")
                ),
                Rule(
                    name = "Klíma Időzítő Indítása",
                    label = "Klíma",
                    conditionExpression = "temp > 25",
                    timerAction = "start",
                    timerName = "Klíma Időzítő",
                    referencedVariables = listOf("temp")
                ),
                Rule(
                    name = "Klíma Időzítő Leállítása",
                    label = "Klíma",
                    conditionExpression = "temp <= 20",
                    timerAction = "stop",
                    timerName = "Klíma Időzítő",
                    referencedVariables = listOf("temp")
                ),
                Rule(
                    name = "Hőmérséklet Riasztás",
                    label = "Klíma",
                    conditionExpression = "temp > 25",
                    targetVariable = "status",
                    valueExpression = "\"Hűtés_szükséges\"",
                    referencedVariables = listOf("temp")
                )
            ),
            timers = listOf(
                Timer(
                    name = "Klíma Időzítő",
                    targetLabel = "Klíma",
                    intervalMillis = 2000L,
                    isRunning = false
                )
            ),
            classDefs = mapOf(
                "Alma" to ClassDef(
                    name = "Alma",
                    properties = mapOf("szín" to "StringNode", "súly" to "NumberNode")
                ),
                "Almafa" to ClassDef(
                    name = "Almafa",
                    properties = mapOf("almák" to "Alma")
                )
            ),
            objects = mapOf(
                "zöldAlmafa" to ObjectInstance(
                    name = "zöldAlmafa",
                    className = "Almafa",
                    properties = mapOf(
                        "almák" to ObjectInstance(
                            name = "zöldAlmafa_almák",
                            className = "Alma",
                            properties = mapOf("szín" to StringNode("zöld"), "súly" to NumberNode(150.0))
                        )
                    )
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
                    valueExpression = "m * g",
                    referencedVariables = listOf("m")
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
        val wsStr = workspaces.toMap().values.joinToString(",") { it.toJsonString() }
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
