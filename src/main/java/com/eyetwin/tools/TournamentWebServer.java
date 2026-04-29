package com.eyetwin.tools;

import com.eyetwin.entities.Match;
import com.eyetwin.entities.Tournoi;
import com.eyetwin.interfaces.IMatchService;
import com.eyetwin.interfaces.ITournoiService;
import com.eyetwin.services.MatchServiceImpl;
import com.eyetwin.services.TournoiServiceImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class TournamentWebServer {

    private static HttpServer server;
    private static String localIp;
    private static int port = 0; // 0 allows OS to pick an available port
    private static ITournoiService tournoiService = new TournoiServiceImpl();
    private static IMatchService matchService = new MatchServiceImpl();

    public static void start() {
        if (server != null) return; // already started
        
        try {
            localIp = InetAddress.getLocalHost().getHostAddress();
            server = HttpServer.create(new InetSocketAddress(port), 0);
            port = server.getAddress().getPort(); // get the dynamically assigned port
            
            server.createContext("/tournoi", new TournoiHandler());
            
            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("=========================================================");
            System.out.println("📱 [Web Server] Started for QR Codes!");
            System.out.println("🔗 Mobile Server URL: http://" + localIp + ":" + port);
            System.out.println("=========================================================");
            
        } catch (IOException e) {
            System.err.println("Failed to start Tournament Web Server: " + e.getMessage());
        }
    }

    public static String getBaseUrl() {
        if (localIp == null) {
            try {
                localIp = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                localIp = "127.0.0.1";
            }
        }
        return "http://" + localIp + ":" + port;
    }

    static class TournoiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            int tournoiId = -1;
            
            if (query != null && query.contains("id=")) {
                try {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("id=")) {
                            tournoiId = Integer.parseInt(param.split("=")[1]);
                            break;
                        }
                    }
                } catch (Exception e) {
                    // ignoré
                }
            }

            Tournoi tournoi = null;
            if (tournoiId != -1) {
                tournoi = tournoiService.getById(tournoiId);
            }

            String response;
            if (tournoi == null) {
                response = "<h1>Tournoi introuvable / Tournament not found</h1>";
            } else {
                response = generateModernHtml(tournoi);
            }

            byte[] bytes = response.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");
            exchange.sendResponseHeaders(200, bytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String generateModernHtml(Tournoi t) {
            String nom = t.getNom() != null ? t.getNom() : "Tournoi Sans Nom";
            String type = t.getTypeTournoi() != null ? t.getTypeTournoi().toString() : "Standard";
            String dateDebut = t.getDateDebut() != null ? t.getDateDebut().toString() : "?";
            String dateFin = t.getDateFin() != null ? t.getDateFin().toString() : "?";
            double prix = t.getPrix();
            String description = t.getDescription() != null ? t.getDescription() : "Aucune description fournie.";

            java.util.List<Match> matches = matchService.getByTournoi(t.getId());
            StringBuilder matchesHtml = new StringBuilder();
            
            if (matches == null || matches.isEmpty()) {
                matchesHtml.append("<div class=\"match-card\"><div class=\"match-teams\">Aucun match prévu pour l'instant</div></div>");
            } else {
                for (Match m : matches) {
                    matchesHtml.append("<div class=\"match-card\">")
                               .append("  <div class=\"match-teams\">").append(m.getEquipe1()).append(" <span class=\"vs\">VS</span> ").append(m.getEquipe2()).append("</div>")
                               .append("  <div class=\"match-info\">📍 ").append(m.getPlayMode() != null ? m.getPlayMode() : "En Ligne").append(" | 📅 ").append(m.getDateMatch() != null ? m.getDateMatch() : "?").append("</div>")
                               .append("</div>");
                }
            }

            return "<!DOCTYPE html>\n" +
                    "<html lang=\"fr\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <title>" + nom + " - EyeTwin</title>\n" +
                    "    <style>\n" +
                    "        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&family=Rajdhani:wght@500;700&display=swap');\n" +
                    "        :root {\n" +
                    "            --bg: #090a0f;\n" +
                    "            --card-bg: #151828;\n" +
                    "            --card-light: #1e293b;\n" +
                    "            --text-main: #f8fafc;\n" +
                    "            --text-muted: #94a3b8;\n" +
                    "            --accent: #3b82f6;\n" +
                    "            --accent-glow: rgba(59, 130, 246, 0.5);\n" +
                    "            --success: #10b981;\n" +
                    "        }\n" +
                    "        body {\n" +
                    "            margin: 0;\n" +
                    "            padding: 20px 0;\n" +
                    "            font-family: 'Inter', sans-serif;\n" +
                    "            background-color: var(--bg);\n" +
                    "            color: var(--text-main);\n" +
                    "            display: flex;\n" +
                    "            justify-content: center;\n" +
                    "            align-items: flex-start;\n" +
                    "            min-height: 100vh;\n" +
                    "            background-image: radial-gradient(circle at top, #1e293b 0%, var(--bg) 60%);\n" +
                    "        }\n" +
                    "        .container {\n" +
                    "            width: 90%;\n" +
                    "            max-width: 500px;\n" +
                    "            background: var(--card-bg);\n" +
                    "            border-radius: 20px;\n" +
                    "            padding: 30px;\n" +
                    "            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 20px var(--accent-glow);\n" +
                    "            border: 1px solid rgba(255, 255, 255, 0.05);\n" +
                    "            animation: fadeIn 0.5s ease-out;\n" +
                    "            box-sizing: border-box;\n" +
                    "        }\n" +
                    "        @keyframes fadeIn {\n" +
                    "            from { opacity: 0; transform: translateY(20px); }\n" +
                    "            to { opacity: 1; transform: translateY(0); }\n" +
                    "        }\n" +
                    "        .header-section {\n" +
                    "            text-align: center;\n" +
                    "            margin-bottom: 25px;\n" +
                    "        }\n" +
                    "        .badge {\n" +
                    "            display: inline-block;\n" +
                    "            background: linear-gradient(135deg, var(--success), #059669);\n" +
                    "            color: white;\n" +
                    "            font-weight: 600;\n" +
                    "            padding: 5px 15px;\n" +
                    "            border-radius: 30px;\n" +
                    "            font-size: 0.85rem;\n" +
                    "            margin-bottom: 15px;\n" +
                    "            text-transform: uppercase;\n" +
                    "            letter-spacing: 1px;\n" +
                    "        }\n" +
                    "        h1 {\n" +
                    "            font-family: 'Rajdhani', sans-serif;\n" +
                    "            font-size: 2.2rem;\n" +
                    "            margin: 0 0 10px 0;\n" +
                    "            text-transform: uppercase;\n" +
                    "            letter-spacing: 1px;\n" +
                    "            background: linear-gradient(to right, #fff, #94a3b8);\n" +
                    "            -webkit-background-clip: text;\n" +
                    "            -webkit-text-fill-color: transparent;\n" +
                    "        }\n" +
                    "        .dates {\n" +
                    "            color: var(--accent);\n" +
                    "            font-weight: 600;\n" +
                    "            font-size: 1rem;\n" +
                    "            background: rgba(59, 130, 246, 0.1);\n" +
                    "            padding: 10px;\n" +
                    "            border-radius: 10px;\n" +
                    "            display: inline-block;\n" +
                    "        }\n" +
                    "        .section-title {\n" +
                    "            font-family: 'Rajdhani', sans-serif;\n" +
                    "            font-size: 1.4rem;\n" +
                    "            border-bottom: 1px solid rgba(255,255,255,0.1);\n" +
                    "            padding-bottom: 8px;\n" +
                    "            margin: 25px 0 15px 0;\n" +
                    "            color: #e2e8f0;\n" +
                    "            text-transform: uppercase;\n" +
                    "        }\n" +
                    "        .description {\n" +
                    "            color: var(--text-muted);\n" +
                    "            line-height: 1.6;\n" +
                    "            font-size: 0.95rem;\n" +
                    "            background: rgba(0,0,0,0.2);\n" +
                    "            padding: 15px;\n" +
                    "            border-radius: 10px;\n" +
                    "            border-left: 3px solid var(--accent);\n" +
                    "        }\n" +
                    "        .match-card {\n" +
                    "            background: var(--card-light);\n" +
                    "            padding: 12px 15px;\n" +
                    "            border-radius: 10px;\n" +
                    "            margin-bottom: 10px;\n" +
                    "            border: 1px solid rgba(255,255,255,0.05);\n" +
                    "            transition: transform 0.2s;\n" +
                    "        }\n" +
                    "        .match-card:hover {\n" +
                    "            transform: translateX(5px);\n" +
                    "            border-color: rgba(59, 130, 246, 0.3);\n" +
                    "        }\n" +
                    "        .match-teams {\n" +
                    "            font-weight: 600;\n" +
                    "            font-size: 1.05rem;\n" +
                    "            margin-bottom: 5px;\n" +
                    "        }\n" +
                    "        .vs {\n" +
                    "            color: var(--accent);\n" +
                    "            font-size: 0.85rem;\n" +
                    "        }\n" +
                    "        .match-info {\n" +
                    "            color: var(--text-muted);\n" +
                    "            font-size: 0.85rem;\n" +
                    "        }\n" +
                    "        .price-box {\n" +
                    "            background: linear-gradient(135deg, rgba(251, 191, 36, 0.1), rgba(0,0,0,0.3));\n" +
                    "            border: 1px solid rgba(251, 191, 36, 0.2);\n" +
                    "            border-radius: 12px;\n" +
                    "            padding: 15px;\n" +
                    "            display: flex;\n" +
                    "            justify-content: space-between;\n" +
                    "            align-items: center;\n" +
                    "            margin: 25px 0;\n" +
                    "        }\n" +
                    "        .price-label {\n" +
                    "            color: var(--text-muted);\n" +
                    "            font-weight: 600;\n" +
                    "            text-transform: uppercase;\n" +
                    "            font-size: 0.9rem;\n" +
                    "        }\n" +
                    "        .price-value {\n" +
                    "            font-family: 'Rajdhani', sans-serif;\n" +
                    "            font-size: 1.8rem;\n" +
                    "            font-weight: 700;\n" +
                    "            color: #fbbf24;\n" +
                    "            text-shadow: 0 0 10px rgba(251, 191, 36, 0.3);\n" +
                    "        }\n" +
                    "        .action-button {\n" +
                    "            display: block;\n" +
                    "            width: 100%;\n" +
                    "            padding: 15px;\n" +
                    "            background: linear-gradient(135deg, var(--accent), #2563eb);\n" +
                    "            color: white;\n" +
                    "            text-align: center;\n" +
                    "            text-decoration: none;\n" +
                    "            font-weight: 600;\n" +
                    "            font-size: 1.1rem;\n" +
                    "            border-radius: 10px;\n" +
                    "            transition: all 0.3s ease;\n" +
                    "            box-sizing: border-box;\n" +
                    "            border: none;\n" +
                    "            text-transform: uppercase;\n" +
                    "            letter-spacing: 1px;\n" +
                    "            box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);\n" +
                    "        }\n" +
                    "        .action-button:hover {\n" +
                    "            transform: translateY(-2px);\n" +
                    "            box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);\n" +
                    "        }\n" +
                    "        .footer {\n" +
                    "            margin-top: 30px;\n" +
                    "            font-size: 0.8rem;\n" +
                    "            color: #475569;\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"header-section\">\n" +
                    "            <div class=\"badge\">" + type + "</div>\n" +
                    "            <h1>" + nom + "</h1>\n" +
                    "            <div class=\"dates\">\n" +
                    "                📅 " + dateDebut + " ➔ " + dateFin + "\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "\n" +
                    "        <div class=\"section-title\">À Propos</div>\n" +
                    "        <div class=\"description\">\n" +
                    "            " + description + "\n" +
                    "        </div>\n" +
                    "\n" +
                    "        <div class=\"section-title\">Matchs Programmés</div>\n" +
                    "        <div class=\"matches-list\">\n" +
                    matchesHtml.toString() +
                    "        </div>\n" +
                    "\n" +
                    "        <div class=\"price-box\">\n" +
                    "            <span class=\"price-label\">Frais d'inscription</span>\n" +
                    "            <span class=\"price-value\">" + prix + " DT</span>\n" +
                    "        </div>\n" +
                    "\n" +
                    "        <a href=\"#\" class=\"action-button\" onclick=\"alert('Ouvrez l\\'application EyeTwin pour vous inscrire !')\">S'inscrire Maintenant ⚔️</a>\n" +
                    "\n" +
                    "        <div class=\"footer\">\n" +
                    "            ⚡ Propulsé par EyeTwin Platform | " + localIp + "\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";
        }
    }
}
