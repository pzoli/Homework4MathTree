package com.example.homework4mathtree.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.homework4mathtree.model.MathNode
import com.example.homework4mathtree.parser.MathExpressionParser
import com.example.homework4mathtree.parser.UserFunction

/**
 * ViewModel a matematikai kifejezésfa alkalmazás állapotának megőrzésére.
 * Megakadályozza az adatok elvesztését képernyőforgatáskor (Configuration Change).
 */
class MathTreeViewModel : ViewModel() {

    var expressionText by mutableStateOf("függvény1(10 * a + 2 * függvény2(b))")
        private set

    var variableInputs by mutableStateOf(mapOf("a" to "1", "b" to "3", "x" to "45"))
        private set

    var isDegreeMode by mutableStateOf(true) // true: Fok (DEG), false: Radián (RAD)
        private set

    var userFunctionsList by mutableStateOf(
        listOf(
            UserFunction(
                name = "függvény1",
                parameters = listOf("x"),
                expressionBody = "2 * x"
            ),
            UserFunction(
                name = "függvény2",
                parameters = listOf("b"),
                expressionBody = "b ^ 2"
            )
        )
    )
        private set

    var selectedTab by mutableIntStateOf(0) // 0: Kártya Nézet, 1: Grafikus Ábra
        private set

    var parsedTree by mutableStateOf<MathNode?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val parser = MathExpressionParser()

    init {
        autoDetectVariables()
        buildTree()
    }

    fun updateExpressionText(newText: String) {
        expressionText = newText
        autoDetectVariables()
        buildTree()
    }

    fun updateVariableInput(varName: String, valueStr: String) {
        variableInputs += (varName to valueStr)
        buildTree()
    }

    fun addVariable(varName: String, valueStr: String) {
        variableInputs += (varName to valueStr)
        buildTree()
    }

    fun removeVariable(varName: String) {
        variableInputs -= varName
        buildTree()
    }

    fun updateDegreeMode(isDegree: Boolean) {
        isDegreeMode = isDegree
        buildTree()
    }

    fun updateSelectedTab(tabIndex: Int) {
        selectedTab = tabIndex
    }

    fun addOrUpdateUserFunction(function: UserFunction, editIndex: Int? = null) {
        val updated = userFunctionsList.toMutableList()
        if ((editIndex != null) && (editIndex in updated.indices)) {
            updated[editIndex] = function
        } else {
            updated.add(function)
        }
        userFunctionsList = updated
        autoDetectVariables()
        buildTree()
    }

    fun removeUserFunction(index: Int) {
        if (index in userFunctionsList.indices) {
            val updated = userFunctionsList.toMutableList()
            updated.removeAt(index)
            userFunctionsList = updated
            autoDetectVariables()
            buildTree()
        }
    }

    private fun autoDetectVariables() {
        val userFuncMap = userFunctionsList.associateBy { it.name }
        val detectedVars = parser.extractVariables(expressionText, userFuncMap)
        val updatedMap = variableInputs.toMutableMap()
        var changed = false
        detectedVars.forEach { varName ->
            if (!updatedMap.containsKey(varName)) {
                updatedMap[varName] = "1"
                changed = true
            }
        }
        if (changed) {
            variableInputs = updatedMap
        }
    }

    fun buildTree() {
        errorMessage = null
        try {
            val doubleVariables = variableInputs.mapValues {
                it.value.toDoubleOrNull() ?: 0.0
            }
            val userFuncMap = userFunctionsList.associateBy { it.name }
            parsedTree = parser.parse(expressionText, doubleVariables, userFuncMap, isDegreeMode)
        } catch (e: Exception) {
            parsedTree = null
            errorMessage = e.message ?: "Hiba történt a kifejezés feldolgozása során!"
        }
    }
}
