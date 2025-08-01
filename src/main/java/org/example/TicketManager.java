package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.Permission;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class TicketManager {
    private static final Logger LOGGER = Logger.getLogger(TicketManager.class.getName());
    private static final String FORM_BUTTON_ID = "form_button";
    private final Dotenv dotenv;

    public TicketManager(Dotenv dotenv) {
        this.dotenv = dotenv;
    }

    public void createTicket(Category ticketCategory, String channelName, Member member, String category, ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        ticketCategory.createTextChannel(channelName)
                .addPermissionOverride(Objects.requireNonNull(member), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .addPermissionOverride(Objects.requireNonNull(guild.getRoleById(Long.parseLong(dotenv.get("ADMIN_ROLE")))), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                .queue(
                        textChannel -> {
                            Button formButton = Button.primary(FORM_BUTTON_ID, "Wypełnij formularz");
                            textChannel.sendMessage(MessageCreateData.fromContent(
                                    event.getUser().getAsMention() + " zgłosił ticket w kategorii: **" + category + "**\nOpisz swój problem poniżej:"
                            )).setComponents(ActionRow.of(formButton)).queue();
                            event.reply("✅ Ticket utworzony! Możesz go zobaczyć tutaj: " + textChannel.getAsMention())
                                    .setEphemeral(true)
                                    .queue();

                            event.getHook().editOriginalComponents(
                                    event.getMessage().getComponents().stream()
                                            .map(component -> ((ActionRow) component).asDisabled())
                                            .collect(Collectors.toList())
                            ).queue();
                            LOGGER.info("Created ticket channel: " + channelName);
                        },
                        throwable -> {
                            event.reply("❌ Wystąpił błąd podczas tworzenia ticketa: " + throwable.getMessage()).setEphemeral(true).queue();
                            LOGGER.severe("Failed to create ticket: " + throwable.getMessage());
                        }
                );
    }
}