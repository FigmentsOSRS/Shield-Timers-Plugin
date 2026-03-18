package com.shieldtimer;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Shield Timers",
	description = "Tracks independent activation cooldowns for Dragonfire Shield, Dragonfire Ward, and Ancient Wyvern Shield",
	tags = {"shield", "dragonfire", "wyvern", "timer", "cooldown", "dfs", "ward"}
)
public class ShieldTimerPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(ShieldTimerPlugin.class);

	// Each varbit unit = 8 ticks = 4.8 seconds
	private static final double SECONDS_PER_VARBIT_UNIT = 8 * 0.6;

	// Fixed cooldown for AWS (no cooldown varbit exposed to client)
	static final int COOLDOWN_SECONDS = 120;

	// Item IDs — equipped form, confirmed via log
	static final int DRAGONFIRE_SHIELD_CHARGED = 11283;
	static final int DRAGONFIRE_WARD_CHARGED   = 22002;
	static final int ANCIENT_WYVERN_SHIELD     = 21633;

	// Varbit IDs — confirmed in-game
	// DFS:  6539 (Varbits.DRAGONFIRE_SHIELD_COOLDOWN)
	// Ward: 6540
	// AWS:  none — timer starts on confirmed operate animation
	private static final int WARD_VARBIT_ID = 6540;

	// AWS operate animation ID — confirmed in-game via log
	private static final int AWS_OPERATE_ANIM = 7700;

	// Config keys for persisting remaining time across logout (stored as millis remaining)
	private static final String CONFIG_GROUP       = "shieldtimer";
	private static final String KEY_DFS_REMAINING  = "dfsRemainingMs";
	private static final String KEY_WARD_REMAINING = "wardRemainingMs";
	private static final String KEY_AWS_REMAINING  = "awsRemainingMs";

	@Inject private Client client;
	@Inject private InfoBoxManager infoBoxManager;
	@Inject private ItemManager itemManager;
	@Inject private ConfigManager configManager;
	@Inject private ShieldTimerConfig config;

	private final Map<Integer, ShieldTimerInfoBox> activeTimers = new HashMap<>();

	private int lastDfsVarbit  = 0;
	private int lastWardVarbit = 0;

	// True after player clicks Operate on AWS.
	// Timer starts only when animation 7700 fires on the local player.
	private boolean awsSpecPending = false;

	@Override
	protected void startUp()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			lastDfsVarbit  = client.getVarbitValue(Varbits.DRAGONFIRE_SHIELD_COOLDOWN);
			lastWardVarbit = client.getVarbitValue(WARD_VARBIT_ID);
			restoreTimers();
		}
		log.debug("ShieldTimerPlugin started");
	}

	@Override
	protected void shutDown()
	{
		// Don't save on shutdown — only save on logout so plugin disable clears timers cleanly
		clearAllTimers();
		clearSavedTimers();
		lastDfsVarbit  = 0;
		lastWardVarbit = 0;
		awsSpecPending = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGGED_IN:
				lastDfsVarbit  = client.getVarbitValue(Varbits.DRAGONFIRE_SHIELD_COOLDOWN);
				lastWardVarbit = client.getVarbitValue(WARD_VARBIT_ID);
				awsSpecPending = false;
				restoreTimers();
				break;

			case LOGIN_SCREEN:
			case HOPPING:
				// Persist remaining time before clearing so it survives the logout
				saveTimers();
				clearAllTimers();
				lastDfsVarbit  = 0;
				lastWardVarbit = 0;
				awsSpecPending = false;
				break;

			default:
				break;
		}
	}

	// -------------------------------------------------------------------------
	// DFS  — varbit 6539 fires 0 -> 24 when effect lands
	// Ward — varbit 6540 fires 0 -> 24 when effect lands
	// -------------------------------------------------------------------------
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int id      = event.getVarbitId();
		int current = event.getValue();

		if (id == Varbits.DRAGONFIRE_SHIELD_COOLDOWN)
		{
			if (lastDfsVarbit == 0 && current > 0 && config.trackDfs())
			{
				double seconds = current * SECONDS_PER_VARBIT_UNIT;
				log.debug("DFS activated — varbit={} derived={}s", current, seconds);
				startTimer(DRAGONFIRE_SHIELD_CHARGED, seconds);
			}
			else if (current == 0 && lastDfsVarbit > 0)
			{
				removeTimer(DRAGONFIRE_SHIELD_CHARGED);
				clearSavedTimer(KEY_DFS_REMAINING);
			}
			lastDfsVarbit = current;
		}
		else if (id == WARD_VARBIT_ID)
		{
			if (lastWardVarbit == 0 && current > 0 && config.trackWard())
			{
				double seconds = current * SECONDS_PER_VARBIT_UNIT;
				log.debug("Ward activated — varbit={} derived={}s", current, seconds);
				startTimer(DRAGONFIRE_WARD_CHARGED, seconds);
			}
			else if (current == 0 && lastWardVarbit > 0)
			{
				removeTimer(DRAGONFIRE_WARD_CHARGED);
				clearSavedTimer(KEY_WARD_REMAINING);
			}
			lastWardVarbit = current;
		}
	}

	// -------------------------------------------------------------------------
	// AWS — Operate click sets pending flag.
	// Timer starts only when animation 7700 fires on the local player.
	// -------------------------------------------------------------------------
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!"Operate".equals(event.getMenuOption()) || !config.trackAws())
		{
			return;
		}

		int eventItemId = event.getItemId();
		int equippedId  = getEquippedShieldId();
		int resolvedId  = (eventItemId != -1) ? eventItemId : equippedId;

		if (resolvedId == ANCIENT_WYVERN_SHIELD)
		{
			log.debug("AWS Operate clicked — waiting for animation {}", AWS_OPERATE_ANIM);
			awsSpecPending = true;
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!awsSpecPending)
		{
			return;
		}

		Actor actor = event.getActor();
		Player localPlayer = client.getLocalPlayer();

		if (actor != localPlayer)
		{
			return;
		}

		if (actor.getAnimation() == AWS_OPERATE_ANIM)
		{
			log.debug("AWS animation {} confirmed — starting {}s timer", AWS_OPERATE_ANIM, COOLDOWN_SECONDS);
			startTimer(ANCIENT_WYVERN_SHIELD, COOLDOWN_SECONDS);
			awsSpecPending = false;
		}
	}

	// -------------------------------------------------------------------------
	// Persist / restore timer state across logout
	// -------------------------------------------------------------------------

	private void saveTimers()
	{
		saveRemainingMs(DRAGONFIRE_SHIELD_CHARGED, KEY_DFS_REMAINING);
		saveRemainingMs(DRAGONFIRE_WARD_CHARGED,   KEY_WARD_REMAINING);
		saveRemainingMs(ANCIENT_WYVERN_SHIELD,     KEY_AWS_REMAINING);
	}

	private void saveRemainingMs(int itemId, String key)
	{
		ShieldTimerInfoBox box = activeTimers.get(itemId);
		if (box == null)
		{
			return;
		}
		long remainingMs = box.getEndTime().toEpochMilli() - Instant.now().toEpochMilli();
		if (remainingMs > 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, key, remainingMs);
			log.debug("Saved {} remaining {}ms", key, remainingMs);
		}
	}

	private void restoreTimers()
	{
		restoreFromKey(DRAGONFIRE_SHIELD_CHARGED, KEY_DFS_REMAINING,  config.trackDfs());
		restoreFromKey(DRAGONFIRE_WARD_CHARGED,   KEY_WARD_REMAINING, config.trackWard());
		restoreFromKey(ANCIENT_WYVERN_SHIELD,     KEY_AWS_REMAINING,  config.trackAws());
	}

	private void restoreFromKey(int itemId, String key, boolean enabled)
	{
		if (!enabled)
		{
			return;
		}
		String raw = configManager.getConfiguration(CONFIG_GROUP, key);
		if (raw == null)
		{
			return;
		}
		try
		{
			long remainingMs = Long.parseLong(raw);
			if (remainingMs > 0)
			{
				log.debug("Restoring {} with {}ms remaining", key, remainingMs);
				startTimer(itemId, remainingMs / 1000.0);
				clearSavedTimer(key);
			}
		}
		catch (NumberFormatException e)
		{
			log.warn("Failed to parse saved timer for {}: {}", key, raw);
			clearSavedTimer(key);
		}
	}

	private void clearSavedTimers()
	{
		clearSavedTimer(KEY_DFS_REMAINING);
		clearSavedTimer(KEY_WARD_REMAINING);
		clearSavedTimer(KEY_AWS_REMAINING);
	}

	private void clearSavedTimer(String key)
	{
		configManager.unsetConfiguration(CONFIG_GROUP, key);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private int getEquippedShieldId()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
		{
			return -1;
		}
		Item shield = equipment.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
		return (shield != null) ? shield.getId() : -1;
	}

	void startTimer(int itemId, double seconds)
	{
		removeTimer(itemId);
		Instant endTime = Instant.now().plusMillis((long) (seconds * 1000));
		BufferedImage image = itemManager.getImage(itemId);
		ShieldTimerInfoBox box = new ShieldTimerInfoBox(image, this, itemId, endTime);
		activeTimers.put(itemId, box);
		infoBoxManager.addInfoBox(box);
		log.debug("Timer started for item {} ({} seconds)", itemId, seconds);
	}

	void removeTimer(int itemId)
	{
		ShieldTimerInfoBox box = activeTimers.remove(itemId);
		if (box != null)
		{
			infoBoxManager.removeInfoBox(box);
		}
	}

	private void clearAllTimers()
	{
		activeTimers.values().forEach(infoBoxManager::removeInfoBox);
		activeTimers.clear();
	}

	@Provides
	ShieldTimerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ShieldTimerConfig.class);
	}
}
