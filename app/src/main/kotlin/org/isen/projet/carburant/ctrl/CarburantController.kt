package org.isen.projet.carburant.ctrl

import org.isen.projet.carburant.model.CarburantModel
import org.isen.projet.carburant.view.ICarburantView

class CarburantController(private val model: CarburantModel) {
    private val views = mutableListOf<ICarburantView>()

    fun registerView(view: ICarburantView) {
        model.addObserver(view)
        views.add(view)
    }

    fun displayAll() {
        views.forEach { it.display() }
    }

    /**
     * 🌍 **Appelle directement le modèle pour récupérer les stations**
     */
    fun updateModelForCityWithSource(
        city: String,
        useJsonSource: Boolean,
        fuelType: String? = null,
        hasToilets: Boolean = false,
        hasAirPump: Boolean = false,
        hasFoodShop: Boolean = false
    ) {
        model.fetchStations(city, useJsonSource, fuelType, hasToilets, hasAirPump, hasFoodShop)
    }
}










