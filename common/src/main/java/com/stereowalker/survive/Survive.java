package com.stereowalker.survive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(value = "survive")
public class Survive extends MinecraftMod implements PacketHolder {

	public static final float DEFAULT_TEMP = 37.0F;
	public static final int PURIFIED_WATER_COLOR = 0x41d3f8;
	public static Map<Potion,List<Fluid>> POTION_FLUID_MAP;
	public static final String MOD_ID = "survive";

	public static final Config CONFIG = new Config();
	public static final StaminaConfig STAMINA_CONFIG = new StaminaConfig();
	public static final HygieneConfig HYGIENE_CONFIG = new HygieneConfig();
	public static final TemperatureConfig TEMPERATURE_CONFIG = new TemperatureConfig();
	public static final ThirstConfig THIRST_CONFIG = new ThirstConfig();
	public static final WellbeingConfig WELLBEING_CONFIG = new WellbeingConfig();

	public static boolean isPrimalWinterLoaded;
	public static final ItemConsummableDataManager consummableReloader = new ItemConsummableDataManager();
	public static final PotionDrinkDataManager potionReloader = new PotionDrinkDataManager();
	public static final ArmorDataManager armorReloader = new ArmorDataManager();
	public static final BlockDataManager blockReloader = new BlockDataManager();
	public static final BiomeDataManager biomeReloader = new BiomeDataManager();
	public static final EntityTemperatureDataManager entityReloader = new EntityTemperatureDataManager();
	public static final FluidDataManager fluidReloader = new FluidDataManager();
	private static Survive instance;

	public static boolean isCombatLoaded() {
		return ModList.get().isLoaded("combat");
	}
	public static boolean isOriginsLoaded() {
		return ModList.get().isLoaded("origins");
	}

	public Survive() 
	{
		super("survive", () -> new SurviveClientSegment(), () -> new ServerSegment());
		instance = this;
		eventBus().addListener(this::clientRegistries);
		eventBus().addListener((Consumer<RegisterGuiOverlaysEvent>) event -> {
			GuiHelper.registerOverlays(event);
		});
		//		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.addListener((Consumer<PotionToFluidEvent>) event -> {
			if (event.getPotion() == SPotions.PURIFIED_WATER) {
				event.setFluid(SFluids.PURIFIED_WATER);
				event.setFlowingFluid(SFluids.FLOWING_PURIFIED_WATER);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((Consumer<FluidToPotionEvent.FromStateEvent>) event -> {
			if (event.getFluid().getType() instanceof PurifiedWaterFluid) {
				event.setPotion(SPotions.PURIFIED_WATER);
			}
		});
		isPrimalWinterLoaded = ModList.get().isLoaded("primalwinter");
	}
	
	@Override
	public void onModConstruct() {
		new FluidSTags();
		new ItemSTags();
		if (isCombatLoaded())SSpells.registerAll(eventBus());
		if (isOriginsLoaded())OriginsCompat.initOriginsPatcher();
		BlockPropertyHandlerImpl.init();
	}
	
	@Override
	public void onModStartup() {
		SCauldronInteraction.bootStrap();
		BrewingRecipes.addBrewingRecipes();
		CGameRules.init();

		for(Item item : ForgeRegistries.ITEMS) {
			if (item.isEdible())
				DataMaps.Server.defaultFood.put(ForgeRegistries.ITEMS.getKey(item), item.getFoodProperties());
		}
	}
	
	@Override
	public void onModStartupInClient() {
		RenderType frendertype = RenderType.translucent();
		ItemBlockRenderTypes.setRenderLayer(SFluids.PURIFIED_WATER, frendertype);
		ItemBlockRenderTypes.setRenderLayer(SFluids.FLOWING_PURIFIED_WATER, frendertype);
	}
	
	@Override
	public void setupConfigs(ConfigCollector collector) {
		collector.registerConfig(ServerConfig.class);
		collector.registerConfig(CONFIG);
		collector.registerConfig(HYGIENE_CONFIG); 
		collector.registerConfig(TEMPERATURE_CONFIG);
		collector.registerConfig(THIRST_CONFIG);
		collector.registerConfig(WELLBEING_CONFIG);
		collector.registerConfig(STAMINA_CONFIG);
	}
	
	@Override
	public void setupCommands(CommandCollector collector) {
		NeedsCommand.register(collector.dispatcher());
	}

	@Override
	public void setupRegistries(RegistryCollector collector) {
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
	
	@Override
	public void registerInserts(InsertCollector collector) {
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
				ResourceLocation table = Survive.getInstance().location("inject/" + name);
				return LootTableReference.lootTableReference(table).setWeight(weight);
			};
			
			LOOT_MODIFIERS.forEach((pair) -> {
				if(id.equals(pair.getKey())) {
					pair.getValue().forEach((file) -> {
						Survive.getInstance().debug("Injecting \""+file+"\" in "+pair.getKey());
						lootTable.get().addPool(LootPool.lootPool()
								.add(getInjectEntry.apply(file, 1))
								.setBonusRolls(UniformGenerator.between(0.0F, 1.0F))
								.name("survive_inject").build());
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
	
	@Override
	public void modifyDefaultEntityAttributes(DefaultAttributeModifier modifier) {
		super.modifyDefaultEntityAttributes(modifier);
		modifier.addToEntity(EntityType.PLAYER, SAttributes.COLD_RESISTANCE, SAttributes.HEAT_RESISTANCE, SAttributes.MAX_STAMINA);
	}
	
	@Override
	public void registerPackets(PacketCollector collector) {
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
	public static void registerDrinkDataForItem(ResourceLocation location, FoodJsonHolder drinkData) {
		DataMaps.Server.consummableItem.put(location, drinkData);
	}
	public static void registerDrinkDataForPotion(ResourceLocation location, PotionJsonHolder consummableData) {
		DataMaps.Server.potionDrink.put(location, consummableData);
	}
	public static void registerArmorTemperatures(ResourceLocation location, ArmorJsonHolder armorData) {
		DataMaps.Server.armor.put(location, armorData);
	}
	public static void registerBlockTemperatures(ResourceLocation location, BlockTemperatureJsonHolder drinkData) {
		DataMaps.Server.blockTemperature.put(location, drinkData);
	}
	public static void registerEntityTemperatures(ResourceLocation location, EntityTemperatureJsonHolder drinkData) {
		DataMaps.Server.entityTemperature.put(location, drinkData);
	}
	public static void registerBiomeTemperatures(ResourceLocation location, BiomeJsonHolder biomeData) {
		DataMaps.Server.biome.put(location, biomeData);
	}

	public void debug(Object message) {
		if (CONFIG.debugMode)getLogger().debug(message);
	}

	public void clientRegistries(final FMLClientSetupEvent event)
	{
		event.enqueueWork(()->{
			SItemProperties.registerAll();
		});
	}
	
	public static ItemStack convertToPlayerStatusBook(ItemStack stack) {
		ItemStack result = new ItemStack(Items.WRITTEN_BOOK);
		if (stack.getTag() != null) {
			result.setTag(stack.getTag().copy());
         }
		result.addTagElement("status_owner", StringTag.valueOf(""));
		result.getTag().putInt("generation", 0);
		return result;
	}
	
	@Override
	public void registerServerRelaodableResources(ReloadListeners reloadListener) {
		reloadListener.listenTo(consummableReloader);
		reloadListener.listenTo(potionReloader);
		reloadListener.listenTo(armorReloader);
		reloadListener.listenTo(blockReloader);
		reloadListener.listenTo(biomeReloader);
		reloadListener.listenTo(entityReloader);
		reloadListener.listenTo(fluidReloader);
	}
	
	@Override
	public void registerCreativeTabs(CreativeTabBuilder builder) {
		builder.addTab("main_tab", SCreativeModeTab.TAB_MAIN);
	}
	
	@Override
	public void populateCreativeTabs(CreativeTabPopulator populator) {
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

	public static List<String> defaultDimensionMods() {
		List<String> dims = new ArrayList<String>();
		dims.add("minecraft:overworld,0.0");
		dims.add("minecraft:the_nether,0.0");
		dims.add("minecraft:the_end,0.0");
		return dims;
	}
	
	public static Survive getInstance() {
		return instance;
	}
}
