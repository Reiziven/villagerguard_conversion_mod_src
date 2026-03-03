package com.example.mobconversion.util;

import com.example.mobconversion.config.EntityPoolEntry;
import com.example.mobconversion.config.MobConversionConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class EntityPoolManager {
    private static final Logger LOGGER = LogManager.getLogger("MobConversion");
    private static volatile List<EntityPoolEntry> cached;
    private static volatile Set<String> cachedTriggerIds;
    private static final ConcurrentHashMap<UUID, Map<ResourceLocation, Long>> entryCooldowns = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ResourceLocation, Map<UUID, Long>> sharedCooldowns = new ConcurrentHashMap<>();

    private EntityPoolManager() {}

    public static List<EntityPoolEntry> getEntries() {
        if (cached != null) return cached;
        
        List<? extends String> raw = MobConversionConfig.ENTRIES.get();
        int cap = MobConversionConfig.MAX_ENTRIES.get();
        List<EntityPoolEntry> parsed = raw.stream()
                .limit(cap)
                .map(EntityPoolEntry::parse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(EntityPoolEntry::priority))
                .collect(Collectors.toList());
        cached = parsed;
        cachedTriggerIds = null; // Invalidate trigger IDs cache when entries change
        return cached;
    }

    public static void invalidateCache() {
        cached = null;
        cachedTriggerIds = null;
    }

    public static Set<String> getAllTriggerIds() {
        if (cachedTriggerIds != null) return cachedTriggerIds;
        
        Set<String> all = new HashSet<>((List<String>) MobConversionConfig.CONVERTIBLE_ENTITY_IDS.get());
        for (EntityPoolEntry e : getEntries()) {
            all.addAll(e.triggerEntityIds());
        }
        cachedTriggerIds = all;
        return all;
    }

    public static Optional<EntityPoolEntry> select(ServerLevel level, LivingEntity target, LivingEntity trigger) {
        List<EntityPoolEntry> allEntries = cached != null ? cached : getEntries();
        long now = level.getGameTime();
        ResourceLocation triggerId = BuiltInRegistries.ENTITY_TYPE.getKey(trigger.getType());
        if (triggerId == null) return Optional.empty();

        List<EntityPoolEntry> specificEntries = new ArrayList<>();
        List<EntityPoolEntry> generalEntries = new ArrayList<>();

        for (EntityPoolEntry e : allEntries) {
            if (e.triggerEntityIds().isEmpty()) {
                generalEntries.add(e);
            } else if (e.triggerEntityIds().contains(triggerId.toString())) {
                specificEntries.add(e);
            }
        }

        // Prioritize specific entries. If any exist, only use them.
        List<EntityPoolEntry> candidates = !specificEntries.isEmpty() ? specificEntries : generalEntries;

        for (EntityPoolEntry e : candidates) {
            Optional<EntityType<?>> optType = e.getEntityType();
            if (optType.isEmpty()) continue;

            EntityType<?> type = optType.get();
            if (!isAllowedAt(level, target, e)) continue;
            if (isEntryOnCooldown(target.getUUID(), e.entityId(), now)) continue;
            if (isSharedCooldownActive(level, target, e.entityId(), now)) continue;
            
            int resolvedLimit = resolveLimit(e.maxLimit());
            if (isEntityLimitReached(level, target, type, resolvedLimit)) {
                LOGGER.debug("Limit reached for {}: {} allowed", e.entityId(), resolvedLimit);
                continue;
            }

            if (level.getRandom().nextDouble() <= e.chance()) {
                setCooldowns(level, target, e.entityId(), now + e.cooldownTicks());
                LOGGER.info("Selected entity {} for conversion (chance: {})", e.entityId(), e.chance());
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    private static boolean isSharedCooldownActive(ServerLevel level, LivingEntity target, ResourceLocation id, long now) {
        Map<UUID, Long> cooldownMap = sharedCooldowns.get(id);
        if (cooldownMap == null) return false;

        int radius = MobConversionConfig.SHARED_COOLDOWN_RADIUS.get();
        AABB searchBox = target.getBoundingBox().inflate(radius);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> true);

        for (LivingEntity entity : nearbyEntities) {
            if (cooldownMap.getOrDefault(entity.getUUID(), 0L) > now) {
                return true;
            }
        }

        return false;
    }

    private static void setCooldowns(ServerLevel level, LivingEntity target, ResourceLocation id, long until) {
        // Individual cooldown
        entryCooldowns.computeIfAbsent(target.getUUID(), k -> new ConcurrentHashMap<>()).put(id, until);

        // Shared cooldown
        Map<UUID, Long> cooldownMap = sharedCooldowns.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        cooldownMap.put(target.getUUID(), until);

        int radius = MobConversionConfig.SHARED_COOLDOWN_RADIUS.get();
        AABB searchBox = target.getBoundingBox().inflate(radius);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> true);

        for (LivingEntity entity : nearbyEntities) {
            cooldownMap.put(entity.getUUID(), until);
        }
    }



    public static void clearSharedCooldown(ResourceLocation id, UUID entityId) {
        Map<UUID, Long> cooldownMap = sharedCooldowns.get(id);
        if (cooldownMap != null) {
            cooldownMap.remove(entityId);
        }
    }

    private static int resolveLimit(int entryLimit) {
        // If entry specifies a limit >= 0, use it.
        if (entryLimit >= 0) return entryLimit;
        
        // Otherwise, check defaultMaxLimit
        int defaultLimit = MobConversionConfig.DEFAULT_MAX_LIMIT.get();
        if (defaultLimit >= 0) return defaultLimit;
        
        // Finally, fallback to global area limit
        return MobConversionConfig.MAX_ENTITIES_PER_AREA.get();
    }

    private static boolean isEntityLimitReached(ServerLevel level, LivingEntity entity, EntityType<?> type, int maxEntities) {
        if (maxEntities < 0) return false; // Should not happen with resolveLimit but for safety

        int radius = MobConversionConfig.ENTITY_SEARCH_RADIUS.get();
        AABB searchBox = entity.getBoundingBox().inflate(radius);

        // Count living entities of the specific type in the search radius
        List<? extends net.minecraft.world.entity.Entity> entities = level.getEntities(type, searchBox, net.minecraft.world.entity.Entity::isAlive);
        return entities.size() >= maxEntities;
    }

    private static boolean isAllowedAt(ServerLevel level, LivingEntity entity, EntityPoolEntry e) {
        MobConversionConfig.VillageScopeOverride scope = e.villageScope();
        
        // Optimize: check ANYWHERE/GLOBAL ANYWHERE immediately to skip POI scanning
        if (scope == MobConversionConfig.VillageScopeOverride.ANYWHERE) {
            LOGGER.debug("Entry scope override is ANYWHERE, skipping village checks.");
            return true;
        }
        
        MobConversionConfig.VillageScope globalScope = MobConversionConfig.VILLAGE_SCOPE.get();
        if (scope == MobConversionConfig.VillageScopeOverride.GLOBAL && globalScope == MobConversionConfig.VillageScope.ANYWHERE) {
            LOGGER.debug("Entry scope is GLOBAL and global scope is ANYWHERE, skipping village checks.");
            return true;
        }

        MobConversionConfig.BooleanOverride bell = e.requireBell();
        boolean natural = VillageHelper.isInsideNaturalVillage(level, entity.blockPosition());
        boolean artificial = VillageHelper.isInsideArtificialVillage(level, entity.blockPosition()) && !natural;
        boolean hasBell = VillageHelper.isBellNearby(level, entity.blockPosition());

        boolean allowed;
        if (scope == MobConversionConfig.VillageScopeOverride.GLOBAL) {
            MobConversionConfig.VillageScope s = MobConversionConfig.VILLAGE_SCOPE.get();
            boolean requireBell = MobConversionConfig.REQUIRE_BELL.get();
            allowed = switch (s) {
                case ANYWHERE -> true;
                case NATURAL -> natural && (!requireBell || hasBell);
                case ARTIFICIAL -> artificial;
                case BOTH -> (natural && (!requireBell || hasBell)) || artificial;
                default -> true;
            };
        } else {
            allowed = switch (scope) {
                case ANYWHERE -> true;
                case NATURAL -> {
                    boolean rb = bell == MobConversionConfig.BooleanOverride.GLOBAL
                            ? MobConversionConfig.REQUIRE_BELL.get()
                            : bell == MobConversionConfig.BooleanOverride.TRUE;
                    yield natural && (!rb || hasBell);
                }
                case ARTIFICIAL -> artificial;
                case BOTH -> {
                    boolean rb2 = bell == MobConversionConfig.BooleanOverride.GLOBAL
                            ? MobConversionConfig.REQUIRE_BELL.get()
                            : bell == MobConversionConfig.BooleanOverride.TRUE;
                    yield (natural && (!rb2 || hasBell)) || artificial;
                }
                default -> false;
            };
        }

        if (!allowed) {
            LOGGER.debug("Entry {} disallowed at {}: scopeOverride={}, natural={}, artificial={}, hasBell={}", 
                    e.entityId(), entity.blockPosition(), scope, natural, artificial, hasBell);
        }
        return allowed;
    }

    private static boolean isEntryOnCooldown(UUID entityId, ResourceLocation id, long now) {
        Map<ResourceLocation, Long> map = entryCooldowns.get(entityId);
        if (map == null) return false;
        long until = map.getOrDefault(id, 0L);
        return now < until;
    }



    public static void clear(UUID entityId) {
        entryCooldowns.remove(entityId);
    }
}
