package com.example.homework4mathtree.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.homework4mathtree.model.MathNode
import com.example.homework4mathtree.model.Rule
import com.example.homework4mathtree.model.Simulation
import com.example.homework4mathtree.model.UserFunctionData
import com.example.homework4mathtree.model.Value
import com.example.homework4mathtree.model.Workspace
import com.example.homework4mathtree.parser.MathExpressionParser
import com.example.homework4mathtree.parser.UserFunction

data class RuleResult(
    val conditionMet: Boolean,
    val assignedValue: Value? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel a Munkaterületeket (Workspaces) tároló Szimuláció (Simulation)
 * és a Szabályok (Rules: if...then...conclude that) kiértékelésének kezelésére.
 */
class MathTreeViewModel : ViewModel() {

    var simulation by mutableStateOf(Simulation())
        private set

    var selectedTab by mutableIntStateOf(0) // 0: Kártya Nézet, 1: Grafikus Ábra
        private set

    var isDegreeMode by mutableStateOf(true) // true: Fok (DEG), false: Radián (RAD)
        private set

    // Aktuális fókuszált címke szűrő (null = Összes címke)
    var selectedLabelFilter by mutableStateOf<String?>(null)
        private set

    // Szabályok kiértékelési eredményei (ruleId -> RuleResult)
    var ruleResults by mutableStateOf(mapOf<String, RuleResult>())
        private set

    // A párbeszédablakban megtekintett szabály feltétel-fája
    var selectedRuleForTreeDialog by mutableStateOf<Rule?>(null)
        private set

    var selectedRuleConditionTree by mutableStateOf<MathNode?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val parser = MathExpressionParser()

    val activeWorkspace: Workspace
        get() = simulation.getActiveWorkspace()

    val variableInputs: Map<String, String>
        get() = activeWorkspace.variables

    val userFunctionsList: List<UserFunctionData>
        get() = activeWorkspace.functions

    val rulesList: List<Rule>
        get() = activeWorkspace.rules

    val availableLabels: List<String>
        get() = rulesList.map { it.label }.distinct()

    init {
        autoDetectVariables()
        evaluateRules()
    }

    fun switchWorkspace(workspaceName: String) {
        if (simulation.workspaces.containsKey(workspaceName)) {
            simulation = simulation.copy(activeWorkspaceName = workspaceName)
            autoDetectVariables()
            evaluateRules()
        }
    }

    fun createWorkspace(workspaceName: String) {
        val trimmed = workspaceName.trim()
        if (trimmed.isEmpty()) return
        if (!simulation.workspaces.containsKey(trimmed)) {
            val newWs = Workspace(
                name = trimmed,
                variables = mapOf("x" to "10"),
                functions = emptyList(),
                rules = emptyList()
            )
            val updatedMap = simulation.workspaces + (trimmed to newWs)
            simulation = simulation.copy(
                activeWorkspaceName = trimmed,
                workspaces = updatedMap
            )
            autoDetectVariables()
            evaluateRules()
        }
    }

    fun deleteActiveWorkspace() {
        if (simulation.workspaces.size > 1) {
            val currentName = activeWorkspace.name
            val updatedMap = simulation.workspaces - currentName
            val newActive = updatedMap.keys.first()
            simulation = simulation.copy(
                activeWorkspaceName = newActive,
                workspaces = updatedMap
            )
            autoDetectVariables()
            evaluateRules()
        }
    }

    fun selectLabelFilter(label: String?) {
        selectedLabelFilter = label
    }

    fun updateVariableInput(varName: String, valueStr: String) {
        val updatedVars = activeWorkspace.variables + (varName to valueStr)
        val currentWs = activeWorkspace.copy(variables = updatedVars)
        updateCurrentWorkspace(currentWs)
    }

    fun addVariable(varName: String, valueStr: String) {
        updateVariableInput(varName, valueStr)
    }

    fun removeVariable(varName: String) {
        val updatedVars = activeWorkspace.variables - varName
        val currentWs = activeWorkspace.copy(variables = updatedVars)
        updateCurrentWorkspace(currentWs)
    }

    fun updateDegreeMode(isDegree: Boolean) {
        isDegreeMode = isDegree
        evaluateRules()
    }

    fun updateSelectedTab(tabIndex: Int) {
        selectedTab = tabIndex
    }

    fun addOrUpdateUserFunction(function: UserFunctionData, editIndex: Int? = null) {
        val updatedFuncs = activeWorkspace.functions.toMutableList()
        if ((editIndex != null) && (editIndex in updatedFuncs.indices)) {
            updatedFuncs[editIndex] = function
        } else {
            updatedFuncs.add(function)
        }
        val currentWs = activeWorkspace.copy(functions = updatedFuncs)
        updateCurrentWorkspace(currentWs)
    }

    fun removeUserFunction(index: Int) {
        if (index in activeWorkspace.functions.indices) {
            val updatedFuncs = activeWorkspace.functions.toMutableList()
            updatedFuncs.removeAt(index)
            val currentWs = activeWorkspace.copy(functions = updatedFuncs)
            updateCurrentWorkspace(currentWs)
        }
    }

    fun addOrUpdateRule(rule: Rule, editIndex: Int? = null) {
        val updatedRules = activeWorkspace.rules.toMutableList()
        if ((editIndex != null) && (editIndex in updatedRules.indices)) {
            updatedRules[editIndex] = rule
        } else {
            updatedRules.add(rule)
        }
        val currentWs = activeWorkspace.copy(rules = updatedRules)
        updateCurrentWorkspace(currentWs)
    }

    fun removeRule(index: Int) {
        if (index in activeWorkspace.rules.indices) {
            val updatedRules = activeWorkspace.rules.toMutableList()
            updatedRules.removeAt(index)
            val currentWs = activeWorkspace.copy(rules = updatedRules)
            updateCurrentWorkspace(currentWs)
        }
    }

    /**
     * Megnyitja a megadott szabály feltétel-fájának megjelenítő párbeszédablakát.
     */
    fun openRuleConditionTreeDialog(rule: Rule) {
        selectedRuleForTreeDialog = rule
        try {
            val valueVariables = activeWorkspace.variables.mapValues { parseVariableValue(it.value) }
            val userFuncMap = activeWorkspace.functions.associate {
                it.name to UserFunction(it.name, it.parameters, it.expressionBody)
            }
            selectedRuleConditionTree = parser.parse(
                input = rule.conditionExpression,
                variables = valueVariables,
                userFunctions = userFuncMap,
                isDegreeMode = isDegreeMode,
                simulation = simulation
            )
        } catch (_: Exception) {
            selectedRuleConditionTree = null
        }
    }

    fun closeRuleConditionTreeDialog() {
        selectedRuleForTreeDialog = null
        selectedRuleConditionTree = null
    }

    fun exportSimulationToJson(): String {
        return simulation.toJsonString()
    }

    fun importSimulationFromJson(jsonString: String) {
        try {
            val importedSim = Simulation.fromJsonString(jsonString)
            simulation = importedSim
            evaluateRules()
        } catch (e: Exception) {
            errorMessage = "Érvénytelen JSON formátum: ${e.message}"
        }
    }

    /**
     * Szabályok kiértékelése (Futtatása).
     * Ha szűrő van beállítva (selectedLabelFilter), csak az adott címkével rendelkező szabályok futnak le.
     * A "focus on <címke>" típusú szabályok rekurzívan lefuttatják a cél címkéhez tartozó szabályokat is.
     */
    fun evaluateRules(labelFilter: String? = selectedLabelFilter) {
        errorMessage = null
        val currentWs = activeWorkspace
        val updatedVars = currentWs.variables.toMutableMap()
        val resultsMap = mutableMapOf<String, RuleResult>()

        val userFuncMap = currentWs.functions.associate {
            it.name to UserFunction(it.name, it.parameters, it.expressionBody)
        }

        fun evaluateRuleList(rulesToRun: List<Rule>) {
            rulesToRun.forEach { rule ->
                if (!rule.isActive) return@forEach
                if (labelFilter != null && labelFilter != "Összes" && rule.label != labelFilter) {
                    return@forEach
                }

                try {
                    val valueVariables = updatedVars.mapValues { parseVariableValue(it.value) }
                    val condNode = parser.parse(
                        input = rule.conditionExpression,
                        variables = valueVariables,
                        userFunctions = userFuncMap,
                        isDegreeMode = isDegreeMode,
                        simulation = simulation
                    )
                    val condMet = condNode.getValue().toBoolean()

                    if (condMet) {
                        if (rule.isFocusRule) {
                            resultsMap[rule.id] = RuleResult(conditionMet = true)
                            val targetLabel = rule.focusLabel
                            if (targetLabel != null) {
                                val focusRules = currentWs.rules.filter {
                                    it.label.equals(targetLabel, ignoreCase = true) && it.id != rule.id
                                }
                                evaluateRuleList(focusRules)
                            }
                        } else {
                            val valueNode = parser.parse(
                                input = rule.valueExpression,
                                variables = valueVariables,
                                userFunctions = userFuncMap,
                                isDegreeMode = isDegreeMode,
                                simulation = simulation
                            )
                            val assignedVal = valueNode.getValue()
                            updatedVars[rule.targetVariable] = assignedVal.toDisplayString()
                            resultsMap[rule.id] = RuleResult(conditionMet = true, assignedValue = assignedVal)
                        }
                    } else {
                        resultsMap[rule.id] = RuleResult(conditionMet = false)
                    }
                } catch (e: Exception) {
                    resultsMap[rule.id] = RuleResult(conditionMet = false, errorMessage = e.message)
                }
            }
        }

        evaluateRuleList(currentWs.rules)

        ruleResults = resultsMap
        if (updatedVars != currentWs.variables) {
            val updatedWs = currentWs.copy(variables = updatedVars)
            val updatedWorkspaces = simulation.workspaces + (updatedWs.name to updatedWs)
            simulation = simulation.copy(workspaces = updatedWorkspaces)
        }
    }

    private fun updateCurrentWorkspace(updatedWs: Workspace) {
        val updatedWorkspaces = simulation.workspaces + (updatedWs.name to updatedWs)
        simulation = simulation.copy(workspaces = updatedWorkspaces)
        autoDetectVariables()
        evaluateRules()
    }

    private fun autoDetectVariables() {
        val currentWs = activeWorkspace
        val userFuncMap = currentWs.functions.associate {
            it.name to UserFunction(it.name, it.parameters, it.expressionBody)
        }
        val updatedVars = currentWs.variables.toMutableMap()
        var changed = false

        currentWs.rules.forEach { rule ->
            val detectedCondVars = parser.extractVariables(rule.conditionExpression, userFuncMap, simulation)
            val detectedValVars = if (rule.valueExpression.isNotEmpty()) {
                parser.extractVariables(rule.valueExpression, userFuncMap, simulation)
            } else {
                emptyList()
            }

            (detectedCondVars + detectedValVars).distinct().forEach { varName ->
                if (!updatedVars.containsKey(varName) && !varName.contains(".")) {
                    updatedVars[varName] = "1"
                    changed = true
                }
            }
        }

        if (changed) {
            val newWs = currentWs.copy(variables = updatedVars)
            val updatedWorkspaces = simulation.workspaces + (newWs.name to newWs)
            simulation = simulation.copy(workspaces = updatedWorkspaces)
        }
    }

    private fun parseVariableValue(inputStr: String): Value {
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
}
