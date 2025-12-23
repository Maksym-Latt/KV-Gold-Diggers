package com.chicken.goldroad.data

data class PlayerPreferences(
    val coins: Int = 0,
    val selectedBasketId: String = "classic",
    val ownedBaskets: Set<String> = setOf("classic"),
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val currentLevel: Int = 1
)
