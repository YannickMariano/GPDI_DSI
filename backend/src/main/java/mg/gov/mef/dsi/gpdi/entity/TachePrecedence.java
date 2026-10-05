package mg.gov.mef.dsi.gpdi.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

@Entity
@Table(name = "tache_precedences")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Getter
@Setter
@NoArgsConstructor
public class TachePrecedence {

    @EmbeddedId
    private TachePrecedenceId id;

    @ManyToOne
    @MapsId("idTacheSource")
    @JoinColumn(name = "id_tache_source")
    private Tache tacheSource;

    @ManyToOne
    @MapsId("idTacheCible")
    @JoinColumn(name = "id_tache_cible")
    private Tache tacheCible;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "type_lien")
    private TypeLien typeLien;
}
