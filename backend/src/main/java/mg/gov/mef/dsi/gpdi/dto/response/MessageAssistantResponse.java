package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageAssistantResponse {

    private Long id;

    private String question;

    private String reponse;

    /** Sur quoi s'appuie la réponse, en quelques mots. Non conservé en base : absent quand le message vient de l'historique. */
    private String fondement;

    private LocalDateTime dateCreation;
}
