package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;


public class Main {

    private static final Dotenv dotenv = Dotenv.load();

    public static void main(String[] args) throws Exception {
        String token = dotenv.get("DISCORD_TOKEN");
        JDA jda = JDABuilder.createDefault(token)
                .addEventListeners(
                        new CommandHandler(dotenv),
                        new InteractionHandler(dotenv),
                        new ModalHandler(dotenv)
                )
                .build()
                .awaitReady();

        jda.updateCommands().addCommands(
                Commands.slash("createticket", "Tworzy nowy ticket"),
                Commands.slash("zamknij", "Zamyka lub usuwa ticket")
                        .addOption(OptionType.BOOLEAN, "delete", "Czy usunąć ticket", false),
                Commands.slash("claim", "Przejmuje ticket przez administratora"),
                Commands.slash("formularz", "Wyświetla formularz użytkownika")
        ).queue();
    }
}
