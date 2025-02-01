package org.isen.projet.carburant.view

import java.beans.PropertyChangeListener

interface ICarburantView: PropertyChangeListener{
    fun display()
    fun close()
}