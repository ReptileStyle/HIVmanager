package com.saqtan.saqtan.data.repository

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.datastore.dataStore
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.saqtan.saqtan.App
import com.saqtan.saqtan.data.model.*
import com.saqtan.saqtan.data.model.notification.AlarmScheduler
import com.saqtan.saqtan.data.model.notification.NotificationHelper
import com.saqtan.saqtan.ui.screens.chat.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


val Context.dataStore by dataStore("data.json", UserDataSerializer)

/** класс для взаимодействия с данными пользователя
 * локальными и данными из базы
 * */
@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext
    val context: Context,
    private val database: FirebaseDatabase,
    val notificationHelper: NotificationHelper,
    val scheduler: AlarmScheduler,
) {
    var userType: String = ""
    var userDoctorID: String = ""
    var patientList: MutableList<String> = mutableListOf()
    var userDataFlow: Flow<UserData> = context.dataStore.data

    /** получить локальные данные о пользователе
     * */
    suspend fun loadUserLocalData(): UserData {
        try {
            return context.dataStore.data.first()
        } catch (e: Exception) {
            Log.d("UserRepository", "${e.message}")
        }
        return UserData()
    }

    /** получить данные пользователя из базы
     * */
    suspend fun loadUserDataFromDatabase(uid: String): UserData {//for doctor usage
        try {
            val result = firestore.collection("users").document(uid).get().await()
            return constructUserDataFromFirestore(result)
        } catch (e: Exception) {
            //    Toast.makeText(context,"${e.message}",Toast.LENGTH_LONG).show()
            return UserData()
        }
    }

    /** получить из базы данные о том, является ли пользователь врачом
     * если да, то получить список пациентов, эти данные хранятся только в базе
     * */
    suspend fun loadUserData(uid: String) {
        val result = firestore.collection(Constants.USERS).document(uid).get().await()
        userType = result.get("type").toString()
        userDoctorID = result.get("doctor").toString()
        if (userType == "doctor") {
            val patients =
                firestore.collection(Constants.USERS).document(uid).collection("patients").get()
                    .await()
            for (patient in patients) {
                patientList.add(patient.id)
            }
        }
    }

    /** при входе данные из базы перезаписывают локальные данные
     *  выставляются все напоминания, полученные из базы
     * */
    suspend fun onSignIn(uid: String) {//load data and save it locally on sign in
        context.dataStore.updateData { loadUserDataFromDatabase(uid) }
        for (pillInfo in loadUserDataFromDatabase(auth.uid!!).pillInfoList) {
            notificationHelper.createNotificationRequest(pillInfo, scheduler)
        }
    }

    /** при выходе локальные данные заменяются на пустые,
     * все напоминания отменяются
     * */
    suspend fun onSignOut() {
        for (pillInfo in loadUserDataFromDatabase(auth.uid!!).pillInfoList) {
            notificationHelper.cancelNotifications(pillInfo, scheduler)
        }
        context.dataStore.updateData { UserData() }
        auth.signOut()
    }

    /** эта функция проходит по списку пациентов врача и загружает их последние сообщения
     * полученные сообщения отправляются в функцию listAdder в формате "uid,message"
     * функция onChildAdded срабатывает сначала на все данные, а затем всякий раз, когда добавляются новые
     * таким образом, последнее сообщение отображается в реальном времени
     * */
    fun loadLastMessages(
        scope: CoroutineScope,
        listAdder: (String, String) -> Unit
    ) {//only for doctor usage
        for (patient in patientList) {
            scope.launch {
                Log.d("userRepository", "${patient}${auth.uid}")
                try {
                    val lastMessage =
                        database.getReference("messages").child("${patient}${auth.uid}")
                            .orderByKey().limitToLast(1)
                    lastMessage.addChildEventListener(object : ChildEventListener {
                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            listAdder(patient, snapshot.child("message").value.toString())
                        }

                        //остальные функции не использованы, так как в них нет необходимости
                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {

                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {

                        }

                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {

                        }

                        override fun onCancelled(error: DatabaseError) {

                        }

                    })


                } catch (e: NullPointerException) {
                    Log.d("userRepository", "exception")
                }
            }
        }

    }

    /**добавляет пользователя в базу, если его там нет
     * использовается при входе
     */

    fun addUserToDatabase(uid: String?) {
        var ifExists = false
        if (uid == null) return
        firestore.collection(Constants.USERS).document(uid).get().addOnCompleteListener() { task ->
            if (task.result.exists()) ifExists = true
            if (!ifExists) {
                firestore.collection(Constants.USERS).document(uid).set(
                    mapOf("type" to "user", "doctor" to "null", "uid" to auth.uid),
                    SetOptions.merge()
                )
            }
        }
    }

    /** отправляет код подтверждения пользователю
     * */

    fun sendVerificationCode(
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        activity: Activity,
        resendToken: ForceResendingToken? = null
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(activity)
            .setCallbacks(callbacks)
            .let { if (resendToken != null) it.setForceResendingToken(resendToken) else it }
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /** обновляет данные пользователя в базе(для синхронизации данных в базе и локальных)
     * */
    suspend fun updateUserDataOnDatabase() {
        try {
            Log.d("userRepository", "updating base, userType = $userType")
            if (userType == "user")
                firestore.collection("users").document(auth.uid!!).update(
                    mapOf("data" to context.dataStore.data.first()),
                )
        } catch (e: Exception) {
            Log.d("userRepository", "${e.message}")
        }
    }

    /** удаляет напоминание из локального хранилища и обновляет данные в базе
     * */
    suspend fun deletePillInfo(index: Int) {
        Log.d("userRepository", "before update local")
        context.dataStore.updateData {
            it.copy(pillInfoList = it.pillInfoList.minus(it.pillInfoList[index]))
        }
        Log.d("userRepository", "before update in base")
        updateUserDataOnDatabase()
        //  userData = userData.copy(pillInfoList = userData.pillInfoList.minus(userData.pillInfoList[index]))
    }

    /** добавляет напоминание в локальное хранилище и обновляет данные в базе
     * */
    suspend fun addPillInfo(pillInfo: PillInfo) {
        context.dataStore.updateData {
            it.copy(pillInfoList = it.pillInfoList.plus(pillInfo))
        }
        updateUserDataOnDatabase()
        //  userData = userData.copy(pillInfoList = userData.pillInfoList.plus(pillInfo))

    }

    /** добавляет запись дневника наблюдений в локальное хранилище и обновляет данные в базе
     * */
    suspend fun addDiaryEntry(diaryEntry: DiaryEntry) {
        context.dataStore.updateData {
            it.copy(diaryEntries = listOf(diaryEntry).plus(it.diaryEntries))
        }
        updateUserDataOnDatabase()
        //userData = userData.copy(diaryEntries = userData.diaryEntries.plus(diaryEntry))
    }

    /** удаляет запись дневника наблюдений из локального хранилища и обновляет данные в базе
     * */
    suspend fun deleteDiaryEntry(diaryEntry: DiaryEntry) {
        context.dataStore.updateData {
            it.copy(diaryEntries = it.diaryEntries.minus(diaryEntry))
        }
        updateUserDataOnDatabase()
        // userData = userData.copy(diaryEntries = userData.diaryEntries.minus(diaryEntry))
    }

    /** добавляет рост в локальное хранилище и обновляет данные в базе
     * */

    suspend fun setHeight(height: Int) {
        context.dataStore.updateData {
            it.copy(height = height)
        }
        updateUserDataOnDatabase()
    }

    /** добавляет аллергии в локальное хранилище и обновляет данные в базе
     * */
    suspend fun setAllergies(allergies: String) {
        context.dataStore.updateData {
            it.copy(allergies = allergies)
        }
        updateUserDataOnDatabase()
    }


    /** отправляет сообщение в чат
     * id чата формируется как соединение id пользователя и id врача
     *  поддерживается загрузка изображений
     * */
    suspend fun sendMessage(
        chatID: String,
        message: String,
        imageUri: Uri?
    ): Flow<Response<Boolean>> {
        return flow {
            emit(Response.Loading)
            try {
                val ref = database.getReference("messages").child(chatID).push()
                var link: Uri? = null
                if (imageUri != null)
                    link = uploadImage(imageUri, chatID, ref.key!!)
                ref.updateChildren(
                    mapOf(
                        "message" to message,
                        "time" to "${com.google.firebase.Timestamp.now().seconds}",
                        "author" to "${auth.uid}",
                        "image" to if (imageUri == null) "" else link.toString()
                    )
                )
                emit(Response.Success(true))
            } catch (e: Exception) {
                emit(Response.Failure(e))
            }

        }
    }

    /** эта функция позволяет получать сообщения в реальном времени, методы, кроме onChildAdded
     * не реализованы, так как в них нет нужды
     * */
    fun setOnUpdateListener(
        chatID: String,
        onChildAddedListener: (DataSnapshot) -> Unit,
        onLoaded: () -> Unit
    ) {
        database.getReference("messages").child(chatID).addChildEventListener(
            object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    onChildAddedListener(snapshot)
                    onLoaded()
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    //  TODO("Not yet implemented")
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    //  TODO("Not yet implemented")
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    //TODO("Not yet implemented")
                }

                override fun onCancelled(error: DatabaseError) {
                    //  TODO("Not yet implemented")
                }

            }
        )

    }

    /** функция для загрузки сообщений в чат
     * */
    fun getMessageList(chatID: String, onGetData: (MutableList<Message>) -> Unit) {
        val messageList: MutableList<Message> = mutableListOf()
        database.getReference("messages").child(chatID).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val iterator = child.children.iterator()
                        while (iterator.hasNext()) {
                            val senderID = iterator.next().value
                            val image = iterator.next().value
                            val message = iterator.next().value
                            val time = iterator.next().value
                            messageList.add(
                                Message(
                                    senderID.toString(),
                                    message.toString(),
                                    time.toString().toLong(),
                                    image.toString()
                                )
                            )
                        }
                    }
                    onGetData(messageList)
                }

                override fun onCancelled(error: DatabaseError) {
                    //TODO("Not yet implemented")
                }

            }
        )
    }

    /** загрузка изображения в базу данных
     * */
    private suspend fun uploadImage(uri: Uri?, chatID: String, messageID: String): Uri? {
        val imageRef = FirebaseStorage.getInstance().getReference("images/${chatID}/${messageID}")
        if (uri != null)
            imageRef.putFile(File(context.cacheDir, "images/temp.jpeg").toUri()).await()
        return FirebaseStorage.getInstance()
            .getReference("images/${chatID}/${messageID}").downloadUrl.await()
    }

    /** функция сохранения изображения из чата в галерею
     * */
    fun saveMediaToStorage(
        bitmap: Bitmap,
        imageName: String,
        destination: String = Environment.DIRECTORY_PICTURES
    ) {
        Log.d("ChatViewModel", "")
        //Generating a file name
        val filename = "$imageName.png"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            context?.contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, destination)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(destination)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "Изображение сохранено", Toast.LENGTH_SHORT).show()
        }
    }


}