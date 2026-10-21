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
) : java.io.Serializable {
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
) : java.io.Serializable {
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
                        props[k] = fromMap(subObjMap)
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

fun updateLocalNestedObject(
    props: Map<String, Any>,
    pathSegments: List<String>,
    createdSubObj: ObjectInstance
): Map<String, Any> {
    if (pathSegments.isEmpty()) return props
    val head = pathSegments[0]
    if (pathSegments.size == 1) {
        return props + (head to createdSubObj)
    } else {
        val childVal = props[head]
        val childObj: ObjectInstance = when (childVal) {
            is ObjectInstance -> childVal
            else -> ObjectInstance(name = head, className = "ClassDef", properties = emptyMap())
        }
        val updatedChildProps = updateLocalNestedObject(childObj.properties, pathSegments.subList(1, pathSegments.size), createdSubObj)
        val updatedChildObj = childObj.copy(properties = updatedChildProps)
        return props + (head to updatedChildObj)
    }
}

fun updateLocalNestedPropertyValue(
    props: Map<String, Any>,
    pathSegments: List<String>,
    newValue: Any
): Map<String, Any> {
    if (pathSegments.isEmpty()) return props
    val head = pathSegments[0]
    if (pathSegments.size == 1) {
        return props + (head to newValue)
    } else {
        val childVal = props[head]
        val childObj: ObjectInstance = when (childVal) {
            is ObjectInstance -> childVal
            else -> ObjectInstance(name = head, className = "ClassDef", properties = emptyMap())
        }
        val updatedChildProps = updateLocalNestedPropertyValue(childObj.properties, pathSegments.subList(1, pathSegments.size), newValue)
        val updatedChildObj = childObj.copy(properties = updatedChildProps)
        return props + (head to updatedChildObj)
    }
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
) : java.io.Serializable {
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
 * Egy szabály akciója (következménye):
 * - Értékadás: conclude that <targetVariable> = <valueExpression>
 * - Fókusz / Invokáció: invoke <focusLabel> rules
 * - Időzítő állapotváltás: start/stop timer <timerName>
 * - Objektum példányosítás: create object from <className> into <target>
 */
sealed class RuleAction : java.io.Serializable {
    data class Assignment(
        val targetVariable: String,
        val valueExpression: String
    ) : RuleAction()

    data class Focus(
        val focusLabel: String
    ) : RuleAction()

    data class TimerAction(
        val action: String, // "start" or "stop"
        val timerName: String
    ) : RuleAction()

    data class CreateObject(
        val className: String,
        val target: String
    ) : RuleAction()

    fun toActionText(): String {
        return when (this) {
            is Assignment -> "conclude that $targetVariable = $valueExpression"
            is Focus -> "invoke $focusLabel rules"
            is TimerAction -> "$action timer $timerName"
            is CreateObject -> "create object from $className into ${if (target.contains(".")) "property $target" else "variable $target"}"
        }
    }
}

/**
 * Szabály (Rule) adatmodell:
 * Támogatja egy vagy több akció (következmény) sorrendi összefűzését 'then' kulcsszóval.
 */
data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                            // Szabály neve (pl. "Szabály 1")
    val label: String = "Alapértelmezett",        // Címke (pl. "init", "Teszt", "Klíma")
    val conditionExpression: String,             // Feltétel kifejezés (pl. "x1 <= 10")
    val actions: List<RuleAction> = emptyList(), // Akciók listája sorrendben
    val isUnconditional: Boolean = false,        // true esetén feltétel nélküli szabály
    val mayCauseChaining: Boolean = true,        // Szabályozza, hogy az értékadás indítson-e újabb szabály kiértékelést
    val referencedVariables: List<String> = emptyList(), // A feltételben (condition) hivatkozott változók
    val isActive: Boolean = true
) : java.io.Serializable {

    // Visszafelé kompatibilitás segéd-getterek
    val targetVariable: String get() = (actions.firstOrNull { it is RuleAction.Assignment } as? RuleAction.Assignment)?.targetVariable ?: ""
    val valueExpression: String get() = (actions.firstOrNull { it is RuleAction.Assignment } as? RuleAction.Assignment)?.valueExpression ?: ""
    val focusLabel: String? get() = (actions.firstOrNull { it is RuleAction.Focus } as? RuleAction.Focus)?.focusLabel
    val timerAction: String? get() = (actions.firstOrNull { it is RuleAction.TimerAction } as? RuleAction.TimerAction)?.action
    val timerName: String? get() = (actions.firstOrNull { it is RuleAction.TimerAction } as? RuleAction.TimerAction)?.timerName
    val createObjectClass: String? get() = (actions.firstOrNull { it is RuleAction.CreateObject } as? RuleAction.CreateObject)?.className
    val createObjectTarget: String? get() = (actions.firstOrNull { it is RuleAction.CreateObject } as? RuleAction.CreateObject)?.target

    val isFocusRule: Boolean get() = actions.any { it is RuleAction.Focus }
    val isTimerActionRule: Boolean get() = actions.any { it is RuleAction.TimerAction }
    val isCreateObjectRule: Boolean get() = actions.any { it is RuleAction.CreateObject }

    /**
     * Visszaadja a szabály teljes szöveges formátumát.
     */
    fun toRuleText(): String {
        val actionsStr = if (actions.isEmpty()) {
            "conclude that $targetVariable = $valueExpression"
        } else {
            actions.joinToString(" then ") { it.toActionText() }
        }
        return if (isUnconditional) {
            "unconditionally $actionsStr"
        } else {
            "if $conditionExpression then $actionsStr"
        }
    }

    fun toJsonString(): String {
        val actionsJson = actions.joinToString(",") { actionToJson(it) }
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
            "\"isActive\":$isActive," +
            "\"actions\":[$actionsJson]" +
            "}"
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Rule {
            val cond = map["conditionExpression"]?.toString() ?: "true"
            val refVars = (map["referencedVariables"] as? List<*>)?.mapNotNull { it?.toString() }
                ?: MathExpressionParser().extractVariables(cond)

            val actionsList = mutableListOf<RuleAction>()
            val rawActions = map["actions"] as? List<Map<String, Any?>>
            if (rawActions != null) {
                for (aMap in rawActions) {
                    val act = parseRuleActionFromMap(aMap)
                    if (act != null) actionsList.add(act)
                }
            }

            if (actionsList.isEmpty()) {
                val tVar = map["targetVariable"]?.toString() ?: ""
                val vExpr = map["valueExpression"]?.toString() ?: ""
                val fLabel = map["focusLabel"]?.toString()
                val tAction = map["timerAction"]?.toString()
                val tName = map["timerName"]?.toString()
                val cClass = map["createObjectClass"]?.toString()
                val cTarget = map["createObjectTarget"]?.toString()

                if (!cClass.isNullOrEmpty() && !cTarget.isNullOrEmpty()) {
                    actionsList.add(RuleAction.CreateObject(cClass, cTarget))
                } else if (!tAction.isNullOrEmpty() && !tName.isNullOrEmpty()) {
                    actionsList.add(RuleAction.TimerAction(tAction, tName))
                } else if (!fLabel.isNullOrEmpty()) {
                    actionsList.add(RuleAction.Focus(fLabel))
                } else if (tVar.isNotEmpty()) {
                    actionsList.add(RuleAction.Assignment(tVar, vExpr))
                }
            }

            return Rule(
                id = map["id"]?.toString() ?: UUID.randomUUID().toString(),
                name = map["name"]?.toString() ?: "Szabály",
                label = map["label"]?.toString() ?: "Alapértelmezett",
                conditionExpression = cond,
                actions = actionsList,
                isUnconditional = map["isUnconditional"] as? Boolean ?: false,
                mayCauseChaining = map["mayCauseChaining"] as? Boolean ?: true,
                referencedVariables = refVars,
                isActive = map["isActive"] as? Boolean ?: true
            )
        }
    }
}

private fun actionToJson(action: RuleAction): String {
    return when (action) {
        is RuleAction.Assignment -> "{\"type\":\"Assignment\",\"targetVariable\":\"${escapeJson(action.targetVariable)}\",\"valueExpression\":\"${escapeJson(action.valueExpression)}\"}"
        is RuleAction.Focus -> "{\"type\":\"Focus\",\"focusLabel\":\"${escapeJson(action.focusLabel)}\"}"
        is RuleAction.TimerAction -> "{\"type\":\"TimerAction\",\"action\":\"${escapeJson(action.action)}\",\"timerName\":\"${escapeJson(action.timerName)}\"}"
        is RuleAction.CreateObject -> "{\"type\":\"CreateObject\",\"className\":\"${escapeJson(action.className)}\",\"target\":\"${escapeJson(action.target)}\"}"
    }
}

private fun parseRuleActionFromMap(map: Map<String, Any?>): RuleAction? {
    val type = map["type"]?.toString() ?: return null
    return when (type) {
        "Assignment" -> {
            val tVar = map["targetVariable"]?.toString() ?: ""
            val vExpr = map["valueExpression"]?.toString() ?: ""
            RuleAction.Assignment(tVar, vExpr)
        }
        "Focus" -> {
            val fLabel = map["focusLabel"]?.toString() ?: ""
            RuleAction.Focus(fLabel)
        }
        "TimerAction" -> {
            val act = map["action"]?.toString() ?: "start"
            val tName = map["timerName"]?.toString() ?: ""
            RuleAction.TimerAction(act, tName)
        }
        "CreateObject" -> {
            val cName = map["className"]?.toString() ?: ""
            val target = map["target"]?.toString() ?: ""
            RuleAction.CreateObject(cName, target)
        }
        else -> null
    }
}

/**
 * Szöveges Rule Parser: leképezi a szöveges szabályokat Rule objektummá.
 * Támogatja több 'then' akció sorrendi összefűzését.
 */
object RuleParser {
    fun parseRuleText(
        name: String,
        text: String,
        defaultLabel: String = "Alapértelmezett",
        mayCauseChaining: Boolean = true,
        id: String = UUID.randomUUID().toString()
    ): Rule {
        val normalizedText = text.replace("“", "\"").replace("”", "\"").replace("‘", "'").replace("’", "'").trim()
        val lower = normalizedText.lowercase()
        val parser = MathExpressionParser()

        val isUnconditional = lower.startsWith("unconditionally")
        val (condition, actionsText) = if (isUnconditional) {
            val rest = normalizedText.substring(15).trim()
            Pair("true", rest)
        } else {
            val ifIndex = lower.indexOf("if")
            if (ifIndex == -1) {
                throw IllegalArgumentException("A szabálynak 'if...then...', 'if...invoke...', 'if...start/stop timer' vagy 'unconditionally...' formátumúnak kell lennie!")
            }

            val thenIndex = lower.indexOf("then", ifIndex + 2)
            if (thenIndex == -1) {
                throw IllegalArgumentException("A szabálynak tartalmaznia kell a 'then' kulcsszót az 'if' feltétel után!")
            }

            val cond = normalizedText.substring(ifIndex + 2, thenIndex).trim()
            if (cond.isEmpty()) {
                throw IllegalArgumentException("Az 'if' és 'then' közötti feltétel nem lehet üres!")
            }
            val rest = normalizedText.substring(thenIndex + 4).trim()
            Pair(cond, rest)
        }

        val refVars = if (isUnconditional) emptyList() else parser.extractVariables(condition)

        val clauseStrings = splitActionsByThen(actionsText)
        if (clauseStrings.isEmpty()) {
            throw IllegalArgumentException("A szabálynak tartalmaznia kell legalább egy akciót a 'then' vagy 'unconditionally' után!")
        }

        val parsedActions = clauseStrings.map { parseSingleActionClause(it) }

        return Rule(
            id = id,
            name = name,
            label = defaultLabel,
            conditionExpression = condition,
            actions = parsedActions,
            isUnconditional = isUnconditional,
            mayCauseChaining = mayCauseChaining,
            referencedVariables = refVars
        )
    }

    private fun splitActionsByThen(text: String): List<String> {
        val clauses = mutableListOf<String>()
        val sb = StringBuilder()
        var inDoubleQuote = false
        var inSingleQuote = false
        var i = 0

        while (i < text.length) {
            val c = text[i]

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote
                sb.append(c)
                i++
                continue
            }
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote
                sb.append(c)
                i++
                continue
            }

            if (!inDoubleQuote && !inSingleQuote) {
                val remaining = text.substring(i)
                val lowerRemaining = remaining.lowercase()
                if (lowerRemaining.startsWith("then ") || lowerRemaining.startsWith("then\n") || lowerRemaining.startsWith("then\t")) {
                    if (sb.trim().isNotEmpty()) {
                        clauses.add(sb.toString().trim())
                        sb.clear()
                    }
                    i += 4 // Skip "then"
                    continue
                }
            }

            sb.append(c)
            i++
        }

        if (sb.trim().isNotEmpty()) {
            clauses.add(sb.toString().trim())
        }

        return clauses
    }

    private fun parseSingleActionClause(clauseText: String): RuleAction {
        var rest = clauseText.trim()
        if (rest.lowercase().startsWith("then ")) {
            rest = rest.substring(5).trim()
        }
        val lower = rest.lowercase()

        // 1. "invoke <címke> rules" / "focus on <címke>"
        if (lower.startsWith("invoke") || lower.startsWith("focus on") || lower.startsWith("focuse on")) {
            var labelPart = if (lower.startsWith("invoke")) {
                rest.substring(6).trim()
            } else if (lower.startsWith("focus on")) {
                rest.substring(8).trim()
            } else {
                rest.substring(9).trim()
            }
            if (labelPart.lowercase().endsWith("rules")) {
                labelPart = labelPart.substring(0, labelPart.length - 5).trim()
            }
            if (labelPart.isEmpty()) {
                throw IllegalArgumentException("Meg kell adni a meghívandó címkét! Pl.: invoke Klíma rules")
            }
            return RuleAction.Focus(labelPart)
        }

        // 2. "start timer <név>" / "stop timer <név>"
        if (lower.startsWith("start timer") || lower.startsWith("stop timer") ||
            lower.startsWith("start") || lower.startsWith("stop")
        ) {
            val action = if (lower.contains("start")) "start" else "stop"
            val timerIndex = lower.indexOf("timer")
            val tName = if (timerIndex != -1) {
                rest.substring(timerIndex + 5).trim().removeSurrounding("\"").removeSurrounding("'")
            } else {
                rest.substring(action.length).trim().removeSurrounding("\"").removeSurrounding("'")
            }
            if (tName.isEmpty()) {
                throw IllegalArgumentException("Meg kell adni az időzítő nevét! Pl.: start timer Klíma Időzítő")
            }
            return RuleAction.TimerAction(action, tName)
        }

        // 3. "create object from <Class> into <target>"
        if (lower.startsWith("create object") || lower.startsWith("create object from")) {
            var createRest = rest
            if (lower.startsWith("create object from ")) {
                createRest = createRest.substring(19).trim()
            } else if (lower.startsWith("create object ")) {
                createRest = createRest.substring(14).trim()
            }

            val intoIndex = createRest.lowercase().indexOf(" into ")
            if (intoIndex == -1) {
                throw IllegalArgumentException("A 'create object' szabálynak tartalmaznia kell az 'into' kulcsszót! Pl.: create object from Alma into variable alma1")
            }

            val classNamePart = createRest.substring(0, intoIndex).trim()
            var targetPart = createRest.substring(intoIndex + 6).trim()
            val targetLower = targetPart.lowercase()

            if (targetLower.startsWith("variable ")) {
                targetPart = targetPart.substring(9).trim()
            } else if (targetLower.startsWith("property ")) {
                targetPart = targetPart.substring(9).trim()
            }

            if (classNamePart.isEmpty()) {
                throw IllegalArgumentException("Hiányzó Osztály (ClassDef) név a 'create object from' után!")
            }
            if (targetPart.isEmpty()) {
                throw IllegalArgumentException("Hiányzó cél változó vagy tulajdonság név az 'into' után!")
            }

            return RuleAction.CreateObject(classNamePart, targetPart)
        }

        // 4. "conclude that <változó> = <kifejezés>" vagy "<változó> = <kifejezés>"
        var concludePart = rest
        if (lower.startsWith("conclude that ")) {
            concludePart = rest.substring(13).trim()
        } else if (lower.startsWith("conclude ")) {
            concludePart = rest.substring(8).trim()
        }

        val eqIndex = concludePart.indexOf('=')
        if (eqIndex == -1) {
            throw IllegalArgumentException("A 'then' utáni akciónál meg kell adni egy érvényes műveletet (értékadás, időzítő vezérlés, fókusz vagy objektum létrehozás)!")
        }

        val targetVar = concludePart.substring(0, eqIndex).trim()
        val valExpr = concludePart.substring(eqIndex + 1).trim()

        if (targetVar.isEmpty()) {
            throw IllegalArgumentException("Hiányzó cél változó név az '=' előtt!")
        }
        if (valExpr.isEmpty()) {
            throw IllegalArgumentException("Hiányzó érték kifejezés az '=' után!")
        }

        return RuleAction.Assignment(targetVar, valExpr)
    }
}

/**
 * Felhasználó által definiált függvény adatai (szerializálható formátumban).
 */
data class UserFunctionData(
    val name: String,
    val parameters: List<String> = emptyList(),
    val expressionBody: String = ""
) : java.io.Serializable

/**
 * Egyedi névvel rendelkező Munkaterület (Workspace),
 * amely szabályokat (Rules), változókat, függvényeket, időzítőket (Timers), osztályokat (ClassDef) és objektumokat (ObjectInstance) tárol.
 */
data class Workspace(
    val name: String,
    val variables: Map<String, String> = emptyMap(),
    val variableTypes: Map<String, String> = emptyMap(),
    val functions: List<UserFunctionData> = emptyList(),
    val rules: List<Rule> = emptyList(),
    val timers: List<Timer> = emptyList(),
    val classDefs: Map<String, ClassDef> = emptyMap(),
    val objects: Map<String, ObjectInstance> = emptyMap()
) {
    fun getVariableType(varName: String): String {
        val explicitType = variableTypes[varName]
        if (!explicitType.isNullOrEmpty()) return explicitType

        if (objects.containsKey(varName)) {
            return objects[varName]?.className ?: "StringNode"
        }

        val valStr = variables[varName]?.trim() ?: ""
        if (valStr.lowercase() == "true" || valStr.lowercase() == "false" || valStr.lowercase() == "igaz" || valStr.lowercase() == "hamis") {
            return "BooleanNode"
        }
        if (valStr.toDoubleOrNull() != null) {
            return "NumberNode"
        }
        return "StringNode"
    }

    fun toJsonString(): String {
        val varsStr = variables.toMap().entries.joinToString(",") { (k, v) ->
            "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
        }
        val varTypesStr = variableTypes.toMap().entries.joinToString(",") { (k, v) ->
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
        return "{\"name\":\"${escapeJson(name)}\",\"variables\":{$varsStr},\"variableTypes\":{$varTypesStr},\"functions\":[$funcsStr],\"rules\":[$rulesStr],\"timers\":[$timersStr],\"classDefs\":[$classDefsStr],\"objects\":[$objectsStr]}"
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Workspace {
            val name = map["name"] as? String ?: "Workspace"

            val varsMap = mutableMapOf<String, String>()
            (map["variables"] as? Map<String, Any?>)?.forEach { (k, v) ->
                varsMap[k] = v?.toString() ?: ""
            }

            val varTypesMap = mutableMapOf<String, String>()
            (map["variableTypes"] as? Map<String, Any?>)?.forEach { (k, v) ->
                varTypesMap[k] = v?.toString() ?: "StringNode"
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

            return Workspace(name, varsMap, varTypesMap, funcsList, rulesList, timersList, classDefsMap, objectsMap)
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
            variableTypes = mapOf("v1" to "StringNode", "a" to "NumberNode", "b" to "NumberNode", "temp" to "NumberNode", "isHot" to "BooleanNode", "x1" to "NumberNode"),
            functions = listOf(
                UserFunctionData("függvény1", listOf("x"), "2 * x")
            ),
            rules = listOf(
                Rule(
                    name = "Init Szabály - x1 kezdőérték",
                    label = "init",
                    conditionExpression = "true",
                    actions = listOf(RuleAction.Assignment("x1", "2")),
                    isUnconditional = true
                ),
                Rule(
                    name = "Chaining Szabály - x1 növelése",
                    label = "Teszt",
                    conditionExpression = "x1 <= 10",
                    actions = listOf(RuleAction.Assignment("x1", "x1 + 2")),
                    mayCauseChaining = true,
                    referencedVariables = listOf("x1")
                ),
                Rule(
                    name = "Kánikula Fókusz",
                    label = "init",
                    conditionExpression = "isHot == true",
                    actions = listOf(RuleAction.Focus("Klíma")),
                    referencedVariables = listOf("isHot")
                ),
                Rule(
                    name = "Klíma Időzítő Indítása",
                    label = "Klíma",
                    conditionExpression = "temp > 25",
                    actions = listOf(RuleAction.TimerAction("start", "Klíma Időzítő")),
                    referencedVariables = listOf("temp")
                ),
                Rule(
                    name = "Klíma Időzítő Leállítása",
                    label = "Klíma",
                    conditionExpression = "temp <= 20",
                    actions = listOf(RuleAction.TimerAction("stop", "Klíma Időzítő")),
                    referencedVariables = listOf("temp")
                ),
                Rule(
                    name = "Hőmérséklet Riasztás",
                    label = "Klíma",
                    conditionExpression = "temp > 25",
                    actions = listOf(RuleAction.Assignment("status", "\"Hűtés_szükséges\"")),
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
                    properties = mapOf("csemete" to "Almafa", "almák" to "Alma")
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
            variableTypes = mapOf("g" to "NumberNode", "m" to "NumberNode"),
            functions = listOf(
                UserFunctionData("f1", listOf("x"), "3 * x"),
                UserFunctionData("f2", listOf("b"), "b ^ 2")
            ),
            rules = listOf(
                Rule(
                    name = "Erő számítás",
                    label = "Fizika",
                    conditionExpression = "m > 0",
                    actions = listOf(RuleAction.Assignment("F", "m * g")),
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
