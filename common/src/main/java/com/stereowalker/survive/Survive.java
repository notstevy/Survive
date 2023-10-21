package com.stereowalker.survive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.RegistrarManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.stereowalker.survive.compat.OriginsCompat;
import com.stereowalker.survive.compat.SItemProperties;
import com.stereowalker.survive.config.Config;
import com.stereowalker.survive.config.HygieneConfig;
import com.stereowalker.survive.config.ServerConfig;
import com.stereowalker.survive.config.StaminaConfig;
import com.stereowalker.survive.config.TemperatureConfig;
import com.stereowalker.survive.config.ThirstConfig;
import com.stereowalker.survive.config.WellbeingConfig;
import com.stereowalker.survive.core.cauldron.SCauldronInteraction;
import com.stereowalker.survive.events.SurviveEvents;
import com.stereowalker.survive.json.ArmorJsonHolder;
import com.stereowalker.survive.json.BiomeJsonHolder;
import com.stereowalker.survive.json.BlockTemperatureJsonHolder;
import com.stereowalker.survive.json.EntityTemperatureJsonHolder;
import com.stereowalker.survive.json.FoodJsonHolder;
import com.stereowalker.survive.json.PotionJsonHolder;
import com.stereowalker.survive.json.property.BlockPropertyHandlerImpl;
import com.stereowalker.survive.network.protocol.game.ClientboundDataTransferPacket;
import com.stereowalker.survive.network.protocol.game.ClientboundDrinkSoundPacket;
import com.stereowalker.survive.network.protocol.game.ClientboundSurvivalStatsPacket;
import com.stereowalker.survive.network.protocol.game.ServerboundArmorStaminaPacket;
import com.stereowalker.survive.network.protocol.game.ServerboundInteractWithWaterPacket;
import com.stereowalker.survive.network.protocol.game.ServerboundPlayerStatusBookPacket;
import com.stereowalker.survive.network.protocol.game.ServerboundRelaxPacket;
import com.stereowalker.survive.network.protocol.game.ServerboundStaminaExhaustionPacket;
import com.stereowalker.survive.network.protocol.game.ServerboundThirstMovementPacket;
import com.stereowalker.survive.resource.ArmorDataManager;
import com.stereowalker.survive.resource.BiomeDataManager;
import com.stereowalker.survive.resource.BlockDataManager;
import com.stereowalker.survive.resource.EntityTemperatureDataManager;
import com.stereowalker.survive.resource.FluidDataManager;
import com.stereowalker.survive.resource.ItemConsummableDataManager;
import com.stereowalker.survive.resource.PotionDrinkDataManager;
import com.stereowalker.survive.server.commands.NeedsCommand;
import com.stereowalker.survive.tags.FluidSTags;
import com.stereowalker.survive.tags.ItemSTags;
import com.stereowalker.survive.world.DataMaps;
import com.stereowalker.survive.world.effect.SMobEffects;
import com.stereowalker.survive.world.entity.ai.attributes.SAttributes;
import com.stereowalker.survive.world.item.CanteenItem;
import com.stereowalker.survive.world.item.HygieneItems;
import com.stereowalker.survive.world.item.SCreativeModeTab;
import com.stereowalker.survive.world.item.SItems;
import com.stereowalker.survive.world.item.alchemy.BrewingRecipes;
import com.stereowalker.survive.world.item.alchemy.SPotions;
import com.stereowalker.survive.world.item.crafting.SRecipeSerializer;
import com.stereowalker.survive.world.item.enchantment.StaminaEnchantments;
import com.stereowalker.survive.world.item.enchantment.TemperatureEnchantments;
import com.stereowalker.survive.world.level.CGameRules;
import com.stereowalker.survive.world.level.block.SBlocks;
import com.stereowalker.survive.world.level.material.PurifiedWaterFluid;
import com.stereowalker.survive.world.level.material.SFluids;
import com.stereowalker.survive.world.spellcraft.SSpells;
import com.stereowalker.unionlib.api.collectors.CommandCollector;
import com.stereowalker.unionlib.api.collectors.ConfigCollector;
import com.stereowalker.unionlib.api.collectors.DefaultAttributeModifier;
import com.stereowalker.unionlib.api.collectors.InsertCollector;
import com.stereowalker.unionlib.api.collectors.PacketCollector;
import com.stereowalker.unionlib.api.collectors.ReloadListeners;
import com.stereowalker.unionlib.api.creativetabs.CreativeTabBuilder;
import com.stereowalker.unionlib.api.creativetabs.CreativeTabPopulator;
import com.stereowalker.unionlib.api.registries.RegistryCollector;
import com.stereowalker.unionlib.event.potionfluid.FluidToPotionEvent;
import com.stereowalker.unionlib.event.potionfluid.PotionToFluidEvent;
import com.stereowalker.unionlib.insert.Inserts;
import com.stereowalker.unionlib.mod.MinecraftMod;
import com.stereowalker.unionlib.mod.PacketHolder;
import com.stereowalker.unionlib.mod.ServerSegment;
import com.stereowalker.unionlib.util.RegistryHelper;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.apache.logging.log4j.Logger;

public interface Survive extends PacketHolder {

	String MOD_ID = "survive";
	float DEFAULT_TEMP = 37.0F;
	int PURIFIED_WATER_COLOR = 0x41d3f8;
	Config CONFIG = new Config();
	StaminaConfig STAMINA_CONFIG = new StaminaConfig();
	HygieneConfig HYGIENE_CONFIG = new HygieneConfig();
	TemperatureConfig TEMPERATURE_CONFIG = new TemperatureConfig();
	ThirstConfig THIRST_CONFIG = new ThirstConfig();
	WellbeingConfig WELLBEING_CONFIG = new WellbeingConfig();
	ItemConsummableDataManager consummableReloader = new ItemConsummableDataManager();
	PotionDrinkDataManager potionReloader = new PotionDrinkDataManager();
	ArmorDataManager armorReloader = new ArmorDataManager();
	BlockDataManager blockReloader = new BlockDataManager();
	BiomeDataManager biomeReloader = new BiomeDataManager();
	EntityTemperatureDataManager entityReloader = new EntityTemperatureDataManager();
	FluidDataManager fluidReloader = new FluidDataManager();
	Supplier<RegistrarManager> MANAGER = Suppliers.memoize(() -> RegistrarManager.get(MOD_ID));

	static boolean isCombatLoaded() {
		return Platform.isModLoaded("combat");
	}
	static boolean isOriginsLoaded() {
		return Platform.isModLoaded("origins");
	}
	static boolean isPrimalWinterLoaded() {
		return Platform.isModLoaded("primalwinter");
	}
	
	default void onModConstruct() {
		if (isCombatLoaded()) SSpells.registerAll();
		if (isOriginsLoaded()) OriginsCompat.initOriginsPatcher();
		BlockPropertyHandlerImpl.init();
	}
	
	default void onModStartup() {
		SCauldronInteraction.bootStrap();
		BrewingRecipes.addBrewingRecipes();
		CGameRules.init();

		for(Map.Entry<ResourceKey<Item>, Item> entry : MANAGER.get().get(Registries.ITEM).entrySet()) {
			var item = entry.getValue();

			if (item.isEdible())
				DataMaps.Server.defaultFood.put(entry.getKey().location(), item.getFoodProperties());
		}
	}
	
	default void onModStartupInClient() {
		RenderType frendertype = RenderType.translucent();
		ItemBlockRenderTypes.setRenderLayer(SFluids.PURIFIED_WATER, frendertype);
		ItemBlockRenderTypes.setRenderLayer(SFluids.FLOWING_PURIFIED_WATER, frendertype);
	}
	
	default void setupConfigs(ConfigCollector collector) {
		collector.registerConfig(ServerConfig.class);
		collector.registerConfig(CONFIG);
		collector.registerConfig(HYGIENE_CONFIG); 
		collector.registerConfig(TEMPERATURE_CONFIG);
		collector.registerConfig(THIRST_CONFIG);
		collector.registerConfig(WELLBEING_CONFIG);
		collector.registerConfig(STAMINA_CONFIG);
	}
	
	default void setupCommands(CommandCollector collector) {
		NeedsCommand.register(collector.dispatcher());
	}

	default void setupRegistries(RegistryCollector collector) {
		collector.addRegistryHolder(SBlocks.class);
		collector.addRegistryHolder(SFluids.class);
		collector.addRegistryHolder(SItems.class);
		collector.addRegistryHolder(HygieneItems.class);
		collector.addRegistryHolder(SRecipeSerializer.class);
		collector.addRegistryHolder(SAttributes.class);
		collector.addRegistryHolder(SMobEffects.class);
		if (Survive.STAMINA_CONFIG.enabled) {
			collector.addRegistryHolder(StaminaEnchantments.class);
		}
		if (Survive.TEMPERATURE_CONFIG.enabled) {
			collector.addRegistryHolder(TemperatureEnchantments.class);
		}
	}
	
	default void registerInserts(InsertCollector collector) {
		collector.addInsert(Inserts.LIVING_TICK, SurviveEvents::sendToClient);
		collector.addInsert(Inserts.LIVING_TICK, SurviveEvents::updateEnvTemperature);
		collector.addInsert(Inserts.PLAYER_RESTORE, SurviveEvents::restoreStats);
		collector.addInsert(Inserts.LOGGED_OUT, SurviveEvents::desyncClient);
		collector.addInsert(Inserts.LEVEL_LOAD, SurviveEvents::addReload);
		collector.addInsert(Inserts.LOOT_TABLE_LOAD, (id,lootTable,cancel)->{
			String ANIMAL_LOOT = "entities/animal_fat";
			List<Pair<ResourceLocation, List<String>>> LOOT_MODIFIERS = Lists.newArrayList(
					Pair.of(new ResourceLocation("entities/sheep"), Lists.newArrayList(ANIMAL_LOOT)),
					Pair.of(new ResourceLocation("entities/chicken"), Lists.newArrayList(ANIMAL_LOOT)),
					Pair.of(new ResourceLocation("entities/cow"), Lists.newArrayList(ANIMAL_LOOT)),
					Pair.of(new ResourceLocation("entities/pig"), Lists.newArrayList(ANIMAL_LOOT))
					);
			

			BiFunction<String, Integer, LootPoolEntryContainer.Builder<?>> getInjectEntry = (name, weight) -> {
				ResourceLocation table = location("inject/" + name);
				return LootTableReference.lootTableReference(table).setWeight(weight);
			};
			
			LOOT_MODIFIERS.forEach((pair) -> {
				if(id.equals(pair.getKey())) {
					pair.getValue().forEach((file) -> {
						debug("Injecting \"" + file + "\" in " +pair.getKey());

						var lootTableInstance = lootTable.get();
						lootTableInstance.pools = Arrays.copyOf(lootTableInstance.pools, lootTableInstance.pools.length + 1);
						lootTableInstance.pools[lootTableInstance.pools.length - 1] = LootPool.lootPool()
								.add(getInjectEntry.apply(file, 1))
								.setBonusRolls(UniformGenerator.between(0.0F, 1.0F))
								// .name("survive_inject")
								.build();
					});
				}
			});
		});
		collector.addInsert(Inserts.ITEM_TOOLTIP, (stack, player, tip, flag)->{
			if (player != null)
				FoodUtils.applyFoodStatusToTooltip(player, stack, tip);
		});
		collector.addInsert(Inserts.MENU_OPEN, (player, menu)->{
			if (player != null)
				FoodUtils.giveLifespanToFood(menu.getItems(), player.level().getGameTime());
		});
	}
	
	default void modifyDefaultEntityAttributes(DefaultAttributeModifier modifier) {
		modifier.addToEntity(EntityType.PLAYER, SAttributes.COLD_RESISTANCE, SAttributes.HEAT_RESISTANCE, SAttributes.MAX_STAMINA);
	}
	
	@Override
	default void registerPackets(PacketCollector collector) {
		collector.registerServerboundPacket(location(""), ServerboundArmorStaminaPacket.class, (packetBuffer) -> {return new ServerboundArmorStaminaPacket(packetBuffer);});
		collector.registerServerboundPacket(location(""), ServerboundThirstMovementPacket.class, (packetBuffer) -> {return new ServerboundThirstMovementPacket(packetBuffer);});
		collector.registerServerboundPacket(location(""), ServerboundInteractWithWaterPacket.class, (packetBuffer) -> {return new ServerboundInteractWithWaterPacket(packetBuffer);});
		collector.registerServerboundPacket(location(""), ServerboundStaminaExhaustionPacket.class, ServerboundStaminaExhaustionPacket::new);
		collector.registerServerboundPacket(location(""), ServerboundRelaxPacket.class, ServerboundRelaxPacket::new);
		collector.registerServerboundPacket(location(""), ServerboundPlayerStatusBookPacket.class, ServerboundPlayerStatusBookPacket::new);
		collector.registerClientboundPacket(location(""), ClientboundSurvivalStatsPacket.class, (packetBuffer) -> {return new ClientboundSurvivalStatsPacket(packetBuffer);});
		collector.registerClientboundPacket(location(""), ClientboundDrinkSoundPacket.class, (packetBuffer) -> {return new ClientboundDrinkSoundPacket(packetBuffer);});
		collector.registerClientboundPacket(location(""), ClientboundDataTransferPacket.class, (packetBuffer) -> {return new ClientboundDataTransferPacket(packetBuffer);});
	}

	//TODO: FInd Somewhere to put all these
	static void registerDrinkDataForItem(ResourceLocation location, FoodJsonHolder drinkData) {
		DataMaps.Server.consummableItem.put(location, drinkData);
	}
	static void registerDrinkDataForPotion(ResourceLocation location, PotionJsonHolder consummableData) {
		DataMaps.Server.potionDrink.put(location, consummableData);
	}
	static void registerArmorTemperatures(ResourceLocation location, ArmorJsonHolder armorData) {
		DataMaps.Server.armor.put(location, armorData);
	}
	static void registerBlockTemperatures(ResourceLocation location, BlockTemperatureJsonHolder drinkData) {
		DataMaps.Server.blockTemperature.put(location, drinkData);
	}
	static void registerEntityTemperatures(ResourceLocation location, EntityTemperatureJsonHolder drinkData) {
		DataMaps.Server.entityTemperature.put(location, drinkData);
	}
	static void registerBiomeTemperatures(ResourceLocation location, BiomeJsonHolder biomeData) {
		DataMaps.Server.biome.put(location, biomeData);
	}

	
	static ItemStack convertToPlayerStatusBook(ItemStack stack) {
		ItemStack result = new ItemStack(Items.WRITTEN_BOOK);
		if (stack.getTag() != null) {
			result.setTag(stack.getTag().copy());
         }
		result.addTagElement("status_owner", StringTag.valueOf(""));
		result.getTag().putInt("generation", 0);
		return result;
	}

	default void registerServerReldableResources(ReloadListeners reloadListener) {
		reloadListener.listenTo(consummableReloader);
		reloadListener.listenTo(potionReloader);
		reloadListener.listenTo(armorReloader);
		reloadListener.listenTo(blockReloader);
		reloadListener.listenTo(biomeReloader);
		reloadListener.listenTo(entityReloader);
		reloadListener.listenTo(fluidReloader);
	}

	default void registerCreativeTabs(CreativeTabBuilder builder) {
		builder.addTab("main_tab", SCreativeModeTab.TAB_MAIN);
	}
	
	default void populateCreativeTabs(CreativeTabPopulator populator) {
		//Hygiene related
		if (populator.getTab().getDisplayName().equals(SCreativeModeTab.TAB_MAIN.getDisplayName()) && Survive.HYGIENE_CONFIG.enabled) {
			populator.addItems(HygieneItems.BATH_SPONGE);
			populator.addItems(HygieneItems.WHITE_WASHCLOTH);
			populator.addItems(HygieneItems.ORANGE_WASHCLOTH);
			populator.addItems(HygieneItems.MAGENTA_WASHCLOTH);
			populator.addItems(HygieneItems.LIGHT_BLUE_WASHCLOTH);
			populator.addItems(HygieneItems.YELLOW_WASHCLOTH);
			populator.addItems(HygieneItems.LIME_WASHCLOTH);
			populator.addItems(HygieneItems.PINK_WASHCLOTH);
			populator.addItems(HygieneItems.GRAY_WASHCLOTH);
			populator.addItems(HygieneItems.LIGHT_GRAY_WASHCLOTH);
			populator.addItems(HygieneItems.CYAN_WASHCLOTH);
			populator.addItems(HygieneItems.PURPLE_WASHCLOTH);
			populator.addItems(HygieneItems.BLUE_WASHCLOTH);
			populator.addItems(HygieneItems.BROWN_WASHCLOTH);
			populator.addItems(HygieneItems.GREEN_WASHCLOTH);
			populator.addItems(HygieneItems.RED_WASHCLOTH);
			populator.addItems(HygieneItems.BLACK_WASHCLOTH);
			populator.addItems(HygieneItems.WOOD_ASH);
			populator.addItems(HygieneItems.POTASH_SOLUTION);
			populator.addItems(HygieneItems.POTASH);
			populator.addItems(HygieneItems.ANIMAL_FAT);
			populator.addItems(HygieneItems.SOAP_MIX);
			populator.addItems(HygieneItems.SOAP_BOTTLE);
		}
		if (populator.getTab().getDisplayName().equals(SCreativeModeTab.TAB_MAIN.getDisplayName())) {
			populator.addItems(SItems.WOOL_HAT);
			populator.addItems(SItems.WOOL_JACKET);
			populator.addItems(SItems.WOOL_PANTS);
			populator.addItems(SItems.WOOL_BOOTS);
			populator.addItems(SItems.STIFFENED_HONEY_HELMET);
			populator.addItems(SItems.STIFFENED_HONEY_CHESTPLATE);
			populator.addItems(SItems.STIFFENED_HONEY_LEGGINGS);
			populator.addItems(SItems.STIFFENED_HONEY_BOOTS);
			populator.addItems(SItems.CANTEEN);
			for(Potion potion : RegistryHelper.potions()) {
				if (potion != Potions.EMPTY) {
					populator.getOutput().accept(CanteenItem.addToCanteen(new ItemStack(SItems.FILLED_CANTEEN), THIRST_CONFIG.canteen_fill_amount, potion));
				}
			}
			populator.addItems(SItems.WATER_BOWL);
			populator.addItems(SItems.PURIFIED_WATER_BOWL);
			populator.addItems(SItems.ICE_CUBE);
			populator.addItems(SItems.THERMOMETER);
			populator.addItems(SItems.TEMPERATURE_REGULATOR);
			populator.addItems(SItems.LARGE_HEATING_PLATE);
			populator.addItems(SItems.LARGE_COOLING_PLATE);
			populator.addItems(SItems.MEDIUM_HEATING_PLATE);
			populator.addItems(SItems.MEDIUM_COOLING_PLATE);
			populator.addItems(SItems.SMALL_HEATING_PLATE);
			populator.addItems(SItems.SMALL_COOLING_PLATE);
			populator.addItems(SItems.CHARCOAL_FILTER);
			populator.addItems(SItems.PURIFIED_WATER_BUCKET);
			populator.addItems(SItems.MAGMA_PASTE);
		}
		
	}

	ResourceLocation location(String location);
	Logger getLogger();

	default void debug(Object message) {
		if (CONFIG.debugMode) getLogger().debug(message);
	}


	static List<String> defaultDimensionMods() {
		List<String> dims = new ArrayList<String>();
		dims.add("minecraft:overworld,0.0");
		dims.add("minecraft:the_nether,0.0");
		dims.add("minecraft:the_end,0.0");
		return dims;
	}

}
