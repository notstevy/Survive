package com.stereowalker.survive;

import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Map;

public class SurviveRegistry {

    public static Map<Potion, List<Fluid>> POTION_FLUID_MAP;
    private static Survive instance;


    public static Survive getInstance() {
        return instance;
    }

    public static void setInstance(Survive instance) {
        SurviveRegistry.instance = instance;
    }
}
