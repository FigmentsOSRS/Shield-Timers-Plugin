package com.shieldtimer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

public class ShieldTimerInfoBox extends InfoBox
{
	private final Instant endTime;
	private final int itemId;

	public ShieldTimerInfoBox(BufferedImage image, Plugin plugin, int itemId, Instant endTime)
	{
		super(image, plugin);
		this.itemId  = itemId;
		this.endTime = endTime;
		setPriority(InfoBoxPriority.MED);
	}

	public Instant getEndTime()
	{
		return endTime;
	}

	@Override
	public boolean render()
	{
		return !Duration.between(Instant.now(), endTime).isNegative();
	}

	@Override
	public String getText()
	{
		Duration remaining = Duration.between(Instant.now(), endTime);

		if (remaining.isNegative() || remaining.isZero())
		{
			return "0:00";
		}

		long totalSeconds = remaining.getSeconds() + 1;
		return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
	}

	@Override
	public Color getTextColor()
	{
		Duration remaining = Duration.between(Instant.now(), endTime);
		long seconds = remaining.getSeconds();

		if (seconds <= 10) return Color.RED;
		if (seconds <= 30) return Color.YELLOW;
		return Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		Duration remaining = Duration.between(Instant.now(), endTime);

		if (remaining.isNegative() || remaining.isZero())
		{
			return getShieldName() + " — Ready!";
		}

		long seconds = remaining.getSeconds();
		return String.format("%s — ready in %d:%02d", getShieldName(), seconds / 60, seconds % 60);
	}

	private String getShieldName()
	{
		switch (itemId)
		{
			case ShieldTimerPlugin.DRAGONFIRE_SHIELD_CHARGED: return "Dragonfire Shield";
			case ShieldTimerPlugin.DRAGONFIRE_WARD_CHARGED:   return "Dragonfire Ward";
			case ShieldTimerPlugin.ANCIENT_WYVERN_SHIELD:     return "Ancient Wyvern Shield";
			default:                                           return "Shield";
		}
	}
}
