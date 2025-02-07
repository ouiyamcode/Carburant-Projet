package org.isen.projet.carburant.data.impl

import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.result.Result
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.isen.projet.carburant.data.IDataSource
import org.isen.projet.carburant.model.Station
import java.io.File
import java.io.StringReader
import java.text.Normalizer
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import java.nio.charset.StandardCharsets

class SourceXml : IDataSource {

    private val zipUrl = "https://donnees.roulez-eco.fr/opendata/instantane"
    private val dataDirectory = "data"
    private val zipFilePath = "$dataDirectory/prix-carburants.zip"
    private val extractedXmlPath = "$dataDirectory/PrixCarburants_instantane.xml"
    private val logger: Logger = LogManager.getLogger(SourceXml::class.java)

    override fun fetchData(): String {
        val dataDir = File(dataDirectory)
        if (!dataDir.exists()) dataDir.mkdirs()

        if (!downloadFile(zipUrl, zipFilePath)) {
            logger.error("❌ Erreur : Impossible de télécharger le fichier ZIP.")
            return ""
        }

        if (!extractZipFile(zipFilePath, extractedXmlPath)) {
            logger.error("❌ Erreur : Impossible d'extraire le fichier XML.")
            return ""
        }

        val xmlFile = File(extractedXmlPath)
        return if (xmlFile.exists() && xmlFile.length() > 0) {
            xmlFile.readText(Charsets.UTF_8) // Force la lecture en UTF-8
        } else {
            logger.error("❌ Erreur : Le fichier XML extrait est vide.")
            ""
        }
    }

    override fun fetchDataForCity(
        city: String,
        fuelType: String?,
        hasToilets: Boolean,
        hasAirPump: Boolean,
        hasFoodShop: Boolean
    ): List<Station> {
        val rawData = fetchData()
        return if (rawData.isNotEmpty()) {
            parseData(rawData).filter { station ->
                normalizeText(station.ville) == normalizeText(city) &&
                        (fuelType == null || station.prixCarburants.containsKey(fuelType)) &&
                        (!hasToilets || stationContainsService(rawData, station.id, "Toilettes publiques")) &&
                        (!hasAirPump || stationContainsService(rawData, station.id, "Station de gonflage")) &&
                        (!hasFoodShop || stationContainsService(rawData, station.id, "Boutique alimentaire"))
            }
        } else {
            emptyList()
        }
    }

    override fun parseData(data: String): List<Station> {
        if (data.isEmpty()) {
            logger.error("❌ Erreur : Données XML vides.")
            return emptyList()
        }

        logger.info("📡 Parsing des données XML...")

        val stationsList = mutableListOf<Station>()

        try {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(data)))
            val stationNodes = document.getElementsByTagName("pdv")

            for (i in 0 until stationNodes.length) {
                val stationElement = stationNodes.item(i) as org.w3c.dom.Element

                val id = stationElement.getAttribute("id") ?: continue
                val latitude = (stationElement.getAttribute("latitude").toDoubleOrNull()?.div(100000))?.toString() ?: "0.0"
                val longitude = (stationElement.getAttribute("longitude").toDoubleOrNull()?.div(100000))?.toString() ?: "0.0"
                val codePostal = stationElement.getAttribute("cp") ?: "00000"

                val villeNode = stationElement.getElementsByTagName("ville").item(0)
                val rawVille = villeNode?.textContent ?: "Non précisée"
                val ville = normalizeText(rawVille)

                val adresseNode = stationElement.getElementsByTagName("adresse").item(0)
                val rawAdresse = adresseNode?.textContent ?: "Non précisée"
                val adresse = normalizeText(rawAdresse)

                /*logger.info("🔍 Ville brute : $rawVille | Ville normalisée : $ville")
                logger.info("🔍 Adresse brute : $rawAdresse | Adresse normalisée : $adresse")*/

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

                stationsList.add(Station(id, latitude, longitude, codePostal, ville, adresse, prixCarburants))
            }

        } catch (e: Exception) {
            logger.error("❌ Erreur lors du parsing XML: ${e.message}")
        }

        logger.info("✅ ${stationsList.size} stations extraites avec succès.")
        return stationsList
    }

    private fun stationContainsService(xmlData: String, stationId: String, serviceName: String): Boolean {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(xmlData)))
        val stationNodes = document.getElementsByTagName("pdv")

        for (i in 0 until stationNodes.length) {
            val stationElement = stationNodes.item(i) as org.w3c.dom.Element
            if (stationElement.getAttribute("id") == stationId) {
                val servicesNodes = stationElement.getElementsByTagName("service")
                for (j in 0 until servicesNodes.length) {
                    val serviceElement = servicesNodes.item(j)
                    if (serviceElement.textContent == serviceName) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun downloadFile(url: String, destinationPath: String): Boolean {
        logger.info("📡 Téléchargement du fichier ZIP...")

        val (_, _, result) = url.httpDownload().fileDestination { _, _ -> File(destinationPath) }.response()
        return when (result) {
            is Result.Success -> {
                logger.info("✅ Fichier ZIP téléchargé avec succès.")
                true
            }
            is Result.Failure -> {
                logger.error("❌ Erreur lors du téléchargement du ZIP : ${result.error.message}")
                false
            }
        }
    }

    private fun extractZipFile(zipFilePath: String, outputFilePath: String): Boolean {
        return try {
            val zipFile = ZipFile(zipFilePath)
            val entry = zipFile.entries().asSequence().firstOrNull { it.name.endsWith(".xml") }
            if (entry != null) {
                zipFile.getInputStream(entry).use { input ->
                    File(outputFilePath).outputStream().use { output -> input.copyTo(output) }
                }
                logger.info("📂 Fichier XML extrait avec succès.")
                true
            } else {
                logger.error("❌ Aucun fichier XML trouvé dans le ZIP.")
                false
            }
        } catch (e: Exception) {
            logger.error("❌ Erreur lors de l'extraction du ZIP : ${e.message}")
            false
        }
    }
    
    private fun normalizeText(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "") // Supprime les accents
            .replace("œ", "oe")
            .replace("æ", "ae")
            .replace("É", "E").replace("é", "e")
            .replace("È", "E").replace("è", "e")
            .replace("Ê", "E").replace("ê", "e")
            .replace("Ë", "E").replace("ë", "e")
            .replace("À", "A").replace("à", "a")
            .replace("Ù", "U").replace("ù", "u")
            .replace("Î", "I").replace("î", "i")
            .replace("Ï", "I").replace("ï", "i")
            .replace("Ô", "O").replace("ô", "o")

        return normalized.uppercase()
    }

}
