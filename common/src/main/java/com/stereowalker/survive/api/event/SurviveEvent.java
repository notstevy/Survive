package com.stereowalker.survive.api.event;

import com.stereowalker.survive.world.temperature.TemperatureModifier;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.events.common.EntityEvent;
import org.jetbrains.annotations.NotNull;

public interface SurviveEvent {

    Event<TemperatureModifierSet> TEMPERATURE_MODIFIER_SET = EventFactory.createLoop();

    interface TemperatureModifierSet {
        void setModifier(TemperatureModifierResult modifier);
    }

    interface TemperatureModifierResult {
        @NotNull TemperatureModifier get();

        void set(@NotNull TemperatureModifier temperatureModifier);
    }

}
