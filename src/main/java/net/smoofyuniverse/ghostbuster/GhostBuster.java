/*
 * Copyright (c) 2021 Hugo Dupanloup (Yeregorix)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.smoofyuniverse.ghostbuster;

import com.google.inject.Inject;
import net.smoofyuniverse.ore.update.UpdateChecker;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.AABB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

@Plugin(id = "ghostbuster", name = "GhostBuster", version = "1.0.0", authors = "Yeregorix", description = "A ghost block fixer")
public class GhostBuster {
	public static final Logger LOGGER = LoggerFactory.getLogger("GhostBuster");
	private static GhostBuster instance;

	@Inject
	private Game game;
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;
	@Inject
	private PluginContainer container;

	public GhostBuster() {
		if (instance != null)
			throw new IllegalStateException();
		instance = this;
	}

	@Listener
	public void onGamePreInit(GamePreInitializationEvent e) {
		try {
			Files.createDirectories(this.configDir);
		} catch (IOException ignored) {
		}

		this.game.getEventManager().registerListeners(this, new UpdateChecker(LOGGER, this.container,
				createConfigLoader(this.configDir.resolve("update.conf")), "Yeregorix", "GhostBuster"));
	}

	public ConfigurationLoader<CommentedConfigurationNode> createConfigLoader(Path file) {
		return HoconConfigurationLoader.builder().setPath(file).build();
	}

	@Listener
	public void onServerStarted(GameStartedServerEvent e) {
		LOGGER.info("GhostBuster " + this.container.getVersion().orElse("?") + " was loaded successfully.");
	}

	@Listener
	public void onPlayerMove(MoveEntityEvent e, @Getter("getTargetEntity") Player p) {
		Optional<AABB> playerBox = p.getBoundingBox();
		if (playerBox.isPresent()) {
			Set<AABB> blocks = p.getWorld().getIntersectingBlockCollisionBoxes(playerBox.get());
			if (!blocks.isEmpty() && p.hasPermission("ghostbuster.resend")) {
				for (AABB blockBox : blocks) {
					p.resetBlockChange(blockBox.getCenter().toInt());
				}
			}
		}
	}

	public PluginContainer getContainer() {
		return this.container;
	}

	public static GhostBuster get() {
		if (instance == null)
			throw new IllegalStateException("Instance not available");
		return instance;
	}
}
