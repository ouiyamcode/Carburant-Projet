package org.isen.projet.carburant.view.impl

import org.isen.projet.carburant.ctrl.CarburantController
import org.isen.projet.carburant.view.ICarburantView
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyChangeEvent
import javax.swing.*

class CarburantTestView(val ctrl: CarburantController) : JFrame("Recherche de Stations-Service"), ICarburantView, ActionListener {

    private val textArea = JTextArea()
    private val searchField = JTextField(15)
    private val searchButton = JButton("Rechercher")

    init {
        ctrl.registerView(this)
        preferredSize = Dimension(500, 400)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        layout = BorderLayout()

        // ✅ Zone d'affichage en haut
        textArea.isEditable = false
        val scrollPane = JScrollPane(textArea)
        scrollPane.preferredSize = Dimension(500, 300)
        add(scrollPane, BorderLayout.CENTER)

        // ✅ Panneau de recherche en bas
        val searchPanel = JPanel(FlowLayout()).apply {
            add(JLabel("Ville:"))
            add(searchField)
            add(searchButton)
        }
        add(searchPanel, BorderLayout.SOUTH)

        searchButton.addActionListener(this)

        isVisible = false
        pack()
    }

    override fun display() {
        isVisible = true
    }

    override fun close() {
        isVisible = false
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == "stations") {
            val stations = evt.newValue as List<*>
            textArea.text = if (stations.isNotEmpty()) {
                stations.joinToString("\n") { it.toString() }
            } else {
                "🚫 Aucune station trouvée pour cette ville."
            }
        }
    }

    override fun actionPerformed(e: ActionEvent?) {
        val city = searchField.text.trim()
        if (city.isNotEmpty()) {
            println("📡 Recherche en cours pour la ville : $city")
            ctrl.updateModelForCity(city)
        } else {
            textArea.text = "⚠️ Veuillez entrer un nom de ville."
        }
    }
}

