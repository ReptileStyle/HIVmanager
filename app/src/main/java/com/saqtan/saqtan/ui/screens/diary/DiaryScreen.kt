package com.saqtan.saqtan.ui.screens.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.saqtan.saqtan.data.model.DiaryEntry
import com.saqtan.saqtan.data.model.UserData
import com.saqtan.saqtan.navigation.NavigationEvent
import com.saqtan.saqtan.ui.screens.components.BottomNavBar
import com.saqtan.saqtan.ui.screens.components.MyTopAppBar
import com.saqtan.saqtan.ui.screens.components.OutlinedTextFieldNoContentPadding
import com.saqtan.saqtan.ui.theme.HIVmanagerTheme
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/***
 * экран дневника наблюдений
 */
@Composable
fun DiaryScreen(
    onNavigate: (route: String, popBackStack: Boolean) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    LaunchedEffect(key1 = true) {
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
    var diaryEntries2 = viewModel.userRepository.userDataFlow.collectAsState(initial = UserData())

    DiaryScreenUi(
        bottomNavBarNavigationEventSender = { viewModel.sendNavigationEvent(it) },
        onAddEntryClick = { viewModel.onEvent(DiaryEvent.OnAddDiaryEntryClick(it)) },
        diaryList = diaryEntries2.value.diaryEntries,
        onDeleteEntryClick = { viewModel.onEvent(DiaryEvent.OnDeleteDiaryEntryClick(it)) },
        onBackClick = {viewModel.sendNavigationEvent(NavigationEvent.NavigateUp)}
    )
}

@Composable
private fun DiaryScreenUi(
    bottomNavBarNavigationEventSender: (NavigationEvent) -> Unit = {},
    onAddEntryClick: (DiaryEntry) -> Unit = {},
    onDeleteEntryClick: (DiaryEntry) -> Unit = {},
    diaryList: List<DiaryEntry> = listOf(diaryEntryExample),
    onBackClick:()->Unit = {}
) {
    Scaffold(
        topBar = { MyTopAppBar("Күнделік", onBackClick = onBackClick) },
        bottomBar = { BottomNavBar({ bottomNavBarNavigationEventSender(it) }) }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            item {
                DiaryHeader()
            }
            item {
                AddEntry(onAddEntryClick = onAddEntryClick)
            }
            items(diaryList) { diaryEntry ->
                DiaryEntryContainer(diaryEntry, onDeleteDiaryEntryClick = onDeleteEntryClick)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AddEntryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next
    ),
) {
    OutlinedTextFieldNoContentPadding(
        value = value,
        onValueChange = onValueChange,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = MaterialTheme.colors.primary,
            focusedBorderColor = MaterialTheme.colors.primaryVariant
        ),
        modifier = modifier
            .padding(horizontal = 3.dp, vertical = 2.dp)
            .height(35.dp),
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions
    )
}
/***
 * функционал для добавления записи в дневник
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AddEntry(
    onAddEntryClick: (DiaryEntry) -> Unit = {}
) {
    var sys by remember {
        mutableStateOf("")
    }
    var dia by remember {
        mutableStateOf("")
    }
    var pulse by remember {
        mutableStateOf("")
    }
    var temperature by remember {
        mutableStateOf("")
    }
    var weight by remember {
        mutableStateOf("")
    }
    var comment by remember {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    val (sysField, diaField, pulseField, tempField, weightField, commentField) = remember { FocusRequester.createRefs() }

    fun clear() {
        comment = ""
        sys = ""
        dia = ""
        pulse = ""
        temperature = ""
        weight = ""
    }
    Row(Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            AddEntryTextField(
                value = sys,
                onValueChange = {
                    if (isAllDigits(it) && it.length <= 3)
                        sys = it
                },
                modifier = Modifier
                    .focusRequester(sysField)
                    .focusProperties { next = diaField },
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            AddEntryTextField(
                value = dia,
                onValueChange = {
                    if (isAllDigits(it) && it.length <= 3)
                        dia = it
                },
                modifier = Modifier
                    .focusRequester(diaField)
                    .focusProperties { next = pulseField },
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            AddEntryTextField(
                value = pulse,
                onValueChange = {
                    if (isAllDigits(it) && it.length <= 3)
                        pulse = it
                },
                modifier = Modifier
                    .focusRequester(pulseField)
                    .focusProperties { next = tempField },
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            AddEntryTextField(
                value = temperature,
                onValueChange = {
                    if (isAllDigitsOrDot(it) && it.length <= 4)
                        temperature = it
                },
                modifier = Modifier
                    .focusRequester(tempField)
                    .focusProperties { next = weightField },
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            AddEntryTextField(
                value = weight,
                onValueChange = {
                    if (isAllDigitsOrDot(it) && it.length <= 5)
                        weight = it
                },
                modifier = Modifier
                    .focusRequester(weightField)
                    .focusProperties { next = commentField },
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = comment,
                onValueChange = {
                    comment = it
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = MaterialTheme.colors.primary,
                    focusedBorderColor = MaterialTheme.colors.primaryVariant
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .focusRequester(commentField),
                placeholder = { Text(text = "Пікірлер...") },
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            TextButton(
                onClick = { clear() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Тазалау")
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            TextButton(
                onClick = {
                    onAddEntryClick(
                        DiaryEntry(
                            upperTension = if (sys.isNotEmpty()) sys.toInt() else 0,
                            lowerTension = if (dia.isNotEmpty()) dia.toInt() else 0,
                            pulse = if (pulse.isNotEmpty()) pulse.toInt() else 0,
                            temperature = if (temperature.isNotEmpty()) temperature.toDouble() else 0.0,
                            weight = if (weight.isNotEmpty()) weight.toDouble() else 0.0,
                            comment = comment,
                            time = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        )
                    )
                    clear()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Қосу")
            }
        }
    }

}

private fun isAllDigits(s: String): Boolean {
    for (char in s) {
        if (!char.isDigit()) return false
    }
    return true
}

private fun isAllDigitsOrDot(s: String): Boolean {
    for (char in s) {
        if (!char.isDigit() && char != '.') return false
    }
    return true
}

@Composable
private fun DiaryHeader() {
    Row(Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "СИС", color = MaterialTheme.colors.primary, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "ДИА", color = MaterialTheme.colors.primary, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Пульс", color = MaterialTheme.colors.primary, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Темп", color = MaterialTheme.colors.primary, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Салмақ", color = MaterialTheme.colors.primary, fontSize = 20.sp)
        }
    }
}
/***
 * хранит запись в дневнике
 */
@Composable
private fun DiaryEntryContainer(
    diaryEntry: DiaryEntry = diaryEntryExample,
    onDeleteDiaryEntryClick: (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val deleteEntryDialogState = rememberMaterialDialogState()
    DeleteEntryDialog(
        dialogState = deleteEntryDialogState,
        onConfirmClick = {onDeleteDiaryEntryClick(diaryEntry)}
    )
    Surface(
        modifier = modifier.padding(8.dp),
        elevation = 6.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${diaryEntry.time}",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
                IconButton(onClick = { deleteEntryDialogState.show() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "delete diary entry"
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${diaryEntry.upperTension}",
                        color = MaterialTheme.colors.primary,
                        fontSize = 20.sp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${diaryEntry.lowerTension}",
                        color = MaterialTheme.colors.primary,
                        fontSize = 20.sp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${diaryEntry.pulse}",
                        color = MaterialTheme.colors.primary,
                        fontSize = 20.sp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${diaryEntry.temperature}",
                        color = MaterialTheme.colors.primary,
                        fontSize = 20.sp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${diaryEntry.weight}",
                        color = MaterialTheme.colors.primary,
                        fontSize = 20.sp
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                commentHolder(comment = diaryEntry.comment)
            }
        }
    }
}
/***
 * хранит комментарий записи в дневнике, который открывается по нажатию
 */
@Composable
private fun commentHolder(comment: String = "my comment") {
    var isExpanded by remember { mutableStateOf(false) }
    Column() {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Пікірлер", modifier = Modifier.padding(start = 8.dp))
            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "expend more/less article text",
                    tint = if (isExpanded) MaterialTheme.colors.primaryVariant else LocalContentColor.current.copy(
                        alpha = LocalContentAlpha.current
                    )
                )
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()

            ) {
                Text(text = comment, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            }
        }
    }
}

@Composable
fun DeleteEntryDialog(
    dialogState: MaterialDialogState,
    onConfirmClick: ()->Unit
){
    MaterialDialog(
        dialogState = dialogState,
        properties = DialogProperties (
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
        buttons = {
            positiveButton(text = "ОК", onClick = onConfirmClick)
            negativeButton(text = "Болдырмау")
        }
    ) {
        Text(
            text= "Жазбаны жойғыңыз келетініне сенімдісіз бе ?",
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun DiaryScreenPreview() {
    HIVmanagerTheme {
        DiaryScreenUi()
    }
}

private val diaryEntryExample = DiaryEntry(
    120,
    80,
    60.5,
    37.2,
    83,
    "комментарий",
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
)