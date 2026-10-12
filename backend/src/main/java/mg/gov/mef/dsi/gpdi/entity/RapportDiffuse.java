package mg.gov.mef.dsi.gpdi.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Rapport diffusé par le chef de projet aux membres concernés du projet (et au Directeur DSI) :
 * photo figée (JSON) du rapport tel qu'il était au moment de la diffusion, consultable en lecture seule.
 */
@Entity
@Table(name = "rapports_diffuses")
@Getter
@Setter
@NoArgsConstructor
public class RapportDiffuse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rapport_diffuse")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_projet")
    private Projet projet;

    @ManyToOne
    @JoinColumn(name = "id_auteur")
    private Utilisateur auteur;

    /** Identifiant du modèle de rapport (ex. « hebdo », « sprint », « temps »). */
    @Column(length = 50, nullable = false)
    private String modele;

    @Column(length = 200, nullable = false)
    private String nom;

    @Column(length = 300)
    private String titre;

    @Column(columnDefinition = "text", nullable = false)
    private String contenu;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateDiffusion;
}
