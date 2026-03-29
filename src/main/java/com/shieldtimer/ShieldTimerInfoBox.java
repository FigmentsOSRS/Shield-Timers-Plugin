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
	private Instant endTime;      // null = Ready state
	private int charges = -1;     // -1 = unknown (never checked)
	private boolean showCharges;  // mirrors per-shield config setting
	private final int itemId;

	public ShieldTimerInfoBox(BufferedImage image, Plugin plugin, int itemId, boolean showCharges)
	{
		super(image, plugin);
		this.itemId      = itemId;
		this.showCharges = showCharges;
		this.endTime     = null;
		setPriority(InfoBoxPriority.MED);
	}

	public void setEndTime(Instant endTime)
	{
		this.endTime = endTime;
	}

	public Instant getEndTime()
	{
		return endTime;
	}

	public void setCharges(int charges)
	{
		this.charges = charges;
	}

	public int getCharges()
	{
		return charges;
	}

	public void setShowCharges(boolean showCharges)
	{
		this.showCharges = showCharges;
	}

	public boolean isOnCooldown()
	{
		return endTime != null && !Duration.between(Instant.now(), endTime).isNegative();
	}

	@Override
	public boolean render()
	{
		return true;
	}

	@Override
	public String getText()
	{
		if (endTime != null)
		{
			Duration remaining = Duration.between(Instant.now(), endTime);
			if (!remaining.isNegative() && !remaining.isZero())
			{
				long totalSeconds = remaining.getSeconds() + 1;
				return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
			}
		}

		if (showCharges && charges >= 0)
		{
			return String.valueOf(charges);
		}

		return "Ready";
	}

	@Override
	public Color getTextColor()
	{
		if (charges >= 0 && charges < 5)
		{
			return Color.RED;
		}

		if (endTime == null)
		{
			return Color.GREEN;
		}

		Duration remaining = Duration.between(Instant.now(), endTime);
		long seconds = remaining.getSeconds();

		if (seconds <= 0)  return Color.GREEN;
		if (seconds <= 10) return Color.RED;
		if (seconds <= 30) return Color.YELLOW;
		return Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		StringBuilder sb = new StringBuilder(getShieldName());

		if (endTime == null || Duration.between(Instant.now(), endTime).isNegative())
		{
			sb.append(" - Ready!");
		}
		else
		{
			long seconds = Duration.between(Instant.now(), endTime).getSeconds();
			sb.append(String.format(" - ready in %d:%02d", seconds / 60, seconds % 60));
		}

		if (charges >= 0)
		{
			sb.append(" (").append(charges).append(" charges)");
		}

		return sb.toString();
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
