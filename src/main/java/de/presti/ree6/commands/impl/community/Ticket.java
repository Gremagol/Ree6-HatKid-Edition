package de.presti.ree6.commands.impl.community;

import de.presti.ree6.commands.Category;
import de.presti.ree6.commands.CommandEvent;
import de.presti.ree6.commands.interfaces.Command;
import de.presti.ree6.commands.interfaces.ICommand;
import de.presti.ree6.language.LanguageService;
import de.presti.ree6.main.Main;
import de.presti.ree6.sql.SQLSession;
import de.presti.ree6.sql.entities.Tickets;
import de.presti.ree6.utils.data.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;

/**
 * A command used to set up the Ticket system
 */
@Command(name = "tickets", description = "command.description.tickets", category = Category.COMMUNITY)
public class Ticket implements ICommand {

    /**
     * @inheritDoc
     */
    @Override
    public void onPerform(CommandEvent commandEvent) {
        if (!commandEvent.getGuild().getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
            commandEvent.reply(commandEvent.getResource("message.default.needPermission", Permission.MANAGE_WEBHOOKS.getName()));
            return;
        }

        if (!commandEvent.isSlashCommand()) {
            commandEvent.reply(commandEvent.getResource("command.perform.onlySlashSupported"));
            return;
        }

        if (!commandEvent.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            commandEvent.reply(commandEvent.getResource("message.default.insufficientPermission", Permission.ADMINISTRATOR.getName()));
            return;
        }

        OptionMapping ticketChannel = commandEvent.getOption("supportchannel");
        OptionMapping logChannel = commandEvent.getOption("logchannel");

        EmbedBuilder embedBuilder = new EmbedBuilder();

        Tickets tickets = SQLSession.getSqlConnector().getSqlWorker().getEntity(new Tickets(), "SELECT * FROM Tickets WHERE GUILDID=:gid", Map.of("gid", commandEvent.getGuild().getId()));

        if (tickets != null) {
            SQLSession.getSqlConnector().getSqlWorker().deleteEntity(tickets);
        }

        tickets = new Tickets();
        tickets.setChannelId(ticketChannel.getAsChannel().getIdLong());
        tickets.setGuildId(commandEvent.getGuild().getIdLong());

        Tickets finalTickets = tickets;

        logChannel.getAsChannel().asStandardGuildMessageChannel().createWebhook("Ticket-Log").queue(webhook -> {
            finalTickets.setLogChannelId(webhook.getIdLong());
            finalTickets.setLogChannelWebhookToken(webhook.getToken());
            commandEvent.getGuild().createCategory("Tickets").addPermissionOverride(commandEvent.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL)).queue(category1 -> {
                finalTickets.setTicketCategory(category1.getIdLong());
                SQLSession.getSqlConnector().getSqlWorker().updateEntity(finalTickets);

                MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
                messageCreateBuilder.setEmbeds(new EmbedBuilder()
                        .setTitle(LanguageService.getByGuild(commandEvent.getGuild(), "label.openTicket"))
                        .setDescription(LanguageService.getByGuild(commandEvent.getGuild(), "message.ticket.menuDescription"))
                        .setColor(0x55ff00)
                        .setThumbnail(commandEvent.getGuild().getIconUrl())
                        .setFooter(commandEvent.getGuild().getName() + " - " + Data.ADVERTISEMENT, commandEvent.getGuild().getIconUrl())
                        .build());
                messageCreateBuilder.setActionRow(Button.of(ButtonStyle.PRIMARY, "re_ticket_open", LanguageService.getByGuild(commandEvent.getGuild(), "label.openTicket"), Emoji.fromUnicode("U+1F4E9")));
                Main.getInstance().getCommandManager().sendMessage(messageCreateBuilder.build(), ticketChannel.getAsChannel().asTextChannel());
            });
        });

        embedBuilder.setDescription(LanguageService.getByGuild(commandEvent.getGuild(), "message.ticket.setupSuccess"));
        embedBuilder.setColor(new Color(0xFBD76B));

        commandEvent.reply(embedBuilder.build());
    }

    /**
     * @inheritDoc
     */
    @Override
    public CommandData getCommandData() {
        return new CommandDataImpl("tickets", LanguageService.getDefault("command.description.tickets"))
                .addOption(OptionType.CHANNEL, "supportchannel", "The channel that should have the ticket creation message.", true)
                .addOption(OptionType.CHANNEL, "logchannel", "The channel that should receive the transcripts.", true);
    }

    /**
     * @inheritDoc
     */
    @Override
    public String[] getAlias() {
        return new String[0];
    }
}
