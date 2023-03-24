package com.saqtan.saqtan.ui.screens.chat

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.memory.MemoryCache
import com.saqtan.saqtan.R
import com.saqtan.saqtan.navigation.NavigationEvent
import com.saqtan.saqtan.ui.screens.components.BottomNavBar
import com.saqtan.saqtan.ui.screens.components.ImageContainer
import com.saqtan.saqtan.ui.screens.components.LoadingGif
import com.saqtan.saqtan.ui.screens.components.MyTopAppBar
import com.saqtan.saqtan.ui.theme.HIVmanagerTheme
import com.saqtan.saqtan.ui.theme.White200
import com.saqtan.saqtan.ui.theme.White500
import kotlinx.coroutines.flow.collect

@Composable
fun ChatScreen(
    onNavigate: (route: String, popBackStack: Boolean) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    isDoctor: Boolean = false
) {
    val activity = (LocalContext.current as Activity)


    LaunchedEffect(key1 = true) {
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        viewModel.uiEvent.collect {
            when (it) {
                is NavigationEvent.Navigate -> {
                    onNavigate(it.route, it.popBackStack)
                }
                is NavigationEvent.NavigateUp -> {
                    onNavigateUp()
                }
                else -> {}
            }
        }
    }
    val context = LocalContext.current

    /** обработка загрузки фотографии из локального хранилища на экран(не в базу)
     * */
    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("photo", "launcher entered")
        viewModel.setImageUri(uri)
        var bitmap: Bitmap? = null
        viewModel.state.imageUri?.let {
            bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images
                    .Media.getBitmap(context.contentResolver, it)

            } else {
                val source = ImageDecoder
                    .createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
        }
        viewModel.setImageBitmap(bitmap?.asImageBitmap())
    }

    /** загружаем чат, если пользователю назвачен врач или же он сам является врачом
     * */
    if ((viewModel.userRepository.userDoctorID != "null" && viewModel.userRepository.userDoctorID.isNotEmpty()) || viewModel.userRepository.userType == "doctor")
        ChatScreenUi(
            navigationEventSender = {
                activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED)
                viewModel.sendNavigationEvent(it)
            },
            textFieldValue = viewModel.state.message,
            onTextFieldValueChange = { viewModel.onEvent(ChatEvent.OnMessageValueChange(it)) },
            onSendMessageButtonClick = { viewModel.onEvent(ChatEvent.OnSendMessageButtonClick) },
            messageList = viewModel.state.allMessages,
            userID = viewModel.auth.uid,
            // lazyListState = viewModel.lazyColumnScrollState,
            isLoading = viewModel.state.isLoading,
            isDoctor = isDoctor,
            onAddImageClick = { launcher.launch("image/*") },
            imageBitmap = viewModel.state.imageBitmap,
            onDeleteImageClick = {
                viewModel.setImageBitmap(null)
                viewModel.setImageUri(null)
            },
            isMessageLoading = viewModel.state.isMessageLoading,
            onSaveImageClick = { image, name -> viewModel.saveMediaToStorage(image, name) }
        )
    else {
        ChatNowAvailableUi(
            bottomNavBarNavigationEventSender = { viewModel.sendNavigationEvent(it) },
            onReloadClick = { viewModel.onEvent(ChatEvent.OnReloadClick) }
        )
    }
}

@Composable
private fun ChatNowAvailableUi(
    bottomNavBarNavigationEventSender: (NavigationEvent) -> Unit = {},
    onReloadClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { MyTopAppBar("Хабарламалар") },
        bottomBar = {
            BottomNavBar(bottomNavBarNavigationEventSender, 2)
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Біз әлі сізді дәрігерге тіркемедік, \"sadairu11@gmail.com\" бойынша хабарласыңыз",
                    modifier = Modifier.width(200.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
                TextButton(onClick = onReloadClick) {
                    Text(
                        text = "Жаңарту",
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        }
    }
}


@Composable
private fun ChatScreenUi(
    navigationEventSender: (NavigationEvent) -> Unit = {},
    textFieldValue: String = "",
    onTextFieldValueChange: (String) -> Unit = {},
    onSendMessageButtonClick: () -> Unit = {},
    messageList: List<Message> = listOf(),
    userID: String? = "",
    lazyListState: LazyListState = rememberLazyListState(),
    isLoading: Boolean = false,
    isDoctor: Boolean = false,
    onAddImageClick: () -> Unit = {},
    onDeleteImageClick: () -> Unit = {},
    imageBitmap: ImageBitmap? = null,
    isMessageLoading: Boolean = false,
    onSaveImageClick: (Bitmap, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var hasSaveFilePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasSaveFilePermission = isGranted
        }
    )
    Scaffold(
        topBar = {
            if (!isDoctor)
                MyTopAppBar("Хабарламалар")
            else
                MyTopAppBar(
                    "Хабарламалар",
                    onBackClick = { navigationEventSender(NavigationEvent.NavigateUp) })
        },
        bottomBar = {
            Column() {
                if (imageBitmap != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        ImageContainer(
                            imageBitmap = imageBitmap, onCloseClick = onDeleteImageClick,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(76.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = textFieldValue,
                        onValueChange = onTextFieldValueChange,
                        leadingIcon = {
                            IconButton(onClick = onAddImageClick) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = "attach image",
                                    tint = MaterialTheme.colors.primaryVariant
                                )
                            }
                        },
                        trailingIcon = {
                            if (isMessageLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colors.primaryVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                IconButton(onClick = onSendMessageButtonClick) {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = "send message",
                                        tint = MaterialTheme.colors.primaryVariant
                                    )
                                }
                            }
                        },
                        placeholder = { Text(text = "Сіздің хабарлама...") }
                    )
                }
                if (!isDoctor) {
                    BottomNavBar(navigationEventSender, 2)
                }
            }

        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingGif(Modifier.size(60.dp))
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    reverseLayout = true
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(messageList) { index, message ->
                        MessageContainer(
                            message = message,
                            userID = userID,
                            onSaveClick = {
                                onSaveImageClick(
                                    it,
                                    message.imageBitmap.substringAfterLast('/')
                                )
                            },
                            hasSaveFilePermission = hasSaveFilePermission,
                            askForPermission = {launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)}
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageContainer(
    message: Message,
    userID: String?,
    onSaveClick: (Bitmap) -> Unit,
    hasSaveFilePermission: Boolean,
    askForPermission:()->Unit
) {

    Row(
        horizontalArrangement = if (message.sender == userID) Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 270.dp),
            elevation = 4.dp,
            color = if (message.sender == userID) MaterialTheme.colors.primary else MaterialTheme.colors.primaryVariant,
            shape = RoundedCornerShape(5.dp),

            ) {
            Column(

            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                        horizontal = 10.dp
                    ),
                    color = if (isSystemInDarkTheme()) White500 else White200
                )
                if (message.imageBitmap.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = message.imageBitmap,
                        modifier = Modifier
                            .width(270.dp)
                            .heightIn(max = 350.dp, min = 200.dp),
                        contentDescription = null
                    ) {
                        val state = painter.state
                        if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                            Box(
                                modifier = Modifier
                                    .width(270.dp)
                                    .height(200.dp), contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(32.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF252525)
                                )
                            }
                        } else {
                            ImageContainer(painter = this.painter.state.painter!!, onSaveClick = {
                                if(hasSaveFilePermission) {
                                    val key = message.imageBitmap
                                    val value =
                                        this.painter.imageLoader.memoryCache?.get(
                                            MemoryCache.Key(
                                                key
                                            )
                                        )
                                    value?.bitmap?.let { onSaveClick(it) }
                                }else{
                                    askForPermission()
                                }
                            })
                            Log.d("asdf", "${this.painter.request.data} ")
                            this.painter.request.diskCacheKey
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChatScreenPreview() {
    HIVmanagerTheme {
        ChatScreenUi()
    }
}

@Preview
@Composable
private fun ChatNotAvailablePreview() {
    HIVmanagerTheme {
        ChatNowAvailableUi()
    }
}