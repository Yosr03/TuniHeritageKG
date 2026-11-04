
/* ==========================================
   TUNIHERITAGE KG — JAVASCRIPT COMPLET
   Images en premier + pagination
   ========================================== */

const API_BASE = "/api";

const endpoints = {
    sites: "/sites",
    monuments: "/monuments",
    musees: "/musees",
    traditions: "/traditions"
};

const titres = {
    sites: "Site archéologique",
    monuments: "Monument",
    musees: "Musée",
    traditions: "Tradition"
};

const ELEMENTS_PAR_PAGE = 9;

let categorieActuelle = "sites";
let donneesActuelles = [];
let requeteActuelle = 0;
let pageActuelle = 1;
let carteLeaflet = null;

const heritageList = document.getElementById("heritage-list");
const message = document.getElementById("message");
const searchInput = document.getElementById("search-input");
const regionInput = document.getElementById("region-input");
const regionBtn = document.getElementById("region-btn");
const pagination = document.getElementById("pagination");
const prevPage = document.getElementById("prev-page");
const nextPage = document.getElementById("next-page");
const pageInfo = document.getElementById("page-info");

/* ==========================================
   MESSAGES ET API
   ========================================== */

function afficherMessage(texte, erreur = false) {
    message.textContent = texte;
    message.classList.toggle("error", erreur);
}

async function appelerAPI(endpoint) {
    const reponse = await fetch(API_BASE + endpoint);

    if (!reponse.ok) {
        throw new Error("Erreur HTTP " + reponse.status);
    }

    return reponse.json();
}

/* ==========================================
   STATISTIQUES
   ========================================== */

async function chargerStatistiques() {
    try {
        const stats = await appelerAPI("/statistiques");

        document.getElementById("stat-sites").textContent =
            stats.sitesArcheologiques ?? "—";

        document.getElementById("stat-monuments").textContent =
            stats.monuments ?? "—";

        document.getElementById("stat-musees").textContent =
            stats.musees ?? "—";

        document.getElementById("stat-traditions").textContent =
            stats.traditions ?? "—";

    } catch (erreur) {
        console.error("Erreur statistiques :", erreur);
    }
}

/* ==========================================
   IMAGES WIKIDATA / COMMONS
   ========================================== */

// Cache des URLs Wikidata
const cacheImages = new Map();

// Cache des résultats de chargement des photos
const cacheVerificationImages = new Map();

function extraireIdentifiantWikidata(uri) {
    if (!uri) return null;

    const resultat = String(uri).match(
        /(?:^|\/)(Q\d+)\/?(?:[?#].*)?$/
    );

    return resultat ? resultat[1] : null;
}

async function recupererImageWikidata(uri) {
    const id = extraireIdentifiantWikidata(uri);

    if (!id) return null;

    if (cacheImages.has(id)) {
        return cacheImages.get(id);
    }

    const promesse = (async () => {
        try {
            const paramsWikidata = new URLSearchParams({
                action: "wbgetentities",
                ids: id,
                props: "claims",
                format: "json",
                origin: "*"
            });

            const reponseWikidata = await fetch(
                "https://www.wikidata.org/w/api.php?" +
                paramsWikidata.toString()
            );

            if (!reponseWikidata.ok) {
                throw new Error(
                    "Erreur Wikidata " + reponseWikidata.status
                );
            }

            const donnees = await reponseWikidata.json();

            const declarations =
                donnees.entities?.[id]?.claims?.P18 || [];

            const declaration = declarations.find(item =>
                item.rank !== "deprecated" &&
                typeof item.mainsnak?.datavalue?.value === "string"
            );

            if (!declaration) return null;

            const nomFichier =
                declaration.mainsnak.datavalue.value;

            const paramsCommons = new URLSearchParams({
                action: "query",
                format: "json",
                prop: "imageinfo",
                iiprop: "url",
                iiurlwidth: "600",
                titles: "File:" + nomFichier,
                origin: "*"
            });

            const reponseCommons = await fetch(
                "https://commons.wikimedia.org/w/api.php?" +
                paramsCommons.toString()
            );

            if (!reponseCommons.ok) {
                throw new Error(
                    "Erreur Commons " + reponseCommons.status
                );
            }

            const donneesCommons =
                await reponseCommons.json();

            const pages = Object.values(
                donneesCommons.query?.pages || {}
            );

            const info = pages[0]?.imageinfo?.[0];

            return info?.thumburl || info?.url || null;

        } catch (erreur) {
            console.warn(
                "Image Wikidata indisponible :", id,
                erreur
            );
            return null;
        }
    })();

    cacheImages.set(id, promesse);

    return promesse;
}

/*
 * Vérifier qu'une photographie se charge réellement.
 * Aucun élément HTML visible n'est créé ici.
 */

function verifierChargementImage(url) {
    if (!url) return Promise.resolve(false);

    if (cacheVerificationImages.has(url)) {
        return cacheVerificationImages.get(url);
    }

    const promesse = new Promise(resolve => {
        const image = new Image();

        image.onload = () => {
            resolve(image.naturalWidth > 0);
        };

        image.onerror = () => {
            resolve(false);
        };

        image.src = url;
    });

    cacheVerificationImages.set(url, promesse);

    return promesse;
}

/* ==========================================
   TRI : PHOTOS EN PREMIER
   ========================================== */

async function trierDonneesParImages(donnees) {
    const resultats = await Promise.all(
        donnees.map(async (element, index) => {
            const imageUrl =
                await recupererImageWikidata(element.uri);

            const imageChargee =
                await verifierChargementImage(imageUrl);

            return {
                ...element,
                imageUrl: imageChargee ? imageUrl : null,
                possedeImage: imageChargee,
                ordreInitial: index
            };
        })
    );

    resultats.sort((a, b) => {
        if (a.possedeImage !== b.possedeImage) {
            return a.possedeImage ? -1 : 1;
        }

        return a.ordreInitial - b.ordreInitial;
    });

    return resultats;
}

/* ==========================================
   AFFICHER UNE PHOTOGRAPHIE
   ========================================== */

function chargerImageCarte(element, carte) {
    if (!element.imageUrl) return;

    const image = new Image();

    image.className = "heritage-image";
    image.alt = "Photographie de " +
        (element.nom || "patrimoine tunisien");

    image.decoding = "async";

    image.onload = () => {
        if (!carte.isConnected) return;

        if (carte.querySelector(
            ".heritage-image-container"
        )) {
            return;
        }

        const conteneur =
            document.createElement("div");

        conteneur.className =
            "heritage-image-container";

        conteneur.appendChild(image);

        carte.prepend(conteneur);
        carte.classList.add("has-image");
    };

    image.onerror = () => {
        console.warn(
            "Impossible d'afficher la photo :",
            element.nom
        );
    };

    image.src = element.imageUrl;

    if (image.complete && image.naturalWidth > 0) {
        image.onload();
    }
}

/* ==========================================
   CRÉATION DES FICHES
   ========================================== */

function creerCarte(element) {
    const carte = document.createElement("article");
    carte.className = "heritage-card";

    const tag = document.createElement("span");
    tag.className = "heritage-tag";
    tag.textContent =
        titres[categorieActuelle] || "Patrimoine culturel";

    const titre = document.createElement("h3");
    titre.textContent = element.nom || "Sans nom";

    carte.appendChild(tag);
    carte.appendChild(titre);

    if (element.region) {
        const region = document.createElement("p");
        region.textContent =
            "Localisation : " + element.region;
        carte.appendChild(region);
    }

    if (element.uri) {
        try {
            const url = new URL(element.uri);

            if (
                url.protocol === "https:" ||
                url.protocol === "http:"
            ) {
                const lien = document.createElement("a");

                lien.href = url.href;
                lien.target = "_blank";
                lien.rel = "noopener noreferrer";
                lien.textContent =
                    "Voir la ressource Wikidata";

                carte.appendChild(lien);
            }
        } catch (erreur) {
            console.warn("URI invalide :", element.uri);
        }
    }

    chargerImageCarte(element, carte);

    return carte;
}

/* ==========================================
   RECHERCHE ET PAGINATION
   ========================================== */

function obtenirResultatsFiltres() {
    const recherche = searchInput.value
        .trim()
        .toLocaleLowerCase("fr");

    return donneesActuelles.filter(element => {
        const nom = String(element.nom || "")
            .toLocaleLowerCase("fr");

        const region = String(element.region || "")
            .toLocaleLowerCase("fr");

        return nom.includes(recherche) ||
            region.includes(recherche);
    });
}

function afficherDonnees() {
    const resultats = obtenirResultatsFiltres();

    const totalPages = Math.ceil(
        resultats.length / ELEMENTS_PAR_PAGE
    );

    pageActuelle = Math.max(
        1,
        Math.min(pageActuelle, totalPages || 1)
    );

    const debut =
        (pageActuelle - 1) * ELEMENTS_PAR_PAGE;

    const fin = debut + ELEMENTS_PAR_PAGE;

    const resultatsPage =
        resultats.slice(debut, fin);

    heritageList.replaceChildren();

    if (resultatsPage.length === 0) {
        const vide = document.createElement("div");
        vide.className = "empty-state";
        vide.textContent = "Aucun résultat trouvé.";

        heritageList.appendChild(vide);
    } else {
        const fragment =
            document.createDocumentFragment();

        resultatsPage.forEach(element => {
            fragment.appendChild(creerCarte(element));
        });

        heritageList.appendChild(fragment);
    }

    if (pagination && prevPage && nextPage && pageInfo) {
        pageInfo.textContent =
            "Page " + pageActuelle +
            " / " + (totalPages || 1);

        prevPage.disabled = pageActuelle === 1;

        nextPage.disabled =
            pageActuelle >= totalPages ||
            totalPages === 0;

        pagination.style.display =
            resultats.length > ELEMENTS_PAR_PAGE
                ? "flex"
                : "none";
    }

    if (resultats.length > 0) {
        afficherMessage(
            resultats.length +
            " résultat(s) trouvé(s). Affichage de " +
            (debut + 1) + " à " +
            Math.min(fin, resultats.length) + "."
        );
    } else {
        afficherMessage("Aucun résultat trouvé.");
    }
}

/* ==========================================
   CHARGEMENT DES CATÉGORIES
   ========================================== */

async function chargerCategorie(categorie) {
    if (!endpoints[categorie]) return;

    const identifiant = ++requeteActuelle;

    categorieActuelle = categorie;
    donneesActuelles = [];
    pageActuelle = 1;

    searchInput.value = "";
    heritageList.replaceChildren();

    pagination.style.display = "none";

    document.querySelectorAll(".filter-btn")
        .forEach(bouton => {
            bouton.classList.toggle(
                "active",
                bouton.dataset.type === categorie
            );
        });

    afficherMessage("Chargement des données...");

    try {
        const donnees = await appelerAPI(
            endpoints[categorie]
        );

        if (identifiant !== requeteActuelle) return;

        afficherMessage(
            "Recherche des photographies Wikidata..."
        );

        const liste = Array.isArray(donnees)
            ? donnees
            : [];

        const donneesTriees =
            await trierDonneesParImages(liste);

        if (identifiant !== requeteActuelle) return;

        donneesActuelles = donneesTriees;

        afficherDonnees();

    } catch (erreur) {
        if (identifiant !== requeteActuelle) return;

        afficherMessage(
            "Impossible de charger les données : " +
            erreur.message,
            true
        );
    }
}

/* ==========================================
   RECHERCHE PAR RÉGION
   ========================================== */

async function rechercherParRegion() {
    const region = regionInput.value.trim();

    if (!region) {
        afficherMessage(
            "Veuillez saisir une région, par exemple Tunis.",
            true
        );
        regionInput.focus();
        return;
    }

    const identifiant = ++requeteActuelle;

    categorieActuelle = "sites";
    donneesActuelles = [];
    pageActuelle = 1;

    searchInput.value = "";
    heritageList.replaceChildren();
    pagination.style.display = "none";

    document.querySelectorAll(".filter-btn")
        .forEach(bouton => {
            bouton.classList.toggle(
                "active",
                bouton.dataset.type === "sites"
            );
        });

    afficherMessage(
        "Recherche des sites dans la région..."
    );

    try {
        const donnees = await appelerAPI(
            "/sites/region?nom=" +
            encodeURIComponent(region)
        );

        if (identifiant !== requeteActuelle) return;

        afficherMessage(
            "Recherche des photographies Wikidata..."
        );

        const liste = Array.isArray(donnees)
            ? donnees
            : [];

        const donneesTriees =
            await trierDonneesParImages(liste);

        if (identifiant !== requeteActuelle) return;

        donneesActuelles = donneesTriees;

        afficherDonnees();

    } catch (erreur) {
        if (identifiant !== requeteActuelle) return;

        afficherMessage(
            "Erreur de recherche : " +
            erreur.message,
            true
        );
    }
}

/* ==========================================
   CARTE INTERACTIVE LEAFLET
   ========================================== */

async function initialiserCarte() {
    const conteneur = document.getElementById("map");

    if (!conteneur) return;

    if (typeof L === "undefined") {
        console.error("Leaflet n'est pas chargé.");
        return;
    }

    if (carteLeaflet) return;

    carteLeaflet = L.map("map", {
        zoomControl: true,
        scrollWheelZoom: false
    }).setView([34.0, 9.5], 6);

    L.tileLayer(
        "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
        {
            attribution:
                '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 19
        }
    ).addTo(carteLeaflet);

    const groupeMarqueurs =
        L.featureGroup().addTo(carteLeaflet);

    try {
        const sites = await appelerAPI("/carte");

        if (!Array.isArray(sites)) {
            throw new Error(
                "Format de données incorrect"
            );
        }

        let nombreMarqueurs = 0;

        sites.forEach(site => {
            if (
                site.latitude == null ||
                site.longitude == null
            ) {
                return;
            }

            const latitude = Number(site.latitude);
            const longitude = Number(site.longitude);

            if (
                !Number.isFinite(latitude) ||
                !Number.isFinite(longitude) ||
                latitude < -90 ||
                latitude > 90 ||
                longitude < -180 ||
                longitude > 180
            ) {
                return;
            }

            const marqueur = L.marker([
                latitude,
                longitude
            ]);

            const popup = document.createElement("div");

            const titre = document.createElement("strong");
            titre.textContent =
                site.nom || "Site sans nom";

            const categorie = document.createElement("p");
            categorie.textContent =
                site.categorie || "Site archéologique";

            popup.appendChild(titre);
            popup.appendChild(categorie);

            if (site.uri) {
                try {
                    const url = new URL(site.uri);

                    if (
                        url.protocol === "https:" ||
                        url.protocol === "http:"
                    ) {
                        const lien =
                            document.createElement("a");

                        lien.href = url.href;
                        lien.target = "_blank";
                        lien.rel = "noopener noreferrer";
                        lien.textContent =
                            "Découvrir sur Wikidata";

                        popup.appendChild(lien);
                    }
                } catch (erreur) {
                    // URI invalide
                }
            }

            marqueur.bindPopup(popup);
            marqueur.addTo(groupeMarqueurs);

            nombreMarqueurs++;
        });

        if (nombreMarqueurs > 0) {
            carteLeaflet.fitBounds(
                groupeMarqueurs.getBounds(),
                {
                    padding: [35, 35],
                    maxZoom: 8
                }
            );
        }

        console.log(
            "Carte chargée : " +
            nombreMarqueurs +
            " sites géolocalisés."
        );

    } catch (erreur) {
        console.error(
            "Erreur de chargement de la carte :",
            erreur
        );
    }

    requestAnimationFrame(() => {
        carteLeaflet.invalidateSize();
    });
}

/* ==========================================
   ÉVÉNEMENTS
   ========================================== */

document.querySelectorAll(".filter-btn")
    .forEach(bouton => {
        bouton.addEventListener("click", () => {
            regionInput.value = "";
            chargerCategorie(bouton.dataset.type);
        });
    });

searchInput.addEventListener("input", () => {
    pageActuelle = 1;
    afficherDonnees();
});

regionBtn.addEventListener(
    "click",
    rechercherParRegion
);

regionInput.addEventListener("keydown", evenement => {
    if (evenement.key === "Enter") {
        rechercherParRegion();
    }
});

prevPage.addEventListener("click", () => {
    if (pageActuelle > 1) {
        pageActuelle--;
        afficherDonnees();

        document.getElementById("explorer")
            .scrollIntoView({
                behavior: "smooth"
            });
    }
});

nextPage.addEventListener("click", () => {
    const totalPages = Math.ceil(
        obtenirResultatsFiltres().length /
        ELEMENTS_PAR_PAGE
    );

    if (pageActuelle < totalPages) {
        pageActuelle++;
        afficherDonnees();

        document.getElementById("explorer")
            .scrollIntoView({
                behavior: "smooth"
            });
    }
});

/* ==========================================
   INITIALISATION
   ========================================== */

chargerStatistiques();
chargerCategorie("sites");
initialiserCarte();
