package com.stereowalker.survive.world.temperature.conditions;

import com.google.gson.JsonObject;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class OnFireCondition extends TemperatureChangeCondition<OnFireCondition.Instance>{

	public OnFireCondition() {
		super();
	}

	@Override
	public Instance createInstance(JsonObject object) {
		float temperatureIn = 0;
		int fireTimerIn = 0;
		String operationIn = "";
		if(object.has("temperature") && object.get("temperature").isJsonPrimitive()) {
			temperatureIn = object.get("temperature").getAsFloat();
		}
		if(object.has("fireTimer") && object.get("fireTimer").isJsonPrimitive()) {
			fireTimerIn = object.get("fireTimer").getAsInt();
		}
		if(object.has("operation") && object.get("operation").isJsonPrimitive()) {
			operationIn = object.get("operation").getAsString();
		}
		return new Instance(temperatureIn, fireTimerIn, operationIn);
	}
	
	@Override
	public Instance createInstance(CompoundTag nbt) {
		float temperatureIn = nbt.getFloat("temperature");
		int fireTimerIn = nbt.getInt("fireTimer");
		String operationIn = nbt.getString("operation");
		return new Instance(temperatureIn, fireTimerIn, operationIn);
	}

	static class Instance extends TemperatureChangeInstance {
		private int fireTimer;
		private String operation;

		public Instance(float temperatureIn, int fireTimerIn, String operationIn) {
			super(temperatureIn);
			this.fireTimer = fireTimerIn;
			this.operation = operationIn;
		}

		@Override
		public boolean shouldChangeTemperature(Player player) {
			if (player.isOnFire()) {
				if (this.fireTimer > 0) {
					if (this.operation.equals("=")) {
						return player.getRemainingFireTicks() == this.fireTimer;
					} else if (this.operation.equals(">=")) {
						return player.getRemainingFireTicks() >= this.fireTimer;
					} else if (this.operation.equals("<=")) {
						return player.getRemainingFireTicks() <= this.fireTimer;
					} else if (this.operation.equals(">")) {
						return player.getRemainingFireTicks() > this.fireTimer;
					} else if (this.operation.equals("<")) {
						return player.getRemainingFireTicks() < this.fireTimer;
					}
					return player.getRemainingFireTicks() < this.fireTimer;
				} else {
					return true;
				}
			} else {
				return false;
			}
		}

		@Override
		public CompoundTag serialize() {
			CompoundTag nbt = new CompoundTag();
			nbt.putFloat("temperature", this.getTemperature());
			nbt.putInt("fireTimer", this.fireTimer);
			nbt.putString("operation", this.operation);
			return nbt;
		}
		
		@Override
		@Environment(EnvType.CLIENT)
		public Component getAdditionalContext() {
			if (this.fireTimer > 0) {
				return Component.translatable("temperature_context.on_fire", this.operation.equals("") ? "<" : this.operation, this.fireTimer);
			} else {
				return Component.translatable("temperature_context.on_fire_no_timer");
			}
		}
	}
}
