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

@Entity
@Table(name = "equipes")
@Getter
@Setter
@NoArgsConstructor
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipe")
    private Long id;

    private String nom;

    private String description;

    private Boolean active = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "id_chef_equipe")
    private Utilisateur chefEquipe;
}
