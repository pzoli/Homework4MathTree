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
import hu.infokristaly.homework4mathtree.model.UserFunctionData
import hu.infokristaly.homework4mathtree.ui.theme.Homework4MathTreeTheme

class FunctionEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val funcIndex = intent.getIntExtra(EXTRA_FUNC_INDEX, -1)
        val initialName = intent.getStringExtra(EXTRA_FUNC_NAME) ?: ""
        val initialParams = intent.getStringExtra(EXTRA_FUNC_PARAMS) ?: "x"
        val initialBody = intent.getStringExtra(EXTRA_FUNC_BODY) ?: ""

        val workspaceVars = intent.getStringArrayListExtra(EXTRA_WORKSPACE_VARS) ?: ArrayList()
        val workspaceFuncs = intent.getStringArrayListExtra(EXTRA_WORKSPACE_FUNCS) ?: ArrayList()
        val workspaceObjects = intent.getStringArrayListExtra(EXTRA_WORKSPACE_OBJECTS) ?: ArrayList()
        val workspaceObjectPaths = intent.getStringArrayListExtra(EXTRA_WORKSPACE_OBJECT_PATHS) ?: ArrayList()

        setContent {
            Homework4MathTreeTheme {
                FunctionEditorScreen(
                    isEditing = funcIndex >= 0,
                    funcIndex = funcIndex,
                    initialName = initialName,
                    initialParams = initialParams,
                    initialBody = initialBody,
                    workspaceVars = workspaceVars,
                    workspaceFuncs = workspaceFuncs,
                    workspaceObjects = workspaceObjects,
                    workspaceObjectPaths = workspaceObjectPaths,
                    onSave = { userFunc ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_FUNC_INDEX, funcIndex)
                            putExtra(EXTRA_USER_FUNCTION, userFunc)
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
        const val EXTRA_FUNC_INDEX = "extra_func_index"
        const val EXTRA_FUNC_NAME = "extra_func_name"
        const val EXTRA_FUNC_PARAMS = "extra_func_params"
        const val EXTRA_FUNC_BODY = "extra_func_body"
        const val EXTRA_USER_FUNCTION = "extra_user_function"

        const val EXTRA_WORKSPACE_VARS = "extra_workspace_vars"
        const val EXTRA_WORKSPACE_FUNCS = "extra_workspace_funcs"
        const val EXTRA_WORKSPACE_OBJECTS = "extra_workspace_objects"
        const val EXTRA_WORKSPACE_OBJECT_PATHS = "extra_workspace_object_paths"

        fun launch(
            context: Context,
            launcher: ActivityResultLauncher<Intent>,
            index: Int? = null,
            userFunction: UserFunctionData? = null,
            workspaceVars: List<String>,
            workspaceFuncs: List<String>,
            workspaceObjects: List<String>,
            workspaceObjectPaths: List<String>
        ) {
            val intent = Intent(context, FunctionEditorActivity::class.java).apply {
                if (userFunction != null && index != null) {
                    putExtra(EXTRA_FUNC_INDEX, index)
                    putExtra(EXTRA_FUNC_NAME, userFunction.name)
                    putExtra(EXTRA_FUNC_PARAMS, userFunction.parameters.joinToString(", "))
                    putExtra(EXTRA_FUNC_BODY, userFunction.expressionBody)
                } else {
                    putExtra(EXTRA_FUNC_INDEX, -1)
                    putExtra(EXTRA_FUNC_NAME, "")
                    putExtra(EXTRA_FUNC_PARAMS, "x")
                    putExtra(EXTRA_FUNC_BODY, "")
                }
                putStringArrayListExtra(EXTRA_WORKSPACE_VARS, ArrayList(workspaceVars))
                putStringArrayListExtra(EXTRA_WORKSPACE_FUNCS, ArrayList(workspaceFuncs))
                putStringArrayListExtra(EXTRA_WORKSPACE_OBJECTS, ArrayList(workspaceObjects))
                putStringArrayListExtra(EXTRA_WORKSPACE_OBJECT_PATHS, ArrayList(workspaceObjectPaths))
            }
            launcher.launch(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionEditorScreen(
    isEditing: Boolean,
    funcIndex: Int,
    initialName: String,
    initialParams: String,
    initialBody: String,
    workspaceVars: List<String>,
    workspaceFuncs: List<String>,
    workspaceObjects: List<String>,
    workspaceObjectPaths: List<String>,
    onSave: (UserFunctionData) -> Unit,
    onCancel: () -> Unit
) {
    var funcNameInput by remember { mutableStateOf(initialName) }
    var funcParamsInput by remember { mutableStateOf(initialParams) }
    var funcBodyInput by remember { mutableStateOf(initialBody) }
    var funcError by remember { mutableStateOf<String?>(null) }

    val builtInMathFunctions = remember {
        listOf(
            "sin(", "cos(", "tan(", "sqrt(", "sqr(", "abs(", "log(", "exp(",
            "max(", "min(", "pow(", "pi", "e", "ceil(", "floor("
        )
    }

    val tokenDelimiters = remember {
        charArrayOf(' ', '\n', '\t', '(', ')', '+', '-', '*', '/', '=', '<', '>', '!', '%', ',', '^', '[', ']', ';')
    }

    val parsedParams = remember(funcParamsInput) {
        funcParamsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Függvény szerkesztése" else "Új függvény / kifejezés",
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
                            if (funcNameInput.isNotEmpty() && funcBodyInput.isNotEmpty()) {
                                onSave(
                                    UserFunctionData(
                                        name = funcNameInput,
                                        parameters = parsedParams,
                                        expressionBody = funcBodyInput
                                    )
                                )
                            } else {
                                funcError = "Add meg a függvény nevét és törzsét!"
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
                value = funcNameInput,
                onValueChange = {
                    funcNameInput = it.trim()
                    funcError = null
                },
                label = { Text("Függvény neve (pl. f1, sqr)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = funcParamsInput,
                onValueChange = {
                    funcParamsInput = it
                    funcError = null
                },
                label = { Text("Paraméterek (pl. x vagy x, y)") },
                placeholder = { Text("pl.: x") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = funcBodyInput,
                onValueChange = {
                    funcBodyInput = it
                    funcError = null
                },
                label = { Text("Kifejezés törzse (pl. 2 * x + 3)") },
                placeholder = { Text("pl.: 2 * x") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )

            // Prediktív Szövegbevitel a Kifejezés Törzséhez (Predictive Autocomplete Suggestions)
            val lastDelimIdx = funcBodyInput.lastIndexOfAny(tokenDelimiters)
            val currentToken = if (lastDelimIdx == -1) funcBodyInput else funcBodyInput.substring(lastDelimIdx + 1)

            val predictiveSuggestions: List<String> = remember(
                currentToken, parsedParams, workspaceVars, workspaceFuncs,
                workspaceObjects, workspaceObjectPaths
            ) {
                val allCandidates = (
                    parsedParams +
                    builtInMathFunctions +
                    workspaceVars +
                    workspaceObjects +
                    workspaceObjectPaths +
                    workspaceFuncs
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
                        text = "💡 Prediktív kifejezés javaslatok:",
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
                                    val lastDelim = funcBodyInput.lastIndexOfAny(tokenDelimiters)
                                    funcBodyInput = if (lastDelim == -1) {
                                        suggestion
                                    } else {
                                        funcBodyInput.substring(0, lastDelim + 1) + suggestion
                                    }
                                    funcError = null
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

            funcError?.let { err ->
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
                        if (funcNameInput.isNotEmpty() && funcBodyInput.isNotEmpty()) {
                            onSave(
                                UserFunctionData(
                                    name = funcNameInput,
                                    parameters = parsedParams,
                                    expressionBody = funcBodyInput
                                )
                            )
                        } else {
                            funcError = "Add meg a függvény nevét és törzsét!"
                        }
                    }
                ) {
                    Text("Mentés")
                }
            }
        }
    }
}
