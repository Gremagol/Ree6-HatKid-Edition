package de.presti.ree6.commands.impl.mod;

import de.presti.ree6.commands.Category;
import de.presti.ree6.commands.CommandEvent;
import de.presti.ree6.commands.interfaces.Command;
import de.presti.ree6.commands.interfaces.ICommand;
import de.presti.ree6.language.LanguageService;
import de.presti.ree6.sql.SQLSession;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import java.nio.charset.StandardCharsets;

/**
 * A Command to set a Role for a Level.
 */
@Command(name = "levelrole", description = "command.description.levelrole", category = Category.MOD)
public class LevelRole implements ICommand {

    /**
     * @param commandEvent the Event, with every needed data.
     */
    @Override
    public void onPerform(CommandEvent commandEvent) {
        if (!commandEvent.isSlashCommand()) {
            commandEvent.reply(commandEvent.getResource("command.perform.onlySlashSupported"));
            return;
        }

        if (!commandEvent.getMember().hasPermission(Permission.MANAGE_ROLES)) {
            commandEvent.reply(commandEvent.getResource("message.default.insufficientPermission", Permission.MANAGE_ROLES.getName()));
            return;
        }

        String command = commandEvent.getSlashCommandInteractionEvent().getSubcommandName();
        OptionMapping voiceMapping = commandEvent.getSlashCommandInteractionEvent().getOption("voice");
        OptionMapping levelMapping = commandEvent.getSlashCommandInteractionEvent().getOption("level");
        OptionMapping roleMapping = commandEvent.getSlashCommandInteractionEvent().getOption("role");

        if (command == null || (!command.equalsIgnoreCase("list") && (voiceMapping == null || levelMapping == null || roleMapping == null))) {
            commandEvent.reply(commandEvent.getResource("message.default.invalidQuery"));
            return;
        }

        Role role = roleMapping.getAsRole();
        long level = levelMapping.getAsLong();

        switch (command) {
            case "add" -> {
                if (voiceMapping.getAsBoolean()) {
                    SQLSession.getSqlConnector().getSqlWorker().addVoiceLevelReward(commandEvent.getGuild().getId(), role.getId(), level);
                } else {
                    SQLSession.getSqlConnector().getSqlWorker().addChatLevelReward(commandEvent.getGuild().getId(), role.getId(), level);
                }
                commandEvent.reply(commandEvent.getResource("message.levelrole.added", role.getName(), level));
            }
            case "remove" -> {
                if (voiceMapping.getAsBoolean()) {
                    SQLSession.getSqlConnector().getSqlWorker().removeVoiceLevelReward(commandEvent.getGuild().getId(), role.getId(), level);
                } else {
                    SQLSession.getSqlConnector().getSqlWorker().removeChatLevelReward(commandEvent.getGuild().getId(), role.getId(), level);
                }
                commandEvent.reply(commandEvent.getResource("message.levelrole.removed", role.getName(), level));
            }
            case "list" -> {
                MessageCreateBuilder createBuilder = new MessageCreateBuilder();
                StringBuilder voiceStringBuilder = new StringBuilder();
                StringBuilder chatStringBuilder = new StringBuilder();
                createBuilder.setContent(commandEvent.getResource("message.levelrole.list"));

                SQLSession.getSqlConnector().getSqlWorker().getChatLevelRewards(commandEvent.getGuild().getId())
                        .forEach((level1, role1) -> voiceStringBuilder.append(level1).append(" -> ").append(role1));
                SQLSession.getSqlConnector().getSqlWorker().getChatLevelRewards(commandEvent.getGuild().getId())
                        .forEach((level1, role1) -> chatStringBuilder.append(level1).append(" -> ").append(role1));

                createBuilder.addFiles(FileUpload.fromData(voiceStringBuilder.toString().getBytes(StandardCharsets.UTF_8), "voice.txt"),
                        FileUpload.fromData(chatStringBuilder.toString().getBytes(StandardCharsets.UTF_8), "chat.txt"));

                commandEvent.reply(createBuilder.build());
            }

            default -> commandEvent.reply(commandEvent.getResource("message.default.invalidOption"));
        }
    }

    /**
     * @return
     */
    @Override
    public CommandData getCommandData() {
        return new CommandDataImpl("levelrole",
                LanguageService.getDefault("command.description.levelrole"))
                .addSubcommands(new SubcommandData("add", "Add a new Level-Role")
                                .addOptions(new OptionData(OptionType.BOOLEAN, "voice", "True if the Level-Role should be applied to the Voice-Level and false if it should be applied to the Text-Level").setRequired(true),
                                        new OptionData(OptionType.NUMBER, "level", "The level that needs to be reached to get the role.").setRequired(true).setMinValue(1),
                                        new OptionData(OptionType.ROLE, "role", "The name of the already created action").setRequired(true)),
                        new SubcommandData("remove", "Remove a Level-Role")
                                .addOptions(new OptionData(OptionType.BOOLEAN, "voice", "True if the Level-Role should be applied to the Voice-Level and false if it should be applied to the Text-Level").setRequired(true),
                                        new OptionData(OptionType.NUMBER, "level", "The level that needs to be reached to get the role.").setRequired(true).setMinValue(1),
                                        new OptionData(OptionType.ROLE, "role", "The name of the already created action").setRequired(true)),
                        new SubcommandData("list", "List all Level-Roles"));
    }

    /**
     * @return
     */
    @Override
    public String[] getAlias() {
        return new String[0];
    }
}
