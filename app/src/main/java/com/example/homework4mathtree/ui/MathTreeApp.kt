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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathTreeApp() {
    var expressionText by remember { mutableStateOf("x + 5 * y") }
    var variableInputs by remember { mutableStateOf(mapOf("x" to "10", "y" to "2")) }

    var parsedTree by remember { mutableStateOf<MathNode?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Kártya Nézet, 1: Grafikus Ábra

    var showAddVarDialog by remember { mutableStateOf(false) }
    var newVarName by remember { mutableStateOf("") }
    var newVarValue by remember { mutableStateOf("") }

    val parser = remember { MathExpressionParser() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // Automatikus változó felismerés kifejezés változásakor
    LaunchedEffect(expressionText) {
        val detectedVars = parser.extractVariables(expressionText)
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
            val root = parser.parse(expressionText, doubleVariables)
            parsedTree = root
        } catch (e: Exception) {
            parsedTree = null
            errorMessage = e.message ?: "Hiba történt a kifejezés feldolgozása során!"
        }
    }

    // Automatikus elemzés kifejezés vagy változók módosításakor
    LaunchedEffect(expressionText, variableInputs) {
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
                    Text(
                        text = "Matematikai kifejezés megadása:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = expressionText,
                        onValueChange = { expressionText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Pl.: x + 5 * y vagy a ^ 2 + b ^ 2") },
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

                    // Gyors szimbólum és változó beszúró gombok
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val symbols = listOf("+", "-", "*", "/", "^", "√", "(", ")", "x", "y", "a", "b")
                        symbols.forEach { symbol ->
                            SuggestionChip(
                                onClick = { expressionText += " $symbol " },
                                label = {
                                    Text(
                                        text = symbol,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
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
                            "x + 5 * y",
                            "a ^ 2 + b ^ 2",
                            "(10 - x) / (1 + y)",
                            "sqrt(x) + y",
                            "3 + 5 * 2"
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
                            text = "Nincsenek változók. Írj be betűket a kifejezésbe (pl. x, y), és automatikusan megjelennek!",
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
                        label = { Text("Változó neve (pl. x, y, a)") },
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
