package com.example.mobconversion.util;

import com.example.mobconversion.config.MobConversionConfig;
import com.example.mobconversion.config.EntityPoolEntry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobConversionManager {
    private static final Logger LOGGER = LogManager.getLogger("MobConversion");
    private static final ConcurrentHashMap<UUID, Long> SCAN_COOLDOWNS = new ConcurrentHashMap<>();
    private static final List<ParticleEntry> PARTICLES = new ArrayList<>();

    private MobConversionManager() {}

    public static void onLivingEntityTick(ServerLevel level, LivingEntity entity) {
        long time = level.getGameTime();
        UUID uuid = entity.getUUID();

        if (time < SCAN_COOLDOWNS.getOrDefault(uuid, 0L)) return;
        SCAN_COOLDOWNS.put(uuid, time + MobConversionConfig.SCAN_COOLDOWN_TICKS.get());

        if (!VillageHelper.isHostileMobNearby(level, entity)) {
            return;
        }

        Optional<LivingEntity> target = ConversionHelper.selectConversionTarget(level, entity);
        if (target.isPresent()) {
            Optional<EntityPoolEntry> entry = EntityPoolManager.select(level, target.get(), entity);
            if (entry.isPresent()) {
                performConversion(level, target.get(), entry.get());
            } else {
                // If no entry was selected, it might be due to village scope or chance
                LOGGER.debug("No valid entry selected for target {} at {}", target.get().getType(), entity.blockPosition());
            }
        }
    }

    public static void onLivingEntityAttacked(ServerLevel level, LivingEntity entity, Entity attacker) {
        if (!entity.isAlive()) return;
        if (attacker == null) return;
        if (isExemptAttacker(level, entity, attacker)) return;

        Optional<LivingEntity> target = ConversionHelper.selectConversionTarget(level, entity);
        if (target.isPresent()) {
            Optional<EntityPoolEntry> entry = EntityPoolManager.select(level, target.get(), entity);
            if (entry.isPresent()) {
                performConversion(level, target.get(), entry.get());
            } else {
                LOGGER.debug("No valid entry selected after attack for target {} at {}", target.get().getType(), entity.blockPosition());
            }
        }
    }

    public static void performConversion(ServerLevel level, LivingEntity entityToConvert, EntityPoolEntry entry) {
        Optional<EntityType<?>> optType = entry.getEntityType();
        if (optType.isEmpty()) return;

        EntityType<?> type = optType.get();
        Entity newEntity = type.create(level);
        if (newEntity != null) {
            newEntity.moveTo(entityToConvert.getX(), entityToConvert.getY(), entityToConvert.getZ(), entityToConvert.getYRot(), entityToConvert.getXRot());
            
            if (newEntity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.CONVERSION, null);
            }
            
            boolean convert = entry.mode() == MobConversionConfig.ConversionMode.CONVERT;
            if (convert) {
                entityToConvert.discard();
                SCAN_COOLDOWNS.remove(entityToConvert.getUUID());
            }
            
            if (!entry.dropLoot()) {
                newEntity.getPersistentData().putBoolean("mobconversion_nodrops", true);
            }
            
            level.addFreshEntity(newEntity);
            LOGGER.info("Entity {} converted/spawned into {} at {}", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entityToConvert.getType()), entry.entityId(), newEntity.blockPosition());
            queueConversionParticles(level, newEntity.getX(), newEntity.getY(), newEntity.getZ());
        }
    }

    public static void clearCooldown(UUID uuid) {
        SCAN_COOLDOWNS.remove(uuid);
        EntityPoolManager.clear(uuid);
    }

    public static void tickParticles() {
        if (PARTICLES.isEmpty()) return;
        Iterator<ParticleEntry> iterator = PARTICLES.iterator();
        while (iterator.hasNext()) {
            ParticleEntry entry = iterator.next();
            if (entry.remainingTicks-- <= 0) {
                iterator.remove();
                continue;
            }
            entry.level.sendParticles(
                    ParticleTypes.POOF,
                    entry.x,
                    entry.y + 0.5,
                    entry.z,
                    6,
                    0.3,
                    0.5,
                    0.3,
                    0.02
            );
        }
    }

    private static void queueConversionParticles(ServerLevel level, double x, double y, double z) {
        if (!MobConversionConfig.CONVERSION_PARTICLES_ENABLED.get()) return;
        int duration = MobConversionConfig.CONVERSION_PARTICLE_DURATION_TICKS.get();
        PARTICLES.add(new ParticleEntry(level, x, y, z, duration));
    }

    private static boolean isExemptAttacker(ServerLevel level, LivingEntity victim, Entity attacker) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType());
        if (id == null) return false;
        if (!MobConversionConfig.EXEMPT_ATTACKER_ENTITY_IDS.get().contains(id.toString())) return false;
        return VillageHelper.isSameVillage(level, victim.blockPosition(), attacker.blockPosition());
    }

    private static final class ParticleEntry {
        private final ServerLevel level;
        private final double x;
        private final double y;
        private final double z;
        private int remainingTicks;

        private ParticleEntry(ServerLevel level, double x, double y, double z, int remainingTicks) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.remainingTicks = remainingTicks;
        }
    }
}
