package com.shofe.service;

import com.shofe.domain.entity.DriverWallet;
import com.shofe.domain.entity.LedgerEntry;
import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.LedgerEntryType;
import com.shofe.domain.enums.Role;
import com.shofe.exception.ApiException;
import com.shofe.repo.DriverWalletRepository;
import com.shofe.repo.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final DriverWalletRepository wallets;
    private final LedgerEntryRepository ledger;
    private final CurrentUserService currentUserService;

    @Value("${shofe.commission.percent:12}")
    private BigDecimal commissionPercent;

    @Value("${shofe.commission.min-wallet:5}")
    private BigDecimal minWallet;

    @Value("${shofe.fare.currency}")
    private String defaultCurrency;

    @Transactional
    public DriverWallet requireWallet(User driver) {
        return wallets.findByUserId(driver.getId()).orElseGet(() -> {
            DriverWallet wallet = new DriverWallet();
            wallet.setUser(driver);
            wallet.setCurrency(defaultCurrency);
            wallet.setBalance(BigDecimal.ZERO);
            return wallets.save(wallet);
        });
    }

    @Transactional(readOnly = true)
    public WalletView mine() {
        User user = currentUserService.require();
        if (user.getDriverProfile() == null && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Switch to driver mode to open a commission wallet");
        }
        DriverWallet wallet = wallets.findByUserId(user.getId()).orElse(null);
        BigDecimal balance = wallet == null ? BigDecimal.ZERO : wallet.getBalance();
        String currency = wallet == null ? defaultCurrency : wallet.getCurrency();
        return new WalletView(balance, currency, minWallet, commissionPercent,
                balance.compareTo(minWallet) >= 0,
                ledger.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().limit(20).map(this::toLine).toList());
    }

    @Transactional
    public void creditTopUp(User driver, BigDecimal amount, String currency, String idempotencyKey, UUID paymentId, String note) {
        if (ledger.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }
        DriverWallet wallet = requireWallet(driver);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setCurrency(currency);
        wallet.setUpdatedAt(Instant.now());
        LedgerEntry entry = new LedgerEntry();
        entry.setUser(driver);
        entry.setType(LedgerEntryType.WALLET_TOPUP);
        entry.setAmount(amount);
        entry.setCurrency(currency);
        entry.setIdempotencyKey(idempotencyKey);
        entry.setPaymentId(paymentId);
        entry.setNote(note);
        ledger.save(entry);
    }

    @Transactional
    public void deductCommission(Ride ride) {
        User driver = ride.getDriver();
        String key = "commission-" + ride.getId();
        if (ledger.findByIdempotencyKey(key).isPresent()) {
            return;
        }
        BigDecimal commission = ride.getFare()
                .multiply(commissionPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        if (commission.signum() <= 0) {
            return;
        }
        DriverWallet wallet = requireWallet(driver);
        wallet.setBalance(wallet.getBalance().subtract(commission));
        wallet.setUpdatedAt(Instant.now());
        LedgerEntry entry = new LedgerEntry();
        entry.setUser(driver);
        entry.setType(LedgerEntryType.COMMISSION_DEDUCT);
        entry.setAmount(commission.negate());
        entry.setCurrency(ride.getCurrency());
        entry.setIdempotencyKey(key);
        entry.setRideId(ride.getId());
        entry.setNote("Commission " + commissionPercent + "% on ride " + ride.getId());
        ledger.save(entry);
    }

    @Transactional(readOnly = true)
    public void assertCanReceiveRequests(User driver) {
        DriverWallet wallet = wallets.findByUserId(driver.getId()).orElse(null);
        BigDecimal balance = wallet == null ? BigDecimal.ZERO : wallet.getBalance();
        if (balance.compareTo(minWallet) < 0) {
            throw ApiException.forbidden("Top up your commission wallet before going online (min " + minWallet + ")");
        }
    }

    private LedgerLine toLine(LedgerEntry entry) {
        return new LedgerLine(entry.getId(), entry.getType().name(), entry.getAmount(), entry.getCurrency(),
                entry.getNote(), entry.getCreatedAt());
    }

    public record WalletView(
            BigDecimal balance,
            String currency,
            BigDecimal minBalance,
            BigDecimal commissionPercent,
            boolean canReceiveRequests,
            List<LedgerLine> recent
    ) {}

    public record LedgerLine(UUID id, String type, BigDecimal amount, String currency, String note, Instant createdAt) {}
}
