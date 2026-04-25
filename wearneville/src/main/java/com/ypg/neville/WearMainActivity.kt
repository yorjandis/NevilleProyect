package com.ypg.neville

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WearMainScreen()
            }
        }
    }

    @Composable
    private fun WearMainScreen() {
        var frase by remember { mutableStateOf(frases()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Neville",
                color = Color(0xFFF57F17),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = frase,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable { frase = frases() },
                fontSize = 14.sp
            )
        }
    }

    private fun frases(): String {
        // Este módulo no tenía frases activas (el array estaba comentado en la versión anterior).
        return ""
    }
}
