package dev.psychocat.catclicker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.psychocat.catclicker.assets.gameImage
import dev.psychocat.catclicker.ui.theme.ArtBackground
import dev.psychocat.catclicker.ui.theme.CardBorder
import dev.psychocat.catclicker.ui.theme.CreamCard
import dev.psychocat.catclicker.ui.theme.Success

/** Minimum touch target for every button in the game (Android recommends 48 dp; we go larger on purpose). */
val MinTouchHeight = 60.dp

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.Primary,
    /** Tighter side padding for two buttons in one row, so that a whole word fits on one line. */
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    val shape = RoundedCornerShape(16.dp)
    val sized = modifier.heightIn(min = MinTouchHeight)
    when (style) {
        ButtonStyle.Primary -> Button(onClick, sized, enabled, shape, contentPadding = contentPadding) { ButtonText(text) }
        ButtonStyle.Tonal -> FilledTonalButton(onClick, sized, enabled, shape, contentPadding = contentPadding) { ButtonText(text) }
        ButtonStyle.Outlined -> OutlinedButton(onClick, sized, enabled, shape, contentPadding = contentPadding) { ButtonText(text) }
        ButtonStyle.Danger -> Button(
            onClick, sized, enabled, shape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            contentPadding = contentPadding,
        ) { ButtonText(text) }
    }
}

enum class ButtonStyle { Primary, Tonal, Outlined, Danger }

@Composable
private fun ButtonText(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

/** A "Buy" button with the price and the fish icon on the right. */
@Composable
fun PriceButton(label: String, price: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = MinTouchHeight),
        shape = RoundedCornerShape(16.dp),
    ) {
        // Two lines so that the label and a long price both fit into a narrow two-column card.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(gameImage("resource_01"), contentDescription = null, modifier = Modifier.size(28.dp))
                Text(" $price", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun StatusLabel(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().heightIn(min = MinTouchHeight), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Success, textAlign = TextAlign.Center)
    }
}

@Composable
fun ItemCard(modifier: Modifier = Modifier, highlighted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = CreamCard,
        border = BorderStroke(if (highlighted) 3.dp else 1.dp, if (highlighted) MaterialTheme.colorScheme.primary else CardBorder),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

/** Card picture area: the item image on a warm background. */
@Composable
fun CardArt(imageName: String, description: String?, height: Dp = 130.dp) {
    Box(
        modifier = Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(14.dp)).background(ArtBackground),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = gameImage(imageName),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f),
        )
    }
}

@Composable
fun CardTexts(badge: String?, title: String, body: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (badge != null) {
            Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (body != null) {
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Lays [entries] out in rows of [columns] equally wide, equally tall cards, as items of a lazy list. */
fun <T> LazyListScope.cardGrid(entries: List<T>, columns: Int, keyOf: (T) -> Any, card: @Composable (T, Modifier) -> Unit) {
    items(entries.chunked(columns), key = { row -> row.joinToString("|") { keyOf(it).toString() } }) { row ->
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { item -> card(item, Modifier.weight(1f).fillMaxHeight()) }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun SectionHeading(eyebrow: String, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
