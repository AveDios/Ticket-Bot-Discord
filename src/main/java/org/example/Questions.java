package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Questions {
    private static final Logger LOGGER = Logger.getLogger(Questions.class.getName());
    public static final Map<String, List<String>> TICKET_QUESTIONS = new HashMap<>();

    static {
        TICKET_QUESTIONS.put("inne", List.of("Jaki masz nick?", "Jaki tryb?", "Czego dotyczy zgłoszenie?"));
        TICKET_QUESTIONS.put("backup", List.of("Jaki masz nick?", "Jak/od kogo zginąłeś?", "Czy masz clipa na dane zajście?"));
        TICKET_QUESTIONS.put("zapomniane-hasło", List.of("Jaki masz nick?", "Czy masz premkę?", "Czy masz dowód, że to twoje konto?"));
        TICKET_QUESTIONS.put("problem-z-łatnością", List.of("Jaki masz nick?", "Na jakim trybie coś kupiłeś?", "Czy masz potwierdzenie płatności?"));
        TICKET_QUESTIONS.put("odwołanie-od-bana", List.of("Jaki masz nick?", "Za co masz bana?", "Od kogo?"));
        TICKET_QUESTIONS.put("skarga-na-administratora", List.of("Jaki masz nick?", "Kogo zgłaszasz?", "Za co?", "Czy masz na to dowód?"));
        TICKET_QUESTIONS.put("zgłoszenie-graczy", List.of("Jaki tryb?", "Jaki masz nick?", "Czy masz dowód na zgłoszenie gracza?"));
        TICKET_QUESTIONS.put("błąd", List.of("Jaki masz nick?", "Na czym polega błąd?", "Na jakim trybie?"));

        // Log available categories for debugging
        LOGGER.info("Loaded ticket categories: " + TICKET_QUESTIONS.keySet());
    }

    public static List<String> getQuestionsForCategory(String category) {
        String normalizedCategory = normalizeCategory(category);
        List<String> questions = TICKET_QUESTIONS.getOrDefault(normalizedCategory, List.of("Jaki masz nick?", "Opisz problem:"));
        LOGGER.info("Category: " + normalizedCategory + ", Questions: " + questions);
        return questions;
    }

    public static String normalizeCategory(String category) {
        if (category == null) return "inne";
        return category.toLowerCase().replace(" ", "-");
    }
}