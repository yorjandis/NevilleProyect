package com.ypg.neville.feature.morningdialog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ypg.neville.feature.morningdialog.ui.components.MorningDialogStyles
import com.ypg.neville.feature.morningdialog.ui.components.SectionCard

@Composable
fun MorningDialogNoteScreen(
    initialNote: String,
    onSave: (String) -> Unit
) {
    var note by remember(initialNote) { mutableStateOf(initialNote) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MorningDialogStyles.backgroundBrush)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Nota del ritual",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        SectionCard(title = "Editor de nota") {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                label = { Text("Escribe tus apuntes", color = Color.Black) },
                placeholder = { Text("Observaciones, aprendizajes, intención, etc.") },
                minLines = 8,
                maxLines = 20,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black, fontWeight = FontWeight.Bold)
            )
            Button(
                onClick = { onSave(note) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MorningDialogStyles.buttonColor,
                    contentColor = MorningDialogStyles.buttonTextColor
                )
            ) {
                Text("Guardar nota")
            }
        }
    }
}
