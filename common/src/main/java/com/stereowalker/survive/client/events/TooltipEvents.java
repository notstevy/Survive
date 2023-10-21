package com.stereowalker.survive.client.events;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.stereowalker.survive.Survive;
import com.stereowalker.survive.core.WeightHandler;
import com.stereowalker.survive.world.DataMaps;
import com.stereowalker.survive.world.temperature.conditions.TemperatureChangeInstance;
import com.stereowalker.unionlib.util.RegistryHelper;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;;

@Environment(EnvType.CLIENT)
public class TooltipEvents {

	public static void register() {
		ClientTooltipEvent.ITEM.register(TooltipEvents::tooltips);
	}


	@Environment(EnvType.CLIENT)
	private static void accessoryTooltip(Player player, ItemStack stack, List<Component> tooltip, boolean displayWeight, boolean displayTemp) {
		List<Component> tooltipsToAdd = new ArrayList<Component>();
		if (DataMaps.Client.armor.containsKey(RegistryHelper.items().getKey(stack.getItem()))) {
			float kg = WeightHandler.getArmorWeightClient(stack);
			float rawPound = kg*2.205f;
			int poundInt = (int)(rawPound*1000);
			float pound = poundInt/1000.0F;
			if (displayWeight) tooltipsToAdd.add(Component.translatable("tooltip.survive.weight", Survive.STAMINA_CONFIG.displayWeightInPounds ? pound : kg, Survive.STAMINA_CONFIG.displayWeightInPounds ? "lbs" : "kg").withStyle(ChatFormatting.DARK_PURPLE));
			if (displayTemp)
				for (Pair<String,TemperatureChangeInstance> instance : DataMaps.Client.armor.get(RegistryHelper.items().getKey(stack.getItem())).getTemperatureModifier()) {
					if (instance.getSecond().shouldChangeTemperature(player)) {
						if (instance.getSecond().getAdditionalContext() != null)
							tooltipsToAdd.add(Component.translatable("tooltip.survive.temperature", instance.getSecond().getTemperature()).append(instance.getSecond().getAdditionalContext()).withStyle(ChatFormatting.DARK_PURPLE));
						else
							tooltipsToAdd.add(Component.translatable("tooltip.survive.temperature", instance.getSecond().getTemperature()).withStyle(ChatFormatting.DARK_PURPLE));
						break;
					}
				}
		} else {
			if (displayWeight) tooltipsToAdd.add(Component.translatable("tooltip.survive.weight", 0, Survive.STAMINA_CONFIG.displayWeightInPounds ? "lbs" : "kg").withStyle(ChatFormatting.DARK_PURPLE));
			if (displayTemp) tooltipsToAdd.add(Component.translatable("tooltip.survive.temperature", 0).withStyle(ChatFormatting.DARK_PURPLE));
		}

		tooltip.addAll(1, tooltipsToAdd);
	}

	private static void tooltips(ItemStack itemStack, List<Component> components, TooltipFlag tooltipFlag) {
		Player player = Minecraft.getInstance().player;
		boolean showWeight = false;
		boolean showTemp = false;
		if (player != null && (Survive.STAMINA_CONFIG.enabled && Survive.STAMINA_CONFIG.enable_weights) || Survive.TEMPERATURE_CONFIG.enabled) {
			for(EquipmentSlot type : EquipmentSlot.values()) {
				if (type.getType() == Type.ARMOR && Mob.getEquipmentSlotForItem(itemStack) == type) {
					showWeight = Survive.STAMINA_CONFIG.enabled && Survive.STAMINA_CONFIG.enable_weights;
					showTemp = Survive.TEMPERATURE_CONFIG.enabled;
					break;
				}
			}
		}

		if (showWeight || showTemp) {
			accessoryTooltip(player, itemStack, components, showWeight, showTemp);
		}
	}
}
