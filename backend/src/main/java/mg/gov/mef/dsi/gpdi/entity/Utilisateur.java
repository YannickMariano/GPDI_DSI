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
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String matricule;

    private String nom;

    private String prenom;

    private String email;

    private String telephone;

    private String motDePasse;

    private String specialite;

    private Boolean actif = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "id_role")
    private Role role;
}
