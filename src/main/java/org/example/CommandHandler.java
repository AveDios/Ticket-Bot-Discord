package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class CommandHandler extends ListenerAdapter {
    private static final Logger LOGGER = Logger.getLogger(CommandHandler.class.getName());
    private static final String SELECT_CATEGORY_ID = "ticket_category_select";
    private static final String CREATE_TICKET_PREFIX = "create_ticket_button:";
    private final Dotenv dotenv;

    public CommandHandler(Dotenv dotenv) {
        this.dotenv = dotenv;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("createticket")) {
            StringSelectMenu select = StringSelectMenu.create(SELECT_CATEGORY_ID)
                    .setPlaceholder("Wybierz kategorię zgłoszenia")
                    .addOption("Zgłoszenie graczy", "Zgłoszenie graczy")
                    .addOption("Zapomniane hasło", "Zapomniane hasło")
                    .addOption("Błąd", "Błąd")
                    .addOption("Skarga na administratora", "Skarga na administratora")
                    .addOption("Problem z płatnością", "Problem z płatnością")
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
            if (!event.getChannel().getType().isMessage()) {
                event.reply("❌ Ta komenda może być użyta tylko w kanałach tekstowych ticketów.").setEphemeral(true).queue();
                return;
            }

            TextChannel textChannel = event.getChannel().asTextChannel();
            boolean delete = event.getOption("delete") != null && Objects.requireNonNull(event.getOption("delete")).getAsBoolean();

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

            Role everyoneRole = Objects.requireNonNull(event.getGuild()).getPublicRole();
            textChannel.getManager()
                    .putPermissionOverride(everyoneRole, null, EnumSet.of(Permission.MESSAGE_SEND))
                    .queue(
                            success -> event.getHook().sendMessage("✅ Ticket został zamknięty.").setEphemeral(true).queue(),
                            error -> event.getHook().sendMessage("❌ Wystąpił błąd przy zamykaniu ticketa: " + error.getMessage()).setEphemeral(true).queue()
                    );
        } else if (event.getName().equals("claim")) {
            if (!event.getChannel().getType().isMessage()) {
                event.reply("❌ Ta komenda może być użyta tylko w kanałach tekstowych ticketów.").setEphemeral(true).queue();
                return;
            }

            TextChannel textChannel = event.getChannel().asTextChannel();

            if (!textChannel.getName().startsWith("ticket-")) {
                event.reply("❌ Ta komenda może być użyta tylko w kanałach ticketów.").setEphemeral(true).queue();
                return;
            }

            long adminRoleId = Long.parseLong(dotenv.get("ADMIN_ROLE"));
            boolean isAdmin = event.getMember() != null &&
                    event.getMember().getRoles().stream()
                            .anyMatch(role -> role.getIdLong() == adminRoleId);

            if (!isAdmin) {
                event.reply("❌ Tylko administrator może przejąć ticket.").setEphemeral(true).queue();
                return;
            }

            event.deferReply(true).queue();

            String channelName = textChannel.getName();
            String[] nameParts = channelName.split("-", 3);
            Member ticketCreator = null;

            if (nameParts.length >= 2) {
                String creatorName = nameParts[1];
                ticketCreator = Objects.requireNonNull(event.getGuild()).getMembers().stream()
                        .filter(member -> member.getUser().getGlobalName() != null && member.getUser().getGlobalName().equalsIgnoreCase(creatorName))
                        .findFirst()
                        .orElse(null);
            }

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
        } else if (event.getName().equals("formularz")) {
            String category = "inne";
            LOGGER.info("Formularz command category: " + category);
            List<String> questions = Questions.getQuestionsForCategory(category);

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
        }
    }
}