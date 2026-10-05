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
@Table(name = "sous_taches")
@Getter
@Setter
@NoArgsConstructor
public class SousTache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sous_tache")
    private Long id;

    private String titre;

    private Boolean fait = false;

    @ManyToOne
    @JoinColumn(name = "id_tache")
    private Tache tache;
}
