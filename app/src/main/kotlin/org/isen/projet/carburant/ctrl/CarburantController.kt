package org.isen.projet.carburant.ctrl

import kotlinx.coroutines.*
import org.isen.projet.carburant.data.impl.SourceJson
import org.isen.projet.carburant.model.CarburantModel
import org.isen.projet.carburant.model.Station
import org.isen.projet.carburant.view.ICarburantView
import java.text.Normalizer
import java.util.Locale

class CarburantController(private val model: CarburantModel) {
    private val views = mutableListOf<ICarburantView>()
    private val dataSource = SourceJson()

    fun registerView(view: ICarburantView) {
        model.addObserver(view)
        views.add(view)
    }

    fun displayAll() {
        views.forEach { it.display() }
    }

    fun updateModelForCity(city: String) {
        val normalizedCity = normalizeString(city)
        println("📡 Récupération des stations-service pour la ville : $normalizedCity...")

        GlobalScope.launch(Dispatchers.IO) {
            val jsonData = dataSource.fetchDataForCity(normalizedCity)
            val stations = dataSource.parseData(jsonData)

            withContext(Dispatchers.Default) { // ✅ Utilisation de Default pour éviter Main
                model.updateStations(stations)
                println("✅ Modèle mis à jour avec ${stations.size} stations pour $normalizedCity !")
            }
        }
    }

    // 🔥 Fonction pour normaliser une chaîne : supprime accents et met en minuscules
    private fun normalizeString(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")  // Supprime les accents
            .uppercase(Locale.getDefault())  // Met en minuscules
    }
}







