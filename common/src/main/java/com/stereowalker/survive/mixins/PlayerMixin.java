package com.stereowalker.survive.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.stereowalker.survive.Survive;
import com.stereowalker.survive.core.SurviveEntityStats;
import com.stereowalker.survive.json.ConsummableJsonHolder;
import com.stereowalker.survive.needs.CustomFoodData;
import com.stereowalker.survive.needs.HygieneData;
import com.stereowalker.survive.needs.IRealisticEntity;
import com.stereowalker.survive.needs.NutritionData;
import com.stereowalker.survive.needs.SleepData;
import com.stereowalker.survive.needs.StaminaData;
import com.stereowalker.survive.needs.TemperatureData;
import com.stereowalker.survive.needs.WaterData;
import com.stereowalker.survive.needs.WellbeingData;
import com.stereowalker.survive.world.DataMaps;
import com.stereowalker.unionlib.util.RegistryHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements IRealisticEntity {
	@Shadow protected FoodData foodData;
	@Shadow private int sleepCounter;
	private WellbeingData wellbeingData = new WellbeingData();
	private NutritionData nutritionData = new NutritionData();

	protected PlayerMixin(EntityType<? extends LivingEntity> type, Level worldIn) {
		super(type, worldIn);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	public void initInject(CallbackInfo ci) {
		this.foodData = new CustomFoodData(this.foodData);
	}
	
	@Inject(method = "eat", at = @At("HEAD"))
	public void eatInject(Level pLevel, ItemStack pFood, CallbackInfoReturnable<ItemStack> cir) {
		this.getStaminaData().eat(pFood.getItem(), pFood, this);
		this.getWaterData().drink(pFood.getItem(), pFood, this);
		this.getRealFoodData().markAsSpoiled(pFood, this);
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", shift = Shift.AFTER, target = "Lnet/minecraft/world/entity/player/Player;updateIsUnderwater()Z"))
	public void tickInject(CallbackInfo ci) {
		SurviveEntityStats.addStatsOnSpawn((Player)(Object)this);
		//
		if (!this.level().isClientSide && (Player)(Object)this instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer)(Object)this;
			if (Survive.THIRST_CONFIG.enabled) {
				if (player.level().getDifficulty() == Difficulty.PEACEFUL && player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
					if (getWaterData().needWater() && player.tickCount % 10 == 0) {
						getWaterData().setWaterLevel(getWaterData().getWaterLevel() + 1);
					}
				}
				getWaterData().save(player);
			}
		}
		//
		if (!this.level().isClientSide) {
			getStaminaData().baseTick((Player)(Object)this);
			getHygieneData().baseTick((Player)(Object)this);
			this.nutritionData.baseTick((Player)(Object)this);
			getTemperatureData().baseTick((Player)(Object)this);
			getWaterData().baseTick((Player)(Object)this);
			this.wellbeingData.baseTick((Player)(Object)this);
			getSleepData().baseTick((Player)(Object)this);
		}
	}
	
	@Redirect(method = "canEat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;needsFood()Z"))
	public boolean makeEdible(FoodData foodData) {
		if (foodData instanceof CustomFoodData) {
			return ((CustomFoodData)foodData).canConsumeFood();
		} else {
			return foodData.needsFood();
		}
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"), method = {"jumpFromGround", "actuallyHurt", "checkMovementStatistics"})
	public void morphExhaustion(Player player, float value) {
		if (Survive.STAMINA_CONFIG.enabled) {
			getStaminaData().addExhaustion(player, value*2.5f, "Jumped, Got hurt or moved");
		}
		else if (Survive.CONFIG.nutrition_enabled) {
			this.nutritionData.removeCarbs(Mth.ceil(value*2.5f));
		}
		else {
			player.causeFoodExhaustion(value);
		}
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"), method = "attack")
	public void morphStaminaDuringAttack(Player player, float value) {
		if (Survive.STAMINA_CONFIG.enabled) {
			getStaminaData().addExhaustion(player, 1.25f, "Player Attacked");
		}
		else if (Survive.CONFIG.nutrition_enabled) {
			this.nutritionData.removeCarbs(Mth.ceil(value*2.5f));
		}
		else {
			player.causeFoodExhaustion(value);
		}
	}

	@Inject(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V"))
	//	@Inject(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;)V"))
	public void addNutrients(Level arg0, ItemStack p_213357_2_, CallbackInfoReturnable<ItemStack> cir) {
		if (Survive.CONFIG.nutrition_enabled) {
			float protein = 1;
			float carbs = 1;
			if (DataMaps.Server.consummableItem.containsKey(RegistryHelper.items().getKey(p_213357_2_.getItem()))) {
				ConsummableJsonHolder data = DataMaps.Server.consummableItem.get(RegistryHelper.items().getKey(p_213357_2_.getItem()));
				protein = data.getProteinRatio();
				carbs = data.getCarbohydrateRatio();
			}
			FoodProperties food = p_213357_2_.getItem().getFoodProperties();
			float total = protein+carbs;
			this.nutritionData.addCarbs(food.getNutrition()*Mth.ceil((carbs/total)*10));
			this.nutritionData.addProtein(food.getNutrition()*Mth.ceil((protein/total)*10));
		}
	}
	
	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void readAdditionalSaveData_inject(CompoundTag pCompound, CallbackInfo ci) {
		if (pCompound.contains("surviveData", 10)) {
			this.wellbeingData.read(pCompound);
			this.nutritionData.read(pCompound);
		}
	}
	
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	public void addAdditionalSaveData_inject(CompoundTag pCompound, CallbackInfo ci) {
		if (!pCompound.contains("surviveData", 10)) {
			pCompound.put("surviveData", new CompoundTag());
		}
		this.wellbeingData.write(pCompound.getCompound("surviveData"));
		this.nutritionData.write(pCompound.getCompound("surviveData"));
	}

	public StaminaData getStaminaData() {
		return SurviveEntityStats.getEnergyStats((Player)(Object)this);
	}

	public HygieneData getHygieneData(){
		return SurviveEntityStats.getHygieneStats((Player)(Object)this);
	}

	public NutritionData getNutritionData(){
		return this.nutritionData;
	}
	
	@Override
	public void setNutritionData(NutritionData data) {
		this.nutritionData = data;
	}

	public TemperatureData getTemperatureData(){
		return SurviveEntityStats.getTemperatureStats((Player)(Object)this);
	}

	public WaterData getWaterData(){
		return SurviveEntityStats.getWaterStats((Player)(Object)this);
	}

	@Override
	public WellbeingData getWellbeingData(){
		return this.wellbeingData;
	}
	
	@Override
	public void setWellbeingData(WellbeingData data) {
		this.wellbeingData = data;
	}

	public SleepData getSleepData(){
		return SurviveEntityStats.getSleepStats((Player)(Object)this);
	}

	public CustomFoodData getRealFoodData(){
		if (foodData instanceof CustomFoodData)
			return (CustomFoodData) foodData;
		else return null;
	}

}
