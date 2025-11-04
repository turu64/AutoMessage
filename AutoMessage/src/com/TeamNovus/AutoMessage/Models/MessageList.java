package com.TeamNovus.AutoMessage.Models;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import com.TeamNovus.AutoMessage.AutoMessage;
import com.TeamNovus.AutoMessage.Util.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class MessageList {
    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

    private boolean enabled = true;
    private int interval = 45;
    private long expiry = -1L;
    private boolean random = false;
    private List<Message> messages = new LinkedList<Message>();

    private transient int currentIndex = 0;

    public MessageList() {
        messages.add(new Message("First message in the list!"));
        messages.add(new Message("&aSecond message in the list with formatters!"));
        messages.add(new Message("&bThird message in the list with formatters and a \nnew line!"));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiry && expiry != -1;
    }

    public boolean isRandom() {
        return random;
    }

    public void setRandom(boolean random) {
        this.random = random;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public Message getMessage(Integer index) {
        try {
            return this.messages.get(index.intValue());
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public void addMessage(Integer index, Message message) {
        try {
            this.messages.add(index.intValue(), message);
        } catch (IndexOutOfBoundsException e) {
            this.messages.add(message);
        }
    }

    public boolean editMessage(Integer index, Message message) {
        try {
            return this.messages.set(index.intValue(), message) != null;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public boolean removeMessage(Integer index) {
        try {
            return this.messages.remove(index.intValue()) != null;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public boolean hasMessages() {
        return messages.size() > 0;
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;

        if (currentIndex >= messages.size() || currentIndex < 0) {
            this.currentIndex = 0;
        }
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void broadcast(int index) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            broadcastTo(index, player);
        }

        broadcastTo(index, Bukkit.getConsoleSender());
    }

    public void broadcastTo(int index, CommandSender to) {
        Message message = getMessage(index);

        if (message == null) {
            return;
        }

        List<String> lines = message.getMessages();
        List<String> commands = message.getCommands();
        Calendar now = Calendar.getInstance();

        if (message.isJsonFormat()) {
            for (String line : lines) {
                String processed = applyPlaceholders(line, to, now);
                sendJsonMessage(to, processed);
            }
        } else {
            for (String line : lines) {
                String processed = applyPlaceholders(line, to, now);
                to.sendMessage(Utils.translateColorCodes(processed));
            }
        }

        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceFirst("/", ""));
        }
    }

    private String applyPlaceholders(String message, CommandSender target, Calendar calendar) {
        String result = message;

        if (target instanceof Player) {
            Player player = (Player) target;
            result = result.replace("{NAME}", player.getName());
            result = result.replace("{DISPLAY_NAME}", player.getDisplayName());
            result = result.replace("{WORLD}", player.getWorld().getName());
            result = result.replace("{BIOME}", player.getLocation().getBlock().getBiome().toString());
        } else if (target instanceof ConsoleCommandSender) {
            result = result.replace("{NAME}", target.getName());
            result = result.replace("{DISPLAY_NAME}", target.getName());
            result = result.replace("{WORLD}", "UNKNOWN");
            result = result.replace("{BIOME}", "UNKNOWN");
        }

        result = result.replace("{ONLINE}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        result = result.replace("{MAX_ONLINE}", String.valueOf(Bukkit.getMaxPlayers()));
        result = result.replace("{UNIQUE_PLAYERS}", String.valueOf(Bukkit.getOfflinePlayers().length));

        result = result.replace("{YEAR}", String.valueOf(calendar.get(Calendar.YEAR)));
        result = result.replace("{MONTH}", String.valueOf(calendar.get(Calendar.MONTH)));
        result = result.replace("{WEEK_OF_MONTH}", String.valueOf(calendar.get(Calendar.WEEK_OF_MONTH)));
        result = result.replace("{WEEK_OF_YEAR}", String.valueOf(calendar.get(Calendar.WEEK_OF_YEAR)));
        result = result.replace("{DAY_OF_WEEK}", String.valueOf(calendar.get(Calendar.DAY_OF_WEEK)));
        result = result.replace("{DAY_OF_MONTH}", String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
        result = result.replace("{DAY_OF_YEAR}", String.valueOf(calendar.get(Calendar.DAY_OF_YEAR)));
        result = result.replace("{HOUR}", String.valueOf(calendar.get(Calendar.HOUR)));
        result = result.replace("{HOUR_OF_DAY}", String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)));
        result = result.replace("{MINUTE}", String.valueOf(calendar.get(Calendar.MINUTE)));
        result = result.replace("{SECOND}", String.valueOf(calendar.get(Calendar.SECOND)));

        return result;
    }

    private void sendJsonMessage(CommandSender target, String jsonPayload) {
        try {
            Component component = deserializeJson(jsonPayload);
            target.sendMessage(component);
        } catch (Exception ex) {
            if (AutoMessage.log != null) {
                AutoMessage.log.warning("Failed to parse JSON broadcast: " + ex.getMessage());
            }
            target.sendMessage(Utils.translateColorCodes(jsonPayload));
        }
    }

    private Component deserializeJson(String jsonPayload) {
        JSONObject root = new JSONObject(jsonPayload);
        translateLegacyCodes(root);
        return GSON_SERIALIZER.deserialize(root.toString());
    }

    private void translateLegacyCodes(Object node) {
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            for (String key : object.keySet()) {
                Object value = object.get(key);
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    translateLegacyCodes(value);
                } else if ("text".equalsIgnoreCase(key) && value instanceof String) {
                    object.put(key, Utils.translateColorCodes((String) value));
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) {
                Object element = array.get(i);
                if (element instanceof JSONObject || element instanceof JSONArray) {
                    translateLegacyCodes(element);
                } else if (element instanceof String) {
                    array.put(i, Utils.translateColorCodes((String) element));
                }
            }
        }
    }
}
