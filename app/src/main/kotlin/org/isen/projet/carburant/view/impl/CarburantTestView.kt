package org.isen.projet.carburant.view.impl

import org.isen.projet.carburant.ctrl.CarburantController
import org.isen.projet.carburant.view.ICarburantView
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyChangeEvent
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

class CarburantTestView(val ctrl: CarburantController) : JFrame("CarburantTestView"), ICarburantView, ActionListener {
    init{
        ctrl.registerView(this);
        preferredSize = Dimension(300,200)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        contentPane = JPanel().apply{ layout = GridLayout(1,2) }
        val myButton = JButton("push")
        myButton.addActionListener(this)
        contentPane.add(myButton)
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
        println("event : $evt")
    }

    override fun actionPerformed(e: ActionEvent?) {
        println("push")
        this.ctrl.updateModel()
    }
}