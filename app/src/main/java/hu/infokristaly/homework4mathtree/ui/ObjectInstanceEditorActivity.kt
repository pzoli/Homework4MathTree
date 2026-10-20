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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.infokristaly.homework4mathtree.model.ClassDef
import hu.infokristaly.homework4mathtree.model.ObjectInstance
import hu.infokristaly.homework4mathtree.model.StringNode
import hu.infokristaly.homework4mathtree.model.createTypedPropertyValue
import hu.infokristaly.homework4mathtree.model.updateLocalNestedObject
import hu.infokristaly.homework4mathtree.model.updateLocalNestedPropertyValue
import hu.infokristaly.homework4mathtree.ui.theme.Homework4MathTreeTheme

class ObjectInstanceEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val editingObjectName = intent.getStringExtra(EXTRA_EDITING_OBJECT_NAME)
        val initialName = intent.getStringExtra(EXTRA_OBJECT_NAME) ?: "újObjektum"
        val initialClassName = intent.getStringExtra(EXTRA_OBJECT_CLASS_NAME) ?: "Alma"

        val initialObject = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_OBJECT, ObjectInstance::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_OBJECT) as? ObjectInstance
        }

        val classDefsList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_CLASS_DEFS_LIST, ArrayList::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_CLASS_DEFS_LIST) as? ArrayList<*>
        }
        val classDefsMap = classDefsList?.filterIsInstance<ClassDef>()?.associateBy { it.name } ?: emptyMap()

        val objectsList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_OBJECTS_LIST, ArrayList::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_OBJECTS_LIST) as? ArrayList<*>
        }
        val objectsMap = objectsList?.filterIsInstance<ObjectInstance>()?.associateBy { it.name } ?: emptyMap()

        val initialProps = run {
            val props = mutableMapOf<String, Any>()
            classDefsMap[initialClassName]?.properties?.forEach { (pName, pType) ->
                val existingVal = initialObject?.properties?.get(pName)
                props[pName] = existingVal ?: createTypedPropertyValue(pType, "")
            }
            initialObject?.properties?.forEach { (pName, pVal) ->
                if (!props.containsKey(pName)) {
                    props[pName] = pVal
                }
            }
            props
        }

        setContent {
            Homework4MathTreeTheme {
                ObjectInstanceEditorScreen(
                    isEditing = editingObjectName != null,
                    editingObjectName = editingObjectName,
                    initialName = initialName,
                    initialClassName = initialClassName,
                    initialProps = initialProps,
                    classDefsMap = classDefsMap,
                    objectsMap = objectsMap,
                    onSave = { objectInstance ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_EDITING_OBJECT_NAME, editingObjectName)
                            putExtra(EXTRA_OBJECT, objectInstance)
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
        const val EXTRA_EDITING_OBJECT_NAME = "extra_editing_object_name"
        const val EXTRA_OBJECT_NAME = "extra_object_name"
        const val EXTRA_OBJECT_CLASS_NAME = "extra_object_class_name"
        const val EXTRA_OBJECT = "extra_object"
        const val EXTRA_CLASS_DEFS_LIST = "extra_class_defs_list"
        const val EXTRA_OBJECTS_LIST = "extra_objects_list"

        fun launch(
            context: Context,
            launcher: ActivityResultLauncher<Intent>,
            editingObjectName: String? = null,
            objectInstance: ObjectInstance? = null,
            defaultName: String = "újObjektum",
            defaultClassName: String = "Alma",
            classDefs: List<ClassDef>,
            objects: List<ObjectInstance>
        ) {
            val intent = Intent(context, ObjectInstanceEditorActivity::class.java).apply {
                putExtra(EXTRA_EDITING_OBJECT_NAME, editingObjectName)
                putExtra(EXTRA_OBJECT_NAME, objectInstance?.name ?: defaultName)
                putExtra(EXTRA_OBJECT_CLASS_NAME, objectInstance?.className ?: defaultClassName)
                if (objectInstance != null) {
                    putExtra(EXTRA_OBJECT, objectInstance)
                }
                putExtra(EXTRA_CLASS_DEFS_LIST, ArrayList(classDefs))
                putExtra(EXTRA_OBJECTS_LIST, ArrayList(objects))
            }
            launcher.launch(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectInstanceEditorScreen(
    isEditing: Boolean,
    editingObjectName: String?,
    initialName: String,
    initialClassName: String,
    initialProps: Map<String, Any>,
    classDefsMap: Map<String, ClassDef>,
    objectsMap: Map<String, ObjectInstance>,
    onSave: (ObjectInstance) -> Unit,
    onCancel: () -> Unit
) {
    var objNameInput by remember { mutableStateOf(initialName) }
    var objClassInput by remember { mutableStateOf(initialClassName) }
    var objPropsState by remember { mutableStateOf(initialProps) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Objektum szerkesztése" else "Új Objektum (Object)",
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
                            if (objNameInput.isNotEmpty() && objClassInput.isNotEmpty()) {
                                onSave(
                                    ObjectInstance(
                                        name = objNameInput,
                                        className = objClassInput,
                                        properties = objPropsState
                                    )
                                )
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
                value = objNameInput,
                onValueChange = { objNameInput = it.trim() },
                label = { Text("Objektum neve (pl. zöldAlmafa)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Osztály (ClassDef) Típusa:", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                classDefsMap.keys.forEach { cdName ->
                    FilterChip(
                        selected = objClassInput == cdName,
                        onClick = {
                            objClassInput = cdName
                            val initialTypedProps = mutableMapOf<String, Any>()
                            classDefsMap[cdName]?.properties?.forEach { (pName, pType) ->
                                initialTypedProps[pName] = objPropsState[pName] ?: createTypedPropertyValue(pType, "")
                            }
                            objPropsState = initialTypedProps
                        },
                        label = { Text(cdName) }
                    )
                }
            }

            Text(
                text = "Tulajdonság Értékek (Beállítás):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // REKURZÍV FELÜLETI SZERKESZTŐ
            RecursiveObjectPropertiesEditor(
                parentPath = objNameInput,
                properties = objPropsState,
                className = objClassInput,
                classDefsList = classDefsMap,
                objectsList = objectsMap,
                depth = 0,
                onPropertyCommit = { path, valStr ->
                    val pathSegments = path.split(".")
                    val relativePath = if (pathSegments.isNotEmpty() && pathSegments[0] == objNameInput) {
                        pathSegments.subList(1, pathSegments.size)
                    } else {
                        pathSegments
                    }

                    if (relativePath.isNotEmpty()) {
                        val propType = resolvePropertyType(objClassInput, relativePath, classDefsMap)
                        val typedVal = createTypedPropertyValue(propType, valStr)
                        objPropsState = updateLocalNestedPropertyValue(objPropsState, relativePath, typedVal)
                    }
                },
                onCreateSubObject = { pPath, pName, targetClass ->
                    val subClassDef = classDefsMap[targetClass]
                    val subProps = mutableMapOf<String, Any>()
                    subClassDef?.properties?.forEach { (sName, sType) ->
                        subProps[sName] = createTypedPropertyValue(sType, "")
                    }
                    val createdSubObj = ObjectInstance(
                        name = "${pPath.replace(".", "_")}_$pName",
                        className = targetClass,
                        properties = subProps
                    )

                    val pathSegments = pPath.split(".")
                    val relativePath = if (pathSegments.isNotEmpty() && pathSegments[0] == objNameInput) {
                        pathSegments.subList(1, pathSegments.size)
                    } else {
                        pathSegments
                    }

                    val fullRelativeSegments = relativePath + pName
                    objPropsState = updateLocalNestedObject(objPropsState, fullRelativeSegments, createdSubObj)
                },
                onNullifySubObject = { path ->
                    val pathSegments = path.split(".")
                    val relativePath = if (pathSegments.isNotEmpty() && pathSegments[0] == objNameInput) {
                        pathSegments.subList(1, pathSegments.size)
                    } else {
                        pathSegments
                    }

                    if (relativePath.isNotEmpty()) {
                        objPropsState = updateLocalNestedPropertyValue(objPropsState, relativePath, StringNode(""))
                    }
                }
            )

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
                        if (objNameInput.isNotEmpty() && objClassInput.isNotEmpty()) {
                            onSave(
                                ObjectInstance(
                                    name = objNameInput,
                                    className = objClassInput,
                                    properties = objPropsState
                                )
                            )
                        }
                    }
                ) {
                    Text("Mentés")
                }
            }
        }
    }
}

private fun resolvePropertyType(
    rootClassName: String,
    relativePath: List<String>,
    classDefsMap: Map<String, ClassDef>
): String {
    var currentClass = rootClassName
    var foundType = "StringNode"
    for (i in relativePath.indices) {
        val seg = relativePath[i]
        val propType = classDefsMap[currentClass]?.properties?.get(seg) ?: "StringNode"
        foundType = propType
        currentClass = propType
    }
    return foundType
}
