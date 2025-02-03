package org.isen.projet.carburant.ctrl

import kotlinx.coroutines.*
import org.isen.projet.carburant.data.impl.SourceJson
import org.isen.projet.carburant.data.impl.SourceXml
import org.isen.projet.carburant.model.CarburantModel
import org.isen.projet.carburant.model.Station
import org.isen.projet.carburant.view.ICarburantView

class CarburantController(private val model: CarburantModel) {
    private val views = mutableListOf<ICarburantView>()
    private val dataSourceJson = SourceJson()
    private val dataSourceXml = SourceXml()

    fun registerView(view: ICarburantView) {
        model.addObserver(view)
        views.add(view)
    }

    fun displayAll() {
        views.forEach { it.display() }
    }

    fun updateModelForCityWithSource(
        city: String,
        useJsonSource: Boolean,
        fuelType: String? = null,
        hasToilets: Boolean = false,
        hasAirPump: Boolean = false,
        hasFoodShop: Boolean = false
    ) {
        val dataSource = if (useJsonSource) dataSourceJson else dataSourceXml
        val sourceName = if (useJsonSource) "📡 Source Principale (JSON)" else "📡 Source Secondaire (XML)"
        println("$sourceName : Recherche en cours pour la ville : $city...")

        GlobalScope.launch(Dispatchers.IO) {
            val stations: List<Station> = dataSource.fetchDataForCity(city, fuelType, hasToilets, hasAirPump, hasFoodShop)

            withContext(Dispatchers.Default) {
                model.updateStations(stations)
                println("✅ Modèle mis à jour avec ${stations.size} stations pour $city depuis $sourceName !")
            }
        }
    }
}










