package com.example.mobconversion.util;

import com.example.mobconversion.config.MobConversionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;

import java.util.List;

public final class VillageHelper {

    private VillageHelper() {}

    public static boolean isConversionAllowedAt(ServerLevel level, LivingEntity entity) {
        MobConversionConfig.VillageScope scope = MobConversionConfig.VILLAGE_SCOPE.get();
        if (scope == MobConversionConfig.VillageScope.ANYWHERE) {
            org.apache.logging.log4j.LogManager.getLogger("MobConversion").info("Global scope is ANYWHERE, allowing conversion.");
            return true;
        }

        boolean isNatural = isInsideNaturalVillage(level, entity.blockPosition());
        boolean isArtificial = isInsideArtificialVillage(level, entity.blockPosition()) && !isNatural;
        boolean hasBell = isBellNearby(level, entity.blockPosition());
        boolean requireBell = MobConversionConfig.REQUIRE_BELL.get();

        boolean allowed = switch (scope) {
            case ANYWHERE -> true;
            case NATURAL -> isNatural && (!requireBell || hasBell);
            case ARTIFICIAL -> isArtificial;
            case BOTH -> (isNatural && (!requireBell || hasBell)) || isArtificial;
            default -> true;
        };

        if (!allowed) {
            org.apache.logging.log4j.LogManager.getLogger("MobConversion")
                    .info("Conversion disallowed at {} for scope {}: natural={}, artificial={}, hasBell={}", 
                            entity.blockPosition(), scope, isNatural, isArtificial, hasBell);
        }
        return allowed;
    }

    public static boolean isInsideNaturalVillage(ServerLevel level, BlockPos pos) {
        var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        
        for (var entry : level.structureManager().getAllStructuresAt(pos).entrySet()) {
            Structure structure = entry.getKey();
            if (level.structureManager().getStructureAt(pos, structure).isValid()) {
                var resourceKey = structureRegistry.getResourceKey(structure);
                if (resourceKey.isPresent()) {
                    var holder = structureRegistry.getHolder(resourceKey.get());
                    if (holder.isPresent() && holder.get().is(StructureTags.VILLAGE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isInsideArtificialVillage(ServerLevel level, BlockPos center) {
        int radius = MobConversionConfig.ARTIFICIAL_VILLAGE_POI_RADIUS.get();
        
        // Optimize: Use PoiManager to find beds instead of manual block scanning
        PoiManager poiManager = level.getPoiManager();
        boolean hasBed = poiManager.findAll(
                holder -> holder.is(PoiTypes.HOME), // HOME is the POI type for beds
                p -> true,
                center,
                radius,
                PoiManager.Occupancy.ANY
        ).findAny().isPresent();

        if (!hasBed) return false;

        // Check for job site
        return poiManager.findAll(
                holder -> holder.is(PoiTypeTags.ACQUIRABLE_JOB_SITE),
                p -> true,
                center,
                radius,
                PoiManager.Occupancy.ANY
        ).findAny().isPresent();
    }

    public static boolean isSameVillage(ServerLevel level, BlockPos a, BlockPos b) {
        int radius = MobConversionConfig.ARTIFICIAL_VILLAGE_POI_RADIUS.get();
        long maxDist = (long) radius * radius;
        if (a.distSqr(b) > maxDist) return false;

        boolean naturalA = isInsideNaturalVillage(level, a);
        boolean naturalB = isInsideNaturalVillage(level, b);
        if (naturalA && naturalB) return true;

        boolean artificialA = isInsideArtificialVillage(level, a) && !naturalA;
        boolean artificialB = isInsideArtificialVillage(level, b) && !naturalB;
        return artificialA && artificialB;
    }

    public static boolean isBellNearby(ServerLevel level, BlockPos center) {
        int radius = MobConversionConfig.BELL_DETECTION_RADIUS.get();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockState(mutable).getBlock() instanceof BellBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isHostileMobNearby(ServerLevel level, LivingEntity entity) {
        int radius = MobConversionConfig.THREAT_DETECTION_RADIUS.get();
        AABB searchBox = entity.getBoundingBox().inflate(radius);
        
        // Optimize: Check if there are ANY monsters first before doing line-of-sight raycasts
        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class, searchBox, e -> e.isAlive() && !e.isSpectator());
        if (hostiles.isEmpty()) return false;

        for (Monster monster : hostiles) {
            if (hasLineOfSight(level, entity, monster)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLineOfSight(ServerLevel level, LivingEntity entity, Monster monster) {
        ClipContext ctx = new ClipContext(
                entity.getEyePosition(),
                monster.getEyePosition(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        );
        BlockHitResult hit = level.clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }
}
