package com.eyetwin.services;

import com.eyetwin.entities.Planning;
import com.eyetwin.entities.TrainingSession;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IPlanningService;
import com.eyetwin.interfaces.ITrainingSessionService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AiCoachService {
    private final IPlanningService planningService;
    private final ITrainingSessionService sessionService;
    private final HttpClient httpClient;

    public AiCoachService() {
        this.planningService = new PlanningServiceImpl();
        this.sessionService = new TrainingSessionServiceImpl();
        this.httpClient = HttpClient.newHttpClient();
    }

    public CompletableFuture<String> getRecommendations(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (user == null) {
                    return "Veuillez vous connecter pour recevoir des recommandations personnalisées.";
                }

                String planningContext = buildPlanningContext();
                String userHistoryContext = buildUserHistoryContext(user);

                String systemPrompt = """
                    Tu es un Coach e-sport de haut niveau sur la plateforme EyeTwin.
                    Ta mission est d'analyser l'historique de l'utilisateur et de lui recommander la MEILLEURE session prochaine dans le planning.
            
                    CONTEXTE DU PLANNING ACTUEL :
                    %s
            
                    HISTORIQUE DE L'UTILISATEUR :
                    %s
            
                    DIRECTIVES :
                    1. Sois motivant et utilise un ton de coach professionnel.
                    2. Identifie les jeux que l'utilisateur pratique le plus et son niveau habituel.
                    3. Choisis 1 ou 2 sessions du planning qui l'aideraient à progresser.
                    4. Si l'utilisateur n'a pas d'historique, souhaite-lui la bienvenue et propose-lui une session de découverte selon le planning.
                    5. Réponds en français (ou anglais si l'historique suggère une préférence).
                    6. Format : Utilise du markdown (gras, listes) pour rendre le conseil lisible.
                    """.formatted(planningContext, userHistoryContext);

                JSONObject payload = new JSONObject();
                payload.put("model", "llama-3.3-70b-versatile");

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
                messages.put(new JSONObject().put("role", "user").put("content", "Analyse mon profil et donne-moi tes recommandations de coaching pour les prochaines sessions."));

                payload.put("messages", messages);
                payload.put("temperature", 0.8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GroqConfig.API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + GroqConfig.API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    return jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                } else {
                    return "Désolé, l'IA de coaching rencontre une erreur technique (Code: " + response.statusCode() + ").";
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "Désolé, je ne peux pas générer de recommandations pour le moment.";
            }
        });
    }

    private String buildPlanningContext() throws SQLException {
        List<Planning> plannings = planningService.getAllPlannings();
        if (plannings.isEmpty()) {
            return "Aucune session disponible.";
        }

        StringBuilder sb = new StringBuilder("SESSIONS DISPONIBLES :\n");
        for (Planning p : plannings) {
            int participants = sessionService.countActiveParticipants(p.getIdPlanning());
            sb.append("- ID ").append(p.getIdPlanning()).append(": ").append(p.getType()).append(" (")
                    .append(p.getLevel()).append(") le ").append(p.getDate()).append(" @ ").append(p.getLocalisation())
                    .append(". Description: ").append(p.getDescription()).append(". Inscrits: ").append(participants).append("\n");
        }
        return sb.toString();
    }

    private String buildUserHistoryContext(User user) throws SQLException {
        List<TrainingSession> sessions = sessionService.getActiveSessionsByUser(user.getId());
        if (sessions.isEmpty()) {
            return "Aucun historique.";
        }

        StringBuilder sb = new StringBuilder();
        for (TrainingSession s : sessions) {
            Planning p = planningService.getPlanningById(s.getIdPlanning());
            if (p != null) {
                sb.append("- A participé à : ").append(p.getType()).append(" (").append(p.getLevel()).append(") le ").append(p.getDate()).append("\n");
            }
        }
        return sb.toString();
    }
}
