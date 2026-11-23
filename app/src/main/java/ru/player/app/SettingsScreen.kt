@file:OptIn(ExperimentalMaterial3Api::class)

package ru.player.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val presets = listOf(
    "Фиолетовый" to 265f,
    "Синий" to 215f,
    "Бирюзовый" to 175f,
    "Зелёный" to 130f,
    "Жёлтый" to 50f,
    "Оранжевый" to 25f,
    "Красный" to 5f,
    "Розовый" to 330f
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) }
    )
}

@Composable
private fun Swatch(hue: Float, selected: Boolean, onClick: () -> Unit) {
    val color = hslColor(hue, 0.6f, 0.5f)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun SettingsScreen(vm: PlayerViewModel, onBack: () -> Unit) {
    val s = vm.settings
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionTitle("Тема")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Системная", "Светлая", "Тёмная").forEachIndexed { i, name ->
                    FilterChip(
                        selected = s.themeMode == i,
                        onClick = { vm.updateSettings(s.copy(themeMode = i)) },
                        label = { Text(name) }
                    )
                }
            }

            SectionTitle("Цвета")
            SettingSwitch(
                "Цвета из обоев",
                "Material You: палитра подстраивается под обои телефона",
                s.dynamicColor
            ) { vm.updateSettings(s.copy(dynamicColor = it)) }

            if (!s.dynamicColor) {
                Spacer(Modifier.height(8.dp))
                Text("Цвет акцента", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    presets.forEach { (_, h) ->
                        Swatch(h, selected = abs(s.hue - h) < 1f) {
                            vm.updateSettings(s.copy(hue = h))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Свой оттенок", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp))
                Slider(
                    value = s.hue,
                    onValueChange = { vm.updateSettings(s.copy(hue = it)) },
                    valueRange = 0f..360f,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            SettingSwitch(
                "Чёрный фон (AMOLED)",
                "Полностью чёрный фон в тёмной теме",
                s.amoled
            ) { vm.updateSettings(s.copy(amoled = it)) }

            SectionTitle("О приложении")
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("Плеер") },
                supportingContent = { Text("Версия 1.0") }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
