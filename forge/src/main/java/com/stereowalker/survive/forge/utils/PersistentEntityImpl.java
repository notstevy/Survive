package com.stereowalker.survive.forge.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public class PersistentEntityImpl {

    public static CompoundTag getPersistentData(Entity entity) {
        return entity.getPersistentData();
    }

}
