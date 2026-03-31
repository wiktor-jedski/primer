package com.example.primer.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val githubRepo by viewModel.githubRepo.collectAsStateWithLifecycle()
    val githubBranch by viewModel.githubBranch.collectAsStateWithLifecycle()
    val pat by viewModel.pat.collectAsStateWithLifecycle()
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    val affirmation by viewModel.affirmation.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val strategies by viewModel.strategies.collectAsStateWithLifecycle()
    val notificationHour by viewModel.notificationHour.collectAsStateWithLifecycle()
    val notificationMinute by viewModel.notificationMinute.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var newMarkerText by remember { mutableStateOf("") }
    var newHabitText by remember { mutableStateOf("") }
    var newStrategyText by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "GitHub",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = githubRepo,
                    onValueChange = { viewModel.saveGithubRepo(it) },
                    label = { Text("Repository (owner/repo)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("github_repo_field")
                )
            }
            item {
                OutlinedTextField(
                    value = githubBranch,
                    onValueChange = { viewModel.saveGithubBranch(it) },
                    label = { Text("Branch") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("github_branch_field")
                )
            }
            item {
                OutlinedTextField(
                    value = pat,
                    onValueChange = { viewModel.savePat(it) },
                    label = { Text("Personal Access Token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pat_field")
                )
            }

            item {
                Text(
                    text = "Journal Markers",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            itemsIndexed(markers, key = { i, _ -> "marker_$i" }) { index, marker ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = marker, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.moveMarkerUp(index) },
                        modifier = Modifier.testTag("marker_up_$index")
                    ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                    IconButton(
                        onClick = { viewModel.moveMarkerDown(index) },
                        modifier = Modifier.testTag("marker_down_$index")
                    ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                    IconButton(
                        onClick = { viewModel.removeMarker(index) },
                        modifier = Modifier.testTag("marker_delete_$index")
                    ) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMarkerText,
                        onValueChange = { newMarkerText = it },
                        label = { Text("New marker") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_marker_input")
                    )
                    IconButton(
                        onClick = {
                            if (newMarkerText.isNotBlank()) {
                                viewModel.addMarker(newMarkerText)
                                newMarkerText = ""
                            }
                        },
                        modifier = Modifier.testTag("add_marker_confirm")
                    ) { Icon(Icons.Default.Check, contentDescription = "Add marker") }
                }
            }

            item {
                Text(
                    text = "Affirmation",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = affirmation,
                    onValueChange = { viewModel.saveAffirmation(it) },
                    label = { Text("Affirmation") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("affirmation_field")
                )
            }

            item {
                Text(
                    text = "Daily Habits",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            itemsIndexed(habits, key = { i, _ -> "habit_$i" }) { index, habit ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = habit, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.moveHabitUp(index) },
                        modifier = Modifier.testTag("habit_up_$index")
                    ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                    IconButton(
                        onClick = { viewModel.moveHabitDown(index) },
                        modifier = Modifier.testTag("habit_down_$index")
                    ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                    IconButton(
                        onClick = { viewModel.removeHabit(index) },
                        modifier = Modifier.testTag("habit_delete_$index")
                    ) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newHabitText,
                        onValueChange = { newHabitText = it },
                        label = { Text("New habit") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_habit_input")
                    )
                    IconButton(
                        onClick = {
                            if (newHabitText.isNotBlank()) {
                                viewModel.addHabit(newHabitText)
                                newHabitText = ""
                            }
                        },
                        modifier = Modifier.testTag("add_habit_confirm")
                    ) { Icon(Icons.Default.Check, contentDescription = "Add habit") }
                }
            }

            item {
                Text(
                    text = "Strategies",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            itemsIndexed(strategies, key = { i, _ -> "strategy_$i" }) { index, strategy ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strategy, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.removeStrategy(index) },
                        modifier = Modifier.testTag("strategy_delete_$index")
                    ) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newStrategyText,
                        onValueChange = { newStrategyText = it },
                        label = { Text("New strategy") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_strategy_input")
                    )
                    IconButton(
                        onClick = {
                            if (newStrategyText.isNotBlank()) {
                                viewModel.addStrategy(newStrategyText)
                                newStrategyText = ""
                            }
                        },
                        modifier = Modifier.testTag("add_strategy_confirm")
                    ) { Icon(Icons.Default.Check, contentDescription = "Add strategy") }
                }
            }

            item {
                Text(
                    text = "Notification",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%02d:%02d", notificationHour, notificationMinute),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notification_time")
                    )
                    Button(onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> viewModel.saveNotificationTime(hour, minute) },
                            notificationHour,
                            notificationMinute,
                            true
                        ).show()
                    }) {
                        Text("Change")
                    }
                }
            }
        }
    }
}
