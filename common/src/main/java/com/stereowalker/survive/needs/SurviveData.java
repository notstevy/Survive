package com.stereowalker.survive.needs;

import java.util.Random;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public abstract class SurviveData {
	Random rng;
	public abstract void tick(Player player);
	public abstract void read(CompoundTag compound);
	public abstract void write(CompoundTag compound);
	public abstract void save(LivingEntity player);
	public abstract boolean shouldTick();
	
	public SurviveData() {
		this.rng = new Random();
	}
	
	public void baseTick(Player player) {
		if (shouldTick()) {
			tick(player);
			save(player);
		}
	}

	@Environment(EnvType.CLIENT)
	public void baseClientTick(AbstractClientPlayer player) {
		if (shouldTick()) {
			clientTick(player);
		}
	}

	@Environment(EnvType.CLIENT)
	public void clientTick(AbstractClientPlayer player) {
		
	}
}
