package com.ypg.neville.feature.calmspace.ui

import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ypg.neville.R
import com.ypg.neville.feature.calmspace.data.CalmPersonalPhraseEntity
import com.ypg.neville.feature.calmspace.data.CalmPersonalPhraseRepository
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.util.concurrent.Executors

class FragCalmPhraseManager : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private lateinit var repository: CalmPersonalPhraseRepository

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = NevilleRoomDatabase.getInstance(requireContext().applicationContext)
        repository = CalmPersonalPhraseRepository(db.calmPersonalPhraseDao())

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                CalmPhraseManagerScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbExecutor.shutdown()
    }

    @Composable
    private fun CalmPhraseManagerScreen() {
        val context = LocalContext.current
        val phrases = remember { mutableStateListOf<CalmPersonalPhraseEntity>() }
        var reloadSignal by remember { mutableIntStateOf(0) }
        var draft by remember { mutableStateOf("") }
        var editTarget by remember { mutableStateOf<CalmPersonalPhraseEntity?>(null) }
        var deleteTarget by remember { mutableStateOf<CalmPersonalPhraseEntity?>(null) }

        fun reload() {
            dbExecutor.execute {
                val list = repository.listAll()
                activity?.runOnUiThread {
                    phrases.clear()
                    phrases.addAll(list)
                }
            }
        }

        fun addPhrase(raw: String) {
            val normalized = raw.trim()
            if (normalized.isBlank()) {
                Toast.makeText(context, context.getString(R.string.calm_enter_valid_phrase), Toast.LENGTH_SHORT).show()
                return
            }
            dbExecutor.execute {
                val result = runCatching { repository.create(normalized) }
                activity?.runOnUiThread {
                    result.onSuccess {
                        draft = ""
                        reloadSignal++
                    }.onFailure { error ->
                        val message = if (error is SQLiteConstraintException) {
                            context.getString(R.string.calm_phrase_exists)
                        } else {
                            context.getString(R.string.calm_phrase_save_error)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fun updatePhrase(target: CalmPersonalPhraseEntity, raw: String) {
            val normalized = raw.trim()
            if (normalized.isBlank()) {
                Toast.makeText(context, context.getString(R.string.calm_phrase_empty), Toast.LENGTH_SHORT).show()
                return
            }
            dbExecutor.execute {
                val result = runCatching { repository.update(target.id, normalized) }
                activity?.runOnUiThread {
                    result.onSuccess {
                        editTarget = null
                        reloadSignal++
                    }.onFailure { error ->
                        val message = if (error is SQLiteConstraintException) {
                            context.getString(R.string.calm_phrase_exists)
                        } else {
                            context.getString(R.string.calm_phrase_update_error)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fun deletePhrase(target: CalmPersonalPhraseEntity) {
            dbExecutor.execute {
                repository.delete(target.id)
                activity?.runOnUiThread {
                    deleteTarget = null
                    reloadSignal++
                }
            }
        }

        LaunchedEffect(reloadSignal) {
            reload()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F2D3A), Color(0xFF1C4A4A), Color(0xFF2D6466))
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.calm_phrases_manager_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.common_close),
                    color = Color.White,
                    modifier = Modifier
                        .clickable { findNavController().popBackStack() }
                        .padding(8.dp)
                )
            }

            Text(
                text = stringResource(R.string.calm_phrases_manager_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.calm_new_phrase)) },
                        placeholder = { Text(stringResource(R.string.calm_phrase_example)) },
                        singleLine = false,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { addPhrase(draft) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.calm_add))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (phrases.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.calm_no_personal_phrases),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = phrases, key = { it.id }) { phraseItem ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = phraseItem.phrase,
                                    color = Color.White,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { editTarget = phraseItem }) {
                                        Text(stringResource(R.string.common_edit))
                                    }
                                    TextButton(onClick = { deleteTarget = phraseItem }) {
                                        Text(stringResource(R.string.common_delete))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        editTarget?.let { target ->
            var editDraft by remember(target.id) { mutableStateOf(target.phrase) }
            AlertDialog(
                onDismissRequest = { editTarget = null },
                title = { Text(stringResource(R.string.calm_edit_phrase)) },
                text = {
                    OutlinedTextField(
                        value = editDraft,
                        onValueChange = { editDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                },
                confirmButton = {
                    TextButton(onClick = { updatePhrase(target, editDraft) }) {
                        Text(stringResource(R.string.common_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editTarget = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }

        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text(stringResource(R.string.calm_delete_phrase)) },
                text = { Text(stringResource(R.string.calm_delete_phrase_question)) },
                confirmButton = {
                    TextButton(onClick = { deletePhrase(target) }) {
                        Text(stringResource(R.string.common_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}
