package org.isen.projet.carburant.model

import java.io.File
import java.io.PrintWriter

object LeafletService {
    private const val MAP_FILE_PATH = "app/src/main/resources/map.html"

    fun generateMapHtml(stations: List<Station>, itineraire: List<Pair<Double, Double>>? = null) {
        if (stations.isEmpty()) return

        val firstStation = stations.first()
        val centerLatitude = firstStation.latitude
        val centerLongitude = firstStation.longitude

        val mapHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset='utf-8' />
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <title>Carte des stations-service</title>
                <link rel='stylesheet' href='https://unpkg.com/leaflet@1.7.1/dist/leaflet.css' />
                <script src='https://unpkg.com/leaflet@1.7.1/dist/leaflet.js'></script>
                <style>
                    body { margin: 0; padding: 0; }
                    #map { width: 100vw; height: 100vh; }
                </style>
            </head>
            <body>
                <div id='map'></div>
                <script>
                    var map = L.map('map').setView([${centerLatitude}, ${centerLongitude}], 12);

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; OpenStreetMap contributors'
                    }).addTo(map);

                    ${stations.joinToString("\n") { station ->
            """
                        var marker = L.marker([${station.latitude}, ${station.longitude}])
                            .addTo(map)
                            .bindPopup("<b>${station.ville}</b><br>${station.adresse}<br>Code Postal: ${station.codePostal}<br>${formatPrix(station.prixCarburants)}");

                        marker.on('click', function (e) {
                            this.openPopup();
                        });
            """.trimIndent()
        }}

        ${if (itineraire != null && itineraire.isNotEmpty()) {
            """
                    var routeCoords = [
                        ${itineraire.joinToString(",\n") { "[${it.first}, ${it.second}]" }}
                    ];

                    var routeLine = L.polyline(routeCoords, {color: 'blue', weight: 4}).addTo(map);
                    map.fitBounds(routeLine.getBounds());
            """.trimIndent()
        } else ""}
                </script>
            </body>
            </html>
        """.trimIndent()

        File(MAP_FILE_PATH).apply {
            parentFile.mkdirs()
            PrintWriter(this).use { it.write(mapHtml) }
        }
    }

    private fun formatPrix(prixCarburants: Map<String, String>): String {
        return prixCarburants.entries.joinToString("<br>") { (carburant, prix) -> "$carburant : $prix €" }
    }
}
