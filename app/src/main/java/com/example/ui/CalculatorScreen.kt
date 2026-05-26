package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Calculation
import com.example.math.MathEvaluator
import kotlin.math.roundToInt

// Elegant Dark Theme Palette Colors
val SlateDarkBg = Color(0xFF1C1B1F)       // Deep charcoal background
val SlateCard = Color(0xFF25232A)         // Elevated surface container
val SlateKeypad = Color(0xFF1C1B1F)       // Inner standard key background
val TealNeon = Color(0xFFD0BCFF)          // Beautiful elegant light purple accent
val AmberAccent = Color(0xFFD0BCFF)       // Light purple operator text accent
val SoftRed = Color(0xFFEFB8C8)           // Pinkish tertiary Rose accent for clear operations
val SoftGreen = Color(0xFFD0BCFF)         // Same elegant light purple/accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val isRadians by viewModel.isRadians.collectAsStateWithLifecycle()
    val historyLog by viewModel.historyState.collectAsStateWithLifecycle()

    var showHistoryDrawer by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matrix",
                            color = Color(0xFFE6E1E5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Calc",
                            color = TealNeon,
                            fontWeight = FontWeight.Light,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                actions = {
                    // Language switcher button
                    var showLanguageMenu by remember { mutableStateOf(false) }
                    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()

                    Box {
                        Button(
                            onClick = { showLanguageMenu = !showLanguageMenu },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlateKeypad,
                                contentColor = TealNeon
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("language_selector_button")
                        ) {
                            Text(
                                text = when (currentLang) {
                                    AppLanguage.SYSTEM -> "🌐"
                                    AppLanguage.SPANISH -> "ES"
                                    AppLanguage.ENGLISH -> "EN"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            modifier = Modifier.background(SlateCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text(translate("system_default", viewModel), color = Color.White) },
                                onClick = {
                                    viewModel.appLanguage.value = AppLanguage.SYSTEM
                                    showLanguageMenu = false
                                },
                                leadingIcon = {
                                    if (currentLang == AppLanguage.SYSTEM) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TealNeon)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Español", color = Color.White) },
                                onClick = {
                                    viewModel.appLanguage.value = AppLanguage.SPANISH
                                    showLanguageMenu = false
                                },
                                leadingIcon = {
                                    if (currentLang == AppLanguage.SPANISH) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TealNeon)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("English", color = Color.White) },
                                onClick = {
                                    viewModel.appLanguage.value = AppLanguage.ENGLISH
                                    showLanguageMenu = false
                                },
                                leadingIcon = {
                                    if (currentLang == AppLanguage.ENGLISH) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TealNeon)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // DEG/RAD Toggle Switch
                    Button(
                        onClick = { viewModel.onCalcEvent(CalculatorEvent.ToggleAngleMode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRadians) TealNeon.copy(alpha = 0.15f) else SlateKeypad,
                            contentColor = if (isRadians) TealNeon else Color(0xFF938F99)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("angle_mode_toggle")
                    ) {
                        Text(
                            text = if (isRadians) "RAD" else "DEG",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Custom history toggle rendered via stable Canvas Clock face
                    IconButton(
                        onClick = { showHistoryDrawer = !showHistoryDrawer },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Canvas(modifier = Modifier.size(22.dp)) {
                            drawCircle(
                                color = Color(0xFFE6E1E5),
                                radius = 8.dp.toPx(),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawLine(
                                color = Color(0xFFE6E1E5),
                                start = Offset(size.width / 2, size.height / 2),
                                end = Offset(size.width / 2, size.height / 2 - 4.5f.dp.toPx()),
                                strokeWidth = 1.8f.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFFE6E1E5),
                                start = Offset(size.width / 2, size.height / 2),
                                end = Offset(size.width / 2 + 3.5f.dp.toPx(), size.height / 2),
                                strokeWidth = 1.8f.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBg,
                    titleContentColor = Color(0xFFE6E1E5)
                )
            )
        },
        containerColor = SlateDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                ModeSegmentedControl(
                    selectedMode = appMode,
                    viewModel = viewModel,
                    onModeSelected = { viewModel.appMode.value = it }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (appMode) {
                        AppMode.CALCULATOR -> {
                            CalculatorModule(viewModel = viewModel)
                        }
                        AppMode.GRAPHING -> {
                            GraphingModule(viewModel = viewModel)
                        }
                        AppMode.CONVERSIONS -> {
                            ConversionsModule(viewModel = viewModel)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showHistoryDrawer,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .align(Alignment.CenterEnd)
                    .background(SlateCard)
                    .testTag("history_drawer")
            ) {
                HistoryDrawerContent(
                    history = historyLog,
                    viewModel = viewModel,
                    onLoadItem = { item ->
                        viewModel.onCalcEvent(CalculatorEvent.LoadHistoryItem(item))
                        showHistoryDrawer = false
                    },
                    onClearHistory = {
                        viewModel.onCalcEvent(CalculatorEvent.ClearHistory)
                    },
                    onClose = { showHistoryDrawer = false }
                )
            }
        }
    }
}

@Composable
fun ModeSegmentedControl(
    selectedMode: AppMode,
    viewModel: CalculatorViewModel,
    onModeSelected: (AppMode) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1C1B1F)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val modes = listOf(
                AppMode.CALCULATOR to translate("calculator", viewModel),
                AppMode.GRAPHING to translate("graphing", viewModel),
                AppMode.CONVERSIONS to translate("conversions", viewModel)
            )

            modes.forEach { (mode, label) ->
                val active = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onModeSelected(mode) }
                        .testTag("tab_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = label,
                            color = if (active) Color(0xFFD0BCFF) else Color(0xFF938F99),
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        if (active) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(Color(0xFFD0BCFF))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(7.dp)) // keep spacing balanced
                        }
                    }
                }
            }
        }
        Divider(color = Color(0xFF49454F), thickness = 1.dp)
    }
}

@Composable
fun CalculatorModule(viewModel: CalculatorViewModel) {
    val expression by viewModel.calcExpression.collectAsStateWithLifecycle()
    val result by viewModel.calcResult.collectAsStateWithLifecycle()

    var showAdvancedKeys by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .weight(1.3f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                val scroll = rememberScrollState()
                Text(
                    text = expression.ifEmpty { "0" },
                    color = Color(0xFF938F99),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .horizontalScroll(scroll)
                        .testTag("calculator_display_expr")
                )

                Text(
                    text = if (result.isNotEmpty()) "= $result" else "",
                    color = Color(0xFFE6E1E5),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_display_result")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showAdvancedKeys) translate("scientific_functions", viewModel) else translate("basic_functions", viewModel),
                color = Color(0xFF938F99),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            TextButton(
                onClick = { showAdvancedKeys = !showAdvancedKeys },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD0BCFF)),
                modifier = Modifier.testTag("btn_expand_functions")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (showAdvancedKeys) translate("hide", viewModel) else translate("show_more", viewModel), fontSize = 12.sp)
                    Icon(
                        imageVector = if (showAdvancedKeys) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand science keys",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF25232A))
                .padding(16.dp)
        ) {
            CalculatorKeypad(
                showAdvanced = showAdvancedKeys,
                onKeyPress = { k -> viewModel.onCalcEvent(CalculatorEvent.KeyPress(k)) },
                onClear = { viewModel.onCalcEvent(CalculatorEvent.Clear) },
                onBackspace = { viewModel.onCalcEvent(CalculatorEvent.Backspace) },
                onEvaluate = { viewModel.onCalcEvent(CalculatorEvent.Evaluate) }
            )
        }
    }
}

@Composable
fun CalculatorKeypad(
    showAdvanced: Boolean,
    onKeyPress: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: () -> Unit
) {
    if (showAdvanced) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "sin", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("sin(") }
                KeyButton(label = "cos", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("cos(") }
                KeyButton(label = "tan", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("tan(") }
                KeyButton(label = "√", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("√(") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "asin", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("asin(") }
                KeyButton(label = "acos", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("acos(") }
                KeyButton(label = "atan", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("atan(") }
                KeyButton(label = "^", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("^") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "ln", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("ln(") }
                KeyButton(label = "log", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("log(") }
                KeyButton(label = "π", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("π") }
                KeyButton(label = "e^x", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("e^") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "(", keyColor = Color(0xFF49454F), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("(") }
                KeyButton(label = ")", keyColor = Color(0xFF49454F), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress(")") }
                KeyButton(label = "%", keyColor = Color(0xFF49454F), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("%") }
                KeyButton(label = "abs", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("abs(") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "mean", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("mean(") }
                KeyButton(label = "std", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("stddev(") }
                KeyButton(label = ",", keyColor = Color(0xFF49454F), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress(",") }
                KeyButton(label = "x!", keyColor = Color(0xFF49454F), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("!") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "C", keyColor = Color(0xFF49454F), textColor = Color(0xFFEFB8C8), weight = 1f) { onClear() }
                KeyButton(label = "⌫", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onBackspace() }
                KeyButton(label = "/", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("÷") }
                KeyButton(label = "×", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("×") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "7", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("7") }
                KeyButton(label = "8", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("8") }
                KeyButton(label = "9", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("9") }
                KeyButton(label = "−", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("−") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "4", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("4") }
                KeyButton(label = "5", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("5") }
                KeyButton(label = "6", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("6") }
                KeyButton(label = "+", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("+") }
            }
            Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyButton(label = "1", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("1") }
                        KeyButton(label = "2", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("2") }
                        KeyButton(label = "3", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("3") }
                    }
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyButton(label = "0", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 2f) { onKeyPress("0") }
                        KeyButton(label = ".", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress(".") }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    KeyButton(label = "=", keyColor = Color(0xFFD0BCFF), textColor = Color(0xFF381E72), fillHeight = true) { onEvaluate() }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "C", keyColor = Color(0xFF49454F), textColor = Color(0xFFEFB8C8), weight = 1f) { onClear() }
                KeyButton(label = "⌫", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onBackspace() }
                KeyButton(label = "√", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("√(") }
                KeyButton(label = "÷", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("÷") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "7", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("7") }
                KeyButton(label = "8", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("8") }
                KeyButton(label = "9", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("9") }
                KeyButton(label = "×", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("×") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "4", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("4") }
                KeyButton(label = "5", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("5") }
                KeyButton(label = "6", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("6") }
                KeyButton(label = "−", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("−") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "1", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("1") }
                KeyButton(label = "2", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("2") }
                KeyButton(label = "3", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress("3") }
                KeyButton(label = "+", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("+") }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(label = "0", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1.5f) { onKeyPress("0") }
                KeyButton(label = ".", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { onKeyPress(".") }
                KeyButton(label = "^", keyColor = Color(0xFF381E72), textColor = Color(0xFFD0BCFF), weight = 1f) { onKeyPress("^") }
                KeyButton(label = "=", keyColor = Color(0xFFD0BCFF), textColor = Color(0xFF381E72), weight = 1.5f) { onEvaluate() }
            }
        }
    }
}

@Composable
fun RowScope.KeyButton(
    label: String,
    keyColor: Color,
    textColor: Color,
    weight: Float = 1f,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(keyColor)
            .clickable { onClick() }
            .testTag("btn_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun KeyButton(
    label: String,
    keyColor: Color,
    textColor: Color,
    fillHeight: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.height(54.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(keyColor)
            .clickable { onClick() }
            .testTag("btn_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun GraphingModule(viewModel: CalculatorViewModel) {
    val equation by viewModel.graphFunction.collectAsStateWithLifecycle()
    val xMin by viewModel.graphXRangeMin.collectAsStateWithLifecycle()
    val xMax by viewModel.graphXRangeMax.collectAsStateWithLifecycle()
    val yMin by viewModel.graphYRangeMin.collectAsStateWithLifecycle()
    val yMax by viewModel.graphYRangeMax.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.graphSelectedPreset.collectAsStateWithLifecycle()

    var customEqText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.8f)
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, SlateKeypad)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MathGraphCanvas(
                    functionString = equation,
                    xMin = xMin,
                    xMax = xMax,
                    yMin = yMin,
                    yMax = yMax,
                    isRadians = true,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateDarkBg.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "V: x=[${xMin.roundTo(2)}, ${xMax.roundTo(2)}] y=[${yMin.roundTo(2)}, ${yMax.roundTo(2)}]",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Custom Canvas-based glowing action zoom buttons overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.zoomGraph(0.5) },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = SlateKeypad.copy(alpha = 0.9f)),
                        modifier = Modifier.size(36.dp).clip(CircleShape).testTag("graph_btn_zoom_in")
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawLine(
                                color = TealNeon,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.5f.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            drawLine(
                                color = TealNeon,
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, size.height),
                                strokeWidth = 2.5f.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.zoomGraph(2.0) },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = SlateKeypad.copy(alpha = 0.9f)),
                        modifier = Modifier.size(36.dp).clip(CircleShape).testTag("graph_btn_zoom_out")
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawLine(
                                color = TealNeon,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.5f.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.graphXRangeMin.value = -10.0
                            viewModel.graphXRangeMax.value = 10.0
                            viewModel.graphYRangeMin.value = -10.0
                            viewModel.graphYRangeMax.value = 10.0
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = SlateKeypad.copy(alpha = 0.9f)),
                        modifier = Modifier.size(36.dp).clip(CircleShape).testTag("graph_btn_zoom_reset")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Zoom", tint = AmberAccent)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customEqText,
                onValueChange = { customEqText = it },
                placeholder = { Text(translate("graph_placeholder", viewModel), color = Color.Gray, fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("graph_eq_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedBorderColor = TealNeon,
                    unfocusedBorderColor = SlateKeypad,
                    focusedContainerColor = SlateCard,
                    unfocusedContainerColor = SlateCard
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Button(
                onClick = {
                    if (customEqText.isNotBlank()) {
                        viewModel.graphFunction.value = customEqText
                        viewModel.graphSelectedPreset.value = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealNeon, contentColor = SlateDarkBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp).testTag("graph_btn_apply")
            ) {
                Text(translate("graph_btn", viewModel), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        var showWindowSettings by remember { mutableStateOf(false) }
        var tempXMin by remember(xMin) { mutableStateOf(xMin.toString()) }
        var tempXMax by remember(xMax) { mutableStateOf(xMax.toString()) }
        var tempYMin by remember(yMin) { mutableStateOf(yMin.toString()) }
        var tempYMax by remember(yMax) { mutableStateOf(yMax.toString()) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = translate("preset_equations", viewModel),
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            TextButton(
                onClick = { showWindowSettings = !showWindowSettings },
                colors = ButtonDefaults.textButtonColors(contentColor = TealNeon),
                modifier = Modifier.height(30.dp).testTag("btn_toggle_axes")
            ) {
                Icon(
                    imageVector = if (showWindowSettings) Icons.Default.Close else Icons.Default.Settings,
                    contentDescription = translate("adjust_window", viewModel),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showWindowSettings) translate("close", viewModel) else translate("adjust_window", viewModel), fontSize = 11.sp)
            }
        }

        if (showWindowSettings) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateKeypad)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(translate("view_window_limits", viewModel), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = tempXMin,
                            onValueChange = { tempXMin = it },
                            label = { Text("X Min", fontSize = 9.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("input_xmin"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = TealNeon,
                                unfocusedBorderColor = SlateKeypad
                            )
                        )
                        OutlinedTextField(
                            value = tempXMax,
                            onValueChange = { tempXMax = it },
                            label = { Text("X Max", fontSize = 9.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("input_xmax"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = TealNeon,
                                unfocusedBorderColor = SlateKeypad
                            )
                        )
                        OutlinedTextField(
                            value = tempYMin,
                            onValueChange = { tempYMin = it },
                            label = { Text("Y Min", fontSize = 9.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("input_ymin"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = TealNeon,
                                unfocusedBorderColor = SlateKeypad
                            )
                        )
                        OutlinedTextField(
                            value = tempYMax,
                            onValueChange = { tempYMax = it },
                            label = { Text("Y Max", fontSize = 9.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("input_ymax"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = TealNeon,
                                unfocusedBorderColor = SlateKeypad
                            )
                        )

                        Button(
                            onClick = {
                                tempXMin.toDoubleOrNull()?.let { viewModel.graphXRangeMin.value = it }
                                tempXMax.toDoubleOrNull()?.let { viewModel.graphXRangeMax.value = it }
                                tempYMin.toDoubleOrNull()?.let { viewModel.graphYRangeMin.value = it }
                                tempYMax.toDoubleOrNull()?.let { viewModel.graphYRangeMax.value = it }
                                showWindowSettings = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = SlateDarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(40.dp).testTag("btn_apply_axes")
                        ) {
                            Text("OK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(viewModel.graphPresets) { (formula, label) ->
                val active = selectedPreset == formula
                val presetKey = when (label) {
                    "Seno" -> "sine"
                    "Coseno" -> "cosine"
                    "Tangente" -> "tangent"
                    "Parábola" -> "parabola"
                    "Cúbica" -> "cubic"
                    "Raíz Cuadrada" -> "square_root"
                    "Log natural" -> "natural_log"
                    "Valor Absoluto" -> "absolute_value"
                    "Campana Gauss" -> "gaussian_bell"
                    else -> label
                }
                val localizedLabel = translate(presetKey, viewModel)
                Card(
                    onClick = {
                        viewModel.selectGraphPreset(formula)
                        customEqText = formula
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("preset_$formula"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) SlateKeypad else SlateCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = localizedLabel,
                            color = if (active) TealNeon else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = formula,
                            color = if (active) TealNeon.copy(alpha = 0.8f) else Color.Gray,
                            fontSize = 8.sp,
                            maxLines = 1,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MathGraphCanvas(
    functionString: String,
    xMin: Double,
    xMax: Double,
    yMin: Double = -10.0,
    yMax: Double = 10.0,
    isRadians: Boolean = true,
    modifier: Modifier = Modifier
) {
    val evaluator = remember(isRadians) { MathEvaluator(isRadians = isRadians) }
    val cleanedFunction = remember(functionString) {
        functionString
            .trim()
            .replace("y=", "", ignoreCase = true)
            .replace("y =", "", ignoreCase = true)
    }

    Canvas(
        modifier = modifier
            .background(SlateCard)
    ) {
        val width = size.width
        val height = size.height

        val xSpan = xMax - xMin
        val ySpan = yMax - yMin

        val gridColor = Color.Gray.copy(alpha = 0.18f)
        val axisColor = Color.Gray.copy(alpha = 0.6f)

        val yGridStep = 2.0
        var currentY = yMin - (yMin % yGridStep)
        while (currentY <= yMax) {
            if (currentY != 0.0) {
                val pY = height - (((currentY - yMin) / ySpan) * height).toFloat()
                drawLine(
                    color = gridColor,
                    start = Offset(0f, pY),
                    end = Offset(width, pY),
                    strokeWidth = 1f
                )
            }
            currentY += yGridStep
        }

        val xGridStep = xSpan / 10.0
        var currentX = xMin - (xMin % xGridStep)
        while (currentX <= xMax) {
            if (currentX != 0.0) {
                val pX = (((currentX - xMin) / xSpan) * width).toFloat()
                drawLine(
                    color = gridColor,
                    start = Offset(pX, 0f),
                    end = Offset(pX, height),
                    strokeWidth = 1f
                )
            }
            currentX += xGridStep
        }

        val yAxisX = (((0.0 - xMin) / xSpan) * width).toFloat()
        if (yAxisX in 0f..width) {
            drawLine(
                color = axisColor,
                start = Offset(yAxisX, 0f),
                end = Offset(yAxisX, height),
                strokeWidth = 3f
            )
        }

        val xAxisY = height - (((0.0 - yMin) / ySpan) * height).toFloat()
        if (xAxisY in 0f..height) {
            drawLine(
                color = axisColor,
                start = Offset(0f, xAxisY),
                end = Offset(width, xAxisY),
                strokeWidth = 3f
            )
        }

        val plotPath = Path()
        var isPathStarted = false

        val step = 2
        for (px in 0..width.toInt() step step) {
            val cx = xMin + (px.toDouble() / width.toDouble()) * xSpan
            val cy = evaluator.evaluate(cleanedFunction, cx)

            if (cy.isNaN() || cy.isInfinite() || cy > 1000.0 || cy < -1000.0) {
                isPathStarted = false
                continue
            }

            val py = height - (((cy - yMin) / ySpan) * height).toFloat()

            if (!isPathStarted) {
                plotPath.moveTo(px.toFloat(), py)
                isPathStarted = true
            } else {
                plotPath.lineTo(px.toFloat(), py)
            }
        }

        drawPath(
            path = plotPath,
            color = TealNeon,
            style = Stroke(width = 4f)
        )
    }
}

@Composable
fun ConversionsModule(viewModel: CalculatorViewModel) {
    val category by viewModel.convCategory.collectAsStateWithLifecycle()
    val fromUnit by viewModel.convFromUnit.collectAsStateWithLifecycle()
    val toUnit by viewModel.convToUnit.collectAsStateWithLifecycle()
    val valueInput by viewModel.convInput.collectAsStateWithLifecycle()
    val valueOutput by viewModel.convOutput.collectAsStateWithLifecycle()

    var showFromDropdown by remember { mutableStateOf(false) }
    var showToDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                ConversionCategory.LENGTH to translate("LENGTH", viewModel),
                ConversionCategory.MASS to translate("MASS", viewModel),
                ConversionCategory.TEMPERATURE to translate("TEMPERATURE", viewModel),
                ConversionCategory.SPEED to translate("SPEED", viewModel),
                ConversionCategory.AREA to translate("AREA", viewModel),
                ConversionCategory.VOLUME to translate("VOLUME", viewModel)
            )

            categories.forEach { (cat, label) ->
                val active = category == cat
                CustomCategoryChip(
                    label = label,
                    active = active,
                    onClick = { viewModel.convCategory.value = cat },
                    modifier = Modifier.testTag("chip_${cat.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        TextButton(
                            onClick = { showFromDropdown = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = TealNeon),
                            modifier = Modifier.testTag("btn_from_dropdown")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(translate(fromUnit, viewModel), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Units from")
                            }
                        }
                        DropdownMenu(
                            expanded = showFromDropdown,
                            onDismissRequest = { showFromDropdown = false },
                            modifier = Modifier.background(SlateCard)
                        ) {
                            val list = viewModel.unitDefinitions[category] ?: emptyList()
                            list.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(translate(u, viewModel), color = Color.White) },
                                    onClick = {
                                        viewModel.convFromUnit.value = u
                                        showFromDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = valueInput,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("conv_from_value")
                    )
                }

                Divider(
                    color = SlateKeypad,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        TextButton(
                            onClick = { showToDropdown = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = AmberAccent),
                            modifier = Modifier.testTag("btn_to_dropdown")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(translate(toUnit, viewModel), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Units to")
                            }
                        }
                        DropdownMenu(
                            expanded = showToDropdown,
                            onDismissRequest = { showToDropdown = false },
                            modifier = Modifier.background(SlateCard)
                        ) {
                            val list = viewModel.unitDefinitions[category] ?: emptyList()
                            list.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(translate(u, viewModel), color = Color.White) },
                                    onClick = {
                                        viewModel.convToUnit.value = u
                                        showToDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = valueOutput,
                        color = AmberAccent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("conv_to_value")
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF25232A))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyButton(label = "1", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("1") }
                    KeyButton(label = "2", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("2") }
                    KeyButton(label = "3", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("3") }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyButton(label = "4", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("4") }
                    KeyButton(label = "5", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("5") }
                    KeyButton(label = "6", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("6") }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyButton(label = "7", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("7") }
                    KeyButton(label = "8", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("8") }
                    KeyButton(label = "9", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("9") }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyButton(label = "C", keyColor = Color(0xFF49454F), textColor = Color(0xFFEFB8C8), weight = 1f) { viewModel.onConvertClear() }
                    KeyButton(label = "0", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress("0") }
                    KeyButton(label = ".", keyColor = Color(0xFF1C1B1F), textColor = Color(0xFFE6E1E5), weight = 1f) { viewModel.onConvertDigitPress(".") }
                    KeyButton(label = "⌫", keyColor = Color(0xFF49454F), textColor = Color(0xFFD0BCFF), weight = 1f) { viewModel.onConvertBackspace() }
                }
            }
        }
    }
}

@Composable
fun CustomCategoryChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) TealNeon.copy(alpha = 0.15f) else SlateCard)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) TealNeon else Color.LightGray,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun HistoryDrawerContent(
    history: List<Calculation>,
    viewModel: CalculatorViewModel,
    onLoadItem: (Calculation) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = translate("history", viewModel),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = translate("close", viewModel),
                    tint = Color.Gray
                )
            }
        }

        Divider(color = SlateKeypad, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = translate("no_operations_yet", viewModel),
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { calc ->
                    Card(
                        onClick = { onLoadItem(calc) },
                        colors = CardDefaults.cardColors(containerColor = SlateKeypad),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_item_${calc.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = calc.expression,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Left
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "= ${calc.result}",
                                color = TealNeon,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (history.isNotEmpty()) {
            Button(
                onClick = onClearHistory,
                colors = ButtonDefaults.buttonColors(containerColor = SoftRed.copy(alpha = 0.2f), contentColor = SoftRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_clear_history")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = translate("clear_history", viewModel))
                Spacer(modifier = Modifier.width(6.dp))
                Text(translate("clear_all", viewModel), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return (this * multiplier).roundToInt() / multiplier
}

@Composable
fun translate(key: String, viewModel: CalculatorViewModel): String {
    val langState by viewModel.appLanguage.collectAsStateWithLifecycle()
    val langCode = when (langState) {
        AppLanguage.SYSTEM -> {
            val sysLang = java.util.Locale.getDefault().language.lowercase()
            if (sysLang == "es") "es" else "en"
        }
        AppLanguage.SPANISH -> "es"
        AppLanguage.ENGLISH -> "en"
    }
    return Localizer.translate(key, langCode)
}

object Localizer {
    private val es = mapOf(
        "calculator" to "Calculadora",
        "graphing" to "Gráfico",
        "conversions" to "Conversor",
        "scientific_functions" to "Funciones Científicas",
        "basic_functions" to "Funciones Básicas",
        "preset_equations" to "Ecuaciones Predeterminadas:",
        "adjust_window" to "Ajustar Ventana",
        "close" to "Cerrar",
        "view_window_limits" to "Límites de Visualización",
        "sine" to "Seno",
        "cosine" to "Coseno",
        "tangent" to "Tangente",
        "parabola" to "Parábola",
        "cubic" to "Cúbica",
        "square_root" to "Raíz Cuadrada",
        "natural_log" to "Log natural",
        "absolute_value" to "Valor Absoluto",
        "gaussian_bell" to "Campana Gauss",
        "history" to "Historial",
        "clear_history" to "Borrar Historial",
        "no_operations_yet" to "No hay operaciones aún",
        "history_hint" to "El historial se guardará aquí automáticamente.",
        "amount" to "Cantidad a convertir",
        "from" to "De",
        "to" to "A",
        "result" to "Resultado",
        // Categories
        "LENGTH" to "Longitud",
        "MASS" to "Masa",
        "TEMPERATURE" to "Temperatura",
        "SPEED" to "Velocidad",
        "AREA" to "Área",
        "VOLUME" to "Volumen",
        // Units
        "Meters" to "Metros",
        "Kilometers" to "Kilómetros",
        "Centimeters" to "Centímetros",
        "Millimeters" to "Milímetros",
        "Miles" to "Millas",
        "Yards" to "Yardas",
        "Feet" to "Pies",
        "Inches" to "Pulgadas",
        "Kilograms" to "Kilogramos",
        "Grams" to "Gramos",
        "Milligrams" to "Miligramos",
        "Pounds" to "Libras",
        "Ounces" to "Onzas",
        "Celsius" to "Celsius",
        "Fahrenheit" to "Fahrenheit",
        "Kelvin" to "Kelvin",
        "Meters/sec" to "Metros/seg",
        "Kilometers/hour" to "Kilómetros/hora",
        "Miles/hour" to "Millas/hora",
        "Knots" to "Nudos",
        "Square Meters" to "Metros Cuadrados",
        "Square Kilometers" to "Kilómetros Cuadrados",
        "Square Miles" to "Millas Cuadradas",
        "Acres" to "Acres",
        "Hectares" to "Hectáreas",
        "Liters" to "Litros",
        "Milliliters" to "Mililitros",
        "Gallons" to "Galones",
        "Cups" to "Tazas",
        "Cubic Meters" to "Metros Cúbicos",
        "language_label" to "Idioma",
        "system_default" to "Predeterminado",
        "spanish" to "Español",
        "english" to "Inglés",
        "hide" to "Ocultar",
        "show_more" to "Ver más",
        "graph_placeholder" to "Graficar ej: x^2 - x * 3",
        "graph_btn" to "Graficar",
        "clear_all" to "Limpiar Todo"
    )

    private val en = mapOf(
        "calculator" to "Calculator",
        "graphing" to "Graphing",
        "conversions" to "Converter",
        "scientific_functions" to "Scientific Functions",
        "basic_functions" to "Basic Functions",
        "preset_equations" to "Preset Equations:",
        "adjust_window" to "Adjust Window",
        "close" to "Close",
        "view_window_limits" to "View Window Limits",
        "sine" to "Sine",
        "cosine" to "Cosine",
        "tangent" to "Tangent",
        "parabola" to "Parabola",
        "cubic" to "Cubic",
        "square_root" to "Square Root",
        "natural_log" to "Natural Log",
        "absolute_value" to "Absolute Value",
        "gaussian_bell" to "Gaussian Bell",
        "history" to "History",
        "clear_history" to "Clear History",
        "no_operations_yet" to "No operations yet",
        "history_hint" to "History will be saved here automatically.",
        "amount" to "Amount to convert",
        "from" to "From",
        "to" to "To",
        "result" to "Result",
        // Categories
        "LENGTH" to "Length",
        "MASS" to "Mass",
        "TEMPERATURE" to "Temperature",
        "SPEED" to "Speed",
        "AREA" to "Area",
        "VOLUME" to "Volume",
        // Units
        "Meters" to "Meters",
        "Kilometers" to "Kilometers",
        "Centimeters" to "Centimeters",
        "Millimeters" to "Millimeters",
        "Miles" to "Miles",
        "Yards" to "Yards",
        "Feet" to "Feet",
        "Inches" to "Inches",
        "Kilograms" to "Kilograms",
        "Grams" to "Grams",
        "Milligrams" to "Milligrams",
        "Pounds" to "Pounds",
        "Ounces" to "Ounces",
        "Celsius" to "Celsius",
        "Fahrenheit" to "Fahrenheit",
        "Kelvin" to "Kelvin",
        "Meters/sec" to "Meters/sec",
        "Kilometers/hour" to "Kilometers/hour",
        "Miles/hour" to "Miles/hour",
        "Knots" to "Knots",
        "Square Meters" to "Square Meters",
        "Square Kilometers" to "Square Kilometers",
        "Square Miles" to "Square Miles",
        "Acres" to "Acres",
        "Hectares" to "Hectares",
        "Liters" to "Liters",
        "Milliliters" to "Milliliters",
        "Gallons" to "Gallons",
        "Cups" to "Cups",
        "Cubic Meters" to "Cubic Meters",
        "language_label" to "Language",
        "system_default" to "System default",
        "spanish" to "Spanish",
        "english" to "English",
        "hide" to "Hide",
        "show_more" to "Show more",
        "graph_placeholder" to "Graph e.g.: x^2 - x * 3",
        "graph_btn" to "Graph",
        "clear_all" to "Clear All"
    )

    fun translate(key: String, lang: String): String {
        val dict = if (lang == "es") es else en
        return dict[key] ?: key
    }
}

