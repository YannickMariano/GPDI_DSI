package mg.gov.mef.dsi.gpdi.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/** Risque d'un projet, detecte automatiquement par l'assistant IA (jamais saisi a la main). */
@Entity
@Table(name = "risques")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Getter
@Setter
@NoArgsConstructor
public class Risque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_risque")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_projet")
    private Projet projet;

    /** Analyse au cours de laquelle le risque a ete detecte. */
    @ManyToOne
    @JoinColumn(name = "id_analyse")
    private IaAnalyse analyse;

    /** Personne chargee de surveiller le risque. */
    @ManyToOne
    @JoinColumn(name = "id_porteur")
    private Utilisateur porteur;

    @Column(length = 300)
    private String libelle;

    /** Affichee comme "analyse" a l'ecran. */
    @Column(columnDefinition = "text")
    private String description;

    /** De 1 a 5. */
    private Integer probabilite;

    /** De 1 a 5. */
    private Integer impact;

    /** Toujours calcule par le backend a partir de probabilite x impact, jamais fourni par l'IA. */
    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "niveau_risque")
    private NiveauRisque niveau;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "statut_risque")
    private StatutRisque statut = StatutRisque.OUVERT;

    /** Affichee comme "parade" a l'ecran. */
    @Column(columnDefinition = "text")
    private String mesureMitigation;

    /** Date de detection. */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    /** Nom de colonne explicite : la convention de nommage Spring donnerait "date_miseajour". */
    @UpdateTimestamp
    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour;
}
