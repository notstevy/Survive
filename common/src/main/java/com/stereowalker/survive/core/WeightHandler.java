package com.stereowalker.survive.core;

import com.stereowalker.survive.Survive;
import com.stereowalker.survive.world.DataMaps;
import com.stereowalker.survive.world.item.enchantment.SEnchantmentHelper;
import com.stereowalker.unionlib.util.RegistryHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


public class WeightHandler {

	@Environment(EnvType.CLIENT)
	public static float getArmorWeightClient(ItemStack piece) {
		float totalWeight = 0.0F;
		if (!piece.isEmpty()) {
			if (DataMaps.Client.armor.containsKey(RegistryHelper.items().getKey(piece.getItem())) && !SEnchantmentHelper.hasWeightless(piece)) {
				int i = SEnchantmentHelper.getFeatherweightModifier(piece);
				//Reduces the total weight of that armor piece by 18% for each level
				totalWeight += DataMaps.Client.armor.get(RegistryHelper.items().getKey(piece.getItem())).getWeightModifier() * (1 - i*0.18);
			}
		}
		return totalWeight;
	}

	public static float getTotalArmorWeight(LivingEntity player) {
		float totalWeight = 0.124F;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() == Type.ARMOR) {
				ItemStack stack = player.getItemBySlot(slot);
				if (Survive.STAMINA_CONFIG.enable_weights) {
					totalWeight += getArmorWeight(stack);
				}
			}
		}
		return totalWeight;
	}

	public static float getArmorWeight(ItemStack piece) {
		float totalWeight = 0.0F;
		if (!piece.isEmpty()) {
			if (DataMaps.Server.armor.containsKey(RegistryHelper.items().getKey(piece.getItem())) && !SEnchantmentHelper.hasWeightless(piece)) {
				int i = SEnchantmentHelper.getFeatherweightModifier(piece);
				//Reduces the total weight of that armor piece by 18% for each level
				totalWeight += DataMaps.Server.armor.get(RegistryHelper.items().getKey(piece.getItem())).getWeightModifier() * (1 - i*0.18);
			}
		}
		return totalWeight;
	}
}
