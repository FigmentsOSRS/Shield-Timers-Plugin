package com.shieldtimer;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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

	static final int COOLDOWN_SECONDS = 120;

	// Item IDs — equipped form, confirmed via log
	static final int DRAGONFIRE_SHIELD_CHARGED = 11283;
	static final int DRAGONFIRE_WARD_CHARGED   = 22002;
	static final int ANCIENT_WYVERN_SHIELD     = 21633;

	// Item IDs — inventory (unequipped) form
	private static final int DRAGONFIRE_SHIELD_CHARGED_INV = 11284;
	private static final int DRAGONFIRE_WARD_CHARGED_INV   = 22002;
	private static final int ANCIENT_WYVERN_SHIELD_INV     = 21634;

	// Maps any known item ID (equipped or inventory) to its canonical equipped ID
	private static final Map<Integer, Integer> ITEM_TO_CANONICAL = new HashMap<>();
	static
	{
		ITEM_TO_CANONICAL.put(DRAGONFIRE_SHIELD_CHARGED,     DRAGONFIRE_SHIELD_CHARGED);
		ITEM_TO_CANONICAL.put(DRAGONFIRE_SHIELD_CHARGED_INV, DRAGONFIRE_SHIELD_CHARGED);
		ITEM_TO_CANONICAL.put(DRAGONFIRE_WARD_CHARGED,       DRAGONFIRE_WARD_CHARGED);
		ITEM_TO_CANONICAL.put(DRAGONFIRE_WARD_CHARGED_INV,   DRAGONFIRE_WARD_CHARGED);
		ITEM_TO_CANONICAL.put(ANCIENT_WYVERN_SHIELD,         ANCIENT_WYVERN_SHIELD);
		ITEM_TO_CANONICAL.put(ANCIENT_WYVERN_SHIELD_INV,     ANCIENT_WYVERN_SHIELD);
	}

	private static final List<Integer> ALL_SHIELD_IDS = Arrays.asList(
		DRAGONFIRE_SHIELD_CHARGED,
		DRAGONFIRE_WARD_CHARGED,
		ANCIENT_WYVERN_SHIELD
	);

	// Varbit IDs — confirmed in-game
	private static final int WARD_VARBIT_ID = 6540;

	// Projectile IDs — confirmed in-game
	private static final int PROJECTILE_DRAGONFIRE = 1166; // DFS + Ward share this
	private static final int PROJECTILE_AWS_FREEZE  = 500;  // AWS freeze blast

	// Charge tracking
	private static final Pattern CHARGES_PATTERN = Pattern.compile("The shield has (\\d+) charges\\.");

	// Config keys for persisting remaining time across logout
	private static final String CONFIG_GROUP       = "shieldtimer";
	private static final String KEY_DFS_REMAINING  = "dfsRemainingMs";
	private static final String KEY_WARD_REMAINING = "wardRemainingMs";
	private static final String KEY_AWS_REMAINING  = "awsRemainingMs";

	@Inject private Client client;
	@Inject private InfoBoxManager infoBoxManager;
	@Inject private ItemManager itemManager;
	@Inject private ConfigManager configManager;
	@Inject private Notifier notifier;
	@Inject private ShieldTimerConfig config;

	private final Map<Integer, ShieldTimerInfoBox> shieldBoxes     = new HashMap<>();
	private final Map<Integer, Instant>            activeCooldowns = new HashMap<>();

	// Rainbow cycle state — each counter advances independently per shield group
	private int dragonfireRainbowIdx = 0;
	private int awsRainbowIdx        = 0;

	private int pendingChargeShieldId = -1;
	private int lastDfsVarbit         = 0;
	private int lastWardVarbit        = 0;

	@Override
	protected void startUp()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			lastDfsVarbit  = client.getVarbitValue(Varbits.DRAGONFIRE_SHIELD_COOLDOWN);
			lastWardVarbit = client.getVarbitValue(WARD_VARBIT_ID);
			restoreTimers();
			updateShieldsPresent();
		}
		log.debug("ShieldTimerPlugin started");
	}

	@Override
	protected void shutDown()
	{
		removeAllBoxes();
		activeCooldowns.clear();
		clearSavedTimers();
		dragonfireRainbowIdx  = 0;
		awsRainbowIdx         = 0;
		lastDfsVarbit         = 0;
		lastWardVarbit        = 0;
		pendingChargeShieldId = -1;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGGED_IN:
				lastDfsVarbit  = client.getVarbitValue(Varbits.DRAGONFIRE_SHIELD_COOLDOWN);
				lastWardVarbit = client.getVarbitValue(WARD_VARBIT_ID);
				pendingChargeShieldId = -1;
				restoreTimers();
				updateShieldsPresent();
				break;

			case LOGIN_SCREEN:
			case HOPPING:
				saveTimers();
				removeAllBoxes();
				activeCooldowns.clear();
				dragonfireRainbowIdx  = 0;
				awsRainbowIdx         = 0;
				lastDfsVarbit         = 0;
				lastWardVarbit        = 0;
				pendingChargeShieldId = -1;
				break;

			default:
				break;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		String key = event.getKey();

		if (key.equals("showChargesDfs"))
		{
			ShieldTimerInfoBox box = shieldBoxes.get(DRAGONFIRE_SHIELD_CHARGED);
			if (box != null) box.setShowCharges(config.showChargesDfs());
		}
		else if (key.equals("showChargesWard"))
		{
			ShieldTimerInfoBox box = shieldBoxes.get(DRAGONFIRE_WARD_CHARGED);
			if (box != null) box.setShowCharges(config.showChargesWard());
		}
		else if (key.equals("showChargesAws"))
		{
			ShieldTimerInfoBox box = shieldBoxes.get(ANCIENT_WYVERN_SHIELD);
			if (box != null) box.setShowCharges(config.showChargesAws());
		}
		else if (key.equals("trackDfs") || key.equals("trackWard") || key.equals("trackAws"))
		{
			updateShieldsPresent();
		}
	}

	// -------------------------------------------------------------------------
	// Shield presence
	// -------------------------------------------------------------------------
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id == InventoryID.EQUIPMENT.getId() || id == InventoryID.INVENTORY.getId())
		{
			updateShieldsPresent();
		}
	}

	private void updateShieldsPresent()
	{
		for (int itemId : ALL_SHIELD_IDS)
		{
			boolean shouldTrack      = isShieldTracked(itemId) && isShieldPresent(itemId);
			boolean currentlyShowing = shieldBoxes.containsKey(itemId);

			if (shouldTrack && !currentlyShowing)
			{
				showBox(itemId);
			}
			else if (!shouldTrack && currentlyShowing)
			{
				hideBox(itemId);
			}
		}
	}

	private boolean isShieldTracked(int itemId)
	{
		switch (itemId)
		{
			case DRAGONFIRE_SHIELD_CHARGED: return config.trackDfs();
			case DRAGONFIRE_WARD_CHARGED:   return config.trackWard();
			case ANCIENT_WYVERN_SHIELD:     return config.trackAws();
			default:                        return false;
		}
	}

	private boolean isShieldPresent(int canonicalId)
	{
		Set<Integer> knownIds = new HashSet<>();
		for (Map.Entry<Integer, Integer> entry : ITEM_TO_CANONICAL.entrySet())
		{
			if (entry.getValue() == canonicalId)
			{
				knownIds.add(entry.getKey());
			}
		}

		for (InventoryID containerId : new InventoryID[]{InventoryID.EQUIPMENT, InventoryID.INVENTORY})
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container == null)
			{
				continue;
			}
			for (Item item : container.getItems())
			{
				if (knownIds.contains(item.getId()))
				{
					return true;
				}
			}
		}
		return false;
	}

	private void showBox(int itemId)
	{
		BufferedImage image    = itemManager.getImage(itemId);
		boolean showCharges    = showChargesFor(itemId);
		ShieldTimerInfoBox box = new ShieldTimerInfoBox(image, this, itemId, showCharges);

		Instant endTime = activeCooldowns.get(itemId);
		if (endTime != null)
		{
			box.setEndTime(endTime);
		}

		shieldBoxes.put(itemId, box);
		infoBoxManager.addInfoBox(box);
		log.debug("Showing box for item {}", itemId);
	}

	private void hideBox(int itemId)
	{
		ShieldTimerInfoBox box = shieldBoxes.remove(itemId);
		if (box != null)
		{
			infoBoxManager.removeInfoBox(box);
			log.debug("Hiding box for item {}", itemId);
		}
	}

	private void removeAllBoxes()
	{
		shieldBoxes.values().forEach(infoBoxManager::removeInfoBox);
		shieldBoxes.clear();
	}

	// -------------------------------------------------------------------------
	// DFS + Ward — varbit triggers
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
				log.debug("DFS activated - starting {}s timer", COOLDOWN_SECONDS);
				startCooldown(DRAGONFIRE_SHIELD_CHARGED, COOLDOWN_SECONDS);
			}
			else if (current == 0 && lastDfsVarbit > 0)
			{
				endCooldown(DRAGONFIRE_SHIELD_CHARGED, KEY_DFS_REMAINING);
			}
			lastDfsVarbit = current;
		}
		else if (id == WARD_VARBIT_ID)
		{
			if (lastWardVarbit == 0 && current > 0 && config.trackWard())
			{
				log.debug("Ward activated - starting {}s timer", COOLDOWN_SECONDS);
				startCooldown(DRAGONFIRE_WARD_CHARGED, COOLDOWN_SECONDS);
			}
			else if (current == 0 && lastWardVarbit > 0)
			{
				endCooldown(DRAGONFIRE_WARD_CHARGED, KEY_WARD_REMAINING);
			}
			lastWardVarbit = current;
		}
	}

	// -------------------------------------------------------------------------
	// Projectile handler — AWS timer detection + cosmetic projectile swaps.
	// Only processes projectiles originating from the local player.
	// Swap technique from the Chinbompa plugin (sigterm): create a replacement
	// with the same trajectory, add it to the client list, expire the original.
	// -------------------------------------------------------------------------
	@SuppressWarnings("deprecation")
	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		int id = projectile.getId();

		if (id != PROJECTILE_DRAGONFIRE && id != PROJECTILE_AWS_FREEZE)
		{
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		WorldPoint source = WorldPoint.fromLocal(client, projectile.getX1(), projectile.getY1(), client.getPlane());
		if (!source.equals(localPlayer.getWorldLocation()))
		{
			return;
		}

		if (id == PROJECTILE_AWS_FREEZE && config.trackAws())
		{
			log.debug("AWS projectile {} detected - starting {}s timer", PROJECTILE_AWS_FREEZE, COOLDOWN_SECONDS);
			startCooldown(ANCIENT_WYVERN_SHIELD, COOLDOWN_SECONDS);
		}

		ProjectileSwap swap = (id == PROJECTILE_DRAGONFIRE)
			? config.dragonfireProjectile()
			: config.awsProjectile();

		if (swap == ProjectileSwap.RAINBOW)
		{
			int idx   = (id == PROJECTILE_DRAGONFIRE) ? dragonfireRainbowIdx++ : awsRainbowIdx++;
			int newId = ProjectileSwap.RAINBOW_SEQUENCE[idx % ProjectileSwap.RAINBOW_SEQUENCE.length];
			log.debug("Rainbow swap projectile {} -> {}", id, newId);
			swapProjectile(projectile, newId);
		}
		else if (swap == ProjectileSwap.RANDOM)
		{
			ProjectileSwap pick = ProjectileSwap.random();
			log.debug("Random swap projectile {} -> {} ({})", id, pick.getProjectileId(), pick);
			swapProjectile(projectile, pick.getProjectileId());
		}
		else if (swap != ProjectileSwap.NONE)
		{
			log.debug("Swapping projectile {} -> {}", id, swap.getProjectileId());
			swapProjectile(projectile, swap.getProjectileId());
		}
	}

	// Creates a replacement projectile with an alternative spotanim ID but
	// identical trajectory, then expires the original immediately.
	@SuppressWarnings("deprecation")
	private void swapProjectile(Projectile original, int newId)
	{
		if (original.getTarget() == null)
		{
			return;
		}

		Projectile replacement = client.createProjectile(
			newId,
			original.getFloor(),
			original.getX1(),
			original.getY1(),
			original.getHeight(),
			original.getStartCycle(),
			original.getEndCycle(),
			original.getSlope(),
			original.getStartHeight(),
			original.getEndHeight(),
			original.getInteracting(),
			original.getTarget().getX(),
			original.getTarget().getY()
		);

		client.getProjectiles().addLast(replacement);
		original.setEndCycle(0);
	}

	// -------------------------------------------------------------------------
	// Charge tracking — Inspect (DFS/Ward) or Check (AWS)
	// -------------------------------------------------------------------------
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option   = event.getMenuOption();
		int eventItemId = event.getItemId();
		int equippedId  = getEquippedShieldId();
		int resolvedId  = (eventItemId != -1) ? eventItemId : equippedId;

		Integer canonical = ITEM_TO_CANONICAL.get(resolvedId);
		if (canonical == null)
		{
			return;
		}

		if ("Inspect".equals(option) || "Check".equals(option))
		{
			pendingChargeShieldId = canonical;
			log.debug("Charge check pending for item {}", canonical);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (pendingChargeShieldId == -1)
		{
			return;
		}

		if (event.getType() != ChatMessageType.GAMEMESSAGE
			&& event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		Matcher matcher = CHARGES_PATTERN.matcher(event.getMessage());
		if (!matcher.matches())
		{
			return;
		}

		int charges = Integer.parseInt(matcher.group(1));
		log.debug("Charges {} detected for item {}", charges, pendingChargeShieldId);

		ShieldTimerInfoBox box = shieldBoxes.get(pendingChargeShieldId);
		if (box != null)
		{
			box.setCharges(charges);
		}

		pendingChargeShieldId = -1;
	}

	// -------------------------------------------------------------------------
	// AWS expiry detection — no varbit, check each tick
	// -------------------------------------------------------------------------
	@Subscribe
	public void onGameTick(GameTick event)
	{
		Instant awsEnd = activeCooldowns.get(ANCIENT_WYVERN_SHIELD);
		if (awsEnd != null && Instant.now().isAfter(awsEnd))
		{
			endCooldown(ANCIENT_WYVERN_SHIELD, KEY_AWS_REMAINING);
		}
	}

	// -------------------------------------------------------------------------
	// Cooldown lifecycle
	// -------------------------------------------------------------------------
	private void startCooldown(int itemId, int seconds)
	{
		Instant endTime = Instant.now().plusSeconds(seconds);
		activeCooldowns.put(itemId, endTime);

		ShieldTimerInfoBox box = shieldBoxes.get(itemId);
		if (box != null)
		{
			box.setEndTime(endTime);
		}
	}

	private void endCooldown(int itemId, String persistKey)
	{
		activeCooldowns.remove(itemId);
		clearSavedTimer(persistKey);

		ShieldTimerInfoBox box = shieldBoxes.get(itemId);
		if (box != null)
		{
			box.setEndTime(null);
		}

		if (trayNotifyFor(itemId))
		{
			notifier.notify(shieldNameFor(itemId) + " is ready!");
		}
	}

	// -------------------------------------------------------------------------
	// Per-shield config helpers
	// -------------------------------------------------------------------------
	private boolean showChargesFor(int itemId)
	{
		switch (itemId)
		{
			case DRAGONFIRE_SHIELD_CHARGED: return config.showChargesDfs();
			case DRAGONFIRE_WARD_CHARGED:   return config.showChargesWard();
			case ANCIENT_WYVERN_SHIELD:     return config.showChargesAws();
			default:                        return false;
		}
	}

	private boolean trayNotifyFor(int itemId)
	{
		switch (itemId)
		{
			case DRAGONFIRE_SHIELD_CHARGED: return config.trayNotifyDfs();
			case DRAGONFIRE_WARD_CHARGED:   return config.trayNotifyWard();
			case ANCIENT_WYVERN_SHIELD:     return config.trayNotifyAws();
			default:                        return false;
		}
	}

	private String shieldNameFor(int itemId)
	{
		switch (itemId)
		{
			case DRAGONFIRE_SHIELD_CHARGED: return "Dragonfire Shield";
			case DRAGONFIRE_WARD_CHARGED:   return "Dragonfire Ward";
			case ANCIENT_WYVERN_SHIELD:     return "Ancient Wyvern Shield";
			default:                        return "Shield";
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
		Instant endTime = activeCooldowns.get(itemId);
		if (endTime == null)
		{
			return;
		}
		long remainingMs = endTime.toEpochMilli() - Instant.now().toEpochMilli();
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
				Instant endTime = Instant.now().plusMillis(remainingMs);
				activeCooldowns.put(itemId, endTime);

				ShieldTimerInfoBox box = shieldBoxes.get(itemId);
				if (box != null)
				{
					box.setEndTime(endTime);
				}

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

	@Provides
	ShieldTimerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ShieldTimerConfig.class);
	}
}
