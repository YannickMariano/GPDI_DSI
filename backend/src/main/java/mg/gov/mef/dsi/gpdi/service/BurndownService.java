package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.BurndownPointResponse;

import java.util.List;

public interface BurndownService {

    List<BurndownPointResponse> obtenir(Long idProjet, Long idSprint);
}
