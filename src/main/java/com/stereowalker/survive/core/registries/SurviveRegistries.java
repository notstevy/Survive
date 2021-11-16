package com.stereowalker.survive.core.registries;

import com.stereowalker.survive.world.temperature.TemperatureChangeCondition;

import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

public class SurviveRegistries {
	public static final IForgeRegistry<TemperatureChangeCondition<?>> CONDITION = RegistryManager.ACTIVE.getRegistry(TemperatureChangeCondition.class);
}