package hu.infokristaly.homework4mathtree.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.infokristaly.homework4mathtree.model.*

/**
 * Fa nézet komponens, amely rekurzívan jeleníti meg a MathNode struktúrát kártyák formájában.
 */
@Composable
fun HierarchicalTreeView(
    node: MathNode,
    modifier: Modifier = Modifier,
    branchLabel: String? = "Gyökér művelet (Root)",
    depth: Int = 0
) {
    var isExpanded by remember { mutableStateOf(true) }

    val nodeColor = getNodeColor(node)
    val formattedValue = formatValue(node.getValue())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = nodeColor.copy(alpha = 0.08f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(nodeColor.copy(alpha = 0.5f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !node.isLeaf) { isExpanded = !isExpanded }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Csomópont szimbólum badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(nodeColor)
                    ) {
                        Text(
                            text = node.symbol,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column {
                        if (branchLabel != null) {
                            Text(
                                text = branchLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = node.description,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Eredmény érték (getValue() eredménye ezen a részfán)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "getValue() = $formattedValue",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!node.isLeaf) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "▲" else "▼",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Gyermek ágak (Bal és Jobb oldal)
            AnimatedVisibility(visible = isExpanded && !node.isLeaf) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp)
                ) {
                    // Bal oldali ág
                    node.left?.let { leftChild ->
                        HierarchicalTreeView(
                            node = leftChild,
                            branchLabel = "Bal ág (Left)",
                            depth = depth + 1
                        )
                    }

                    // Jobb oldali ág
                    node.right?.let { rightChild ->
                        HierarchicalTreeView(
                            node = rightChild,
                            branchLabel = "Jobb ág (Right)",
                            depth = depth + 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grafikus rajzolt 2D fa ábra Canvas-el.
 */
@Composable
fun GraphicalTreeView(
    rootNode: MathNode,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val nodeRadius = 22.dp
    val levelHeight = 75.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(horizontalScrollState)
            .verticalScroll(scrollState)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val treeDepth = getTreeDepth(rootNode)
        val canvasWidth = ((1 shl (treeDepth - 1)).coerceAtLeast(4) * 80).dp
        val canvasHeight = (treeDepth * 80 + 40).dp

        Canvas(
            modifier = Modifier
                .width(canvasWidth)
                .height(canvasHeight)
        ) {
            val rootX = size.width / 2f
            val rootY = 35.dp.toPx()
            val initialXOffset = size.width / 4f

            drawNodeAndConnections(
                node = rootNode,
                x = rootX,
                y = rootY,
                xOffset = initialXOffset,
                levelHeight = levelHeight.toPx(),
                nodeRadius = nodeRadius.toPx()
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNodeAndConnections(
    node: MathNode,
    x: Float,
    y: Float,
    xOffset: Float,
    levelHeight: Float,
    nodeRadius: Float
) {
    val color = getNodeColor(node)

    // Bal oldali gyermek vonal
    node.left?.let { leftChild ->
        val childX = x - xOffset
        val childY = y + levelHeight
        drawLine(
            color = Color.LightGray,
            start = Offset(x, y),
            end = Offset(childX, childY),
            strokeWidth = 4f
        )
        drawNodeAndConnections(
            node = leftChild,
            x = childX,
            y = childY,
            xOffset = (xOffset / 2f).coerceAtLeast(40f),
            levelHeight = levelHeight,
            nodeRadius = nodeRadius
        )
    }

    // Jobb oldali gyermek vonal
    node.right?.let { rightChild ->
        val childX = x + xOffset
        val childY = y + levelHeight
        drawLine(
            color = Color.LightGray,
            start = Offset(x, y),
            end = Offset(childX, childY),
            strokeWidth = 4f
        )
        drawNodeAndConnections(
            node = rightChild,
            x = childX,
            y = childY,
            xOffset = (xOffset / 2f).coerceAtLeast(40f),
            levelHeight = levelHeight,
            nodeRadius = nodeRadius
        )
    }

    // Csomópont kör berajzolása
    drawCircle(
        color = color,
        radius = nodeRadius,
        center = Offset(x, y)
    )

    // Szöveg és érték kirajzolása
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.WHITE
            textSize = 34f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        drawText(node.symbol, x, y + 11f, paint)

        val valPaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.DKGRAY
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val formattedVal = formatValue(node.getValue())
        drawText("=$formattedVal", x, y + nodeRadius + 26f, valPaint)
    }
}

private fun getTreeDepth(node: MathNode?): Int {
    if (node == null) return 0
    val leftDepth = getTreeDepth(node.left)
    val rightDepth = getTreeDepth(node.right)
    return 1 + maxOf(leftDepth, rightDepth)
}

fun getNodeColor(node: MathNode): Color {
    return when (node) {
        is AdditionNode -> Color(0xFF2E7D32)            // Zöld
        is SubtractionNode -> Color(0xFFC62828)         // Piros
        is MultiplicationNode -> Color(0xFF1565C0)      // Kék
        is DivisionNode -> Color(0xFFE65100)            // Narancs
        is ExponentiationNode -> Color(0xFF6A1B9A)      // Lila
        is RootNode -> Color(0xFF00838F)                // Cián
        is EqualNode, is NotEqualNode,
        is LessThanNode, is GreaterThanNode,
        is LessOrEqualNode, is GreaterOrEqualNode -> Color(0xFFF57C00) // Amber / Narancssárga
        is AndNode, is OrNode, is NotNode, is BooleanNode -> Color(0xFF3F51B5) // Indigo
        is StringNode -> Color(0xFF795548)              // Barna
        is NumberNode -> Color(0xFF424242)              // Sötétszürke
        is VariableNode -> Color(0xFF00796B)            // Ciánkék / Teal
        is FunctionCallNode -> Color(0xFF4A148C)        // Mélylila
        is TrigonometricNode -> Color(0xFFC2185B)       // Rózsaszín / Rose
        else -> Color(0xFF424242)
    }
}

fun formatValue(value: Value): String = value.toDisplayString()
