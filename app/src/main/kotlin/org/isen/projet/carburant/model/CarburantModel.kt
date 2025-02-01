package org.isen.projet.carburant.model

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import kotlin.properties.Delegates

class CarburantModel {
    private val pcs = PropertyChangeSupport(this)

    // Liste des stations-service
    var stations: List<Station> by Delegates.observable(emptyList()) { property, oldValue, newValue ->
        pcs.firePropertyChange(property.name, oldValue, newValue)
    }

    // Méthode pour mettre à jour les stations
    fun updateStations(newStations: List<Station>) {
        stations = newStations
    }

    // Ajouter un observer
    fun addObserver(l: PropertyChangeListener) {
        pcs.addPropertyChangeListener(l)
    }
}

