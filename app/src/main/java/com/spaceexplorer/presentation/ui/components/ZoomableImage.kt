package com.spaceexplorer.presentation.ui.components

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.spaceexplorer.presentation.ui.theme.SpaceExplorerTheme

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

@Composable
fun ZoomableImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        offset = if (scale > MIN_SCALE) offset + panChange else Offset.Zero
    }

    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .transformable(state = transformableState)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ZoomableImagePreview() {
    SpaceExplorerTheme {
        ZoomableImage(
            url = "",
            contentDescription = "Preview",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}
