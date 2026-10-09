package com.max.musiccloud.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.musiccloud.ui.theme.AccentCyan
import com.max.musiccloud.ui.theme.AccentPink
import com.max.musiccloud.ui.theme.AccentPurple
import com.max.musiccloud.ui.theme.DarkBg
import com.max.musiccloud.ui.theme.DarkSurface
import com.max.musiccloud.ui.theme.DarkSurfaceVariant
import com.max.musiccloud.ui.theme.SuccessGreen
import com.max.musiccloud.ui.theme.TextPrimary
import com.max.musiccloud.ui.theme.TextSecondary
import com.max.musiccloud.ui.theme.TextTertiary

@Composable
fun ByokSettingsDialog(
    initialClientId: String,
    initialClientSecret: String,
    initialTargetFolder: String = "User Music",
    onSaveKeys: (clientId: String, clientSecret: String, targetFolder: String) -> Unit,
    onResetKeys: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var clientId by remember(initialClientId) { mutableStateOf(initialClientId) }
    var clientSecret by remember(initialClientSecret) { mutableStateOf(initialClientSecret) }
    var targetFolder by remember(initialTargetFolder) { mutableStateOf(initialTargetFolder) }
    var isSecretVisible by remember { mutableStateOf(false) }

    fun clean(text: String): String {
        return text
            .replace("\uFEFF", "")
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\u00A0", "")
            .replace("\u202F", "")
            .replace("\u205F", "")
            .replace("\u3000", "")
            .replace(Regex("\\s+"), "")
            .removePrefix("http://")
            .removePrefix("https://")
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()
    }

    val redirectUri = "http://localhost:8888/callback"
    val hasFullStructure = clientId.matches(Regex("^[0-9]+-[a-z0-9_\\-]+\\.apps\\.googleusercontent\\.com$"))
    val isMissingProjectPrefix = !clientId.contains("-") && clientId.endsWith(".apps.googleusercontent.com")
    val isClientIdValid = hasFullStructure

    val validationMessage = when {
        clientId.isBlank() -> ""
        isMissingProjectPrefix -> "⚠️ Нет номера проекта в начале! Формат: [номер]-[хеш].apps.googleusercontent.com"
        !clientId.endsWith(".apps.googleusercontent.com") -> "⚠️ Должен оканчиваться на .apps.googleusercontent.com"
        hasFullStructure -> "✓ Формат верный (${clientId.length} симв.)"
        else -> "⚠️ Проверьте символы Client ID"
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = DarkBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Настройки ключей (BYOK)",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Введите Web Client ID и Secret из Google Cloud Console (тип Web application).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Web Client ID Field
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = clean(it) },
                    label = { Text("Web Client ID", color = TextSecondary) },
                    placeholder = { Text("...apps.googleusercontent.com", color = TextTertiary) },
                    singleLine = false,
                    maxLines = 3,
                    trailingIcon = {
                        IconButton(onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                clientId = clean(clip)
                                Toast.makeText(context, "Client ID вставлен", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = AccentCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = if (isClientIdValid) SuccessGreen else AccentPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Client ID Validation status
                if (clientId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isClientIdValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isClientIdValid) SuccessGreen else AccentPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = validationMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (isClientIdValid) SuccessGreen else AccentPink
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Client Secret Field
                OutlinedTextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = clean(it) },
                    label = { Text("Client Secret", color = TextSecondary) },
                    placeholder = { Text("GOCSPX-...", color = TextTertiary) },
                    visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    clientSecret = clean(clip)
                                    Toast.makeText(context, "Secret вставлен", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = AccentCyan
                                )
                            }
                            IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                Icon(
                                    imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Target Folder Field
                OutlinedTextField(
                    value = targetFolder,
                    onValueChange = { targetFolder = it },
                    label = { Text("Папка с музыкой на Google Диске") },
                    placeholder = { Text("User Music") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = AccentCyan
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "Сканируется только эта папка и её подпапки (например, User Music). Очистите поле для сканирования всего Диска.",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Info & Redirect URI Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Обязательный Redirect URI в Google Cloud:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = redirectUri,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(redirectUri))
                                Toast.makeText(context, "Redirect URI скопирован!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "В Google Cloud Console откройте этот Web Client -> блок «Authorized redirect URIs» -> нажмите «+ Add URI» -> вставьте этот адрес и нажмите SAVE.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reset keys button
                OutlinedButton(
                    onClick = {
                        onResetKeys()
                        clientId = ""
                        clientSecret = ""
                        Toast.makeText(context, "Ключи очищены", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPink),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Очистить поля", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalId = clean(clientId)
                    val finalSecret = clean(clientSecret)
                    if (finalId.isBlank() || finalSecret.isBlank()) {
                        Toast.makeText(context, "Заполните оба поля ключей!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSaveKeys(finalId, finalSecret, targetFolder.trim())
                    Toast.makeText(context, "Настройки успешно сохранены!", Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}
