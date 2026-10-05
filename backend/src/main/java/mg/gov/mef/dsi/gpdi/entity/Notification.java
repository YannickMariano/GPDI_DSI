package mg.gov.mef.dsi.gpdi.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

/** Notification personnelle : chaque destinataire a la sienne, avec son propre etat lu / non lu. */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Utilisateur destinataire;

    @Enumerated(EnumType.STRING)
    private TypeNotification type;

    private String titre;

    /** Contexte affiche sous le titre (ex. titre du projet concerne). */
    private String meta;

    /** Route applicative vers l'element concerne. */
    private String lien;

    private Boolean lu = false;

    /**
     * Cle de deduplication pour les notifications generees par une tache planifiee (ex. ECHEANCE:12) :
     * un meme destinataire ne recoit jamais deux fois la meme cle. Absente pour les notifications d'evenement.
     */
    private String cle;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;
}
