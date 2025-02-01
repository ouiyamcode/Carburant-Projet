package org.isen.projet.carburant.data

import org.isen.projet.carburant.model.Station

interface IDataSource {
    fun fetchData(): String
    fun parseData(data: String): List<Station>
}