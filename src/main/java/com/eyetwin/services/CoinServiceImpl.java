package com.eyetwin.services;

import com.eyetwin.entities.CoinPackage;
import com.eyetwin.entities.CoinPurchase;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ICoinService;
import com.eyetwin.repository.CoinPurchaseRepository;
import com.eyetwin.tools.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implémentation du service Coin.
 * Miroir de CoinController.php (Symfony) — logique métier extraite.
 *
 * Packages disponibles (miroir de COIN_PACKAGES PHP) :
 *   100  coins → 1,99 €  — Starter Pack
 *   300  coins → 4,99 €  — Popular Pack  ⭐
 *   700  coins → 9,99 €  — Pro Pack
 *   1500 coins → 17,99 € — Elite Pack
 */
public class CoinServiceImpl implements ICoinService {

    // ── Catalogue (ordre d'affichage préservé avec LinkedHashMap) ──
    private static final Map<Integer, CoinPackage> PACKAGES = new LinkedHashMap<>();

    static {
        PACKAGES.put(100,  new CoinPackage(100,  199,  "Starter Pack", false));
        PACKAGES.put(300,  new CoinPackage(300,  499,  "Popular Pack", true));
        PACKAGES.put(700,  new CoinPackage(700,  999,  "Pro Pack",     false));
        PACKAGES.put(1500, new CoinPackage(1500, 1799, "Elite Pack",   false));
    }

    private final CoinPurchaseRepository purchaseRepository;

    public CoinServiceImpl() {
        this.purchaseRepository = new CoinPurchaseRepository();
    }

    // ══════════════════════════════════════════════════════════
    //  PACKAGES
    // ══════════════════════════════════════════════════════════

    @Override
    public List<CoinPackage> getAvailablePackages() {
        return List.copyOf(PACKAGES.values());
    }

    @Override
    public Optional<CoinPackage> getPackageByCoins(int coins) {
        return Optional.ofNullable(PACKAGES.get(coins));
    }

    // ══════════════════════════════════════════════════════════
    //  SOLDE
    // ══════════════════════════════════════════════════════════

    @Override
    public int getCoinBalance(User user) {
        return user.getCoinBalance();
    }

    /**
     * Ajoute des coins au solde de l'utilisateur (en base + en mémoire).
     * Miroir de : $user->setCoinBalance($user->getCoinBalance() + $coins); $em->flush();
     */
    @Override
    public void addCoins(User user, int amount) {
        int newBalance = user.getCoinBalance() + amount;
        updateBalanceInDb(user.getId(), newBalance);
        user.setCoinBalance(newBalance);
        System.out.println("[CoinService] ✅ +" + amount + " coins → userId=" + user.getId()
                + " | nouveau solde=" + newBalance);
    }

    /**
     * Dépense des coins.
     * Retourne false si le solde est insuffisant (pas d'exception).
     */
    @Override
    public boolean spendCoins(User user, int amount) {
        if (user.getCoinBalance() < amount) {
            System.out.println("[CoinService] ❌ Solde insuffisant — besoin=" + amount
                    + " | solde=" + user.getCoinBalance());
            return false;
        }
        int newBalance = user.getCoinBalance() - amount;
        updateBalanceInDb(user.getId(), newBalance);
        user.setCoinBalance(newBalance);
        System.out.println("[CoinService] 💸 -" + amount + " coins → userId=" + user.getId()
                + " | nouveau solde=" + newBalance);
        return true;
    }

    // ══════════════════════════════════════════════════════════
    //  ACHATS
    // ══════════════════════════════════════════════════════════

    /**
     * Enregistre un achat complété et crédite les coins.
     * Miroir de la logique dans success() et webhook() du CoinController PHP.
     */
    @Override
    public CoinPurchase recordPurchase(User user, int coins, double pricePaid, String stripeSessionId) {
        CoinPurchase purchase = new CoinPurchase();
        purchase.setUser(user);
        purchase.setCoinsAmount(coins);
        purchase.setPricePaid(BigDecimal.valueOf(pricePaid));
        purchase.setStripeSessionId(stripeSessionId);
        purchase.setStatus("completed");
        purchase.setCreatedAt(LocalDateTime.now());
        purchase.setCompletedAt(LocalDateTime.now());

        purchaseRepository.save(purchase);
        addCoins(user, coins);

        System.out.println("[CoinService] 🪙 Achat enregistré : " + purchase);
        return purchase;
    }

    /**
     * Idempotence : vérifie si la session Stripe a déjà été traitée.
     * Miroir de : $existing = $em->getRepository(CoinPurchase::class)->findOneBy(['stripeSessionId' => $sessionId]);
     */
    @Override
    public boolean isPurchaseAlreadyProcessed(String stripeSessionId) {
        Optional<CoinPurchase> existing = purchaseRepository.findByStripeSessionId(stripeSessionId);
        return existing.isPresent() && "completed".equals(existing.get().getStatus());
    }

    @Override
    public List<CoinPurchase> getPurchaseHistory(User user) {
        return purchaseRepository.findByUser(user);
    }

    @Override
    public List<CoinPurchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPER DB
    // ══════════════════════════════════════════════════════════

    /**
     * Met à jour le coin_balance de l'utilisateur en base.
     */
    private void updateBalanceInDb(int userId, int newBalance) {
        String sql = "UPDATE user SET coin_balance = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newBalance);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CoinService] ❌ updateBalanceInDb() : " + e.getMessage());
        }
    }
}
