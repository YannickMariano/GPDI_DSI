package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.NotificationResponse;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface NotificationService {

    Page<NotificationResponse> rechercherPourUtilisateur(Long idUtilisateur, Pageable pageable);

    NotificationResponse marquerCommeLue(Long idUtilisateur, Long id);

    void marquerToutesCommeLues(Long idUtilisateur);

    /** Previent les assignes de la tache et le responsable du projet qu'elle vient de passer a BLOQUEE. */
    void notifierBlocage(Tache tache, Collection<Utilisateur> assignes);

    /** Previent le responsable du projet qu'un jalon vient de passer EN_RETARD. */
    void notifierJalonEnRetard(Jalon jalon);

    /** Previent le responsable du projet qu'un jalon vient d'etre ATTEINT. */
    void notifierJalonAtteint(Jalon jalon);

    /** Previent les assignes des taches non terminees dont l'echeance est dans 24 h au plus, ou depassee. */
    void notifierEcheancesProches();
}
