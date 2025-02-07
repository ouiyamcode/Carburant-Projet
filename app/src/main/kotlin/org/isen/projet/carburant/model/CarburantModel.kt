package org.isen.projet.carburant.model

import org.isen.projet.carburant.data.impl.SourceJson
import org.isen.projet.carburant.data.impl.SourceXml
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.net.URLEncoder
import kotlin.math.*
import kotlin.properties.Delegates

class CarburantModel {
    private val pcs = PropertyChangeSupport(this)
    private val dataSourceJson = SourceJson()
    private val dataSourceXml = SourceXml()
    private val cacheGeocoding = mutableMapOf<String, Pair<String, String>>() // Cache pour éviter trop de requêtes Nominatim
    private val objectMapper = jacksonObjectMapper() // ✅ Utilisation de Jackson pour parser JSON
    private val logger: Logger = LogManager.getLogger(CarburantModel::class.java)

    var stations: List<Station> by Delegates.observable(emptyList()) { property, oldValue, newValue ->
        pcs.firePropertyChange(property.name, oldValue, newValue)
        LeafletService.generateMapHtml(newValue)
    }

    fun updateStations(newStations: List<Station>) {
        stations = newStations
    }

    fun addObserver(l: PropertyChangeListener) {
        pcs.addPropertyChangeListener(l)
    }
    fun fetchStationsSurItineraire(depart: String, arrivee: String, useJsonSource: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            logger.info("🚗 Début de récupération des stations sur l'itinéraire $depart -> $arrivee")

            val departCoord = geocodeVille(depart)
            val arriveeCoord = geocodeVille(arrivee)

            if (departCoord == null || arriveeCoord == null) {
                logger.error("❌ Géocodage échoué : $depart -> $departCoord, $arrivee -> $arriveeCoord")
                return@launch
            }

            logger.info("📍 Géocodage réussi : $depart -> $departCoord, $arrivee -> $arriveeCoord")

            val itineraire = calculerItineraire(departCoord, arriveeCoord)
            if (itineraire.isNullOrEmpty()) {
                logger.warn("❌ Aucun itinéraire trouvé entre $depart et $arrivee")
                return@launch
            }

            logger.info("📌 Itinéraire trouvé avec ${itineraire.size} points GPS")

            val villes = extraireVillesSurItineraire(itineraire, 10.0)
            logger.info("🔍 Villes détectées sur l'itinéraire : $villes")

            if (villes.isEmpty()) {
                logger.warn("❌ Aucune ville trouvée sur l'itinéraire.")
                return@launch
            }

            val dataSource = if (useJsonSource) dataSourceJson else dataSourceXml
            val allStations = mutableListOf<Station>()

            val jobs = villes.map { ville ->
                async {
                    logger.info("📡 Récupération des stations pour $ville...")
                    val stations = dataSource.fetchDataForCity(ville)

                    if (stations.isNotEmpty()) {
                        synchronized(allStations) { allStations.addAll(stations) }
                        logger.info("✅ ${stations.size} stations récupérées pour $ville")
                    } else {
                        logger.warn("⚠️ Aucune station trouvée pour $ville")
                    }
                }
            }
            jobs.awaitAll()


            logger.info("📊 Vérification finale avant mise à jour : ${allStations.size} stations collectées")

            if (allStations.isEmpty()) {
                logger.error("❌ ERREUR : Aucune station trouvée sur l'itinéraire !")
            } else {
                withContext(Dispatchers.IO) {
                    updateStations(allStations)
                    logger.info("✅ ${allStations.size} stations affichées sur la carte et le tableau")
                }
            }

        }
    }



    /**
     * 🌍 **Géocodage d'une ville (Nom -> Coordonnées GPS)**
     */
    private fun geocodeVille(ville: String): Pair<Double, Double>? {
        cacheGeocoding[ville]?.let {
            logger.info("📍 Cache trouvé pour $ville -> (${it.first}, ${it.second})")
            return Pair(it.first.toDouble(), it.second.toDouble())
        }

        val encodedVille = URLEncoder.encode(ville, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedVille&countrycodes=FR"

        logger.info("🌍 Requête Nominatim pour le géocodage de $ville")

        return try {
            val (_, _, result) = url.httpGet().responseString()
            when (result) {
                is Result.Success -> {
                    val geoResults: List<Map<String, Any>> = objectMapper.readValue(result.get())
                    if (geoResults.isNotEmpty()) {
                        val firstResult = geoResults[0]
                        val lat = firstResult["lat"].toString()
                        val lon = firstResult["lon"].toString()

                        cacheGeocoding[ville] = Pair(lat, lon)
                        logger.info("📍 Géocodage réussi : $ville -> ($lat, $lon)")
                        Pair(lat.toDouble(), lon.toDouble())
                    } else {
                        logger.warn("❌ Aucun résultat pour $ville")
                        null
                    }
                }
                is Result.Failure -> {
                    logger.error("❌ Erreur HTTP lors du géocodage de $ville : ${result.error.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Exception lors du géocodage de $ville : ${e.message}")
            null
        }
    }

    /**
     * 🚗 **Calcul de l'itinéraire entre deux villes**
     */
    /**
     * 🚗 **Calcul de l'itinéraire entre deux villes avec optimisation des points**
     */
    private fun calculerItineraire(departCoord: Pair<Double, Double>, arriveeCoord: Pair<Double, Double>, intervalleMetres: Double = 1000.0): List<Pair<Double, Double>>? {
        val apiKey = "5b3ce3597851110001cf6248b38f36766e024b708ee298402a9ade37"
        val url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=$apiKey" +
                "&start=${departCoord.second},${departCoord.first}&end=${arriveeCoord.second},${arriveeCoord.first}"

        logger.info("🌍 Requête OpenRouteService pour itinéraire $departCoord -> $arriveeCoord")

        return try {
            val (_, _, result) = url.httpGet().responseString()
            when (result) {
                is Result.Success -> {
                    val routes = objectMapper.readValue<Map<String, Any>>(result.get())["features"] as? List<Map<String, Any>>
                    routes?.firstOrNull()?.get("geometry")?.let {
                        val coordinates = (it as Map<String, Any>)["coordinates"] as? List<List<Double>>

                        if (!coordinates.isNullOrEmpty()) {
                            logger.info("✅ Itinéraire initial : ${coordinates.size} points GPS")
                            return filtrerPointsItineraire(coordinates.map { Pair(it[1], it[0]) }, intervalleMetres)
                        }
                    }
                    null
                }
                is Result.Failure -> {
                    logger.error("❌ Erreur OpenRouteService : ${result.error.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Erreur lors de la requête OpenRouteService : ${e.message}")
            null
        }
    }

    /**
     * 🔍 **Filtrer les points GPS pour garder uniquement les points significatifs**
     */
    private fun filtrerPointsItineraire(points: List<Pair<Double, Double>>, intervalleMetres: Double): List<Pair<Double, Double>> {
        if (points.isEmpty()) return emptyList()

        val pointsFiltres = mutableListOf<Pair<Double, Double>>()
        var dernierPoint = points.first()
        pointsFiltres.add(dernierPoint)

        for (point in points) {
            if (distanceGPS(dernierPoint, point) >= intervalleMetres) {
                pointsFiltres.add(point)
                dernierPoint = point
            }
        }

        logger.info("📉 Itinéraire filtré : ${pointsFiltres.size} points GPS (intervalle: $intervalleMetres m)")
        return pointsFiltres
    }

    /**
     * 🌍 **Extraction des villes situées sur un itinéraire**
     */
    private fun extraireVillesSurItineraire(itineraire: List<Pair<Double, Double>>, rayonKm: Double = 3.0): List<String> {
        val villes = mutableSetOf<String>()

        for (point in itineraire) {
            val ville = geocodeVilleInverse(point.first, point.second)

            if (ville != null) {
                val estProche = villes.any {
                    val coordExistante = geocodeVille(it)?.let { coord -> Pair(coord.first, coord.second) }
                    coordExistante != null && distanceGPS(coordExistante, point) <= rayonKm
                }

                if (!estProche) {
                    villes.add(ville)
                    logger.info("📍 Ville ajoutée sur l'itinéraire : $ville")
                } else {
                    logger.warn("⏭ Ville $ville ignorée (trop proche d'une autre déjà ajoutée)")
                }
            } else {
                logger.warn("❌ Aucune ville trouvée pour le point (${point.first}, ${point.second})")
            }
        }

        logger.info("📌 Villes finales retenues : $villes")
        return villes.toList()
    }



    /**
     * 🌍 **Géocodage inverse (Coordonnées GPS -> Nom de la ville)**
     */
    private fun geocodeVilleInverse(latitude: Double, longitude: Double): String? {
        val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude"

        return try {
            val (_, _, result) = url.httpGet().responseString()
            when (result) {
                is Result.Success -> {
                    val jsonData = objectMapper.readValue<Map<String, Any>>(result.get())
                    val address = jsonData["address"] as? Map<String, Any>

                    // Vérification élargie pour trouver un nom de ville
                    val ville = address?.get("city") as? String
                        ?: address?.get("town") as? String
                        ?: address?.get("village") as? String
                        ?: address?.get("hamlet") as? String // 🆕 Ajout des petits villages

                    if (ville != null) {
                        logger.info("📍 Géocodage inverse réussi : $latitude, $longitude -> $ville")
                    } else {
                        logger.warn("❌ Aucune ville trouvée pour les coordonnées : $latitude, $longitude")
                    }

                    ville
                }
                is Result.Failure -> {
                    logger.warn("❌ Erreur lors du géocodage inverse : ${result.error.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Exception lors du géocodage inverse : ${e.message}")
            null
        }
    }

    /**
     * 📏 **Calcul de la distance entre deux points GPS (Haversine)**
     */
    private fun distanceGPS(coord1: Pair<Double, Double>, coord2: Pair<Double, Double>): Double {
        val R = 6371.0 // Rayon de la Terre en km
        val dLat = Math.toRadians(coord2.first - coord1.first)
        val dLon = Math.toRadians(coord2.second - coord1.second)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(coord1.first)) * cos(Math.toRadians(coord2.first)) * sin(dLon / 2).pow(2)
        return 2 * R * atan2(sqrt(a), sqrt(1 - a)) * 1000 // Retourne la distance en mètres
    }



    /**
     * 📡 **Récupérer les stations pour une ville**
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
        val sourceName = if (useJsonSource) "📡 Source JSON" else "📡 Source XML"
        logger.info("$sourceName : Recherche des stations pour la ville : $city...")

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

            withContext(Dispatchers.Default) {  // Remplace Dispatchers.Main
                updateStations(enrichedStations)
                logger.info("✅ Modèle mis à jour avec ${enrichedStations.size} stations pour $city depuis $sourceName !")
            }
        }

    }
    /**
     * 🌍 **Récupérer les coordonnées GPS d’une station si elles sont manquantes**
     */
    private fun fetchCoordinatesFromNominatim(station: Station): Station? {
        val fullAddress = "${station.adresse}, ${station.codePostal}, ${station.ville}, FRANCE"

        // 📌 Vérifier si l'adresse est déjà dans le cache pour éviter les requêtes répétitives
        if (cacheGeocoding.containsKey(fullAddress)) {
            val (lat, lon) = cacheGeocoding[fullAddress]!!
            logger.info("📍 Coordonnées récupérées depuis le cache pour $fullAddress -> ($lat, $lon)")
            return station.copy(latitude = lat, longitude = lon)
        }

        // 🔗 Construire l'URL de requête Nominatim
        val encodedAddress = URLEncoder.encode(fullAddress, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedAddress"

        return try {
            val (_, _, result) = url.httpGet().responseString()

            when (result) {
                is Result.Success -> {
                    val jsonData = result.get()

                    // ✅ Parser la réponse JSON
                    val geoResults: List<Map<String, Any>> = objectMapper.readValue(jsonData)

                    if (geoResults.isNotEmpty()) {
                        val firstResult = geoResults[0]
                        val lat = firstResult["lat"]?.toString() ?: return null
                        val lon = firstResult["lon"]?.toString() ?: return null

                        // 📌 Stocker en cache pour éviter les requêtes répétées
                        cacheGeocoding[fullAddress] = Pair(lat, lon)

                        logger.info("📍 Géocodage réussi : $fullAddress -> ($lat, $lon)")
                        return station.copy(latitude = lat, longitude = lon)
                    } else {
                        logger.warn("❌ Aucun résultat trouvé pour $fullAddress")
                        null
                    }
                }

                is Result.Failure -> {
                    logger.error("❌ Erreur HTTP lors de la requête Nominatim : ${result.error.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Exception lors de la requête Nominatim : ${e.message}")
            null
        }
    }

}
