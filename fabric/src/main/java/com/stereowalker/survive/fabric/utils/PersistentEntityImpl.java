package com.stereowalker.survive.fabric.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public class PersistentEntityImpl {
    public static CompoundTag getPersistentData(Entity entity) {
        return ((PersistentDataEntityStorage) entity).survive$getPersistentData();
    }
}
