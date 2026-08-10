package com.dhiren.atom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AtomAlarmScreen(
    title: String,
    busy: Boolean,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onRemindAgain: () -> Unit,
    onIgnore: () -> Unit,
    onClose: (() -> Unit)? = null,
) {
    val colors = LocalAtomPalette.current
    val now = remember { LocalTime.now() }
    val time = remember(now) { now.format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())) }
    val meridiem = remember(now) { now.format(DateTimeFormatter.ofPattern("a", Locale.getDefault())) }
    Surface(modifier = Modifier.fillMaxSize(), color = colors.quickCard) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            onClose?.let { close ->
                IconButton(onClick = close, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                    Icon(Icons.Rounded.Close, "Close alarm", tint = colors.quickCardText.copy(alpha = .7f))
                }
            }
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AtomGlyph(LogoVariant.Original, Modifier.size(56.dp))
                Spacer(Modifier.height(30.dp))
                Text(time, color = colors.quickCardText, fontSize = 68.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-3).sp)
                Text("$meridiem · REMINDER", color = colors.mint, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(25.dp))
                Text(
                    title,
                    color = colors.quickCardText,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (busy) "Applying your choice…" else "Atom is ringing until you choose.",
                    color = colors.quickCardText.copy(alpha = .55f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(42.dp))
                Button(
                    onClick = onDone,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.mint, contentColor = Color(0xFF082017)),
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(Modifier.size(9.dp))
                    Text("Done")
                }
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    OutlinedButton(
                        onClick = onSnooze,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.quickCardText),
                    ) {
                        Icon(Icons.Rounded.Snooze, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("10 min")
                    }
                    OutlinedButton(
                        onClick = onRemindAgain,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.quickCardText),
                    ) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("1 hour")
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = onIgnore,
                    enabled = !busy,
                ) {
                    Text("Ignore this alarm", color = colors.quickCardText.copy(alpha = .7f))
                }
            }
        }
    }
}
