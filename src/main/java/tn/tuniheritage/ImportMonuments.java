
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

public class ImportMonuments {

    public static void main(String[] args) throws Exception {

        String TH = "http://www.tuniheritage.tn/ontology#";
        String WD = "http://www.wikidata.org/entity/";

        Model model = ModelFactory.createDefaultModel();

        Resource classeMonument =
                model.createResource(TH + "Monument");

        Property aNom = model.createProperty(TH + "aNom");
        Property aIdentifiant =
                model.createProperty(TH + "aIdentifiantWikidata");
        Property latitude = model.createProperty(TH + "aLatitude");
        Property longitude = model.createProperty(TH + "aLongitude");

        int lignesImportees = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (Reader reader = Files.newBufferedReader(
                Path.of("data/wikidata_monuments_tunisie.csv"),
                StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {

                if (record.size() < 5) {
                    continue;
                }

                String uri = record.get(0).trim();
                String nom = record.get(1).trim();
                String coord = record.get(4).trim();

                if (!uri.startsWith(WD)) {
                    continue;
                }

                Resource monument = model.createResource(uri);

                monument.addProperty(RDF.type, classeMonument);

                if (!nom.isEmpty()) {
                    monument.addProperty(RDFS.label, nom, "fr");
                    monument.addProperty(aNom, nom, "fr");
                }

                String identifiant =
                        uri.substring(uri.lastIndexOf('/') + 1);

                monument.addProperty(aIdentifiant, identifiant);

                // Format Wikidata : Point(longitude latitude)
                // Les coordonnees sont facultatives.
                if (coord.startsWith("Point(")
                        && coord.endsWith(")")) {

                    String valeurs =
                            coord.substring(6, coord.length() - 1);

                    String[] xy = valeurs.trim().split("\\s+");

                    if (xy.length == 2) {
                        try {
                            monument.addLiteral(longitude,
                                    Double.parseDouble(xy[0]));

                            monument.addLiteral(latitude,
                                    Double.parseDouble(xy[1]));

                        } catch (NumberFormatException e) {
                            System.out.println(
                                    "Coordonnees invalides : " + uri);
                        }
                    }
                }

                lignesImportees++;
            }
        }

        // Prefixes RDF pour un Turtle lisible
        model.setNsPrefix("th", TH);
        model.setNsPrefix("wd", WD);
        model.setNsPrefix("rdf", RDF.getURI());
        model.setNsPrefix("rdfs", RDFS.getURI());

        // Exporter les triplets dans le fichier Turtle
        try (OutputStream output = Files.newOutputStream(
                Path.of("data/monuments.ttl"))) {

            RDFDataMgr.write(output, model, Lang.TURTLE);
        }

        System.out.println("=== IMPORTATION DES MONUMENTS ===");
        System.out.println("Lignes importees : " + lignesImportees);
        System.out.println("Triplets RDF generes : " + model.size());
        System.out.println(
                "Fichier cree : data/monuments.ttl");
    }
}
