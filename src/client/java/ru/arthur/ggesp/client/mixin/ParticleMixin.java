package ru.arthur.ggesp.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.arthur.ggesp.client.GhostTracker;

import java.util.Set;

@Mixin(ClientPlayNetworkHandler.class)
public class ParticleMixin {

    @Unique
    private static final Set<ParticleType<?>> PLAYER_PARTICLES = Set.of(
        ParticleTypes.CRIT,
        ParticleTypes.ENCHANTED_HIT,
        ParticleTypes.DAMAGE_INDICATOR,
        ParticleTypes.SWEEP_ATTACK,
        ParticleTypes.TOTEM_OF_UNDYING,
        ParticleTypes.EFFECT,
        ParticleTypes.INSTANT_EFFECT,
        ParticleTypes.ENTITY_EFFECT,
        ParticleTypes.SPLASH,
        ParticleTypes.WITCH,
        ParticleTypes.PORTAL,
        ParticleTypes.SMOKE,
        ParticleTypes.LARGE_SMOKE,
        ParticleTypes.CLOUD,
        ParticleTypes.POOF,
        ParticleTypes.EXPLOSION,
        ParticleTypes.ITEM
    );

    @Inject(method = "onParticle", at = @At("RETURN"))
    private void ggesp$onParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!GhostTracker.isEnabled()) return;

        ParticleEffect effect = packet.getParameters();
        if (effect == null) return;

        ParticleType<?> type = effect.getType();
        if (!PLAYER_PARTICLES.contains(type)) return;

        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        double distSq = client.player.squaredDistanceTo(x, y, z);
        if (distSq < 9.0) return;

        String typeName = type.toString();
        GhostTracker.addGhost(x, y, z, "Particle: " + typeName);
    }
}
