package com.shofe.service;

import com.shofe.domain.entity.Payment;
import com.shofe.domain.enums.PaymentStatus;
import com.shofe.integrations.payments.PaymentProvider;
import com.shofe.integrations.payments.PaymentProviderRegistry;
import com.shofe.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Background poll for pending Pesepay payments when the result callback is delayed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPollJob {

    private final PaymentRepository payments;
    private final PaymentProviderRegistry registry;

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void pollPending() {
        List<Payment> pending = payments.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.AWAITING_PAYMENT)
                .filter(p -> p.getProviderRef() != null && !p.getProviderRef().isBlank())
                .limit(40)
                .toList();
        for (Payment payment : pending) {
            try {
                PaymentProvider provider = payment.getProviderRef().startsWith("pi_")
                        ? registry.stripe()
                        : registry.pesepay();
                PaymentProvider.StatusResult status = provider.checkStatus(payment.getProviderRef());
                if (status.finalState() && status.success()) {
                    payment.setStatus(PaymentStatus.PAID);
                    log.info("Payment {} marked PAID via poll", payment.getId());
                }
            } catch (Exception ex) {
                log.debug("Payment poll skipped for {}: {}", payment.getId(), ex.getMessage());
            }
        }
    }
}
