package com.shieldtimer;

/**
 * Replacement projectile options for the cosmetic swap feature.
 *
 * IDs sourced from the Projectile Override plugin by Loze-Put:
 *   https://github.com/Loze-Put/projectile-override
 *
 * Swap technique adapted from the Chinbompa plugin by sigterm:
 *   https://github.com/runelite/plugin-hub (search: chinbompa)
 */
public enum ProjectileSwap
{
	NONE                      ("Default",                           -1),

	AKKHA_MAGIC               ("Akkha (magic)",                  2253),
	AKKHA_RANGED              ("Akkha (ranged)",                  2255),

	CERBERUS_MAGIC            ("Cerberus (magic)",                1242),
	CERBERUS_RANGED           ("Cerberus (ranged)",               1245),

	DAGGANOTH_KINGS_MAGIC     ("Dagganoth Kings (magic)",          162),
	DAGGANOTH_KINGS_RANGED    ("Dagganoth Kings (ranged)",         475),

	DEMONIC_GORILLA_MAGIC     ("Demonic Gorilla (magic)",         1304),
	DEMONIC_GORILLA_RANGED    ("Demonic Gorilla (ranged)",        1302),

	DOOM_MAGIC                ("Doom of Mokhaiotl (magic)",       3379),
	DOOM_RANGED               ("Doom of Mokhaiotl (ranged)",      3380),

	HUEYCOATL_MAGIC           ("Hueycoatl (magic)",               2975),
	HUEYCOATL_RANGED          ("Hueycoatl (ranged)",              2972),

	HUNLLEF_MAGIC             ("Hunllef (magic)",                 1707),
	HUNLLEF_RANGED            ("Hunllef (ranged)",                1711),

	HUNLLEF_CORRUPTED_MAGIC   ("Hunllef Corrupted (magic)",       1708),
	HUNLLEF_CORRUPTED_RANGED  ("Hunllef Corrupted (ranged)",      1712),

	HYDRA_MAGIC               ("Hydra (magic)",                   1662),
	HYDRA_RANGED              ("Hydra (ranged)",                  1663),

	INFERNO_MAGIC             ("Inferno (magic)",                 1380),
	INFERNO_RANGED            ("Inferno (ranged)",                1378),

	KALPHITE_QUEEN_MAGIC      ("Kalphite Queen (magic)",           280),
	KALPHITE_QUEEN_RANGED     ("Kalphite Queen (ranged)",          288),

	KREE_ARRA_MAGIC           ("Kree'arra (magic)",               1200),
	KREE_ARRA_RANGED          ("Kree'arra (ranged)",              1199),

	LEVIATHAN_MAGIC           ("Leviathan (magic)",               2489),
	LEVIATHAN_RANGED          ("Leviathan (ranged)",              2487),

	MANTICORE_MAGIC           ("Manticore (magic)",               2681),
	MANTICORE_RANGED          ("Manticore (ranged)",              2683),

	OLM_MAGIC                 ("Olm (magic)",                     1341),
	OLM_RANGED                ("Olm (ranged)",                    1343),

	SCURRIUS_MAGIC            ("Scurrius (magic)",                2640),
	SCURRIUS_RANGED           ("Scurrius (ranged)",               2642),

	SOTETSEG_MAGIC            ("Sotetseg (magic)",                1606),
	SOTETSEG_RANGED           ("Sotetseg (ranged)",               1607),

	TORMENTED_DEMON_MAGIC     ("Tormented Demon (magic)",         2853),
	TORMENTED_DEMON_RANGED    ("Tormented Demon (ranged)",        2857),

	VARDORVIS_MAGIC           ("Vardorvis (magic)",               2520),
	VARDORVIS_RANGED          ("Vardorvis (ranged)",              2521),

	WARDENS_MAGIC             ("Wardens (magic)",                 2224),
	WARDENS_RANGED            ("Wardens (ranged)",                2241),

	WARDENS_DIVINE_MAGIC      ("Wardens Divine (magic)",          2208),
	WARDENS_DIVINE_RANGED     ("Wardens Divine (ranged)",         2206),

	WHISPERER_MAGIC           ("Whisperer (magic)",               2445),
	WHISPERER_RANGED          ("Whisperer (ranged)",              2444),

	ZEBAK_MAGIC               ("Zebak (magic)",                   2181),

	ZULRAH_MAGIC              ("Zulrah (magic)",                  1046),
	ZULRAH_RANGED             ("Zulrah (ranged)",                 1044);

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

	@Override
	public String toString()
	{
		return displayName;
	}
}
