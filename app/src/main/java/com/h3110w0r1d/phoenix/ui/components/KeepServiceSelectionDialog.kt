package com.h3110w0r1d.phoenix.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.h3110w0r1d.phoenix.R

@Composable
fun KeepServiceSelectionDialog(
    allServices: List<String>,
    selectedServices: List<String>,
    onDismissRequest: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val selectedState =
        remember(allServices, selectedServices) {
            mutableStateListOf<String>().apply {
                addAll(selectedServices.filter { it in allServices })
            }
        }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.keep_service_select_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (allServices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.keep_service_empty),
                        style = typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.keep_service_select_description),
                        style = typography.bodySmall,
                    )
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(allServices) { serviceName ->
                            val checked = serviceName in selectedState
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (checked) {
                                                selectedState.remove(serviceName)
                                            } else {
                                                selectedState.add(serviceName)
                                            }
                                        }.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            if (serviceName !in selectedState) {
                                                selectedState.add(serviceName)
                                            }
                                        } else {
                                            selectedState.remove(serviceName)
                                        }
                                    },
                                )
                                Text(
                                    text = serviceName,
                                    style = typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selectedState.toList())
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
    )
}
