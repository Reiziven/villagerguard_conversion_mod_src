package com.example.mobconversion.event;

import com.example.mobconversion.config.MobConversionConfig;
import com.example.mobconversion.util.EntityPoolManager;
import com.example.mobconversion.util.MobConversionManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Set;

public class MobConversionEvents {

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity livingEntity && livingEntity.level() instanceof ServerLevel serverLevel) {
            ResourceLocation entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());
            if (entityId != null && EntityPoolManager.getAllTriggerIds().contains(entityId.toString())) {
                // We'll only log this once in a while or it will flood the log
                MobConversionManager.onLivingEntityTick(serverLevel, livingEntity);
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            ResourceLocation entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());
            if (entityId != null && EntityPoolManager.getAllTriggerIds().contains(entityId.toString())) {
                MobConversionManager.clearCooldown(livingEntity.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            ResourceLocation entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());
            if (entityId != null && EntityPoolManager.getAllTriggerIds().contains(entityId.toString())) {
                MobConversionManager.clearCooldown(livingEntity.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity livingEntity && livingEntity.level() instanceof ServerLevel serverLevel) {
            ResourceLocation entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());
            if (entityId != null) {
                boolean isTrigger = EntityPoolManager.getAllTriggerIds().contains(entityId.toString());
                if (isTrigger) {
                    org.apache.logging.log4j.LogManager.getLogger("MobConversion").info("Trigger entity {} was hurt, checking for conversion...", entityId);
                    MobConversionManager.onLivingEntityAttacked(serverLevel, livingEntity, event.getSource().getEntity());
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MobConversionManager.tickParticles();
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("mobconversion_nodrops")) {
            event.getDrops().clear();
        }
    }
}
