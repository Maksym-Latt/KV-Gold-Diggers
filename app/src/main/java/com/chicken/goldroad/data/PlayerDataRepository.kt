package com.chicken.goldroad.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chicken.goldroad.domain.model.BasketType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private const val PLAYER_DATA_STORE = "player_preferences"
private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = PLAYER_DATA_STORE)

@Singleton
class PlayerDataRepository @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.playerDataStore

    val playerState: Flow<PlayerPreferences> = dataStore.data.map { prefs ->
        val owned = prefs[OWNED_BASKETS_KEY] ?: setOf(DEFAULT_BASKET_ID)
        PlayerPreferences(
            coins = prefs[COINS_KEY] ?: 0,
            selectedBasketId = prefs[SELECTED_BASKET_KEY] ?: DEFAULT_BASKET_ID,
            ownedBaskets = owned.ifEmpty { setOf(DEFAULT_BASKET_ID) },
            musicEnabled = prefs[MUSIC_ENABLED_KEY] ?: true,
            soundEnabled = prefs[SOUND_ENABLED_KEY] ?: true
        )
    }

    suspend fun addCoins(amount: Int) {
        dataStore.edit { prefs ->
            val current = prefs[COINS_KEY] ?: 0
            prefs[COINS_KEY] = (current + amount).coerceAtLeast(0)
        }
    }

    suspend fun spendCoins(amount: Int): Boolean {
        var success = false
        dataStore.edit { prefs ->
            val current = prefs[COINS_KEY] ?: 0
            if (current >= amount) {
                prefs[COINS_KEY] = current - amount
                success = true
            }
        }
        return success
    }

    suspend fun unlockBasket(type: BasketType): Boolean {
        val price = type.price ?: 0
        if (price <= 0) return true
        val currentPrefs = dataStore.data.first()
        val alreadyOwned = (currentPrefs[OWNED_BASKETS_KEY] ?: emptySet()).contains(type.id)
        if (alreadyOwned) return true
        var unlocked = false
        val success = spendCoins(price)
        if (success) {
            dataStore.edit { prefs ->
                val owned = prefs[OWNED_BASKETS_KEY] ?: emptySet()
                prefs[OWNED_BASKETS_KEY] = owned + type.id
            }
            unlocked = true
        }
        return unlocked
    }

    suspend fun selectBasket(type: BasketType) {
        dataStore.edit { prefs ->
            val owned = prefs[OWNED_BASKETS_KEY] ?: setOf(DEFAULT_BASKET_ID)
            if (type.id in owned) {
                prefs[SELECTED_BASKET_KEY] = type.id
            }
        }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[MUSIC_ENABLED_KEY] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SOUND_ENABLED_KEY] = enabled }
    }

    companion object {
        private const val DEFAULT_BASKET_ID = "classic"
        private val COINS_KEY = intPreferencesKey("coins")
        private val SELECTED_BASKET_KEY = stringPreferencesKey("selected_basket")
        private val OWNED_BASKETS_KEY = stringSetPreferencesKey("owned_baskets")
        private val MUSIC_ENABLED_KEY = booleanPreferencesKey("music_enabled")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
    }
}
