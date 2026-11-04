
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

public class ImportSitesRegions {

    public static void main(String[] args) throws Exception {

        String TH = "http://www.tuniheritage.tn/ontology#";
        String WD = "http://www.wikidata.org/entity/";

        Model model = ModelFactory.createDefaultModel();

        Resource classeLieu =
                model.createResource(TH + "Lieu");

        Property estSitueDans =
                model.createProperty(TH + "estSitueDans");

        Property aNom =
                model.createProperty(TH + "aNom");

        int lignesImportees = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (Reader reader = Files.newBufferedReader(
                Path.of("data/wikidata_sites_regions.csv"),
                StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {

                if (record.size() < 4) {
                    continue;
                }

                String uriSite = record.get(0).trim();
                String nomSite = record.get(1).trim();
                String uriLieu = record.get(2).trim();
                String nomLieu = record.get(3).trim();

                if (!uriSite.startsWith(WD)
                        || !uriLieu.startsWith(WD)) {
                    continue;
                }

                Resource site = model.createResource(uriSite);
                Resource lieu = model.createResource(uriLieu);

                // Declarer le lieu
                lieu.addProperty(RDF.type, classeLieu);

                // Ajouter le nom du lieu
                if (!nomLieu.isEmpty()) {
                    lieu.addProperty(RDFS.label, nomLieu, "fr");
                    lieu.addProperty(aNom, nomLieu, "fr");
                }

                // Conserver le nom du site
                if (!nomSite.isEmpty()) {
                    site.addProperty(RDFS.label, nomSite, "fr");
                }

                // Creer la relation geographique
                site.addProperty(estSitueDans, lieu);

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
                Path.of("data/sites_regions.ttl"))) {

            RDFDataMgr.write(output, model, Lang.TURTLE);
        }

        System.out.println(
                "=== IMPORTATION SITES - REGIONS ===");

        System.out.println(
                "Lignes importees : " + lignesImportees);

        System.out.println(
                "Triplets RDF generes : " + model.size());

        System.out.println(
                "Fichier cree : data/sites_regions.ttl");
    }
}
