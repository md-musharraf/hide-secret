package com.example.secretvault.applock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secretvault.model.VaultMode
import com.example.secretvault.security.SecurityManager
import com.example.secretvault.theme.CalcKeyNumber
import com.example.secretvault.theme.SecretVaultTheme
import com.example.secretvault.theme.VaultAmber
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.theme.VaultBorder
import com.example.secretvault.theme.VaultCardBackground
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultSurface
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary

class AppLockOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_target_package"
        const val EXTRA_FAKE_CRASH = "extra_fake_crash"

        fun start(context: Context, targetPackage: String, isFakeCrash: Boolean) {
            val intent = Intent(context, AppLockOverlayActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE, targetPackage)
                putExtra(EXTRA_FAKE_CRASH, isFakeCrash)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(intent)
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHomeScreen()
            }
        })

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val isFakeCrash = intent.getBooleanExtra(EXTRA_FAKE_CRASH, false)

        val pm = packageManager
        val appName = try {
            val info = pm.getApplicationInfo(targetPackage, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            targetPackage
        }

        val securityManager = SecurityManager.getInstance(this)
        val appLockManager = AppLockManager.getInstance(this)

        setContent {
            SecretVaultTheme {
                var showPinScreen by remember { mutableStateOf(!isFakeCrash) }
                var enteredPin by remember { mutableStateOf("") }
                var isError by remember { mutableStateOf(false) }

                if (!showPinScreen) {
                    // Authentic Fake Crash Screen Dialog
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VaultSurface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = VaultAmber,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { showPinScreen = true } // Secret tap
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "$appName has stopped",
                                        color = VaultTextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Unfortunately, $appName has encountered an error and stopped working.",
                                    color = VaultTextSecondary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { showPinScreen = true } // Secret button to unlock
                                    ) {
                                        Text("Report", color = VaultTextMuted, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { goToHomeScreen() },
                                        colors = ButtonDefaults.buttonColors(containerColor = VaultAmber)
                                    ) {
                                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Sleek PIN Lock Screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VaultBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Header
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 40.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(VaultEmerald.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = VaultEmerald,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = appName,
                                    color = VaultTextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isError) "Incorrect PIN! Try again." else "Enter Vault PIN to Unlock",
                                    color = if (isError) VaultRed else VaultTextSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // PIN dots display
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val count = enteredPin.length.coerceAtMost(8)
                                    for (i in 0 until 4.coerceAtLeast(count)) {
                                        val isFilled = i < count
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isFilled) (if (isError) VaultRed else VaultEmerald)
                                                    else VaultBorder
                                                )
                                        )
                                    }
                                }
                            }

                            // Keypad
                            val keypadKeys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("C", "0", "DEL")
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                keypadKeys.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        row.forEach { key ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(64.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(VaultCardBackground)
                                                    .clickable {
                                                        when (key) {
                                                            "C" -> {
                                                                enteredPin = ""
                                                                isError = false
                                                            }
                                                            "DEL" -> {
                                                                if (enteredPin.isNotEmpty()) {
                                                                    enteredPin = enteredPin.dropLast(1)
                                                                    isError = false
                                                                }
                                                            }
                                                            else -> {
                                                                isError = false
                                                                if (enteredPin.length < 8) {
                                                                    enteredPin += key
                                                                }
                                                                // Check PIN
                                                                if (enteredPin.length >= 4) {
                                                                    val mode = securityManager.verifyPin(enteredPin)
                                                                    if (mode != VaultMode.LOCKED) {
                                                                        appLockManager.unlockForSession(targetPackage)
                                                                        finish()
                                                                    } else if (enteredPin.length >= 4) {
                                                                        // Check on 4 digits or let user continue
                                                                        // If failed on 4 digits, check if longer PIN
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (key == "DEL") {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                        contentDescription = "Delete",
                                                        tint = VaultTextSecondary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = key,
                                                        color = VaultTextPrimary,
                                                        fontSize = 22.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Unlock Button for custom length PINs
                                Button(
                                    onClick = {
                                        val mode = securityManager.verifyPin(enteredPin)
                                        if (mode != VaultMode.LOCKED) {
                                            appLockManager.unlockForSession(targetPackage)
                                            finish()
                                        } else {
                                            isError = true
                                            enteredPin = ""
                                            Toast.makeText(this@AppLockOverlayActivity, "Invalid PIN", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                                ) {
                                    Text("Unlock $appName", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
