package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.CreateFermetureGlobaleRequest;
import be.ephec.padel.backend.dto.response.FermetureGlobaleDto;
import be.ephec.padel.backend.mapper.FermetureGlobaleMapper;
import be.ephec.padel.backend.model.entities.FermetureGlobale;
import be.ephec.padel.backend.service.FermetureGlobaleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fermetures-globales")
public class FermetureGlobaleController {

    private final FermetureGlobaleService service;

    public FermetureGlobaleController(FermetureGlobaleService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<FermetureGlobaleDto>> list() {
        return ResponseEntity.ok(
                service.lister().stream().map(FermetureGlobaleMapper::toDto).toList()
        );
    }

    @PostMapping
    public ResponseEntity<FermetureGlobaleDto> create(@Valid @RequestBody CreateFermetureGlobaleRequest req) {
        FermetureGlobale created = service.creer(req);
        URI location = URI.create("/api/v1/fermetures-globales/" + created.getId());
        return ResponseEntity.created(location).body(FermetureGlobaleMapper.toDto(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}