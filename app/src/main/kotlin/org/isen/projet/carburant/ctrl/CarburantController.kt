package org.isen.projet.carburant.ctrl

import kotlinx.coroutines.*
import org.isen.projet.carburant.data.impl.SourceJson
import org.isen.projet.carburant.model.CarburantModel
import org.isen.projet.carburant.model.Station
import org.isen.projet.carburant.view.ICarburantView

class CarburantController(private val model: CarburantModel) {
    private val views = mutableListOf<ICarburantView>()
    private val dataSource = SourceJson()

    fun registerView(view: ICarburantView) {
        model.addObserver(view)
        views.add(view)
    }

    fun displayAll() {
        views.forEach { it.display() }
    }

    fun updateModelForCity(
        city: String,
        fuelType: String? = null,
        hasToilets: Boolean = false,
        hasAirPump: Boolean = false,
        hasFoodShop: Boolean = false
    ) {
        println("📡 Récupération des stations-service pour la ville : $city...")

        GlobalScope.launch(Dispatchers.IO) {
            val jsonData = dataSource.fetchDataForCity(city, fuelType, hasToilets, hasAirPump, hasFoodShop)
            val stations = dataSource.parseData(jsonData)

            withContext(Dispatchers.Default) {
                model.updateStations(stations)
                println("✅ Modèle mis à jour avec ${stations.size} stations pour $city !")
            }
        }
    }
}








