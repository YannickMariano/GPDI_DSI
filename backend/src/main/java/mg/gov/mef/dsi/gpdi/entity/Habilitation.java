package mg.gov.mef.dsi.gpdi.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Une case de la matrice d'habilitations : le niveau d'acces d'un role metier (valeur de {@code Role.nom},
 * ex. "chef_projet") pour une fonction donnee (ex. "Creation des taches"). Couple (fonction, roleNom) unique
 * (contrainte appliquee en base reelle, voir db/init-lot10-habilitations.sql — schema gere manuellement).
 * Table de configuration peu volumineuse : niveau stocke en VARCHAR (EnumType.STRING), pas de type ENUM
 * natif Postgres necessaire ici contrairement aux entites metier a fort volume.
 */
@Entity
@Table(name = "habilitations")
@Getter
@Setter
@NoArgsConstructor
public class Habilitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String module;

    private String fonction;

    private String roleNom;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private NiveauAcces niveau;
}
