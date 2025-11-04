package com.TeamNovus.AutoMessage.Models;

import java.util.LinkedList;

import org.json.JSONObject;

public class Message {
    private static final String SPLIT_REGEX = "(?<!\\\\)\\\\n";
    private static final String REPLACE_REGEX = "\\\\\\\\n";
    private static final String REPLACEMENT = "\\\\n";

    private String raw;
    private boolean isJson;

    public Message(String raw) {
        this.raw = raw;
        this.isJson = isJsonMessage(raw);
    }

    public String getMessage() {
        return raw;
    }

    public Message setMessage(String raw) {
        this.raw = raw;
        this.isJson = isJsonMessage(raw);
        return this;
    }

    public boolean isJsonFormat() {
        return isJson;
    }

    public boolean isJsonMessage(String msg) {
        try {
            new JSONObject(msg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public LinkedList<String> getMessages() {
        LinkedList<String> messages = new LinkedList<String>();

        if (isJson) {
            String msg = raw.replaceAll(REPLACE_REGEX, REPLACEMENT);
            msg = msg.replaceAll("%n", " ");
            messages.add(msg);
        } else {
            for (String line : raw.split(SPLIT_REGEX)) {
                if (!line.startsWith("/")) {
                    String processed = line.replaceAll(REPLACE_REGEX, REPLACEMENT);
                    processed = processed.replaceAll("%n", " ");
                    messages.add(processed);
                }
            }
        }

        return messages;
    }

    public LinkedList<String> getCommands() {
        LinkedList<String> commands = new LinkedList<String>();

        for (String line : raw.split(SPLIT_REGEX)) {
            if (line.startsWith("/")) {
                String processed = line.replaceAll(REPLACE_REGEX, REPLACEMENT);
                commands.add(processed);
            }
        }

        return commands;
    }
}
