package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.JournalConnexionResponse;

import java.util.List;

public interface JournalConnexionService {

    List<JournalConnexionResponse> rechercherEvenementsRecents(int max);
}
