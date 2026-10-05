package mg.gov.mef.dsi.gpdi.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "partage_clients")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Getter
@Setter
@NoArgsConstructor
public class PartageClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partage")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur client;

    @ManyToOne
    @JoinColumn(name = "id_projet")
    private Projet projet;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "statut_partage")
    private StatutPartage statut = StatutPartage.ACTIF;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    private LocalDate dateEcheance;
}
