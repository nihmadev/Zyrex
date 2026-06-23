package lol.zyrex.utils.connection.irc;

import lol.zyrex.Zyrex;
import lol.zyrex.utils.client.chat.ChatMessage;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class IRCManager {
    private IRCWebSocketClient client;
    final AtomicBoolean isConnecting = new AtomicBoolean(false);

    public IRCWebSocketClient getClient() {
        return client;
    }

    public void connect() {
        // Отключено - соединение с внешним сервером
        // if (isConnecting.get()) {
        //     return;
        // }
        // try {
        //     isConnecting.set(true);
        //     if (client != null && !client.isClosed()) {
        //         client.close();
        //     }
        //     client = new IRCWebSocketClient(new URI("ws://45.155.205.202:8081"));
        //     client.connect();
        // } catch (Exception e) {
        //     if (Zyrex.getInstance().isShowIrcMessages()) {
        //         ChatMessage.ircmessageWithRed("Не удалось подключиться к серверу IRC");
        //     }
        //     isConnecting.set(false);
        // }
    }

    public void disconnect() {
        if (client != null) {
            client.close();
            client = null;
        }
        isConnecting.set(false);
    }

    public void sendMessage(String msg, String prefix) {
        if (client != null && client.isOpen()) {
            client.sendMessage(msg);
        } else if (Zyrex.getInstance().isShowIrcMessages()) {
        }
    }
}