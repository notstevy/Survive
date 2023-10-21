package com.stereowalker.survive.utils;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class PersistentEntity {

    @ExpectPlatform
    public static @NotNull CompoundTag getPersistentData(@NotNull Entity entity) {
        throw new AssertionError();
    }

}
