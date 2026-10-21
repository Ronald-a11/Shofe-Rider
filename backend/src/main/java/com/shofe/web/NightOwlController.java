package com.shofe.web;

import com.shofe.dto.MiscDtos;
import com.shofe.service.NightOwlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/night-owl")
@RequiredArgsConstructor
public class NightOwlController {

    private final NightOwlService nightOwl;

    @GetMapping("/status")
    public MiscDtos.NightOwlStatus status(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "false") boolean manualOn) {
        return nightOwl.status(lat, lng, manualOn);
    }

    @PostMapping("/check-in")
    public MiscDtos.CheckInResponse checkIn(@Valid @RequestBody MiscDtos.CheckInRequest request) {
        return nightOwl.checkIn(request);
    }

    @GetMapping("/quick-help")
    public List<MiscDtos.HelpPlace> quickHelp(@RequestParam double lat, @RequestParam double lng) {
        return nightOwl.quickHelp(lat, lng);
    }

    @GetMapping("/emergency")
    public MiscDtos.EmergencyInfo emergency(@RequestParam(defaultValue = "ZW") String country) {
        return nightOwl.emergencyForCountry(country);
    }
}
