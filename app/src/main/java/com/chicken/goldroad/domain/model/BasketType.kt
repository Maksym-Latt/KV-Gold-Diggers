package com.chicken.goldroad.domain.model

import androidx.annotation.DrawableRes
import com.chicken.goldroad.R

enum class BasketType(
    val id: String,
    val title: String,
    @DrawableRes val imageRes: Int,
    val price: Int?
) {
    CLASSIC(
        id = "classic",
        title = "Classic",
        imageRes = R.drawable.basket_1,
        price = 0
    ),
    GOLD(
        id = "gold",
        title = "Gold",
        imageRes = R.drawable.basket_2,
        price = 450
    ),
    TWILIGHT(
        id = "twilight",
        title = "Twilight",
        imageRes = R.drawable.basket_3,
        price = 580
    ),
    ROYAL(
        id = "royal",
        title = "Royal",
        imageRes = R.drawable.basket_4,
        price = 930
    );

    companion object {
        fun fromId(id: String?): BasketType = values().firstOrNull { it.id == id } ?: CLASSIC
    }
}
