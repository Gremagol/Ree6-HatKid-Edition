package de.presti.ree6.streamtools;

import com.github.twitch4j.common.events.TwitchEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.presti.ree6.bot.BotWorker;
import de.presti.ree6.sql.entities.StreamAction;
import de.presti.ree6.streamtools.action.IStreamAction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A Container used to store all needed Information for a StreamAction.
 */
@Slf4j
public class StreamActionContainer {

    /**
     * The Twitch Channel ID.
     */
    @Getter(AccessLevel.PUBLIC)
    String twitchChannelId;

    /**
     * The Guild.
     */
    @Getter(AccessLevel.PUBLIC)
    Guild guild;

    /**
     * The Extra Argument.
     */
    @Getter(AccessLevel.PUBLIC)
    String extraArgument;

    /**
     * The Actions.
     */
    @Getter(AccessLevel.PUBLIC)
    List<StreamActionRun> actions = new ArrayList<>();

    /**
     * Create a new StreamActionContainer.
     *
     * @param streamAction The StreamAction to create the Container for.
     */
    public StreamActionContainer(StreamAction streamAction) {
        twitchChannelId = streamAction.getIntegration().getChannelId();
        guild = BotWorker.getShardManager().getGuildById(streamAction.getGuildId());
        extraArgument = streamAction.getArgument();

        if (streamAction.getActions() != null && streamAction.getActions().isJsonArray()) {
            JsonArray jsonArray = streamAction.getActions().getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                if (jsonObject.has("action") &&
                        jsonObject.has("value") &&
                        jsonObject.get("action").isJsonPrimitive() &&
                        jsonObject.get("value").isJsonPrimitive()) {
                    String action = jsonObject.getAsJsonPrimitive("action").getAsString();
                    String value = jsonObject.getAsJsonPrimitive("value").getAsString();
                    String[] args = value.split(" ");

                    Class<? extends IStreamAction> actionClass = StreamActionContainerCreator.getAction(action);
                    if (actionClass != null) {
                        try {
                            IStreamAction streamAction1 = actionClass.getConstructor().newInstance();
                            actions.add(new StreamActionRun(streamAction1, args));
                        } catch (Exception e) {
                            log.error("Couldn't parse Stream-action!", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Run all Actions.
     *
     * @param twitchEvent The related Twitch event.
     * @param userInput The User Input.
     */
    public void runActions(TwitchEvent twitchEvent, String userInput) {
        actions.forEach((run) -> {

            String[] args = run.getArguments();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.contains("%user_input%")) {
                    args[i] = arg.replace("%user_input%", userInput);
                }
            }

            run.getAction().runAction(guild, twitchEvent, args);
        });
    }
}
