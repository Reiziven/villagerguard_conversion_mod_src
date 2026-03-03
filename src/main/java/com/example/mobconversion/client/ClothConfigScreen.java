package com.example.mobconversion.client;

import com.example.mobconversion.config.MobConversionConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ClothConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.mobconversion.config"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // --- Detection Category ---
        ConfigCategory detection = builder.getOrCreateCategory(Component.translatable("category.mobconversion.detection"));
        
        detection.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.threatDetectionRadius"), MobConversionConfig.THREAT_DETECTION_RADIUS.get())
                .setDefaultValue(15)
                .setMin(8).setMax(128)
                .setTooltip(Component.translatable("option.mobconversion.threatDetectionRadius.tooltip"))
                .setSaveConsumer(MobConversionConfig.THREAT_DETECTION_RADIUS::set)
                .build());

        detection.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.bellDetectionRadius"), MobConversionConfig.BELL_DETECTION_RADIUS.get())
                .setDefaultValue(80)
                .setMin(8).setMax(128)
                .setTooltip(Component.translatable("option.mobconversion.bellDetectionRadius.tooltip"))
                .setSaveConsumer(MobConversionConfig.BELL_DETECTION_RADIUS::set)
                .build());

        detection.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.scanCooldownTicks"), MobConversionConfig.SCAN_COOLDOWN_TICKS.get())
                .setDefaultValue(80)
                .setMin(20).setMax(600)
                .setTooltip(Component.translatable("option.mobconversion.scanCooldownTicks.tooltip"))
                .setSaveConsumer(MobConversionConfig.SCAN_COOLDOWN_TICKS::set)
                .build());

        detection.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.artificialVillagePoiRadius"), MobConversionConfig.ARTIFICIAL_VILLAGE_POI_RADIUS.get())
                .setDefaultValue(64)
                .setMin(8).setMax(128)
                .setTooltip(Component.translatable("option.mobconversion.artificialVillagePoiRadius.tooltip"))
                .setSaveConsumer(MobConversionConfig.ARTIFICIAL_VILLAGE_POI_RADIUS::set)
                .build());

        // --- Village Category ---
        ConfigCategory village = builder.getOrCreateCategory(Component.translatable("category.mobconversion.village"));

        village.addEntry(entryBuilder.startEnumSelector(Component.translatable("option.mobconversion.villageScope"), MobConversionConfig.VillageScope.class, MobConversionConfig.VILLAGE_SCOPE.get())
                .setDefaultValue(MobConversionConfig.VillageScope.BOTH)
                .setTooltip(Component.translatable("option.mobconversion.villageScope.tooltip"))
                .setSaveConsumer(MobConversionConfig.VILLAGE_SCOPE::set)
                .build());

        village.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.mobconversion.requireBell"), MobConversionConfig.REQUIRE_BELL.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.mobconversion.requireBell.tooltip"))
                .setSaveConsumer(MobConversionConfig.REQUIRE_BELL::set)
                .build());

        village.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.maxEntitiesPerArea"), MobConversionConfig.MAX_ENTITIES_PER_AREA.get())
                .setDefaultValue(2)
                .setMin(0).setMax(50)
                .setTooltip(Component.translatable("option.mobconversion.maxEntitiesPerArea.tooltip"))
                .setSaveConsumer(MobConversionConfig.MAX_ENTITIES_PER_AREA::set)
                .build());

        village.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.entitySearchRadius"), MobConversionConfig.ENTITY_SEARCH_RADIUS.get())
                .setDefaultValue(80)
                .setMin(8).setMax(256)
                .setTooltip(Component.translatable("option.mobconversion.entitySearchRadius.tooltip"))
                .setSaveConsumer(MobConversionConfig.ENTITY_SEARCH_RADIUS::set)
                .build());

        village.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.sharedCooldownRadius"), MobConversionConfig.SHARED_COOLDOWN_RADIUS.get())
                .setDefaultValue(100)
                .setMin(1).setMax(512)
                .setTooltip(Component.translatable("option.mobconversion.sharedCooldownRadius.tooltip"))
                .setSaveConsumer(MobConversionConfig.SHARED_COOLDOWN_RADIUS::set)
                .build());

        // --- Conversion Category ---
        ConfigCategory conversion = builder.getOrCreateCategory(Component.translatable("category.mobconversion.conversion"));

        conversion.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.mobconversion.convertNitwits"), MobConversionConfig.CONVERT_NITWITS.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.mobconversion.convertNitwits.tooltip"))
                .setSaveConsumer(MobConversionConfig.CONVERT_NITWITS::set)
                .build());

        conversion.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.mobconversion.convertUnemployed"), MobConversionConfig.CONVERT_UNEMPLOYED.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.mobconversion.convertUnemployed.tooltip"))
                .setSaveConsumer(MobConversionConfig.CONVERT_UNEMPLOYED::set)
                .build());

        conversion.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.mobconversion.convertUntradedProfessionals"), MobConversionConfig.CONVERT_UNTRADED_PROFESSIONALS.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.mobconversion.convertUntradedProfessionals.tooltip"))
                .setSaveConsumer(MobConversionConfig.CONVERT_UNTRADED_PROFESSIONALS::set)
                .build());

        conversion.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.mobconversion.allowSelfConversion"), MobConversionConfig.ALLOW_SELF_CONVERSION.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.mobconversion.allowSelfConversion.tooltip"))
                .setSaveConsumer(MobConversionConfig.ALLOW_SELF_CONVERSION::set)
                .build());

        conversion.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.mobconversion.spawnInsteadOfConvert"), MobConversionConfig.DEFAULT_SPAWN_INSTEAD_OF_CONVERT.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.mobconversion.spawnInsteadOfConvert.tooltip"))
                .setSaveConsumer(MobConversionConfig.DEFAULT_SPAWN_INSTEAD_OF_CONVERT::set)
                .build());

        conversion.addEntry(entryBuilder.startStrList(Component.translatable("option.mobconversion.exemptSameVillageAttackers"), (List<String>) MobConversionConfig.EXEMPT_ATTACKER_ENTITY_IDS.get())
                .setDefaultValue(List.of("minecraft:villager", "minecraft:iron_golem"))
                .setTooltip(Component.translatable("option.mobconversion.exemptSameVillageAttackers.tooltip"))
                .setSaveConsumer(MobConversionConfig.EXEMPT_ATTACKER_ENTITY_IDS::set)
                .build());

        conversion.addEntry(entryBuilder.startStrList(Component.translatable("option.mobconversion.convertibleEntityIds"), (List<String>) MobConversionConfig.CONVERTIBLE_ENTITY_IDS.get())
                .setDefaultValue(List.of("minecraft:villager", "mca:male_villager", "mca:female_villager"))
                .setTooltip(Component.translatable("option.mobconversion.convertibleEntityIds.tooltip"))
                .setSaveConsumer(MobConversionConfig.CONVERTIBLE_ENTITY_IDS::set)
                .build());

        // --- Effects Category ---
        ConfigCategory effects = builder.getOrCreateCategory(Component.translatable("category.mobconversion.effects"));

        effects.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.mobconversion.conversionParticlesEnabled"), MobConversionConfig.CONVERSION_PARTICLES_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.mobconversion.conversionParticlesEnabled.tooltip"))
                .setSaveConsumer(MobConversionConfig.CONVERSION_PARTICLES_ENABLED::set)
                .build());

        effects.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.conversionParticleDurationTicks"), MobConversionConfig.CONVERSION_PARTICLE_DURATION_TICKS.get())
                .setDefaultValue(40)
                .setMin(1).setMax(200)
                .setTooltip(Component.translatable("option.mobconversion.conversionParticleDurationTicks.tooltip"))
                .setSaveConsumer(MobConversionConfig.CONVERSION_PARTICLE_DURATION_TICKS::set)
                .build());

        // --- Entity Pool Category ---
        ConfigCategory pool = builder.getOrCreateCategory(Component.translatable("category.mobconversion.pool"));

        pool.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.maxEntries"), MobConversionConfig.MAX_ENTRIES.get())
                .setDefaultValue(10)
                .setMin(1).setMax(100)
                .setTooltip(Component.translatable("option.mobconversion.maxEntries.tooltip"))
                .setSaveConsumer(MobConversionConfig.MAX_ENTRIES::set)
                .build());

        pool.addEntry(entryBuilder.startDoubleField(Component.translatable("option.mobconversion.defaultChance"), MobConversionConfig.DEFAULT_CHANCE.get())
                .setDefaultValue(1.0)
                .setMin(0.0).setMax(1.0)
                .setTooltip(Component.translatable("option.mobconversion.defaultChance.tooltip"))
                .setSaveConsumer(MobConversionConfig.DEFAULT_CHANCE::set)
                .build());

        pool.addEntry(entryBuilder.startEnumSelector(Component.translatable("option.mobconversion.defaultMode"), MobConversionConfig.ConversionMode.class, MobConversionConfig.DEFAULT_MODE.get())
                .setDefaultValue(MobConversionConfig.ConversionMode.CONVERT)
                .setTooltip(Component.translatable("option.mobconversion.defaultMode.tooltip"))
                .setSaveConsumer(MobConversionConfig.DEFAULT_MODE::set)
                .build());

        pool.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.defaultPriority"), MobConversionConfig.DEFAULT_PRIORITY.get())
                .setDefaultValue(1)
                .setMin(1).setMax(100)
                .setTooltip(Component.translatable("option.mobconversion.defaultPriority.tooltip"))
                .setSaveConsumer(MobConversionConfig.DEFAULT_PRIORITY::set)
                .build());

        pool.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.mobconversion.defaultDropLoot"), MobConversionConfig.DEFAULT_DROP_LOOT.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.mobconversion.defaultDropLoot.tooltip"))
                .setSaveConsumer(MobConversionConfig.DEFAULT_DROP_LOOT::set)
                .build());

        pool.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.defaultCooldownTicks"), MobConversionConfig.DEFAULT_COOLDOWN_TICKS.get())
                .setDefaultValue(0)
                .setMin(0).setMax(1000000)
                .setTooltip(Component.translatable("option.mobconversion.defaultCooldownTicks.tooltip"))
                .setSaveConsumer(MobConversionConfig.DEFAULT_COOLDOWN_TICKS::set)
                .build());

        pool.addEntry(entryBuilder.startEnumSelector(Component.translatable("option.mobconversion.defaultVillageScope"), MobConversionConfig.VillageScopeOverride.class, MobConversionConfig.DEFAULT_VILLAGE_SCOPE.get())
                .setDefaultValue(MobConversionConfig.VillageScopeOverride.GLOBAL)
                .setTooltip(Component.translatable("option.mobconversion.defaultVillageScope.tooltip"))
                .setSaveConsumer(MobConversionConfig.DEFAULT_VILLAGE_SCOPE::set)
                .build());

        pool.addEntry(entryBuilder.startEnumSelector(Component.translatable("option.mobconversion.defaultRequireBell"), MobConversionConfig.BooleanOverride.class, MobConversionConfig.DEFAULT_REQUIRE_BELL.get())
                .setDefaultValue(MobConversionConfig.BooleanOverride.GLOBAL)
                .setTooltip(Component.translatable("option.mobconversion.defaultRequireBell.tooltip"))
                .setSaveConsumer(MobConversionConfig.DEFAULT_REQUIRE_BELL::set)
                .build());

        pool.addEntry(entryBuilder.startIntField(Component.translatable("option.mobconversion.defaultMaxLimit"), MobConversionConfig.DEFAULT_MAX_LIMIT.get())
                .setDefaultValue(-1)
                .setMin(-1).setMax(100)
                .setTooltip(Component.translatable("option.mobconversion.defaultMaxLimit.tooltip"))
                .setSaveConsumer(MobConversionConfig.DEFAULT_MAX_LIMIT::set)
                .build());

        pool.addEntry(entryBuilder.startStrList(Component.translatable("option.mobconversion.entries"), (List<String>) MobConversionConfig.ENTRIES.get())
                .setDefaultValue(List.of("irons_spellbooks:priest/0.2/CONVERT/1/true/6000/BOTH/false/2"))
                .setTooltip(Component.translatable("option.mobconversion.entries.tooltip"))
                .setSaveConsumer(MobConversionConfig.ENTRIES::set)
                .build());

        builder.setSavingRunnable(() -> {
            MobConversionConfig.SPEC.save();
        });

        return builder.build();
    }
}
