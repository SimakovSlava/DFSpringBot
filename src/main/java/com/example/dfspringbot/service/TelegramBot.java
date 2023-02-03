package com.example.dfspringbot.service;

import com.example.dfspringbot.config.BotConfig;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chartId = update.getMessage().getChatId();


            switch (messageText) {
                case "/start":
                    startCommandReceived(chartId, update.getMessage().getChat().getFirstName());
               break;
                default:
                    sendMessage(chartId, "Sorry, the command is not supported.");
            }
        }
    }

    private void startCommandReceived(long chatId , String name) {
        String answer = "Hi, " + name + ", welcome to the team!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try{
            execute(message);
        } catch (TelegramApiException e) {
           // throw new RuntimeException(e);
        }
    }
}
