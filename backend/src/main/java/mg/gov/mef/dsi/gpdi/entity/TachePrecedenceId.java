package mg.gov.mef.dsi.gpdi.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class TachePrecedenceId implements Serializable {

    @Column(name = "id_tache_source")
    private Long idTacheSource;

    @Column(name = "id_tache_cible")
    private Long idTacheCible;

    public TachePrecedenceId(Long idTacheSource, Long idTacheCible) {
        this.idTacheSource = idTacheSource;
        this.idTacheCible = idTacheCible;
    }
}
