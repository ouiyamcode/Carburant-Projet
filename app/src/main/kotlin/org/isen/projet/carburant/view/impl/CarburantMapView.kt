package org.isen.projet.carburant.view.impl

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import org.isen.projet.carburant.ctrl.CarburantController
import org.isen.projet.carburant.model.Station
import org.isen.projet.carburant.view.ICarburantView
import java.io.File
import javax.swing.JFrame
import javax.swing.WindowConstants

class CarburantMapView(private val ctrl: CarburantController) : JFrame("🗺️ Carte des Stations"), ICarburantView {

    private val jfxPanel = JFXPanel()
    private lateinit var webEngine: WebEngine

    init {
        setSize(1200, 600)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        contentPane.add(jfxPanel)

        Platform.runLater {
            val webView = WebView()
            webEngine = webView.engine
            val mapFile = File("app/src/main/resources/map.html")

            if (mapFile.exists()) {
                webEngine.load(mapFile.toURI().toString())
            } else {
                webEngine.loadContent("<html><body><h2>Carte non disponible</h2></body></html>")
            }

            jfxPanel.scene = Scene(webView)
        }

        isVisible = true
    }

    override fun display() {
        isVisible = true
    }

    override fun close() {
        isVisible = false
    }

    override fun propertyChange(evt: java.beans.PropertyChangeEvent) {
        if (evt.propertyName == "stations") {
            val stations = evt.newValue as? List<Station> ?: return
            Platform.runLater {
                updateStationsOnMap(stations)
            }
        }
    }

    fun updateStationsOnMap(stations: List<Station>) {
        val jsonStations = stations.joinToString(",") { station ->
            """{"latitude": ${station.latitude}, "longitude": ${station.longitude}, "ville": "${station.ville}", "adresse": "${station.adresse}", "codePostal": "${station.codePostal}", "prixCarburants": ${station.prixCarburants.map { "\"${it.key}\": \"${it.value}\"" }.joinToString(", ", "{", "}")}}"""
        }

        val script = """updateStations([$jsonStations]);"""
        webEngine.executeScript(script) // Envoie les nouvelles stations au script JS
    }
    /**
     * 🚗 **Afficher l’itinéraire sur la carte Leaflet**
     * @param itineraire Liste des points GPS de l’itinéraire
     */
    fun updateRouteOnMap(itineraire: List<Pair<Double, Double>>) {
        val jsonItineraire = itineraire.joinToString(",") { point ->
            """[${point.first}, ${point.second}]"""
        }

        val script = """updateRoute([$jsonItineraire]);"""
        Platform.runLater {
            webEngine.executeScript(script) // Envoie les données à Leaflet
        }
    }

}
