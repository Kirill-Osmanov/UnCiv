package com.unciv.logic.civilization

import com.unciv.logic.civilization.diplomacy.RelationshipLevel
import com.unciv.logic.map.RoadStatus
import com.unciv.models.gamebasics.tile.ResourceType
import com.unciv.models.stats.Stat
import com.unciv.models.stats.StatMap
import com.unciv.models.stats.Stats
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** CivInfo class was getting too crowded */
class CivInfoStats(val civInfo: CivilizationInfo){

    private fun getUnitUpkeep(): Int {
        val baseUnitCost = 0.5f
        val freeUnits = 3
        var unitsToPayFor = civInfo.getCivUnits()
        if(civInfo.policies.isAdopted("Oligarchy")) unitsToPayFor = unitsToPayFor.filterNot { it.getTile().isCityCenter() }

        var numberOfUnitsToPayFor = max(0f, unitsToPayFor.count().toFloat() - freeUnits)
        if(civInfo.getNation().unique=="67% chance to earn 25 Gold and recruit a Barbarian unit from a conquered encampment, -25% land units maintenance."){
            val numberOfUnitsWithDiscount = min(numberOfUnitsToPayFor, unitsToPayFor.count { it.type.isLandUnit() }.toFloat())
            numberOfUnitsToPayFor -= 0.25f * numberOfUnitsWithDiscount
        }


        val gameProgress = civInfo.gameInfo.turns/400f // as game progresses Maintenance cost rises
        var cost = baseUnitCost*numberOfUnitsToPayFor*(1+gameProgress)
        cost = cost.pow(1+gameProgress/3) // Why 3? To spread 1 to 1.33
        if(!civInfo.isPlayerCivilization())
            cost *= civInfo.gameInfo.getDifficulty().aiUnitMaintenanceModifier
        if(civInfo.policies.isAdopted("Autocracy")) cost *= 0.66f
        return cost.toInt()
    }

    private fun getTransportationUpkeep(): Int {
        var transportationUpkeep = 0
        // we no longer use .flatMap, because there are a lot of tiles and keeping them all in a list
        // just to go over them once is a waste of memory - there are low-end phones who don't have much ram
        for (city  in civInfo.cities) {
            for (tile in city.getTiles()) {
                if (tile.isCityCenter()) continue
                when (tile.roadStatus) {
                    RoadStatus.Road -> transportationUpkeep += 1
                    RoadStatus.Railroad -> transportationUpkeep += 2
                }
            }
        }
        if (civInfo.policies.isAdopted("Trade Unions")) transportationUpkeep *= (2 / 3f).toInt()
        return transportationUpkeep
    }

    fun getStatMapForNextTurn(): HashMap<String, Stats> {
        val statMap = StatMap()
        for (city in civInfo.cities){
            statMap.add("Cities",city.cityStats.currentCityStats)
        }

        //City states culture bonus
        for (otherCiv in civInfo.getKnownCivs()) {
            if (otherCiv.isCityState() && otherCiv.getCityStateType() == CityStateType.Cultured
                    && otherCiv.getDiplomacyManager(civInfo.civName).relationshipLevel() >= RelationshipLevel.Friend) {
                val cultureBonus = Stats()
                cultureBonus.add(Stat.Culture, 3f * (civInfo.getEra().ordinal+1))
                statMap.add("City States",cultureBonus)
            }
        }

        for (entry in getHappinessBreakdown()) {
            statMap.add(entry.key, Stats().apply { happiness=entry.value })
        }

        statMap["Transportation upkeep"] = Stats().apply { gold=- getTransportationUpkeep().toFloat()}
        statMap["Unit upkeep"] = Stats().apply { gold=- getUnitUpkeep().toFloat()}

        if (civInfo.policies.isAdopted("Mandate Of Heaven")) {
            val happiness = statMap.values.map { it.happiness }.sum()
            if(happiness>0) statMap.add("Policies", Stats().apply { culture=happiness/2 })
        }

        // negative gold hurts science
        // if we have - or 0, then the techs will never be complete and the tech button
        // will show a negative number of turns and int.max, respectively
        if (statMap.values.map { it.gold }.sum() < 0) {
            val scienceDeficit = max(statMap.values.map { it.gold }.sum(),
                    1 - statMap.values.map { it.science }.sum())// Leave at least 1
            statMap["Treasury deficit"] = Stats().apply { science = scienceDeficit }
        }
        val goldDifferenceFromTrade = civInfo.diplomacy.values.sumBy { it.goldPerTurn() }
        if(goldDifferenceFromTrade!=0)
            statMap["Trade"] = Stats().apply { gold= goldDifferenceFromTrade.toFloat() }

        return statMap
    }


    fun getHappinessBreakdown(): HashMap<String, Float> {
        val statMap = HashMap<String, Float>()
        statMap["Base happiness"] = civInfo.getDifficulty().baseHappiness.toFloat()

        var happinessPerUniqueLuxury = 5f
        if (civInfo.policies.isAdopted("Protectionism")) happinessPerUniqueLuxury += 1
        statMap["Luxury resources"]= civInfo.getCivResources().map { it.resource }
                .count { it.resourceType === ResourceType.Luxury } * happinessPerUniqueLuxury

        for(city in civInfo.cities.toList()){
            for(keyvalue in city.cityStats.happinessList){
                if(statMap.containsKey(keyvalue.key))
                    statMap[keyvalue.key] = statMap[keyvalue.key]!!+keyvalue.value
                else statMap[keyvalue.key] = keyvalue.value
            }
        }

        if (civInfo.containsBuildingUnique("Provides 1 happiness per social policy")) {
            if(!statMap.containsKey("Policies")) statMap["Policies"]=0f
            statMap["Policies"] = statMap["Policies"]!! +
                    civInfo.policies.getAdoptedPolicies().count { !it.endsWith("Complete") }.toFloat()
        }

        //From city-states
        for (otherCiv in civInfo.getKnownCivs()) {
            if (otherCiv.isCityState() && otherCiv.getCityStateType() == CityStateType.Mercantile
                    && otherCiv.getDiplomacyManager(civInfo).relationshipLevel() >= RelationshipLevel.Friend) {
                if (statMap.containsKey("City-states"))
                    statMap["City-states"] = statMap["City-states"]!! + 3f
                else
                    statMap["City-states"] = 3f
            }
        }

        return statMap
    }

}