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

/** Point d'information publié par le chef de projet à destination des comptes clients (Direction métier) partagés sur ce projet. */
@Entity
@Table(name = "points_information")
@Getter
@Setter
@NoArgsConstructor
public class PointInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_point_information")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_projet")
    private Projet projet;

    @ManyToOne
    @JoinColumn(name = "id_auteur")
    private Utilisateur auteur;

    @Column(length = 300)
    private String titre;

    @Column(columnDefinition = "text")
    private String corps;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;
}
