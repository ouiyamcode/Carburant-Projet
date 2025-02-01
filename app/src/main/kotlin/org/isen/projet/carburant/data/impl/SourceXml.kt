package org.isen.projet.carburant.data.impl

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.isen.projet.carburant.data.IDataSource
import org.isen.projet.carburant.model.Station
import javax.xml.parsers.DocumentBuilderFactory

class SourceXml : IDataSource {

    // URL de l'API OpenDataSoft pour récupérer les stations-service en XML
    private val url = "https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/prix-des-carburants-j-1/records?limit=100"

    override fun fetchData(): String {
        var data = ""

        val (request, response, result) = url.httpGet().responseString()

        when (result) {
            is Result.Success -> {
                data = result.get()
                println("🔍 Données récupérées :\n${data.take(500)}") // Afficher les 500 premiers caractères pour vérifier le format
            }
            is Result.Failure -> {
                println("❌ Erreur HTTP ${response.statusCode}: ${result.error.message}")
            }
        }

        return data
    }



    /**
     * Parse les données XML et retourne une liste d’objets Station
     */
    override fun parseData(data: String): List<Station> {
        val stations = mutableListOf<Station>()

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(data.byteInputStream())

        val stationNodes = doc.getElementsByTagName("pdv")

        for (i in 0 until stationNodes.length) {
            val node = stationNodes.item(i)

            val id = node.attributes.getNamedItem("id")?.nodeValue ?: "Inconnu"
            val latitude = node.attributes.getNamedItem("latitude")?.nodeValue ?: "0.0"
            val longitude = node.attributes.getNamedItem("longitude")?.nodeValue ?: "0.0"
            val cp = node.attributes.getNamedItem("cp")?.nodeValue ?: "00000"
            val ville = node.attributes.getNamedItem("ville")?.nodeValue ?: "Non précisée"

            var adresse = "Non précisée"
            val prixCarburants = mutableMapOf<String, String>()

            val enfants = node.childNodes
            for (j in 0 until enfants.length) {
                val element = enfants.item(j)

                when (element.nodeName) {
                    "adresse" -> adresse = element.textContent
                    "prix" -> {
                        val type = element.attributes.getNamedItem("nom")?.nodeValue ?: "Inconnu"
                        val prix = element.attributes.getNamedItem("valeur")?.nodeValue ?: "0.0"
                        prixCarburants[type] = prix
                    }
                }
            }

            // Ajout de la station-service dans la liste
            stations.add(Station(id, latitude, longitude, cp, ville, adresse, prixCarburants))
        }

        return stations
    }
}
