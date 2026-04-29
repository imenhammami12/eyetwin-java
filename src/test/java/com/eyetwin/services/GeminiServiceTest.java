package com.eyetwin.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;

public class GeminiServiceTest {

    @Test
    public void testGenerateDescription() throws Exception {
        GeminiService service = new GeminiService();
        String result = service.generateTournamentDescription("Coupe des Champions 2026", "ESPORT", 1000.0)
                .get(15, TimeUnit.SECONDS);

        System.out.println("AI Description Generated: " + result);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.contains("Erreur"));
    }
}
