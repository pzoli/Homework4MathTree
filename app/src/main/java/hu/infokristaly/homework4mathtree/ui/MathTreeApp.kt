package hu.infokristaly.homework4mathtree.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.infokristaly.homework4mathtree.model.RuleParser
import hu.infokristaly.homework4mathtree.model.Timer
import hu.infokristaly.homework4mathtree.model.UserFunctionData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathTreeApp(
    viewModel: MathTreeViewModel = viewModel()
) {
    val context = LocalContext.current

    val simulation = viewModel.simulation
    val activeWorkspace = viewModel.activeWorkspace
    val variableInputs = viewModel.variableInputs
    val userFunctionsList = viewModel.userFunctionsList
    val rulesList = viewModel.rulesList
    val timersList = viewModel.timersList
    val ruleResults = viewModel.ruleResults
    val availableLabels = rulesList.map { it.label }.distinct()

    val isQueueProcessing = viewModel.isQueueProcessing
    val processQueueSize = viewModel.processQueueSize

    val selectedRuleForTreeDialog = viewModel.selectedRuleForTreeDialog
    val selectedRuleConditionTree = viewModel.selectedRuleConditionTree

    val selectedFunctionForTreeDialog = viewModel.selectedFunctionForTreeDialog
    val selectedFunctionTree = viewModel.selectedFunctionTree

    val errorMessage = viewModel.errorMessage

    // Rendszer Fájl Választó: Mentés Fájlba (Export)
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(viewModel.exportSimulationToJson().toByteArray())
                }
            } catch (e: Exception) {
                viewModel.errorMessage = "Hiba a fájl mentése során: ${e.message}"
            }
        }
    }

    // Rendszer Fájl Választó: Megnyitás Fájlból (Import)
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val jsonText = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (!jsonText.isNullOrEmpty()) {
                    viewModel.importSimulationFromJson(jsonText)
                }
            } catch (e: Exception) {
                viewModel.errorMessage = "Hiba a fájl megnyitása során: ${e.message}"
            }
        }
    }

    // Munkaterület hozzáadása dialog
    var showAddWsDialog by rememberSaveable { mutableStateOf(false) }
    var newWsNameInput by rememberSaveable { mutableStateOf("") }

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
    var ruleTextInput by rememberSaveable { mutableStateOf("") }
    var ruleChainingInput by rememberSaveable { mutableStateOf(true) }
    var ruleDialogError by rememberSaveable { mutableStateOf<String?>(null) }

    // Időzítő (Timer) hozzáadása / szerkesztése dialog állapotok
    var showTimerDialog by rememberSaveable { mutableStateOf(false) }
    var editingTimerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var timerNameInput by rememberSaveable { mutableStateOf("") }
    var timerTargetLabelInput by rememberSaveable { mutableStateOf("") }
    var timerIntervalSecInput by rememberSaveable { mutableStateOf("2") }

    var ruleDialogTab by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Matematikai Bináris Fa & Rule Engine",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = { exportFileLauncher.launch("mathtree_simulation.json") }) {
                        Text("💾 Mentés Fájlba", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { importFileLauncher.launch(arrayOf("application/json", "*/*")) }) {
                        Text("📂 Megnyitás", fontWeight = FontWeight.Bold)
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
                .imePadding()
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

            // Szabályok (Rules) Szekció: Process Queue és Initialization
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
                            text = "Szabályok (Rules Process Queue):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Initialization Gomb: Az "init" szabályokat a process queue-ba teszi
                        Button(
                            onClick = { viewModel.triggerInitialization() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("🚀 Initialization", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Process Queue Állapotjelző (Állandóan látható az elrendezés ugrálásának elkerülésére)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isQueueProcessing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = "⚙️ Process Queue futtatása ($processQueueSize szabály a sorban)...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { 0f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "✅ Process Queue tétlen (Készenlétben)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                ruleTextInput = "if x1 <= 10 then conclude that x1 = x1 + 2"
                                ruleChainingInput = true
                                ruleDialogError = null
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
                                                    ruleTextInput = rule.toRuleText()
                                                    ruleChainingInput = rule.mayCauseChaining
                                                    ruleDialogError = null
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

                                    // Szabály egyetlen összefüggő szövegként: if...then...conclude that vagy if...then...focus on
                                    Text(
                                        text = rule.toRuleText(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (rule.referencedVariables.isNotEmpty()) {
                                        Text(
                                            text = "Hivatkozott változók: [ ${rule.referencedVariables.joinToString(", ")} ]",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Kiértékelési állapot
                                    res?.let { r ->
                                        if (r.conditionMet) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE8F5E9)
                                            ) {
                                                val statusText = when {
                                                    rule.isTimerActionRule -> "✅ Feltétel: IGAZ -> Időzítő ${if (rule.timerAction.equals("start", ignoreCase = true)) "elindítva" else "leállítva"}: ${rule.timerName}"
                                                    rule.isUnconditional -> "✅ Feltétel nélküli értékadás -> ${rule.targetVariable} = ${r.assignedValue?.toDisplayString()}"
                                                    rule.isFocusRule -> "✅ Feltétel: IGAZ -> Meghívott címke: ${rule.focusLabel}"
                                                    else -> "✅ Feltétel: IGAZ -> Beállítva: ${rule.targetVariable} = ${r.assignedValue?.toDisplayString()}"
                                                }
                                                Text(
                                                    text = statusText,
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

            // Időzítők (Timers) Szekció: Időszakos Szabály-fókuszálás
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
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
                            text = "Időzítők (Timers - Időszakos Fókuszálás):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(
                            onClick = {
                                editingTimerIndex = null
                                timerNameInput = "Időzítő ${timersList.size + 1}"
                                timerTargetLabelInput = "Klíma"
                                timerIntervalSecInput = "2"
                                showTimerDialog = true
                            }
                        ) {
                            Text("+ Új Időzítő", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (timersList.isEmpty()) {
                        Text(
                            text = "Nincsenek időzítők ezen a munkaterületen. Hozz létre újat a '+ Új Időzítő' gombbal!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        timersList.forEachIndexed { index, timer ->
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
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "⏱️ ${timer.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("invoke ${timer.targetLabel} rules", fontSize = 11.sp) }
                                            )
                                        }
                                        Text(
                                            text = "Időköz: ${timer.intervalMillis / 1000.0} mp (${timer.intervalMillis} ms)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Switch(
                                            checked = timer.isRunning,
                                            onCheckedChange = { viewModel.toggleTimerRunning(timer.id, it) }
                                        )
                                        IconButton(
                                            onClick = {
                                                editingTimerIndex = index
                                                timerNameInput = timer.name
                                                timerTargetLabelInput = timer.targetLabel
                                                timerIntervalSecInput = (timer.intervalMillis / 1000.0).toString()
                                                showTimerDialog = true
                                            }
                                        ) {
                                            Text("✏️", fontSize = 16.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.removeTimer(index)
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
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
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

                                    OutlinedButton(
                                        onClick = { viewModel.openFunctionTreeDialog(func) },
                                        modifier = Modifier.align(Alignment.End),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("🔍 Kifejezés Fájának Megtekintése", fontSize = 12.sp)
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

                                VariableValueInputField(
                                    varName = varName,
                                    initialValue = varValStr,
                                    onValueCommit = { newVal ->
                                        viewModel.updateVariableInput(varName, newVal)
                                    },
                                    modifier = Modifier.weight(1f)
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

    // POPUP DIALOG: Felhasználói Függvény Kifejezés Fájának Megtekintése
    val dialogFunc = selectedFunctionForTreeDialog
    val dialogFuncTree = selectedFunctionTree
    if (dialogFunc != null && dialogFuncTree != null) {
        var funcDialogTab by remember { mutableIntStateOf(0) }
        val paramsStr = dialogFunc.parameters.joinToString(", ")
        val sig = if (paramsStr.isEmpty()) dialogFunc.name else "${dialogFunc.name}($paramsStr)"

        AlertDialog(
            onDismissRequest = { viewModel.closeFunctionTreeDialog() },
            title = {
                Column {
                    Text(text = "Függvény Kifejezés Fája: $sig", fontWeight = FontWeight.Bold)
                    Text(
                        text = "$sig = ${dialogFunc.expressionBody}",
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
                        text = "Kifejezés értéke: ${dialogFuncTree.getValue().toDisplayString()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    TabRow(selectedTabIndex = funcDialogTab) {
                        Tab(
                            selected = funcDialogTab == 0,
                            onClick = { funcDialogTab = 0 },
                            text = { Text("Hierarchikus Fa", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = funcDialogTab == 1,
                            onClick = { funcDialogTab = 1 },
                            text = { Text("Grafikus Ábra", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        if (funcDialogTab == 0) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                HierarchicalTreeView(node = dialogFuncTree)
                            }
                        } else {
                            GraphicalTreeView(rootNode = dialogFuncTree)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.closeFunctionTreeDialog() }) {
                    Text("Bezárás")
                }
            }
        )
    }

    // Új / Szerkesztendő Időzítő (Timer) párbeszédablak
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = {
                Text(if (editingTimerIndex == null) "Új Időzítő (Timer)" else "Időzítő szerkesztése")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = timerNameInput,
                        onValueChange = { timerNameInput = it },
                        label = { Text("Időzítő neve (pl. Klíma Időzítő)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = timerTargetLabelInput,
                        onValueChange = { timerTargetLabelInput = it },
                        label = { Text("Cél szabály címke (pl. Klíma)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = timerIntervalSecInput,
                        onValueChange = { timerIntervalSecInput = it },
                        label = { Text("Időköz másodpercben (pl. 2 vagy 5)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sec = timerIntervalSecInput.toDoubleOrNull() ?: 2.0
                        val millis = (sec * 1000).toLong().coerceAtLeast(500L)
                        val newTimer = Timer(
                            id = if (editingTimerIndex != null && editingTimerIndex!! in timersList.indices) {
                                timersList[editingTimerIndex!!].id
                            } else {
                                java.util.UUID.randomUUID().toString()
                            },
                            name = timerNameInput.ifEmpty { "Időzítő" },
                            targetLabel = timerTargetLabelInput.ifEmpty { "Klíma" },
                            intervalMillis = millis,
                            isRunning = if (editingTimerIndex != null && editingTimerIndex!! in timersList.indices) {
                                timersList[editingTimerIndex!!].isRunning
                            } else {
                                false
                            }
                        )
                        viewModel.addOrUpdateTimer(newTimer, editingTimerIndex)
                        showTimerDialog = false
                    }
                ) {
                    Text("Mentés")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text("Mégse")
                }
            }
        )
    }

    // Új / Szerkesztendő Szabály párbeszédablak
    if (showRuleDialog) {
        AlertDialog(
            onDismissRequest = { showRuleDialog = false },
            title = {
                Text(if (editingRuleIndex == null) "Új Szabály (Rule Editor)" else "Szabály szerkesztése")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = ruleNameInput,
                        onValueChange = { ruleNameInput = it },
                        label = { Text("Szabály neve (pl. Szabály 1)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ruleLabelInput,
                        onValueChange = { ruleLabelInput = it },
                        label = { Text("Címke / Tag (pl. init, Klíma, Teszt)") },
                        singleLine = true
                    )

                    // Láncolás (Chaining) engedélyezése
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = ruleChainingInput,
                            onCheckedChange = { ruleChainingInput = it }
                        )
                        Text(
                            text = "A változó módosulása indítson láncolást (Chaining)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Minta beszúrás chip-ek
                    Text(text = "Minta formátumok:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = { ruleTextInput = "if isHot == true then invoke Klíma rules" },
                            label = { Text("invoke Klíma rules minta", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { ruleTextInput = "if temp > 25 then start timer Klíma Időzítő" },
                            label = { Text("start timer minta", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { ruleTextInput = "if temp <= 20 then stop timer Klíma Időzítő" },
                            label = { Text("stop timer minta", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { ruleTextInput = "if x1 <= 10 then conclude that x1 = x1 + 2" },
                            label = { Text("x1 = x1 + 2 (Chaining)", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { ruleTextInput = "unconditionally conclude that x1 = 10 + y" },
                            label = { Text("unconditionally minta", fontSize = 11.sp) }
                        )
                    }

                    // EGYETLEN SZÖVEGBEVITELI MEZŐ A SZABÁLYNAK
                    OutlinedTextField(
                        value = ruleTextInput,
                        onValueChange = {
                            ruleTextInput = it
                            ruleDialogError = null
                        },
                        label = { Text("Szabály Szövege (if...then...)") },
                        placeholder = { Text("pl.: if x1 <= 10 then conclude that x1 = x1 + 2") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    )

                    // Prediktív Szövegbevitel (Predictive Autocomplete Suggestions)
                    val currentWorkspaceVars = variableInputs.keys.toList()
                    val currentWorkspaceFuncs = userFunctionsList.map { it.name }
                    val currentWorkspaceTimers = timersList.map { it.name }

                    val allKeywordsAndTemplates = remember {
                        listOf(
                            "if ",
                            "then conclude that ",
                            "then invoke ",
                            "then start timer ",
                            "then stop timer ",
                            "unconditionally conclude that ",
                            "unconditionally invoke ",
                            "unconditionally start timer ",
                            "unconditionally stop timer ",
                            "and ",
                            "or ",
                            "not "
                        )
                    }

                    val lastWord = ruleTextInput.split(" ", "\n", "(").lastOrNull()?.trim() ?: ""

                    val predictiveSuggestions: List<String> = remember(ruleTextInput, currentWorkspaceVars, currentWorkspaceFuncs, currentWorkspaceTimers, availableLabels) {
                        if (lastWord.isEmpty()) {
                            emptyList()
                        } else {
                            val candidates = (
                                allKeywordsAndTemplates +
                                currentWorkspaceVars +
                                currentWorkspaceFuncs +
                                currentWorkspaceTimers.map { "\"$it\"" } +
                                availableLabels
                            ).distinct()

                            candidates.filter { item: String ->
                                item.contains(lastWord, ignoreCase = true) && !item.equals(lastWord, ignoreCase = true)
                            }.take(8)
                        }
                    }

                    if (predictiveSuggestions.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Prediktív szövegbeviteli javaslatok:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                predictiveSuggestions.forEach { suggestion: String ->
                                    SuggestionChip(
                                        onClick = {
                                            val lastSpaceIdx = maxOf(
                                                ruleTextInput.lastIndexOf(' '),
                                                ruleTextInput.lastIndexOf('\n'),
                                                ruleTextInput.lastIndexOf('(')
                                            )
                                            ruleTextInput = if (lastSpaceIdx == -1) {
                                                suggestion
                                            } else {
                                                ruleTextInput.substring(0, lastSpaceIdx + 1) + suggestion
                                            }
                                            ruleDialogError = null
                                        },
                                        label = {
                                            Text(
                                                text = "✨ $suggestion",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    ruleDialogError?.let { err ->
                        Text(text = "⚠️ $err", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val parsedRule = RuleParser.parseRuleText(
                                name = ruleNameInput.ifEmpty { "Szabály" },
                                text = ruleTextInput,
                                defaultLabel = ruleLabelInput.ifEmpty { "Alapértelmezett" },
                                mayCauseChaining = ruleChainingInput,
                                id = if (editingRuleIndex != null && editingRuleIndex!! in rulesList.indices) {
                                    rulesList[editingRuleIndex!!].id
                                } else {
                                    java.util.UUID.randomUUID().toString()
                                }
                            )
                            viewModel.addOrUpdateRule(parsedRule, editingRuleIndex)
                            showRuleDialog = false
                        } catch (e: Exception) {
                            ruleDialogError = e.message ?: "Érvénytelen szabály formátum!"
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

    // Új / Módosítandó függvény párbeszédablak
    if (showFuncDialog) {
        AlertDialog(
            onDismissRequest = { showFuncDialog = false },
            title = {
                Text(if (editingFuncIndex == null) "Új függvény / kifejezés" else "Függvény szerkesztése")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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

/**
 * Változó értékét szerkesztő mező, amely KIZÁRÓLAG a fókusz elvesztésekor (vagy az Enter/Done gombra)
 * lépteti életbe a módosítást és indítja el a szabályok kiértékelését.
 */
@Composable
private fun VariableValueInputField(
    varName: String,
    initialValue: String,
    onValueCommit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textState by remember(varName, initialValue) { mutableStateOf(initialValue) }

    OutlinedTextField(
        value = textState,
        onValueChange = { textState = it },
        modifier = modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused && textState != initialValue) {
                onValueCommit(textState)
            }
        },
        singleLine = true,
        placeholder = { Text("Érték (szám vagy szöveg)") },
        keyboardActions = KeyboardActions(
            onDone = {
                if (textState != initialValue) {
                    onValueCommit(textState)
                }
            }
        )
    )
}
