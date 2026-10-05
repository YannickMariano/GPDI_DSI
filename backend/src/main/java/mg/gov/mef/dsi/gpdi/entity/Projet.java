package mg.gov.mef.dsi.gpdi.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "projets")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Getter
@Setter
@NoArgsConstructor
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_projet")
    private Long id;

    private String titre;

    /** Genere a la creation (ex. GPDI-2026-014), jamais fourni par le client. */
    @Column(unique = true)
    private String codeReference;

    private String description;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private LocalDate dateCloture;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "statut_projet")
    private StatutProjet statut = StatutProjet.EN_PREPARATION;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "priorite")
    private Priorite priorite;

    private Integer progression = 0;

    private Boolean archive = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "id_responsable")
    private Utilisateur responsable;

    @ManyToOne
    @JoinColumn(name = "id_equipe")
    private Equipe equipe;
}
