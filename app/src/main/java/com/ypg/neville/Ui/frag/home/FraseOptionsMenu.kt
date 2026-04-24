package com.ypg.neville.ui.frag

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            text = { Text("Convertir en Nota") },
            onClick = {
                onDismiss()
                onConvertirNota()
            }
        )
        DropdownMenuItem(
            text = { Text("Cargar en Lienzo") },
            onClick = {
                onDismiss()
                onCargarLienzo()
            }
        )
        DropdownMenuItem(
            text = { Text("Compartir Frase") },
            onClick = {
                onDismiss()
                onCompartirSistema()
            }
        )
        DropdownMenuItem(
            text = { Text("Acceder a Nota") },
            onClick = {
                onDismiss()
                onAbrirNotaFrase()
            }
        )
        DropdownMenuItem(
            text = { Text("Crear Nueva Frase") },
            onClick = {
                onDismiss()
                onCrearNuevaFrase()
            }
        )
    }
}
