package com.splatball;

import com.mojang.logging.LogUtils;
import com.splatball.block.StationBlock;
import com.splatball.entity.BotEntity;
import com.splatball.entity.Paintball;
import com.splatball.item.MarkerItem;
import com.splatball.item.MarkerStats;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(Splatball.MODID)
public class Splatball {
    public static final String MODID = "splatball";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** Every marker, in kit / creative-tab order. */
    public static final List<RegistryObject<MarkerItem>> MARKERS = new ArrayList<>();

    public static final RegistryObject<MarkerItem> PISTOL = marker("splat_pistol", MarkerStats.PISTOL);
    public static final RegistryObject<MarkerItem> SMG = marker("splatter_smg", MarkerStats.SMG);
    public static final RegistryObject<MarkerItem> SHOTGUN = marker("scatter_blaster", MarkerStats.SHOTGUN);
    public static final RegistryObject<MarkerItem> SNIPER = marker("long_shot", MarkerStats.SNIPER);
    public static final RegistryObject<MarkerItem> LAUNCHER = marker("paint_launcher", MarkerStats.LAUNCHER);

    /** Right-click to drop into a match; see StationBlock. */
    public static final RegistryObject<Block> STATION = BLOCKS.register("paintball_station",
            () -> new StationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(2.0F, 1200.0F).lightLevel(s -> 10)));
    public static final RegistryObject<Item> STATION_ITEM = ITEMS.register("paintball_station",
            () -> new BlockItem(STATION.get(), new Item.Properties()));

    /** Only used to render the flying paintball; not obtainable. */
    public static final RegistryObject<Item> PAINTBALL = ITEMS.register("paintball", () -> new Item(new Item.Properties()));

    public static final RegistryObject<EntityType<Paintball>> PAINTBALL_ENTITY = ENTITIES.register("paintball",
            () -> EntityType.Builder.<Paintball>of(Paintball::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build("paintball"));

    public static final RegistryObject<EntityType<BotEntity>> BOT = ENTITIES.register("bot",
            () -> EntityType.Builder.<BotEntity>of(BotEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("bot"));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("splatball", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.splatball"))
            .icon(() -> new ItemStack(SMG.get()))
            .displayItems((params, output) -> {
                output.accept(STATION_ITEM.get());
                MARKERS.forEach(m -> output.accept(m.get()));
            })
            .build());

    public Splatball() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(BOT.get(), BotEntity.createAttributes().build()));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        Net.register();
    }

    private static RegistryObject<MarkerItem> marker(String name, MarkerStats stats) {
        RegistryObject<MarkerItem> item = ITEMS.register(name, () -> new MarkerItem(stats));
        MARKERS.add(item);
        return item;
    }
}
