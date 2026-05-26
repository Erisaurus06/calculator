package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Calculation
import com.example.data.CalculationRepository
import com.example.math.MathEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class AppMode {
    CALCULATOR,
    GRAPHING,
    CONVERSIONS
}

enum class AppLanguage {
    SYSTEM,
    SPANISH,
    ENGLISH
}

enum class ConversionCategory {
    LENGTH,
    MASS,
    TEMPERATURE,
    SPEED,
    AREA,
    VOLUME
}

class CalculatorViewModel(private val repository: CalculationRepository) : ViewModel() {

    // --- History Flow ---
    val historyState: StateFlow<List<Calculation>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Common Settings ---
    var appMode = MutableStateFlow(AppMode.CALCULATOR)
    var isRadians = MutableStateFlow(true)
    var appLanguage = MutableStateFlow(AppLanguage.SYSTEM)

    // --- Calculator States ---
    var calcExpression = MutableStateFlow("")
    var calcResult = MutableStateFlow("")
    var isCalculated = MutableStateFlow(false)

    // Decimal formatter for clean outputs
    private val decimalFormat = DecimalFormat("0.########", DecimalFormatSymbols(Locale.US))

    fun onCalcEvent(event: CalculatorEvent) {
        when (event) {
            is CalculatorEvent.KeyPress -> {
                if (isCalculated.value) {
                    // Start fresh if calculated, or append operator
                    val char = event.key
                    val isOperator = char in listOf("+", "-", "×", "÷", "^", "%")
                    if (isOperator) {
                        calcExpression.value = calcResult.value + char
                    } else {
                        calcExpression.value = char
                    }
                    isCalculated.value = false
                    calcResult.value = ""
                } else {
                    calcExpression.value += event.key
                }
            }
            CalculatorEvent.Backspace -> {
                val expr = calcExpression.value
                if (expr.isNotEmpty()) {
                    calcExpression.value = expr.dropLast(1)
                }
                isCalculated.value = false
                calcResult.value = ""
            }
            CalculatorEvent.Clear -> {
                calcExpression.value = ""
                calcResult.value = ""
                isCalculated.value = false
            }
            CalculatorEvent.Evaluate -> {
                val expr = calcExpression.value
                if (expr.isNotBlank()) {
                    viewModelScope.launch {
                        val evaluator = MathEvaluator(isRadians.value)
                        val evalResult = evaluator.evaluate(expr)
                        if (evalResult.isNaN()) {
                            calcResult.value = "Error"
                        } else {
                            val formatted = formatResult(evalResult)
                            calcResult.value = formatted
                            isCalculated.value = true
                            
                            // Save to database background thread
                            repository.insert(
                                Calculation(
                                    expression = expr,
                                    result = formatted
                                )
                            )
                        }
                    }
                }
            }
            is CalculatorEvent.LoadHistoryItem -> {
                calcExpression.value = event.calc.expression
                calcResult.value = event.calc.result
                isCalculated.value = true
                appMode.value = AppMode.CALCULATOR
            }
            CalculatorEvent.ClearHistory -> {
                viewModelScope.launch {
                    repository.clearAll()
                }
            }
            is CalculatorEvent.ToggleAngleMode -> {
                isRadians.value = !isRadians.value
                // Re-evaluate if there is an expression
                if (isCalculated.value && calcExpression.value.isNotEmpty()) {
                    viewModelScope.launch {
                        val evaluator = MathEvaluator(isRadians.value)
                        val evalResult = evaluator.evaluate(calcExpression.value)
                        calcResult.value = if (evalResult.isNaN()) "Error" else formatResult(evalResult)
                    }
                }
            }
        }
    }

    private fun formatResult(num: Double): String {
        return if (num % 1.0 == 0.0) {
            if (num >= 1e12 || num <= -1e12) {
                num.toString()
            } else {
                num.toLong().toString()
            }
        } else {
            decimalFormat.format(num)
        }
    }

    // --- Graphing States ---
    var graphFunction = MutableStateFlow("sin(x)")
    var graphXRangeMin = MutableStateFlow(-10.0)
    var graphXRangeMax = MutableStateFlow(10.0)
    var graphYRangeMin = MutableStateFlow(-10.0)
    var graphYRangeMax = MutableStateFlow(10.0)
    var graphSelectedPreset = MutableStateFlow("sin(x)")

    val graphPresets = listOf(
        "sin(x)" to "Seno",
        "cos(x)" to "Coseno",
        "tan(x)" to "Tangente",
        "x^2 - 4" to "Parábola",
        "x^3 - 3*x" to "Cúbica",
        "sqrt(x)" to "Raíz Cuadrada",
        "ln(x)" to "Log natural",
        "abs(x)" to "Valor Absoluto",
        "exp(-x^2)" to "Campana Gauss"
    )

    fun selectGraphPreset(equation: String) {
        graphSelectedPreset.value = equation
        graphFunction.value = equation
    }

    fun zoomGraph(factor: Double) {
        val currentMin = graphXRangeMin.value
        val currentMax = graphXRangeMax.value
        val middle = (currentMin + currentMax) / 2.0
        val halfSpan = (currentMax - currentMin) / 2.0
        val newHalfSpan = halfSpan * factor

        // Prevent extreme zoom levels
        if (newHalfSpan in 0.5..1000.0) {
            graphXRangeMin.value = middle - newHalfSpan
            graphXRangeMax.value = middle + newHalfSpan
        }

        val currentYMin = graphYRangeMin.value
        val currentYMax = graphYRangeMax.value
        val middleY = (currentYMin + currentYMax) / 2.0
        val halfSpanY = (currentYMax - currentYMin) / 2.0
        val newHalfSpanY = halfSpanY * factor

        if (newHalfSpanY in 0.5..1000.0) {
            graphYRangeMin.value = middleY - newHalfSpanY
            graphYRangeMax.value = middleY + newHalfSpanY
        }
    }

    // --- Conversion States ---
    var convCategory = MutableStateFlow(ConversionCategory.LENGTH)
    var convInput = MutableStateFlow("0")
    var convFromUnit = MutableStateFlow("Meters")
    var convToUnit = MutableStateFlow("Kilometers")
    var convOutput = MutableStateFlow("0")

    val unitDefinitions = mapOf(
        ConversionCategory.LENGTH to listOf("Meters", "Kilometers", "Centimeters", "Millimeters", "Miles", "Yards", "Feet", "Inches"),
        ConversionCategory.MASS to listOf("Kilograms", "Grams", "Milligrams", "Pounds", "Ounces"),
        ConversionCategory.TEMPERATURE to listOf("Celsius", "Fahrenheit", "Kelvin"),
        ConversionCategory.SPEED to listOf("Meters/sec", "Kilometers/hour", "Miles/hour", "Knots"),
        ConversionCategory.AREA to listOf("Square Meters", "Square Kilometers", "Square Miles", "Acres", "Hectares"),
        ConversionCategory.VOLUME to listOf("Liters", "Milliliters", "Gallons", "Cups", "Cubic Meters")
    )

    init {
        // Re-calculate conversion when inputs change
        viewModelScope.launch {
            launch {
                convCategory.collect { category ->
                    val units = unitDefinitions[category] ?: emptyList()
                    if (units.isNotEmpty()) {
                        convFromUnit.value = units[0]
                        convToUnit.value = if (units.size > 1) units[1] else units[0]
                    }
                    performConversion()
                }
            }
            launch {
                convInput.collect { performConversion() }
            }
            launch {
                convFromUnit.collect { performConversion() }
            }
            launch {
                convToUnit.collect { performConversion() }
            }
        }
    }

    fun onConvertDigitPress(digit: String) {
        val current = convInput.value
        if (current == "0") {
            convInput.value = digit
        } else {
            convInput.value += digit
        }
    }

    fun onConvertBackspace() {
        val current = convInput.value
        if (current.isNotEmpty()) {
            convInput.value = if (current.length == 1) "0" else current.dropLast(1)
        }
    }

    fun onConvertClear() {
        convInput.value = "0"
    }

    private fun performConversion() {
        val value = convInput.value.toDoubleOrNull() ?: 0.0
        val from = convFromUnit.value
        val to = convToUnit.value
        val category = convCategory.value

        val convertedValue = when (category) {
            ConversionCategory.LENGTH -> convertLength(value, from, to)
            ConversionCategory.MASS -> convertMass(value, from, to)
            ConversionCategory.TEMPERATURE -> convertTemperature(value, from, to)
            ConversionCategory.SPEED -> convertSpeed(value, from, to)
            ConversionCategory.AREA -> convertArea(value, from, to)
            ConversionCategory.VOLUME -> convertVolume(value, from, to)
        }

        convOutput.value = formatResult(convertedValue)
    }

    // High fidelity converter engines
    private fun convertLength(value: Double, from: String, to: String): Double {
        // Normalize to meters
        val toMeters = when (from) {
            "Meters" -> 1.0
            "Kilometers" -> 1000.0
            "Centimeters" -> 0.01
            "Millimeters" -> 0.001
            "Miles" -> 1609.344
            "Yards" -> 0.9144
            "Feet" -> 0.3048
            "Inches" -> 0.0254
            else -> 1.0
        }
        val meters = value * toMeters

        val fromMeters = when (to) {
            "Meters" -> 1.0
            "Kilometers" -> 0.001
            "Centimeters" -> 100.0
            "Millimeters" -> 1000.0
            "Miles" -> 1.0 / 1609.344
            "Yards" -> 1.0 / 0.9144
            "Feet" -> 1.0 / 0.3048
            "Inches" -> 1.0 / 0.0254
            else -> 1.0
        }
        return meters * fromMeters
    }

    private fun convertMass(value: Double, from: String, to: String): Double {
        // Normalize to grams
        val toGrams = when (from) {
            "Kilograms" -> 1000.0
            "Grams" -> 1.0
            "Milligrams" -> 0.001
            "Pounds" -> 453.59237
            "Ounces" -> 28.349523
            else -> 1.0
        }
        val grams = value * toGrams

        val fromGrams = when (to) {
            "Kilograms" -> 0.001
            "Grams" -> 1.0
            "Milligrams" -> 1000.0
            "Pounds" -> 1.0 / 453.59237
            "Ounces" -> 1.0 / 28.349523
            else -> 1.0
        }
        return grams * fromGrams
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        // Normalize to Celsius
        val celsius = when (from) {
            "Celsius" -> value
            "Fahrenheit" -> (value - 32.0) * 5.0 / 9.0
            "Kelvin" -> value - 273.15
            else -> value
        }

        return when (to) {
            "Celsius" -> celsius
            "Fahrenheit" -> (celsius * 9.0 / 5.0) + 32.0
            "Kelvin" -> celsius + 273.15
            else -> celsius
        }
    }

    private fun convertSpeed(value: Double, from: String, to: String): Double {
        // Normalize to m/s
        val toMps = when (from) {
            "Meters/sec" -> 1.0
            "Kilometers/hour" -> 1.0 / 3.6
            "Miles/hour" -> 0.44704
            "Knots" -> 0.514444
            else -> 1.0
        }
        val mps = value * toMps

        val fromMps = when (to) {
            "Meters/sec" -> 1.0
            "Kilometers/hour" -> 3.6
            "Miles/hour" -> 1.0 / 0.44704
            "Knots" -> 1.0 / 0.514444
            else -> 1.0
        }
        return mps * fromMps
    }

    private fun convertArea(value: Double, from: String, to: String): Double {
        // Normalize to sq meters
        val toSqM = when (from) {
            "Square Meters" -> 1.0
            "Square Kilometers" -> 1_000_000.0
            "Square Miles" -> 2_589_988.11
            "Acres" -> 4046.856
            "Hectares" -> 10000.0
            else -> 1.0
        }
        val sqM = value * toSqM

        val fromSqM = when (to) {
            "Square Meters" -> 1.0
            "Square Kilometers" -> 1.0 / 1_000_000.0
            "Square Miles" -> 1.0 / 2_589_988.11
            "Acres" -> 1.0 / 4046.856
            "Hectares" -> 1.0 / 10000.0
            else -> 1.0
        }
        return sqM * fromSqM
    }

    private fun convertVolume(value: Double, from: String, to: String): Double {
        // Normalize to liters
        val toLiters = when (from) {
            "Liters" -> 1.0
            "Milliliters" -> 0.001
            "Gallons" -> 3.78541
            "Cups" -> 0.236588
            "Cubic Meters" -> 1000.0
            else -> 1.0
        }
        val liters = value * toLiters

        val fromLiters = when (to) {
            "Liters" -> 1.0
            "Milliliters" -> 1000.0
            "Gallons" -> 1.0 / 3.78541
            "Cups" -> 1.0 / 0.236588
            "Cubic Meters" -> 0.001
            else -> 1.0
        }
        return liters * fromLiters
    }
}

// Sealed class for UI events related to the Calculator screen
sealed class CalculatorEvent {
    data class KeyPress(val key: String) : CalculatorEvent()
    object Backspace : CalculatorEvent()
    object Clear : CalculatorEvent()
    object Evaluate : CalculatorEvent()
    data class LoadHistoryItem(val calc: Calculation) : CalculatorEvent()
    object ClearHistory : CalculatorEvent()
    object ToggleAngleMode : CalculatorEvent()
}

class CalculatorViewModelFactory(private val repository: CalculationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
