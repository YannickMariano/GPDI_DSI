package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IaRecommandationRepository extends JpaRepository<IaRecommandation, Long> {

    List<IaRecommandation> findByRisqueIdOrderByIdAsc(Long idRisque);

    List<IaRecommandation> findByRisqueIdAndStatut(Long idRisque, StatutRecommandation statut);

    /** La liste ne doit pas etre vide (un IN () vide n'est pas valide en SQL) : l'appelant doit le verifier. */
    List<IaRecommandation> findByRisqueIdInAndStatutOrderByIdAsc(List<Long> idsRisques, StatutRecommandation statut);

    /** Toutes les solutions (proposees, appliquees ou rejetees) : historique complet affiche en lecture seule. */
    List<IaRecommandation> findByRisqueIdInOrderByIdAsc(List<Long> idsRisques);

    /** Recommandations de pilotage de projet (risque absent), pour un projet et un statut donnes. */
    List<IaRecommandation> findByAnalyseProjetIdAndRisqueIsNullAndStatutOrderByIdDesc(Long idProjet, StatutRecommandation statut);
}
