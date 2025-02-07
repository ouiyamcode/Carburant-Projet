package org.isen.projet.carburant.view.impl

import org.isen.projet.carburant.ctrl.CarburantController
import org.isen.projet.carburant.model.Station
import org.isen.projet.carburant.view.ICarburantView
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyChangeEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class CarburantTestView(private val ctrl: CarburantController) : JFrame("🔍 Recherche de Stations"), ICarburantView, ActionListener {

    private val modeComboBox = JComboBox(arrayOf("🔎 Recherche par Ville", "🚗 Mode Itinéraire"))

    private val cityTextField = JTextField(15)
    private val fuelTypeComboBox = JComboBox(arrayOf("Aucun", "Gazole", "SP95", "SP98", "GPLc", "E10", "E85"))

    private val toiletsCheckBox = JCheckBox("🚻 Toilettes publiques")
    private val airPumpCheckBox = JCheckBox(" Station de gonflage")
    private val foodShopCheckBox = JCheckBox("🛒 Boutique alimentaire")

    private val departTextField = JTextField(15)
    private val arriveeTextField = JTextField(15)

    private val primarySourceButton = JButton("🌍 Source Principale").apply {
        background = Color(46, 204, 113)
        foreground = Color.WHITE
        font = Font("Arial", Font.BOLD, 14)
        isFocusPainted = false
        addActionListener(this@CarburantTestView)
    }

    private val secondarySourceButton = JButton("📡 Source Secondaire").apply {
        background = Color(52, 152, 219)
        foreground = Color.WHITE
        font = Font("Arial", Font.BOLD, 14)
        isFocusPainted = false
        addActionListener(this@CarburantTestView)
    }

    private val openMapButton = JButton("🗺️ Ouvrir la Carte").apply {
        background = Color(155, 89, 182)
        foreground = Color.WHITE
        font = Font("Arial", Font.BOLD, 14)
        isFocusPainted = false
        isEnabled = false
        addActionListener {
            val mapView = CarburantMapView(ctrl) // Nouvelle instance à chaque clic
            mapView.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE // Assurer la fermeture sans bloquer
            mapView.isVisible = true
        }
    }

    private val itineraireButton = JButton("🚗 Afficher Itinéraire").apply {
        background = Color(241, 196, 15)
        foreground = Color.BLACK
        font = Font("Arial", Font.BOLD, 14)
        isFocusPainted = false
        addActionListener(this@CarburantTestView)
    }

    private val tableModel = DefaultTableModel(arrayOf("ID", "Ville", "Adresse", "Latitude", "Longitude", "Prix"), 0)
    private val resultTable = JTable(tableModel).apply {
        font = Font("Arial", Font.PLAIN, 14)
        rowHeight = 50
        setShowGrid(true)
        gridColor = Color.GRAY
        autoResizeMode = JTable.AUTO_RESIZE_OFF
        background = Color(230, 230, 230)
        selectionBackground = Color(52, 152, 219)
        selectionForeground = Color.WHITE
        getColumnModel().getColumn(5).cellRenderer = MultiLineTableCellRenderer()
    }

    init {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ctrl.registerView(this)
        preferredSize = Dimension(1200, 600)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        contentPane.background = Color(173, 216, 230)

        modeComboBox.addActionListener { updateMode() }

        val resultPanel = JPanel(BorderLayout()).apply {
            border = EmptyBorder(10, 10, 10, 10)
            background = Color(230, 230, 230)
            val scrollPane = JScrollPane(resultTable).apply {
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = Dimension(1150, 300)
            }
            add(scrollPane, BorderLayout.CENTER)
        }

        val columnWidths = intArrayOf(100, 150, 350, 120, 120, 450)
        for (i in columnWidths.indices) {
            resultTable.columnModel.getColumn(i).preferredWidth = columnWidths[i]
        }
        for (i in 0 until resultTable.rowCount) {
            resultTable.setRowHeight(i, 200) // Met toutes les lignes à 60 pixels
        }




        val searchPanel = JPanel(GridBagLayout()).apply {
            border = EmptyBorder(10, 10, 10, 10)
            background = Color(173, 216, 230)
            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                insets = Insets(5, 5, 5, 5)
            }

            add(JLabel("🔁 Mode de recherche :"), gbc)
            gbc.gridx = 1
            add(modeComboBox, gbc)

            gbc.gridx = 0
            gbc.gridy = 1
            add(JLabel("🏙️ Ville :"), gbc)
            gbc.gridx = 1
            add(cityTextField, gbc)

            gbc.gridx = 0
            gbc.gridy = 2
            add(JLabel("⛽ Type de carburant :"), gbc)
            gbc.gridx = 1
            add(fuelTypeComboBox, gbc)

            gbc.gridx = 0
            gbc.gridy = 3
            add(toiletsCheckBox, gbc)
            gbc.gridx = 1
            add(airPumpCheckBox, gbc)

            gbc.gridx = 0
            gbc.gridy = 4
            add(foodShopCheckBox, gbc)

            gbc.gridx = 0
            gbc.gridy = 5
            add(JLabel("🚦 Départ :"), gbc)
            gbc.gridx = 1
            add(departTextField, gbc)

            gbc.gridx = 0
            gbc.gridy = 6
            add(JLabel("🏁 Arrivée :"), gbc)
            gbc.gridx = 1
            add(arriveeTextField, gbc)
        }

        val buttonPanel = JPanel().apply {
            background = Color(52, 73, 94)
            add(primarySourceButton)
            add(secondarySourceButton)
            add(openMapButton)
            add(itineraireButton)
        }

        val controlPanel = JPanel(BorderLayout()).apply {
            add(searchPanel, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
        }

        contentPane.layout = BorderLayout()
        contentPane.add(resultPanel, BorderLayout.CENTER)
        contentPane.add(controlPanel, BorderLayout.SOUTH)

        updateMode()
        isVisible = true
        pack()
    }

    override fun actionPerformed(e: ActionEvent?) {
        val mode = modeComboBox.selectedIndex
        when (e?.source) {
            primarySourceButton, secondarySourceButton -> {
                if (mode == 0) {
                    val city = cityTextField.text.trim()
                    val selectedFuel = if (fuelTypeComboBox.selectedItem == "Aucun") null else fuelTypeComboBox.selectedItem.toString()
                    val hasToilets = toiletsCheckBox.isSelected
                    val hasAirPump = airPumpCheckBox.isSelected
                    val hasFoodShop = foodShopCheckBox.isSelected
                    if (city.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "🚨 Veuillez entrer une ville !", "Erreur", JOptionPane.ERROR_MESSAGE)
                        return
                    }
                    ctrl.updateModelForCityWithSource(city, e.source == primarySourceButton, selectedFuel, hasToilets, hasAirPump, hasFoodShop)
                }
            }
            itineraireButton -> {
                if (mode == 1) {
                    val depart = departTextField.text.trim()
                    val arrivee = arriveeTextField.text.trim()

                    if (depart.isEmpty() || arrivee.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "🚨 Veuillez entrer une ville de départ et d'arrivée !", "Erreur", JOptionPane.ERROR_MESSAGE)
                        return
                    }
                    ctrl.updateModelForItineraireWithSource(depart, arrivee, false)
                }
            }
        }
    }

    private fun updateMode() {
        val mode = modeComboBox.selectedIndex
        cityTextField.isEnabled = (mode == 0)
        departTextField.isEnabled = (mode == 1)
        arriveeTextField.isEnabled = (mode == 1)
        itineraireButton.isEnabled = (mode == 1)
        primarySourceButton.isEnabled = (mode == 0)
        secondarySourceButton.isEnabled = (mode == 0)
        toiletsCheckBox.isEnabled = (mode == 0)
        airPumpCheckBox.isEnabled = (mode == 0)
        foodShopCheckBox.isEnabled = (mode == 0)
        fuelTypeComboBox.isEnabled = (mode == 0)
    }

    override fun display() {
        isVisible = true
    }

    override fun close() {
        isVisible = false
    }

    override fun propertyChange(evt: PropertyChangeEvent?) {
        if (evt == null) return

        if (evt.propertyName == "stations") {
            val stations = (evt.newValue as? List<*>)?.filterIsInstance<Station>() ?: emptyList()
            tableModel.rowCount = 0 // Vide la table avant d'ajouter de nouvelles valeurs
            for (station in stations) {
                val prix = station.prixCarburants.entries.joinToString("\n") { "${it.key}: ${it.value}€" }
                tableModel.addRow(arrayOf(station.id, station.ville, station.adresse, station.latitude, station.longitude, prix))
            }
            openMapButton.isEnabled = stations.isNotEmpty()
        }
    }

    class MultiLineTableCellRenderer : JScrollPane(), TableCellRenderer {
        private val textArea = JTextArea()

        init {
            textArea.lineWrap = true
            textArea.wrapStyleWord = true
            textArea.isOpaque = true
            textArea.border = null
            viewport.add(textArea)
            verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_NEVER
        }

        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean,
            hasFocus: Boolean, row: Int, column: Int
        ): Component {
            textArea.text = value?.toString() ?: ""
            textArea.background = if (isSelected) table.selectionBackground else table.background
            textArea.foreground = if (isSelected) table.selectionForeground else table.foreground
            return this
        }
    }



}
