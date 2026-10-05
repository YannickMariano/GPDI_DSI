package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.response.NotificationResponse;
import mg.gov.mef.dsi.gpdi.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
