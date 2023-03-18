package com.saqtan.saqtan.ui.screens.userinfo

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saqtan.saqtan.data.model.DiaryEntry
import com.saqtan.saqtan.navigation.NavigationEvent
import com.saqtan.saqtan.ui.screens.components.MyTopAppBar
import com.saqtan.saqtan.ui.theme.HIVmanagerTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun UserInfoScreen(
    onNavigate: (route: String, popBackStack: Boolean) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: UserInfoViewModel = hiltViewModel()
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
    DiaryScreenUi(
        diaryList = viewModel.userData.diaryEntries,
        onBackClick = {viewModel.sendNavigationEvent(NavigationEvent.NavigateUp)},
        height = viewModel.userData.height,
        allergies = viewModel.userData.allergies
    )
}




@Composable
private fun DiaryScreenUi(
    diaryList: List<DiaryEntry> = listOf(diaryEntryExample),
    height: Int = 180,
    allergies:String = "",
    onBackClick:()->Unit = {}
) {
    Scaffold(
        topBar = { MyTopAppBar("Күнделік", onBackClick = onBackClick) },
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            item{
                Text(text = "Бой өлшемі: $height", modifier = Modifier.padding(8.dp))
            }
            item{
                var isExpanded by remember {
                    mutableStateOf(false)
                }
                val allergiesSubstring = if(allergies.length<=10) allergies else  allergies.substring(0,7)
                val text = if(isExpanded)"Аллергиялық аурулары: $allergies" else
                    "Аллергиялық аурулары: $allergiesSubstring..."
                Text(text = text, modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .padding(8.dp), maxLines = if(isExpanded) Int.MAX_VALUE else 1)
                Divider(thickness = 1.dp)
            }
            item {
                DiaryHeader()
            }
            items(diaryList) { diaryEntry ->
                DiaryEntryContainer(diaryEntry)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
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

@Composable
private fun DiaryEntryContainer(
    diaryEntry: DiaryEntry = diaryEntryExample,
    modifier: Modifier = Modifier
) {
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