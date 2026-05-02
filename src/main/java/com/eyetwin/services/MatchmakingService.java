package com.eyetwin.services;

import com.eyetwin.entities.Planning;
import com.eyetwin.entities.User;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de Matchmaking basé sur la table Planning.
 * Permet de trouver des sessions qui cherchent des partenaires (Practice/Scrim).
 */
public class MatchmakingService {

    /**
     * Trouve les meilleures sessions (Plannings) pour un utilisateur.
     * On favorise les sessions qui cochent "need_partner".
     */
    public List<Planning> findMatchesForUser(User user, List<Planning> allPlannings, int limit) {
        return allPlannings.stream()
                .filter(Planning::isNeedPartner) // Uniquement ceux qui cherchent un partenaire
                .sorted(Comparator.comparingDouble((Planning p) -> calculatePlanningScore(user, p)).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calcule un score de compatibilité entre un utilisateur et un planning.
     * (Simulé car on ne stocke pas le niveau du User en DB).
     */
    private double calculatePlanningScore(User user, Planning planning) {
        double score = 0;

        // 1. Priorité aux sessions futures
        if (planning.getDate() != null && planning.getDate().isAfter(java.time.LocalDate.now())) {
            score += 50;
        }

        // 2. Bonus pour les types populaires (Training/Scrim)
        if ("Training".equalsIgnoreCase(planning.getType())) {
            score += 20;
        } else if ("Scrim".equalsIgnoreCase(planning.getType())) {
            score += 30;
        }

        // 3. Plus il manque de participants, plus c'est urgent
        if (planning.getParticipantCount() < 2) {
            score += 20;
        }

        return score;
    }
}
