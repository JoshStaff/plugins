/*
 * Copyright (c) 2020, Charles Xu <github.com/kthisiscvpv>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jsoft.oneclicksocket;

import com.google.inject.Provides;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.net.URISyntaxException;

@Slf4j
@Extension
@PluginDescriptor(
        name = "OneClickSocket",
        description = "Socket connection for broadcasting or receiving clicks across clients.",
        tags = {"socket", "one", "click", "connection", "broadcast"},
        enabledByDefault = false
)
public class OneClickSocketPlugin extends Plugin {

    @Inject
    @Getter(AccessLevel.PUBLIC)
    private Client client;

    @Inject
    @Getter(AccessLevel.PUBLIC)
    private EventBus eventBus;

    @Inject
    @Getter(AccessLevel.PUBLIC)
    private ClientThread clientThread;

    @Inject
    @Getter(AccessLevel.PUBLIC)
    private OneClickSocketConfig config;

    @Provides
    OneClickSocketConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(OneClickSocketConfig.class);
    }

    // This variables controls the next UNIX epoch time to establish the next connection.
    @Getter(AccessLevel.PUBLIC)
    @Setter(AccessLevel.PUBLIC)
    private long nextConnection;

    // This variables controls the current active connection.
    private Socket socket = null;

    @Override
    protected void startUp() {
        nextConnection = 0L;
    }

    @Override
    protected void shutDown() {
        if (socket != null) {
            socket.disconnect();
        }
    }

    @Subscribe
    public void onConfigButtonClicked(ConfigButtonClicked event) throws URISyntaxException {
        if (event.getKey().equals("connectToSocket")) {
            Socket socket = IO.socket("http://192.168.68.104:3002/?socket=12345");


            socket.on("digestClick", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                            "Hello world!",
                            null));
                }
            });
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {

    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        // Notify the user to restart the plugin when the config changes.
        if (event.getGroup().equals("Socket")) {
            clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "<col=b4281e>Configuration changed. Please restart the plugin to see updates.",
                    null));
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        // Terminate all connections to the socket server when the user logs out.
        if (event.getGameState() == GameState.LOGIN_SCREEN) {
            if (socket != null) {
                socket.disconnect();
            }
        }
    }
}
