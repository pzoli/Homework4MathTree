package hu.infokristaly.homework4mathtree.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.infokristaly.homework4mathtree.model.Rule
import hu.infokristaly.homework4mathtree.model.RuleParser
import hu.infokristaly.homework4mathtree.ui.theme.Homework4MathTreeTheme
import java.util.UUID

class RuleEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val editingRuleIndex = intent.getIntExtra(EXTRA_RULE_INDEX, -1)
        val ruleId = intent.getStringExtra(EXTRA_RULE_ID) ?: UUID.randomUUID().toString()
        val initialName = intent.getStringExtra(EXTRA_RULE_NAME) ?: ""
        val initialLabel = intent.getStringExtra(EXTRA_RULE_LABEL) ?: "Alapértelmezett"
        val initialText = intent.getStringExtra(EXTRA_RULE_TEXT) ?: ""
        val initialChaining = intent.getBooleanExtra(EXTRA_RULE_CHAINING, true)

        val workspaceVars = intent.getStringArrayListExtra(EXTRA_WORKSPACE_VARS) ?: ArrayList()
        val workspaceFuncs = intent.getStringArrayListExtra(EXTRA_WORKSPACE_FUNCS) ?: ArrayList()
        val workspaceTimers = intent.getStringArrayListExtra(EXTRA_WORKSPACE_TIMERS) ?: ArrayList()
        val workspaceObjects = intent.getStringArrayListExtra(EXTRA_WORKSPACE_OBJECTS) ?: ArrayList()
        val workspaceObjectPaths = intent.getStringArrayListExtra(EXTRA_WORKSPACE_OBJECT_PATHS) ?: ArrayList()
        val classDefs = intent.getStringArrayListExtra(EXTRA_CLASS_DEFS) ?: ArrayList()
        val availableLabels = intent.getStringArrayListExtra(EXTRA_AVAILABLE_LABELS) ?: ArrayList()

        setContent {
            Homework4MathTreeTheme {
                RuleEditorScreen(
                    isEditing = editingRuleIndex >= 0,
                    ruleId = ruleId,
                    initialName = initialName,
                    initialLabel = initialLabel,
                    initialText = initialText,
                    initialChaining = initialChaining,
                    workspaceVars = workspaceVars,
                    workspaceFuncs = workspaceFuncs,
                    workspaceTimers = workspaceTimers,
                    workspaceObjects = workspaceObjects,
                    workspaceObjectPaths = workspaceObjectPaths,
                    classDefs = classDefs,
                    availableLabels = availableLabels,
                    onSave = { parsedRule ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_RULE_INDEX, editingRuleIndex)
                            putExtra(EXTRA_RULE, parsedRule)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_RULE_INDEX = "extra_rule_index"
        const val EXTRA_RULE_ID = "extra_rule_id"
        const val EXTRA_RULE_NAME = "extra_rule_name"
        const val EXTRA_RULE_LABEL = "extra_rule_label"
        const val EXTRA_RULE_TEXT = "extra_rule_text"
        const val EXTRA_RULE_CHAINING = "extra_rule_chaining"
        const val EXTRA_RULE = "extra_rule"

        const val EXTRA_WORKSPACE_VARS = "extra_workspace_vars"
        const val EXTRA_WORKSPACE_FUNCS = "extra_workspace_funcs"
        const val EXTRA_WORKSPACE_TIMERS = "extra_workspace_timers"
        const val EXTRA_WORKSPACE_OBJECTS = "extra_workspace_objects"
        const val EXTRA_WORKSPACE_OBJECT_PATHS = "extra_workspace_object_paths"
        const val EXTRA_CLASS_DEFS = "extra_class_defs"
        const val EXTRA_AVAILABLE_LABELS = "extra_available_labels"

        fun launch(
            context: Context,
            launcher: ActivityResultLauncher<Intent>,
            index: Int? = null,
            rule: Rule? = null,
            defaultRuleName: String,
            workspaceVars: List<String>,
            workspaceFuncs: List<String>,
            workspaceTimers: List<String>,
            workspaceObjects: List<String>,
            workspaceObjectPaths: List<String>,
            classDefs: List<String>,
            availableLabels: List<String>
        ) {
            val intent = Intent(context, RuleEditorActivity::class.java).apply {
                if (rule != null && index != null) {
                    putExtra(EXTRA_RULE_INDEX, index)
                    putExtra(EXTRA_RULE_ID, rule.id)
                    putExtra(EXTRA_RULE_NAME, rule.name)
                    putExtra(EXTRA_RULE_LABEL, rule.label)
                    putExtra(EXTRA_RULE_TEXT, rule.toRuleText())
                    putExtra(EXTRA_RULE_CHAINING, rule.mayCauseChaining)
                } else {
                    putExtra(EXTRA_RULE_INDEX, -1)
                    putExtra(EXTRA_RULE_NAME, defaultRuleName)
                    putExtra(EXTRA_RULE_LABEL, "Alapértelmezett")
                    putExtra(EXTRA_RULE_TEXT, "if x1 <= 10 then conclude that x1 = x1 + 2")
                    putExtra(EXTRA_RULE_CHAINING, true)
                }
                putStringArrayListExtra(EXTRA_WORKSPACE_VARS, ArrayList(workspaceVars))
                putStringArrayListExtra(EXTRA_WORKSPACE_FUNCS, ArrayList(workspaceFuncs))
                putStringArrayListExtra(EXTRA_WORKSPACE_TIMERS, ArrayList(workspaceTimers))
                putStringArrayListExtra(EXTRA_WORKSPACE_OBJECTS, ArrayList(workspaceObjects))
                putStringArrayListExtra(EXTRA_WORKSPACE_OBJECT_PATHS, ArrayList(workspaceObjectPaths))
                putStringArrayListExtra(EXTRA_CLASS_DEFS, ArrayList(classDefs))
                putStringArrayListExtra(EXTRA_AVAILABLE_LABELS, ArrayList(availableLabels))
            }
            launcher.launch(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(
    isEditing: Boolean,
    ruleId: String,
    initialName: String,
    initialLabel: String,
    initialText: String,
    initialChaining: Boolean,
    workspaceVars: List<String>,
    workspaceFuncs: List<String>,
    workspaceTimers: List<String>,
    workspaceObjects: List<String>,
    workspaceObjectPaths: List<String>,
    classDefs: List<String>,
    availableLabels: List<String>,
    onSave: (Rule) -> Unit,
    onCancel: () -> Unit
) {
    var ruleNameInput by remember { mutableStateOf(initialName) }
    var ruleLabelInput by remember { mutableStateOf(initialLabel) }
    var ruleTextInput by remember { mutableStateOf(initialText) }
    var ruleChainingInput by remember { mutableStateOf(initialChaining) }
    var ruleError by remember { mutableStateOf<String?>(null) }

    val tokenDelimiters = remember {
        charArrayOf(' ', '\n', '\t', '(', ')', '+', '-', '*', '/', '=', '<', '>', '!', '%', ',', '"', '[', ']', ';')
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Szabály szerkesztése" else "Új Szabály (Rule Editor)",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Text("⬅️", fontSize = 18.sp)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            try {
                                val parsedRule = RuleParser.parseRuleText(
                                    name = ruleNameInput.ifEmpty { "Szabály" },
                                    text = ruleTextInput,
                                    defaultLabel = ruleLabelInput.ifEmpty { "Alapértelmezett" },
                                    mayCauseChaining = ruleChainingInput,
                                    id = ruleId
                                )
                                onSave(parsedRule)
                            } catch (e: Exception) {
                                ruleError = e.message ?: "Érvénytelen szabály formátum!"
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Mentés")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = ruleNameInput,
                onValueChange = { ruleNameInput = it },
                label = { Text("Szabály neve (pl. Szabály 1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ruleLabelInput,
                onValueChange = { ruleLabelInput = it },
                label = { Text("Címke / Tag (pl. init, Klíma, Teszt)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
                    fontSize = 13.sp,
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
                    onClick = { ruleTextInput = "unconditionally create object from Alma into variable alma1" },
                    label = { Text("create object variable minta", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = { ruleTextInput = "if x1 > 10 then create object from Alma into property zöldAlmafa.almák" },
                    label = { Text("create object property minta", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = { ruleTextInput = "if zöldAlmafa.almák.szín == \"zöld\" then conclude that zöldAlmafa.almák.szín = \"piros\"" },
                    label = { Text("zöldAlmafa.almák.szín minta", fontSize = 11.sp) }
                )
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
                    ruleError = null
                },
                label = { Text("Szabály Szövege (if...then...)") },
                placeholder = { Text("pl.: unconditionally create object from Alma into variable alma1") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )

            // Prediktív Szövegbevitel (Predictive Autocomplete Suggestions)
            val allKeywordsAndTemplates = remember {
                listOf(
                    "if ",
                    "then conclude that ",
                    "then invoke ",
                    "then start timer ",
                    "then stop timer ",
                    "then create object from ",
                    "unconditionally conclude that ",
                    "unconditionally invoke ",
                    "unconditionally start timer ",
                    "unconditionally stop timer ",
                    "unconditionally create object from ",
                    "into variable ",
                    "into property ",
                    "and ",
                    "or ",
                    "not "
                )
            }

            val lastDelimIdx = ruleTextInput.lastIndexOfAny(tokenDelimiters)
            val currentToken = if (lastDelimIdx == -1) ruleTextInput else ruleTextInput.substring(lastDelimIdx + 1)

            val predictiveSuggestions: List<String> = remember(
                currentToken, workspaceVars, workspaceFuncs, workspaceTimers,
                availableLabels, workspaceObjects, workspaceObjectPaths, classDefs
            ) {
                val allCandidates = (
                    workspaceVars +
                    workspaceObjects +
                    workspaceObjectPaths +
                    classDefs +
                    workspaceFuncs +
                    workspaceTimers.map { "\"$it\"" } +
                    availableLabels +
                    allKeywordsAndTemplates
                ).distinct()

                if (currentToken.isEmpty()) {
                    allCandidates.take(10)
                } else {
                    val trimmedToken = currentToken.trim()
                    val matching = allCandidates.filter { candidate ->
                        !candidate.equals(trimmedToken, ignoreCase = true) &&
                        (candidate.contains(trimmedToken, ignoreCase = true) ||
                         (trimmedToken.contains(".") && candidate.contains(trimmedToken.substringAfterLast("."), ignoreCase = true)))
                    }

                    matching.sortedWith(
                        compareBy<String> { candidate ->
                            when {
                                candidate.startsWith(trimmedToken, ignoreCase = true) -> 0
                                candidate.split(".").any { it.startsWith(trimmedToken, ignoreCase = true) } -> 1
                                else -> 2
                            }
                        }.thenBy { it.length }
                    ).take(10)
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
                                    val lastDelim = ruleTextInput.lastIndexOfAny(tokenDelimiters)
                                    ruleTextInput = if (lastDelim == -1) {
                                        suggestion
                                    } else {
                                        ruleTextInput.substring(0, lastDelim + 1) + suggestion
                                    }
                                    ruleError = null
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

            ruleError?.let { err ->
                Text(
                    text = "⚠️ $err",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Mégse")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        try {
                            val parsedRule = RuleParser.parseRuleText(
                                name = ruleNameInput.ifEmpty { "Szabály" },
                                text = ruleTextInput,
                                defaultLabel = ruleLabelInput.ifEmpty { "Alapértelmezett" },
                                mayCauseChaining = ruleChainingInput,
                                id = ruleId
                            )
                            onSave(parsedRule)
                        } catch (e: Exception) {
                            ruleError = e.message ?: "Érvénytelen szabály formátum!"
                        }
                    }
                ) {
                    Text("Mentés")
                }
            }
        }
    }
}
