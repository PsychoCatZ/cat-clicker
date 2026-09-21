package dev.psychocat.catclicker.assets

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

/**
 * Resolves the drawable names used by the :game data tables (e.g. "cat_basic_01") to resource ids.
 * Every drawable is kept from resource shrinking via res/raw/keep.xml because they are looked up by name.
 */
class AssetRegistry(private val context: Context) {
    private val cache = HashMap<String, Int>()

    @DrawableRes
    fun drawable(name: String): Int = cache.getOrPut(name) {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        require(id != 0) { "Missing drawable: $name" }
        id
    }
}

val LocalAssets = staticCompositionLocalOf<AssetRegistry> { error("AssetRegistry is not provided") }

/** The painter for a drawable name from the game data tables. */
@Composable
fun gameImage(name: String): Painter = painterResource(LocalAssets.current.drawable(name))
