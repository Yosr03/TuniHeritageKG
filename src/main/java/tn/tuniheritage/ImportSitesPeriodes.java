
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

public class ImportSitesPeriodes {

    public static void main(String[] args) throws Exception {

        String TH = "http://www.tuniheritage.tn/ontology#";
        String WD = "http://www.wikidata.org/entity/";

        Model model = ModelFactory.createDefaultModel();

        Resource classePeriode =
                model.createResource(TH + "PeriodeHistorique");

        Property aPeriodeHistorique =
                model.createProperty(TH + "aPeriodeHistorique");

        int lignesImportees = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (Reader reader = Files.newBufferedReader(
                Path.of("data/wikidata_sites_periodes.csv"),
                StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {

                if (record.size() < 4) {
                    continue;
                }

                String uriSite = record.get(0).trim();
                String nomSite = record.get(1).trim();
                String uriPeriode = record.get(2).trim();
                String nomPeriode = record.get(3).trim();

                if (!uriSite.startsWith(WD)
                        || !uriPeriode.startsWith(WD)) {
                    continue;
                }

                Resource site = model.createResource(uriSite);
                Resource periode = model.createResource(uriPeriode);

                // Declarer la periode historique
                periode.addProperty(
                        RDF.type, classePeriode);

                // Ajouter le nom de la periode
                if (!nomPeriode.isEmpty()) {
                    periode.addProperty(
                            RDFS.label, nomPeriode, "fr");
                }

                // Conserver le nom du site
                if (!nomSite.isEmpty()) {
                    site.addProperty(
                            RDFS.label, nomSite, "fr");
                }

                // Creer la relation site -> periode
                site.addProperty(
                        aPeriodeHistorique, periode);

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
                Path.of("data/sites_periodes.ttl"))) {

            RDFDataMgr.write(output, model, Lang.TURTLE);
        }

        System.out.println(
                "=== IMPORTATION SITES - PERIODES ===");

        System.out.println(
                "Lignes importees : " + lignesImportees);

        System.out.println(
                "Triplets RDF generes : " + model.size());

        System.out.println(
                "Fichier cree : data/sites_periodes.ttl");
    }
}
