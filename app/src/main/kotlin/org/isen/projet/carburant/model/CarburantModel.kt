package org.isen.projet.carburant.model

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import kotlin.properties.Delegates

class CarburantModel {
    private val pcs = PropertyChangeSupport(this)

    private var dummy: Int by Delegates.observable(0){ property, oldValue, newValue ->
        pcs.firePropertyChange(property.name, oldValue, newValue)
    }

    fun update(nb:Int){
        dummy = nb
    }

    fun addObserver(l : PropertyChangeListener){
        pcs.addPropertyChangeListener(l)
    }
}