package com.shieldtimer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("shieldtimer")
public interface ShieldTimerConfig extends Config
{
    @ConfigItem(
        keyName = "trackDfs",
        name = "Track Dragonfire Shield",
        description = "Show a cooldown timer when the Dragonfire Shield is activated",
        position = 0
    )
    default boolean trackDfs()
    {
        return true;
    }

    @ConfigItem(
        keyName = "trackWard",
        name = "Track Dragonfire Ward",
        description = "Show a cooldown timer when the Dragonfire Ward is activated",
        position = 1
    )
    default boolean trackWard()
    {
        return true;
    }

    @ConfigItem(
        keyName = "trackAws",
        name = "Track Ancient Wyvern Shield",
        description = "Show a cooldown timer when the Ancient Wyvern Shield is operated",
        position = 2
    )
    default boolean trackAws()
    {
        return true;
    }
}
