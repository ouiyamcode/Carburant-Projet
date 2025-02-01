package org.isen.projet.carburant.view.impl

import org.isen.projet.carburant.ctrl.CarburantController
import org.isen.projet.carburant.view.ICarburantView
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyChangeEvent
import javax.swing.*

class CarburantTestView(val ctrl: CarburantController) : JFrame("CarburantTestView"), ICarburantView, ActionListener {

    private val textArea = JTextArea()

    init {
        ctrl.registerView(this)
        preferredSize = Dimension(400, 300)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        contentPane = JPanel().apply { layout = GridLayout(2, 1) }

        val myButton = JButton("Charger les stations")
        myButton.addActionListener(this)

        contentPane.add(myButton)
        contentPane.add(JScrollPane(textArea))

        isVisible = false
        pack()
    }

    override fun display() {
        isVisible = true
    }

    override fun close() {
        isVisible = false
    }

    // Lorsque les données du modèle changent, on met à jour l'affichage
    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == "stations") {
            val stations = evt.newValue as List<*>
            textArea.text = stations.joinToString("\n") { it.toString() }
        }
    }

    override fun actionPerformed(e: ActionEvent?) {
        println("📡 Bouton pressé : récupération des stations en cours...")
        ctrl.updateModel()
    }
}
