package com.saqtan.saqtan.ui.screens.chat

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.storage.FirebaseStorage
import com.saqtan.saqtan.R
import com.saqtan.saqtan.data.model.Response
import com.saqtan.saqtan.data.repository.UserRepository
import com.saqtan.saqtan.navigation.NavigationEvent
import com.saqtan.saqtan.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

val ONE_MEGABYTE: Long = 1024 * 1024
@HiltViewModel
class ChatViewModel  @Inject constructor(
    val auth: FirebaseAuth,
    val userRepository: UserRepository,
    @ApplicationContext
    val context: Context
    ): ViewModel() {

    var state by mutableStateOf(ChatState())
        private set

    private val _navigationEvent = Channel<NavigationEvent>()
    val uiEvent = _navigationEvent.receiveAsFlow()



    var patientID:String? = null

    var chatID = ""
    /** при инициализации начинаем загрузку сообщений из нужного чата, delay требуется для того, чтобы
     * patientID успел прийти извне(в графе навигации)
     * */
    init {
        viewModelScope.launch {
            if(userRepository.userType=="doctor"){
                while(patientID==null){
                    delay(25)
                }
            }
            chatID = if(patientID==null) "${auth.uid}${userRepository.userDoctorID}" else "${patientID}${auth.uid}"
            userRepository.getMessageList( chatID,{ onGetData(it) })
        }
    }
    /** обработка входящего события
     * */
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.OnMessageValueChange -> onMessageValueChange(event.message)
            ChatEvent.OnSendMessageButtonClick -> onSendMessageButtonClick()
            ChatEvent.OnReloadClick -> onReloadClick()
        }
    }
    /** функция обрабатывает полученный массив сообщений из базы,
     * сортирует по дате, после чего начинает загружать изображения
     * */
    private fun onGetData(list:MutableList<Message>){
        state = state.copy(
            allMessages = list.sortedByDescending { it.time },
            isLoading = false
        )
        Log.d("ChatViewModel","chat loaded")
        userRepository.setOnUpdateListener(
            chatID = chatID,
            onChildAddedListener = {getNewMessages(it)},
            onLoaded = {}
        )
        viewModelScope.launch {
            startDownLoadingImages()
        }
    }
    /** загрузка изображений с конца сообщений
     * */
    suspend fun startDownLoadingImages(){
        Log.d("ChatViewModel","start downloading images")
        for(i in state.allMessages.size-1 downTo 0) {
            val message = state.allMessages[i]
            Log.d("ChatViewModel","${message.imageBitmap}")
            if(message.imageBitmap.isNotEmpty()){
                state = state.copy(
                    images = state.images.plus(message.imageBitmap to downloadImage(message.imageBitmap))
                )
                Log.d("ChatViewModel","downloaded image")
            }
        }
    }
    /** обработка обновления чата, если в базе был установлен врач для текущего пользователя,
     * то откроется чат с ним
     * */
    private fun onReloadClick(){
        try {
            viewModelScope.launch {
                userRepository.loadUserData(auth.uid!!)
                _navigationEvent.send(NavigationEvent.Navigate(Route.chat,true))
            }
        }catch (e:Exception){
            Log.d("ChatViewModel","${e.message}")
        }
    }
    /** здесь хранится ссылка на текущее прикрепленное изображение
     * */
    fun setImageUri(uri: Uri?){
        state = state.copy(imageUri = uri)
    }
    /** здесь хранится текущее прикрепленное изображение
     * */
    fun setImageBitmap(imageBitmap: ImageBitmap?){
        state = state.copy(imageBitmap = imageBitmap)
    }
    /** загрузка изображения из базы в чат
     * разбита на два блока try-catch для двух случаев:
     * 1) что-то не так с базой
     * 2) не получилось обработать загруженный файл
     * */
    suspend fun downloadImage(imagePath:String):ImageBitmap?{
        try {
            val imageBytes = FirebaseStorage.getInstance().getReference(imagePath).getBytes(ONE_MEGABYTE).await()
            try {
                Log.d("ChatViewModel","try block")
                return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size).asImageBitmap()
            }catch (e:Exception){
                Log.d("ChatViewModel","${e.message}")
                return BitmapFactory.decodeResource(context.resources, R.drawable.image_not_available).asImageBitmap()
            }
        }catch (e:Exception){
            return BitmapFactory.decodeResource(context.resources, R.drawable.image_not_available).asImageBitmap()
        }
    }

    /** отправка сообщения
     * */
    private fun onSendMessageButtonClick(){
        if(state.message.isNotEmpty()||state.imageUri!=null) {
            viewModelScope.launch {
                userRepository.sendMessage(chatID, state.message, state.imageUri).collect{
                    when(it){
                        is Response.Loading ->{
                            state = state.copy(isMessageLoading = true)
                        }
                        is Response.Success ->{
                            state = state.copy(message = "", imageBitmap = null, imageUri = null, isMessageLoading = false)
                        }
                        is Response.Failure ->{
                            state = state.copy(isMessageLoading = false)
                            Toast.makeText(context,it.e.message,Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            }
        }
    }
    /** обработка изменения вводимого сообщения
     * */
    private fun onMessageValueChange(message:String){
        state = state.copy(message=message)
    }

    /** отправка события навигации
     * */
    fun sendNavigationEvent(event: NavigationEvent){
        viewModelScope.launch {
            _navigationEvent.send(event)
        }
    }
    /** функция сохранения изображения из чата в галерею
     * */
    fun saveMediaToStorage(bitmap: Bitmap,imageName:String) {
        userRepository.saveMediaToStorage(bitmap,imageName)
    }



    /** обработка появления новых сообщений в базе в реальном времени
     * прежде проходится по всем сообщения, но они пропускаются,
     * так как сначала чат прогружаем в другой функции
     * */
    private fun getNewMessages(snapshot: DataSnapshot){
        val iterator = snapshot.children.iterator()
        while(iterator.hasNext()){
            val senderID = iterator.next().value
            val image = iterator.next().value
            val message = iterator.next().value
            val time = iterator.next().value
            val myMessage = Message(senderID.toString(),message.toString(),time.toString().toLong(), image.toString())
            if(myMessage !in state.allMessages) {
                state = state.copy(
                  //  allMessages = state.allMessages.plus(myMessage)
                    allMessages = listOf(myMessage).plus(state.allMessages)
                )
            }
            Log.d("ChatViewModel","$message")
        }
    }
}