package mg.gov.mef.dsi.gpdi.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "livrables")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Getter
@Setter
@NoArgsConstructor
public class Livrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_livrable")
    private Long id;

    private String description;

    private String type;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "statut_livrable")
    private StatutLivrable statut = StatutLivrable.ATTENDU;

    private LocalDate dateLivraison;

    private LocalDate dateValidation;

    private LocalDate dateRejet;

    private String motifRejet;

    @ManyToOne
    @JoinColumn(name = "id_jalon")
    private Jalon jalon;
}
