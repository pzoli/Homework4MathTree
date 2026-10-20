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
import hu.infokristaly.homework4mathtree.model.ClassDef
import hu.infokristaly.homework4mathtree.model.ObjectInstance
import hu.infokristaly.homework4mathtree.model.Rule
import hu.infokristaly.homework4mathtree.model.StringNode
import hu.infokristaly.homework4mathtree.model.Timer
import hu.infokristaly.homework4mathtree.model.UserFunctionData
import hu.infokristaly.homework4mathtree.model.createTypedPropertyValue
import hu.infokristaly.homework4mathtree.model.getPropertyValueAsString
import hu.infokristaly.homework4mathtree.model.updateLocalNestedObject
import hu.infokristaly.homework4mathtree.model.updateLocalNestedPropertyValue

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
    val classDefsList = viewModel.classDefsList
    val objectsList = viewModel.objectsList
    val ruleResults = viewModel.ruleResults
    val availableLabels = rulesList.map { it.label }.distinct()

    val (extractedObjectPaths, extractedClassDefs) = remember(objectsList, classDefsList) {
        val objectPaths = mutableListOf<String>()
        fun traverseProps(prefix: String, props: Map<String, Any>) {
            for ((key, value) in props) {
                val fullPath = if (prefix.isEmpty()) key else "$prefix.$key"
                objectPaths.add(fullPath)
                when (value) {
                    is ObjectInstance -> traverseProps(fullPath, value.properties)
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        (value as? Map<String, Any>)?.let { traverseProps(fullPath, it) }
                    }
                }
            }
        }
        for ((objName, obj) in objectsList) {
            traverseProps(objName, obj.properties)
        }

        val classItems = mutableListOf<String>()
        for ((className, cd) in classDefsList) {
            classItems.add(className)
            for (pName in cd.properties.keys) {
                classItems.add("$className.$pName")
            }
        }
        Pair(objectPaths.distinct(), classItems.distinct())
    }

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

    // Szabály Szerkesztő Activity indító (RuleEditorActivity)
    val ruleEditorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val ruleIndex = data.getIntExtra(RuleEditorActivity.EXTRA_RULE_INDEX, -1)
            val editingIndex = if (ruleIndex >= 0) ruleIndex else null

            val rule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                data.getSerializableExtra(RuleEditorActivity.EXTRA_RULE, Rule::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getSerializableExtra(RuleEditorActivity.EXTRA_RULE) as? Rule
            }

            if (rule != null) {
                viewModel.addOrUpdateRule(rule, editingIndex)
            }
        }
    }

    // ClassDef Szerkesztő Activity indító
    val classDefEditorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val editingName = data.getStringExtra(ClassDefEditorActivity.EXTRA_EDITING_CLASSDEF_NAME)
            val classDef = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                data.getSerializableExtra(ClassDefEditorActivity.EXTRA_CLASSDEF, ClassDef::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getSerializableExtra(ClassDefEditorActivity.EXTRA_CLASSDEF) as? ClassDef
            }

            if (classDef != null) {
                if (editingName != null && editingName != classDef.name) {
                    viewModel.removeClassDef(editingName)
                }
                viewModel.addOrUpdateClassDef(classDef)
            }
        }
    }

    // ObjectInstance Szerkesztő Activity indító
    val objectInstanceEditorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val editingName = data.getStringExtra(ObjectInstanceEditorActivity.EXTRA_EDITING_OBJECT_NAME)
            val objectInstance = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                data.getSerializableExtra(ObjectInstanceEditorActivity.EXTRA_OBJECT, ObjectInstance::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getSerializableExtra(ObjectInstanceEditorActivity.EXTRA_OBJECT) as? ObjectInstance
            }

            if (objectInstance != null) {
                if (editingName != null && editingName != objectInstance.name) {
                    viewModel.removeObject(editingName)
                }
                viewModel.addOrUpdateObject(objectInstance)
            }
        }
    }

    // Függvény Szerkesztő Activity indító
    val functionEditorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val funcIndex = data.getIntExtra(FunctionEditorActivity.EXTRA_FUNC_INDEX, -1)
            val editingIndex = if (funcIndex >= 0) funcIndex else null

            val userFunction = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                data.getSerializableExtra(FunctionEditorActivity.EXTRA_USER_FUNCTION, UserFunctionData::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getSerializableExtra(FunctionEditorActivity.EXTRA_USER_FUNCTION) as? UserFunctionData
            }

            if (userFunction != null) {
                viewModel.addOrUpdateUserFunction(userFunction, editingIndex)
            }
        }
    }

    // Munkaterület hozzáadása dialog
    var showAddWsDialog by rememberSaveable { mutableStateOf(false) }
    var newWsNameInput by rememberSaveable { mutableStateOf("") }

    // Változó hozzáadása dialog állapotok
    var showAddVarDialog by rememberSaveable { mutableStateOf(false) }
    var newVarName by rememberSaveable { mutableStateOf("") }
    var newVarType by rememberSaveable { mutableStateOf("StringNode") }
    var newVarValue by rememberSaveable { mutableStateOf("") }
    var varTypeDropdownExpanded by remember { mutableStateOf(false) }

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
                                RuleEditorActivity.launch(
                                    context = context,
                                    launcher = ruleEditorLauncher,
                                    defaultRuleName = "Szabály ${rulesList.size + 1}",
                                    workspaceVars = variableInputs.keys.toList(),
                                    workspaceFuncs = userFunctionsList.map { it.name },
                                    workspaceTimers = timersList.map { it.name },
                                    workspaceObjects = objectsList.keys.toList(),
                                    workspaceObjectPaths = extractedObjectPaths,
                                    classDefs = extractedClassDefs,
                                    availableLabels = availableLabels
                                )
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
                                                    RuleEditorActivity.launch(
                                                        context = context,
                                                        launcher = ruleEditorLauncher,
                                                        index = index,
                                                        rule = rule,
                                                        defaultRuleName = rule.name,
                                                        workspaceVars = variableInputs.keys.toList(),
                                                        workspaceFuncs = userFunctionsList.map { it.name },
                                                        workspaceTimers = timersList.map { it.name },
                                                        workspaceObjects = objectsList.keys.toList(),
                                                        workspaceObjectPaths = extractedObjectPaths,
                                                        classDefs = extractedClassDefs,
                                                        availableLabels = availableLabels
                                                    )
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

                                    // Szabály egyetlen összefüggő szövegként
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
                                                    rule.isCreateObjectRule -> "✅ Feltétel: IGAZ -> Objektum létrehozva (${rule.createObjectClass}) -> ${rule.createObjectTarget}"
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

            // Osztályok (ClassDef) Szekció
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
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
                            text = "Osztály Definíciók (ClassDef - ${activeWorkspace.name}):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(
                            onClick = {
                                ClassDefEditorActivity.launch(
                                    context = context,
                                    launcher = classDefEditorLauncher,
                                    availablePropertyTypes = listOf("NumberNode", "BooleanNode", "StringNode", "VariableNode") + classDefsList.keys
                                )
                            }
                        ) {
                            Text("+ Új Osztály", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (classDefsList.isEmpty()) {
                        Text(
                            text = "Nincsenek osztály definíciók ezen a munkaterületen.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        classDefsList.values.forEach { cd ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        Text(
                                            text = "📦 Osztály: ${cd.name}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    ClassDefEditorActivity.launch(
                                                        context = context,
                                                        launcher = classDefEditorLauncher,
                                                        editingClassName = cd.name,
                                                        classDef = cd,
                                                        availablePropertyTypes = listOf("NumberNode", "BooleanNode", "StringNode", "VariableNode") + classDefsList.keys
                                                    )
                                                }
                                            ) {
                                                Text("✏️", fontSize = 16.sp)
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeClassDef(cd.name) }
                                            ) {
                                                Text("🗑️", fontSize = 16.sp)
                                            }
                                        }
                                    }

                                    if (cd.properties.isEmpty()) {
                                        Text("Nincsenek tulajdonságok megadva.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        cd.properties.forEach { (propName, propType) ->
                                            Text(
                                                text = "  • $propName : $propType",
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Objektumok (ObjectInstance) Szekció
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
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
                            text = "Objektumok (Objects - ${activeWorkspace.name}):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        TextButton(
                            onClick = {
                                val defaultClass = classDefsList.keys.firstOrNull() ?: "Alma"
                                ObjectInstanceEditorActivity.launch(
                                    context = context,
                                    launcher = objectInstanceEditorLauncher,
                                    defaultName = "újObjektum",
                                    defaultClassName = defaultClass,
                                    classDefs = classDefsList.values.toList(),
                                    objects = objectsList.values.toList()
                                )
                            }
                        ) {
                            Text("+ Új Objektum", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (objectsList.isEmpty()) {
                        Text(
                            text = "Nincsenek objektumok ezen a munkaterületen.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        objectsList.values.forEach { obj ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                                text = "🍏 ${obj.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("Típus: ${obj.className}", fontSize = 11.sp) }
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    ObjectInstanceEditorActivity.launch(
                                                        context = context,
                                                        launcher = objectInstanceEditorLauncher,
                                                        editingObjectName = obj.name,
                                                        objectInstance = obj,
                                                        classDefs = classDefsList.values.toList(),
                                                        objects = objectsList.values.toList()
                                                    )
                                                }
                                            ) {
                                                Text("✏️", fontSize = 16.sp)
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeObject(obj.name) }
                                            ) {
                                                Text("🗑️", fontSize = 16.sp)
                                            }
                                        }
                                    }

                                    // REKURZÍV FELÜLETI SZERKESZTŐ AZ OBJEKTUM KÁRTYÁN
                                    RecursiveObjectPropertiesEditor(
                                        parentPath = obj.name,
                                        properties = obj.properties,
                                        className = obj.className,
                                        classDefsList = classDefsList,
                                        objectsList = objectsList,
                                        depth = 0,
                                        onPropertyCommit = { path, valStr ->
                                            viewModel.updateObjectProperty(path, valStr)
                                        },
                                        onCreateSubObject = { pPath, pName, targetClass ->
                                            viewModel.createAndLinkSubObject(pPath, pName, targetClass)
                                        },
                                        onNullifySubObject = { path ->
                                            viewModel.nullifyObjectProperty(path)
                                        }
                                    )
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
                                FunctionEditorActivity.launch(
                                    context = context,
                                    launcher = functionEditorLauncher,
                                    workspaceVars = variableInputs.keys.toList(),
                                    workspaceFuncs = userFunctionsList.map { it.name },
                                    workspaceObjects = objectsList.keys.toList(),
                                    workspaceObjectPaths = extractedObjectPaths
                                )
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
                                                    FunctionEditorActivity.launch(
                                                        context = context,
                                                        launcher = functionEditorLauncher,
                                                        index = index,
                                                        userFunction = func,
                                                        workspaceVars = variableInputs.keys.toList(),
                                                        workspaceFuncs = userFunctionsList.map { it.name },
                                                        workspaceObjects = objectsList.keys.toList(),
                                                        workspaceObjectPaths = extractedObjectPaths
                                                    )
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

            // Változó szerkesztő szekció (Típusos Támogatással)
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
                        TextButton(onClick = {
                            newVarName = ""
                            newVarType = "StringNode"
                            newVarValue = ""
                            showAddVarDialog = true
                        }) {
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
                            val varType = activeWorkspace.getVariableType(varName)
                            val isClassDefType = classDefsList.containsKey(varType)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00796B))
                                        ) {
                                            Text(
                                                text = varName.take(2),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Text(
                                            text = varName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Típus: $varType", fontSize = 11.sp) }
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.removeVariable(varName)
                                        }
                                    ) {
                                        Text("🗑️", fontSize = 16.sp)
                                    }
                                }

                                if (isClassDefType) {
                                    // HA A VÁLTOZÓ TÍPUSA CLASSDEF (PL. ALMAFA VAGY ALMA)
                                    val varObj = objectsList[varName]
                                    if (varObj != null) {
                                        RecursiveObjectPropertiesEditor(
                                            parentPath = varName,
                                            properties = varObj.properties,
                                            className = varObj.className,
                                            classDefsList = classDefsList,
                                            objectsList = objectsList,
                                            depth = 1,
                                            onPropertyCommit = { path, valStr ->
                                                viewModel.updateObjectProperty(path, valStr)
                                            },
                                            onCreateSubObject = { pPath, pName, targetClass ->
                                                viewModel.createAndLinkSubObject(pPath, pName, targetClass)
                                            },
                                            onNullifySubObject = { path ->
                                                viewModel.nullifyObjectProperty(path)
                                            }
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, top = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "📦 $varName ($varType) = null",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.updateVariableType(varName, varType)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("➕ Alobjektum Létrehozása ($varType)", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else {
                                    // PRIMITÍV TÍPUSÚ VÁLTOZÓ
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "  Érték =",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.widthIn(min = 80.dp)
                                        )
                                        VariableValueInputField(
                                            varName = varName,
                                            initialValue = varValStr,
                                            onValueCommit = { newVal ->
                                                viewModel.updateVariableInput(varName, newVal)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
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



    // Új változó hozzáadása párbeszédablak
    if (showAddVarDialog) {
        val builtInTypes = remember { listOf("NumberNode", "BooleanNode", "StringNode", "VariableNode") }
        val availableTypes = remember(classDefsList) {
            (builtInTypes + classDefsList.keys).distinct()
        }

        AlertDialog(
            onDismissRequest = { showAddVarDialog = false },
            title = { Text("Új változó hozzáadása") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newVarName,
                        onValueChange = { newVarName = it.trim() },
                        label = { Text("Változó neve (pl. v1, v2, x)") },
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = varTypeDropdownExpanded,
                        onExpandedChange = { varTypeDropdownExpanded = !varTypeDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newVarType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Változó Típusa") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = varTypeDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = varTypeDropdownExpanded,
                            onDismissRequest = { varTypeDropdownExpanded = false }
                        ) {
                            availableTypes.forEach { typeOpt ->
                                DropdownMenuItem(
                                    text = { Text(typeOpt) },
                                    onClick = {
                                        newVarType = typeOpt
                                        varTypeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (!classDefsList.containsKey(newVarType)) {
                        OutlinedTextField(
                            value = newVarValue,
                            onValueChange = { newVarValue = it },
                            label = { Text("Kezdő értéke (szám vagy \"szöveg\")") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVarName.isNotEmpty()) {
                            viewModel.addVariable(
                                varName = newVarName,
                                valueStr = newVarValue,
                                typeName = newVarType
                            )
                            newVarName = ""
                            newVarValue = ""
                            newVarType = "StringNode"
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
 * Rekurzív felületi komponens tetszőleges mélységű egymásba ágyazott objektum tulajdonságok
 * megjelenítésére, létrehozására és szerkesztésére.
 */
@Composable
fun RecursiveObjectPropertiesEditor(
    parentPath: String,
    properties: Map<String, Any>,
    className: String,
    classDefsList: Map<String, ClassDef>,
    objectsList: Map<String, ObjectInstance>,
    depth: Int = 0,
    onPropertyCommit: (path: String, valueStr: String) -> Unit,
    onCreateSubObject: (parentPath: String, propName: String, targetClassName: String) -> Unit,
    onNullifySubObject: (path: String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        properties.forEach { (propName, propVal) ->
            val propType = classDefsList[className]?.properties?.get(propName) ?: "StringNode"
            val fullPath = if (parentPath.isEmpty()) propName else "$parentPath.$propName"

            if (classDefsList.containsKey(propType)) {
                // Alobjektum típusú tulajdonság (pl. csemete: Almafa vagy almák: Alma)
                val subObj: ObjectInstance? = when (propVal) {
                    is ObjectInstance -> propVal
                    else -> {
                        val subName = getPropertyValueAsString(propVal).trim().removeSurrounding("\"").removeSurrounding("'")
                        if (subName.isEmpty()) null else objectsList[subName]
                    }
                }

                val startPadding = (depth * 12 + 8).dp

                if (subObj != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = startPadding, top = 4.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📦 $fullPath ($propType):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { onNullifySubObject(fullPath) },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("❌ Null", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }

                        // RECURSIVE CALL FOR DEEPER LEVELS
                        RecursiveObjectPropertiesEditor(
                            parentPath = fullPath,
                            properties = subObj.properties,
                            className = subObj.className,
                            classDefsList = classDefsList,
                            objectsList = objectsList,
                            depth = depth + 1,
                            onPropertyCommit = onPropertyCommit,
                            onCreateSubObject = onCreateSubObject,
                            onNullifySubObject = onNullifySubObject
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = startPadding, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📦 $fullPath ($propType) = null",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { onCreateSubObject(parentPath, propName, propType) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("➕ Alobjektum Létrehozása ($propType)", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                // Primitív értékadó tulajdonság
                val displayStr = getPropertyValueAsString(propVal)
                val startPadding = (depth * 12 + 8).dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = startPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "  $fullPath =",
                        fontWeight = FontWeight.Bold,
                        fontSize = (13 - depth.coerceAtMost(2)).sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.widthIn(min = (140 + depth * 10).dp, max = (220 + depth * 10).dp),
                        maxLines = 1
                    )
                    VariableValueInputField(
                        varName = fullPath,
                        initialValue = displayStr,
                        onValueCommit = { newVal ->
                            onPropertyCommit(fullPath, newVal)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
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
        onValueChange = {
            textState = it
            onValueCommit(it)
        },
        modifier = modifier,
        singleLine = true,
        placeholder = { Text("Érték (szám vagy szöveg)") }
    )
}
