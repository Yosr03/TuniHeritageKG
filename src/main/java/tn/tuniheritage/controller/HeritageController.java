
package tn.tuniheritage.controller;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;

import tn.tuniheritage.service.HeritageService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
public class HeritageController {

    private final HeritageService heritageService;

    public HeritageController(HeritageService heritageService) {
        this.heritageService = heritageService;
    }

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of(
                "projet", "TuniHeritageKG",
                "message", "API REST fonctionne !"
        );
    }

    @GetMapping("/sites")
    public List<Map<String, String>> getSites() {
        return heritageService.getSitesArcheologiques();
    }

    @GetMapping("/monuments")
    public List<Map<String, String>> getMonuments() {
        return heritageService.getMonuments();
    }

    @GetMapping("/musees")
    public List<Map<String, String>> getMusees() {
        return heritageService.getMusees();
    }


    @GetMapping("/traditions")
    public List<Map<String, String>> getTraditions() {
        return heritageService.getTraditions();
    }


    @GetMapping("/sites/region")
    public List<Map<String, String>> getSitesParRegion(
            @RequestParam(required = false) String nom) {

        if (nom == null || nom.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le paramètre 'nom' est obligatoire."
            );
        }

        return heritageService.getSitesParRegion(nom);
    }

    @GetMapping("/debug/regions")
    public List<Map<String, String>> verifierRegions() {
        return heritageService.verifierRegions();
    }


    @GetMapping("/statistiques")
    public Map<String, Long> getStatistiques() {
        return heritageService.getStatistiques();
    }

    @GetMapping("/carte")
    public List<Map<String, Object>> getSitesPourCarte() {
        return heritageService.getSitesPourCarte();
    }

}
