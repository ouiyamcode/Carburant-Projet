package org.isen.projet.carburant.data.impl

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.isen.projet.carburant.data.IDataSource
import org.isen.projet.carburant.model.Station
import java.net.URLEncoder
import java.text.Normalizer

class SourceJson : IDataSource {

    private val baseUrl = "https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/prix-des-carburants-j-1/records"
    private val logger: Logger = LogManager.getLogger(SourceJson::class.java)

    override fun fetchData(): String {
        return fetchDataFromUrl("$baseUrl?limit=100")
    }

    override fun fetchDataForCity(
        city: String,
        fuelType: String?,
        hasToilets: Boolean,
        hasAirPump: Boolean,
        hasFoodShop: Boolean
    ): List<Station> {
        val cityNormalized = normalizeText(city)
        val cityEncoded = URLEncoder.encode(cityNormalized, "UTF-8")

        var filters = "com_arm_name=\"$cityEncoded\""

        if (fuelType != null) {
            val fuelEncoded = URLEncoder.encode(fuelType, "UTF-8")
            filters += " AND fuel=\"$fuelEncoded\""
        }
        if (hasToilets) filters += " AND services LIKE \"%Toilettes publiques%\""
        if (hasAirPump) filters += " AND services LIKE \"%Station de gonflage%\""
        if (hasFoodShop) filters += " AND services LIKE \"%Boutique alimentaire%\""

        val url = "$baseUrl?where=$filters&limit=100"

        val jsonData = fetchDataFromUrl(url)
        return parseData(jsonData)
    }

    private fun fetchDataFromUrl(url: String): String {
        var jsonData = ""

        val (_, response, result) = url.httpGet().responseString()

        when (result) {
            is Result.Success -> {
                jsonData = result.get()
                logger.info("📡 JSON brut reçu : ${jsonData.take(500)}") // Vérification rapide
            }
            is Result.Failure -> {
                logger.error("❌ Erreur HTTP ${response.statusCode}: ${result.error.message}")
            }
        }

        return jsonData
    }

    override fun parseData(data: String): List<Station> {
        logger.info("📡 Parsing des données JSON...")

        val stationsMap = mutableMapOf<String, Station>()
        val mapper = jacksonObjectMapper()
        var countValidGPS = 0
        var countMissingGPS = 0

        try {
            val rootNode = mapper.readValue<Map<String, Any>>(data)
            val records = rootNode["results"] as? List<Map<String, Any>> ?: return emptyList()

            for (record in records) {
                val id = record["id"]?.toString() ?: continue

                var latitude = "0.0"
                var longitude = "0.0"

                if (record["geo_point"] is Map<*, *>) {
                    val geoPoint = record["geo_point"] as Map<*, *>
                    latitude = geoPoint["lat"]?.toString() ?: "0.0"
                    longitude = geoPoint["lon"]?.toString() ?: "0.0"
                }

                val codePostal = record["cp"]?.toString() ?: "00000"
                val ville = normalizeText(record["com_arm_name"]?.toString() ?: "Non précisée")
                val adresse = record["address"]?.toString() ?: "Non précisée"

                val prixCarburants = mutableMapOf<String, String>()
                val carburantKeys = mapOf(
                    "price_gazole" to "Gazole",
                    "price_sp95" to "SP95",
                    "price_sp98" to "SP98",
                    "price_gplc" to "GPLc",
                    "price_e10" to "E10",
                    "price_e85" to "E85"
                )

                for ((jsonKey, nomCarburant) in carburantKeys) {
                    if (record[jsonKey] != null) {
                        prixCarburants[nomCarburant] = (record[jsonKey] as Number).toString()
                    }
                }

                if (!stationsMap.containsKey(id)) {
                    stationsMap[id] = Station(id, latitude, longitude, codePostal, ville, adresse, prixCarburants)

                    if (latitude != "0.0" && longitude != "0.0") {
                        countValidGPS++
                    } else {
                        countMissingGPS++
                        logger.warn("⚠️ Station ID=$id sans GPS: $ville, $adresse")
                    }
                }
            }

        } catch (e: Exception) {
            logger.error("❌ Erreur lors du parsing JSON: ${e.message}")
        }

        logger.info("📊 Résumé: ${countValidGPS} stations avec GPS, ${countMissingGPS} sans GPS.")

        return stationsMap.values.toList()
    }

    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .uppercase()
    }
}
