package com.example.tasks

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var addReq by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("add", false)) addReq++
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { Screen(addReq) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("add", false)) addReq++
    }
}

@Composable
fun Screen(addReq: Int) {
    val c = LocalContext.current
    var tasks by remember { mutableStateOf(TaskStore.all(c)) }
    var cfg by remember { mutableStateOf(TaskStore.cfg(c)) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var adding by remember { mutableStateOf(false) }
    var showCfg by remember { mutableStateOf(false) }
    LaunchedEffect(addReq) { if (addReq > 0) adding = true }
    fun sync() { tasks = TaskStore.all(c); TasksWidget.refresh(c) }

    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showCfg = !showCfg }) { Text(stringResource(R.string.customize)) }
            }
            if (showCfg) CfgPanel(cfg) { cfg = it; TaskStore.saveCfg(c, it); TasksWidget.refresh(c) }

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(tasks, key = { it.id }) { t ->
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        val line = MaterialTheme.colorScheme.onSurface
                        val shape = RoundedCornerShape(5.dp)
                        Row(Modifier.fillMaxWidth().animateItem().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                            Text(
                                t.text, Modifier.weight(1f).alpha(if (t.done) .5f else 1f), fontSize = 17.sp,
                                textDecoration = if (t.done) TextDecoration.LineThrough else null
                            )
                            Text("✎", Modifier.clickable { editing = t }.padding(horizontal = 10.dp), color = line.copy(alpha = .55f), fontSize = 17.sp)
                            Text("✕", Modifier.clickable { TaskStore.delete(c, t.id); sync() }.padding(horizontal = 10.dp), color = line.copy(alpha = .55f), fontSize = 17.sp)
                            Box(
                                Modifier.padding(top = 2.dp).size(22.dp).clip(shape)
                                    .background(if (t.done) line.copy(alpha = .3f) else Color.Transparent)
                                    .border(1.2.dp, line, shape)
                                    .clickable { TaskStore.toggle(c, t.id); sync() },
                                contentAlignment = Alignment.Center
                            ) { if (t.done) Text("✔", fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Text("+", fontSize = 26.sp)
        }
    }
    if (adding) TaskDialog("", { TaskStore.add(c, it); sync() }) { adding = false }
    editing?.let { t -> TaskDialog(t.text, { TaskStore.edit(c, t.id, it); sync() }) { editing = null } }
}

@Composable
fun TaskDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.isEmpty()) R.string.new_task else R.string.edit_task)) },
        text = {
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.hint)) })
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onSave(text); onDismiss() }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun CfgPanel(cfg: Cfg, onChange: (Cfg) -> Unit) {
    val swatches = listOf(0xFFFFFF, 0x000000, 0x2563EB, 0x16A34A, 0xEC4899, 0xF59E0B, 0x64748B)
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF7DB4FF), Color(0xFFFFA6C9))))) {
            Box(Modifier.fillMaxSize().background(Color(0xFF000000.toInt() or cfg.rgb).copy(alpha = cfg.alpha / 100f)))
            Text("Aa  ✔", color = Color(cfg.textColor), fontSize = 18.sp, modifier = Modifier.align(Alignment.Center))
        }
        Text(stringResource(R.string.bg_color))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { rgb ->
                val on = cfg.rgb == rgb
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF000000.toInt() or rgb))
                    .border(if (on) 3.dp else 1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (on) 1f else .25f), CircleShape)
                    .clickable { onChange(cfg.copy(rgb = rgb, alpha = if (cfg.alpha == 0) 60 else cfg.alpha)) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${stringResource(R.string.opacity)} ${cfg.alpha}%", Modifier.weight(1f))
            TextButton(onClick = { onChange(cfg.copy(alpha = 0)) }) { Text(stringResource(R.string.transparent)) }
        }
        Slider(cfg.alpha.toFloat(), { onChange(cfg.copy(alpha = it.toInt())) }, valueRange = 0f..100f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.text_color), Modifier.weight(1f))
            FilterChip(cfg.darkText, { onChange(cfg.copy(darkText = true)) }, { Text(stringResource(R.string.dark)) })
            FilterChip(!cfg.darkText, { onChange(cfg.copy(darkText = false)) }, { Text(stringResource(R.string.light)) })
        }
    }
}
