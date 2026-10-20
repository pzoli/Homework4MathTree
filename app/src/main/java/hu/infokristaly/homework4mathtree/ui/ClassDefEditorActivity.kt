package hu.infokristaly.homework4mathtree.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
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
import hu.infokristaly.homework4mathtree.model.ClassDef
import hu.infokristaly.homework4mathtree.ui.theme.Homework4MathTreeTheme

class ClassDefEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val editingClassName = intent.getStringExtra(EXTRA_EDITING_CLASSDEF_NAME)
        val initialName = intent.getStringExtra(EXTRA_CLASSDEF_NAME) ?: "ÚjOsztály"
        val initialClassDef = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_CLASSDEF, ClassDef::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_CLASSDEF) as? ClassDef
        }
        val initialProps = initialClassDef?.properties ?: mapOf("szín" to "StringNode")

        val availableTypes = intent.getStringArrayListExtra(EXTRA_AVAILABLE_PROPERTY_TYPES)
            ?: ArrayList(listOf("NumberNode", "BooleanNode", "StringNode", "VariableNode"))

        setContent {
            Homework4MathTreeTheme {
                ClassDefEditorScreen(
                    isEditing = editingClassName != null,
                    editingClassName = editingClassName,
                    initialName = initialName,
                    initialProps = initialProps,
                    availablePropertyTypes = availableTypes,
                    onSave = { classDef ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_EDITING_CLASSDEF_NAME, editingClassName)
                            putExtra(EXTRA_CLASSDEF, classDef)
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
        const val EXTRA_EDITING_CLASSDEF_NAME = "extra_editing_classdef_name"
        const val EXTRA_CLASSDEF_NAME = "extra_classdef_name"
        const val EXTRA_CLASSDEF = "extra_classdef"
        const val EXTRA_AVAILABLE_PROPERTY_TYPES = "extra_available_property_types"

        fun launch(
            context: Context,
            launcher: ActivityResultLauncher<Intent>,
            editingClassName: String? = null,
            classDef: ClassDef? = null,
            availablePropertyTypes: List<String>
        ) {
            val intent = Intent(context, ClassDefEditorActivity::class.java).apply {
                putExtra(EXTRA_EDITING_CLASSDEF_NAME, editingClassName)
                putExtra(EXTRA_CLASSDEF_NAME, classDef?.name ?: "ÚjOsztály")
                if (classDef != null) {
                    putExtra(EXTRA_CLASSDEF, classDef)
                }
                putStringArrayListExtra(EXTRA_AVAILABLE_PROPERTY_TYPES, ArrayList(availablePropertyTypes))
            }
            launcher.launch(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDefEditorScreen(
    isEditing: Boolean,
    editingClassName: String?,
    initialName: String,
    initialProps: Map<String, String>,
    availablePropertyTypes: List<String>,
    onSave: (ClassDef) -> Unit,
    onCancel: () -> Unit
) {
    var cdNameInput by remember { mutableStateOf(initialName) }
    var cdPropsState by remember { mutableStateOf(initialProps) }
    var newCdPropName by remember { mutableStateOf("") }
    var newCdPropType by remember { mutableStateOf("StringNode") }
    var propTypeDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Osztály szerkesztése" else "Új Osztály (ClassDef)",
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
                            if (cdNameInput.isNotEmpty()) {
                                onSave(ClassDef(name = cdNameInput, properties = cdPropsState))
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
                value = cdNameInput,
                onValueChange = { cdNameInput = it.trim() },
                label = { Text("Osztály neve (pl. Alma, Almafa)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Tulajdonságok (Mezők):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (cdPropsState.isEmpty()) {
                Text(
                    text = "Nincsenek mezők hozzáadva.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                cdPropsState.forEach { (pName, pType) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• $pName : $pType",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(onClick = { cdPropsState = cdPropsState - pName }) {
                            Text("🗑️", fontSize = 16.sp)
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(text = "Új Mező Hozzáadása:", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = newCdPropName,
                    onValueChange = { newCdPropName = it.trim() },
                    label = { Text("Mező neve") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = propTypeDropdownExpanded,
                    onExpandedChange = { propTypeDropdownExpanded = !propTypeDropdownExpanded },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = newCdPropType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Típus") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = propTypeDropdownExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = propTypeDropdownExpanded,
                        onDismissRequest = { propTypeDropdownExpanded = false }
                    ) {
                        availablePropertyTypes.forEach { typeOpt ->
                            DropdownMenuItem(
                                text = { Text(typeOpt) },
                                onClick = {
                                    newCdPropType = typeOpt
                                    propTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (newCdPropName.isNotEmpty() && newCdPropType.isNotEmpty()) {
                            cdPropsState = cdPropsState + (newCdPropName to newCdPropType)
                            newCdPropName = ""
                        }
                    }
                ) {
                    Text("+")
                }
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
                        if (cdNameInput.isNotEmpty()) {
                            onSave(ClassDef(name = cdNameInput, properties = cdPropsState))
                        }
                    }
                ) {
                    Text("Mentés")
                }
            }
        }
    }
}
