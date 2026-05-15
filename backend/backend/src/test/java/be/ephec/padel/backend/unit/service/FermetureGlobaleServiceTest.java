package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.request.CreateFermetureGlobaleRequest;
import be.ephec.padel.backend.model.entities.FermetureGlobale;
import be.ephec.padel.backend.repository.FermetureGlobaleRepository;
import be.ephec.padel.backend.service.AnnulationMatchService;
import be.ephec.padel.backend.service.FermetureGlobaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FermetureGlobaleServiceTest {

    private FermetureGlobaleRepository repo;
    private AnnulationMatchService annulationMatchService;
    private FermetureGlobaleService service;

    @BeforeEach
    void setUp() {
        repo = mock(FermetureGlobaleRepository.class);
        annulationMatchService = mock(AnnulationMatchService.class);
        service = new FermetureGlobaleService(repo, annulationMatchService);
    }

    @Test
    void creer_annuleLesMatchsFutursPlanifiesDeLaDateTousSites() {
        CreateFermetureGlobaleRequest request = new CreateFermetureGlobaleRequest();
        request.setDate(LocalDate.of(2030, 1, 2));
        request.setMotif("Jour ferie");

        when(repo.save(any(FermetureGlobale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FermetureGlobale result = service.creer(request);

        assertThat(result.getDate()).isEqualTo(LocalDate.of(2030, 1, 2));
        verify(annulationMatchService).annulerMatchsFutursPlanifiesTousSites(
                LocalDate.of(2030, 1, 2).atStartOfDay(),
                LocalDate.of(2030, 1, 3).atStartOfDay()
        );
    }
}
