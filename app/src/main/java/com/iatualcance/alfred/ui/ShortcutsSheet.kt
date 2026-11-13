package com.iatualcance.alfred.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iatualcance.alfred.ai.Quick

/** Tabla de atajos: frases que Alfred entiende al instante (escritas o por voz). Al tocar una, se pone en la caja para editarla. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsSheet(onPick: (String) -> Unit, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Surf) {
        Column(Modifier.navigationBarsPadding()) {
            Text("Atajos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                "Frases directas que Alfred hace al momento, sin pensar. Toca una para ponerla en la caja y cambia los datos.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            )
            LazyColumn(Modifier.fillMaxWidth()) {
                Quick.TABLE.forEach { (group, rows) ->
                    item(key = "g$group") { Text(group.uppercase(), style = MaterialTheme.typography.labelMedium, color = Red, modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 2.dp)) }
                    rows.forEach { (what, example) ->
                        item(key = "$group$what") {
                            Column(Modifier.fillMaxWidth().clickable { onPick(example) }.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                Text(what, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("«$example»", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
