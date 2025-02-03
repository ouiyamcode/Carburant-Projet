package org.isen.projet.carburant.view.impl

import javafx.embed.swing.JFXPanel
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import org.isen.projet.carburant.ctrl.CarburantController
import org.isen.projet.carburant.model.Station
import org.isen.projet.carburant.view.ICarburantView
import java.awt.BorderLayout
import java.awt.Desktop
import java.beans.PropertyChangeEvent
import java.net.URI
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

class CarburantMapView(private val ctrl: CarburantController) : JFrame("🗺️ Carte des Stations"), ICarburantView {

    private lateinit var webEngine: WebEngine
    private val jfxPanel = JFXPanel()
    private val openBrowserButton = JButton("🌍 Ouvrir dans le navigateur").apply {
        addActionListener { openMapInBrowser() }
    }

    init {
        preferredSize = java.awt.Dimension(800, 600)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        contentPane.layout = BorderLayout()
        contentPane.add(jfxPanel, BorderLayout.CENTER)

        val buttonPanel = JPanel()
        buttonPanel.add(openBrowserButton)
        contentPane.add(buttonPanel, BorderLayout.SOUTH)

        Platform.runLater {
            val webView = WebView()
            webEngine = webView.engine
            val url = "file:///${System.getProperty("user.dir")}/src/main/resources/map.html" // ✅ Chargement local
            webEngine.load(url)
            jfxPanel.scene = Scene(webView)
        }

        isVisible = true
        pack()
    }

    override fun display() {
        isVisible = true
    }

    override fun close() {
        isVisible = false
    }

    /**
     * 📌 **Ouvre la carte dans le navigateur**
     */
    private fun openMapInBrowser() {
        val mapPath = "file:///home/ouiyam/TP/KOTLIN/Projet/app/build/resources/main/map.html"
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(mapPath))
        } else {
            println("❌ Desktop API non supportée")
        }
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == "stations") {
            val stations = (evt.newValue as? List<*>)?.filterIsInstance<Station>() ?: return

            val jsStations = stations.map {
                """{ "id": "${it.id}", "latitude": ${it.latitude}, "longitude": ${it.longitude}, "ville": "${it.ville}", "adresse": "${it.adresse}" }"""
            }.joinToString(",", "[", "]")

            Platform.runLater {
                println("📡 Envoi des stations à la carte : $jsStations") // Debug
                webEngine.executeScript("loadStations($jsStations)")
            }

            try {
                val url = URI("file:///home/ouiyam/TP/KOTLIN/Projet/app/build/resources/main/map.html?stations=$jsStations")
                Desktop.getDesktop().browse(url)
            } catch (e: Exception) {
                println("❌ Erreur lors de l'ouverture de la carte : ${e.message}")
            }
        }
    }
}

