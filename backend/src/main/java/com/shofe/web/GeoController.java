package com.shofe.web;

import com.shofe.dto.MiscDtos;
import com.shofe.dto.RideDtos;
import com.shofe.service.GeoService;
import com.shofe.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;
    private final RideService rideService;

    @GetMapping("/api/geo/search")
    public List<MiscDtos.Place> search(@RequestParam String q) {
        return geoService.search(q);
    }

    @GetMapping("/api/geo/route")
    public RideDtos.QuoteResponse route(
            @RequestParam double fromLat,
            @RequestParam double fromLng,
            @RequestParam double toLat,
            @RequestParam double toLng) {
        return geoService.route(fromLat, fromLng, toLat, toLng);
    }

    @GetMapping("/api/public/track/{token}")
    public RideDtos.TrackResponse track(@PathVariable String token) {
        return rideService.track(token);
    }
}
