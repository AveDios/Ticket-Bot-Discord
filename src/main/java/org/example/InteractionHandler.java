package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

public class InteractionHandler extends ListenerAdapter {
    private static final Logger LOGGER = Logger.getLogger(InteractionHandler.class.getName());
    private static final String SELECT_CATEGORY_ID = "ticket_category_select";
    private static final String CREATE_TICKET_PREFIX = "create_ticket_button:";
    private static final String FORM_BUTTON_ID = "form_button";
    private final Dotenv dotenv;
    private final TicketManager ticketManager;

    public InteractionHandler(Dotenv dotenv) {
        this.dotenv = dotenv;
        this.ticketManager = new TicketManager(dotenv);
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals(SELECT_CATEGORY_ID)) return;

        String selectedCategory = event.getValues().getFirst();
        LOGGER.info("Selected category: " + selectedCategory);

        Button updatedButton = Button.success(CREATE_TICKET_PREFIX + selectedCategory, "\uD83C\uDFAB Stwórz ticket")
                .withDisabled(false);

        StringSelectMenu selectMenu = event.getComponent();

        event.editComponents(ActionRow.of(selectMenu), ActionRow.of(updatedButton)).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().equals(FORM_BUTTON_ID)) {
            if (!event.getChannel().getType().isMessage() || !event.getChannel().getName().startsWith("ticket-")) {
                event.reply("❌ Ten przycisk może być użyty tylko w kanałach ticketów.").setEphemeral(true).queue();
                return;
            }

            // Extract category from channel name (ticket-username-category)
            String channelName = event.getChannel().getName();
            String[] parts = channelName.split("-", 3);
            String category = parts.length >= 3 ? parts[2] : "inne";
            LOGGER.info("Extracted category from channel: " + category);

            // Get questions for the category
            List<String> questions = Questions.getQuestionsForCategory(category);

            // Create dynamic modal fields
            Modal.Builder modalBuilder = Modal.create("formularz-modal:" + category, "Formularz użytkownika");
            for (int i = 0; i < questions.size(); i++) {
                String question = questions.get(i);
                TextInput input = TextInput.create("field" + i, question, i == 1 && !question.equals("Jaki tryb?") ? TextInputStyle.PARAGRAPH : TextInputStyle.SHORT)
                        .setPlaceholder("Wpisz odpowiedź")
                        .setMinLength(2)
                        .setMaxLength(i == 1 && !question.equals("Jaki tryb?") ? 500 : 50)
                        .build();
                modalBuilder.addActionRow(input);
            }

            event.replyModal(modalBuilder.build()).queue();
            return;
        }

        if (!event.getComponentId().startsWith(CREATE_TICKET_PREFIX)) return;

        String[] parts = event.getComponentId().split(":", 2);
        if (parts.length < 2 || parts[1].equals("none")) {
            event.reply("❌ Najpierw wybierz kategorię z listy!").setEphemeral(true).queue();
            return;
        }
        String category = parts[1];
        LOGGER.info("Creating ticket with category: " + category);

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ Ta komenda może być użyta tylko na serwerze.").setEphemeral(true).queue();
            return;
        }

        long ticketCategoryId = Long.parseLong(dotenv.get("TICKET_CATEGORY"));
        Category ticketCategory = guild.getCategoryById(ticketCategoryId);
        if (ticketCategory == null) {
            event.reply("❌ Nie znaleziono kategorii ticketów. Sprawdź konfigurację.").setEphemeral(true).queue();
            return;
        }

        String channelName = "ticket-" + event.getUser().getGlobalName() + "-" + Questions.normalizeCategory(category);
        LOGGER.info("Creating channel: " + channelName);

        ticketManager.createTicket(ticketCategory, channelName, event.getMember(), category, event);
    }
}