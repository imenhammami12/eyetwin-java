package com.eyetwin.tools;

import com.eyetwin.entities.Planning;
import com.eyetwin.entities.User;
import com.eyetwin.services.MatchmakingService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MatchmakingTester {
    public static void main(String[] args) {
        MatchmakingService service = new MatchmakingService();
        User me = new User();
        me.setUsername("ProPlayer");

        List<Planning> plannings = new ArrayList<>();
        
        // Mocking some plannings
        Planning p1 = new Planning();
        p1.setDescription("Pro Scrim Training");
        p1.setNeedPartner(true);
        p1.setType("Scrim");
        p1.setDate(LocalDate.now().plusDays(2));
        
        Planning p2 = new Planning();
        p2.setDescription("Fun Casual Play");
        p2.setNeedPartner(false);
        p2.setType("Training");
        
        plannings.add(p1);
        plannings.add(p2);

        System.out.println("--- Matchmaking Results ---");
        List<Planning> matches = service.findMatchesForUser(me, plannings, 5);
        for (Planning p : matches) {
            System.out.println("Suggested Match: " + p.getDescription() + " (" + p.getType() + ")");
        }
    }
}
