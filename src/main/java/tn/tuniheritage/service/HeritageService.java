
package tn.tuniheritage.service;

import jakarta.annotation.PostConstruct;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import org.apache.jena.riot.RDFDataMgr;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import org.springframework.stereotype.Service;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HeritageService {

    private Model model;

    // 1. Charger les fichiers RDF au démarrage
    @PostConstruct
    public void chargerDonnees() {

        model = ModelFactory.createDefaultModel();

        List<String> fichiers = List.of(
                "ontology/ontology.ttl",
                "data/heritage.ttl",
                "data/sites_archeologiques.ttl",
                "data/monuments.ttl",
                "data/musees.ttl",
                "data/patrimoine_immateriel.ttl",
                "data/sites_regions.ttl",
                "data/sites_periodes.ttl",
                "data/sites_cultures.ttl"
        );

        for (String fichier : fichiers) {
            RDFDataMgr.read(model, fichier);
            System.out.println("Fichier chargé : " + fichier);
        }

        System.out.println(
                "Nombre total de triplets RDF : " + model.size()
        );
    }

    // 2. Retourner le nombre de triplets RDF
    public long getNombreTriplets() {
        return model.size();
    }


    public List<Map<String, String>> getSitesArcheologiques() {

        String requete = """
        PREFIX th: <http://www.tuniheritage.tn/ontology#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

        SELECT DISTINCT ?site ?nom
        WHERE {
            ?site a th:SiteArcheologique .

            OPTIONAL {
                ?site rdfs:label ?nom .
                FILTER(
                    LANG(?nom) = "fr" ||
                    LANG(?nom) = ""
                )
            }
        }
        LIMIT 100
        """;

        List<Map<String, String>> sites = new ArrayList<>();

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requete, model)) {

            ResultSet resultats = execution.execSelect();

            while (resultats.hasNext()) {

                QuerySolution ligne = resultats.nextSolution();

                Map<String, String> site = new LinkedHashMap<>();

                site.put(
                        "uri",
                        ligne.getResource("site").getURI()
                );

                site.put(
                        "nom",
                        ligne.contains("nom")
                                ? ligne.getLiteral("nom").getString()
                                : "Nom inconnu"
                );

                sites.add(site);
            }
        }

        return sites;
    }

    public List<Map<String, String>> getMonuments() {

        String requete = """
        PREFIX th: <http://www.tuniheritage.tn/ontology#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

        SELECT DISTINCT ?monument ?nom
        WHERE {
            ?monument a th:Monument .

            OPTIONAL {
                ?monument rdfs:label ?nom .
                FILTER(
                    LANG(?nom) = "fr" ||
                    LANG(?nom) = ""
                )
            }
        }
        LIMIT 100
        """;

        List<Map<String, String>> monuments = new ArrayList<>();

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requete, model)) {

            ResultSet resultats = execution.execSelect();

            while (resultats.hasNext()) {
                QuerySolution ligne = resultats.nextSolution();

                Map<String, String> monument = new LinkedHashMap<>();

                monument.put(
                        "uri",
                        ligne.getResource("monument").getURI()
                );

                monument.put(
                        "nom",
                        ligne.contains("nom")
                                ? ligne.getLiteral("nom").getString()
                                : "Nom inconnu"
                );

                monuments.add(monument);
            }
        }

        return monuments;
    }

    public List<Map<String, String>> getMusees() {

        String requete = """
        PREFIX th: <http://www.tuniheritage.tn/ontology#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

        SELECT DISTINCT ?musee ?nom
        WHERE {
            ?musee a th:Musee .

            OPTIONAL {
                ?musee rdfs:label ?nom .
                FILTER(
                    LANG(?nom) = "fr" ||
                    LANG(?nom) = ""
                )
            }
        }
        LIMIT 100
        """;

        List<Map<String, String>> musees = new ArrayList<>();

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requete, model)) {

            ResultSet resultats = execution.execSelect();

            while (resultats.hasNext()) {

                QuerySolution ligne = resultats.nextSolution();

                Map<String, String> musee = new LinkedHashMap<>();

                musee.put(
                        "uri",
                        ligne.getResource("musee").getURI()
                );

                musee.put(
                        "nom",
                        ligne.contains("nom")
                                ? ligne.getLiteral("nom").getString()
                                : "Nom inconnu"
                );

                musees.add(musee);
            }
        }

        return musees;
    }


    public List<Map<String, String>> getTraditions() {

        String requete = """
        PREFIX th: <http://www.tuniheritage.tn/ontology#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

        SELECT DISTINCT ?tradition ?nom
        WHERE {
            ?tradition a th:Tradition .

            OPTIONAL {
                ?tradition rdfs:label ?nom .
                FILTER(
                    LANG(?nom) = "fr" ||
                    LANG(?nom) = ""
                )
            }
        }
        LIMIT 100
        """;

        List<Map<String, String>> traditions = new ArrayList<>();

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requete, model)) {

            ResultSet resultats = execution.execSelect();

            while (resultats.hasNext()) {

                QuerySolution ligne = resultats.nextSolution();

                Map<String, String> tradition = new LinkedHashMap<>();

                tradition.put(
                        "uri",
                        ligne.getResource("tradition").getURI()
                );

                tradition.put(
                        "nom",
                        ligne.contains("nom")
                                ? ligne.getLiteral("nom").getString()
                                : "Nom inconnu"
                );

                traditions.add(tradition);
            }
        }

        return traditions;
    }


    public List<Map<String, String>> getSitesParRegion(String nomRegion) {

        String requete = """
        PREFIX th: <http://www.tuniheritage.tn/ontology#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

        SELECT DISTINCT ?site ?nom ?regionNom
        WHERE {
            ?site a th:SiteArcheologique .
            ?site th:estSitueDans ?region .

            ?region rdfs:label ?regionNom .

            FILTER(
                CONTAINS(
                    LCASE(STR(?regionNom)),
                    LCASE(STR(?recherche))
                )
            )

            OPTIONAL {
                ?site rdfs:label ?nom .
                FILTER(
                    LANG(?nom) = "fr" ||
                    LANG(?nom) = ""
                )
            }
        }
        LIMIT 100
        """;

        ParameterizedSparqlString pss =
                new ParameterizedSparqlString(requete);

        pss.setLiteral("recherche", nomRegion.trim());

        List<Map<String, String>> sites = new ArrayList<>();

        try (QueryExecution execution =
                     QueryExecutionFactory.create(pss.asQuery(), model)) {

            ResultSet resultats = execution.execSelect();

            while (resultats.hasNext()) {

                QuerySolution ligne = resultats.nextSolution();

                Map<String, String> site = new LinkedHashMap<>();

                site.put(
                        "uri",
                        ligne.getResource("site").getURI()
                );

                site.put(
                        "nom",
                        ligne.contains("nom")
                                ? ligne.getLiteral("nom").getString()
                                : "Nom inconnu"
                );

                site.put(
                        "region",
                        ligne.getLiteral("regionNom").getString()
                );

                sites.add(site);
            }
        }

        return sites;
    }


    public List<Map<String, String>> verifierRegions() {

        String requete = """
        PREFIX th: <http://www.tuniheritage.tn/ontology#>

        SELECT ?site ?relation ?region
        WHERE {
            ?site a th:SiteArcheologique .
            ?site ?relation ?region .

            FILTER(
                ?relation IN (
                    th:estLocaliseDans,
                    th:estSitueDans
                )
            )
        }
        LIMIT 10
        """;

        List<Map<String, String>> resultats = new ArrayList<>();

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requete, model)) {

            ResultSet lignes = execution.execSelect();

            while (lignes.hasNext()) {

                QuerySolution ligne = lignes.nextSolution();

                Map<String, String> resultat = new LinkedHashMap<>();

                resultat.put("site", ligne.get("site").toString());
                resultat.put("relation", ligne.get("relation").toString());
                resultat.put("region", ligne.get("region").toString());

                resultats.add(resultat);
            }
        }

        return resultats;
    }


    public Map<String, Long> getStatistiques() {

        String requete = """
        PREFIX th: <http://www.tuniheritage.tn/ontology#>

        SELECT ?type (COUNT(DISTINCT ?element) AS ?total)
        WHERE {
            VALUES ?type {
                th:SiteArcheologique
                th:Monument
                th:Musee
                th:Tradition
            }

            ?element a ?type .
        }
        GROUP BY ?type
        """;

        Map<String, Long> statistiques = new LinkedHashMap<>();

        statistiques.put("sitesArcheologiques", 0L);
        statistiques.put("monuments", 0L);
        statistiques.put("musees", 0L);
        statistiques.put("traditions", 0L);

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requete, model)) {

            ResultSet resultats = execution.execSelect();

            while (resultats.hasNext()) {

                QuerySolution ligne = resultats.nextSolution();

                String type = ligne.getResource("type").getLocalName();
                long total = ligne.getLiteral("total").getLong();

                switch (type) {
                    case "SiteArcheologique" ->
                            statistiques.put("sitesArcheologiques", total);
                    case "Monument" ->
                            statistiques.put("monuments", total);
                    case "Musee" ->
                            statistiques.put("musees", total);
                    case "Tradition" ->
                            statistiques.put("traditions", total);
                }
            }
        }

        statistiques.put("tripletsRDF", model.size());

        return statistiques;
    }

    // Récupérer les sites archéologiques avec leurs coordonnées
    public List<Map<String, Object>> getSitesPourCarte() {

        String requete = """
            PREFIX th: <http://www.tuniheritage.tn/ontology#>

            SELECT ?site (SAMPLE(?nomValeur) AS ?nom)
                         (SAMPLE(?latValeur) AS ?latitude)
                         (SAMPLE(?lonValeur) AS ?longitude)
            WHERE {
                ?site a th:SiteArcheologique ;
                      th:aNom ?nomValeur ;
                      th:aLatitude ?latValeur ;
                      th:aLongitude ?lonValeur .
            }
            GROUP BY ?site
            """;

        List<Map<String, Object>> sites = new ArrayList<>();

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requete, model)) {

            ResultSet resultats = execution.execSelect();

            while (resultats.hasNext()) {

                QuerySolution ligne = resultats.nextSolution();

                double latitude =
                        ligne.getLiteral("latitude").getDouble();

                double longitude =
                        ligne.getLiteral("longitude").getDouble();

                // Vérifier que les coordonnées sont valides
                if (latitude < -90 || latitude > 90 ||
                        longitude < -180 || longitude > 180) {
                    continue;
                }

                Map<String, Object> site = new LinkedHashMap<>();

                site.put("uri", ligne.getResource("site").getURI());
                site.put("nom", ligne.getLiteral("nom").getString());
                site.put("latitude", latitude);
                site.put("longitude", longitude);
                site.put("categorie", "Site archéologique");

                sites.add(site);
            }
        }

        return sites;
    }




}
