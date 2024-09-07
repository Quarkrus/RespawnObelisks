package com.redpxnda.respawnobelisks.registry.block.entity.theme;

import com.redpxnda.nucleus.client.Rendering;
import com.redpxnda.nucleus.math.ParticleShaper;
import com.redpxnda.respawnobelisks.config.RespawnObelisksConfig;
import com.redpxnda.respawnobelisks.registry.ModRegistries;
import com.redpxnda.respawnobelisks.registry.block.RespawnObeliskBlock;
import com.redpxnda.respawnobelisks.registry.block.entity.RespawnObeliskBlockEntity;
import com.redpxnda.respawnobelisks.registry.block.entity.theme.ThemeLayout.ThemeData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.redpxnda.nucleus.client.Rendering.addVertex;
import static com.redpxnda.nucleus.client.Rendering.lerpColors;
import static com.redpxnda.respawnobelisks.registry.ModRegistries.rl;
import static com.redpxnda.respawnobelisks.registry.block.entity.RespawnObeliskBER.RUNES;
import static com.redpxnda.respawnobelisks.registry.block.entity.RespawnObeliskBER.RUNE_SPRITE;
import static com.redpxnda.respawnobelisks.util.RenderUtils.*;

@FunctionalInterface
public interface RenderTheme {
    Map<Identifier, RenderTheme> themes = new HashMap<>();
    Random rdm = new Random();

    Identifier defCharge = rl("default_charge");
    Identifier defDep = rl("default_deplete");
    Identifier defRunes = rl("default_runes");
    Identifier sculk = rl("sculk");
    Identifier blazing = rl("blazing");
    Identifier angel = rl("angel");
    Identifier blueSpiral = rl("blue_spiral");

    static void init() {
        register(defCharge, (be, pt, ps, bs, pl, po) -> {
            World level = be.getWorld();
            if (level == null) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            ThemeData data = be.themeLayout.get(defCharge);
            BlockPos pos = be.getPos();
            if (mc.player != null) {
                if (
                        rdm.nextDouble() > 0.95 &&
                        mc.crosshairTarget instanceof BlockHitResult bhr &&
                        (be.getPos().equals(bhr.getBlockPos()) || be.getPos().equals(bhr.getBlockPos().down())) &&
                        RespawnObelisksConfig.INSTANCE.radiance.chargingItems.containsKey(mc.player.getMainHandStack().getItem())
                ) {
                    tickLoopedExecution(be, data, "defaultCharge", blockEntity -> {
                        if (be.getLastCharge() < level.getTime() - 50) {
                            double rX = rdm.nextDouble(6) - 3;
                            double rY = rdm.nextDouble(1.75);
                            double rZ = rdm.nextDouble(6) - 3;
                            level.addParticle(ModRegistries.chargeIndicatorParticle.get(), pos.getX() + 0.5 + rX, pos.getY() + rY, pos.getZ() + 0.5 + rZ, pos.getX()+0.5, pos.getY() + rY, pos.getZ()+0.5);
                        }
                    });
                }
            }
            timedExecution(be, data, be.getLastCharge(), "defaultCharge-main", x -> {
                level.addParticle(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY()+0.1, pos.getZ() + 0.5, 0, 0, 0);
                level.playSound(
                        pos.getX(), pos.getY(), pos.getZ(),
                        Registries.SOUND_EVENT.getOrEmpty(new Identifier(RespawnObelisksConfig.INSTANCE.radiance.chargingSound)).orElse(SoundEvents.UI_BUTTON_CLICK.value()), SoundCategory.BLOCKS,
                        1, 1, false
                );
            });
        });
        register(defDep, new BasicDepleteAnimation(defDep, "time", be -> {
            World level = be.getWorld();
            BlockPos pos = be.getPos();
            assert level != null : "Level is somehow null in BasicDepleteAnimation";
            level.playSound(
                    pos.getX(), pos.getY(), pos.getZ(),
                    Registries.SOUND_EVENT.getOrEmpty(new Identifier(RespawnObelisksConfig.INSTANCE.radiance.depletingSound)).orElse(SoundEvents.UI_BUTTON_CLICK.value()), SoundCategory.BLOCKS,
                    1, 1, false
            );
            level.addParticle(ModRegistries.depleteRingParticle.get(), pos.getX()+0.5, pos.getY()+1.05, pos.getZ()+0.5, 0, 0, 0);
        }));
        register(defRunes, (be, pt, ps, bs, pl, po) -> {
            if (RUNE_SPRITE == null) RUNE_SPRITE = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(RUNES);
            renderRunes(RUNE_SPRITE, be, pt, ps, bs, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        });
        register(sculk, (be, pt, ps, bs, pl, po) -> {
            World level = be.getWorld();
            if (level == null) return;
            BlockPos pos = be.getPos();
            ThemeData data = be.themeLayout.get(sculk);

            tickLoopedExecution(be, data, "tick", x -> {
                double charge = data.getDouble("lastCharge", be.getClientCharge());
                if (charge != be.clientCharge) {
                    if (charge > be.clientCharge) {
                        if (rdm.nextBoolean())
                            level.playSound(
                                    pos.getX(), pos.getY(), pos.getZ(),
                                    SoundEvents.BLOCK_SCULK_BREAK, SoundCategory.BLOCKS,
                                    0.5f, 1, false
                            );
                        charge = Math.max(charge - 0.5, be.clientCharge);
                    } else {
                        if (be.getLastCharge()+10 <= be.getGameTime() && rdm.nextBoolean()) {
                            level.playSound(
                                    pos.getX(), pos.getY(), pos.getZ(),
                                    SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.BLOCKS,
                                    1, 1, false
                            );
                            level.addParticle(
                                    ParticleTypes.SCULK_CHARGE_POP,
                                    pos.getX() + rdm.nextDouble(2) - 1,
                                    pos.getY() + rdm.nextDouble(2),
                                    pos.getZ() + rdm.nextDouble(2) - 1,
                                    0, 0, 0
                            );
                        }
                        charge = Math.min(charge + 0.5, be.clientCharge);
                    }
                    data.put("lastCharge", charge);
                }
            });
            timedExecution(be, data, be.getLastCharge(), "charging", x -> level.playSound(
                    pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.BLOCKS,
                    25, 1, false
            ));
            renderSculkTendrils(be, ps, bs, pl);
            renderSculkOverlay(Rendering.alphaAnimation, be, data.getDouble("lastCharge", be.getClientCharge()), pt, ps, bs, pl);
        });
        register(blazing, new MultipartAnimation(blazing, be -> {
            World level = be.getWorld();
            assert level != null;
            BlockPos pos = be.getPos();

            if (rdm.nextDouble() > 0.95) {
                int count = randomInt(rdm, 3, 6);
                double yStart = randomDouble(rdm, 0.1, 0.2);
                double yInc = randomDouble(rdm, 0.7, 0.75);
                ParticleShaper.square(ParticleTypes.FLAME, 2, count, 1).fromClient().runAt(level, pos.getX()+0.5, pos.getY()+1.5, pos.getZ()+0.5);
            }
        }, be -> {
            World level = be.getWorld();
            assert level != null;
            BlockPos pos = be.getPos();

            ParticleShaper.square(ParticleTypes.FLAME, 3, 100, 1).fromClient().runAt(level, pos.getX()+0.5, pos.getY()+1.5, pos.getZ()+0.5);
            level.playSound(
                    pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.BLOCKS,
                    1, 1, false
            );
        }, be -> {
            World level = be.getWorld();
            assert level != null;
            BlockPos pos = be.getPos();

            //ParticleShaper.expandingSquare(ParticleTypes.FLAME, 3, 100, 1, -0.125).fromClient().runAt(level, pos.getX()+1.5, pos.getY()+1.5, pos.getZ()+0.5);
            level.playSound(
                    pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.ENTITY_BLAZE_DEATH, SoundCategory.BLOCKS,
                    1, 1, false
            );
        }, (be, pt, ps, bs, pl, po) -> {
            World level = be.getWorld();
            assert level != null;

            renderBlaze(be, pt, ps, bs);
            if (RUNE_SPRITE == null) RUNE_SPRITE = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(RUNES);
            renderRunes(RenderLayer.getTranslucent(), RUNE_SPRITE, lerpColors(level.getTime(), 100, new float[][] {
                    { 255, 50, 0 },
                    { 255, 175, 0 }
            }), be, pt, ps, bs, pl);
        }));
        register(angel, (be, pt, ps, bs, pl, po) -> {
            if (angelSprite == null) angelSprite = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(rl("block/wings"));
            VertexConsumer vc = bs.getBuffer(RenderLayer.getCutout());
            ps.push();
            ps.translate(0.5f, 0.5f + (1 - Math.sin(Rendering.getGameAndPartialTime()/16))/16, 0.5f);

            int direction = 0;
            switch (be.getCachedState().get(RespawnObeliskBlock.RESPAWN_SIDE)) {
                case EAST -> direction = 1;
                case SOUTH -> direction = 2;
                case WEST -> direction = 3;
            }
            ps.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90*direction)); // rotating by spawn direction

            ps.push();
            ps.translate(0.4, 0, 0);
            ps.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) (Math.sin(Rendering.getGameAndPartialTime()/16)*30)));

            // first side
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 17/16f, 0, 0, angelSprite.getFrameU(3.5), angelSprite.getFrameV(14), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 17/16f, 23/16f, 0, angelSprite.getFrameU(3.5), angelSprite.getFrameV(2.5), LightmapTextureManager.MAX_LIGHT_COORDINATE); // since image is 32x32, 6.5 = pixel 13 (x2)
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 0, 23/16f, 0, angelSprite.getFrameU(12), angelSprite.getFrameV(2.5), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 0, 0, 0, angelSprite.getFrameU(12), angelSprite.getFrameV(14), LightmapTextureManager.MAX_LIGHT_COORDINATE);

            // second side
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 17/16f, 23/16f, 0, angelSprite.getFrameU(3.5), angelSprite.getFrameV(2.5), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 17/16f, 0, 0, angelSprite.getFrameU(3.5), angelSprite.getFrameV(14), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 0, 0, 0, angelSprite.getFrameU(12), angelSprite.getFrameV(14), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 0, 23/16f, 0, angelSprite.getFrameU(12), angelSprite.getFrameV(2.5), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            ps.pop();

            ps.push();
            ps.translate(-0.4f, 0, 0); // other wing
            ps.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (Math.sin(Rendering.getGameAndPartialTime()/16)*30)));

            // first side
            addVertex(ps, vc, 1f, 1f, 1f, 1f, -17/16f, 23/16f, 0, angelSprite.getFrameU(3.5), angelSprite.getFrameV(2.5), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, -17/16f, 0, 0, angelSprite.getFrameU(3.5), angelSprite.getFrameV(14), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 0, 0, 0, angelSprite.getFrameU(12), angelSprite.getFrameV(14), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 0, 23/16f, 0, angelSprite.getFrameU(12), angelSprite.getFrameV(2.5), LightmapTextureManager.MAX_LIGHT_COORDINATE);

            // second side
            addVertex(ps, vc, 1f, 1f, 1f, 1f, -17/16f, 0, 0, angelSprite.getFrameU(3.5), angelSprite.getFrameV(14), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, -17/16f, 23/16f, 0, angelSprite.getFrameU(3.5), angelSprite.getFrameV(2.5), LightmapTextureManager.MAX_LIGHT_COORDINATE); // since image is 32x32, 6.5 = pixel 13 (x2)
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 0, 23/16f, 0, angelSprite.getFrameU(12), angelSprite.getFrameV(2.5), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            addVertex(ps, vc, 1f, 1f, 1f, 1f, 0, 0, 0, angelSprite.getFrameU(12), angelSprite.getFrameV(14), LightmapTextureManager.MAX_LIGHT_COORDINATE);
            ps.pop();

            ps.pop();
        });
        register(blueSpiral, (be, pt, ps, bs, pl, po) -> {
            World level = be.getWorld();
            if (level == null) return;
            BlockPos pos = be.getPos();
            ThemeData data = be.themeLayout.get(blueSpiral);

            tickLoopedExecution(be, data, "tick", x -> {
                int animationPosition = data.getInt("animationPosition", 0);
                if (animationPosition < 900) {
                    if (rdm.nextBoolean()) {
                        level.playSound(
                                pos.getX(), pos.getY(), pos.getZ(),
                                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS,
                                2, 1, false
                        );
                    }
                    double radians = animationPosition * Math.PI / 180;
                    level.addParticle(new DustParticleEffect(Vec3d.unpackRgb(0x00c8ff).toVector3f(), 1f), pos.getX() + Math.sin(radians) * 0.75 + 0.5, pos.getY() + animationPosition / 360f, pos.getZ() + Math.cos(radians) * 0.75 + 0.5, Math.sin(radians) / 20, 0, Math.cos(radians) / 20);
                    animationPosition+=20;
                    data.put("animationPosition", animationPosition);
                }
            });
            timedExecution(be, data, be.getLastCharge(), "charging", x -> data.put("animationPosition", 0));
        });
    }

    static void register(Identifier name, RenderTheme theme) {
        themes.put(name, theme);
    }

    static void timedExecution(RespawnObeliskBlockEntity blockEntity, ThemeData data, long checkAgainst, String name, BlockEntityOnly handler, BlockEntityOnly ifFailed) {
        long gameTime = blockEntity.getGameTime();
        float renderProgress = data.getFloat(name, 0f);
        if (gameTime-3 <= checkAgainst && renderProgress < 1f) {
            handler.call(blockEntity);
            data.put(name, 1f);
        } else if (gameTime-3 > checkAgainst && renderProgress >= 1f) {
            data.put(name, 0f);
            ifFailed.call(blockEntity);
        }
    }
    static void timedExecution(RespawnObeliskBlockEntity blockEntity, ThemeData data, long checkAgainst, String name, BlockEntityOnly handler) {
        timedExecution(blockEntity, data, checkAgainst, name, handler, be -> {});
    }

    static void tickLoopedExecution(RespawnObeliskBlockEntity blockEntity, ThemeData data, String name, BlockEntityOnly handler) {
        long gameTime = blockEntity.getGameTime();
        long checkAgainst = data.getLong(name, 0L);
        if (gameTime != checkAgainst) {
            handler.call(blockEntity);
            data.put(name, gameTime);
        }
    }

    void render(RespawnObeliskBlockEntity blockEntity, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay);

    @FunctionalInterface
    interface BlockEntityOnly extends RenderTheme {
        void call(RespawnObeliskBlockEntity blockEntity);

        default void render(RespawnObeliskBlockEntity blockEntity, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
            call(blockEntity);
        }
    }
}
