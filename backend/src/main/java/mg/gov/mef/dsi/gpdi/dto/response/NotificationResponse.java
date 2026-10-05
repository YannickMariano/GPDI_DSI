package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.TypeNotification;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class NotificationResponse {

    private Long id;

    private TypeNotification type;

    private String titre;

    private String meta;

    private String lien;

    private Boolean lu;

    private LocalDateTime dateCreation;
}
