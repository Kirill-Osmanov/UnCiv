package com.unciv.models.gamebasics

import com.badlogic.gdx.graphics.Color
import com.unciv.logic.civilization.CityStateType
import com.unciv.models.stats.INamed
import com.unciv.ui.utils.colorFromRGB

enum class VictoryType{
    Neutral,
    Cultural,
    Domination,
    Scientific
}

class Nation : INamed {
    override lateinit var name: String
    var translatedName=""
    fun getNameTranslation(): String {
        if(translatedName!="") return translatedName
        else return name
    }

    var leaderName=""
    fun getLeaderDisplayName() = if(isCityState()) getNameTranslation()
        else "[$leaderName] of [${getNameTranslation()}]"

    var cityStateType: CityStateType?=null
    var preferredVictoryType:VictoryType = VictoryType.Neutral
    var declaringWar=""
    var attacked=""
    var defeated=""
    var introduction=""
    var tradeRequest=""

    var neutralHello=""
    var hateHello=""

    var neutralLetsHearIt = ArrayList<String>()
    var neutralYes = ArrayList<String>()
    var neutralNo = ArrayList<String>()

    var hateLetsHearIt = ArrayList<String>()
    var hateYes = ArrayList<String>()
    var hateNo = ArrayList<String>()

    var afterPeace=""

    lateinit var outerColor: List<Int>
    var unique:String?=null
    var innerColor: List<Int>?=null
    var startBias = ArrayList<String>()


    fun getColor(): Color {
        return colorFromRGB(outerColor[0], outerColor[1], outerColor[2])
    }
    fun getSecondaryColor(): Color {
        if(innerColor==null) return Color.BLACK
        return colorFromRGB(innerColor!![0], innerColor!![1], innerColor!![2])
    }

    fun isCityState()= cityStateType != null

    lateinit var cities: List<String>
}
