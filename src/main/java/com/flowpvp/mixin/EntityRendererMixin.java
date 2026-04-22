package com.flowpvp.mixin;

import com.flowpvp.client.FlowPvPClient;
import com.flowpvp.client.config.ModConfig;
import com.flowpvp.client.data.PlayerStats;
import com.flowpvp.client.data.RankedLadder;
import com.flowpvp.client.util.RankedMatchDetector;
import com.flowpvp.client.util.TierTextBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
//? if >=1.21.2 {
import net.minecraft.client.render.entity.state.EntityRenderState;
//?}
//? if >=1.21.9 {
/*import net.minecraft.client.render.entity.state.PlayerEntityRenderState;*/
//?}
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.UUID;

@Mixin(net.minecraft.client.render.entity.EntityRenderer.class)
public abstract class EntityRendererMixin {

    // =========================================================================
    // 1.21.10+ (OrderedRenderCommandQueue API)
    // =========================================================================

    //? if >=1.21.9 {
    /*
    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/Entity;" +
                 "Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
        at = @At("RETURN")
    )
    private void flowpvp$modifyNametag19(Entity entity, EntityRenderState state,
                                          float tickDelta, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.showTierAboveHead) return;
        if (!(entity instanceof PlayerEntity player)) return;
        if (state.nameLabelPos == null) return;

        // Avoid duplicate tier/ELO when FlowPvP is already showing it above heads.
        if (ModConfig.INSTANCE.suppressInRankedMatch) {
            if (RankedMatchDetector.isInRankedMatch()) return;
            if (state.displayName != null
                    && RankedMatchDetector.nameAlreadyHasTierInfo(state.displayName)) return;
        }

        UUID uuid = player.getUuid();
        PlayerStats stats = FlowPvPClient.STATS_CACHE.getCachedByUuid(uuid);
        if (stats == null) {
            FlowPvPClient.STATS_CACHE.scheduleStatsByUuid(uuid);
            return;
        }

        Text tierText = TierTextBuilder.build(stats);
        if (state.displayName != null) {
            if (ModConfig.INSTANCE.eloAlignment == com.flowpvp.client.config.NametagEloAlignment.LEFT) {
                state.displayName = net.minecraft.text.Text.empty()
                        .append(tierText)
                        .append(net.minecraft.text.Text.literal("  ").setStyle(Style.EMPTY.withColor(0xFFFFFF)))
                        .append(state.displayName);
            } else {
                state.displayName = net.minecraft.text.Text.empty()
                        .append(state.displayName)
                        .append(net.minecraft.text.Text.literal("  ").setStyle(Style.EMPTY.withColor(0xFFFFFF)))
                        .append(tierText);
            }
        }
    }
    */
    //?}

    // =========================================================================
    // 1.21.2–1.21.9 (render state API with VertexConsumerProvider)
    // =========================================================================

    //? if >=1.21.2 && <1.21.9 {
    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/Entity;" +
                    "Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void flowpvp$modifyNametag(Entity entity, EntityRenderState state,
                                       float tickDelta, CallbackInfo ci) {
        if (!ModConfig.INSTANCE.showTierAboveHead) return;
        if (!(entity instanceof PlayerEntity player)) return;
        if (state.displayName == null) return;

        // ---- Avoid duplicate tier/ELO above heads inside a FlowPvP ranked match ----
        if (ModConfig.INSTANCE.suppressInRankedMatch) {
            if (RankedMatchDetector.isInRankedMatch()) return;
            if (RankedMatchDetector.nameAlreadyHasTierInfo(state.displayName)) return;
        }

        UUID uuid = player.getUuid();
        PlayerStats stats = FlowPvPClient.STATS_CACHE.getCachedByUuid(uuid);
        if (stats == null) {
            FlowPvPClient.STATS_CACHE.scheduleStatsByUuid(uuid);
            return;
        }

        Text tierText = TierTextBuilder.build(stats);

        if (ModConfig.INSTANCE.eloAlignment == com.flowpvp.client.config.NametagEloAlignment.LEFT) {
            state.displayName = net.minecraft.text.Text.empty()
                    .append(tierText)
                    .append(net.minecraft.text.Text.literal("  ").setStyle(Style.EMPTY.withColor(0xFFFFFF)))
                    .append(state.displayName);
        } else {
            state.displayName = net.minecraft.text.Text.empty()
                    .append(state.displayName)
                    .append(net.minecraft.text.Text.literal("  ").setStyle(Style.EMPTY.withColor(0xFFFFFF)))
                    .append(tierText);
        }
    }
//?}

    // =========================================================================
    // 1.21 / 1.21.1 (legacy entity-based API)
    // =========================================================================

    //? if <1.21.2 {
    /*@ModifyArgs(
        method = "render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/render/entity/EntityRenderer;renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V")
    )
    private void flowpvp$modifyLabelText121(Args args) {
        Entity entity = args.get(0);
        Text text = args.get(1);
        if (!(entity instanceof PlayerEntity player)) return;
        if (!ModConfig.INSTANCE.showTierAboveHead) return;
        if (text == null || !text.getString().contains(player.getName().getString())) return;

        if (ModConfig.INSTANCE.suppressInRankedMatch) {
            if (RankedMatchDetector.isInRankedMatch()) return;
            if (RankedMatchDetector.nameAlreadyHasTierInfo(text)) return;
        }

        UUID uuid = player.getUuid();
        PlayerStats stats = FlowPvPClient.STATS_CACHE.getCachedByUuid(uuid);
        if (stats == null) {
            FlowPvPClient.STATS_CACHE.scheduleStatsByUuid(uuid);
            return;
        }

        Text tierText = TierTextBuilder.build(stats);
        net.minecraft.text.MutableText combined;
        if (ModConfig.INSTANCE.eloAlignment == com.flowpvp.client.config.NametagEloAlignment.LEFT) {
            combined = net.minecraft.text.Text.empty()
                    .append(tierText)
                    .append(net.minecraft.text.Text.literal("  ").setStyle(Style.EMPTY.withColor(0xFFFFFF)))
                    .append(text);
        } else {
            combined = net.minecraft.text.Text.empty()
                    .append(text)
                    .append(net.minecraft.text.Text.literal("  ").setStyle(Style.EMPTY.withColor(0xFFFFFF)))
                    .append(tierText);
        }
        args.set(1, combined);
    }*/
    //?}

}