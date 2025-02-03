package org.isen.projet.carburant.data

import org.isen.projet.carburant.model.Station

interface IDataSource {
    fun fetchData(): String
    fun parseData(data: String): List<Station>

    // ✅ Ajout de la méthode pour rechercher une ville avec filtres
    fun fetchDataForCity(
        city: String,
        fuelType: String? = null,
        hasToilets: Boolean = false,
        hasAirPump: Boolean = false,
        hasFoodShop: Boolean = false
    ): List<Station>
}
