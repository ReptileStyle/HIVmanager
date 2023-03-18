package com.saqtan.saqtan.ui.screens.addpill

import android.app.Activity
import android.app.TimePickerDialog
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Done
import com.saqtan.saqtan.navigation.NavigationEvent
import com.saqtan.saqtan.ui.screens.components.BottomNavBar
import com.saqtan.saqtan.ui.screens.components.MyFloatingActionButton
import com.saqtan.saqtan.ui.screens.components.MyTopAppBar
import com.saqtan.saqtan.ui.theme.HIVmanagerTheme

/** экран, где происходит добавление напоминания в список
 * */
@Composable
fun AddPillScreen(
    onNavigate: (route: String, popBackStack: Boolean) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: AddPillViewModel = hiltViewModel()
) {

    val activity = (LocalContext.current as Activity)

    LaunchedEffect(key1 = true) {
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
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
    AddPillScreenUi(
        bottomNavBarNavigationEventSender = {viewModel.sendNavigationEvent(it)},
        pillName = viewModel.state.pillName,
        onPillNameChange = {viewModel.onEvent(AddPillEvent.OnPillNameChange(it))},
        pillStart = DateTimeFormatter.ofPattern("dd.MM.YYYY").format(viewModel.state.pillStart),
        onPillStartDatePick = {viewModel.onEvent(AddPillEvent.OnPillStartChange(it))},
        pillDuration = viewModel.state.pillDuration,
        onPillDurationChange = {viewModel.onEvent(AddPillEvent.OnPillDurationChange(it))},
        pillTime = viewModel.state.pillTime,
        onTimeAdded = {viewModel.onEvent(AddPillEvent.OnPillTimeAdded(it))},
        onDeletePillTimeClick = {viewModel.onEvent(AddPillEvent.OnDeletePillTimeClick(it))},
        onConfirmClick = {viewModel.onEvent(AddPillEvent.OnConfirmClick)},
        onBackClick = {viewModel.sendNavigationEvent(NavigationEvent.NavigateUp)}
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddPillScreenUi(
    bottomNavBarNavigationEventSender:(NavigationEvent)->Unit = {},
    pillName:String = "Аспирин",
    onPillNameChange:(String)->Unit = {},
    pillStart:String = "Аспирин",
    onPillStartDatePick:(LocalDate)->Unit = {},
    pillTime:List<String> = listOf("19:00"),
  //  onPillTimeChange:(String)->Unit = {},
    pillDuration:String = "Аспирин",
    onPillDurationChange:(String)->Unit = {},
    onTimeAdded: (String) -> Unit = {},
    onDeletePillTimeClick:(Int)->Unit = {},
    onConfirmClick:()->Unit = {},
    onBackClick:()->Unit = {}
){
    val dateDialogState = rememberMaterialDialogState()

    val mCalendar = Calendar.getInstance()
    val mHour = mCalendar[Calendar.HOUR_OF_DAY]
    val mMinute = mCalendar[Calendar.MINUTE]
    val mTimePickerDialog = TimePickerDialog(
        LocalContext.current,
        {_, mHour : Int, mMinute: Int ->
            if("$mMinute".length==1)
                onTimeAdded("$mHour:0$mMinute")
            else
                onTimeAdded("$mHour:$mMinute")
        }, mHour, mMinute, true
    )

    Scaffold(
        topBar = { MyTopAppBar("Ескертулер қосу",onBackClick = onBackClick) },
        bottomBar = { BottomNavBar(bottomNavBarNavigationEventSender) },
        floatingActionButton = {
            MyFloatingActionButton(
                imageVector = Icons.Filled.Done,
                onClick = onConfirmClick
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = 24.dp),
        ) {
            datePickerDialog(dateDialogState = dateDialogState, onDatePick = onPillStartDatePick)

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Дәрі-дәрмек")
            AddPillTextField(
                value = pillName,
                onValueChange = onPillNameChange,
                label = {Text(text = "Аты және бір реттік дозасы")}
                )
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Бастау кезі")
            AddPillTextField(
                value = pillStart,
                onValueChange = {},
                label = {Text(text = "Қабылдау күндер саны")},
                onClick = {
                    Log.d("AddPillScreen","entered onClick")
                    dateDialogState.show()
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Қабылдау уақыты")
            LazyColumn(modifier = Modifier.heightIn(max=220.dp)){
                itemsIndexed(pillTime){ index,time ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        AddPillTextField(
                            value = time,
                            onValueChange = {},
                            label = {Text(text = "Есерту уақыты")},
                            onClick = {}
                        )
                        IconButton(
                            onClick = {onDeletePillTimeClick(index)}
                        ){
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "delete pill time",
                                tint = MaterialTheme.colors.primary
                            )
                        }
                    }
                }
            }


            Button(onClick = { mTimePickerDialog.show() }) {
                Text(text ="Уақыт қосу")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Курс ұзақтығы")
            AddPillTextField(
                value = pillDuration,
                onValueChange = {if(it.length <= 3)
                    onPillDurationChange(it)},
                label = {Text(text = "Қабылдауды бастау күні")}
            )
        }
    }
}

/** выбор даты при помощи календаря
 * */
@Composable
private fun datePickerDialog(
    dateDialogState:MaterialDialogState,
    onDatePick:(LocalDate)->Unit = {}
){
    MaterialDialog (
        dialogState = dateDialogState,
        properties = DialogProperties (
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
        buttons = {
            positiveButton(text = "Ok")
            negativeButton(text = "Cancel")
        }
    ) {
        datepicker (
            initialDate = LocalDate.now(),
            title = "Pick a date"
        ) {
            onDatePick(it)
        }
    }
}

/** текстовое поле для этого экрана
 * */
@Composable
private fun AddPillTextField(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onValueChange: (String) -> Unit = {},
    value: String = "",
    label: @Composable (() -> Unit)? = null,
) {
    if (onClick == null) {
        TextField(
            modifier = modifier,
            value = value,
            onValueChange = onValueChange,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent
            ),
            label=label
        )
    } else {

        TextField(
            modifier=modifier.clickable { onClick() },
            enabled = false,
            value = value,
            onValueChange = {},
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                disabledLabelColor =  MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
                disabledTextColor = LocalContentColor.current.copy(LocalContentAlpha.current),

            ),
            label=label
        )

    }
}

@Preview
@Composable
private fun AddPillScreenPreview(){
    HIVmanagerTheme {
        AddPillScreenUi()
    }
}