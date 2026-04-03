package com.eyetwin.interfaces;

/**
 * IStatsService — contrat des statistiques globales.
 * Fusionne StatsDAO Symfony.
 */
public interface IStatsService {
    int countPlayers();
    int countCoaches();
    int countTeams();
    int countTournaments();
}