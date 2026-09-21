package dev.psychocat.catclicker.ui.room

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Cat
import dev.psychocat.catclicker.game.data.Room
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.game.model.RoomProgress

/** Portrait phones use the web version's "mobile" furniture layout, landscape and tablets the "desktop" one. */
const val SCENE_ASPECT_PORTRAIT = 0.9f
const val SCENE_ASPECT_LANDSCAPE = 1.35f

/**
 * The room: background, the cat to tap and the hint line. Tapping a sleeping cat is allowed and only explains
 * why nothing happens (nothing in the game is ever a dead end for a beginner).
 */
@Composable
fun RoomScene(
    room: Room,
    cat: Cat?,
    progress: RoomProgress,
    clickReward: Double,
    aspectRatio: Float,
    onCatTap: () -> Unit,
    onSleepingTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sleeping = progress.hunger <= 0 || progress.lightsOff
    var taps by remember { mutableIntStateOf(0) }
    val floating = remember { Animatable(1f) }
    LaunchedEffect(taps) {
        if (taps > 0) {
            floating.snapTo(0f)
            floating.animateTo(1f, tween(durationMillis = 800))
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !sleeping) 0.94f else 1f, label = "catPress")

    val hint = when {
        progress.hunger <= 0 -> "Кот уснул. Купите корм — пассивный доход остаётся"
        progress.lightsOff -> "Свет выключен. Кот спит, сытость не тратится"
        else -> "Нажимайте на кота, чтобы собирать рыбок"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(5.dp, Color(0xFFFFFAF2)),
        shadowElevation = 6.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)) {
            Image(
                painter = gameImage(if (sleeping) room.nightImage else room.dayImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0.02f, 0f),
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color(0x00000000), 0.7f to Color(0x00000000), 1f to Color(0x66281308)),
                ),
            )

            if (cat != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxHeight(0.82f)
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 44.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .semantics {
                            contentDescription = if (sleeping) {
                                "${cat.name} спит. " + if (progress.hunger <= 0) "Купите корм" else "Включите свет"
                            } else {
                                "Нажать на кота ${cat.name} и получить ${Numbers.format(clickReward)} рыбок"
                            }
                        }
                        .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                            if (sleeping) {
                                onSleepingTap()
                            } else {
                                taps += 1
                                onCatTap()
                            }
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Image(
                        painter = gameImage(if (sleeping) cat.sleepingImage else cat.image),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                        .clip(RoundedCornerShape(16.dp)).background(Color(0xEDFFF9E9)).padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("СЕЙЧАС С ВАМИ", style = MaterialTheme.typography.labelMedium, color = Color(0xFFA36C53))
                    Text(cat.name, style = MaterialTheme.typography.titleLarge, color = Color(0xFF4E2D20))
                }
            }

            if (taps > 0 && floating.value < 1f) {
                val p = floating.value
                Text(
                    "+${Numbers.format(clickReward)}",
                    modifier = Modifier.align(Alignment.Center).offset(y = (-(20f + 90f * p)).dp).graphicsLayer { alpha = 1f - p },
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFFFFF3C4),
                )
            }

            Text(
                hint,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xCC45271B)).padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFF9EC),
                textAlign = TextAlign.Center,
            )
        }
    }
}
