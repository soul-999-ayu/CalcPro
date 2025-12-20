package com.devayu.calcpro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

class CalculatorActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private lateinit var scientificPad: GridLayout
    private lateinit var btnExpand: ImageView
    private lateinit var historyLayout: LinearLayout
    private lateinit var tvHistoryList: TextView

    // State variables
    private var currentInput = ""
    private var isDegree = true
    private var isInverse = false // ADDED: Track Inverse state
    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        tvDisplay = findViewById(R.id.tvDisplay)
        scientificPad = findViewById(R.id.scientificPad)
        btnExpand = findViewById(R.id.btnExpand)
        historyLayout = findViewById(R.id.historyLayout)
        tvHistoryList = findViewById(R.id.tvHistoryList)

        findViewById<ImageView>(R.id.btnHistory).setOnClickListener { toggleHistory() }
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener { view -> showMenu(view) }

        btnExpand.setOnClickListener {
            if (scientificPad.visibility == View.GONE) {
                scientificPad.visibility = View.VISIBLE
                btnExpand.rotation = 0f
            } else {
                scientificPad.visibility = View.GONE
                btnExpand.rotation = 180f
            }
        }

        // Initialize Number Buttons
        val numberIds = listOf(R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9)
        numberIds.forEachIndexed { index, id ->
            findViewById<Button>(id).setOnClickListener { appendInput(index.toString()) }
        }

        // Initialize Basic Operations
        findViewById<Button>(R.id.btnAdd).setOnClickListener { appendInput("+") }
        findViewById<Button>(R.id.btnSub).setOnClickListener { appendInput("-") }
        findViewById<Button>(R.id.btnMul).setOnClickListener { appendInput("*") }
        findViewById<Button>(R.id.btnDiv).setOnClickListener { appendInput("/") }
        findViewById<Button>(R.id.btnDot).setOnClickListener { appendInput(".") }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { appendInput("%") }

        findViewById<Button>(R.id.btnPara).setOnClickListener {
            val open = currentInput.count { it == '(' }
            val close = currentInput.count { it == ')' }
            if (open == close || currentInput.endsWith("(")) appendInput("(") else appendInput(")")
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            currentInput = ""
            tvDisplay.text = "0"
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = currentInput.dropLast(1)
                tvDisplay.text = if (currentInput.isEmpty()) "0" else formatForDisplay(currentInput)
            }
        }

        // --- SCIENTIFIC FUNCTIONS (FIXED INV TINT) ---

        // 1. Setup INV Button
        val btnInv = findViewById<Button>(R.id.btnInv)
        val defaultTextColors = btnInv.textColors     // Save original text colors
        val defaultTint = btnInv.backgroundTintList   // Save original Tonal tint

        btnInv.setOnClickListener {
            isInverse = !isInverse
            if (isInverse) {
                // Active: Use the EQUALS button colors (Blue background, Dark text)
                // This matches your app theme perfectly and is highly visible.
                btnInv.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.btn_equals))
                btnInv.setTextColor(getColor(R.color.btn_equals_text))
            } else {
                // Inactive: Restore original Tonal tint and text
                btnInv.backgroundTintList = defaultTint
                btnInv.setTextColor(defaultTextColors)
            }
        }

        // 2. Setup Trig & Log Buttons
        findViewById<Button>(R.id.btnSin).setOnClickListener {
            if (isInverse) appendFunction("asin(") else appendFunction("sin(")
        }
        findViewById<Button>(R.id.btnCos).setOnClickListener {
            if (isInverse) appendFunction("acos(") else appendFunction("cos(")
        }
        findViewById<Button>(R.id.btnTan).setOnClickListener {
            if (isInverse) appendFunction("atan(") else appendFunction("tan(")
        }
        findViewById<Button>(R.id.btnLog).setOnClickListener {
            if (isInverse) appendFunction("10^") else appendFunction("log(")
        }
        findViewById<Button>(R.id.btnLn).setOnClickListener {
            if (isInverse) appendFunction("e^") else appendFunction("ln(")
        }

        // Other Sci Buttons
        findViewById<Button>(R.id.btnRoot).setOnClickListener { appendFunction("√(") }
        findViewById<Button>(R.id.btnPi).setOnClickListener { appendInput("π") }
        findViewById<Button>(R.id.btnE).setOnClickListener { appendInput("e") }
        findViewById<Button>(R.id.btnPower).setOnClickListener { appendInput("^") }
        findViewById<Button>(R.id.btnFact).setOnClickListener { appendInput("!") }

        val btnRad = findViewById<Button>(R.id.btnRad)
        btnRad.setOnClickListener {
            isDegree = !isDegree
            btnRad.text = if (isDegree) "Rad" else "Deg"
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener { processLogic() }
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        currentInput = ""
        tvDisplay.text = "0"
        if (historyLayout.visibility == View.VISIBLE) toggleHistory()
    }

    private fun appendInput(str: String) {
        if (historyLayout.visibility == View.VISIBLE) toggleHistory()
        if (currentInput == "0" && str != ".") currentInput = ""
        currentInput += str
        tvDisplay.text = formatForDisplay(currentInput)
    }

    private fun appendFunction(func: String) {
        if (historyLayout.visibility == View.VISIBLE) toggleHistory()
        if (currentInput == "0") currentInput = ""
        currentInput += func
        tvDisplay.text = formatForDisplay(currentInput)
    }

    private fun formatForDisplay(input: String): String {
        val regex = Regex("(\\d+\\.?\\d*|\\.\\d+)")
        return regex.replace(input) { match ->
            val numStr = match.value
            try {
                if (numStr == "." || numStr.isEmpty()) return@replace numStr
                val parts = numStr.split(".")
                val integerPart = if (parts[0].isNotEmpty()) {
                    NumberFormat.getNumberInstance(Locale.US).format(parts[0].toLong())
                } else ""
                if (numStr.contains(".")) {
                    val fractionPart = if (parts.size > 1) parts[1] else ""
                    if (integerPart.isEmpty()) ".$fractionPart" else "$integerPart.$fractionPart"
                } else integerPart
            } catch (e: Exception) { numStr }
        }
    }

    private fun processLogic() {
        val prefs = getSharedPreferences("VaultPrefs", MODE_PRIVATE)
        val password = prefs.getString("password", "1234")

        if (currentInput == "+0+0") {
            startActivity(Intent(this, SettingsActivity::class.java))
        } else if (currentInput == password) {
            if (intent.getBooleanExtra("UNLOCK_MODE", false)) {
                setResult(RESULT_OK)
                finish()
            } else {
                startActivity(Intent(this, VaultActivity::class.java))
            }
        } else {
            try {
                val result = eval(currentInput)
                val df = DecimalFormat("#.##########", DecimalFormatSymbols(Locale.US))
                val resultStr = df.format(result)

                if (currentInput != resultStr) {
                    addToHistory("${formatForDisplay(currentInput)} = ${formatForDisplay(resultStr)}")
                }
                tvDisplay.text = formatForDisplay(resultStr)
                currentInput = resultStr
            } catch (e: Exception) {
                tvDisplay.text = "Error"
                currentInput = ""
            }
        }
    }

    // ... Helper methods (toggleHistory, addToHistory, etc.) remain the same ...
    private fun toggleHistory() {
        if (historyLayout.visibility == View.VISIBLE) {
            historyLayout.visibility = View.GONE
        } else {
            historyLayout.visibility = View.VISIBLE
            updateHistoryView()
        }
    }
    private fun addToHistory(entry: String) {
        historyList.add(0, entry)
        if (historyList.size > 20) historyList.removeAt(historyList.size - 1)
        saveHistory()
    }
    private fun updateHistoryView() {
        tvHistoryList.text = if (historyList.isEmpty()) "No History" else historyList.joinToString("\n\n")
    }
    private fun saveHistory() {
        val prefs = getSharedPreferences("CalcHistory", MODE_PRIVATE)
        prefs.edit().putStringSet("history", historyList.toSet()).apply()
    }
    private fun loadHistory() {
        val prefs = getSharedPreferences("CalcHistory", MODE_PRIVATE)
        val set = prefs.getStringSet("history", emptySet())
        historyList.clear()
        historyList.addAll(set ?: emptySet())
    }
    private fun showMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Clear history")
        popup.menu.add("Privacy policy")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Clear history" -> {
                    historyList.clear()
                    saveHistory()
                    updateHistoryView()
                    true
                }
                "Privacy policy" -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy")))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // --- UPDATED EVAL ENGINE WITH INVERSE MATH ---
    private fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() { ch = if (++pos < str.length) str[pos].code else -1 }
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) { nextChar(); return true }
                return false
            }
            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if      (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if      (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else if (eat('%'.code)) x %= parseFactor()
                    else return x
                }
            }
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                    while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code || ch == 'π'.code) {
                    // Capture the identifier (function name or constant)
                    while (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code || ch == 'π'.code) nextChar()
                    val func = str.substring(startPos, pos)

                    // FIX: Check if it's a constant FIRST
                    if (func == "π") {
                        x = Math.PI
                    } else if (func == "e") {
                        x = Math.E
                    } else {
                        // If it's a function (sin, cos, etc.), parse the next factor as the argument
                        x = parseFactor()
                        x = when (func) {
                            "sin" -> if (isDegree) sin(Math.toRadians(x)) else sin(x)
                            "cos" -> if (isDegree) cos(Math.toRadians(x)) else cos(x)
                            "tan" -> if (isDegree) tan(Math.toRadians(x)) else tan(x)
                            "asin" -> if (isDegree) Math.toDegrees(asin(x)) else asin(x)
                            "acos" -> if (isDegree) Math.toDegrees(acos(x)) else acos(x)
                            "atan" -> if (isDegree) Math.toDegrees(atan(x)) else atan(x)
                            "log" -> log10(x)
                            "ln" -> ln(x)
                            "√" -> sqrt(x)
                            else -> x
                        }
                    }
                } else {
                    throw RuntimeException("Unknown: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor())

                // Handle Factorial (!)
                if (eat('!'.code)) {
                    var f = 1.0
                    for (i in 1..x.toInt()) f *= i
                    x = f
                }

                if (eat('%'.code)) x /= 100.0
                return x
            }
        }.parse()
    }
}