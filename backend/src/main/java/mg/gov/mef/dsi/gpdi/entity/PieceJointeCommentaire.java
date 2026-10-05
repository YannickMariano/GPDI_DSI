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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Piece jointe d'un commentaire de tache (une seule par commentaire). Entite separee de Commentaire
 * pour que la lecture de la liste des commentaires (findByTacheIdOrderByDateCreationAsc) ne charge
 * jamais le contenu binaire : celui-ci n'est lu que par le telechargement dedie.
 */
@Entity
@Table(name = "pieces_jointes_commentaires")
@Getter
@Setter
@NoArgsConstructor
public class PieceJointeCommentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piece_jointe")
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_commentaire", unique = true)
    private Commentaire commentaire;

    private String nomFichier;

    private String typeContenu;

    private Long taille;

    // Pas de @Lob : sur PostgreSQL, Hibernate le mapperait au type "oid" (reference numerique vers un
    // objet large externe, pg_largeobject) au lieu du type "bytea" declare dans le schema (cf.
    // init-lot11-pieces-jointes-commentaires.sql) : un byte[] simple est deja mappe en bytea sans @Lob.
    @Column(columnDefinition = "bytea")
    private byte[] contenu;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;
}
