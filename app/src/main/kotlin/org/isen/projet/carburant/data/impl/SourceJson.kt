package org.isen.projet.carburant.data.impl

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.isen.projet.carburant.data.IDataSource
import org.isen.projet.carburant.model.Station
import java.net.URLEncoder
import java.text.Normalizer

class SourceJson : IDataSource {

    private val baseUrl = "https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/prix-des-carburants-j-1/records"

    override fun fetchData(): String {
        return fetchDataFromUrl("$baseUrl?limit=100")
    }

    fun fetchDataForCity(city: String): String {
        val cityNormalized = normalizeText(city)
        val cityEncoded = URLEncoder.encode(cityNormalized, "UTF-8")
        val url = "$baseUrl?where=com_arm_name=\"$cityEncoded\"&limit=100"
        return fetchDataFromUrl(url)
    }

    private fun fetchDataFromUrl(url: String): String {
        var jsonData = ""

        val (_, response, result) = url.httpGet().responseString()

        when (result) {
            is Result.Success -> {
                jsonData = result.get()
                println("📡 JSON brut reçu : ${jsonData.take(500)}") // Vérification rapide
            }
            is Result.Failure -> {
                println("❌ Erreur HTTP ${response.statusCode}: ${result.error.message}")
            }
        }

        return jsonData
    }

    override fun parseData(data: String): List<Station> {
        println("📡 Parsing des données JSON...")

        val stationsMap = mutableMapOf<String, Station>()
        val mapper = jacksonObjectMapper()
        var countValidGPS = 0
        var countMissingGPS = 0

        try {
            val rootNode = mapper.readValue<Map<String, Any>>(data)
            val records = rootNode["results"] as? List<Map<String, Any>> ?: return emptyList()

            for (record in records) {
                val id = record["id"]?.toString() ?: continue

                // 🔥 Correction : Vérification et extraction correcte de `geo_point`
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

                // 🔥 Vérification pour éviter les doublons
                if (!stationsMap.containsKey(id)) {
                    stationsMap[id] = Station(id, latitude, longitude, codePostal, ville, adresse, prixCarburants)

                    if (latitude != "0.0" && longitude != "0.0") {
                        countValidGPS++
                    } else {
                        countMissingGPS++
                        println("⚠️ Station ID=$id sans GPS: $ville, $adresse")
                    }
                }
            }

        } catch (e: Exception) {
            println("❌ Erreur lors du parsing JSON: ${e.message}")
        }

        println("📊 Résumé: ${countValidGPS} stations avec GPS, ${countMissingGPS} sans GPS.")

        return stationsMap.values.toList()
    }

    // 🔥 Fonction pour normaliser les noms (supprimer accents et convertir en majuscules)
    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .uppercase()
    }
}









