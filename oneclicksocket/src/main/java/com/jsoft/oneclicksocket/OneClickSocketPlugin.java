package com.jsoft.oneclicksocket;

import com.google.inject.Provides;
import javax.inject.Inject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import lombok.AccessLevel;
import lombok.Getter;
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

import java.net.URISyntaxException;

@Extension
@PluginDescriptor(
		name = "OneClickSocket",
		description = "Socket connection for broadcasting or receiving clicks across clients.",
		tags = {"socket", "one", "click", "connection", "broadcast"},
		enabledByDefault = false
)
@Slf4j
public class OneClickSocketPlugin extends Plugin
{

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


	// This variables controls the current active connection.
	private Socket socket = null;



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