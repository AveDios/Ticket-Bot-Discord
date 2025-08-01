package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class ModalHandler extends ListenerAdapter {
    private static final Logger LOGGER = Logger.getLogger(ModalHandler.class.getName());
    private static final String FORM_BUTTON_ID = "form_button";
    private final Dotenv dotenv;

    public ModalHandler(Dotenv dotenv) {
        this.dotenv = dotenv;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().startsWith("formularz-modal")) {
            // Extract category from modal ID
            String[] modalParts = event.getModalId().split(":", 2);
            String category = modalParts.length > 1 ? modalParts[1] : "inne";
            LOGGER.info("Modal category: " + category);

            // Get questions for the category
            List<String> questions = Questions.getQuestionsForCategory(category);

            // Collect answers dynamically
            StringBuilder response = new StringBuilder("Dziękuję za wypełnienie formularza!\n");
            for (int i = 0; i < questions.size(); i++) {
                String answer = Objects.requireNonNull(event.getValue("field" + i)).getAsString();
                response.append(questions.get(i)).append(": ").append(answer).append("\n");
            }

            // Disable the form button in the ticket channel
            if (event.getChannel().getType().isMessage() && event.getChannel().getName().startsWith("ticket-")) {
                TextChannel textChannel = event.getChannel().asTextChannel();
                textChannel.getHistory().retrievePast(10).queue(messages -> {
                    for (Message message : messages) {
                        if (!message.getComponents().isEmpty() && message.getComponents().getFirst().getComponents().stream()
                                .anyMatch(component -> component instanceof Button && FORM_BUTTON_ID.equals(((Button) component).getId()))) {
                            Button disabledButton = Button.primary(FORM_BUTTON_ID, "Wypełnij formularz").withDisabled(true);
                            message.editMessageComponents(ActionRow.of(disabledButton)).queue();
                            LOGGER.info("Disabled form button in channel: " + textChannel.getName());
                            break;
                        }
                    }
                });
            }

            event.reply(response.toString()).setEphemeral(true).queue();
        }
    }
}