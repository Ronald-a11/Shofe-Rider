package com.shofe.service;

import com.shofe.domain.entity.Payment;
import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.PaymentMethod;
import com.shofe.domain.enums.PaymentStatus;
import com.shofe.domain.enums.Role;
import com.shofe.exception.ApiException;
import com.shofe.integrations.payments.PaymentProvider;
import com.shofe.integrations.payments.PaymentProviderRegistry;
import com.shofe.integrations.payments.PesepayProvider;
import com.shofe.repo.PaymentRepository;
import com.shofe.repo.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private final CurrentUserService currentUserService;
    private final RideRepository rides;
    private final PaymentRepository payments;
    private final PaymentProviderRegistry registry;
    private final WalletService walletService;

    /** Pending wallet top-ups awaiting Pesepay confirmation (demo + live poll). */
    private final Map<String, PendingTopUp> pendingTopUps = new ConcurrentHashMap<>();

    @Value("${shofe.payments.default-country:ZW}")
    private String defaultCountry;

    @Transactional
    public PayStart startRidePayment(UUID rideId, String preferredProvider) {
        User rider = currentUserService.require();
        Ride ride = rides.findById(rideId).orElseThrow(() -> ApiException.notFound("Ride not found"));
        if (!ride.getRider().getId().equals(rider.getId())) {
            throw ApiException.forbidden("Only the rider can pay");
        }
        Payment payment = payments.findFirstByRideIdOrderByCreatedAtDesc(rideId)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (payment.getStatus() == PaymentStatus.PAID) {
            return new PayStart(payment.getId(), payment.getProviderRef(), payment.getRedirectUrl(),
                    payment.getPollUrl(), payment.getStatus().name(), false);
        }
        if (payment.getMethod() == PaymentMethod.CASH) {
            throw ApiException.badRequest("Cash is settled with the driver");
        }

        PaymentProvider provider = preferredProvider != null && !preferredProvider.isBlank()
                ? registry.byCode(preferredProvider)
                : registry.forCountryCurrency(defaultCountry, payment.getCurrency());

        payment.setMethod("PESEPAY".equals(provider.code()) ? PaymentMethod.PESEPAY : PaymentMethod.CARD);

        PaymentProvider.InitiateResult started = provider.initiate(new PaymentProvider.InitiateRequest(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                billingEmail(rider),
                rider.getPhone(),
                "Shofe ride " + ride.getId(),
                "ride-pay-" + payment.getId()));
        payment.setProviderRef(started.providerRef());
        payment.setStatus(PaymentStatus.AWAITING_PAYMENT);
        payment.setPollUrl(started.pollUrl());
        payment.setRedirectUrl(started.redirectUrl());
        return new PayStart(payment.getId(), started.providerRef(), started.redirectUrl(), started.pollUrl(),
                started.status(), started.demo());
    }

    @Transactional
    public WalletTopUpStart startWalletTopUp(BigDecimal amount) {
        User driver = currentUserService.require();
        if (driver.getDriverProfile() == null && driver.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Switch to driver mode to top up");
        }
        if (amount == null || amount.compareTo(new BigDecimal("1")) < 0) {
            throw ApiException.badRequest("Top-up amount is too low");
        }
        var wallet = walletService.requireWallet(driver);
        PaymentProvider.InitiateResult started = registry.pesepay().initiate(new PaymentProvider.InitiateRequest(
                null,
                amount,
                wallet.getCurrency(),
                billingEmail(driver),
                driver.getPhone(),
                "Shofe driver wallet top-up",
                "wallet-topup-" + driver.getId() + "-" + System.currentTimeMillis()));
        pendingTopUps.put(started.providerRef(), new PendingTopUp(driver.getId(), amount, wallet.getCurrency()));
        return new WalletTopUpStart(started.providerRef(), started.redirectUrl(), started.pollUrl(), amount,
                wallet.getCurrency(), started.demo());
    }

    @Transactional
    public PaymentProvider.StatusResult poll(String providerRef) {
        PaymentProvider.StatusResult status = resolveProvider(providerRef).checkStatus(providerRef);
        if (status.finalState() && status.success()) {
            payments.findByProviderRef(providerRef).ifPresent(payment -> payment.setStatus(PaymentStatus.PAID));
            PendingTopUp pending = pendingTopUps.remove(providerRef);
            if (pending != null) {
                User driver = currentUserService.require();
                if (!driver.getId().equals(pending.userId())) {
                    throw ApiException.forbidden("This top-up belongs to another account");
                }
                walletService.creditTopUp(driver, pending.amount(), pending.currency(),
                        "topup-" + providerRef, null, "Pesepay wallet top-up");
            }
        }
        return status;
    }

    @Transactional
    public PaymentProvider.StatusResult confirmDemo(String providerRef) {
        registry.pesepay().markDemoPaid(providerRef);
        return poll(providerRef);
    }

    public List<PesepayProvider.MethodOption> pesepayMethods(String currency) {
        return registry.pesepay().methodsFor(currency == null ? "USD" : currency);
    }

    private PaymentProvider resolveProvider(String providerRef) {
        if (providerRef != null && providerRef.startsWith("pi_")) {
            return registry.stripe();
        }
        return registry.pesepay();
    }

    private String billingEmail(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return user.getId() + "@users.shofe.local";
    }

    private record PendingTopUp(UUID userId, BigDecimal amount, String currency) {}

    public record PayStart(UUID paymentId, String providerRef, String redirectUrl, String pollUrl, String status, boolean demo) {}
    public record WalletTopUpStart(String providerRef, String redirectUrl, String pollUrl, BigDecimal amount, String currency, boolean demo) {}
}
