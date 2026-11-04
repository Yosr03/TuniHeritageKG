
package tn.tuniheritage;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.Reader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImportPatrimoineImmateriel {

    public static void main(String[] args) throws Exception {

        String TH = "http://www.tuniheritage.tn/ontology#";
        String WD = "http://www.wikidata.org/entity/";

        Model model = ModelFactory.createDefaultModel();

        Resource classeTradition =
                model.createResource(TH + "Tradition");

        Property aNom = model.createProperty(TH + "aNom");

        Property aIdentifiant =
                model.createProperty(TH + "aIdentifiantWikidata");

        Property aTypeWikidata =
                model.createProperty(TH + "aTypeWikidata");

        int lignesImportees = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (Reader reader = Files.newBufferedReader(
                Path.of("data/wikidata_patrimoine_immateriel.csv"),
                StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {

                if (record.size() < 4) {
                    continue;
                }

                String uri = record.get(0).trim();
                String nom = record.get(1).trim();
                String uriType = record.get(2).trim();
                String nomType = record.get(3).trim();

                if (!uri.startsWith(WD)) {
                    continue;
                }

                Resource patrimoine = model.createResource(uri);

                // Declarer la tradition
                patrimoine.addProperty(
                        RDF.type, classeTradition);

                // Ajouter le nom
                if (!nom.isEmpty()) {
                    patrimoine.addProperty(
                            RDFS.label, nom, "fr");

                    patrimoine.addProperty(
                            aNom, nom, "fr");
                }

                // Ajouter l'identifiant Wikidata
                String identifiant =
                        uri.substring(uri.lastIndexOf('/') + 1);

                patrimoine.addProperty(
                        aIdentifiant, identifiant);

                // Conserver le type Wikidata d'origine
                if (uriType.startsWith(WD)) {

                    Resource type = model.createResource(uriType);

                    patrimoine.addProperty(
                            aTypeWikidata, type);

                    if (!nomType.isEmpty()) {
                        type.addProperty(
                                RDFS.label, nomType, "fr");
                    }
                }

                lignesImportees++;
            }
        }

        // Prefixes RDF
        model.setNsPrefix("th", TH);
        model.setNsPrefix("wd", WD);
        model.setNsPrefix("rdf", RDF.getURI());
        model.setNsPrefix("rdfs", RDFS.getURI());

        // Exportation au format Turtle
        try (OutputStream output = Files.newOutputStream(
                Path.of("data/patrimoine_immateriel.ttl"))) {

            RDFDataMgr.write(output, model, Lang.TURTLE);
        }

        System.out.println(
                "=== IMPORTATION DU PATRIMOINE IMMATERIEL ===");

        System.out.println(
                "Lignes importees : " + lignesImportees);

        System.out.println(
                "Triplets RDF generes : " + model.size());

        System.out.println(
                "Fichier cree : data/patrimoine_immateriel.ttl");
    }
}
