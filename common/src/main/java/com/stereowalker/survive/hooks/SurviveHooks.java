package com.stereowalker.survive.hooks;

import com.stereowalker.survive.api.event.SurviveEvent;
import com.stereowalker.survive.world.temperature.TemperatureModifier;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class SurviveHooks {

	public static TemperatureModifier getTemperatureModifer(LivingEntity entity, TemperatureModifier originalModifier)
	{

		class TemperatureModifierResultImpl implements SurviveEvent.TemperatureModifierResult {
			private TemperatureModifier temperatureModifier;

			private TemperatureModifierResultImpl(@NotNull TemperatureModifier originalModifier) {
				this.temperatureModifier = originalModifier;
			}

			@Override
			public @NotNull TemperatureModifier get() {
				return temperatureModifier;
			}

			@Override
			public void set(@NotNull TemperatureModifier temperatureModifier) {
				this.temperatureModifier = temperatureModifier;
			}
		}

		TemperatureModifierResultImpl result = new TemperatureModifierResultImpl(originalModifier);
		SurviveEvent.TEMPERATURE_MODIFIER_SET.invoker()
				.setModifier(result);

		return result.get();
	}
}

