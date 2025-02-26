package de.presti.ree6.streamtools.action.impl;

import com.github.twitch4j.common.events.TwitchEvent;
import de.presti.ree6.main.Main;
import de.presti.ree6.streamtools.action.StreamActionInfo;
import de.presti.ree6.streamtools.action.IStreamAction;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;

/**
 * StreamAction used to say a message.
 */
@NoArgsConstructor
@StreamActionInfo(name = "Say", command = "say", description = "Says a message.", introduced = "2.2.0")
public class SayStreamAction implements IStreamAction {

    /**
     * @inheritDoc
     */
    @Override
    public boolean runAction(@NotNull Guild guild, TwitchEvent twitchEvent, String[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return false;
        }

        String channelId = arguments[0];

        StringBuilder message = new StringBuilder();

        for (String arg : arguments) {
            message.append(arg).append(" ");
        }

        TextChannel textChannel = guild.getTextChannelById(channelId);

        if (textChannel == null) {
            return false;
        }

        Main.getInstance().getCommandManager().sendMessage(message.substring(channelId.length()), textChannel);
        return true;
    }
}
