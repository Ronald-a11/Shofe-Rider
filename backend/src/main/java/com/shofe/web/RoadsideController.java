package com.shofe.web;

import com.shofe.dto.MiscDtos;
import com.shofe.service.RoadsideService;
import com.shofe.service.SafetyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoadsideController {

    private final RoadsideService roadsideService;
    private final SafetyService safetyService;

    @PostMapping("/roadside")
    public MiscDtos.RoadsideResponse create(@Valid @RequestBody MiscDtos.RoadsideCreate request) {
        return roadsideService.create(request);
    }

    @GetMapping("/roadside/mine")
    public List<MiscDtos.RoadsideResponse> mine() {
        return roadsideService.mine();
    }

    @GetMapping("/roadside/open")
    public List<MiscDtos.RoadsideResponse> open() {
        return roadsideService.open();
    }

    @PostMapping("/roadside/{id}/accept")
    public MiscDtos.RoadsideResponse accept(@PathVariable UUID id) {
        return roadsideService.accept(id);
    }

    @PostMapping("/roadside/{id}/status")
    public MiscDtos.RoadsideResponse status(@PathVariable UUID id, @Valid @RequestBody MiscDtos.RoadsideStatusRequest request) {
        return roadsideService.status(id, request.status());
    }

    @PostMapping("/safety/sos")
    public MiscDtos.SosResponse sos(@Valid @RequestBody MiscDtos.SosRequest request) {
        return safetyService.raise(request);
    }
}
