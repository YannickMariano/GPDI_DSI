package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.HabilitationLigneRequest;
import mg.gov.mef.dsi.gpdi.dto.response.HabilitationResponse;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.HabilitationInvalideException;
import mg.gov.mef.dsi.gpdi.exception.HabilitationRefuseeException;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.HabilitationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HabilitationServiceImpl implements HabilitationService {

    private static final Logger log = LoggerFactory.getLogger(HabilitationServiceImpl.class);

    private final HabilitationRepository habilitationRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurCourantService utilisateurCourantService;

    @Override
    @Transactional(readOnly = true)
    public List<HabilitationResponse> rechercherMatrice() {
        Map<String, HabilitationResponse> parFonction = new LinkedHashMap<>();
        for (Habilitation habilitation : habilitationRepository.findAllByOrderByIdAsc()) {
            HabilitationResponse reponse = parFonction.computeIfAbsent(habilitation.getFonction(), fonction -> {
                HabilitationResponse nouvelle = new HabilitationResponse();
                nouvelle.setFonction(fonction);
                nouvelle.setModule(habilitation.getModule());
                nouvelle.setAcces(new LinkedHashMap<>());
                return nouvelle;
            });
            reponse.getAcces().put(habilitation.getRoleNom(), versChaine(habilitation.getNiveau()));
        }
        return new java.util.ArrayList<>(parFonction.values());
    }

    @Override
    @Transactional
    public void mettreAJourMatrice(List<HabilitationLigneRequest> lignes) {
        for (HabilitationLigneRequest ligne : lignes) {
            if (!roleRepository.existsByNom(ligne.getRoleNom())) {
                throw new HabilitationInvalideException("Role inconnu : " + ligne.getRoleNom());
            }
            NiveauAcces niveau = depuisChaine(ligne.getNiveau());
            Habilitation habilitation = habilitationRepository
                    .findByFonctionAndRoleNom(ligne.getFonction(), ligne.getRoleNom())
                    .orElseThrow(() -> new HabilitationInvalideException(
                            "Fonction inconnue dans la matrice : " + ligne.getFonction()));
            habilitation.setNiveau(niveau);
            habilitationRepository.save(habilitation);
        }
        log.info("Matrice d'habilitations mise a jour ({} case(s))", lignes.size());
    }

    @Override
    @Transactional(readOnly = true)
    public void verifierEcriture(String fonction) {
        Utilisateur utilisateur = utilisateurCourantService.obtenirUtilisateurCourant();
        String roleNom = utilisateur.getRole() != null ? utilisateur.getRole().getNom() : null;
        boolean autorise = roleNom != null && habilitationRepository.findByFonctionAndRoleNom(fonction, roleNom)
                .map(habilitation -> habilitation.getNiveau() == NiveauAcces.ECRITURE)
                .orElse(false);
        if (!autorise) {
            throw new HabilitationRefuseeException(fonction);
        }
    }

    private String versChaine(NiveauAcces niveau) {
        switch (niveau) {
            case ECRITURE: return "ecriture";
            case LECTURE: return "lecture";
            default: return "aucun";
        }
    }

    private NiveauAcces depuisChaine(String niveau) {
        if ("ecriture".equals(niveau)) return NiveauAcces.ECRITURE;
        if ("lecture".equals(niveau)) return NiveauAcces.LECTURE;
        if ("aucun".equals(niveau)) return NiveauAcces.AUCUN;
        throw new HabilitationInvalideException("Niveau d'acces invalide : " + niveau);
    }
}
