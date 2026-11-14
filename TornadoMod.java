package com.tornadomod;

import com.tornadomod.command.TornadoCommand;
import com.tornadomod.entity.TornadoEntity;
import com.tornadomod.item.TornadoWandItem;
import com.tornadomod.world.TornadoSpawner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TornadoMod.MOD_ID)
public class TornadoMod {
	public static final String MOD_ID = "tornadomod";

	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

	public static final RegistryObject<EntityType<TornadoEntity>> TORNADO = ENTITIES.register("tornado",
			() -> EntityType.Builder.<TornadoEntity>of(TornadoEntity::new, MobCategory.MISC)
					.sized(2.0f, 6.0f)
					.fireImmune()
					.clientTrackingRange(16)
					.updateInterval(2)
					.build(new ResourceLocation(MOD_ID, "tornado").toString()));

	public static final RegistryObject<Item> TORNADO_WAND = ITEMS.register("tornado_wand",
			() -> new TornadoWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

	/** /gamerule tornadoSpawns - tornadoes form by themselves during thunderstorms. */
	public static final GameRules.Key<GameRules.BooleanValue> RULE_SPAWNS = GameRules.register(
			"tornadoSpawns", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));

	/** /gamerule tornadoDestruction - tornadoes tear blocks out of the world. */
	public static final GameRules.Key<GameRules.BooleanValue> RULE_DESTRUCTION = GameRules.register(
			"tornadoDestruction", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

	public TornadoMod() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ENTITIES.register(modBus);
		ITEMS.register(modBus);
		modBus.addListener(this::addCreativeTabItems);

		MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
		MinecraftForge.EVENT_BUS.addListener(this::onLevelTick);
	}

	private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(TORNADO_WAND.get());
		}
	}

	private void registerCommands(RegisterCommandsEvent event) {
		TornadoCommand.register(event.getDispatcher());
	}

	private void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
			TornadoSpawner.tick(serverLevel);
		}
	}
}
