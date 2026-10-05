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
import java.time.LocalDateTime;

/** Solution proposee par l'assistant IA, a valider (ou rejeter) par un utilisateur. Generique, comme IaAnalyse. */
@Entity
@Table(name = "ia_recommandations")
@TypeDefs({
        @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class),
        @TypeDef(name = "json", typeClass = JsonType.class)
})
@Getter
@Setter
@NoArgsConstructor
public class IaRecommandation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recommandation")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_analyse")
    private IaAnalyse analyse;

    /** Absent quand la recommandation ne concerne pas un risque (autres modules IA). */
    @ManyToOne
    @JoinColumn(name = "id_risque")
    private Risque risque;

    @Column(length = 300)
    private String libelle;

    @Column(columnDefinition = "text")
    private String justification;

    /** Action a executer si la recommandation est appliquee (type + parametres + gain estime). */
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private JsonNode actionProposee;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "statut_recommandation")
    private StatutRecommandation statut = StatutRecommandation.PROPOSEE;

    @ManyToOne
    @JoinColumn(name = "id_validateur")
    private Utilisateur validateur;

    @Column(columnDefinition = "text")
    private String motifRejet;

    private LocalDateTime dateValidation;

    /** Ce qui a reellement ete cree ou modifie lors de l'application (ex. identifiants de taches). */
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private JsonNode resultatApplication;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;
}
