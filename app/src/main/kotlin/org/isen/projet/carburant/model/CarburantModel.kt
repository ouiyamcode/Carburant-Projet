package org.isen.projet.carburant.model

import org.isen.projet.carburant.data.impl.SourceJson
import org.isen.projet.carburant.data.impl.SourceXml
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.net.URLEncoder
import kotlin.properties.Delegates

class CarburantModel {
    private val pcs = PropertyChangeSupport(this)
    private val dataSourceJson = SourceJson()
    private val dataSourceXml = SourceXml()
    private val cacheGeocoding = mutableMapOf<String, Pair<String, String>>() // Cache pour éviter trop de requêtes Nominatim
    private val objectMapper = jacksonObjectMapper() // ✅ Utilisation de Jackson pour parser JSON

    var stations: List<Station> by Delegates.observable(emptyList()) { property, oldValue, newValue ->
        pcs.firePropertyChange(property.name, oldValue, newValue)
    }

    fun updateStations(newStations: List<Station>) {
        stations = newStations
    }

    fun addObserver(l: PropertyChangeListener) {
        pcs.addPropertyChangeListener(l)
    }

    /**
     * 📡 **Méthode pour récupérer les stations-service selon la source sélectionnée**
     */
    fun fetchStations(
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
            val stations = dataSource.fetchDataForCity(city, fuelType, hasToilets, hasAirPump, hasFoodShop)

            // 🔥 Enrichissement des coordonnées avec Nominatim si nécessaire
            val enrichedStations = stations.map { station ->
                if (station.latitude == "0.0" || station.longitude == "0.0") {
                    fetchCoordinatesFromNominatim(station) ?: station
                } else {
                    station
                }
            }

            withContext(Dispatchers.Default) {
                updateStations(enrichedStations)
                println("✅ Modèle mis à jour avec ${enrichedStations.size} stations pour $city depuis $sourceName !")
            }
        }
    }

    /**
     * 🌍 **Méthode pour récupérer les coordonnées manquantes avec Nominatim**
     */
    private fun fetchCoordinatesFromNominatim(station: Station): Station? {
        val fullAddress = "${station.adresse}, ${station.codePostal}, ${station.ville}, FRANCE"

        // 📌 Vérifie si les coordonnées sont déjà en cache
        if (cacheGeocoding.containsKey(fullAddress)) {
            val (lat, lon) = cacheGeocoding[fullAddress]!!
            return station.copy(latitude = lat, longitude = lon)
        }

        val encodedAddress = URLEncoder.encode(fullAddress, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedAddress"

        return try {
            val (_, _, result) = url.httpGet().responseString()
            when (result) {
                is Result.Success -> {
                    val jsonData = result.get()

                    // ✅ Correction de `readValue` en spécifiant le type attendu
                    val geoResults: List<Map<String, Any>> = objectMapper.readValue(jsonData)

                    if (geoResults.isNotEmpty()) {
                        val firstResult = geoResults[0]
                        val lat = firstResult["lat"]?.toString() ?: return null
                        val lon = firstResult["lon"]?.toString() ?: return null

                        // 📌 Stockage en cache pour éviter les requêtes multiples
                        cacheGeocoding[fullAddress] = Pair(lat, lon)

                        println("📍 Géocodage réussi: $fullAddress -> ($lat, $lon)")
                        station.copy(latitude = lat, longitude = lon)
                    } else {
                        println("❌ Aucun résultat pour $fullAddress")
                        null
                    }
                }
                is Result.Failure -> {
                    println("❌ Erreur HTTP lors de la requête Nominatim: ${result.error.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("❌ Erreur lors de la requête Nominatim: ${e.message}")
            null
        }
    }
}


