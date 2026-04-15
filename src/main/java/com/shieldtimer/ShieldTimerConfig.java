package com.shieldtimer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("shieldtimer")
public interface ShieldTimerConfig extends Config
{
    // -------------------------------------------------------------------------
    // Dragonfire Shield
    // -------------------------------------------------------------------------
    @ConfigSection(
        name = "Dragonfire Shield",
        description = "Settings for the Dragonfire Shield",
        position = 0
    )
    String dfsSection = "dfs";

    @ConfigItem(
        keyName = "trackDfs",
        name = "Track",
        description = "Show a cooldown timer when the Dragonfire Shield is activated",
        section = dfsSection,
        position = 0
    )
    default boolean trackDfs()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showChargesDfs",
        name = "Charges shown",
        description = "Display charge count in the InfoBox when off cooldown (right-click Inspect to update)",
        section = dfsSection,
        position = 1
    )
    default boolean showChargesDfs()
    {
        return true;
    }

    @ConfigItem(
        keyName = "trayNotifyDfs",
        name = "Tray notification",
        description = "Send a tray notification when the Dragonfire Shield comes off cooldown",
        section = dfsSection,
        position = 2
    )
    default boolean trayNotifyDfs()
    {
        return true;
    }

    // -------------------------------------------------------------------------
    // Dragonfire Ward
    // -------------------------------------------------------------------------
    @ConfigSection(
        name = "Dragonfire Ward",
        description = "Settings for the Dragonfire Ward",
        position = 1
    )
    String wardSection = "ward";

    @ConfigItem(
        keyName = "trackWard",
        name = "Track",
        description = "Show a cooldown timer when the Dragonfire Ward is activated",
        section = wardSection,
        position = 0
    )
    default boolean trackWard()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showChargesWard",
        name = "Charges shown",
        description = "Display charge count in the InfoBox when off cooldown (right-click Inspect to update)",
        section = wardSection,
        position = 1
    )
    default boolean showChargesWard()
    {
        return true;
    }

    @ConfigItem(
        keyName = "trayNotifyWard",
        name = "Tray notification",
        description = "Send a tray notification when the Dragonfire Ward comes off cooldown",
        section = wardSection,
        position = 2
    )
    default boolean trayNotifyWard()
    {
        return true;
    }

    // -------------------------------------------------------------------------
    // Ancient Wyvern Shield
    // -------------------------------------------------------------------------
    @ConfigSection(
        name = "Ancient Wyvern Shield",
        description = "Settings for the Ancient Wyvern Shield",
        position = 2
    )
    String awsSection = "aws";

    @ConfigItem(
        keyName = "trackAws",
        name = "Track",
        description = "Show a cooldown timer when the Ancient Wyvern Shield is operated",
        section = awsSection,
        position = 0
    )
    default boolean trackAws()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showChargesAws",
        name = "Charges shown",
        description = "Display charge count in the InfoBox when off cooldown (right-click Check to update)",
        section = awsSection,
        position = 1
    )
    default boolean showChargesAws()
    {
        return true;
    }

    @ConfigItem(
        keyName = "trayNotifyAws",
        name = "Tray notification",
        description = "Send a tray notification when the Ancient Wyvern Shield comes off cooldown",
        section = awsSection,
        position = 2
    )
    default boolean trayNotifyAws()
    {
        return true;
    }

    // -------------------------------------------------------------------------
    // Projectile swaps — cosmetic only, local client only
    // -------------------------------------------------------------------------
    @ConfigSection(
        name = "Projectile Swaps",
        description = "Replace your shield spec projectiles with a different visual (cosmetic only)",
        position = 3
    )
    String swapSection = "swap";

    @ConfigItem(
        keyName = "dragonfireProjectile",
        name = "DFS / Ward projectile",
        description = "Replace the Dragonfire Shield and Ward spec projectile with an alternative visual. "
            + "Random picks a different one each time. Rainbow Cycle steps through a colour spectrum on each activation.",
        section = swapSection,
        position = 0
    )
    default ProjectileSwap dragonfireProjectile()
    {
        return ProjectileSwap.NONE;
    }

    @ConfigItem(
        keyName = "awsProjectile",
        name = "Ancient Wyvern Shield projectile",
        description = "Replace the Ancient Wyvern Shield spec projectile with an alternative visual. "
            + "Random picks a different one each time. Rainbow Cycle steps through a colour spectrum on each activation.",
        section = swapSection,
        position = 1
    )
    default ProjectileSwap awsProjectile()
    {
        return ProjectileSwap.NONE;
    }
}
