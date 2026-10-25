package com.carpinchoia;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CarpinchoIA.MODID)
public class CarpinchoIA {
    public static final String MODID = "carpincho_ia";
    /** Duracion del Momento Carpincho: 1 hora. */
    public static final long MOMENTO_MILLIS = 60L * 60L * 1000L;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final RegistryObject<Item> EGG = ITEMS.register("carpincho_ia_eg",
            () -> new CarpinchoEggItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MODIFIER = ITEMS.register("modificador_carpincho",
            () -> new ModifierItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FACE = ITEMS.register("carpincho_face",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<EntityType<CarpinchoEntity>> CARPINCHO = ENTITIES.register("carpincho",
            () -> EntityType.Builder.<CarpinchoEntity>of(CarpinchoEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.0F).clientTrackingRange(10).build("carpincho"));

    public CarpinchoIA() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(ModNet::register));
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(CARPINCHO.get(), CarpinchoEntity.attributes().build()));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                e.accept(EGG);
                e.accept(MODIFIER);
            }
        });
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new ServerEvents());
    }
}
