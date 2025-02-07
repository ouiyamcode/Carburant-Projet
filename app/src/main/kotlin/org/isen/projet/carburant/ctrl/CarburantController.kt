package org.isen.projet.carburant.ctrl

import kotlinx.coroutines.*
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
     * 🚗 **Recherche un itinéraire entre deux villes et affiche les stations-service sur le trajet**
     */



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
    /**
     * 🚗 **Met à jour le modèle avec les stations situées le long d'un itinéraire**
     * @param itineraire Liste des coordonnées (latitude, longitude) de l'itinéraire
     * @param useJsonSource Indique si on utilise la source JSON (true) ou XML (false)
     */
    fun updateModelForItineraireWithSource(
        depart: String,
        arrivee:String,
        useJsonSource: Boolean
    ) {
        model.fetchStationsSurItineraire(depart, arrivee, useJsonSource)

    }



}










