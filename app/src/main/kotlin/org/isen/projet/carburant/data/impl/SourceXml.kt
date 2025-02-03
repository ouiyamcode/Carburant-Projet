package org.isen.projet.carburant.data.impl

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.isen.projet.carburant.data.IDataSource
import org.isen.projet.carburant.model.Station
import java.io.StringReader
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

class SourceXml : IDataSource {

    private val baseUrl = "https://www.prix-carburants.gouv.fr/rubrique/opendata"

    override fun fetchData(): String {
        return fetchDataFromUrl("$baseUrl")
    }

    fun fetchDataForCity(
        city: String,
        fuelType: String? = null,
        hasToilets: Boolean = false,
        hasAirPump: Boolean = false,
        hasFoodShop: Boolean = false
    ): String {
        val cityEncoded = URLEncoder.encode(city, "UTF-8")
        val url = "$baseUrl?ville=$cityEncoded"
        return fetchDataFromUrl(url)
    }

    private fun fetchDataFromUrl(url: String): String {
        var xmlData = ""

        val (request, response, result) = url.httpGet().responseString()

        when (result) {
            is Result.Success -> {
                xmlData = result.get()
                println("📡 XML brut reçu : ${xmlData.take(500)}") // Vérification rapide
            }
            is Result.Failure -> {
                println("❌ Erreur HTTP ${response.statusCode}: ${result.error.message}")
            }
        }

        return xmlData
    }

    override fun parseData(data: String): List<Station> {
        println("📡 Parsing des données XML...")

        val stationsList = mutableListOf<Station>()

        try {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(data)))
            val stationNodes = document.getElementsByTagName("pdv") // 🔥 On récupère toutes les stations

            for (i in 0 until stationNodes.length) {
                val stationElement = stationNodes.item(i) as org.w3c.dom.Element

                val id = stationElement.getAttribute("id") ?: "Inconnu"
                val latitude = (stationElement.getAttribute("latitude").toDoubleOrNull()?.div(100000))?.toString() ?: "0.0"
                val longitude = (stationElement.getAttribute("longitude").toDoubleOrNull()?.div(100000))?.toString() ?: "0.0"
                val codePostal = stationElement.getAttribute("cp") ?: "00000"
                val ville = stationElement.getAttribute("ville") ?: "Non précisée"

                val adresseNode = stationElement.getElementsByTagName("adresse").item(0)
                val adresse = adresseNode?.textContent ?: "Non précisée"

                // 🔥 Récupération des prix des carburants
                val prixCarburants = mutableMapOf<String, String>()
                val prixNodes = stationElement.getElementsByTagName("prix")
                for (j in 0 until prixNodes.length) {
                    val prixElement = prixNodes.item(j) as org.w3c.dom.Element
                    val typeCarburant = prixElement.getAttribute("nom")
                    val prix = prixElement.getAttribute("valeur")
                    if (typeCarburant.isNotEmpty() && prix.isNotEmpty()) {
                        prixCarburants[typeCarburant] = prix
                    }
                }

                // 🚀 Ajouter la station à la liste finale
                stationsList.add(Station(id, latitude, longitude, codePostal, ville, adresse, prixCarburants))
            }

        } catch (e: Exception) {
            println("❌ Erreur lors du parsing XML: ${e.message}")
        }

        return stationsList
    }
}
