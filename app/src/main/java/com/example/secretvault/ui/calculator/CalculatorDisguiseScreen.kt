package com.example.secretvault.ui.calculator

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secretvault.model.VaultMode
import com.example.secretvault.security.SecurityManager
import com.example.secretvault.theme.CalcBg
import com.example.secretvault.theme.CalcDisplayBg
import com.example.secretvault.theme.CalcKeyAction
import com.example.secretvault.theme.CalcKeyEqual
import com.example.secretvault.theme.CalcKeyNumber
import com.example.secretvault.theme.CalcKeyOperator
import com.example.secretvault.theme.VaultBorder
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultSurface
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary

@Composable
fun CalculatorDisguiseScreen(
    securityManager: SecurityManager,
    onVaultUnlocked: (VaultMode) -> Unit
) {
    val context = LocalContext.current
    var isSetupMode by remember { mutableStateOf(!securityManager.isSetupComplete()) }
    var setupStep by remember { mutableStateOf(1) } // 1: Enter PIN, 2: Confirm PIN
    var tempPin by remember { mutableStateOf("") }
    var enteredDigitSequence by remember { mutableStateOf("") }

    // Calculator State
    var displayInput by remember { mutableStateOf("0") }
    var equationDisplay by remember { mutableStateOf("") }
    var currentNumber by remember { mutableStateOf("") }
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var pendingOperator by remember { mutableStateOf<String?>(null) }

    // Recovery Dialog
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var recoveryNewPinInput by remember { mutableStateOf("") }

    fun handleEqualPress() {
        val entered = if (enteredDigitSequence.isNotEmpty()) enteredDigitSequence else currentNumber.ifEmpty { displayInput }
        enteredDigitSequence = ""

        // If in setup mode, handle PIN creation via keypad
        if (isSetupMode) {
            if (setupStep == 1) {
                if (entered.length >= 4 && entered.all { it.isDigit() }) {
                    tempPin = entered
                    setupStep = 2
                    displayInput = "0"
                    currentNumber = ""
                    equationDisplay = "Confirm PIN & Press ="
                    Toast.makeText(context, "Re-enter PIN and press =", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                }
                return
            } else if (setupStep == 2) {
                if (entered == tempPin) {
                    securityManager.setupMasterPin(tempPin, "What is your secret word?", "secret")
                    isSetupMode = false
                    Toast.makeText(context, "Vault Armed! Enter $tempPin= anytime.", Toast.LENGTH_LONG).show()
                    onVaultUnlocked(VaultMode.MASTER)
                } else {
                    Toast.makeText(context, "PINs did not match! Try again.", Toast.LENGTH_SHORT).show()
                    setupStep = 1
                    displayInput = "0"
                    currentNumber = ""
                    equationDisplay = ""
                }
                return
            }
        }

        // Normal mode: First check secret unlock PIN
        val mode = securityManager.verifyPin(entered)
        if (mode != VaultMode.LOCKED) {
            onVaultUnlocked(mode)
            return
        } else if (securityManager.getFailedAttempts() >= 3) {
            Toast.makeText(context, "Incorrect PIN attempt recorded!", Toast.LENGTH_SHORT).show()
        }

        // Otherwise perform normal mathematical computation
        if (firstOperand != null && pendingOperator != null && currentNumber.isNotEmpty()) {
            val second = currentNumber.toDoubleOrNull() ?: 0.0
            val res = when (pendingOperator) {
                "+" -> firstOperand!! + second
                "-" -> firstOperand!! - second
                "×" -> firstOperand!! * second
                "÷" -> if (second != 0.0) firstOperand!! / second else Double.NaN
                "%" -> (firstOperand!! * second) / 100.0
                else -> second
            }

            equationDisplay = "${firstOperand?.let { formatNum(it) }} $pendingOperator ${formatNum(second)} ="
            val formattedResult = if (res.isNaN()) "Error" else formatNum(res)
            displayInput = formattedResult
            currentNumber = formattedResult
            firstOperand = null
            pendingOperator = null
        }
    }

    fun onKeyClick(key: String) {
        when (key) {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> {
                enteredDigitSequence += key
                if (displayInput == "0" || displayInput == "Error") {
                    displayInput = key
                    currentNumber = key
                } else {
                    displayInput += key
                    currentNumber += key
                }
            }
            "." -> {
                if (!currentNumber.contains(".")) {
                    val next = if (currentNumber.isEmpty()) "0." else "$currentNumber."
                    displayInput = if (displayInput == "0") "0." else displayInput + "."
                    currentNumber = next
                }
            }
            "C", "AC" -> {
                enteredDigitSequence = ""
                displayInput = "0"
                currentNumber = ""
                firstOperand = null
                pendingOperator = null
                equationDisplay = if (isSetupMode) {
                    if (setupStep == 1) "Set PIN & Press =" else "Confirm PIN & Press ="
                } else ""
            }
            "DEL" -> {
                if (enteredDigitSequence.isNotEmpty()) enteredDigitSequence = enteredDigitSequence.dropLast(1)
                if (currentNumber.isNotEmpty()) {
                    currentNumber = currentNumber.dropLast(1)
                    displayInput = if (currentNumber.isEmpty()) "0" else currentNumber
                } else if (displayInput.isNotEmpty() && displayInput != "0") {
                    displayInput = displayInput.dropLast(1).ifEmpty { "0" }
                }
            }
            "±" -> {
                if (currentNumber.isNotEmpty() && currentNumber != "0") {
                    if (currentNumber.startsWith("-")) {
                        currentNumber = currentNumber.removePrefix("-")
                    } else {
                        currentNumber = "-$currentNumber"
                    }
                    displayInput = currentNumber
                }
            }
            "+", "-", "×", "÷", "%" -> {
                enteredDigitSequence = ""
                if (currentNumber.isNotEmpty()) {
                    val num = currentNumber.toDoubleOrNull() ?: 0.0
                    firstOperand = num
                    pendingOperator = key
                    equationDisplay = "${formatNum(num)} $key"
                    currentNumber = ""
                } else if (firstOperand != null) {
                    pendingOperator = key
                    equationDisplay = "${formatNum(firstOperand!!)} $key"
                }
            }
            "=" -> {
                handleEqualPress()
            }
        }
    }

    // Fullscreen Calculator Disguise
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalcBg)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSetupMode) "Calculator Setup" else "Calculator",
                color = if (isSetupMode) VaultEmerald else VaultTextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showRecoveryDialog = true }) {
                    Text("Help / PIN", color = VaultEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { showRecoveryDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Forgot PIN",
                        tint = VaultEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Setup Guidance Banner if in setup
        if (isSetupMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(VaultEmerald.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (setupStep == 1) "Enter your 4-8 digit secret PIN and press '='" else "Re-enter the same PIN and press '=' to confirm",
                    color = VaultEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Display Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(CalcDisplayBg)
                .padding(20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (equationDisplay.isNotEmpty()) {
                    Text(
                        text = equationDisplay,
                        color = VaultTextMuted,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = displayInput,
                    color = VaultTextPrimary,
                    fontSize = if (displayInput.length > 9) 32.sp else 48.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad Grid
        val keys = listOf(
            listOf("C", "±", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "DEL", "=")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        val isOperator = key in listOf("÷", "×", "-", "+")
                        val isEqual = key == "="
                        val isAction = key in listOf("C", "±", "%", "DEL")

                        val bgColor = when {
                            isEqual -> CalcKeyEqual
                            isOperator -> CalcKeyOperator
                            isAction -> CalcKeyAction
                            else -> CalcKeyNumber
                        }

                        val textColor = when {
                            isEqual -> Color.Black
                            isOperator -> Color.White
                            else -> VaultTextPrimary
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(bgColor)
                                .clickable { onKeyClick(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "DEL") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete",
                                    tint = textColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    color = textColor,
                                    fontSize = 24.sp,
                                    fontWeight = if (isEqual || isOperator) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Forgot / Recovery Dialog
    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            containerColor = VaultSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = VaultEmerald)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Vault Access & Recovery", color = VaultTextPrimary, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = VaultEmerald.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "💡 Unlock Secret Vault",
                                color = VaultEmerald,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Default Master PIN: 1234\nType 1234 on the keypad and press '=' to enter.",
                                color = VaultTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val mode = securityManager.verifyPin("1234")
                            if (mode != VaultMode.LOCKED) {
                                showRecoveryDialog = false
                                Toast.makeText(context, "Master Vault Unlocked (PIN: 1234)", Toast.LENGTH_SHORT).show()
                                onVaultUnlocked(mode)
                            } else {
                                Toast.makeText(context, "PIN 1234 changed. Use security question below.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                    ) {
                        Text("Quick Unlock Vault (1234)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Reset PIN via Security Question:",
                        color = VaultTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = securityManager.getSecurityQuestion().ifEmpty { "Security Question" },
                        color = VaultEmerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = recoveryAnswerInput,
                        onValueChange = { recoveryAnswerInput = it },
                        label = { Text("Answer (Default: secret)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultEmerald,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recoveryNewPinInput,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 8) recoveryNewPinInput = it },
                        label = { Text("New PIN (e.g. 1234)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultEmerald,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (recoveryNewPinInput.length >= 4 &&
                            securityManager.recoverWithAnswer(recoveryAnswerInput, recoveryNewPinInput)
                        ) {
                            Toast.makeText(context, "PIN reset successfully! Unlocking...", Toast.LENGTH_SHORT).show()
                            showRecoveryDialog = false
                            onVaultUnlocked(VaultMode.MASTER)
                        } else {
                            Toast.makeText(context, "Incorrect answer or PIN too short", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                ) {
                    Text("Reset PIN", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }
}

private fun formatNum(num: Double): String {
    return if (num % 1.0 == 0.0) {
        num.toLong().toString()
    } else {
        "%.6f".format(num).trimEnd('0').trimEnd('.')
    }
}
