package dev.psychocat.catclicker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import dev.psychocat.catclicker.R
import kotlinx.coroutines.delay

/** How long the title picture stays on screen at the start of the game. A tap ends it sooner. */
const val SPLASH_MILLIS = 4000L

/**
 * The picture shown when the game starts. It fills the screen in portrait (a little of its edges may be cropped on
 * very tall phones) and is shown whole in landscape. Nothing else happens meanwhile: the game is already loaded
 * behind it, and any "the cats waited for you" message appears only after the picture is gone.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SPLASH_MILLIS)
        onFinished()
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E1B10))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = "Пропустить заставку") {
                onFinished()
            },
    ) {
        Image(
            painter = painterResource(R.drawable.splash),
            contentDescription = "Котокликер — добрая игра про котиков",
            contentScale = if (maxHeight > maxWidth) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
