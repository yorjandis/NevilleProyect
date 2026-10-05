package com.ypg.neville.feature.ai.ui

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.feature.ai.domain.AiAuthor
import com.ypg.neville.feature.ai.domain.AiTextAction

/**
 * Shared entry point used by quote and note context menus.
 *
 * Interpretation and practical-application requests deliberately pause at the
 * author selector so the user explicitly chooses the knowledge framework.
 */
@Composable
fun AiActionsMenuItem(
    onDismissParent: () -> Unit,
    onTextAction: (AiTextAction, AiAuthor) -> Unit,
    onChat: () -> Unit,
    includeChat: Boolean = true
) {
    var submenuExpanded by remember { mutableStateOf(false) }
    var pendingAuthorAction by remember { mutableStateOf<AiTextAction?>(null) }

    Box {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.ai_functions)) },
            leadingIcon = {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF7B1FA2)
                )
            },
            trailingIcon = { Text("›") },
            onClick = { submenuExpanded = true }
        )
        DropdownMenu(
            expanded = submenuExpanded,
            onDismissRequest = { submenuExpanded = false }
        ) {
            listOf(
                AiTextAction.INTERPRET,
                AiTextAction.CONCRETE_PRACTICE
            ).forEach { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(action.labelResource())) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF7B1FA2)
                        )
                    },
                    onClick = {
                        submenuExpanded = false
                        if (action.requiresAuthor) {
                            pendingAuthorAction = action
                        } else {
                            onDismissParent()
                            onTextAction(action, AiAuthor.NEVILLE)
                        }
                    }
                )
            }
            if (includeChat) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ai_chat_with_ai)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF7B1FA2)
                        )
                    },
                    onClick = {
                        submenuExpanded = false
                        onDismissParent()
                        onChat()
                    }
                )
            }
        }
    }

    pendingAuthorAction?.let { action ->
        AiAuthorSelectionDialog(
            action = action,
            onDismiss = { pendingAuthorAction = null },
            onAuthorSelected = { author ->
                pendingAuthorAction = null
                onDismissParent()
                onTextAction(action, author)
            }
        )
    }
}

@Composable
private fun AiAuthorSelectionDialog(
    action: AiTextAction,
    onDismiss: () -> Unit,
    onAuthorSelected: (AiAuthor) -> Unit
) {
    var selectedAuthor by remember(action) { mutableStateOf(AiAuthor.NEVILLE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ai_choose_knowledge_author)) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    stringResource(R.string.ai_choose_knowledge_author_explanation),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AiAuthor.entries.forEach { author ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAuthor = author }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAuthor == author,
                            onClick = { selectedAuthor = author }
                        )
                        Text(author.displayName)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = { onAuthorSelected(selectedAuthor) }) {
                Text(stringResource(R.string.ai_continue))
            }
        }
    )
}

@Composable
fun AiTextAction.localizedLabel(): String = stringResource(labelResource())

private fun AiTextAction.labelResource(): Int = when (this) {
    AiTextAction.KEY_POINTS -> R.string.ai_action_key_points
    AiTextAction.SUMMARY -> R.string.ai_action_summary
    AiTextAction.PRACTICES -> R.string.ai_action_practices
    AiTextAction.CONCRETE_PRACTICE -> R.string.ai_action_practical_application
    AiTextAction.INTERPRET -> R.string.ai_action_interpret
}

object AiNavigation {
    fun openChat(context: Context, text: String) {
        val args = Bundle().apply {
            putString(FragAiChat.ARG_PREFILL, text.trim())
        }
        MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_ai_chat, args)
    }

    fun openTextTool(
        context: Context,
        title: String,
        text: String,
        action: AiTextAction,
        author: AiAuthor
    ) {
        val args = Bundle().apply {
            putString(
                FragAiTextTool.ARG_TITLE,
                title.ifBlank { context.getString(R.string.ai_selected_text) }
            )
            putString(FragAiTextTool.ARG_TEXT, text.trim())
            putString(FragAiTextTool.ARG_ACTION, action.id)
            putString(FragAiTextTool.ARG_AUTHOR, author.id)
        }
        MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_ai_text_tool, args)
    }
}
