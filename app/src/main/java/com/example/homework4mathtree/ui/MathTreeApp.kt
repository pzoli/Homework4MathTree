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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homework4mathtree.parser.UserFunction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathTreeApp(
    viewModel: MathTreeViewModel = viewModel()
) {
    val expressionText = viewModel.expressionText
    val variableInputs = viewModel.variableInputs
    val isDegreeMode = viewModel.isDegreeMode
    val userFunctionsList = viewModel.userFunctionsList
    val selectedTab = viewModel.selectedTab
    val parsedTree = viewModel.parsedTree
    val errorMessage = viewModel.errorMessage

    // Változó hozzáadása dialog állapotok (rememberSaveable a forgatás túléléséhez)
    var showAddVarDialog by rememberSaveable { mutableStateOf(false) }
    var newVarName by rememberSaveable { mutableStateOf("") }
    var newVarValue by rememberSaveable { mutableStateOf("") }

    // Függvény hozzáadása / szerkesztése dialog állapotok
    var showFuncDialog by rememberSaveable { mutableStateOf(false) }
    var editingFuncIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var funcNameInput by rememberSaveable { mutableStateOf("") }
    var funcParamsInput by rememberSaveable { mutableStateOf("") }
    var funcBodyInput by rememberSaveable { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    fun buildTree() {
        keyboardController?.hide()
        viewModel.buildTree()
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
                            text = "Matematikai / Logikai kifejezés:",
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
                                onClick = { viewModel.updateDegreeMode(true) },
                                label = { Text("Fok (°)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                            FilterChip(
                                selected = !isDegreeMode,
                                onClick = { viewModel.updateDegreeMode(false) },
                                label = { Text("Rad", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = expressionText,
                        onValueChange = { viewModel.updateExpressionText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Pl.: v1 == \"teszt\" vagy x > 5 and y <= 10") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { buildTree() }),
                        trailingIcon = {
                            if (expressionText.isNotEmpty()) {
                                TextButton(onClick = { viewModel.updateExpressionText("") }) {
                                    Text("✕")
                                }
                            }
                        }
                    )

                    // Gyors szimbólum, logikai és változó/függvény beszúró gombok
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val symbols = listOf(
                            "==", "!=", "<", ">", "<=", ">=", "and", "or", "not",
                            "\"teszt\"", "+", "-", "*", "/", "^", "√", "(", ")", ",",
                            "sin", "cos", "tan", "cot", "v1", "a", "b", "x"
                        ) + userFunctionsList.map { it.name }

                        symbols.distinct().forEach { symbol ->
                            SuggestionChip(
                                onClick = { viewModel.updateExpressionText("$expressionText $symbol ") },
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
                            "v1 == \"teszt\"",
                            "x > 5 and y <= 10",
                            "not (a == \"teszt\")",
                            "sin(90) == 1",
                            "függvény1(10 * a + 2 * függvény2(b))"
                        )
                        presets.forEach { preset ->
                            FilterChip(
                                selected = expressionText == preset,
                                onClick = { viewModel.updateExpressionText(preset) },
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
                                                viewModel.removeUserFunction(index)
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

            // Változó szerkesztő szekció (Számok és Szövegek)
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
                            text = "Változók értékei (Szám / Szöveg):",
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
                            text = "Nincsenek változók. Írj be azonosítókat a kifejezésbe (pl. v1, a, x), és automatikusan megjelennek!",
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
                                        viewModel.updateVariableInput(varName, newVal)
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    placeholder = { Text("Érték (szám vagy szöveg)") }
                                )

                                IconButton(
                                    onClick = {
                                        viewModel.removeVariable(varName)
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
                val formattedResult = resultValue.toDisplayString()

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
                        onClick = { viewModel.updateSelectedTab(0) },
                        text = { Text("Hierarchikus Fa", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.updateSelectedTab(1) },
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

                            viewModel.addOrUpdateUserFunction(newFunc, editingFuncIndex)
                            showFuncDialog = false
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
                        label = { Text("Változó neve (pl. v1, a, x)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newVarValue,
                        onValueChange = { newVarValue = it },
                        label = { Text("Kezdő értéke (szám vagy \"szöveg\")") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVarName.isNotEmpty()) {
                            viewModel.addVariable(
                                newVarName,
                                newVarValue.ifEmpty { "teszt" }
                            )
                            newVarName = ""
                            newVarValue = ""
                            showAddVarDialog = false
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
