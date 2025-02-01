package org.isen.projet.carburant.model

data class Station(
    val id: String,
    val latitude: String,
    val longitude: String,
    val codePostal: String,
    val ville: String,
    val adresse: String,
    val prixCarburants: Map<String, String> // Clé : Nom du carburant, Valeur : Prix
)
