package com.eyetwin.interfaces;

import com.eyetwin.entities.CoinPackage;
import com.eyetwin.entities.CoinPurchase;
import com.eyetwin.entities.User;

import java.util.List;
import java.util.Optional;

/**
 * Interface du service de gestion des coins.
 * Miroir de CoinController.php (Symfony)
 */
public interface ICoinService {

    // ── Packages ───────────────────────────────────────────────

    /** Retourne tous les packages disponibles (Starter, Popular, Pro, Elite) */
    List<CoinPackage> getAvailablePackages();

    /** Retourne un package par nombre de coins (ex: 100, 300, 700, 1500) */
    Optional<CoinPackage> getPackageByCoins(int coins);

    // ── Solde ──────────────────────────────────────────────────

    /** Solde actuel de l'utilisateur */
    int getCoinBalance(User user);

    /** Ajouter des coins au solde */
    void addCoins(User user, int amount);

    /** Dépenser des coins (retourne false si solde insuffisant) */
    boolean spendCoins(User user, int amount);

    // ── Achats ─────────────────────────────────────────────────

    /** Enregistre un achat en base (status = completed) */
    CoinPurchase recordPurchase(User user, int coins, double pricePaid, String stripeSessionId);

    /** Vérifie si une session Stripe a déjà été traitée (idempotence) */
    boolean isPurchaseAlreadyProcessed(String stripeSessionId);

    /** Historique des achats d'un utilisateur */
    List<CoinPurchase> getPurchaseHistory(User user);

    /** Historique complet (admin) */
    List<CoinPurchase> getAllPurchases();
}
