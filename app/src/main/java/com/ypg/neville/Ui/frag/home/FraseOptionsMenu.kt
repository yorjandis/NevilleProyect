package com.ypg.neville.ui.frag

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ypg.neville.R
import com.ypg.neville.ui.theme.ContextMenuShape

@Composable
fun FraseOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    favoriteOptionLabel: String? = null,
    onToggleFavorito: (() -> Unit)? = null,
    onConvertirNota: () -> Unit,
    onCargarLienzo: () -> Unit,
    onCompartirSistema: () -> Unit,
    onAbrirNotaFrase: () -> Unit,
    onCrearNuevaFrase: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = ContextMenuShape
    ) {
        if (!favoriteOptionLabel.isNullOrBlank() && onToggleFavorito != null) {
            DropdownMenuItem(
                text = { Text(favoriteOptionLabel) },
                onClick = {
                    onDismiss()
                    onToggleFavorito()
                }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.phrase_action_convert_to_note)) },
            onClick = {
                onDismiss()
                onConvertirNota()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.phrase_action_load_canvas)) },
            onClick = {
                onDismiss()
                onCargarLienzo()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.phrase_action_share)) },
            onClick = {
                onDismiss()
                onCompartirSistema()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.phrase_action_open_note)) },
            onClick = {
                onDismiss()
                onAbrirNotaFrase()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.phrase_action_create_new)) },
            onClick = {
                onDismiss()
                onCrearNuevaFrase()
            }
        )
    }
}
