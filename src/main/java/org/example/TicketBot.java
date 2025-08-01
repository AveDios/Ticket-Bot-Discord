/*
package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;

public class TicketBot extends ListenerAdapter {

    static final Dotenv dotenv = Dotenv.load();
    private static final String SELECT_CATEGORY_ID = "ticket_category_select";
    private static final String CREATE_TICKET_PREFIX = "create_ticket_button:";
    private static final String FORM_BUTTON_ID = "form_button";
    private static Button fromButton;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("createticket")) {
            StringSelectMenu select = StringSelectMenu.create(SELECT_CATEGORY_ID)
                    .setPlaceholder("Wybierz kategorię zgłoszenia")
                    .addOption("Zgłoszenie graczy", "Zgłoszenie graczy")
                    .addOption("Zapomniane hasło", "Zapomniane hasło")
                    .addOption("Błąd", "Błąd")
                    .addOption("Skarga na administratora", "Skarga na administratora")
                    .addOption("Problem z płatnością", "Problem z łatnością")
                    .addOption("Odwołanie od bana", "Odwołanie od bana")
                    .addOption("Backup", "Backup")
                    .addOption("Inne", "Inne")
                    .build();

            Button createButton = Button.secondary(CREATE_TICKET_PREFIX + "none", "\uD83C\uDFAB Stwórz ticket")
                    .withDisabled(true);

            event.reply("🎟️ Wybierz kategorię zgłoszenia, aby aktywować przycisk tworzenia ticketu.")
                    .setComponents(ActionRow.of(select), ActionRow.of(createButton))
                    .setEphemeral(true)
                    .queue();
        } else if (event.getName().equals("zamknij")) {
            // Check if the command is used in a text channel
            if (!event.getChannel().getType().isMessage()) {
                event.reply("❌ Ta komenda może być użyta tylko w kanałach tekstowych ticketów.").setEphemeral(true).queue();
                return;
            }

            TextChannel textChannel = event.getChannel().asTextChannel();
            boolean delete = event.getOption("delete") != null && Objects.requireNonNull(event.getOption("delete")).getAsBoolean();

            // Check if the channel is a ticket channel (e.g., starts with "ticket-")
            if (!textChannel.getName().startsWith("ticket-")) {
                event.reply("❌ Ta komenda może być użyta tylko w kanałach ticketów.").setEphemeral(true).queue();
                return;
            }

            if (delete) {
                long adminRoleId = Long.parseLong(dotenv.get("ADMIN_ROLE"));
                boolean isAdmin = event.getMember() != null &&
                        event.getMember().getRoles().stream()
                                .anyMatch(role -> role.getIdLong() == adminRoleId);

                if (!isAdmin) {
                    event.reply("❌ Tylko administrator może usuwać tickety.").setEphemeral(true).queue();
                    return;
                }

                textChannel.delete().queue(
                        success -> event.reply("🗑️ Ticket został usunięty.").setEphemeral(true).queue(),
                        error -> event.reply("❌ Nie udało się usunąć ticketa: " + error.getMessage()).setEphemeral(true).queue()
                );
                return;
            }

            event.deferReply(true).queue();

            // Lock the channel by denying MESSAGE_SEND to @everyone
            Role everyoneRole = Objects.requireNonNull(event.getGuild()).getPublicRole();
            textChannel.getManager()
                    .putPermissionOverride(everyoneRole, null, EnumSet.of(Permission.MESSAGE_SEND))
                    .queue(
                            success -> event.getHook().sendMessage("✅ Ticket został zamknięty.").setEphemeral(true).queue(),
                            error -> event.getHook().sendMessage("❌ Wystąpił błąd przy zamykaniu ticketa: " + error.getMessage()).setEphemeral(true).queue()
                    );
        } else if (event.getName().equals("claim")) {
            // Check if the command is used in a text channel
            if (!event.getChannel().getType().isMessage()) {
                event.reply("❌ Ta komenda może być użyta tylko w kanałach tekstowych ticketów.").setEphemeral(true).queue();
                return;
            }

            TextChannel textChannel = event.getChannel().asTextChannel();

            // Check if the channel is a ticket channel
            if (!textChannel.getName().startsWith("ticket-")) {
                event.reply("❌ Ta komenda może być użyta tylko w kanałach ticketów.").setEphemeral(true).queue();
                return;
            }

            // Check if the user has the admin role
            long adminRoleId = Long.parseLong(dotenv.get("ADMIN_ROLE"));
            boolean isAdmin = event.getMember() != null &&
                    event.getMember().getRoles().stream()
                            .anyMatch(role -> role.getIdLong() == adminRoleId);

            if (!isAdmin) {
                event.reply("❌ Tylko administrator może przejąć ticket.").setEphemeral(true).queue();
                return;
            }

            event.deferReply(true).queue();

            // Try to extract creator's username from channel name (e.g., ticket-username-category)
            String channelName = textChannel.getName();
            String[] nameParts = channelName.split("-", 3);
            Member ticketCreator = null;

            if (nameParts.length >= 2) {
                String creatorName = nameParts[1]; // username part from ticket-username-category
                ticketCreator = Objects.requireNonNull(event.getGuild()).getMembers().stream()
                        .filter(member -> member.getUser().getGlobalName() != null && member.getUser().getGlobalName().equalsIgnoreCase(creatorName))
                        .findFirst()
                        .orElse(null);
            }

            // Fallback to permission overrides if creator not found via channel name
            if (ticketCreator == null) {
                ticketCreator = textChannel.getPermissionOverrides().stream()
                        .map(PermissionOverride::getMember)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }

            if (ticketCreator == null) {
                event.getHook().sendMessage("❌ Nie można znaleźć twórcy ticketa.").setEphemeral(true).queue();
                return;
            }

            // Update channel permissions: allow claiming admin and ticket creator to send messages
            textChannel.getManager()
                    .clearOverridesAdded()
                    .putPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                    .putPermissionOverride(ticketCreator, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                    .putPermissionOverride(Objects.requireNonNull(Objects.requireNonNull(event.getGuild()).getRoleById(adminRoleId)), EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .putPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                    .queue(
                            success -> {
                                textChannel.sendMessage("✅ Ticket został przejęty przez " + event.getUser().getAsMention() + ".")
                                        .queue();
                                event.getHook().sendMessage("✅ Pomyślnie przejąłeś ticket.").setEphemeral(true).queue();
                            },
                            error -> event.getHook().sendMessage("❌ Wystąpił błąd przy przejmowaniu ticketa: " + error.getMessage()).setEphemeral(true).queue()
                    );
        } */
/*else if (event.getName().equals("formularz")) {
            // Tworzenie pól tekstowych
            TextInput imie = TextInput.create("imie", "Imię", TextInputStyle.SHORT)
                    .setPlaceholder("Wpisz swoje imię")
                    .setMinLength(2)
                    .setMaxLength(50)
                    .build();

            TextInput opis = TextInput.create("opis", "Opis", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Wpisz krótki opis")
                    .setMinLength(10)
                    .setMaxLength(500)
                    .build();

            // Tworzenie modala
            Modal modal = Modal.create("formularz-modal", "Formularz użytkownika")
                    .addActionRow(imie)
                    .addActionRow(opis)
                    .build();

            // Wysłanie modala jako odpowiedzi
            event.replyModal(modal).queue();
        }*//*

    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("formularz-modal")) {
            String nick = Objects.requireNonNull(event.getValue("nick")).getAsString();
            String description = Objects.requireNonNull(event.getValue("description")).getAsString();
            String mode = Objects.requireNonNull(event.getValue("mode")).getAsString();

            // Check if the modal was triggered in a ticket channel
            if (event.getChannel().getType().isMessage() && event.getChannel().getName().startsWith("ticket-")) {
                TextChannel textChannel = event.getChannel().asTextChannel();
                // Fetch the last few messages to find the one with the form button
                textChannel.getHistory().retrievePast(10).queue(messages -> {
                    for (Message message : messages) {
                        if (!message.getComponents().isEmpty() && message.getComponents().get(0).getComponents().stream()
                                .anyMatch(component -> component instanceof Button && FORM_BUTTON_ID.equals(((Button) component).getId()))) {
                            Button disabledButton = Button.primary(FORM_BUTTON_ID, "Wypełnij formularz").withDisabled(true);
                            message.editMessageComponents(ActionRow.of(disabledButton)).queue();
                            break;
                        }
                    }
                });
            }

            event.reply("Dziękuję za wypełnienie formularza!\nNick: " + nick + "\nOpis: " + description + "\nTryb: " + mode)
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals(SELECT_CATEGORY_ID)) return;

        String selectedCategory = event.getValues().getFirst();

        Button updatedButton = Button.success(CREATE_TICKET_PREFIX + selectedCategory, "\uD83C\uDFAB Stwórz ticket")
                .withDisabled(false);

        StringSelectMenu selectMenu = event.getComponent();

        event.editComponents(ActionRow.of(selectMenu), ActionRow.of(updatedButton)).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().equals(FORM_BUTTON_ID)) {
            // Check if the button is used in a ticket channel
            if (!event.getChannel().getType().isMessage() || !event.getChannel().getName().startsWith("ticket-")) {
                event.reply("❌ Ten przycisk może być użyty tylko w kanałach ticketów.").setEphemeral(true).queue();
                return;
            }

            // Create the same modal as in the /formularz command
            TextInput nick = TextInput.create("nick", "Nick", TextInputStyle.SHORT)
                    .setPlaceholder("Wpisz swój nick w minecraft")
                    .setMinLength(2)
                    .setMaxLength(50)
                    .build();

            TextInput description = TextInput.create("description", "Opis sytuacji", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Wpisz krótki opis")
                    .setMinLength(10)
                    .setMaxLength(500)
                    .build();

            TextInput mode = TextInput.create("mode", "Tryb gry", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Wpisz tryb")
                    .setMinLength(2)
                    .setMaxLength(100)
                    .build();

            Modal modal = Modal.create("formularz-modal", "Formularz użytkownika")
                    .addActionRow(nick)
                    .addActionRow(description)
                    .addActionRow(mode)
                    .build();

            event.replyModal(modal).queue();
            return;
        }

        if (!event.getComponentId().startsWith(CREATE_TICKET_PREFIX)) return;

        String[] parts = event.getComponentId().split(":", 2);
        if (parts.length < 2 || parts[1].equals("none")) {
            event.reply("❌ Najpierw wybierz kategorię z listy!").setEphemeral(true).queue();
            return;
        }
        String category = parts[1];

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

        String channelName = "ticket-" + event.getUser().getGlobalName() + "-" + category.toLowerCase().replace(" ", "-");

        // Create the text channel
        ticketCategory.createTextChannel(channelName)
                .addPermissionOverride(Objects.requireNonNull(event.getMember()), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
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
                        },
                        throwable -> event.reply("❌ Wystąpił błąd podczas tworzenia ticketa: " + throwable.getMessage()).setEphemeral(true).queue()
                );
    }
}*/
