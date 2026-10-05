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
@Table(name = "sprints")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Getter
@Setter
@NoArgsConstructor
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sprint")
    private Long id;

    private String nom;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private String objectif;

    /** Capacite de l'equipe sur ce sprint, en heures — sert au calcul de charge cote frontend. */
    private Integer capaciteHeures = 40;

    private Integer progression = 0;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "statut_sprint")
    private StatutSprint statut = StatutSprint.PLANIFIE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "id_projet")
    private Projet projet;
}
