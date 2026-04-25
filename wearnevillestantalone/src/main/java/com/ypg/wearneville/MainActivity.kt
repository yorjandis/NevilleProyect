package com.ypg.wearneville

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(
                    onOpenDetail = {
                        startActivity(Intent(applicationContext, yor2::class.java))
                    }
                )
            }
        }
    }

    @Composable
    private fun MainScreen(onOpenDetail: () -> Unit) {
        var frase by remember { mutableStateOf(Utils.frases(applicationContext)) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Neville",
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF57F17),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = frase,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable {
                        frase = Utils.frases(applicationContext)
                        onOpenDetail()
                    },
                fontSize = 14.sp
            )
        }
    }
}
