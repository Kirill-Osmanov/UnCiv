package com.unciv.logic

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.unciv.GameSettings
import com.unciv.ui.utils.ImageGetter
import java.io.File

class GameSaver {
    private val saveFilesFolder = "SaveFiles"

    fun json() = Json().apply { setIgnoreDeprecated(true); ignoreUnknownFields = true } // Json() is NOT THREAD SAFE so we need to create a new one for each function


    fun getSave(GameName: String): FileHandle {
        return Gdx.files.local("$saveFilesFolder/$GameName")
    }

    fun getSaves(): List<String> {
        return Gdx.files.local(saveFilesFolder).list().map { it.name() }
    }

    fun saveGame(game: GameInfo, GameName: String) {
        json().toJson(game,getSave(GameName))
    }

    fun loadGame(GameName: String) : GameInfo {
        val game = json().fromJson(GameInfo::class.java, getSave(GameName))
        game.setTransients()
        return game
    }

    fun deleteSave(GameName: String){
        getSave(GameName).delete()
    }

    fun getGeneralSettingsFile(): FileHandle {
        return Gdx.files.local("GameSettings.json")
    }

    fun getGeneralSettings(): GameSettings {
        val settingsFile = getGeneralSettingsFile()
        if(!settingsFile.exists()) return GameSettings()
        val settings = json().fromJson(GameSettings::class.java, settingsFile)

        val currentTileSets = ImageGetter.atlas.regions.filter { it.name.startsWith("TileSets") }
                .map { it.name.split("/")[1] }.distinct()
        if(settings.tileSet !in currentTileSets) settings.tileSet = "Default"
        return settings
    }

    fun setGeneralSettings(gameSettings: GameSettings){
        getGeneralSettingsFile().writeString(json().toJson(gameSettings), false)
    }

    fun autoSave(gameInfo: GameInfo, postRunnable: () -> Unit = {}) {
        // The save takes a long time (up to a few seconds on large games!) and we can do it while the player continues his game.
        // On the other hand if we alter the game data while it's being serialized we could get a concurrent modification exception.
        // So what we do is we clone all the game data and serialize the clone.
        val gameInfoClone = gameInfo.clone()
        kotlin.concurrent.thread {
            saveGame(gameInfoClone, "Autosave")

            // keep auto-saves for the last 10 turns for debugging purposes
            // eg turn 238 is saved as "Autosave-8"
            getSave("Autosave").copyTo(Gdx.files.local(saveFilesFolder + File.separator + "Autosave-${gameInfoClone.turns%10}"))

            // do this on main thread
            Gdx.app.postRunnable {
                postRunnable()
            }
        }

    }
}

