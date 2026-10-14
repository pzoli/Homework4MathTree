package com.example.homework4mathtree.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homework4mathtree.model.MathNode
import com.example.homework4mathtree.model.Rule
import com.example.homework4mathtree.model.Simulation
import com.example.homework4mathtree.model.UserFunctionData
import com.example.homework4mathtree.model.Value
import com.example.homework4mathtree.model.Workspace
import com.example.homework4mathtree.parser.MathExpressionParser
import com.example.homework4mathtree.parser.UserFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RuleResult(
    val conditionMet: Boolean,
    val assignedValue: Value? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel a Munkaterületeket (Workspaces) tároló Szimuláció (Simulation),
 * a Szabályok (Rules) és az aszinkron Process Queue kiértékelési motor kezelésére.
 */
class MathTreeViewModel : ViewModel() {

    var simulation by mutableStateOf(Simulation())
        private set

    var selectedTab by mutableIntStateOf(0) // 0: Kártya Nézet, 1: Grafikus Ábra
        private set

    var isDegreeMode by mutableStateOf(true) // true: Fok (DEG), false: Radián (RAD)
        private set

    // Process Queue állapotok
    var isQueueProcessing by mutableStateOf(false)
        private set

    var processQueueSize by mutableIntStateOf(0)
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

    init {
        autoDetectVariables()
        evaluateRules()
    }

    /**
     * Initialization gomb: Az "init" vagy "initialization" címkéjű (valamint a feltétel nélküli)
     * szabályokat beteszi a Process Queue-ba és elindítja az aszinkron feldolgozást.
     */
    fun triggerInitialization() {
        val initRules = activeWorkspace.rules.filter {
            it.label.equals("init", ignoreCase = true) ||
                it.label.equals("initialization", ignoreCase = true) ||
                it.isUnconditional
        }
        enqueueAndProcessRules(initRules)
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
     * Szabályok kiértékelése Aszinkron módon a Process Queue használatával.
     */
    fun evaluateRules() {
        enqueueAndProcessRules(activeWorkspace.rules)
    }

    /**
     * Aszinkron ütemezett szabály-feldolgozó motor (Process Queue).
     * Ha egy változó értéke megváltozik ÉS a szabály engedélyezi a láncolást (mayCauseChaining = true),
     * a program automatikusan sorba állítja az adott változóra hivatkozó szabályokat.
     */
    fun enqueueAndProcessRules(rulesToEnqueue: List<Rule>) {
        viewModelScope.launch(Dispatchers.Default) {
            val queue = ArrayDeque<Rule>()
            queue.addAll(rulesToEnqueue)

            val updatedVars = activeWorkspace.variables.toMutableMap()
            val resultsMap = ruleResults.toMutableMap()
            val userFuncMap = activeWorkspace.functions.associate {
                it.name to UserFunction(it.name, it.parameters, it.expressionBody)
            }

            var iterations = 0
            val maxIterations = 100 // Végtelen ciklus elleni védelem

            withContext(Dispatchers.Main) {
                isQueueProcessing = true
                processQueueSize = queue.size
            }

            while (queue.isNotEmpty() && iterations < maxIterations) {
                iterations++
                val currentRule = queue.removeFirst()

                withContext(Dispatchers.Main) {
                    processQueueSize = queue.size
                }

                delay(50) // Késleltetés az ütemezés vizuális követhetőségéhez

                try {
                    val valueVariables = updatedVars.mapValues { parseVariableValue(it.value) }
                    val condNode = parser.parse(
                        input = currentRule.conditionExpression,
                        variables = valueVariables,
                        userFunctions = userFuncMap,
                        isDegreeMode = isDegreeMode,
                        simulation = simulation
                    )
                    val condMet = condNode.getValue().toBoolean()

                    if (condMet) {
                        if (currentRule.isFocusRule) {
                            resultsMap[currentRule.id] = RuleResult(conditionMet = true)
                            val targetLabel = currentRule.focusLabel
                            if (targetLabel != null) {
                                val focusRules = activeWorkspace.rules.filter {
                                    it.label.equals(targetLabel, ignoreCase = true) && !queue.contains(it)
                                }
                                queue.addAll(focusRules)
                            }
                        } else {
                            val valueNode = parser.parse(
                                input = currentRule.valueExpression,
                                variables = valueVariables,
                                userFunctions = userFuncMap,
                                isDegreeMode = isDegreeMode,
                                simulation = simulation
                            )
                            val assignedVal = valueNode.getValue()
                            val newDisplayStr = assignedVal.toDisplayString()
                            val oldValue = updatedVars[currentRule.targetVariable]

                            val valueChanged = oldValue != newDisplayStr
                            updatedVars[currentRule.targetVariable] = newDisplayStr
                            resultsMap[currentRule.id] = RuleResult(conditionMet = true, assignedValue = assignedVal)

                            // Ha a változó értéke megváltozott ÉS a szabály engedélyezi a chaining-et:
                            if (valueChanged && currentRule.mayCauseChaining) {
                                val dependentRules = activeWorkspace.rules.filter { dependentRule ->
                                    dependentRule.isActive &&
                                        dependentRule.referencedVariables.contains(currentRule.targetVariable) &&
                                        !queue.contains(dependentRule)
                                }
                                queue.addAll(dependentRules)
                            }
                        }
                    } else {
                        resultsMap[currentRule.id] = RuleResult(conditionMet = false)
                    }
                } catch (e: Exception) {
                    resultsMap[currentRule.id] = RuleResult(conditionMet = false, errorMessage = e.message)
                }

                withContext(Dispatchers.Main) {
                    ruleResults = resultsMap.toMap()
                    if (updatedVars != activeWorkspace.variables) {
                        val updatedWs = activeWorkspace.copy(variables = updatedVars)
                        val updatedWorkspaces = simulation.workspaces + (updatedWs.name to updatedWs)
                        simulation = simulation.copy(workspaces = updatedWorkspaces)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                isQueueProcessing = false
                processQueueSize = 0
            }
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
