package com.shofe.web;

import com.shofe.dto.RideDtos;
import com.shofe.service.AccountService;
import com.shofe.service.ChatService;
import com.shofe.service.FareService;
import com.shofe.service.LocationService;
import com.shofe.service.OfferService;
import com.shofe.service.RideRequestService;
import com.shofe.service.RideService;
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
public class RideController {

    private final FareService fareService;
    private final RideRequestService rideRequestService;
    private final OfferService offerService;
    private final RideService rideService;
    private final ChatService chatService;
    private final LocationService locationService;
    private final AccountService accountService;

    @PostMapping("/rides/quote")
    public RideDtos.QuoteResponse quote(@Valid @RequestBody RideDtos.QuoteRequest request) {
        return fareService.quote(request);
    }

    @PostMapping("/rides/requests")
    public RideDtos.RideRequestResponse create(@Valid @RequestBody RideDtos.CreateRequest request) {
        return rideRequestService.create(request);
    }

    @GetMapping("/rides/requests/mine")
    public List<RideDtos.RideRequestResponse> mine() {
        return rideRequestService.mine();
    }

    @GetMapping("/rides/requests/open")
    public List<RideDtos.RideRequestResponse> open() {
        return rideRequestService.openForDriver();
    }

    @GetMapping("/rides/requests/{id}")
    public RideDtos.RideRequestResponse getRequest(@PathVariable UUID id) {
        return rideRequestService.get(id);
    }

    @PostMapping("/rides/requests/{id}/cancel")
    public RideDtos.RideRequestResponse cancelRequest(@PathVariable UUID id) {
        return rideRequestService.cancel(id);
    }

    @PostMapping("/rides/requests/{id}/offers")
    public RideDtos.OfferResponse offer(@PathVariable UUID id, @Valid @RequestBody RideDtos.OfferRequest request) {
        return offerService.submit(id, request);
    }

    @GetMapping("/rides/requests/{id}/offers")
    public List<RideDtos.OfferResponse> offers(@PathVariable UUID id) {
        return offerService.list(id);
    }

    @PostMapping("/rides/offers/{offerId}/select")
    public RideDtos.RideResponse select(@PathVariable UUID offerId) {
        return rideService.selectOffer(offerId);
    }

    @GetMapping("/rides/active")
    public RideDtos.RideResponse active() {
        return rideService.active();
    }

    @GetMapping("/rides/history")
    public List<RideDtos.RideResponse> history() {
        return rideService.history();
    }

    @GetMapping("/rides/{id}")
    public RideDtos.RideResponse ride(@PathVariable UUID id) {
        return rideService.get(id);
    }

    @PostMapping("/rides/{id}/status")
    public RideDtos.RideResponse status(@PathVariable UUID id, @Valid @RequestBody RideDtos.StatusRequest request) {
        return rideService.updateStatus(id, request);
    }

    @PostMapping("/rides/{id}/verify-pin")
    public RideDtos.RideResponse pin(@PathVariable UUID id, @Valid @RequestBody RideDtos.PinRequest request) {
        return rideService.verifyPin(id, request.pin());
    }

    @PostMapping("/rides/{id}/pay")
    public RideDtos.RideResponse pay(@PathVariable UUID id) {
        return rideService.pay(id);
    }

    @PostMapping("/rides/{id}/rate")
    public void rate(@PathVariable UUID id, @Valid @RequestBody RideDtos.RateRequest request) {
        accountService.rate(id, request);
    }

    @GetMapping("/rides/{id}/chat")
    public List<RideDtos.ChatResponse> chat(@PathVariable UUID id) {
        return chatService.list(id);
    }

    @PostMapping("/rides/{id}/chat")
    public RideDtos.ChatResponse sendChat(@PathVariable UUID id, @Valid @RequestBody RideDtos.ChatRequest request) {
        return chatService.send(id, request.body());
    }

    @PostMapping("/drivers/location")
    public void location(@Valid @RequestBody RideDtos.LocationRequest request) {
        locationService.update(request);
    }
}
