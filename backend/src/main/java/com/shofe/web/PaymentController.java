package com.shofe.web;

import com.shofe.dto.MiscDtos;
import com.shofe.integrations.payments.PaymentProvider;
import com.shofe.integrations.payments.PesepayProvider;
import com.shofe.service.PaymentOrchestrationService;
import com.shofe.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentOrchestrationService payments;
    private final WalletService walletService;
    private final PesepayProvider pesepay;

    @PostMapping("/rides/{rideId}/start")
    public PaymentOrchestrationService.PayStart startRide(
            @PathVariable UUID rideId,
            @RequestBody(required = false) MiscDtos.PayStartRequest body) {
        String provider = body == null ? "PESEPAY" : body.provider();
        return payments.startRidePayment(rideId, provider == null || provider.isBlank() ? "PESEPAY" : provider);
    }

    @GetMapping("/status/{providerRef}")
    public PaymentProvider.StatusResult status(@PathVariable String providerRef) {
        return payments.poll(providerRef);
    }

    @GetMapping("/pesepay/methods")
    public List<PesepayProvider.MethodOption> methods(@RequestParam(defaultValue = "USD") String currency) {
        return payments.pesepayMethods(currency);
    }

    @GetMapping("/wallet")
    public WalletService.WalletView wallet() {
        return walletService.mine();
    }

    @PostMapping("/wallet/top-up")
    public PaymentOrchestrationService.WalletTopUpStart topUp(@Valid @RequestBody MiscDtos.WalletTopUpRequest body) {
        return payments.startWalletTopUp(body.amount());
    }

    @PostMapping("/pesepay/result")
    public Map<String, String> pesepayResult(
            @RequestHeader(value = "authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> payload) {
        if (!pesepay.matchesCallbackAuth(authorization)) {
            return Map.of("received", "false");
        }
        Object reference = payload == null ? null : payload.get("referenceNumber");
        if (reference instanceof String ref && !ref.isBlank()) {
            payments.applyStatus(ref);
        }
        return Map.of("received", "true");
    }
}
