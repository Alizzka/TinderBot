package com.javarush.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.GetMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.reactions.SetMessageReaction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonCommands;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MultiSessionTelegramBot extends TelegramLongPollingBot {

    private String name;
    private String token;

    private ThreadLocal<Update> updateEvent = new ThreadLocal<>();

    public MultiSessionTelegramBot(String name, String token) {
        this.name = name;
        this.token = token;
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public final void onUpdateReceived(Update updateEvent) {
        try {
            this.updateEvent.set(updateEvent);
            onUpdateEventReceived(this.updateEvent.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void onUpdateEventReceived(Update updateEvent) throws Exception {
        //do nothing
    }

    /**
     * Метод возвращает ID текущего Telegram-чата
     */
    public Long getCurrentChatId() {
        if (updateEvent.get().hasMessage()) {
            return updateEvent.get().getMessage().getFrom().getId();
        }

        if (updateEvent.get().hasCallbackQuery()) {
            return updateEvent.get().getCallbackQuery().getFrom().getId();
        }

        return null;
    }

    /**
     * Метод возвращает текст из последнего сообщения Telegram-чата
     */
    public String getMessageText() {
        return updateEvent.get().hasMessage() ? updateEvent.get().getMessage().getText() : "";
    }

    public  boolean isMessageCommand() {
        return updateEvent.get().hasMessage() && updateEvent.get().getMessage().isCommand();
    }


    /**
     * Метод возвращает код нажатой кнопки (buttonKey).
     * Речь идет о кнопках, которые были добавлены к сообщению.
     */
    public String getCallbackQueryButtonKey() {
        return updateEvent.get().hasCallbackQuery() ? updateEvent.get().getCallbackQuery().getData() : "";
    }

    /**
     * Метод отправляет в чат ТЕКСТ (текстовое сообщение).
     * Поддерживается markdown-разметка.
     */
    public Message sendTextMessage(String text) {
        long underscoreCount = text != null ? text.chars().filter(c -> c == '_').count() : 0;
        if (underscoreCount % 2 == 0) {
            SendMessage command = createApiSendMessageCommand(String.valueOf(text));
            return executeTelegramApiMethod(command);
        } else {
            var message = "Строка '%s' является невалидной с точки зрения markdown. Воспользуйтесь методом sendHtmlMessage()".formatted(text);
            System.out.println(message);
            return sendHtmlMessage(message);
        }
    }

    /**
     * Метод отправляет в чат HTML (текстовое сообщение).
     * Поддерживается html-разметка.
     */
    public Message sendHtmlMessage(String text) {
        SendMessage message = new SendMessage();
        message.setText(new String(text.getBytes(), StandardCharsets.UTF_8));
        message.setParseMode("html");
        message.setChatId(getCurrentChatId());
        return executeTelegramApiMethod(message);
    }

    /**
     * Метод отправляет в чат ФОТО (Картинку).
     * Картинка задается ключем - photoKey.
     * Все картинки содержатся в папке resources/images
     */
    public Message sendPhotoMessage(String photoKey) {
        SendPhoto command = createApiPhotoMessageCommand(photoKey, null);
        return executeTelegramApiMethod(command);
    }

    /**
     * Метод отправляет в чат ФОТО (Картинку) и ТЕКСТ.
     * Картинка задается ключем - photoKey.
     * Все картинки содержатся в папке resources/images
     */
    public Message sendPhotoTextMessage(String photoKey, String text)  {
        SendPhoto command = createApiPhotoMessageCommand(photoKey, text);
        return executeTelegramApiMethod(command);
    }

    /**
     * Метод изменяет ТЕКСТ в уже отправленном сообщении.
     */
    public void updateTextMessage(Message message, String text) {
        EditMessageText command = new EditMessageText();
        command.setChatId(message.getChatId());
        command.setMessageId(message.getMessageId());
        command.setText(text);
        executeTelegramApiMethod(command);
    }

    /**
     * Сообщение с кнопками (Inline Buttons)
     */
    public Message sendTextButtonsMessage(String text, String... buttons) {
        SendMessage command = createApiSendMessageCommand(text);
        if (buttons.length > 0)
            attachButtons(command, List.of(buttons));

        return executeTelegramApiMethod(command);
    }

    /**
     * Сообщение с кнопками (Inline Buttons)
     */
    public void sendTextButtonsMessage(String text, List<String> buttons) {
        SendMessage command = createApiSendMessageCommand(text);
        if (buttons != null && !buttons.isEmpty())
            attachButtons(command, buttons);

        executeTelegramApiMethod(command);
    }

    public void showMainMenu(String... commands) {
        ArrayList<BotCommand> list = new ArrayList<BotCommand>();

        //convert strings to command list
        for (int i = 0; i < commands.length; i += 2) {
            String description = commands[i];
            String key = commands[i+1];

            if (key.startsWith("/")) //remove first /
                key = key.substring(1);

            BotCommand bc = new BotCommand(key, description);
            list.add(bc);
        }

        //get commands list
        var chatId = getCurrentChatId();
        GetMyCommands gmcs = new GetMyCommands();
        gmcs.setScope(BotCommandScopeChat.builder().chatId(chatId).build());
        ArrayList<BotCommand> oldCommands = executeTelegramApiMethod(gmcs);

        //ignore commands change for same command list
        if (oldCommands.equals(list))
            return;

        //set commands list
        SetMyCommands cmds = new SetMyCommands();
        cmds.setCommands(list);
        cmds.setScope(BotCommandScopeChat.builder().chatId(chatId).build());
        executeTelegramApiMethod(cmds);

        //show menu button
        var ex = new SetChatMenuButton();
        ex.setChatId(chatId);
        ex.setMenuButton(MenuButtonCommands.builder().build());
        executeTelegramApiMethod(ex);
    }

    public void hideMainMenu() {
        //delete commands list
        var chatId = getCurrentChatId();
        DeleteMyCommands dmds = new DeleteMyCommands();
        dmds.setScope(BotCommandScopeChat.builder().chatId(chatId).build());
        executeTelegramApiMethod(dmds);

        //hide menu button
        var ex = new SetChatMenuButton();
        ex.setChatId(chatId);
        ex.setMenuButton(MenuButtonDefault.builder().build());
        executeTelegramApiMethod(ex);
    }

    public List<List<PhotoSize>> getUserProfilePhotos() {
        var userId = getCurrentChatId();
        var request = GetUserProfilePhotos.builder().userId(userId).offset(0).limit(100).build();
        UserProfilePhotos userProfilePhotos = executeTelegramApiMethod(request);
        return userProfilePhotos.getPhotos();
    }

    public List<List<PhotoSize>> getChatBotProfilePhotos() {
        var me = executeTelegramApiMethod(new GetMe());
        var userId = me.getId();
        var request = GetUserProfilePhotos.builder().userId(userId).offset(0).limit(100).build();
        UserProfilePhotos userProfilePhotos = executeTelegramApiMethod(request);
        return userProfilePhotos.getPhotos();
    }

    private SendMessage createApiSendMessageCommand(String text) {
        SendMessage message = new SendMessage();
        message.setText(new String(text.getBytes(), StandardCharsets.UTF_8));
        message.setParseMode("markdown");
        message.setChatId(getCurrentChatId());
        return message;
    }

    private void attachButtons(SendMessage message, List<String> buttons) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (int i = 0; i < buttons.size(); i += 2) {
            String buttonName = buttons.get(i);
            String buttonValue = buttons.get(i + 1);

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(new String(buttonName.getBytes(), StandardCharsets.UTF_8));
            button.setCallbackData(buttonValue);

            keyboard.add(List.of(button));
        }

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
    }

    private SendPhoto createApiPhotoMessageCommand(String photoKey, String text) {
        try {
            InputFile inputFile = new InputFile();
            var is = loadImage(photoKey);
            inputFile.setMedia(is, photoKey);

            SendPhoto photo = new SendPhoto();
            photo.setPhoto(inputFile);
            photo.setChatId(getCurrentChatId());

            if (text != null && !text.isEmpty())
                photo.setCaption(text);

            return photo;
        } catch (Exception e) {
            throw new RuntimeException("Can't create photo message!");
        }
    }


    public static String loadPrompt(String name) {
        try {
            var is = ClassLoader.getSystemResourceAsStream("prompts/" + name + ".txt");
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Can't load GPT prompt!");
        }
    }

    public static String loadMessage(String name) {
        try {
            var is = ClassLoader.getSystemResourceAsStream("messages/" + name + ".txt");
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Can't load message!");
        }
    }

    public static InputStream loadImage(String name) {
        try {
            return ClassLoader.getSystemResourceAsStream("images/" + name + ".jpg");
        } catch (Exception e) {
            throw new RuntimeException("Can't load photo!");
        }
    }


    private <T extends Serializable, Method extends BotApiMethod<T>> T executeTelegramApiMethod(Method method) {
        try {
            return super.sendApiMethod(method);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private Message executeTelegramApiMethod(SendPhoto message) {
        try {
            return super.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
