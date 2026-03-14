package com.example.moneychangersdatacollectionsystem.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Splash : Screen()
    
    @Serializable
    data object Login : Screen()

    @Serializable
    data object Dashboard : Screen()
    
    @Serializable
    data object CollectionForm : Screen()

    @Serializable
    data object ManualCrop : Screen()
    
    @Serializable
    data object History : Screen()

    @Serializable
    data object CollectionFlow : Screen()
}
