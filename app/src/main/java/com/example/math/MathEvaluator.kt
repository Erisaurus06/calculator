package com.example.math

import kotlin.math.*

class MathEvaluator(private val isRadians: Boolean = true) {

    sealed class Token {
        data class Number(val value: Double) : Token()
        data class Operator(val op: Char) : Token()
        data class Function(val name: String) : Token()
        object ParenthesisLeft : Token()
        object ParenthesisRight : Token()
        object VariableX : Token()
        object ConstantPi : Token()
        object ConstantE : Token()
        object Percent : Token()
        object Factorial : Token()
        object Comma : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val n = expr.length

        while (i < n) {
            val c = expr[i]
            when {
                c.isWhitespace() -> {
                    i++
                }
                c == '+' -> {
                    tokens.add(Token.Operator('+'))
                    i++
                }
                c == '-' || c == '−' -> {
                    tokens.add(Token.Operator('-'))
                    i++
                }
                c == '*' || c == '×' || c == '·' -> {
                    tokens.add(Token.Operator('*'))
                    i++
                }
                c == '/' || c == '÷' -> {
                    tokens.add(Token.Operator('/'))
                    i++
                }
                c == '^' -> {
                    tokens.add(Token.Operator('^'))
                    i++
                }
                c == '%' -> {
                    tokens.add(Token.Percent)
                    i++
                }
                c == '!' -> {
                    tokens.add(Token.Factorial)
                    i++
                }
                c == ',' -> {
                    tokens.add(Token.Comma)
                    i++
                }
                c == '(' -> {
                    tokens.add(Token.ParenthesisLeft)
                    i++
                }
                c == ')' -> {
                    tokens.add(Token.ParenthesisRight)
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < n && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    val numVal = sb.toString().toDoubleOrNull() ?: 0.0
                    tokens.add(Token.Number(numVal))
                }
                c.isLetter() || c == 'π' || c == '√' -> {
                    if (c == 'π') {
                        tokens.add(Token.ConstantPi)
                        i++
                    } else if (c == '√') {
                        tokens.add(Token.Function("sqrt"))
                        i++
                    } else {
                        val sb = StringBuilder()
                        while (i < n && (expr[i].isLetter() || expr[i].isDigit())) {
                            sb.append(expr[i])
                            i++
                        }
                        val str = sb.toString().lowercase()
                        when (str) {
                            "x" -> tokens.add(Token.VariableX)
                            "pi" -> tokens.add(Token.ConstantPi)
                            "e" -> tokens.add(Token.ConstantE)
                            "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt", "exp", "abs", "mean", "stddev", "std" -> {
                                tokens.add(Token.Function(str))
                            }
                            else -> {
                                if (str.startsWith("x")) {
                                    tokens.add(Token.VariableX)
                                    i -= (str.length - 1)
                                } else {
                                    tokens.add(Token.Function(str))
                                }
                            }
                        }
                    }
                }
                else -> {
                    i++
                }
            }
        }

        val processed = mutableListOf<Token>()
        for (idx in 0 until tokens.size) {
            val current = tokens[idx]
            processed.add(current)
            if (idx < tokens.size - 1) {
                val next = tokens[idx + 1]
                val itemTriggersRight = (current is Token.Number || current is Token.VariableX || current is Token.ConstantPi || current is Token.ConstantE || current is Token.ParenthesisRight || current is Token.Percent || current is Token.Factorial)
                val itemTriggersLeft = (next is Token.Number || next is Token.VariableX || next is Token.ConstantPi || next is Token.ConstantE || next is Token.ParenthesisLeft || next is Token.Function)
                
                if (itemTriggersRight && itemTriggersLeft) {
                    processed.add(Token.Operator('*'))
                }
            }
        }

        return processed
    }

    fun evaluate(expression: String, xValue: Double = 0.0): Double {
        val tokens = tokenize(expression)
        if (tokens.isEmpty()) return 0.0
        return try {
            val parser = Parser(tokens, xValue, isRadians)
            val res = parser.parse()
            if (res.isNaN() || res.isInfinite()) Double.NaN else res
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private class Parser(
        private val tokens: List<Token>,
        private val xValue: Double,
        private val isRadians: Boolean
    ) {
        private var index = 0

        private fun peek(): Token? = if (index < tokens.size) tokens[index] else null
        private fun consume(): Token = tokens[index++]

        fun parse(): Double {
            return parseExpr()
        }

        private fun parseExpr(): Double {
            var value = parseTerm()
            while (true) {
                val next = peek()
                if (next is Token.Operator && (next.op == '+' || next.op == '-')) {
                    consume()
                    val right = parseTerm()
                    if (next.op == '+') value += right else value -= right
                } else {
                    break
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (true) {
                val next = peek()
                if (next is Token.Operator && (next.op == '*' || next.op == '/')) {
                    consume()
                    val right = parsePower()
                    if (next.op == '*') {
                        value *= right
                    } else {
                        if (right == 0.0) return Double.NaN
                        value /= right
                    }
                } else {
                    break
                }
            }
            return value
        }

        private fun parsePower(): Double {
            var value = parseFactor()
            val next = peek()
            if (next is Token.Operator && next.op == '^') {
                consume()
                val exponent = parsePower()
                value = value.pow(exponent)
            }
            return value
        }

        private fun parseFactor(): Double {
            var rawValue = 0.0
            val token = peek() ?: return 0.0

            if (token is Token.Operator && (token.op == '+' || token.op == '-')) {
                consume()
                val nested = parseFactor()
                rawValue = if (token.op == '+') nested else -nested
            } else {
                when (token) {
                    is Token.Number -> {
                        consume()
                        rawValue = token.value
                    }
                    is Token.ConstantPi -> {
                        consume()
                        rawValue = Math.PI
                    }
                    is Token.ConstantE -> {
                        consume()
                        rawValue = Math.E
                    }
                    is Token.VariableX -> {
                        consume()
                        rawValue = xValue
                    }
                    is Token.ParenthesisLeft -> {
                        consume()
                        rawValue = parseExpr()
                        if (peek() is Token.ParenthesisRight) {
                            consume()
                        }
                    }
                    is Token.Function -> {
                        consume()
                        val hasParens = peek() is Token.ParenthesisLeft
                        if (hasParens) {
                            consume()
                        }
                        if (token.name == "mean" || token.name == "stddev" || token.name == "std") {
                            val args = mutableListOf<Double>()
                            args.add(parseExpr())
                            while (peek() is Token.Comma) {
                                consume()
                                args.add(parseExpr())
                            }
                            if (hasParens && peek() is Token.ParenthesisRight) {
                                consume()
                            }
                            rawValue = when (token.name) {
                                "mean" -> if (args.isEmpty()) 0.0 else args.average()
                                "stddev", "std" -> {
                                    if (args.size <= 1) 0.0
                                    else {
                                        val avg = args.average()
                                        val sumSq = args.fold(0.0) { sum, element -> sum + (element - avg).pow(2) }
                                        sqrt(sumSq / (args.size - 1))
                                    }
                                }
                                else -> 0.0
                            }
                        } else {
                            val argument = parseExpr()
                            if (hasParens && peek() is Token.ParenthesisRight) {
                                consume()
                            }
                            rawValue = when (token.name) {
                                "sin" -> if (isRadians) sin(argument) else sin(Math.toRadians(argument))
                                "cos" -> if (isRadians) cos(argument) else cos(Math.toRadians(argument))
                                "tan" -> if (isRadians) tan(argument) else tan(Math.toRadians(argument))
                                "asin" -> if (isRadians) asin(argument) else Math.toDegrees(asin(argument))
                                "acos" -> if (isRadians) acos(argument) else Math.toDegrees(acos(argument))
                                "atan" -> if (isRadians) atan(argument) else Math.toDegrees(atan(argument))
                                "ln" -> ln(argument)
                                "log" -> log10(argument)
                                "sqrt" -> if (argument < 0.0) Double.NaN else sqrt(argument)
                                "exp" -> exp(argument)
                                "abs" -> abs(argument)
                                else -> 0.0
                            }
                        }
                    }
                    else -> {
                        consume()
                        rawValue = 0.0
                    }
                }
            }

            while (peek() is Token.Percent || peek() is Token.Factorial) {
                val nextTok = peek()
                consume()
                if (nextTok is Token.Percent) {
                    rawValue /= 100.0
                } else if (nextTok is Token.Factorial) {
                    rawValue = factorial(rawValue)
                }
            }

            return rawValue
        }

        private fun factorial(n: Double): Double {
            if (n < 0.0 || n > 170.0) return Double.NaN
            val num = n.roundToInt()
            if (num < 0) return Double.NaN
            if (num == 0) return 1.0
            var result = 1.0
            for (i in 2..num) {
                result *= i
            }
            return result
        }
    }
}
