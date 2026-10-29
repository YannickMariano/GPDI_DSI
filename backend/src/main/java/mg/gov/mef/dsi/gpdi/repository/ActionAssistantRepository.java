package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.ActionAssistant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionAssistantRepository extends JpaRepository<ActionAssistant, Long> {

    /** La liste ne doit pas etre vide (un IN () vide n'est pas valide en SQL) : l'appelant doit le verifier. */
    List<ActionAssistant> findByAnalyseIdIn(List<Long> idsAnalyses);
}
