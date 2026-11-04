
package tn.tuniheritage;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import java.io.Reader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImportSitesArcheologiques {

    public static void main(String[] args) throws Exception {

        String TH = "http://www.tuniheritage.tn/ontology#";
        String WD = "http://www.wikidata.org/entity/";

        Model model = ModelFactory.createDefaultModel();

        Resource siteArcheologique =
                model.createResource(TH + "SiteArcheologique");

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
                Path.of("data/wikidata_sites_archeologiques.csv"),
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

                Resource site = model.createResource(uri);

                site.addProperty(RDF.type, siteArcheologique);

                if (!nom.isEmpty()) {
                    site.addProperty(RDFS.label, nom, "fr");
                    site.addProperty(aNom, nom, "fr");
                }

                String identifiant =
                        uri.substring(uri.lastIndexOf('/') + 1);

                site.addProperty(aIdentifiant, identifiant);

                // Format Wikidata : Point(longitude latitude)
                if (coord.startsWith("Point(")
                        && coord.endsWith(")")) {

                    String valeurs =
                            coord.substring(6, coord.length() - 1);

                    String[] xy = valeurs.trim().split("\\s+");

                    if (xy.length == 2) {
                        try {
                            site.addLiteral(longitude,
                                    Double.parseDouble(xy[0]));
                            site.addLiteral(latitude,
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

        model.setNsPrefix("th", TH);
        model.setNsPrefix("wd", WD);
        model.setNsPrefix("rdf", RDF.getURI());
        model.setNsPrefix("rdfs", RDFS.getURI());

        try (OutputStream output = Files.newOutputStream(
                Path.of("data/sites_archeologiques.ttl"))) {

            RDFDataMgr.write(output, model, Lang.TURTLE);
        }

        System.out.println("=== IMPORTATION DES SITES ===");
        System.out.println("Lignes importees : " + lignesImportees);
        System.out.println("Triplets RDF generes : " + model.size());
        System.out.println(
                "Fichier cree : data/sites_archeologiques.ttl");
    }
}
