package com.dhiren.atom.ui

import android.app.Activity
import com.dhiren.atom.AtomApplication
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private const val OwnerName = "Dhiren Sir"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtomApp() {
    val context = LocalContext.current
    val reminderRepository = remember(context) {
        (context.applicationContext as AtomApplication).reminderRepository
    }
    val reminders by reminderRepository.reminders.collectAsState(initial = emptyList())
    val persistenceScope = rememberCoroutineScope()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    var darkTheme by rememberSaveable { mutableStateOf(systemDark) }
    var currentScreen by rememberSaveable { mutableStateOf(AtomScreen.Today) }
    var selectedLogo by rememberSaveable { mutableStateOf(LogoVariant.Original) }
    var showLogoGallery by rememberSaveable { mutableStateOf(false) }
    var showAlarmPreview by rememberSaveable { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderUi?>(null) }

    AtomTheme(darkTheme = darkTheme) {
        val colors = LocalAtomPalette.current
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val activity = view.context as? Activity ?: return@SideEffect
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = colors.canvas) {
            Scaffold(
                containerColor = colors.canvas,
                bottomBar = {
                    if (currentScreen != AtomScreen.Capture) {
                        AtomBottomBar(
                            selected = currentScreen,
                            onSelect = { currentScreen = it },
                            onAdd = {
                                editingReminder = null
                                currentScreen = AtomScreen.Capture
                            },
                        )
                    }
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding()),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        label = "Atom screen",
                    ) { screen ->
                        when (screen) {
                            AtomScreen.Today -> HomeScreen(
                                reminders = reminders,
                                darkTheme = darkTheme,
                                selectedLogo = selectedLogo,
                                onToggleTheme = { darkTheme = !darkTheme },
                                onLogoClick = { showLogoGallery = true },
                                onCapture = {
                                    editingReminder = null
                                    currentScreen = AtomScreen.Capture
                                },
                                onShowAlarm = { showAlarmPreview = true },
                                onSeeAll = { currentScreen = AtomScreen.Reminders },
                                onEdit = {
                                    editingReminder = it
                                    currentScreen = AtomScreen.Capture
                                },
                            )

                            AtomScreen.Capture -> CaptureScreen(
                                reminder = editingReminder,
                                onBack = { currentScreen = AtomScreen.Today },
                                onSave = { draft ->
                                    val existing = editingReminder
                                    val replacement = draft.copy(
                                        id = existing?.id ?: 0L,
                                    )
                                    persistenceScope.launch {
                                        reminderRepository.save(replacement)
                                    }
                                    editingReminder = null
                                    currentScreen = AtomScreen.Reminders
                                },
                            )

                            AtomScreen.Reminders -> RemindersScreen(
                                reminders = reminders,
                                onEdit = {
                                    editingReminder = it
                                    currentScreen = AtomScreen.Capture
                                },
                                onDelete = {
                                    persistenceScope.launch {
                                        reminderRepository.delete(it.id)
                                    }
                                },
                                onAdd = {
                                    editingReminder = null
                                    currentScreen = AtomScreen.Capture
                                },
                            )

                            AtomScreen.Settings -> SettingsScreen(
                                darkTheme = darkTheme,
                                selectedLogo = selectedLogo,
                                onToggleTheme = { darkTheme = !darkTheme },
                                onChooseLogo = { showLogoGallery = true },
                                onAlarmPreview = { showAlarmPreview = true },
                            )
                        }
                    }
                }
            }

            if (showLogoGallery) {
                LogoGallery(
                    selected = selectedLogo,
                    onSelect = {
                        selectedLogo = it
                        showLogoGallery = false
                    },
                    onDismiss = { showLogoGallery = false },
                )
            }

            if (showAlarmPreview) {
                AlarmPreview(onDismiss = { showAlarmPreview = false })
            }
        }
    }
}

@Composable
private fun ScreenFrame(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun HomeScreen(
    reminders: List<ReminderUi>,
    darkTheme: Boolean,
    selectedLogo: LogoVariant,
    onToggleTheme: () -> Unit,
    onLogoClick: () -> Unit,
    onCapture: () -> Unit,
    onShowAlarm: () -> Unit,
    onSeeAll: () -> Unit,
    onEdit: (ReminderUi) -> Unit,
) {
    val colors = LocalAtomPalette.current
    ScreenFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 30.dp),
        ) {
            AppHeader(
                darkTheme = darkTheme,
                selectedLogo = selectedLogo,
                onToggleTheme = onToggleTheme,
                onLogoClick = onLogoClick,
                onNotifications = onShowAlarm,
            )
            Spacer(Modifier.height(28.dp))
            GreetingCard(scheduledCount = reminders.count { it.state == ReminderState.Scheduled })
            Spacer(Modifier.height(18.dp))
            QuickCaptureCard(onCapture = onCapture)
            Spacer(Modifier.height(28.dp))
            SectionHeading(
                eyebrow = "COMING UP",
                title = "Your next reminder",
                action = "See all",
                onAction = onSeeAll,
            )
            Spacer(Modifier.height(12.dp))
            reminders.firstOrNull()?.let {
                NextReminderCard(reminder = it, onEdit = { onEdit(it) })
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = reminders.count { it.state == ReminderState.Scheduled }.toString().padStart(2, '0'),
                    label = "Scheduled",
                    tint = colors.mintPale,
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = reminders.count { it.state != ReminderState.Scheduled }.toString().padStart(2, '0'),
                    label = "Needs a detail",
                    tint = colors.coralPale,
                )
            }
        }
    }
}

@Composable
private fun AppHeader(
    darkTheme: Boolean,
    selectedLogo: LogoVariant,
    onToggleTheme: () -> Unit,
    onLogoClick: () -> Unit,
    onNotifications: () -> Unit,
) {
    val colors = LocalAtomPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AtomWordmark(
            variant = selectedLogo,
            modifier = Modifier.clickable(onClick = onLogoClick),
        )
        Spacer(Modifier.weight(1f))
        HeaderIcon(
            icon = if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
            description = if (darkTheme) "Use light mode" else "Use dark mode",
            onClick = onToggleTheme,
        )
        Spacer(Modifier.width(6.dp))
        Box {
            HeaderIcon(
                icon = Icons.Rounded.NotificationsNone,
                description = "Preview alarm",
                onClick = onNotifications,
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.coral)
                    .align(Alignment.TopEnd)
                    .offset(x = (-7).dp, y = 7.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.ink),
            contentAlignment = Alignment.Center,
        ) {
            Text("D", color = colors.canvas, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HeaderIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val colors = LocalAtomPalette.current
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.surface),
    ) {
        Icon(icon, contentDescription = description, tint = colors.ink, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun GreetingCard(scheduledCount: Int) {
    val colors = LocalAtomPalette.current
    val now = remember { LocalDateTime.now() }
    Surface(
        color = colors.paper,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.line, RoundedCornerShape(28.dp)),
    ) {
        Row(
            modifier = Modifier.padding(start = 22.dp, top = 22.dp, end = 10.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friendlyDate(now.toLocalDate()).uppercase(Locale.getDefault()),
                    color = colors.muted,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.1.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    buildAnnotatedString {
                        append("${greetingForHour(now.hour)},\n")
                        withStyle(SpanStyle(color = colors.mintDark)) { append("$OwnerName.") }
                    },
                    color = colors.ink,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You have $scheduledCount reminders ready.",
                    color = colors.muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            AtomDoodle(modifier = Modifier.size(width = 105.dp, height = 105.dp))
        }
    }
}

@Composable
private fun AtomDoodle(modifier: Modifier = Modifier) {
    val colors = LocalAtomPalette.current
    val transition = rememberInfiniteTransition(label = "Doodle wave")
    val wave by transition.animateFloat(
        initialValue = -8f,
        targetValue = 11f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Waving hand",
    )
    Canvas(modifier = modifier.semantics { contentDescription = "Atom waving hello" }) {
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(colors.mintPale, radius = size.minDimension * .45f, center = center)
        drawCircle(colors.ink, radius = size.minDimension * .15f, center = Offset(size.width * .51f, size.height * .38f), style = stroke)
        drawCircle(colors.ink, radius = 1.5.dp.toPx(), center = Offset(size.width * .46f, size.height * .37f))
        drawCircle(colors.ink, radius = 1.5.dp.toPx(), center = Offset(size.width * .56f, size.height * .37f))
        drawArc(
            color = colors.coral,
            startAngle = 18f,
            sweepAngle = 145f,
            useCenter = false,
            topLeft = Offset(size.width * .45f, size.height * .38f),
            size = Size(size.width * .13f, size.height * .11f),
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
        )
        val body = Path().apply {
            moveTo(size.width * .36f, size.height * .79f)
            quadraticBezierTo(size.width * .38f, size.height * .55f, size.width * .52f, size.height * .55f)
            quadraticBezierTo(size.width * .68f, size.height * .56f, size.width * .70f, size.height * .81f)
        }
        drawPath(body, colors.ink, style = stroke)
        drawLine(colors.ink, Offset(size.width * .42f, size.height * .65f), Offset(size.width * .26f, size.height * .57f), 3.dp.toPx(), StrokeCap.Round)
        rotate(wave, pivot = Offset(size.width * .67f, size.height * .62f)) {
            drawLine(colors.ink, Offset(size.width * .64f, size.height * .65f), Offset(size.width * .79f, size.height * .49f), 3.dp.toPx(), StrokeCap.Round)
            drawLine(colors.ink, Offset(size.width * .79f, size.height * .49f), Offset(size.width * .82f, size.height * .40f), 2.5.dp.toPx(), StrokeCap.Round)
            drawLine(colors.ink, Offset(size.width * .79f, size.height * .49f), Offset(size.width * .88f, size.height * .47f), 2.5.dp.toPx(), StrokeCap.Round)
        }
        drawCircle(colors.coral, radius = 4.dp.toPx(), center = Offset(size.width * .23f, size.height * .26f))
        drawCircle(colors.mint, radius = 3.dp.toPx(), center = Offset(size.width * .80f, size.height * .23f))
    }
}

@Composable
private fun QuickCaptureCard(onCapture: () -> Unit) {
    val colors = LocalAtomPalette.current
    var text by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "Quick capture accents")
    val motion by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (focused) 1400 else 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Accent motion",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = .25f))
            .clip(RoundedCornerShape(28.dp))
            .background(colors.quickCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCapture,
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shift = if (focused) motion * 20f else motion * 8f
            val mintPath = Path().apply {
                moveTo(size.width * .70f, -8f)
                cubicTo(size.width * .91f, size.height * .18f + shift, size.width * .72f, size.height * .62f, size.width * 1.04f, size.height * .74f)
            }
            val coralPath = Path().apply {
                moveTo(size.width * .89f, -10f)
                cubicTo(size.width * .69f, size.height * .30f - shift, size.width * .98f, size.height * .52f, size.width * .82f, size.height * 1.05f)
            }
            drawPath(mintPath, colors.mint.copy(alpha = .8f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            drawPath(coralPath, colors.coral.copy(alpha = .8f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(colors.mint))
                Spacer(Modifier.width(8.dp))
                Text(
                    "QUICK CAPTURE",
                    color = colors.mint,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp,
                )
            }
            Spacer(Modifier.height(15.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.quickCardText),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focused = it.isFocused }
                        .focusable(),
                    decorationBox = { inner ->
                        if (text.isBlank()) {
                            Text("What should I remind you?", color = colors.quickCardText.copy(alpha = .62f))
                        }
                        inner()
                    },
                )
                FilledIconButton(
                    onClick = onCapture,
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.mint,
                        contentColor = Color(0xFF092117),
                    ),
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = "Speak a reminder", modifier = Modifier.size(23.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Try “in 20 minutes” or “every weekday at 9 AM”",
                color = colors.quickCardText.copy(alpha = .48f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SectionHeading(
    eyebrow: String,
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalAtomPalette.current
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(eyebrow, color = colors.muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, color = colors.ink, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, color = colors.mintDark)
                Icon(Icons.Rounded.ChevronRight, null, tint = colors.mintDark, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun NextReminderCard(reminder: ReminderUi, onEdit: () -> Unit) {
    val colors = LocalAtomPalette.current
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.line, RoundedCornerShape(24.dp))
            .clickable(onClick = onEdit),
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(colors.mintPale),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Alarm, null, tint = colors.mintDark)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, color = colors.ink, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(5.dp))
                Text(
                    listOfNotNull(reminder.date, reminder.time).joinToString(" · "),
                    color = colors.muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Rounded.Edit, contentDescription = "Edit reminder", tint = colors.muted, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, tint: Color) {
    val colors = LocalAtomPalette.current
    Surface(
        modifier = modifier,
        color = tint,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(value, color = colors.ink, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(3.dp))
            Text(label, color = colors.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AtomBottomBar(
    selected: AtomScreen,
    onSelect: (AtomScreen) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalAtomPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper,
        shadowElevation = 18.dp,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .height(76.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            BottomDestination(Icons.Rounded.Home, "Today", selected == AtomScreen.Today) { onSelect(AtomScreen.Today) }
            BottomDestination(Icons.Rounded.ListAlt, "Reminders", selected == AtomScreen.Reminders) { onSelect(AtomScreen.Reminders) }
            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(54.dp)
                    .shadow(12.dp, CircleShape, ambientColor = colors.mint.copy(alpha = .35f)),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.ink, contentColor = colors.canvas),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New reminder", modifier = Modifier.size(25.dp))
            }
            BottomDestination(Icons.Rounded.Settings, "Settings", selected == AtomScreen.Settings) { onSelect(AtomScreen.Settings) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomDestination(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAtomPalette.current
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) colors.mintDark else colors.muted, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (selected) colors.ink else colors.muted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private data class CommandDraft(
    val title: String,
    val date: String?,
    val time: String?,
    val relative: String? = null,
    val recurrence: String? = null,
) {
    val missingDate: Boolean get() = date == null && relative == null
    val missingTime: Boolean get() = time == null && relative == null
}

private fun analyzeCommand(input: String): CommandDraft {
    val normalized = input.trim()
    val lower = normalized.lowercase(Locale.getDefault())
    val relativeMatch = Regex("\\bin\\s+(\\d+)\\s+(minute|minutes|hour|hours)\\b", RegexOption.IGNORE_CASE)
        .find(normalized)
    val timeMatch = Regex("\\b(?:at\\s+)?(1[0-2]|0?[1-9])(?::([0-5]\\d))?\\s*(am|pm)\\b", RegexOption.IGNORE_CASE)
        .find(normalized)

    val date = when {
        "tomorrow" in lower -> "Tomorrow"
        "today" in lower -> "Today"
        Regex("\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b").containsMatchIn(lower) ->
            Regex("\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b").find(lower)?.value?.replaceFirstChar { it.uppercase() }
        else -> null
    }
    val time = timeMatch?.let { match ->
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].ifBlank { "00" }
        val meridiem = match.groupValues[3].uppercase(Locale.getDefault())
        "$hour:$minute $meridiem"
    }
    val relative = relativeMatch?.value?.replaceFirstChar { it.uppercase() }
    val recurrence = when {
        "every weekday" in lower -> "Every weekday"
        "every day" in lower || "daily" in lower -> "Every day"
        "every week" in lower || "weekly" in lower -> "Every week"
        "every month" in lower || "monthly" in lower -> "Every month"
        else -> null
    }

    var title = normalized
    naturalLanguagePrefixes.sortedByDescending { it.length }.forEach { prefix ->
        title = title.replace(Regex("^${Regex.escape(prefix)}[\\s,:-]*", RegexOption.IGNORE_CASE), "")
    }
    title = title
        .replace(Regex("^(?:remind me(?: again)?(?: to| about)?|to)\\s+", RegexOption.IGNORE_CASE), "")
        .replace(relativeMatch?.value.orEmpty(), "")
        .replace(timeMatch?.value.orEmpty(), "")
        .replace(Regex("\\b(today|tomorrow|on (?:monday|tuesday|wednesday|thursday|friday|saturday|sunday))\\b", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\b(every weekday|every day|daily|every week|weekly|every month|monthly)\\b", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim(' ', ',', '.', '-')
        .ifBlank { "Untitled reminder" }

    return CommandDraft(
        title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
        date = if (recurrence != null) recurrence else date,
        time = time,
        relative = relative,
        recurrence = recurrence,
    )
}

@Composable
private fun CaptureScreen(
    reminder: ReminderUi?,
    onBack: () -> Unit,
    onSave: (ReminderUi) -> Unit,
) {
    val colors = LocalAtomPalette.current
    val initialCommand = reminder?.let {
        it.sourceText.ifBlank {
            "Remind me to ${it.title}${it.date?.let { date -> " $date" }.orEmpty()}${it.time?.let { time -> " at $time" }.orEmpty()}"
        }
    } ?: ""
    var command by remember(reminder?.id) { mutableStateOf(initialCommand) }
    var listening by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<CommandDraft?>(null) }
    var showFollowUp by remember { mutableStateOf(false) }

    fun saveDraft(value: CommandDraft) {
        val state = when {
            value.missingDate && value.missingTime -> ReminderState.Unscheduled
            value.missingDate -> ReminderState.NeedsDate
            value.missingTime -> ReminderState.NeedsTime
            else -> ReminderState.Scheduled
        }
        onSave(
            ReminderUi(
                id = reminder?.id ?: 0L,
                title = value.title,
                date = value.date ?: value.relative,
                time = value.time,
                source = if (listening) "Voice" else "Text",
                state = state,
                accent = reminder?.accent ?: ReminderAccent.Mint,
                recurrence = value.recurrence,
                sourceText = command,
            ),
        )
    }

    ScreenFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 34.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HeaderIcon(Icons.Rounded.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (reminder == null) "New reminder" else "Update reminder",
                        color = colors.ink,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        if (reminder == null) "Say it the way you think it" else "Change the task, date, or time",
                        color = colors.muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (command.isNotBlank()) {
                    IconButton(onClick = { command = ""; draft = null }) {
                        Icon(Icons.Rounded.Close, "Clear", tint = colors.muted)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Say it naturally.\nI’ll find the when.",
                color = colors.ink,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "“Atom” is optional after you press the microphone.",
                color = colors.muted,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(26.dp))
            VoiceOrb(
                listening = listening,
                onClick = {
                    listening = !listening
                    if (command.isBlank()) {
                        command = "Hey Atom, remind me to send the product brief tomorrow at 6:30 PM"
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.line, RoundedCornerShape(24.dp)),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("YOUR WORDS", color = colors.muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.1.sp)
                        Spacer(Modifier.weight(1f))
                        Text(if (listening) "Listening…" else "Editable", color = if (listening) colors.coral else colors.mintDark, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    BasicTextField(
                        value = command,
                        onValueChange = { command = it; draft = null },
                        minLines = 3,
                        maxLines = 6,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (command.isBlank()) {
                                Text("Remind me in 20 minutes to check the oven…", color = colors.muted)
                            }
                            inner()
                        },
                    )
                }
            }
            Spacer(Modifier.height(15.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("In 20 minutes", "Tomorrow", "Every weekday", "12:00 AM").forEach { sample ->
                    SuggestionChip(sample) {
                        command = when (sample) {
                            "In 20 minutes" -> "Remind me in 20 minutes to check the oven"
                            "Tomorrow" -> "Please remind me to call Rhea tomorrow"
                            "Every weekday" -> "Every weekday at 9 AM remind me to review my priorities"
                            else -> "Hey Atom, remind me to submit the report tomorrow at 12:00 AM"
                        }
                        draft = null
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (command.isNotBlank()) {
                        draft = analyzeCommand(command)
                        listening = false
                    }
                },
                enabled = command.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.canvas),
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text("Understand reminder")
            }
            draft?.let { understood ->
                Spacer(Modifier.height(18.dp))
                DraftReviewCard(
                    draft = understood,
                    isEditing = reminder != null,
                    onContinue = {
                        if (understood.missingDate || understood.missingTime) showFollowUp = true else saveDraft(understood)
                    },
                )
            }
        }
    }

    if (showFollowUp && draft != null) {
        MissingDetailsDialog(
            draft = draft!!,
            onDismiss = { showFollowUp = false },
            onSave = {
                showFollowUp = false
                saveDraft(it)
            },
        )
    }
}

@Composable
private fun VoiceOrb(listening: Boolean, onClick: () -> Unit) {
    val colors = LocalAtomPalette.current
    val transition = rememberInfiniteTransition(label = "Voice pulse")
    val pulse by transition.animateFloat(
        initialValue = .86f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "Voice ring",
    )
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(154.dp)) {
            val radius = size.minDimension * .46f * if (listening) pulse else .9f
            drawCircle(colors.mint.copy(alpha = if (listening) .14f else .08f), radius)
            drawCircle(colors.mint.copy(alpha = if (listening) .28f else .14f), radius * .76f, style = Stroke(1.dp.toPx()))
            for (i in 0 until 24) {
                val angle = (i / 24.0) * Math.PI * 2.0
                val inner = radius * .84f
                val wave = ((sin(i.toDouble() + pulse.toDouble() * 5.0) + 1.0) / 2.0).toFloat()
                val outer = inner + (if (listening) (4f + 8f * wave) else 3f).dp.toPx()
                drawLine(
                    colors.mint.copy(alpha = if (listening) .8f else .25f),
                    Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                    Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                    1.5.dp.toPx(),
                    StrokeCap.Round,
                )
            }
        }
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(82.dp).shadow(16.dp, CircleShape),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (listening) colors.coral else colors.ink,
                contentColor = if (listening) Color.White else colors.canvas,
            ),
        ) {
            Icon(if (listening) Icons.Rounded.Check else Icons.Rounded.Mic, if (listening) "Stop listening" else "Start listening", modifier = Modifier.size(31.dp))
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    val colors = LocalAtomPalette.current
    Surface(
        color = colors.paper,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .border(1.dp, colors.line, RoundedCornerShape(50))
            .clickable(onClick = onClick),
    ) {
        Text(label, color = colors.muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
    }
}

@Composable
private fun DraftReviewCard(draft: CommandDraft, isEditing: Boolean, onContinue: () -> Unit) {
    val colors = LocalAtomPalette.current
    Surface(color = colors.mintPale, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = colors.mintDark, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("I understood", color = colors.mintDark, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(14.dp))
            Text(draft.title, color = colors.ink, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailPill(Icons.Rounded.CalendarToday, draft.date ?: draft.relative ?: "Date needed", draft.missingDate)
                DetailPill(Icons.Rounded.Schedule, draft.time ?: if (draft.relative != null) "Relative" else "Time needed", draft.missingTime)
            }
            if (draft.missingDate || draft.missingTime) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "I’ll ask once for ${listOfNotNull(if (draft.missingDate) "a date" else null, if (draft.missingTime) "a time" else null).joinToString(" and ")}.",
                    color = colors.muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.canvas),
            ) {
                Text(
                    when {
                        draft.missingDate || draft.missingTime -> "Add missing details"
                        isEditing -> "Update reminder"
                        else -> "Schedule reminder"
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, warning: Boolean) {
    val colors = LocalAtomPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (warning) colors.coralPale else colors.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (warning) colors.coral else colors.muted, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = colors.ink, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MissingDetailsDialog(
    draft: CommandDraft,
    onDismiss: () -> Unit,
    onSave: (CommandDraft) -> Unit,
) {
    val colors = LocalAtomPalette.current
    var date by remember { mutableStateOf(draft.date.orEmpty()) }
    var time by remember { mutableStateOf(draft.time.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.elevated,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text("One quick detail", color = colors.ink, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(5.dp))
                Text("When should I remind you?", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (draft.missingDate) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        placeholder = { Text("Today, tomorrow, or a date") },
                        leadingIcon = { Icon(Icons.Rounded.CalendarToday, null) },
                        trailingIcon = { Icon(Icons.Rounded.Mic, "Speak the date") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                }
                if (draft.missingTime) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time · 12-hour format") },
                        placeholder = { Text("6:30 PM") },
                        leadingIcon = { Icon(Icons.Rounded.Schedule, null) },
                        trailingIcon = { Icon(Icons.Rounded.Mic, "Speak the time") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                }
                TextButton(onClick = { onSave(draft) }, modifier = Modifier.align(Alignment.Start)) {
                    Text("Save to Unscheduled", color = colors.muted)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        draft.copy(
                            date = date.ifBlank { draft.date },
                            time = time.ifBlank { draft.time },
                        ),
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.canvas),
            ) { Text("Save reminder") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back", color = colors.muted) } },
    )
}

@Composable
private fun RemindersScreen(
    reminders: List<ReminderUi>,
    onEdit: (ReminderUi) -> Unit,
    onDelete: (ReminderUi) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalAtomPalette.current
    var filter by rememberSaveable { mutableStateOf("All") }
    val visible = reminders.filter {
        when (filter) {
            "Scheduled" -> it.state == ReminderState.Scheduled
            "Needs details" -> it.state != ReminderState.Scheduled
            "Repeats" -> it.recurrence != null
            else -> true
        }
    }
    ScreenFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("REMINDERS", color = colors.muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp)
                        Text("Everything on your radar", color = colors.ink, style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(Modifier.weight(1f))
                    HeaderIcon(Icons.Rounded.Search, "Search reminders") {}
                }
                Spacer(Modifier.height(19.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("All", "Scheduled", "Needs details", "Repeats").forEach { option ->
                        FilterChip(option, option == filter) { filter = option }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (visible.isEmpty()) {
                    item { EmptyReminders(onAdd) }
                }
                items(visible, key = { it.id }) { reminder ->
                    ReminderCard(reminder, onEdit = { onEdit(reminder) }, onDelete = { onDelete(reminder) })
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAtomPalette.current
    Surface(
        color = if (selected) colors.ink else colors.paper,
        contentColor = if (selected) colors.canvas else colors.muted,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .border(1.dp, if (selected) colors.ink else colors.line, RoundedCornerShape(50))
            .clickable(onClick = onClick),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

@Composable
private fun ReminderCard(reminder: ReminderUi, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalAtomPalette.current
    val accent = when (reminder.accent) {
        ReminderAccent.Mint -> colors.mint
        ReminderAccent.Coral -> colors.coral
        ReminderAccent.Lime -> colors.lime
    }
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(23.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, colors.line, RoundedCornerShape(23.dp)),
    ) {
        Row(modifier = Modifier.padding(17.dp)) {
            Box(Modifier.width(4.dp).height(72.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(reminder.title, color = colors.ink, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Edit, "Edit", tint = colors.muted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, "Delete", tint = colors.muted, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarToday, null, tint = colors.muted, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(reminder.date ?: "Date needed", color = if (reminder.date == null) colors.coral else colors.muted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Rounded.Schedule, null, tint = colors.muted, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(reminder.time ?: "Time needed", color = if (reminder.time == null) colors.coral else colors.muted, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = colors.paper, shape = RoundedCornerShape(50)) {
                        Text(reminder.source, color = colors.muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                    reminder.recurrence?.let {
                        Spacer(Modifier.width(7.dp))
                        Icon(Icons.Rounded.Repeat, null, tint = colors.mintDark, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(it, color = colors.mintDark, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReminders(onAdd: () -> Unit) {
    val colors = LocalAtomPalette.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(colors.mintPale), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DoneAll, null, tint = colors.mintDark, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Nothing here", color = colors.ink, style = MaterialTheme.typography.titleMedium)
        Text("Your mind is clear.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onAdd) { Text("Add a reminder", color = colors.mintDark) }
    }
}

@Composable
private fun SettingsScreen(
    darkTheme: Boolean,
    selectedLogo: LogoVariant,
    onToggleTheme: () -> Unit,
    onChooseLogo: () -> Unit,
    onAlarmPreview: () -> Unit,
) {
    val colors = LocalAtomPalette.current
    var alarmMode by rememberSaveable { mutableStateOf(true) }
    var aiFallback by rememberSaveable { mutableStateOf(false) }
    var showPrefixes by rememberSaveable { mutableStateOf(false) }
    val locale = remember { Locale.getDefault().displayName }
    val timezone = remember { ZoneId.systemDefault().id }

    ScreenFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 30.dp),
        ) {
            Text("SETTINGS", color = colors.muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp)
            Text("Make Atom yours", color = colors.ink, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(22.dp))
            Surface(color = colors.ink, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(colors.mint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("D", color = Color(0xFF092117), style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(OwnerName, color = colors.quickCardText, style = MaterialTheme.typography.titleMedium)
                        Text("Atom’s sole owner", color = colors.quickCardText.copy(alpha = .58f), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Lock, null, tint = colors.mint, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.height(25.dp))
            SettingsLabel("PERSONALIZATION")
            Spacer(Modifier.height(9.dp))
            SettingsGroup {
                SettingsRow(
                    icon = if (darkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    title = "Appearance",
                    subtitle = if (darkTheme) "Dark mode" else "Light mode",
                    trailing = {
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors = atomSwitchColors(),
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Atom mark",
                    subtitle = selectedLogo.label,
                    onClick = onChooseLogo,
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Language,
                    title = "Locale & timezone",
                    subtitle = "$locale · $timezone",
                )
            }
            Spacer(Modifier.height(22.dp))
            SettingsLabel("REMINDING")
            Spacer(Modifier.height(9.dp))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.Alarm,
                    title = "Alarm mode",
                    subtitle = "Full-screen ring until dismissed",
                    trailing = {
                        Switch(
                            checked = alarmMode,
                            onCheckedChange = { alarmMode = it },
                            colors = atomSwitchColors(),
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.VolumeUp,
                    title = "Preview ring screen",
                    subtitle = "Test dismiss, snooze, and remind again",
                    onClick = onAlarmPreview,
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Notification health",
                    subtitle = "Permissions checked on this device",
                    badge = "Ready",
                )
            }
            Spacer(Modifier.height(22.dp))
            SettingsLabel("UNDERSTANDING")
            Spacer(Modifier.height(9.dp))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.KeyboardVoice,
                    title = "Accepted openings",
                    subtitle = "${naturalLanguagePrefixes.size} natural prefixes",
                    onClick = { showPrefixes = true },
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "OpenAI fallback",
                    subtitle = if (aiFallback) "Only when local parsing is uncertain" else "Off · no token usage",
                    trailing = {
                        Switch(
                            checked = aiFallback,
                            onCheckedChange = { aiFallback = it },
                            colors = atomSwitchColors(),
                        )
                    },
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.CloudOff, null, tint = colors.muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Offline device storage is active. Alarms, speech recognition, and optional cloud sync are wired in the implementation phases that follow.",
                    color = colors.muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Atom · Phase 1 UI · v0.1.0", color = colors.muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    if (showPrefixes) {
        PrefixesDialog(onDismiss = { showPrefixes = false })
    }
}

@Composable
private fun atomSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = LocalAtomPalette.current.mintDark,
    uncheckedThumbColor = LocalAtomPalette.current.muted,
    uncheckedTrackColor = LocalAtomPalette.current.paper,
    uncheckedBorderColor = LocalAtomPalette.current.line,
)

@Composable
private fun SettingsLabel(text: String) {
    Text(text, color = LocalAtomPalette.current.muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.1.sp)
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    val colors = LocalAtomPalette.current
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(23.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, colors.line, RoundedCornerShape(23.dp)),
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = LocalAtomPalette.current.line, modifier = Modifier.padding(start = 66.dp))
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalAtomPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = colors.mintDark, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.ink, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
        if (badge != null) {
            Surface(color = colors.mintPale, shape = RoundedCornerShape(50)) {
                Text(badge, color = colors.mintDark, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
            }
        } else if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Rounded.ChevronRight, null, tint = colors.muted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PrefixesDialog(onDismiss: () -> Unit) {
    val colors = LocalAtomPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.elevated,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Speak naturally", color = colors.ink, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text("Atom ignores these optional openings before understanding the reminder:", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(330.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(naturalLanguagePrefixes) { prefix ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(colors.mint))
                            Spacer(Modifier.width(9.dp))
                            Text(prefix, color = colors.ink, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogoGallery(
    selected: LogoVariant,
    onSelect: (LogoVariant) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAtomPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.elevated,
        contentColor = colors.ink,
        dragHandle = {
            Box(
                Modifier.padding(top = 11.dp, bottom = 7.dp).size(width = 40.dp, height = 4.dp).clip(CircleShape).background(colors.line),
            )
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Choose Atom’s mark", color = colors.ink, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(5.dp))
            Text("The original stays first. Here are thirteen more directions.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(500.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                gridItems(LogoVariant.entries) { option ->
                    LogoOption(
                        variant = option,
                        selected = option == selected,
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LogoOption(variant: LogoVariant, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAtomPalette.current
    Surface(
        color = if (selected) colors.mintPale else colors.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (selected) colors.mint else colors.line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AtomGlyph(variant, Modifier.size(42.dp))
            Spacer(Modifier.width(11.dp))
            Text(variant.label, color = colors.ink, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Rounded.Check, null, tint = colors.mintDark, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AtomWordmark(variant: LogoVariant, modifier: Modifier = Modifier) {
    val colors = LocalAtomPalette.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AtomGlyph(variant = variant, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(9.dp))
        Text("atom", color = colors.ink, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.3).sp)
    }
}

@Composable
private fun AtomGlyph(variant: LogoVariant, modifier: Modifier = Modifier) {
    val colors = LocalAtomPalette.current
    val darkBack = variant !in listOf(LogoVariant.Pulse, LogoVariant.Halo, LogoVariant.Mono, LogoVariant.Arc)
    val shape = when (variant) {
        LogoVariant.Orbit, LogoVariant.Nucleus, LogoVariant.Eclipse, LogoVariant.Ripple -> CircleShape
        LogoVariant.Spark, LogoVariant.Prism -> RoundedCornerShape(11.dp, 5.dp, 11.dp, 5.dp)
        else -> RoundedCornerShape(10.dp)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (darkBack) colors.ink else Color.Transparent)
            .then(if (!darkBack) Modifier.border(1.dp, colors.line, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val mint = colors.mint
            val coral = colors.coral
            val ink = if (darkBack) colors.quickCardText else colors.ink
            val thin = 1.6.dp.toPx()
            val centerPoint = center
            when (variant) {
                LogoVariant.Original -> {
                    repeat(3) { index ->
                        rotate(index * 60f, centerPoint) {
                            drawOval(mint, topLeft = Offset(size.width * .34f, size.height * .05f), size = Size(size.width * .32f, size.height * .90f), style = Stroke(thin))
                        }
                    }
                    drawCircle(coral, size.minDimension * .09f, centerPoint)
                }
                LogoVariant.Orbit -> {
                    rotate(18f, centerPoint) { drawOval(mint, Offset(size.width * .05f, size.height * .32f), Size(size.width * .90f, size.height * .36f), style = Stroke(thin)) }
                    rotate(-48f, centerPoint) { drawOval(coral, Offset(size.width * .05f, size.height * .32f), Size(size.width * .90f, size.height * .36f), style = Stroke(thin)) }
                    drawCircle(ink, size.minDimension * .08f, centerPoint)
                }
                LogoVariant.Pulse -> {
                    drawCircle(colors.ink, size.minDimension * .45f, style = Stroke(thin))
                    drawCircle(mint, size.minDimension * .31f, style = Stroke(thin))
                    drawCircle(coral, size.minDimension * .16f, style = Stroke(thin))
                    drawCircle(coral, size.minDimension * .06f)
                }
                LogoVariant.Spark -> {
                    drawLine(mint, Offset(center.x, size.height * .08f), Offset(center.x, size.height * .92f), thin, StrokeCap.Round)
                    drawLine(coral, Offset(size.width * .08f, center.y), Offset(size.width * .92f, center.y), thin, StrokeCap.Round)
                    drawLine(ink.copy(alpha = .5f), Offset(size.width * .18f, size.height * .18f), Offset(size.width * .82f, size.height * .82f), thin, StrokeCap.Round)
                    drawCircle(ink, size.minDimension * .08f)
                }
                LogoVariant.Nucleus -> {
                    drawCircle(mint.copy(alpha = .5f), size.minDimension * .42f, style = Stroke(thin))
                    rotate(28f, centerPoint) { drawOval(mint, Offset(size.width * .05f, size.height * .35f), Size(size.width * .9f, size.height * .3f), style = Stroke(thin)) }
                    rotate(-28f, centerPoint) { drawOval(coral, Offset(size.width * .05f, size.height * .35f), Size(size.width * .9f, size.height * .3f), style = Stroke(thin)) }
                    drawCircle(mint, size.minDimension * .13f)
                }
                LogoVariant.Halo -> {
                    drawRoundRect(colors.ink, style = Stroke(thin), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
                    drawCircle(mint, size.minDimension * .30f, style = Stroke(thin))
                    drawArc(coral, 215f, 105f, false, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(coral, size.minDimension * .06f)
                }
                LogoVariant.Bond -> {
                    drawLine(mint, Offset(size.width * .25f, center.y), Offset(size.width * .75f, center.y), 2.dp.toPx(), StrokeCap.Round)
                    drawCircle(mint, size.minDimension * .19f, Offset(size.width * .24f, center.y), style = Stroke(thin))
                    drawCircle(coral, size.minDimension * .19f, Offset(size.width * .76f, center.y), style = Stroke(thin))
                    drawCircle(ink, size.minDimension * .06f)
                }
                LogoVariant.Mono -> {
                    val a = Path().apply {
                        moveTo(size.width * .20f, size.height * .86f)
                        lineTo(size.width * .50f, size.height * .12f)
                        lineTo(size.width * .80f, size.height * .86f)
                        moveTo(size.width * .32f, size.height * .60f)
                        lineTo(size.width * .68f, size.height * .60f)
                    }
                    drawPath(a, colors.ink, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(coral, size.minDimension * .07f, Offset(size.width * .50f, size.height * .60f))
                }
                LogoVariant.Prism -> {
                    val triangle = Path().apply {
                        moveTo(center.x, size.height * .08f)
                        lineTo(size.width * .91f, size.height * .84f)
                        lineTo(size.width * .09f, size.height * .84f)
                        close()
                    }
                    drawPath(triangle, mint, style = Stroke(thin, cap = StrokeCap.Round))
                    drawLine(coral, Offset(center.x, size.height * .08f), Offset(center.x, size.height * .84f), thin)
                    drawCircle(ink, size.minDimension * .07f, Offset(center.x, size.height * .61f))
                }
                LogoVariant.Twin -> {
                    drawCircle(mint, size.minDimension * .29f, Offset(size.width * .39f, center.y), style = Stroke(2.dp.toPx()))
                    drawCircle(coral, size.minDimension * .29f, Offset(size.width * .61f, center.y), style = Stroke(2.dp.toPx()))
                    drawCircle(ink, size.minDimension * .07f)
                }
                LogoVariant.Eclipse -> {
                    drawArc(mint, 55f, 250f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(coral, 235f, 195f, false, topLeft = Offset(size.width * .15f, size.height * .15f), size = Size(size.width * .70f, size.height * .70f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(ink, size.minDimension * .08f)
                }
                LogoVariant.Ripple -> {
                    listOf(.14f, .27f, .41f).forEachIndexed { index, radius ->
                        drawCircle(if (index == 1) coral else mint, size.minDimension * radius, style = Stroke(thin))
                    }
                    drawCircle(ink, size.minDimension * .055f)
                }
                LogoVariant.Node -> {
                    val points = listOf(
                        Offset(center.x, size.height * .14f),
                        Offset(size.width * .18f, size.height * .72f),
                        Offset(size.width * .82f, size.height * .72f),
                        centerPoint,
                    )
                    drawLine(mint, points[0], points[1], thin)
                    drawLine(mint, points[0], points[2], thin)
                    drawLine(coral, points[1], points[2], thin)
                    points.forEachIndexed { index, point -> drawCircle(if (index == 3) coral else ink, size.minDimension * if (index == 3) .08f else .06f, point) }
                }
                LogoVariant.Arc -> {
                    drawArc(colors.ink, 205f, 250f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(mint, 205f, 185f, false, topLeft = Offset(size.width * .16f, size.height * .16f), size = Size(size.width * .68f, size.height * .68f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(coral, size.minDimension * .09f, Offset(size.width * .76f, size.height * .68f))
                }
            }
        }
    }
}

@Composable
private fun AlarmPreview(onDismiss: () -> Unit) {
    val colors = LocalAtomPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.quickCard) {
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                    Icon(Icons.Rounded.Close, "Close preview", tint = colors.quickCardText.copy(alpha = .7f))
                }
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AtomGlyph(LogoVariant.Original, Modifier.size(56.dp))
                    Spacer(Modifier.height(30.dp))
                    Text("12:00", color = colors.quickCardText, fontSize = 68.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-3).sp)
                    Text("PM · REMINDER", color = colors.mint, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(25.dp))
                    Text(
                        "Send product brief to Aisha",
                        color = colors.quickCardText,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Atom is ringing until you choose.", color = colors.quickCardText.copy(alpha = .55f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(42.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.mint, contentColor = Color(0xFF082017)),
                    ) {
                        Icon(Icons.Rounded.Check, null)
                        Spacer(Modifier.width(9.dp))
                        Text("Done")
                    }
                    Spacer(Modifier.height(11.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.quickCardText),
                        ) {
                            Icon(Icons.Rounded.Snooze, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Snooze")
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.quickCardText),
                        ) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Remind again")
                        }
                    }
                }
            }
        }
    }
}
