package org.isen.projet.carburant.ctrl

import org.isen.projet.carburant.data.impl.SourceXml
import org.isen.projet.carburant.model.CarburantModel
import org.isen.projet.carburant.view.ICarburantView
import kotlinx.coroutines.*

class CarburantController(private val model: CarburantModel) {
    private val views = mutableListOf<ICarburantView>()
    private val dataSource = SourceXml()

    // Enregistrer une vue
    fun registerView(view: ICarburantView) {
        model.addObserver(view)
        views.add(view)
    }

    // Afficher toutes les vues
    fun displayAll() {
        views.forEach { it.display() }
    }

    // Mettre à jour le modèle avec les vraies données XML
    fun updateModel() {
        println("📡 Récupération des stations-service en cours...")

        GlobalScope.launch(Dispatchers.IO) {
            val xmlData = dataSource.fetchData()
            val stations = dataSource.parseData(xmlData)

            withContext(Dispatchers.Main) {
                model.updateStations(stations)
                println("✅ Modèle mis à jour avec ${stations.size} stations-service !")
            }
        }
    }
}

