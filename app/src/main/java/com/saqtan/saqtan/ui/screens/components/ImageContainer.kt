package com.saqtan.saqtan.ui.screens.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap
import coil.compose.AsyncImagePainter.State.Empty.painter
import kotlinx.serialization.json.JsonNull.content

/** контейнер для изображения, используется в чате,
 * две версии для приема ImageBitmap, если изображение скачано, и painter, если локальное
 * */
@Composable
fun ImageContainer(
    imageBitmap: ImageBitmap,
    imageName: String = "",
    onCloseClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onSaveClick: (Bitmap) -> Unit = {}
) {
    Surface(
        modifier = modifier,
        elevation = 4.dp
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (image, closeButton) = createRefs()
            Image(
                modifier = Modifier
                    .constrainAs(image) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .heightIn(350.dp)
                    .widthIn(270.dp),
                bitmap = imageBitmap,
                contentDescription = "image",
                contentScale = ContentScale.Crop,
            )
            if (onCloseClick != null) {
                IconButton(onClick = onCloseClick, modifier = Modifier
                    .size(18.dp)
                    .constrainAs(closeButton) {
                        top.linkTo(parent.top, 8.dp)
                        end.linkTo(parent.end, 8.dp)
                    }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "delete image button"
                    )
                }
            } else {
                Surface(elevation = 6.dp, modifier = Modifier
                    .constrainAs(closeButton) {
                        top.linkTo(parent.top, 8.dp)
                        end.linkTo(parent.end, 8.dp)
                    }
                    .clickable { onSaveClick(imageBitmap.asAndroidBitmap()) }
                ) {
                        Icon(
                            modifier = Modifier
                                .size(23.dp),
                            imageVector = Icons.Filled.Download,
                            contentDescription = "delete image button"
                        )

                }
            }
        }
    }
}

@Composable
fun ImageContainer(
    painter: Painter,
    imageName: String = "",
    onCloseClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        elevation = 4.dp
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (image, closeButton) = createRefs()
            Image(
                modifier = Modifier
                    .constrainAs(image) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .heightIn(350.dp)
                    .widthIn(270.dp),
                painter = painter,
                contentDescription = "image",
                contentScale = ContentScale.Crop,
            )
            if (onCloseClick != null) {
                IconButton(onClick = onCloseClick, modifier = Modifier
                    .size(18.dp)
                    .constrainAs(closeButton) {
                        top.linkTo(parent.top, 8.dp)
                        end.linkTo(parent.end, 8.dp)
                    }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "delete image button"
                    )
                }
            } else {
//                val snapshot = CaptureBitmap {
//                    Canvas(Modifier.wrapContentSize()) {
//                        with(painter) {
//                            draw(painter.intrinsicSize)
//                        }
//                    }
//                }
                Surface(elevation = 6.dp, modifier = Modifier
                    .constrainAs(closeButton) {
                        top.linkTo(parent.top, 8.dp)
                        end.linkTo(parent.end, 8.dp)
                    }
                    .clickable { onSaveClick() }
                ) {
                    Icon(
                        modifier = Modifier
                            .size(23.dp),
                        imageVector = Icons.Filled.Download,
                        contentDescription = "delete image button"
                    )

                }

            }
        }
    }
}

@Composable
fun bitmapCapturer(painter: Painter): Bitmap {
    val snapshot = CaptureBitmap {
        Canvas(Modifier.wrapContentSize()) {
            with(painter) {
                draw(painter.intrinsicSize)
            }
        }
    }
    return snapshot.invoke()
}
@Composable
fun CaptureBitmap(
    content: @Composable () -> Unit,
): ()->Bitmap {

    val context = LocalContext.current

    /**
     * ComposeView that would take composable as its content
     * Kept in remember so recomposition doesn't re-initialize it
     **/
    val composeView = remember { ComposeView(context) }

    /**
     * Callback function which could get latest image bitmap
     **/
    fun captureBitmap(): Bitmap {
        return composeView.drawToBitmap()
    }

    /** Use Native View inside Composable **/
    AndroidView(
        factory = {
            composeView.apply {
                setContent {
                    content.invoke()
                }
            }
        }
    )

    /** returning callback to bitmap **/
    return ::captureBitmap
}




