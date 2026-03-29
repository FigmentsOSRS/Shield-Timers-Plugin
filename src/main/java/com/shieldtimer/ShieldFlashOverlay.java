package com.shieldtimer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class ShieldFlashOverlay extends Overlay
{
	private static final long FLASH_DURATION_MS = 1500;
	private static final int  MAX_ALPHA         = 80;

	// item ID -> flash start time (ms)
	private final Map<Integer, Long>  activeFlashes = new HashMap<>();
	private final Map<Integer, Color> flashColors   = new HashMap<>();

	@Inject
	public ShieldFlashOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(OverlayPriority.HIGH);
	}

	public void flash(int itemId, Color color)
	{
		activeFlashes.put(itemId, System.currentTimeMillis());
		flashColors.put(itemId, color);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (activeFlashes.isEmpty())
		{
			return null;
		}

		long now = System.currentTimeMillis();
		Iterator<Map.Entry<Integer, Long>> it = activeFlashes.entrySet().iterator();

		while (it.hasNext())
		{
			Map.Entry<Integer, Long> entry = it.next();
			long elapsed = now - entry.getValue();

			if (elapsed >= FLASH_DURATION_MS)
			{
				it.remove();
				flashColors.remove(entry.getKey());
				continue;
			}

			Color base = flashColors.get(entry.getKey());
			if (base == null)
			{
				continue;
			}

			float progress = (float) elapsed / FLASH_DURATION_MS;
			int alpha = (int) (MAX_ALPHA * (1f - progress));

			graphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));

			Rectangle bounds = graphics.getClipBounds();
			if (bounds != null)
			{
				graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		}

		return null;
	}
}
