package org.isen.projet.carburant.ctrl

import org.isen.projet.carburant.model.CarburantModel
import org.isen.projet.carburant.view.ICarburantView

class CarburantController(private val model: CarburantModel) {
    private val views = mutableListOf<ICarburantView>()

    fun registerView(view: ICarburantView) {
        model.addObserver(view)
        views.add(view)
    }

    fun displayAll(){
        views.forEach {
            it.display()
        }
    }

    fun updateModel(){
        model.update(5)
    }
}