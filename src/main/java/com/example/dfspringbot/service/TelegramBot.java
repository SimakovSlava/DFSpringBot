package com.example.dfspringbot.service;

import com.example.dfspringbot.config.BotConfig;
import com.example.dfspringbot.model.User;
import com.example.dfspringbot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private static final String HELP_TEXT = EmojiParser.parseToUnicode(
            ":information_source: :information_source: :information_source: \n\n" +
                    "This bot is made to learn and train Spring skills based on Telegram.\n\n" +
                    "Try all the features of this bot.\n\n" +
                    ":information_source: :information_source: :information_source:");
    private final BotConfig config;

    private static boolean registered = false;

    @Autowired
    private UserRepository repository;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "start application"));
        listOfCommands.add(new BotCommand("/mydata", "view data about me"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "change settings"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

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

            if (messageText.contains("/send") && config.getAdminId() == chartId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.
                        substring(messageText.indexOf(" ")));
                var users = repository.findAll();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend);

                }
            }

            switch (messageText) {
                case "/start":
                    if (registeredUser(update.getMessage())) {
                        startCommandReceived(chartId, update.getMessage().getChat().getFirstName());
                    } else
                        sendMessage(update.getMessage().getChatId(),
                                "You are a registered user.");
                    break;
                case "/help":
                    sendMessage(chartId, HELP_TEXT);
                    break;
                case "/mydata":
                    viewUserData(update.getMessage());
                    break;
                case "/deletedata":
                    deleteMyData(update.getMessage());
                    break;
                case "/register":
                    register(chartId);
                    break;
                default:
                    sendMessage(chartId, "Sorry, the command is not supported.");
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals("YES_BUTTON")) {
                String text = "You pressed YES button";
                EditMessageText editMessageText = new EditMessageText();
                editMessageText.setChatId(String.valueOf(chatId));
                editMessageText.setText(text);
                editMessageText.setMessageId((int) messageId);

                try {
                    execute(editMessageText);
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            } else if (callBackData.equals("NO_BUTTON")) {
                String text = "You pressed NO button";
                EditMessageText editMessageText = new EditMessageText();
                editMessageText.setChatId(String.valueOf(chatId));
                editMessageText.setText(text);
                editMessageText.setMessageId((int) messageId);
                try {
                    execute(editMessageText);
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            }
        }


    }

    private void register(long chartId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chartId));
        message.setText("Do you relly want to register?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var button1 = new InlineKeyboardButton();
        button1.setText("Yes");
        button1.setCallbackData("YES_BUTTON");

        var button2 = new InlineKeyboardButton();
        button2.setText("No");
        button2.setCallbackData("NO_BUTTON");

        rowInLine.add(button1);
        rowInLine.add(button2);

        rowsInline.add(rowInLine);
        markupInLine.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInLine);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void deleteMyData(Message message) {
        if (registered == true) {
            sendMessage(message.getChatId(), "All data about you has been deleted. " +
                    "You can verify this with the /mydata command. " +
                    "Thank you for your cooperation.");
            repository.deleteById(message.getChatId());
            registered = false;
        } else {
            sendMessage(message.getChatId(), "There is no data about you.");
        }
    }

    private void viewUserData(Message message) {
        Optional<User> user = repository.findById(message.getChatId());
        if (user.isPresent()) {
            String chatId = String.valueOf(user.get().getChatId());
            String firstName = user.get().getFirstName();
            String lastName = user.get().getLastName();
            String registered = String.valueOf(user.get().getRegisteredAt());
            String userName = user.get().getUserName();

            sendMessage(Long.parseLong(chatId), EmojiParser.parseToUnicode(
                    "Data about you:\n\n" +
                            "First name: " + firstName + " :sunglasses:\n" +
                            "Last name: " + lastName + " :innocent:\n" +
                            "Id: " + chatId + " :id:\n" +
                            "Date of registration with this bot: " + registered + " :date:\n" +
                            "User name: " + userName + " :speaking_head_in_silhouette:"));
        } else {
            sendMessage(message.getChatId(), "You are not registered.");
        }
    }

    private boolean registeredUser(Message message) {
        if (repository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            repository.save(user);
            log.info("User saved: " + user);

            registered = true;

            return true;
        } else {
            registered = true;
            return false;
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", welcome to the team!" + " :blush:");
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        KeyboardRow row = new KeyboardRow();
        row.add("weather");
        row.add("get random joke");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }
}
