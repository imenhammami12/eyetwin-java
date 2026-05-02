package com.eyetwin.services;

import com.eyetwin.entities.Planning;
import com.eyetwin.interfaces.IPlanningService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChatbotService {
    private final IPlanningService planningService;
    private final HttpClient httpClient;

    public ChatbotService() {
        this.planningService = new PlanningServiceImpl();
        this.httpClient = HttpClient.newHttpClient();
    }

    public CompletableFuture<String> getResponse(String question) {
        final String q = question == null ? "" : question.trim();
        return CompletableFuture.supplyAsync(() -> {
            String lang = detectLanguage(q);
            String apiKey = GroqConfig.apiKey();

            // Priorité absolue à Groq si la clé est dispo (comme Symfony)
            if (apiKey != null && !apiKey.isBlank()) {
                try {
                    return getGroqResponse(q, apiKey, lang);
                } catch (Exception e) {
                    // fallback rules + message utile si problème d'auth
                    String msg = e.getMessage() == null ? "" : e.getMessage();
                    if (msg.contains("status=401") || msg.contains("status=403")) {
                        return "en".equals(lang)
                                ? "Groq authentication failed. Please verify GROQ_API_KEY."
                                : "Échec d’authentification Groq. Vérifiez la clé GROQ_API_KEY.";
                    }
                    return getRuleBasedResponse(q.toLowerCase(), lang);
                }
            }
            return getRuleBasedResponse(q.toLowerCase(), lang);
        });
    }

    private String getGroqResponse(String question, String apiKey, String lang) throws Exception {
        // Mirror Symfony ChatbotService.php as close as possible
        String context = buildPlanningContext(lang);
        String languageName = "en".equals(lang) ? "English" : "French";
        String forcedInstruction = "en".equals(lang) ? "Respond strictly in English." : "Réponds strictement en français.";

        String systemPrompt = """
            Tu es un assistant virtuel expert en e-sport pour la plateforme EyeTwin.

            CONTEXTE DES SESSIONS (données réelles de la base de données) :
            %s

            DIRECTIVES :
            1. Utilise UNIQUEMENT les données fournies ci-dessus pour répondre aux questions sur le planning.
            2. Si une session n'est pas dans la liste, elle n'existe pas.
            3. Réponds aux questions sur l'e-sport en général (stratégies, jeux, etc.) de manière professionnelle.
            4. Si l'utilisateur pose une question hors sujet (pas d'e-sport, pas de planning), refuse poliment.
            5. Réponds TOUJOURS dans la même langue que l'utilisateur (%s).
            6. Sois précis sur les détails : date, heure, lieu, niveau, et description.
            7. %s
            """.formatted(context, languageName, forcedInstruction);

        JSONObject payload = new JSONObject();
        payload.put("model", GroqConfig.MODEL);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", question));
        payload.put("messages", messages);
        payload.put("temperature", 0.7);
        payload.put("max_tokens", 500);
        payload.put("top_p", 1);
        payload.put("stream", false);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GroqConfig.API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            String body = res.body() == null ? "" : res.body().trim();
            if (body.length() > 600) body = body.substring(0, 600) + "...";
            throw new IllegalStateException("Groq API request failed (status=" + res.statusCode() + ", body=" + body + ")");
        }

        JSONObject json = new JSONObject(res.body());
        return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
    }

    private String buildPlanningContext(String lang) throws SQLException {
        List<Planning> plannings = planningService.getAllPlannings();
        if (plannings == null || plannings.isEmpty()) {
            return "en".equals(lang)
                    ? "No training session is available at the moment."
                    : "Aucune session d'entraînement n'est disponible pour le moment.";
        }

        // Symfony: ordered by date ASC. Here we just sort in-memory.
        plannings = plannings.stream()
                .sorted((a, b) -> {
                    if (a.getDate() == null && b.getDate() == null) return 0;
                    if (a.getDate() == null) return 1;
                    if (b.getDate() == null) return -1;
                    int c = a.getDate().compareTo(b.getDate());
                    if (c != 0) return c;
                    if (a.getTime() == null && b.getTime() == null) return 0;
                    if (a.getTime() == null) return 1;
                    if (b.getTime() == null) return -1;
                    return a.getTime().compareTo(b.getTime());
                })
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("en".equals(lang)
                ? "Exhaustive list of training sessions:\n"
                : "Liste exhaustive des sessions d'entraînement :\n");

        for (Planning p : plannings) {
            sb.append("- SESSION CODED #").append(p.getIdPlanning()).append(" :\n");
            sb.append("  Type : ").append(nullSafe(p.getType())).append("\n");
            sb.append("  Level : ").append(nullSafe(p.getLevel())).append("\n");
            sb.append("  Date : ").append(nullSafe(p.getDate())).append(" at ").append(nullSafe(p.getTime())).append("\n");
            sb.append("  Location : ").append(nullSafe(p.getLocalisation())).append("\n");
            sb.append("  Description : ").append(nullSafe(p.getDescription())).append("\n");
            sb.append("  Constraint : ").append(p.isNeedPartner() ? "Partner required" : "No partner needed").append("\n\n");
        }
        return sb.toString();
    }

    private String detectLanguage(String text) {
        if (text == null) return "fr";
        String t = text.toLowerCase();
        String englishPattern = "\\b(the|a|an|is|are|am|i|you|he|she|they|we|what|whats|what's|where|how|why|when|which|who|whom|whose|this|that|these|those|at|on|in|with|from|by|for|to|of|and|or|not|no|yes|do|does|did|will|would|can|could|shall|should|may|might|must|have|has|had|getting|going|about|my|your|his|her|its|our|their|be|been|being|game|gaming|esport)\\b";
        String frenchPattern = "\\b(le|la|les|un|une|des|est|sont|suis|je|tu|il|elle|ils|elles|nous|vous|pourquoi|comment|quand|que|qui|quel|quelle|quels|quelles|où|dans|sur|avec|pour|par|mais|ou|et|ne|pas|oui|non|faire|fait|faites|font|aider|aide|veux|veut|voulons|voulez|veulent|ce|cette|ces|mon|ton|son|notre|votre|leur|mes|tes|ses|nos|vos|leurs|être|été|en|jeu|jeux|esport)\\b";
        int en = countRegexMatches(t, englishPattern);
        int fr = countRegexMatches(t, frenchPattern);
        return en > fr ? "en" : "fr";
    }

    private int countRegexMatches(String text, String regex) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
            int c = 0;
            while (m.find()) c++;
            return c;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getRuleBasedResponse(String questionLower, String lang) {
        boolean en = "en".equals(lang);
        if (questionLower == null) questionLower = "";

        if (questionLower.contains("join") || questionLower.contains("rejoindre")) {
            return en
                    ? "To join a session, click 'Join Session' on the session card. You must be logged in."
                    : "Pour rejoindre une session, cliquez sur 'Join Session' sur la carte de la session. Vous devez être connecté.";
        }
        if (questionLower.contains("when") || questionLower.contains("quand") || questionLower.contains("next") || questionLower.contains("prochaine")) {
            return en
                    ? "I can tell you the next available session if you ask: 'What is the next session?'"
                    : "Je peux vous indiquer la prochaine session disponible. Demandez : 'Quelle est la prochaine session ?'";
        }
        if (questionLower.contains("location") || questionLower.contains("où") || questionLower.contains("localisation")) {
            return en
                    ? "Ask me: 'Where is the session located?' and I’ll answer using the available sessions."
                    : "Demandez : 'Où se déroule la session ?' et je répondrai avec les sessions disponibles.";
        }
        return en
                ? "I’m specialized in e-sports and training sessions. Ask me about sessions (time, location, level, type) or e-sports tips."
                : "Je suis spécialisé en e-sport et en sessions d'entraînement. Posez-moi une question sur les sessions (heure, lieu, niveau, type) ou des conseils e-sport.";
    }

    private String nullSafe(Object o) {
        return o == null ? "—" : String.valueOf(o);
    }
}
