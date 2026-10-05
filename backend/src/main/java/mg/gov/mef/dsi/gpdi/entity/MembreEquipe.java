package mg.gov.mef.dsi.gpdi.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "membre_equipes")
@Getter
@Setter
@NoArgsConstructor
public class MembreEquipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_membre")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_equipe")
    private Equipe equipe;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    private Boolean actif = true;
}
