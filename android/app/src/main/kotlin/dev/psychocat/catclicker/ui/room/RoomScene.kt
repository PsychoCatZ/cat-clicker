package dev.psychocat.catclicker.ui.room

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.game.data.Cat
import dev.psychocat.catclicker.game.data.Furniture
import dev.psychocat.catclicker.game.data.FurniturePoint
import dev.psychocat.catclicker.game.data.FurnitureSurface
import dev.psychocat.catclicker.game.data.Room
import dev.psychocat.catclicker.game.data.SceneLayout
import dev.psychocat.catclicker.game.data.Upgrade
import dev.psychocat.catclicker.game.format.Numbers
import dev.psychocat.catclicker.game.model.RoomProgress
import kotlin.math.min

/**
 * Scene proportions of the web version (its "mobile" layout is about 370x430, its "desktop" layout about 1200x665).
 * Furniture coordinates are percentages of the scene, so the proportions must stay close to these.
 */
const val SCENE_ASPECT_PORTRAIT = 0.86f
const val SCENE_ASPECT_LANDSCAPE = 1.8f

/** A bought item and where it stands in the current layout (null: bought but not placed yet). */
data class SceneFurniture(val upgrade: Upgrade, val point: FurniturePoint?)

/** Present while the player arranges furniture. [selectedId] is the item that the next tap or drag moves. */
class FurnitureEdit(val selectedId: String?, val onPlace: (id: String, x: Double, y: Double) -> Unit)

/**
 * The room: background, furniture, the cat to tap and the hint line. Tapping a sleeping cat is allowed and only
 * explains why nothing happens (nothing in the game is ever a dead end for a beginner).
 *
 * While arranging ([edit] != null) the cat does not react; a tap puts the selected item at that spot and a drag
 * moves it. Both ways end in the same [FurnitureEdit.onPlace] call.
 */
@Composable
fun RoomScene(
    room: Room,
    cat: Cat?,
    progress: RoomProgress,
    clickReward: Double,
    aspectRatio: Float,
    layout: SceneLayout,
    furniture: List<SceneFurniture>,
    edit: FurnitureEdit?,
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

    val selected = furniture.firstOrNull { it.upgrade.id == edit?.selectedId }?.upgrade
    var draft by remember(selected?.id) { mutableStateOf<FurniturePoint?>(null) }

    val hint = when {
        edit != null && selected != null ->
            (if (selected.surface == FurnitureSurface.WALL) "Стена" else "Пол") + " · коснитесь места или перетащите предмет"
        edit != null -> "Выберите предмет в панели ниже"
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
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)
                .then(
                    if (edit != null && selected != null) {
                        Modifier
                            .pointerInput(selected.id, layout) {
                                detectTapGestures { tap ->
                                    edit.onPlace(selected.id, tap.x / size.width * 100.0, tap.y / size.height * 100.0)
                                }
                            }
                            .pointerInput(selected.id, layout) {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        draft = FurniturePoint(start.x / size.width * 100.0, start.y / size.height * 100.0)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        draft = FurniturePoint(
                                            change.position.x / size.width * 100.0,
                                            change.position.y / size.height * 100.0,
                                        )
                                    },
                                    onDragEnd = {
                                        draft?.let { edit.onPlace(selected.id, it.x, it.y) }
                                        draft = null
                                    },
                                    onDragCancel = { draft = null },
                                )
                            }
                    } else {
                        Modifier
                    },
                ),
        ) {
            val sceneWidth = maxWidth
            val sceneHeight = maxHeight

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

            if (edit != null && selected != null) PlacementGuide(selected.surface)

            furniture.forEach { item ->
                val upgrade = item.upgrade
                val isSelected = upgrade.id == selected?.id
                val dragged = if (isSelected) draft?.let { Furniture.constrainPoint(upgrade, layout, it) } else null
                val point = dragged ?: item.point ?: return@forEach
                val mobile = layout == SceneLayout.MOBILE
                val widthPct = if (mobile) min(35.0, upgrade.placement.width * 1.4) else upgrade.placement.width
                val heightPct = if (mobile) upgrade.placement.mobileHeight ?: upgrade.placement.height else upgrade.placement.height
                val width = sceneWidth * (widthPct / 100).toFloat()
                val height = sceneHeight * (heightPct / 100).toFloat()
                Box(
                    modifier = Modifier
                        .size(width, height)
                        .offset(x = sceneWidth * (point.x / 100).toFloat() - width / 2, y = sceneHeight * (point.y / 100).toFloat() - height / 2)
                        .then(
                            if (edit != null && isSelected) {
                                Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x55FFE082))
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Image(
                        painter = gameImage(upgrade.image),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (cat != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxHeight(0.82f)
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 44.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .then(
                            if (edit == null) {
                                Modifier
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
                                    }
                            } else {
                                Modifier
                            },
                        ),
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

            if (edit == null && taps > 0 && floating.value < 1f) {
                val p = floating.value
                Text(
                    "+${Numbers.format(clickReward)}",
                    modifier = Modifier.align(Alignment.Center).offset(y = (-(20f + 90f * p)).dp).graphicsLayer { alpha = 1f - p },
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
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

/** Dashed frame showing where the selected item may stand: wall items on the wall, floor items on the floor. */
@Composable
private fun PlacementGuide(surface: FurnitureSurface) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val top = if (surface == FurnitureSurface.WALL) 0.15f else 0.55f
        val bottom = if (surface == FurnitureSurface.WALL) 0.55f else 0.98f
        val topLeft = Offset(size.width * 0.02f, size.height * top)
        val frame = Size(size.width * 0.96f, size.height * (bottom - top))
        val corner = CornerRadius(16.dp.toPx())
        drawRoundRect(Color(0x19FFEFAD), topLeft, frame, corner)
        drawRoundRect(
            Color(0xAAFFF2AF), topLeft, frame, corner,
            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
        )
    }
}
