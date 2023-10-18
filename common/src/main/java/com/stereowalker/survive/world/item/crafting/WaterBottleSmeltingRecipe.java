package com.stereowalker.survive.world.item.crafting;

import com.stereowalker.survive.world.item.alchemy.SPotions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

public class WaterBottleSmeltingRecipe extends SmeltingRecipe {

	public WaterBottleSmeltingRecipe(ResourceLocation pId, String pGroup, CookingBookCategory pCategory, Ingredient pIngredient, ItemStack pResult, float pExperience, int pCookingTime) {
		super(pId, pGroup, pCategory, Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)), PotionUtils.setPotion(new ItemStack(Items.POTION), SPotions.PURIFIED_WATER), pExperience, pCookingTime);
	}

	@Override
	public boolean matches(Container pInv, Level pLevel) {
		if (PotionUtils.getPotion(pInv.getItem(0)) == Potions.WATER) return this.ingredient.test(pInv.getItem(0)); else return false;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SRecipeSerializer.PURIFIED_WATER_BOTTLE;
	}

}
