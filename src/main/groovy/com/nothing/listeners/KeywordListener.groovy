package com.nothing.listeners


import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.listener.message.MessageCreateListener

abstract class KeywordListener implements MessageCreateListener, CommandNameListener {
    @Override
    void onMessageCreate(MessageCreateEvent event) {
        try {
            def msgWords = event.messageContent.split("\\s+")
            if (!msgWords[0].equalsIgnoreCase(commandName())) {
                return
            }

            process(event, msgWords.drop(1) as List)
        } catch (Exception e) {
            event.channel.sendMessage("something went wrong for request '${event.messageContent}'")
            throw e
        }
    }

    abstract def process(MessageCreateEvent event, List<String> params)
}
