package com.example.homework4mathtree.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework4mathtree.model.MathNode
import com.example.homework4mathtree.parser.MathExpressionParser
import com.example.homework4mathtree.parser.UserFunction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathTreeApp() {
    var expressionText by remember { mutableStateOf("függvény1(10 * a + 2 * függvény2(b))") }
    var variableInputs by remember { mutableStateOf(mapOf("a" to "1", "b" to "3", "x" to "45")) }
    var isDegreeMode by remember { mutableStateOf(true) } // true: Fok (DEG), false: Radián (RAD)

    var userFunctionsList by remember {
        mutableStateOf(
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
    }

    var parsedTree by remember { mutableStateOf<MathNode?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Kártya Nézet, 1: Grafikus Ábra

    // Változó hozzáadása dialog
    var showAddVarDialog by remember { mutableStateOf(false) }
    var newVarName by remember { mutableStateOf("") }
    var newVarValue by remember { mutableStateOf("") }

    // Függvény hozzáadása / szerkesztése dialog
    var showFuncDialog by remember { mutableStateOf(false) }
    var editingFuncIndex by remember { mutableStateOf<Int?>(null) }
    var funcNameInput by remember { mutableStateOf("") }
    var funcParamsInput by remember { mutableStateOf("") }
    var funcBodyInput by remember { mutableStateOf("") }

    val parser = remember { MathExpressionParser() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    fun getUserFunctionsMap(): Map<String, UserFunction> {
        return userFunctionsList.associateBy { it.name }
    }

    // Automatikus változó felismerés kifejezés vagy függvények változásakor
    LaunchedEffect(expressionText, userFunctionsList) {
        val userFuncMap = getUserFunctionsMap()
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
        keyboardController?.hide()
        errorMessage = null
        try {
            val doubleVariables = variableInputs.mapValues {
                it.value.toDoubleOrNull() ?: 0.0
            }
            val userFuncMap = getUserFunctionsMap()
            val root = parser.parse(expressionText, doubleVariables, userFuncMap, isDegreeMode)
            parsedTree = root
        } catch (e: Exception) {
            parsedTree = null
            errorMessage = e.message ?: "Hiba történt a kifejezés feldolgozása során!"
        }
    }

    // Automatikus elemzés kifejezés, változók, függvények vagy szögmód módosításakor
    LaunchedEffect(expressionText, variableInputs, userFunctionsList, isDegreeMode) {
        buildTree()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Matematikai Bináris Fa Építő",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Beviteli mező és kifejezés beállítások
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matematikai kifejezés megadása:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Szög mértékegység gombok (Fok vs Radián)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = isDegreeMode,
                                onClick = { isDegreeMode = true },
                                label = { Text("Fok (°)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                            FilterChip(
                                selected = !isDegreeMode,
                                onClick = { isDegreeMode = false },
                                label = { Text("Rad", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = expressionText,
                        onValueChange = { expressionText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Pl.: sin(90) + cos(0) vagy sin(x ^ 2)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { buildTree() }),
                        trailingIcon = {
                            if (expressionText.isNotEmpty()) {
                                TextButton(onClick = { expressionText = "" }) {
                                    Text("✕")
                                }
                            }
                        }
                    )

                    // Gyors szimbólum, szögfüggvény és változó/függvény beszúró gombok
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val symbols = listOf(
                            "+", "-", "*", "/", "^", "√", "(", ")", ",",
                            "sin", "cos", "tan", "cot", "π", "a", "b", "x", "y"
                        ) + userFunctionsList.map { it.name }

                        symbols.distinct().forEach { symbol ->
                            SuggestionChip(
                                onClick = { expressionText += " $symbol " },
                                label = {
                                    Text(
                                        text = symbol,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }

                    // Minta kifejezések
                    Text(
                        text = "Minta kifejezések:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "sin(90) + cos(0)",
                            "tan(x) * cot(x)",
                            "sin(x) ^ 2 + cos(x) ^ 2",
                            "függvény1(10 * a + 2 * függvény2(b))",
                            "a ^ 2 + b ^ 2"
                        )
                        presets.forEach { preset ->
                            FilterChip(
                                selected = expressionText == preset,
                                onClick = {
                                    expressionText = preset
                                },
                                label = { Text(preset, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Saját Függvények és Elnevezett Kifejezések Szekció
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saját Függvények & Kifejezések:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        TextButton(
                            onClick = {
                                editingFuncIndex = null
                                funcNameInput = ""
                                funcParamsInput = "x"
                                funcBodyInput = ""
                                showFuncDialog = true
                            }
                        ) {
                            Text("+ Új függvény", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (userFunctionsList.isEmpty()) {
                        Text(
                            text = "Nincsenek egyéni függvények. Hozz létre újat a '+ Új függvény' gombbal!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        userFunctionsList.forEachIndexed { index, func ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4A148C))
                                        ) {
                                            Text(
                                                text = "f",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }

                                        Column {
                                            val paramsStr = func.parameters.joinToString(", ")
                                            val sig = if (paramsStr.isEmpty()) func.name else "${func.name}($paramsStr)"
                                            Text(
                                                text = "$sig = ${func.expressionBody}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingFuncIndex = index
                                                funcNameInput = func.name
                                                funcParamsInput = func.parameters.joinToString(", ")
                                                funcBodyInput = func.expressionBody
                                                showFuncDialog = true
                                            }
                                        ) {
                                            Text("✏️", fontSize = 16.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                userFunctionsList = userFunctionsList.toMutableList().apply { removeAt(index) }
                                            }
                                        ) {
                                            Text("🗑️", fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Változó szerkesztő szekció
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Változók értékei (Variable List):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(onClick = { showAddVarDialog = true }) {
                            Text("+ Új változó", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (variableInputs.isEmpty()) {
                        Text(
                            text = "Nincsenek változók. Írj be betűket a kifejezésbe (pl. a, b, x), és automatikusan megjelennek!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        variableInputs.forEach { (varName, varValStr) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00796B))
                                ) {
                                    Text(
                                        text = varName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Text(
                                    text = "$varName =",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                OutlinedTextField(
                                    value = varValStr,
                                    onValueChange = { newVal ->
                                        variableInputs = variableInputs + (varName to newVal)
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    placeholder = { Text("0") }
                                )

                                IconButton(
                                    onClick = {
                                        variableInputs = variableInputs - varName
                                    }
                                ) {
                                    Text("🗑️", fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { buildTree() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Bináris Fa Építése és Újraszámítás",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Hibaüzenet megjelenítése
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "⚠️ $error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Eredmény és Fa Nézet
            parsedTree?.let { root ->
                val resultValue = root.getValue()
                val formattedResult = if (resultValue % 1.0 == 0.0) {
                    resultValue.toLong().toString()
                } else {
                    String.format(java.util.Locale.US, "%.4f", resultValue)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Művelet Eredménye (root.getValue())",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formattedResult,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gyökér művelet: ${root.description}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Nézet váltó fülek (Fa Kártyák vs 2D Grafikus Ábra)
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Hierarchikus Fa", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Grafikus Ábra", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (selectedTab == 0) {
                    Text(
                        text = "Generált bináris fa struktúra:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HierarchicalTreeView(node = root)
                } else {
                    Text(
                        text = "2D Grafikus Fa Ábra (görgethető):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    GraphicalTreeView(rootNode = root)
                }
            }
        }
    }

    // Új / Módosítandó függvény párbeszédablak
    if (showFuncDialog) {
        AlertDialog(
            onDismissRequest = { showFuncDialog = false },
            title = {
                Text(if (editingFuncIndex == null) "Új függvény / kifejezés" else "Függvény szerkesztése")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = funcNameInput,
                        onValueChange = { funcNameInput = it.trim() },
                        label = { Text("Függvény neve (pl. függvény1, sqr)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = funcParamsInput,
                        onValueChange = { funcParamsInput = it },
                        label = { Text("Paraméterek (pl. x vagy x, y)") },
                        placeholder = { Text("pl.: x") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = funcBodyInput,
                        onValueChange = { funcBodyInput = it },
                        label = { Text("Kifejezés törzse (pl. 2 * x + 3)") },
                        placeholder = { Text("pl.: 2 * x") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (funcNameInput.isNotEmpty() && funcBodyInput.isNotEmpty()) {
                            val parsedParams = funcParamsInput
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            val newFunc = UserFunction(
                                name = funcNameInput,
                                parameters = parsedParams,
                                expressionBody = funcBodyInput
                            )

                            val updatedList = userFunctionsList.toMutableList()
                            if (editingFuncIndex != null) {
                                updatedList[editingFuncIndex!!] = newFunc
                            } else {
                                updatedList.add(newFunc)
                            }
                            userFunctionsList = updatedList
                            showFuncDialog = false
                            buildTree()
                        }
                    }
                ) {
                    Text("Mentés")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFuncDialog = false }) {
                    Text("Mégse")
                }
            }
        )
    }

    // Új változó hozzáadása párbeszédablak
    if (showAddVarDialog) {
        AlertDialog(
            onDismissRequest = { showAddVarDialog = false },
            title = { Text("Új változó hozzáadása") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newVarName,
                        onValueChange = { newVarName = it.trim() },
                        label = { Text("Változó neve (pl. a, b, x)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newVarValue,
                        onValueChange = { newVarValue = it.trim() },
                        label = { Text("Kezdő értéke (pl. 5 vagy 2.5)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVarName.isNotEmpty()) {
                            variableInputs = variableInputs + (newVarName to if (newVarValue.isEmpty()) "0" else newVarValue)
                            newVarName = ""
                            newVarValue = ""
                            showAddVarDialog = false
                            buildTree()
                        }
                    }
                ) {
                    Text("Hozzáadás")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVarDialog = false }) {
                    Text("Mégse")
                }
            }
        )
    }
}
