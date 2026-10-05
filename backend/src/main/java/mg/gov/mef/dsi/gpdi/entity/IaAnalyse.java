package mg.gov.mef.dsi.gpdi.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trace d'un appel a l'assistant IA (quel projet, qui l'a demande, ce qui a ete envoye au modele et
 * ce qu'il a repondu). Generique : reutilisee par tous les modules IA, pas seulement les risques.
 */
@Entity
@Table(name = "ia_analyses")
@TypeDefs({
        @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class),
        @TypeDef(name = "json", typeClass = JsonType.class)
})
@Getter
@Setter
@NoArgsConstructor
public class IaAnalyse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_analyse")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_projet")
    private Projet projet;

    /** Absent quand l'analyse est declenchee par le systeme (tache planifiee). */
    @ManyToOne
    @JoinColumn(name = "id_demandeur")
    private Utilisateur demandeur;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "type_analyse_ia")
    private TypeAnalyseIa typeAnalyse;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "portee_analyse_ia")
    private PorteeAnalyseIa portee = PorteeAnalyseIa.PROJET;

    /** Absente pour les analyses automatiques. */
    @Column(columnDefinition = "text")
    private String question;

    @Column(columnDefinition = "text")
    private String reponse;

    /** Donnees envoyees au modele, conservees pour pouvoir expliquer et rejouer une analyse. */
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private JsonNode contexte;

    @Column(precision = 4, scale = 3)
    private BigDecimal indiceConfiance;

    private String modele;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;
}
