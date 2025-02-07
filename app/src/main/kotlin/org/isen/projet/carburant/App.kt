/*
 * This source file was generated by the Gradle 'init' task
 */
package org.isen.projet.carburant
import javafx.application.Application
import javafx.application.Platform
import org.isen.projet.carburant.ctrl.CarburantController
import org.isen.projet.carburant.data.impl.SourceJson
import org.isen.projet.carburant.data.impl.SourceXml
import org.isen.projet.carburant.model.CarburantModel
//import org.isen.projet.carburant.view.impl.CarburantMapView
import org.isen.projet.carburant.view.impl.CarburantTestView

fun main() {
    val model = CarburantModel()
    val controller = CarburantController(model)
    val view = CarburantTestView(controller)
    //val mapView = CarburantMapView(controller)

    controller.displayAll()
}