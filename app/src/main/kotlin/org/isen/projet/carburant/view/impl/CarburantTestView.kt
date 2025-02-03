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

    private val cityTextField = JTextField(15)
    private val fuelTypeComboBox = JComboBox(arrayOf("Aucun", "Gazole", "SP95", "SP98", "GPLc", "E10", "E85"))

    private val toiletsCheckBox = JCheckBox("🚻 Toilettes publiques")
    private val airPumpCheckBox = JCheckBox("🛞 Station de gonflage")
    private val foodShopCheckBox = JCheckBox("🛒 Boutique alimentaire")

    private val searchButton = JButton("🔎 Rechercher").apply {
        background = Color(231, 76, 60)
        foreground = Color.WHITE
        font = Font("Arial", Font.BOLD, 16)
        isFocusPainted = false
        border = BorderFactory.createLineBorder(Color(192, 57, 43), 2)
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

        // ✅ Appliquer le renderer multi-ligne pour la colonne Prix
        getColumnModel().getColumn(5).cellRenderer = MultiLineTableCellRenderer()
    }

    init {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ctrl.registerView(this)
        preferredSize = Dimension(1100, 600)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

        contentPane.background = Color(44, 62, 80)

        val resultPanel = JPanel(BorderLayout()).apply {
            border = EmptyBorder(10, 10, 10, 10)
            background = Color(230, 230, 230)
            val scrollPane = JScrollPane(resultTable).apply {
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = Dimension(1050, 250)
            }
            add(scrollPane, BorderLayout.CENTER)
        }

        val columnWidths = intArrayOf(80, 120, 300, 100, 100, 450)
        for (i in columnWidths.indices) {
            resultTable.columnModel.getColumn(i).preferredWidth = columnWidths[i]
        }

        val searchPanel = JPanel(GridBagLayout()).apply {
            border = EmptyBorder(10, 10, 10, 10)
            background = Color(52, 73, 94)
            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                insets = Insets(5, 5, 5, 5)
            }

            add(JLabel("🏙️ Ville :"), gbc)
            gbc.gridx = 1
            add(cityTextField, gbc)

            gbc.gridx = 0
            gbc.gridy = 1
            add(JLabel("⛽ Type de carburant :"), gbc)
            gbc.gridx = 1
            add(fuelTypeComboBox, gbc)

            gbc.gridx = 0
            gbc.gridy = 2
            add(toiletsCheckBox, gbc)
            gbc.gridx = 1
            add(airPumpCheckBox, gbc)

            gbc.gridx = 0
            gbc.gridy = 3
            add(foodShopCheckBox, gbc)
        }

        val buttonPanel = JPanel().apply {
            background = Color(52, 73, 94)
            add(searchButton)
        }
        searchButton.addActionListener(this)

        val controlPanel = JPanel(BorderLayout()).apply {
            add(searchPanel, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
        }

        contentPane.layout = BorderLayout()
        contentPane.add(resultPanel, BorderLayout.CENTER)
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
            val stations = (evt.newValue as? List<*>)?.filterIsInstance<Station>() ?: emptyList()
            tableModel.rowCount = 0
            for (station in stations) {
                val prix = station.prixCarburants.entries.joinToString("\n") { "${it.key}: ${it.value}€" }
                tableModel.addRow(arrayOf(station.id, station.ville, station.adresse, station.latitude, station.longitude, prix))
            }
        }
    }

    override fun actionPerformed(e: ActionEvent?) {
        val city = cityTextField.text.trim()
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(this, "🚨 Veuillez entrer une ville !", "Erreur", JOptionPane.ERROR_MESSAGE)
            return
        }

        val selectedFuel = if (fuelTypeComboBox.selectedItem == "Aucun") null else fuelTypeComboBox.selectedItem.toString()
        val hasToilets = toiletsCheckBox.isSelected
        val hasAirPump = airPumpCheckBox.isSelected
        val hasFoodShop = foodShopCheckBox.isSelected

        ctrl.updateModelForCity(city, selectedFuel, hasToilets, hasAirPump, hasFoodShop)
    }

    /** ✅ Correction : Ajout de MultiLineTableCellRenderer directement ici */
    class MultiLineTableCellRenderer : JTextArea(), TableCellRenderer {
        init {
            lineWrap = true
            wrapStyleWord = true
            isOpaque = true
        }

        override fun getTableCellRendererComponent(
                table: JTable, value: Any?, isSelected: Boolean,
                hasFocus: Boolean, row: Int, column: Int
        ): Component {
            text = value?.toString() ?: ""
            if (isSelected) {
                background = table.selectionBackground
                foreground = table.selectionForeground
            } else {
                background = table.background
                foreground = table.foreground
            }
            return this
        }
    }
}
