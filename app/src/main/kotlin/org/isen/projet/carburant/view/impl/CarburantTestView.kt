package org.isen.projet.carburant.view.impl

import org.isen.projet.carburant.ctrl.CarburantController
import org.isen.projet.carburant.view.ICarburantView
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyChangeEvent
import javax.swing.*

class CarburantTestView(val ctrl: CarburantController) : JFrame("Recherche de Stations"), ICarburantView, ActionListener {

    private val textArea = JTextArea()
    private val cityTextField = JTextField(15)

    // Checkboxes pour les critères supplémentaires
    private val fuelTypeComboBox = JComboBox(arrayOf("Aucun", "Gazole", "SP95", "SP98", "GPLc", "E10", "E85"))
    private val toiletsCheckBox = JCheckBox("Toilettes publiques")
    private val airPumpCheckBox = JCheckBox("Station de gonflage")
    private val foodShopCheckBox = JCheckBox("Boutique alimentaire")

    private val searchButton = JButton("Rechercher")

    init {
        ctrl.registerView(this)
        preferredSize = Dimension(600, 400)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        contentPane.layout = BorderLayout()

        // 🌟 Zone d'affichage des résultats
        textArea.isEditable = false
        contentPane.add(JScrollPane(textArea), BorderLayout.CENTER)

        // 🎛️ Panneau des critères de recherche
        val searchPanel = JPanel().apply {
            layout = GridLayout(3, 2, 5, 5)
            add(JLabel("Ville :"))
            add(cityTextField)
            add(JLabel("Type de carburant :"))
            add(fuelTypeComboBox)
            add(toiletsCheckBox)
            add(airPumpCheckBox)
            add(foodShopCheckBox)
        }

        // 🖱️ Bouton de recherche
        val buttonPanel = JPanel()
        buttonPanel.add(searchButton)
        searchButton.addActionListener(this)

        // 📌 Ajout des composants à la fenêtre
        val controlPanel = JPanel().apply {
            layout = BorderLayout()
            add(searchPanel, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
        }
        contentPane.add(controlPanel, BorderLayout.SOUTH)

        isVisible = true
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
            textArea.text = stations.joinToString("\n") { it.toString() }
        }
    }

    override fun actionPerformed(e: ActionEvent?) {
        val city = cityTextField.text.trim()
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer une ville !", "Erreur", JOptionPane.ERROR_MESSAGE)
            return
        }

        println("📡 Recherche en cours pour la ville : $city")

        val selectedFuel = if (fuelTypeComboBox.selectedItem == "Aucun") null else fuelTypeComboBox.selectedItem.toString()
        val hasToilets = toiletsCheckBox.isSelected
        val hasAirPump = airPumpCheckBox.isSelected
        val hasFoodShop = foodShopCheckBox.isSelected

        ctrl.updateModelForCity(city, selectedFuel, hasToilets, hasAirPump, hasFoodShop)
    }
}


