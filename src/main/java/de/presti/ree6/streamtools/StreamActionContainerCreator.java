package de.presti.ree6.streamtools;

import de.presti.ree6.sql.SQLSession;
import de.presti.ree6.sql.entities.StreamAction;
import de.presti.ree6.streamtools.action.IStreamAction;
import de.presti.ree6.streamtools.action.StreamActionInfo;
import de.presti.ree6.streamtools.action.impl.*;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Utility class used to create StreamActionContainers.
 */
public class StreamActionContainerCreator {

    /**
     * A Cache for all StreamActions.
     */
    private static HashMap<String, Class<? extends IStreamAction>> cachedActions = new HashMap<>();

    /**
     * Load all actions.
     */
    public static void loadAll() {
        cachedActions.put("voice-join", VoiceJoinStreamAction.class);
        cachedActions.put("voice-leave", VoiceLeaveStreamAction.class);
        cachedActions.put("play-url", PlayUrlStreamAction.class);
        cachedActions.put("play-tts", PlayTTSStreamAction.class);
        cachedActions.put("say", SayStreamAction.class);
    }

    /**
     * Get the Class of a StreamAction.
     * @param action The Name of the Action.
     * @return The Class of the Action.
     */
    public static Class<? extends IStreamAction> getAction(String action) {
        action = action.trim().toLowerCase();

        if (cachedActions.containsKey(action))
            return cachedActions.get(action);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addClassLoaders(ClasspathHelper.staticClassLoader());
        Reflections reflections = new Reflections(configurationBuilder);
        Set<Class<? extends IStreamAction>> classes = reflections.getSubTypesOf(IStreamAction.class);

        for (Class<? extends IStreamAction> aClass : classes) {
            if (aClass.isAnnotationPresent(StreamActionInfo.class) && aClass.getAnnotation(StreamActionInfo.class).name().trim().equalsIgnoreCase(action)) {
                if (!cachedActions.containsKey(action)) {
                    cachedActions.put(action, aClass);
                }

                return aClass;
            }
        }

        return null;
    }

    /**
     * Get all StreamActionContainers.
     *
     * @param listenerId The Listener to get the Containers for.
     * @return A List of StreamActionContainers.
     */
    public static List<StreamActionContainer> getContainers(int listenerId) {
        return SQLSession.getSqlConnector().getSqlWorker()
                .getEntityList(new StreamAction(), "SELECT * FROM StreamActions WHERE listener = :listener", Map.of("listener", listenerId))
                .stream().map(StreamActionContainer::new).toList();
    }

}
