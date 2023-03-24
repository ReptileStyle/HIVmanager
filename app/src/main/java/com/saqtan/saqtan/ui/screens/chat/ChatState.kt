package com.saqtan.saqtan.ui.screens.chat

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap

/** состояние чата
 * */
data class ChatState(
    val message:String = "",
    val allMessages:List<Message> = listOf(),
    val isLoading:Boolean = true,
    val imageUri:Uri? = null,
    val imageBitmap: ImageBitmap? = null,
    val isMessageLoading:Boolean = false
)

/** структура сообщения в чате
 * */
data class Message(
    val sender:String,
    val text:String,
    val time:Long,
    val imageBitmap: String
)