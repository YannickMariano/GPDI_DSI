package mg.gov.mef.dsi.gpdi.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

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

/**
 * Action proposee par l'assistant IA en reponse a une demande de l'utilisateur. Rien n'est execute tant que le
 * demandeur ne l'a pas confirmee ; les parametres ne contiennent que des identifiants deja verifies au moment
 * de la proposition (projets dont il est responsable, membres de l'equipe, sprints du projet...).
 */
@Entity
@Table(name = "ia_actions_assistant")
@TypeDef(name = "json", typeClass = JsonType.class)
@Getter
@Setter
@NoArgsConstructor
public class ActionAssistant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_action")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_analyse")
    private IaAnalyse analyse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_demandeur")
    private Utilisateur demandeur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_action", length = 40)
    private TypeActionAssistant type;

    @Column(length = 300)
    private String libelle;

    /** { "cible": { idProjet, idTache... }, "valeurs": { champs a appliquer }, "details": [ lignes affichees ] } */
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private JsonNode parametres;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatutActionAssistant statut = StatutActionAssistant.PROPOSEE;

    @Column(length = 500)
    private String resultat;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateTraitement;
}
