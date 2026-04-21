package com.h3110w0r1d.phoenix.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun CountdownWarningDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    countdownSeconds: Int = 3,
    dismissLabel: String,
    continueLabel: String,
    continueWithCountdownLabel: @Composable (secondsRemaining: Int) -> String,
    dontRemindLabel: String,
    onCancel: () -> Unit,
    onContinue: (dontRemindAgain: Boolean) -> Unit,
) {
    var remainingSeconds by remember { mutableIntStateOf(countdownSeconds) }
    var dontRemindAgain by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (!visible) {
            remainingSeconds = countdownSeconds
            dontRemindAgain = false
            return@LaunchedEffect
        }
        remainingSeconds = countdownSeconds
        dontRemindAgain = false
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = {
            onCancel()
            onDismissRequest()
        },
        title = title,
        text = {
            Column {
                text()
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = dontRemindAgain,
                                onValueChange = { dontRemindAgain = it },
                                role = Role.Checkbox,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = dontRemindAgain,
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        text = dontRemindLabel,
                        style = typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onCancel()
                    onDismissRequest()
                },
            ) {
                Text(dismissLabel)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onContinue(dontRemindAgain)
                    onDismissRequest()
                },
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = colorScheme.error,
                    ),
                enabled = remainingSeconds == 0,
            ) {
                Text(
                    if (remainingSeconds > 0) {
                        continueWithCountdownLabel(remainingSeconds)
                    } else {
                        continueLabel
                    },
                )
            }
        },
    )
}
