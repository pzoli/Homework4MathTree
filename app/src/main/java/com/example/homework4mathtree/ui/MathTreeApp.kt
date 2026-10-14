package com.example.homework4mathtree.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homework4mathtree.model.Rule
import com.example.homework4mathtree.model.UserFunctionData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathTreeApp(
    viewModel: MathTreeViewModel = viewModel()
) {
    val simulation = viewModel.simulation
    val activeWorkspace = viewModel.activeWorkspace
    val variableInputs = viewModel.variableInputs
    val userFunctionsList = viewModel.userFunctionsList
    val rulesList = viewModel.rulesList
    val ruleResults = viewModel.ruleResults
    val selectedLabelFilter = viewModel.selectedLabelFilter
    val availableLabels = viewModel.availableLabels

    val selectedRuleForTreeDialog = viewModel.selectedRuleForTreeDialog
    val selectedRuleConditionTree = viewModel.selectedRuleConditionTree
    val errorMessage = viewModel.errorMessage

    // Munkaterület hozzáadása dialog
    var showAddWsDialog by rememberSaveable { mutableStateOf(false) }
    var newWsNameInput by rememberSaveable { mutableStateOf("") }

    // JSON Export / Import dialogok
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var jsonImportInput by rememberSaveable { mutableStateOf("") }

    // Változó hozzáadása dialog állapotok
    var showAddVarDialog by rememberSaveable { mutableStateOf(false) }
    var newVarName by rememberSaveable { mutableStateOf("") }
    var newVarValue by rememberSaveable { mutableStateOf("") }

    // Függvény hozzáadása / szerkesztése dialog állapotok
    var showFuncDialog by rememberSaveable { mutableStateOf(false) }
    var editingFuncIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var funcNameInput by rememberSaveable { mutableStateOf("") }
    var funcParamsInput by rememberSaveable { mutableStateOf("") }
    var funcBodyInput by rememberSaveable { mutableStateOf("") }

    // Szabály (Rule) hozzáadása / szerkesztése dialog állapotok
    var showRuleDialog by rememberSaveable { mutableStateOf(false) }
    var editingRuleIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var ruleNameInput by rememberSaveable { mutableStateOf("") }
    var ruleLabelInput by rememberSaveable { mutableStateOf("") }
    var ruleCondInput by rememberSaveable { mutableStateOf("") }
    var ruleTargetVarInput by rememberSaveable { mutableStateOf("") }
    var ruleValueExprInput by rememberSaveable { mutableStateOf("") }

    var ruleDialogTab by remember { mutableIntStateOf(0) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Matematikai Bináris Fa & Rule Építő",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = { showExportDialog = true }) {
                        Text("📤 JSON", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showImportDialog = true }) {
                        Text("📥 JSON", fontWeight = FontWeight.Bold)
                    }
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
            // Munkaterületek (Workspaces) Kezelő Szekció
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Munkaterület (Workspace): ${activeWorkspace.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row {
                            TextButton(onClick = { showAddWsDialog = true }) {
                                Text("+ Új Munkaterület", fontWeight = FontWeight.Bold)
                            }
                            if (simulation.workspaces.size > 1) {
                                IconButton(onClick = { viewModel.deleteActiveWorkspace() }) {
                                    Text("🗑️", fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    // Munkaterület választó gombok (Chips)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        simulation.workspaces.keys.forEach { wsName ->
                            FilterChip(
                                selected = wsName == activeWorkspace.name,
                                onClick = { viewModel.switchWorkspace(wsName) },
                                label = { Text(wsName, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            // Szabályok (Rules) Szekció: if...then...conclude that
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
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
                            text = "Szabályok (Rules - if...then...):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.evaluateRules() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("▶️ Szabályok Futtatása", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Címke (Label) szűrő és fókuszálás gombok
                    Text(
                        text = "Fókuszálás Címke (Tag) alapján:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedLabelFilter == null || selectedLabelFilter == "Összes",
                            onClick = {
                                viewModel.selectLabelFilter("Összes")
                                viewModel.evaluateRules("Összes")
                            },
                            label = { Text("Összes Címke", fontWeight = FontWeight.Bold) }
                        )
                        availableLabels.forEach { labelName ->
                            FilterChip(
                                selected = selectedLabelFilter == labelName,
                                onClick = {
                                    viewModel.selectLabelFilter(labelName)
                                    viewModel.evaluateRules(labelName)
                                },
                                label = { Text("🏷️ $labelName") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                editingRuleIndex = null
                                ruleNameInput = "Szabály ${rulesList.size + 1}"
                                ruleLabelInput = "Alapértelmezett"
                                ruleCondInput = "v1 == \"teszt\""
                                ruleTargetVarInput = "x1"
                                ruleValueExprInput = "10"
                                showRuleDialog = true
                            }
                        ) {
                            Text("+ Új Szabály", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (rulesList.isEmpty()) {
                        Text(
                            text = "Nincsenek szabályok ezen a munkaterületen. Hozz létre újat a '+ Új Szabály' gombbal!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        rulesList.forEachIndexed { index, rule ->
                            val res = ruleResults[rule.id]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = rule.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("🏷️ ${rule.label}", fontSize = 11.sp) }
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingRuleIndex = index
                                                    ruleNameInput = rule.name
                                                    ruleLabelInput = rule.label
                                                    ruleCondInput = rule.conditionExpression
                                                    ruleTargetVarInput = rule.targetVariable
                                                    ruleValueExprInput = rule.valueExpression
                                                    showRuleDialog = true
                                                }
                                            ) {
                                                Text("✏️", fontSize = 16.sp)
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.removeRule(index)
                                                }
                                            ) {
                                                Text("🗑️", fontSize = 16.sp)
                                            }
                                        }
                                    }

                                    // Szabály Formula megjelenítése: if...then...conclude that
                                    Text(
                                        text = "if (${rule.conditionExpression})\nthen conclude that ${rule.targetVariable} = ${rule.valueExpression}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Kiértékelési állapot
                                    res?.let { r ->
                                        if (r.conditionMet) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE8F5E9)
                                            ) {
                                                Text(
                                                    text = "✅ Feltétel: IGAZ -> Beállítva: ${rule.targetVariable} = ${r.assignedValue?.toDisplayString()}",
                                                    color = Color(0xFF2E7D32),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFFEBEE)
                                            ) {
                                                Text(
                                                    text = if (r.errorMessage != null) "⚠️ Hiba: ${r.errorMessage}" else "❌ Feltétel: HAMIS",
                                                    color = Color(0xFFC62828),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Gomb a feltétel bináris fa megtekintéséhez popup ablakban
                                    OutlinedButton(
                                        onClick = { viewModel.openRuleConditionTreeDialog(rule) },
                                        modifier = Modifier.align(Alignment.End),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("🔍 Feltétel Fájának Megtekintése", fontSize = 12.sp)
                                    }
                                }
                            }
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
                            text = "Függvények (${activeWorkspace.name}):",
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
                            text = "Nincsenek egyéni függvények ezen a munkaterületen.",
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
                            text = "Változók (${activeWorkspace.name}):",
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
                            text = "Nincsenek változók ezen a munkaterületen.",
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
        }
    }

    // POPUP DIALOG: Szabály Feltétel Fájának (Hierarchikus és 2D Fa) Megtekintése
    val dialogRule = selectedRuleForTreeDialog
    val dialogCondTree = selectedRuleConditionTree
    if (dialogRule != null && dialogCondTree != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeRuleConditionTreeDialog() },
            title = {
                Column {
                    Text(text = "Szabály Feltétel Fája: ${dialogRule.name}", fontWeight = FontWeight.Bold)
                    Text(
                        text = "if (${dialogRule.conditionExpression})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Feltétel értéke: ${dialogCondTree.getValue().toDisplayString()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    TabRow(selectedTabIndex = ruleDialogTab) {
                        Tab(
                            selected = ruleDialogTab == 0,
                            onClick = { ruleDialogTab = 0 },
                            text = { Text("Hierarchikus Fa", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = ruleDialogTab == 1,
                            onClick = { ruleDialogTab = 1 },
                            text = { Text("Grafikus Ábra", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        if (ruleDialogTab == 0) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                HierarchicalTreeView(node = dialogCondTree)
                            }
                        } else {
                            GraphicalTreeView(rootNode = dialogCondTree)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.closeRuleConditionTreeDialog() }) {
                    Text("Bezárás")
                }
            }
        )
    }

    // Új / Szerkesztendő Szabály párbeszédablak
    if (showRuleDialog) {
        AlertDialog(
            onDismissRequest = { showRuleDialog = false },
            title = {
                Text(if (editingRuleIndex == null) "Új Szabály (if...then...)" else "Szabály szerkesztése")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ruleNameInput,
                        onValueChange = { ruleNameInput = it },
                        label = { Text("Szabály neve (pl. Szabály 1)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ruleLabelInput,
                        onValueChange = { ruleLabelInput = it },
                        label = { Text("Címke / Tag (pl. Klíma, Fizika)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ruleCondInput,
                        onValueChange = { ruleCondInput = it },
                        label = { Text("IF Feltétel (pl. temp > 25 vagy v1 == \"teszt\")") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ruleTargetVarInput,
                        onValueChange = { ruleTargetVarInput = it.trim() },
                        label = { Text("THEN Cél Változó neve (pl. x1 vagy status)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ruleValueExprInput,
                        onValueChange = { ruleValueExprInput = it },
                        label = { Text("CONCLUDE VALUE Érték kifejezése (pl. 10 vagy \"Hűtés\")") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ruleNameInput.isNotEmpty() && ruleCondInput.isNotEmpty() && ruleTargetVarInput.isNotEmpty()) {
                            val newRule = Rule(
                                name = ruleNameInput,
                                label = ruleLabelInput.ifEmpty { "Alapértelmezett" },
                                conditionExpression = ruleCondInput,
                                targetVariable = ruleTargetVarInput,
                                valueExpression = ruleValueExprInput
                            )
                            viewModel.addOrUpdateRule(newRule, editingRuleIndex)
                            showRuleDialog = false
                        }
                    }
                ) {
                    Text("Mentés")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRuleDialog = false }) {
                    Text("Mégse")
                }
            }
        )
    }

    // Új Munkaterület hozzáadása párbeszédablak
    if (showAddWsDialog) {
        AlertDialog(
            onDismissRequest = { showAddWsDialog = false },
            title = { Text("Új Munkaterület (Workspace)") },
            text = {
                OutlinedTextField(
                    value = newWsNameInput,
                    onValueChange = { newWsNameInput = it.trim() },
                    label = { Text("Munkaterület neve (pl. Fizika, Geometria)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newWsNameInput.isNotEmpty()) {
                            viewModel.createWorkspace(newWsNameInput)
                            newWsNameInput = ""
                            showAddWsDialog = false
                        }
                    }
                ) {
                    Text("Létrehozás")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWsDialog = false }) {
                    Text("Mégse")
                }
            }
        )
    }

    // JSON Export párbeszédablak
    if (showExportDialog) {
        val exportedJson = remember { viewModel.exportSimulationToJson() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Szimuláció Exportálása (JSON)") },
            text = {
                OutlinedTextField(
                    value = exportedJson,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) {
                    Text("Bezárás")
                }
            }
        )
    }

    // JSON Import párbeszédablak
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Szimuláció Importálása (JSON)") },
            text = {
                OutlinedTextField(
                    value = jsonImportInput,
                    onValueChange = { jsonImportInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("Illeszd be a JSON kódját ide...") },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonImportInput.isNotEmpty()) {
                            viewModel.importSimulationFromJson(jsonImportInput)
                            jsonImportInput = ""
                            showImportDialog = false
                        }
                    }
                ) {
                    Text("Importálás")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Mégse")
                }
            }
        )
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
                        label = { Text("Függvény neve (pl. f1, sqr)") },
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

                            val newFunc = UserFunctionData(
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
