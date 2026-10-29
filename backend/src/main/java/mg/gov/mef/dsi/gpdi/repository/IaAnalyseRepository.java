package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.PorteeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IaAnalyseRepository extends JpaRepository<IaAnalyse, Long> {

    List<IaAnalyse> findByProjetIdAndTypeAnalyseOrderByDateCreationAsc(Long idProjet, TypeAnalyseIa typeAnalyse);

    List<IaAnalyse> findByDemandeurIdAndPorteeOrderByDateCreationAsc(Long idDemandeur, PorteeAnalyseIa portee);
}
