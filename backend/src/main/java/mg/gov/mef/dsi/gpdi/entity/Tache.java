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
import java.time.LocalDateTime;

@Entity
@Table(name = "taches")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Getter
@Setter
@NoArgsConstructor
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tache")
    private Long id;

    @Column(unique = true)
    private String code;

    private String titre;

    private String description;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "statut_tache")
    private StatutTache statut = StatutTache.A_FAIRE;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "priorite")
    private Priorite priorite;

    /**
     * Renseignes dans une autre section, pas geres par ce CRUD pour l'instant.
     */
    private LocalDate dateDebut;

    private LocalDate dateFinPrevue;

    private LocalDate dateFinReelle;

    private Double tempsEstime;

    private Double tempsPasse;

    /** Horodatage de démarrage du chronomètre de suivi du temps ; absent si la tâche n'est pas en cours de saisie. */
    private LocalDateTime demarreeLe;

    /** Vrai si la tâche a été terminée après la date de fin de son sprint (la clôture reste permise, elle est seulement signalée). */
    private boolean depasseSprint;

    /** Tâche terminée retirée du Kanban et consultable dans les archives du projet. */
    private boolean archivee;

    private LocalDateTime dateArchivage;

    @ManyToOne
    @JoinColumn(name = "id_sprint")
    private Sprint sprint;

    @ManyToOne
    @JoinColumn(name = "id_projet")
    private Projet projet;
}
