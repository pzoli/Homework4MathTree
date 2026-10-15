package hu.infokristaly.homework4mathtree.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.infokristaly.homework4mathtree.model.ExpressionNode
import hu.infokristaly.homework4mathtree.model.Rule
import hu.infokristaly.homework4mathtree.model.Simulation
import hu.infokristaly.homework4mathtree.model.Timer
import hu.infokristaly.homework4mathtree.model.UserFunctionData
import hu.infokristaly.homework4mathtree.model.Value
import hu.infokristaly.homework4mathtree.model.Workspace
import hu.infokristaly.homework4mathtree.parser.MathExpressionParser
import hu.infokristaly.homework4mathtree.parser.UserFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val AUTOSAVE_FILE_NAME = "simulation_autosave.json"

data class RuleResult(
    val conditionMet: Boolean,
    val assignedValue: Value? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel a Munkaterületeket (Workspaces) tároló Szimuláció (Simulation),
 * a Szabályok (Rules), Időzítők (Timers) és az aszinkron Process Queue kezelésére.
 * Támogatja az automatikus fájlba mentést és visszaállítást alkalmazás indításakor.
 */
class MathTreeViewModel(application: Application) : AndroidViewModel(application) {

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

    var selectedRuleConditionTree by mutableStateOf<ExpressionNode?>(null)
        private set

    // A párbeszédablakban megtekintett függvény kifejezés-fája
    var selectedFunctionForTreeDialog by mutableStateOf<UserFunctionData?>(null)
        private set

    var selectedFunctionTree by mutableStateOf<ExpressionNode?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)

    // Aktív időzítők Coroutine Job-jai (timerId -> Job)
    private val activeTimerJobs = mutableMapOf<String, Job>()

    private val parser = MathExpressionParser()

    val activeWorkspace: Workspace
        get() = simulation.getActiveWorkspace()

    val variableInputs: Map<String, String>
        get() = activeWorkspace.variables

    val userFunctionsList: List<UserFunctionData>
        get() = activeWorkspace.functions

    val rulesList: List<Rule>
        get() = activeWorkspace.rules

    val timersList: List<Timer>
        get() = activeWorkspace.timers

    init {
        loadAutoSaveSimulation()
        syncTimers()
    }

    /**
     * Elmenti az aktuális szimuláció JSON állapotát az alkalmazás privát tárhelyére (automatikus mentés).
     */
    private fun autoSaveSimulation() {
        try {
            val context = getApplication<Application>().applicationContext
            val file = File(context.filesDir, AUTOSAVE_FILE_NAME)
            file.writeText(simulation.toJsonString())
        } catch (_: Exception) { }
    }

    /**
     * Betölti az automatikusan elmentett szimuláció JSON-t az alkalmazás indításakor.
     */
    private fun loadAutoSaveSimulation() {
        try {
            val context = getApplication<Application>().applicationContext
            val file = File(context.filesDir, AUTOSAVE_FILE_NAME)
            if (file.exists()) {
                val jsonStr = file.readText()
                if (jsonStr.isNotEmpty()) {
                    val importedSim = Simulation.fromJsonString(jsonStr)
                    simulation = importedSim
                    evaluateRules()
                    return
                }
            }
        } catch (_: Exception) { }

        autoDetectVariables()
        evaluateRules()
    }

    /**
     * Initialization gomb: Az "init" vagy "initialization" címkéjű szabályokat
     * beteszi a Process Queue-ba és elindítja az aszinkron feldolgozást.
     */
    fun triggerInitialization() {
        val initRules = activeWorkspace.rules.filter {
            it.label.equals("init", ignoreCase = true) ||
                it.label.equals("initialization", ignoreCase = true)
        }
        enqueueAndProcessRules(initRules)
    }

    fun switchWorkspace(workspaceName: String) {
        if (simulation.workspaces.containsKey(workspaceName)) {
            simulation = simulation.copy(activeWorkspaceName = workspaceName)
            autoDetectVariables()
            evaluateRules()
            syncTimers()
            autoSaveSimulation()
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
                rules = emptyList(),
                timers = emptyList()
            )
            val updatedMap = simulation.workspaces + (trimmed to newWs)
            simulation = simulation.copy(
                activeWorkspaceName = trimmed,
                workspaces = updatedMap
            )
            autoDetectVariables()
            evaluateRules()
            syncTimers()
            autoSaveSimulation()
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
            syncTimers()
            autoSaveSimulation()
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

    fun addOrUpdateTimer(timer: Timer, editIndex: Int? = null) {
        val updatedTimers = activeWorkspace.timers.toMutableList()
        if ((editIndex != null) && (editIndex in updatedTimers.indices)) {
            updatedTimers[editIndex] = timer
        } else {
            updatedTimers.add(timer)
        }
        val currentWs = activeWorkspace.copy(timers = updatedTimers)
        updateCurrentWorkspace(currentWs)
        syncTimers()
    }

    fun toggleTimerRunning(timerId: String, isRunning: Boolean) {
        val updatedTimers = activeWorkspace.timers.map { t ->
            if (t.id == timerId) t.copy(isRunning = isRunning) else t
        }
        val currentWs = activeWorkspace.copy(timers = updatedTimers)
        val updatedWorkspaces = simulation.workspaces + (currentWs.name to currentWs)
        simulation = simulation.copy(workspaces = updatedWorkspaces)
        syncTimers()
        autoSaveSimulation()
    }

    fun removeTimer(index: Int) {
        if (index in activeWorkspace.timers.indices) {
            val updatedTimers = activeWorkspace.timers.toMutableList()
            val removed = updatedTimers.removeAt(index)
            activeTimerJobs[removed.id]?.cancel()
            activeTimerJobs.remove(removed.id)
            val currentWs = activeWorkspace.copy(timers = updatedTimers)
            updateCurrentWorkspace(currentWs)
            syncTimers()
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

    /**
     * Megnyitja a megadott felhasználói függvény kifejezés-fájának megjelenítő párbeszédablakát.
     */
    fun openFunctionTreeDialog(function: UserFunctionData) {
        selectedFunctionForTreeDialog = function
        try {
            val sampleVars = activeWorkspace.variables.mapValues { parseVariableValue(it.value) }.toMutableMap()
            function.parameters.forEach { param ->
                if (!sampleVars.containsKey(param)) {
                    sampleVars[param] = Value.Number(1.0)
                }
            }
            val userFuncMap = activeWorkspace.functions.associate {
                it.name to UserFunction(it.name, it.parameters, it.expressionBody)
            }
            selectedFunctionTree = parser.parse(
                input = function.expressionBody,
                variables = sampleVars,
                userFunctions = userFuncMap,
                isDegreeMode = isDegreeMode,
                simulation = simulation
            )
        } catch (_: Exception) {
            selectedFunctionTree = null
        }
    }

    fun closeFunctionTreeDialog() {
        selectedFunctionForTreeDialog = null
        selectedFunctionTree = null
    }

    fun exportSimulationToJson(): String {
        return simulation.toJsonString()
    }

    fun importSimulationFromJson(jsonString: String) {
        try {
            val importedSim = Simulation.fromJsonString(jsonString)
            simulation = importedSim
            evaluateRules()
            syncTimers()
            autoSaveSimulation()
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Érvénytelen JSON formátum: ${e.message}"
        }
    }

    /**
     * Szabályok általános kiértékelése Aszinkron módon a Process Queue használatával.
     * Az 'unconditionally' (feltétel nélküli) szabályok kihagyásra kerülnek általános kiértékeléskor;
     * csak akkor hajtódnak végre, ha kifejezetten rájuk fókuszál a szimuláció (invoke <címke> rules).
     */
    fun evaluateRules() {
        val conditionalRules = activeWorkspace.rules.filter { !it.isUnconditional }
        enqueueAndProcessRules(conditionalRules)
    }

    /**
     * Aszinkron ütemezett szabály-feldolgozó motor (Process Queue).
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
                        if (currentRule.isTimerActionRule) {
                            resultsMap[currentRule.id] = RuleResult(conditionMet = true)
                            val action = currentRule.timerAction
                            val tName = currentRule.timerName
                            if (action != null && tName != null) {
                                val targetTimer = activeWorkspace.timers.find {
                                    it.name.equals(tName, ignoreCase = true)
                                }
                                if (targetTimer != null) {
                                    val shouldRun = action.equals("start", ignoreCase = true)
                                    if (targetTimer.isRunning != shouldRun) {
                                        withContext(Dispatchers.Main) {
                                            toggleTimerRunning(targetTimer.id, shouldRun)
                                        }
                                    }
                                }
                            }
                        } else if (currentRule.isFocusRule) {
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
                                        !dependentRule.isUnconditional &&
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

    /**
     * Szinkronizálja az aktív időzítők háttérben futó Coroutine feladatait.
     */
    private fun syncTimers() {
        val activeWs = activeWorkspace
        val activeTimerIds = activeWs.timers.filter { it.isRunning }.map { it.id }.toSet()

        // Leállítjuk azon időzítők feladatait, amelyek már nem futnak
        activeTimerJobs.keys.toList().forEach { timerId ->
            if (!activeTimerIds.contains(timerId)) {
                activeTimerJobs[timerId]?.cancel()
                activeTimerJobs.remove(timerId)
            }
        }

        // Elindítjuk az újonnan bekapcsolt időzítők feladatait
        activeWs.timers.filter { it.isRunning }.forEach { timer ->
            if (!activeTimerJobs.containsKey(timer.id)) {
                val job = viewModelScope.launch(Dispatchers.Default) {
                    while (isActive) {
                        delay(timer.intervalMillis)
                        val targetRules = activeWorkspace.rules.filter {
                            it.isActive && it.label.equals(timer.targetLabel, ignoreCase = true)
                        }
                        if (targetRules.isNotEmpty()) {
                            enqueueAndProcessRules(targetRules)
                        }
                    }
                }
                activeTimerJobs[timer.id] = job
            }
        }
    }

    private fun updateCurrentWorkspace(updatedWs: Workspace) {
        val updatedWorkspaces = simulation.workspaces + (updatedWs.name to updatedWs)
        simulation = simulation.copy(workspaces = updatedWorkspaces)
        autoDetectVariables()
        evaluateRules()
        autoSaveSimulation()
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
