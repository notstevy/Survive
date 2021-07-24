package com.stereowalker.survive.client.events;

import java.util.List;

import com.stereowalker.survive.events.SurviveEvents;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.EquipmentSlotType.Group;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = "survive", value = Dist.CLIENT)
public class TooltipEvents {

	@OnlyIn(Dist.CLIENT)
	public static void accessoryTooltip(ItemStack stack, List<ITextComponent> tooltip) {
		tooltip.add(1, new TranslationTextComponent("tooltip.survive.weight", SurviveEvents.getArmorWeight(stack)).mergeStyle(TextFormatting.DARK_PURPLE));
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void tooltips(ItemTooltipEvent event) {
		boolean showWeight = false;
		for(EquipmentSlotType type : EquipmentSlotType.values()) {
			if (event.getItemStack().canEquip(type, event.getPlayer()) && type.getSlotType() == Group.ARMOR) {
				showWeight = true;
				break;
			}
		}
		if (showWeight) {
			accessoryTooltip(event.getItemStack(), event.getToolTip());
		}
	}
}
