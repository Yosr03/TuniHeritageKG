import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;

import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.rdf.model.InfModel;

public class Main {

    public static void main(String[] args) {

        // Créer un modèle RDF
        Model model = ModelFactory.createDefaultModel();

        // Charger notre ontologie RDFS
        RDFDataMgr.read(model, "ontology/ontology.ttl", Lang.TURTLE);
        // Charger les données du patrimoine tunisien
        RDFDataMgr.read(model, "data/heritage.ttl", Lang.TURTLE);
        RDFDataMgr.read(model, "data/sites_archeologiques.ttl", Lang.TURTLE);
        RDFDataMgr.read(model, "data/monuments.ttl", Lang.TURTLE);
        RDFDataMgr.read(model, "data/musees.ttl", Lang.TURTLE);
        RDFDataMgr.read(model, "data/patrimoine_immateriel.ttl", Lang.TURTLE);
        RDFDataMgr.read(model, "data/sites_regions.ttl", Lang.TURTLE);
        RDFDataMgr.read(model, "data/sites_periodes.ttl", Lang.TURTLE);

        RDFDataMgr.read(model, "data/sites_cultures.ttl", Lang.TURTLE);


        String requeteCultures = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

    SELECT DISTINCT ?nomSite ?nomCulture
    WHERE {
        ?site th:appartientAUneCulture ?culture ;
              rdfs:label ?nomSite .

        ?culture rdfs:label ?nomCulture .
    }
    ORDER BY ?nomSite ?nomCulture
    """;

        System.out.println(
                "\n=== SITES ET CULTURES HISTORIQUES ==="
        );

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requeteCultures, model)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect()
            );
        }


        String requetePeriodes = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

    SELECT DISTINCT ?nomSite ?nomPeriode
    WHERE {
        ?site th:aPeriodeHistorique ?periode ;
              rdfs:label ?nomSite .

        ?periode rdfs:label ?nomPeriode .
    }
    ORDER BY ?nomSite ?nomPeriode
    """;

        System.out.println(
                "\n=== SITES ET PERIODES HISTORIQUES ==="
        );

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requetePeriodes, model)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect()
            );
        }

        String requeteSitesRegions = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

    SELECT DISTINCT ?nomSite ?nomLieu
    WHERE {
        ?site a th:SiteArcheologique ;
              rdfs:label ?nomSite ;
              th:estSitueDans ?lieu .

        ?lieu rdfs:label ?nomLieu .
    }
    ORDER BY ?nomSite
    LIMIT 20
    """;

        System.out.println("\n=== SITES ARCHEOLOGIQUES ET LIEUX ===");

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requeteSitesRegions, model)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect()
            );
        }

        String requeteTraditions = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>

    SELECT (COUNT(DISTINCT ?tradition) AS ?nombreTraditions)
    WHERE {
        ?tradition a th:Tradition .
    }
    """;

        System.out.println("\n=== NOMBRE DE TRADITIONS ===");

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requeteTraditions, model)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect()
            );
        }

        String requeteMusees = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>

    SELECT (COUNT(DISTINCT ?musee) AS ?nombreMusees)
    WHERE {
        ?musee a th:Musee .
    }
    """;

        System.out.println("\n=== NOMBRE DE MUSEES ===");

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requeteMusees, model)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect()
            );
        }



        String requeteMonuments = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

    SELECT ?monument ?nom
    WHERE {
        ?monument a th:Monument ;
                  rdfs:label ?nom .
    }
    LIMIT 10
    """;

        System.out.println("\n=== VERIFICATION DES MONUMENTS ===");

        try (QueryExecution execution =
                     QueryExecutionFactory.create(requeteMonuments, model)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect()
            );
        }

        // Afficher le nombre de triplets
        System.out.println("=== TuniHeritage KG ===");
        System.out.println("Nombre total de triplets RDF : " + model.size());

        System.out.println("\n=== Exemples de triplets RDF ===");

        model.listStatements()
                .toList()
                .stream()
                .limit(10)
                .forEach(statement -> {
                    System.out.println("Sujet     : " + statement.getSubject());
                    System.out.println("Prédicat  : " + statement.getPredicate());
                    System.out.println("Objet     : " + statement.getObject());
                    System.out.println("-----------------------------");
                });


        System.out.println("\n=== REQUETE SPARQL 1 : Sites archeologiques ===");

        String sparql = """
                PREFIX th: <http://www.tuniheritage.tn/ontology#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                
                SELECT ?site ?nom
                WHERE {
                    ?site a th:SiteArcheologique ;
                          rdfs:label ?nom .
                    FILTER(LANG(?nom) = "fr")
                }
                """;

        Query query = QueryFactory.create(sparql);

        try (QueryExecution execution =
                     QueryExecutionFactory.create(query, model)) {

            ResultSet results = execution.execSelect();
            ResultSetFormatter.out(System.out, results, query);
        }


        System.out.println("\n=== REQUETE SPARQL 2 : Sites et cultures historiques ===");

        String sparql2 = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

    SELECT ?nomSite ?nomCulture
    WHERE {
        ?site a th:SiteArcheologique ;
              rdfs:label ?nomSite ;
              th:appartientAUneCulture ?culture .

        ?culture rdfs:label ?nomCulture .

        FILTER(LANG(?nomSite) = "fr")
        FILTER(LANG(?nomCulture) = "fr")
    }
    ORDER BY ?nomSite
    """;

        Query query2 = QueryFactory.create(sparql2);

        try (QueryExecution execution2 =
                     QueryExecutionFactory.create(query2, model)) {

            ResultSet results2 = execution2.execSelect();
            ResultSetFormatter.out(System.out, results2, query2);
        }


        System.out.println("\n=== REQUETE SPARQL 3 : Sites et periodes historiques ===");

        String sparql3 = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

    SELECT ?nomSite ?nomPeriode
    WHERE {
        ?site a th:SiteArcheologique ;
              rdfs:label ?nomSite ;
              th:aPeriodeHistorique ?periode .

        ?periode rdfs:label ?nomPeriode .

        FILTER(LANG(?nomSite) = "fr")
        FILTER(LANG(?nomPeriode) = "fr")
    }
    ORDER BY ?nomSite
    """;

        Query query3 = QueryFactory.create(sparql3);

        try (QueryExecution execution3 =
                     QueryExecutionFactory.create(query3, model)) {

            ResultSet results3 = execution3.execSelect();
            ResultSetFormatter.out(System.out, results3, query3);
        }

        System.out.println("\n=== REQUETE SPARQL 4 : Sites et localisations ===");

        String sparql4 = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

    SELECT ?nomSite ?nomLieu
    WHERE {
        ?site a th:SiteArcheologique ;
              rdfs:label ?nomSite ;
              th:estSitueDans ?lieu .

        ?lieu rdfs:label ?nomLieu .

        FILTER(LANG(?nomSite) = "fr")
        FILTER(LANG(?nomLieu) = "fr")
    }
    ORDER BY ?nomSite
    """;

        Query query4 = QueryFactory.create(sparql4);

        try (QueryExecution execution4 =
                     QueryExecutionFactory.create(query4, model)) {

            ResultSet results4 = execution4.execSelect();
            ResultSetFormatter.out(System.out, results4, query4);
        }

        System.out.println("\n=== REQUETE SPARQL 5 : Nombre de sites par culture ===");

        String sparql5 = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

    SELECT ?nomCulture (COUNT(DISTINCT ?site) AS ?nombreSites)
    WHERE {
        ?site a th:SiteArcheologique ;
              th:appartientAUneCulture ?culture .

        ?culture rdfs:label ?nomCulture .

        FILTER(LANG(?nomCulture) = "fr")
    }
    GROUP BY ?nomCulture
    ORDER BY DESC(?nombreSites)
    """;

        Query query5 = QueryFactory.create(sparql5);

        try (QueryExecution execution5 =
                     QueryExecutionFactory.create(query5, model)) {

            ResultSet results5 = execution5.execSelect();
            ResultSetFormatter.out(System.out, results5, query5);
        }


        System.out.println("\n=== INFERENCE 1 : Hierarchie des classes ===");

        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        InfModel infModel = ModelFactory.createInfModel(reasoner, model);

        String sparqlInference1 = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX wd: <http://www.wikidata.org/entity/>

    SELECT DISTINCT ?type
    WHERE {
        wd:Q32378 a ?type .
        FILTER(?type IN (
            th:SiteArcheologique,
            th:PatrimoineMateriel,
            th:PatrimoineCulturel
        ))
    }
    ORDER BY ?type
    """;

        Query queryInference1 = QueryFactory.create(sparqlInference1);

// Sans inférence
        System.out.println("\n--- SANS INFERENCE ---");

        try (QueryExecution execution =
                     QueryExecutionFactory.create(queryInference1, model)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect(),
                    queryInference1
            );
        }

// Avec inférence
        System.out.println("\n--- AVEC INFERENCE RDFS ---");

        try (QueryExecution execution =
                     QueryExecutionFactory.create(queryInference1, infModel)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect(),
                    queryInference1
            );
        }

        System.out.println("\n=== INFERENCE 2 : Hierarchie des proprietes ===");

        String sparqlInference2 = """
    PREFIX th: <http://www.tuniheritage.tn/ontology#>
    PREFIX wd: <http://www.wikidata.org/entity/>

    SELECT ?lieu
    WHERE {
        wd:Q32378 th:estLocaliseDans ?lieu .
    }
    """;

        Query queryInference2 = QueryFactory.create(sparqlInference2);

// Sans inférence
        System.out.println("\n--- SANS INFERENCE ---");

        try (QueryExecution execution =
                     QueryExecutionFactory.create(queryInference2, model)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect(),
                    queryInference2
            );
        }

// Avec inférence
        System.out.println("\n--- AVEC INFERENCE RDFS ---");

        try (QueryExecution execution =
                     QueryExecutionFactory.create(queryInference2, infModel)) {

            ResultSetFormatter.out(
                    System.out,
                    execution.execSelect(),
                    queryInference2
            );
        }

    }
}
