package com.stereowalker.survive.fabric.mixins;

import com.stereowalker.survive.Survive;
import com.stereowalker.survive.fabric.utils.PersistentDataEntityStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class SurvivePersistentDataStorageMixin implements PersistentDataEntityStorage {
    @Unique
    private CompoundTag persistentData;
    @Unique
    private static final String persistentDataId = Survive.MOD_ID + ".persistent_data";


    @Inject(method = "load", at = @At("HEAD"))
    protected void loadPersistentData(CompoundTag compound, CallbackInfo ci) {
        if(compound.contains(persistentDataId, 10)) {
            persistentData = compound.getCompound(persistentDataId);
        }
    }

    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    protected void writePersistentData(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        if(persistentData == null)
            return;

        compound.put(persistentDataId, persistentData);
    }

    @Override
    public CompoundTag survive$getPersistentData() {
        if(this.persistentData == null) {
            this.persistentData = new CompoundTag();
        }

        return persistentData;
    }

}
