package com.example.mobconversion.util;

import com.example.mobconversion.config.MobConversionConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ConversionHelper {

    private ConversionHelper() {}

    public static Optional<LivingEntity> selectConversionTarget(ServerLevel level, LivingEntity detector) {
        ResourceLocation detectorId = BuiltInRegistries.ENTITY_TYPE.getKey(detector.getType());
        String detectorIdStr = detectorId != null ? detectorId.toString() : "";
        List<String> convertibleIds = (List<String>) MobConversionConfig.CONVERTIBLE_ENTITY_IDS.get();
        boolean isStandardConvertible = convertibleIds.contains(detectorIdStr);
        
        org.apache.logging.log4j.LogManager.getLogger("MobConversion")
                .info("Checking target for detector: {}. isStandardConvertible: {}", detectorIdStr, isStandardConvertible);

        // Optimization: If it's NOT a standard convertible (like a Pig), prioritize self-conversion
        // AND skip searching for neighbors to prevent "stealing" villagers.
        if (!isStandardConvertible) {
            if (MobConversionConfig.ALLOW_SELF_CONVERSION.get() && isEligible(detector)) {
                org.apache.logging.log4j.LogManager.getLogger("MobConversion")
                        .info("Non-standard trigger {} detected threat. Prioritizing self-conversion.", detectorIdStr);
                return Optional.of(detector);
            }
            return Optional.empty(); // Non-standard triggers should only convert themselves.
        }

        // Standard convertible logic (Villagers): Search for nearest neighbor first.
        int radius = MobConversionConfig.THREAT_DETECTION_RADIUS.get();
        AABB searchBox = detector.getBoundingBox().inflate(radius);

        // Optimize: Perform ONE search for all LivingEntities and filter in memory.
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, 
                ent -> ent.isAlive() && !ent.isSpectator() && ent != detector);

        Optional<LivingEntity> nearest = candidates.stream()
                .filter(e -> {
                    ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                    boolean eligible = id != null && convertibleIds.contains(id.toString()) && isEligible(e);
                    if (eligible) {
                        org.apache.logging.log4j.LogManager.getLogger("MobConversion")
                                .info("Found potential neighbor target for {}: {}", detectorIdStr, id);
                    }
                    return eligible;
                })
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(detector)));

        if (nearest.isPresent()) {
            org.apache.logging.log4j.LogManager.getLogger("MobConversion")
                    .info("Selected nearest neighbor {} for conversion (detector: {})", nearest.get().getType(), detectorIdStr);
            return nearest;
        }

        // Fallback to self for villagers.
        if (MobConversionConfig.ALLOW_SELF_CONVERSION.get() && isEligible(detector)) {
            org.apache.logging.log4j.LogManager.getLogger("MobConversion")
                    .info("No neighbors found for villager {}. Converting self.", detectorIdStr);
            return Optional.of(detector);
        }

        return Optional.empty();
    }

    public static boolean isEligible(LivingEntity entity) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) return false;
        
        String idStr = id.toString();
        boolean isConvertible = ((List<String>) MobConversionConfig.CONVERTIBLE_ENTITY_IDS.get()).contains(idStr);
        boolean isTrigger = EntityPoolManager.getAllTriggerIds().contains(idStr);

        if (!isConvertible && !isTrigger) {
            return false;
        }

        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            VillagerProfession profession = villager.getVillagerData().getProfession();

            if (profession == VillagerProfession.NITWIT) {
                return MobConversionConfig.CONVERT_NITWITS.get();
            }

            if (profession == VillagerProfession.NONE) {
                return MobConversionConfig.CONVERT_UNEMPLOYED.get();
            }

            if (MobConversionConfig.CONVERT_UNTRADED_PROFESSIONALS.get()) {
                return villager.getVillagerData().getLevel() <= 1 && villager.getVillagerXp() == 0;
            }
        }

        // For non-vanilla villagers, we can add more checks here if needed.
        // For now, we'll assume they are always eligible if they are on the list.
        return !(entity instanceof Villager);
    }
}
