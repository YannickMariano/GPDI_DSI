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
import java.time.LocalDate;

/**
 * Instantane quotidien du travail restant d'un sprint, capture par {@link mg.gov.mef.dsi.gpdi.service.impl.BurndownPlanificateur}.
 * Necessaire car {@link Tache#getTempsPasse()} n'est qu'un cumul, sans historique date : sans cet
 * instantane, il serait impossible de reconstituer la courbe de burndown reelle jour par jour.
 */
@Entity
@Table(name = "burndown_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class BurndownSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_snapshot")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_sprint")
    private Sprint sprint;

    private LocalDate date;

    private Double heuresRestantes;
}
