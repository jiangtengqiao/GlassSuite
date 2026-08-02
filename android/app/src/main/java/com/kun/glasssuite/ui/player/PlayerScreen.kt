package com.kun.glasssuite.ui.player

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kun.glasssuite.App
import com.kun.glasssuite.data.Api
import com.kun.glasssuite.data.AppConfig
import com.kun.glasssuite.data.Song
import com.kun.glasssuite.player.PlayerManager
import com.kun.glasssuite.ui.common.CoverImage
import com.kun.glasssuite.util.LrcParser
import com.kun.glasssuite.util.Utils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val scope = rememberCoroutineScope()
    val song by PlayerManager.currentSong.collectAsState()
    val playing by PlayerManager.playing.collectAsState()
    val position by PlayerManager.position.collectAsState()
    val duration by PlayerManager.duration.collectAsState()
    val quality by PlayerManager.quality.collectAsState()
    val repeatOne by PlayerManager.repeatOne.collectAsState()
    val shuffle by PlayerManager.shuffle.collectAsState()
    val queue by PlayerManager.queue.collectAsState()
    val index by PlayerManager.index.collectAsState()
    val liked by PlayerManager.likedIds.collectAsState()

    var showLyric by remember { mutableStateOf(true) }
    var showQuality by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyricSettings by remember { mutableStateOf(false) }

    val offsetMs = AppConfig.lyricOffsetMs
    val fontSize = AppConfig.lyricFontSize
    val mode = AppConfig.lyricMode

    // 当前歌词行
    val adjustedPos = position + offsetMs
    val lyricLines = rememberLyricLines(mode, song?.id)
    val currentIdx = remember(lyricLines, adjustedPos) {
        var idx = -1
        for (i in lyricLines.indices) {
            if (lyricLines[i].time <= adjustedPos) idx = i else break
        }
        idx
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            song?.name ?: "未在播放",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song?.artistNames ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 封面 / 歌词切换
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                FilterChip(
                    selected = !showLyric,
                    onClick = { showLyric = false },
                    label = { Text("封面") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = showLyric,
                    onClick = { showLyric = true },
                    label = { Text("歌词") },
                )
            }

            if (showLyric) {
                LyricList(
                    lines = lyricLines,
                    currentIdx = currentIdx,
                    fontSize = fontSize,
                    offsetMs = offsetMs,
                )
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CoverImage(song?.albumPic, Modifier.size(280.dp), corner = 16)
                }
            }

            // 进度
            Slider(
                value = position.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                onValueChange = { PlayerManager.seekTo(it.toLong()) },
                valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(Utils.formatDuration(position), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text(Utils.formatDuration(duration), style = MaterialTheme.typography.labelSmall)
            }

            // 主控制
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { PlayerManager.toggleRepeatOne() }) {
                    Icon(
                        Icons.Default.Repeat,
                        "单曲循环",
                        tint = if (repeatOne) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { PlayerManager.prev() }) {
                    Icon(Icons.Default.SkipPrevious, "上一首", Modifier.size(36.dp))
                }
                IconButton(
                    onClick = { PlayerManager.toggle() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                ) {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "播放/暂停",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = { PlayerManager.next() }) {
                    Icon(Icons.Default.SkipNext, "下一首", Modifier.size(36.dp))
                }
                IconButton(onClick = { PlayerManager.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        "随机播放",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 点赞 / 音质 / 歌词设置 / 队列
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val curSong = song
                val isLiked = curSong?.let { s -> s.id in liked } ?: false
                IconButton(onClick = { song?.let { PlayerManager.toggleLike(it.id) } }) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "喜欢",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showQuality = true }) {
                    Text("音质：${Api.QUALITY_NAMES[quality] ?: quality}")
                }
                TextButton(onClick = { showLyricSettings = true }) {
                    Icon(Icons.Default.Lyrics, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("歌词设置")
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "播放队列")
                }
            }
        }
    }

    if (showQuality) {
        QualityDialog(
            current = quality,
            onSelect = { PlayerManager.setQuality(it); showQuality = false },
            onDismiss = { showQuality = false },
        )
    }
    if (showQueue) {
        QueueDialog(
            queue = queue,
            currentIndex = index,
            onPlay = { i -> PlayerManager.playQueue(queue, i) },
            onDismiss = { showQueue = false },
        )
    }
    if (showLyricSettings) {
        LyricSettingsDialog(
            mode = mode,
            fontSize = fontSize,
            offsetMs = offsetMs,
            diyLyric = AppConfig.diyLyric,
            onModeChange = { m ->
                AppConfig.lyricMode = m
                scope.launch { app.settings.setLyricMode(m) }
            },
            onFontSizeChange = { s ->
                AppConfig.lyricFontSize = s
                scope.launch { app.settings.setLyricFontSize(s) }
            },
            onOffsetChange = { o ->
                AppConfig.lyricOffsetMs = o
                scope.launch { app.settings.setLyricOffset(o) }
            },
            onDiyChange = { text ->
                AppConfig.diyLyric = text
                scope.launch { app.settings.setDiyLyric(text) }
            },
            onDismiss = { showLyricSettings = false },
        )
    }
}

/** 根据歌词模式生成展示行 */
@Composable
private fun rememberLyricLines(mode: Int, songId: Long?): List<com.kun.glasssuite.util.LrcLine> {
    val lyric by PlayerManager.lyric.collectAsState()
    val tlyric by PlayerManager.tlyric.collectAsState()
    val romalrc by PlayerManager.romalrc.collectAsState()
    return remember(lyric, tlyric, romalrc, mode, songId) {
        when (mode) {
            1 -> {
                val map = LrcParser.align(tlyric, lyric)
                lyric.map { it.copy(text = it.text + if (map[it.time].isNullOrBlank()) "" else "\n${map[it.time]}") }
            }
            2 -> if (romalrc.isNotEmpty()) romalrc else lyric
            3 -> {
                val diy = LrcParser.parse(AppConfig.diyLyric)
                if (diy.isNotEmpty()) diy else lyric
            }
            else -> lyric
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.LyricList(
    lines: List<com.kun.glasssuite.util.LrcLine>,
    currentIdx: Int,
    fontSize: Int,
    offsetMs: Int,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIdx) {
        if (currentIdx >= 0) {
            listState.animateScrollToItem((currentIdx - 3).coerceAtLeast(0))
        }
    }
    if (lines.isEmpty()) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text("暂无歌词，可点击「歌词设置」导入 DIY 歌词")
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(lines) { i, line ->
            val isCurrent = i == currentIdx
            Text(
                line.text.ifBlank { "♪" },
                fontSize = (if (isCurrent) fontSize + 4 else fontSize).sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { PlayerManager.seekTo(line.time - offsetMs) }
                    .padding(vertical = 10.dp, horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun QualityDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("音质选择") },
        text = {
            Column {
                Api.QUALITY_LEVELS.forEach { level ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(level) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = level == current, onClick = { onSelect(level) })
                        Spacer(Modifier.width(8.dp))
                        Text(Api.QUALITY_NAMES[level] ?: level)
                    }
                }
                Text(
                    "无损音质需要登录且具备相应会员权益；\n受限曲目将自动降档播放。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun QueueDialog(
    queue: List<Song>,
    currentIndex: Int,
    onPlay: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("播放队列（${queue.size}）") },
        text = {
            LazyColumn(Modifier.height(320.dp)) {
                itemsIndexed(queue) { i, s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(i) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            (i + 1).toString().padStart(2, '0'),
                            color = if (i == currentIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                s.name ?: "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (i == currentIndex) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(
                                s.artistNames,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun LyricSettingsDialog(
    mode: Int,
    fontSize: Int,
    offsetMs: Int,
    diyLyric: String,
    onModeChange: (Int) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onOffsetChange: (Int) -> Unit,
    onDiyChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var diyText by remember(diyLyric) { mutableStateOf(diyLyric) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("歌词设置") },
        text = {
            Column(Modifier.height(380.dp).verticalScrollState()) {
                Text("显示模式", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.padding(vertical = 6.dp)) {
                    listOf(0 to "原词", 1 to "翻译", 2 to "罗马音", 3 to "DIY").forEach { (m, label) ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { onModeChange(m) },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Text("歌词时间偏移（当前 ${offsetMs}ms）", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Button(onClick = { onOffsetChange(offsetMs - 500) }) { Text("-0.5s") }
                    Button(onClick = { onOffsetChange(0) }) { Text("复位") }
                    Button(onClick = { onOffsetChange(offsetMs + 500) }) { Text("+0.5s") }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Text("歌词字号（${fontSize}sp）", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Button(onClick = { onFontSizeChange((fontSize - 2).coerceAtLeast(12)) }) { Text("A-") }
                    Button(onClick = { onFontSizeChange(18) }) { Text("默认") }
                    Button(onClick = { onFontSizeChange((fontSize + 2).coerceAtMost(30)) }) { Text("A+") }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Text("DIY 歌词（LRC 格式）", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = diyText,
                    onValueChange = { diyText = it },
                    placeholder = { Text("[00:01.00] 示例歌词") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
                TextButton(onClick = { onDiyChange(diyText) }) { Text("应用 DIY 歌词") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

private fun Modifier.verticalScrollState(): Modifier = this
