/*
 * NothingNotes – privacy-first notes for Android
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.real3rr0r303yt.nothingnotes.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.real3rr0r303yt.nothingnotes.BuildConfig
import com.real3rr0r303yt.nothingnotes.R
import com.real3rr0r303yt.nothingnotes.ui.components.CardContainer
import com.real3rr0r303yt.nothingnotes.ui.components.ConfirmDialog
import com.real3rr0r303yt.nothingnotes.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingnotes.ui.components.NothingTopBar
import com.real3rr0r303yt.nothingnotes.ui.components.RedDot
import com.real3rr0r303yt.nothingnotes.ui.components.SectionLabel
import com.real3rr0r303yt.nothingnotes.ui.components.nothingCardStroke
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingBlack
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingGrey
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingGreyDim
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingOutline
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingRed
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingWhite

const val DONATION_URL = "https://buymeacoffee.com/3rr0r303YT"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val context = LocalContext.current
    val encryption by viewModel.encryptionEnabled.collectAsStateWithLifecycle()
    val secureScreen by viewModel.secureScreen.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    var confirmWipe by remember { mutableStateOf(false) }
    var confirmDonate by remember { mutableStateOf(false) }
    val wipedMessage = stringResource(R.string.settings_delete_all_done)
    val noBrowserMessage = stringResource(R.string.donate_no_browser)

    Column(Modifier.fillMaxSize()) {
        NothingTopBar(title = stringResource(R.string.settings_title), onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            // --- Language -------------------------------------------------
            SectionLabel(stringResource(R.string.settings_section_language))
            SettingsCard(Modifier.selectableGroup()) {
                val options = listOf(
                    AppLanguage.SYSTEM to R.string.settings_language_system,
                    AppLanguage.GERMAN to R.string.settings_language_de,
                    AppLanguage.ENGLISH to R.string.settings_language_en,
                )
                options.forEachIndexed { index, (option, label) ->
                    if (index > 0) CardDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == option,
                                role = Role.RadioButton,
                                onClick = { viewModel.setLanguage(option) },
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(label), Modifier.weight(1f), color = NothingWhite)
                        RadioButton(
                            selected = language == option,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NothingRed,
                                unselectedColor = NothingGreyDim,
                            ),
                        )
                    }
                }
            }

            // --- Privacy --------------------------------------------------
            SectionLabel(stringResource(R.string.settings_section_privacy))
            SettingsCard(highlight = true) {
                InfoRow(
                    icon = Icons.Outlined.WifiOff,
                    title = stringResource(R.string.settings_offline_title),
                    text = stringResource(R.string.settings_offline_text),
                    iconTint = NothingRed,
                )
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                SwitchRow(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.settings_encryption_title),
                    text = if (busy) stringResource(R.string.settings_encryption_working)
                    else stringResource(R.string.settings_encryption_text),
                    checked = encryption,
                    enabled = !busy,
                    onCheckedChange = viewModel::setEncryption,
                )
                CardDivider()
                SwitchRow(
                    icon = Icons.Outlined.ScreenLockPortrait,
                    title = stringResource(R.string.settings_secure_title),
                    text = stringResource(R.string.settings_secure_text),
                    checked = secureScreen,
                    enabled = true,
                    onCheckedChange = { viewModel.setSecureScreen(it) },
                )
            }

            // --- Data -----------------------------------------------------
            SectionLabel(stringResource(R.string.settings_section_data))
            SettingsCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) { confirmWipe = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.DeleteForever, null, tint = NothingRed)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_delete_all_title), color = NothingRed)
                        Text(
                            stringResource(R.string.settings_delete_all_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = NothingGrey,
                        )
                    }
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = NothingRed, strokeWidth = 2.dp)
                }
            }

            // --- Support --------------------------------------------------
            SectionLabel(stringResource(R.string.settings_section_support))
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.settings_donate_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = NothingGrey,
                    )
                    OutlinedButton(
                        onClick = { confirmDonate = true },
                        border = BorderStroke(1.dp, NothingOutline),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(Icons.Outlined.Coffee, null, tint = NothingWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_donate_title), color = NothingWhite)
                    }
                }
            }

            // --- About ----------------------------------------------------
            SectionLabel(stringResource(R.string.settings_section_about))
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixText("NOTHINGNOTES", dotSize = 2.5.dp, gap = 1.dp)
                    Spacer(Modifier.width(5.dp))
                    RedDot(size = 4.dp)
                }
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    color = NothingGrey,
                )
                Text(
                    stringResource(R.string.settings_license),
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingGreyDim,
                )
                Text(
                    stringResource(R.string.settings_copyright),
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingGreyDim,
                )
            }
        }
    }

    if (confirmWipe) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_all_title),
            text = stringResource(R.string.dialog_delete_all_text),
            confirmLabel = stringResource(R.string.dialog_delete_all_confirm),
            onConfirm = {
                confirmWipe = false
                viewModel.deleteAllData {
                    Toast.makeText(context.applicationContext, wipedMessage, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { confirmWipe = false },
        )
    }

    if (confirmDonate) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_donate_title),
            text = stringResource(R.string.dialog_donate_text),
            confirmLabel = stringResource(R.string.action_open),
            destructive = false,
            onConfirm = {
                confirmDonate = false
                // The browser does the networking – this app has no INTERNET permission.
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL))
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, noBrowserMessage, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { confirmDonate = false },
        )
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = nothingCardStroke(highlight),
        colors = CardDefaults.cardColors(containerColor = CardContainer),
    ) {
        content()
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(color = NothingOutline, thickness = 1.dp)
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, text: String, iconTint: Color = NothingWhite) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = iconTint)
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = NothingWhite, style = MaterialTheme.typography.titleMedium)
            Text(text, color = NothingGrey, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (checked) NothingRed else NothingGrey)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = NothingWhite)
            Text(text, color = NothingGrey, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NothingWhite,
                checkedTrackColor = NothingRed,
                checkedBorderColor = NothingRed,
                uncheckedThumbColor = NothingGrey,
                uncheckedTrackColor = NothingBlack,
                uncheckedBorderColor = NothingOutline,
            ),
        )
    }
}
