package com.flowpvp.mixin;

import com.flowpvp.client.FlowPvPClient;
import com.flowpvp.client.config.ModConfig;
import com.flowpvp.client.data.PlayerStats;
import com.flowpvp.client.util.RankedMatchDetector;
import com.flowpvp.client.util.TierTextBuilder;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void flowpvp$appendTier(PlayerListEntry entry,
                                    CallbackInfoReturnable<Text> cir) {
        if (!ModConfig.INSTANCE.showTierInTabList) return;

        if (ModConfig.INSTANCE.suppressInRankedMatch) {
            if (RankedMatchDetector.isInRankedMatch()) return;
            if (RankedMatchDetector.nameAlreadyHasTierInfo(cir.getReturnValue())) return;
        }

        String rawName = cir.getReturnValue().getString();
        if (rawName != null && rawName.matches("^\\d{2,5}[\\s|].*")) return;

        //? if >=1.21.9 {
        /*UUID uuid = entry.getProfile().id();*/
        //?} else {
        UUID uuid = entry.getProfile().getId();
        //?}
        if (uuid == null) return;

        PlayerStats stats = FlowPvPClient.STATS_CACHE.getCachedByUuid(uuid);
        if (stats == null) {
            FlowPvPClient.STATS_CACHE.getStatsByUuid(uuid);
            return;
        }

        Text tierText = TierTextBuilder.build(stats);

        MutableText modified;
        if (ModConfig.INSTANCE.eloAlignment == com.flowpvp.client.config.NametagEloAlignment.LEFT) {
            modified = Text.empty()
                    .append(tierText)
                    .append(Text.literal("  ").setStyle(Style.EMPTY.withColor(0xFFFFFF)))
                    .append(cir.getReturnValue());
        } else {
            modified = cir.getReturnValue().copy()
                    .append(Text.literal("  ").setStyle(Style.EMPTY.withColor(0xFFFFFF)))
                    .append(tierText);
        }

        cir.setReturnValue(modified);
    }
}
