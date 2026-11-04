
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

public class ImportSitesCultures {

    public static void main(String[] args) throws Exception {

        String TH = "http://www.tuniheritage.tn/ontology#";
        String WD = "http://www.wikidata.org/entity/";

        Model model = ModelFactory.createDefaultModel();

        Resource classeCulture =
                model.createResource(TH + "CultureHistorique");

        Property appartientAUneCulture =
                model.createProperty(TH + "appartientAUneCulture");

        int lignesImportees = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (Reader reader = Files.newBufferedReader(
                Path.of("data/wikidata_sites_cultures.csv"),
                StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {

                if (record.size() < 4) {
                    continue;
                }

                String uriSite = record.get(0).trim();
                String nomSite = record.get(1).trim();
                String uriCulture = record.get(2).trim();
                String nomCulture = record.get(3).trim();

                if (!uriSite.startsWith(WD)
                        || !uriCulture.startsWith(WD)) {
                    continue;
                }

                Resource site = model.createResource(uriSite);
                Resource culture = model.createResource(uriCulture);

                // Declarer la culture historique
                culture.addProperty(RDF.type, classeCulture);

                // Ajouter le nom de la culture
                if (!nomCulture.isEmpty()) {
                    culture.addProperty(
                            RDFS.label, nomCulture, "fr");
                }

                // Conserver le nom du site
                if (!nomSite.isEmpty()) {
                    site.addProperty(
                            RDFS.label, nomSite, "fr");
                }

                // Creer la relation entre le site et la culture
                site.addProperty(
                        appartientAUneCulture, culture);

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
                Path.of("data/sites_cultures.ttl"))) {

            RDFDataMgr.write(output, model, Lang.TURTLE);
        }

        System.out.println(
                "=== IMPORTATION SITES - CULTURES ===");

        System.out.println(
                "Lignes importees : " + lignesImportees);

        System.out.println(
                "Triplets RDF generes : " + model.size());

        System.out.println(
                "Fichier cree : data/sites_cultures.ttl");
    }
}
