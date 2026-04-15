package com.shieldtimer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Replacement projectile options for the cosmetic swap feature.
 *
 * Boss projectile IDs sourced from the Projectile Override plugin by Loze-Put:
 *   https://github.com/Loze-Put/projectile-override
 *
 * Additional IDs researched via the RuneMonk entity viewer:
 *   https://runemonk.com/tools/entityviewer-beta
 *
 * Swap technique adapted from the Chinbompa plugin by sigterm:
 *   https://github.com/runelite/plugin-hub (search: chinbompa)
 */
public enum ProjectileSwap
{
	// -------------------------------------------------------------------------
	// Special options
	// -------------------------------------------------------------------------
	NONE             ("Default",        -1),
	RANDOM           ("Random",         -2),
	RAINBOW          ("Rainbow Cycle",  -3),

	// -------------------------------------------------------------------------
	// Boss projectiles — sourced from Loze-Put/projectile-override
	// -------------------------------------------------------------------------
	AKKHA_MAGIC               ("Red-2253",          2253),
	AKKHA_RANGED              ("Gold-2255",         2255),

	CERBERUS_MAGIC            ("Grey-1242",         1242),
	CERBERUS_RANGED           ("Grey-1245",         1245),

	DEMONIC_GORILLA_MAGIC     ("Green-1304",        1304),
	DEMONIC_GORILLA_RANGED    ("Grey-1302",         1302),

	DOOM_MAGIC                ("Blue-3379",         3379),
	DOOM_RANGED               ("Green-3380",        3380),

	HUEYCOATL_MAGIC           ("Black-2975",        2975),
	HUEYCOATL_RANGED          ("Green-2972",        2972),

	HUNLLEF_MAGIC             ("Cyan-1707",         1707),
	HUNLLEF_RANGED            ("Cyan-1711",         1711),

	HUNLLEF_CORRUPTED_MAGIC   ("Red-1708",          1708),
	HUNLLEF_CORRUPTED_RANGED  ("Red-1712",          1712),

	HYDRA_RANGED              ("Olive-1663",        1663),

	INFERNO_MAGIC             ("Orange-1380",       1380),
	INFERNO_RANGED            ("Red-1378",          1378),

	KALPHITE_QUEEN_RANGED     ("Green-288",          288),

	LEVIATHAN_MAGIC           ("Cyan-2489",         2489),
	LEVIATHAN_RANGED          ("Green-2487",        2487),

	MANTICORE_MAGIC           ("Cyan-2681",         2681),
	MANTICORE_MELEE           ("Red-2685",          2685),

	OLM_MAGIC                 ("Purple-1341",       1341),
	OLM_RANGED                ("Green-1343",        1343),

	SOTETSEG_MAGIC            ("Red-1606",          1606),

	TORMENTED_DEMON_MAGIC     ("Gold-2853",         2853),

	VARDORVIS_MAGIC           ("Black-2520",        2520),
	VARDORVIS_RANGED          ("Black-2521",        2521),

	WARDENS_MAGIC             ("Teal-2224",         2224),
	WARDENS_RANGED            ("Brown-2241",        2241),

	WARDENS_DIVINE_MAGIC      ("Blue-2208",         2208),

	WHISPERER_MAGIC           ("Cyan-2445",         2445),
	WHISPERER_RANGED          ("Purple-2444",       2444),

	ZEBAK_MAGIC               ("Red-2181",          2181),

	ZULRAH_MAGIC              ("Amber-1046",        1046),
	ZULRAH_RANGED             ("Teal-1044",         1044),

	// -------------------------------------------------------------------------
	// Additional projectiles — researched via RuneMonk entity viewer
	// -------------------------------------------------------------------------
	SMALL_FIREBALL            ("Orange-88",           88),

	RED_SPIKED_CRYSTAL        ("Red-1667",          1667),
	RED_DIAMOND               ("Red-1578",          1578),
	DARK_RED_DIAMOND          ("Red-1682",          1682),
	RED_DEBRIS                ("Red-2002",          2002),
	RED_STARBURST             ("Red-2246",          2246),
	GREEN_EXPLOSION           ("Green-2684",        2684),
	RED_SKULL                 ("Red-2340",          2340),
	BLOOD_SPHERE              ("Red-1784",          1784),

	YELLOW_ORB                ("Yellow-1522",       1522),
	GOLD_TEARDROP             ("Gold-2339",         2339),
	GOLD_CROWN_PORTAL         ("Gold-2632",         2632),
	GOLD_FLAMING_SKULL        ("Gold-2855",         2855),
	GOLDEN_LEAF               ("Gold-3321",         3321),

	GREEN_FLAME               ("Green-1349",        1349),
	DARK_GREEN_ORB            ("Green-1470",        1470),
	GREEN_SPHERE              ("Green-2167",        2167),
	EARTH_ROCK                ("Brown-2915",        2915),

	CYAN_SPHERE               ("Cyan-1479",         1479),
	DARK_TEAL_CRYSTAL         ("Teal-1497",         1497),
	BLUE_CRYSTAL_CHUNK        ("Blue-2434",         2434),
	CYAN_BUBBLE_CLUSTER       ("Cyan-3316",         3316),

	BLUE_VORTEX               ("Blue-1459",         1459),
	BLUE_FLAME                ("Blue-2917",         2917),
	BLUE_CRYSTAL_WAVES        ("Blue-3141",         3141),

	PURPLE_DUAL_ORBS          ("Purple-1327",       1327),
	PURPLE_DIAMOND            ("Purple-1735",       1735),
	PURPLE_FLAME              ("Purple-2007",       2007),

	PINK_SPIKED_CRYSTAL       ("Pink-1471",         1471),
	PINK_FLAME                ("Pink-1764",         1764);

	// -------------------------------------------------------------------------
	// Rainbow sequence — one representative per hue band, spectrum-ordered.
	// The plugin cycles through these IDs on successive activations.
	// -------------------------------------------------------------------------
	static final int[] RAINBOW_SEQUENCE = {
		2685,  // Red-2685
		1380,  // Orange-1380
		1522,  // Yellow-1522
		3380,  // Green-3380
		2487,  // Green-2487
		2681,  // Cyan-2681
		3379,  // Blue-3379
		1327,  // Purple-1327
		1471,  // Pink-1471
	};

	private final String displayName;
	private final int projectileId;

	ProjectileSwap(String displayName, int projectileId)
	{
		this.displayName = displayName;
		this.projectileId = projectileId;
	}

	public int getProjectileId()
	{
		return projectileId;
	}

	/** Returns a random swap from all entries with a real projectile ID. */
	public static ProjectileSwap random()
	{
		List<ProjectileSwap> pool = new ArrayList<>();
		for (ProjectileSwap s : values())
		{
			if (s.projectileId > 0)
			{
				pool.add(s);
			}
		}
		return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
