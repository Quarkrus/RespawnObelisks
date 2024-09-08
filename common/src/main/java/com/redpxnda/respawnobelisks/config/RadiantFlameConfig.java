package com.redpxnda.respawnobelisks.config;

import com.redpxnda.nucleus.codec.auto.ConfigAutoCodec;
import com.redpxnda.nucleus.util.Comment;

@ConfigAutoCodec.ConfigClassMarker
public class RadiantFlameConfig {
    @Comment("How many ticks players have to wait before they can place another radiant flame.")
    public int placementCooldown = 18000;

    @Comment("How long radiant flames can exist before automatically despawning. Set to -1 to allow flames to exist forever.")
    public int lifetime = 12000;

    @Comment("How much radiance flames should lose each second.")
    public double radianceReduction = 0;

    @Comment("Multiplier for the radiance radiant lanterns take from obelisks. Ex. 0.5 would make it so that an obelisk with 100 radiance only fills the lantern up to 50.")
    public double radianceEfficiency = 0.6;

    @Comment("The maximum amount of radiance radiant lanterns can take from obelisks. (before the radiance efficiency multiplier is applied)")
    public double maxLanternRadiance = 100;

    @Comment("Whether radiant flames are bound to the player that placed them.")
    public boolean playerBound = true;

    @Comment("Whether radiant lanterns can be reused.")
    public boolean allowMultipleUses = false;
}
