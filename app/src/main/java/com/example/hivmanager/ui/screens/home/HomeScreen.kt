package com.example.hivmanager.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hivmanager.navigation.NavigationEvent
import com.example.hivmanager.ui.screens.components.BottomNavBar
import com.example.hivmanager.ui.screens.components.MyTopAppBar
import com.example.hivmanager.ui.screens.signin.SignInViewModel
import com.example.hivmanager.ui.theme.HIVmanagerTheme

@Composable
fun HomeScreen(
    onNavigate: (route: String, popBackStack: Boolean) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect {
            when (it) {
                is NavigationEvent.Navigate -> {
                    onNavigate(it.route, it.popBackStack)
                }
                else -> {}
            }
        }
    }
    HomeScreenUi(
        bottomNavBarNavigationEventSender = {viewModel.sendNavigationEvent(it)}
    )
}


@Composable
private fun HomeScreenUi(
    bottomNavBarNavigationEventSender:(NavigationEvent)->Unit = {}
){
    Scaffold(
        topBar = {MyTopAppBar("Home")},
        bottomBar = {BottomNavBar(bottomNavBarNavigationEventSender,1)}
    ) {
        Column(modifier = Modifier.padding(it)) {

        }

    }
}

@Preview
@Composable
private fun HomeScreenPreview(){
    HIVmanagerTheme {
        HomeScreenUi()
    }
}