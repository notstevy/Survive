package com.stereowalker.survive.world.temperature.conditions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public abstract class TemperatureChangeInstance {
	
	private float temperature;

	public TemperatureChangeInstance(float temperatureIn) {
		this.temperature = temperatureIn;
	}
	
	public abstract boolean shouldChangeTemperature(Player player);
	
	public abstract CompoundTag serialize();
	
	public float getTemperature() {
		return temperature;
	}

	@Environment(EnvType.CLIENT)
	@Nullable
	public Component getAdditionalContext() {
		return null;
	}
}
