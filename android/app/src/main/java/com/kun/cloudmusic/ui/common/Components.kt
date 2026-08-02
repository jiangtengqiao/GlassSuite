package com.kun.cloudmusic.ui.common

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kun.cloudmusic.data.Playlist
import com.kun.cloudmusic.data.Song
import com.kun.cloudmusic.player.PlayerManager
import com.kun.cloudmusic.util.Utils

@Composable
fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    corner: Int = 8,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(corner.dp)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** 歌曲列表行 */
@Composable
fun SongRow(
    song: Song,
    index: Int = -1,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (index >= 0) {
            Text(
                (index + 1).toString().padStart(2, '0'),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.name ?: "未知歌曲",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                song.artistNames,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** 歌单卡片 */
@Composable
fun PlaylistCard(playlist: Playlist, onClick: () -> Unit, width: Int = 120) {
    Column(
        modifier = Modifier
            .width(width.dp)
            .clickable(onClick = onClick)
    ) {
        Box {
            CoverImage(playlist.coverImgUrl, Modifier.size(width.dp), corner = 8)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f),
                        RoundedCornerShape(topStart = 6.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    Utils.formatPlayCount(playlist.playCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            playlist.name ?: "",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 底部迷你播放条 */
@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val song by PlayerManager.currentSong.collectAsState()
    val playing by PlayerManager.playing.collectAsState()
    if (song == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(song?.albumPic, Modifier.size(44.dp), corner = 6)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song?.name ?: "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song?.artistNames ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { PlayerManager.toggle() }) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "暂停" else "播放",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
