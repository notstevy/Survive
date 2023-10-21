package com.stereowalker.survive.forge;

import com.stereowalker.survive.GuiHelper;
import com.stereowalker.survive.Survive;
import com.stereowalker.survive.SurviveClientSegment;
import com.stereowalker.survive.SurviveRegistry;
import com.stereowalker.survive.compat.SItemProperties;
import com.stereowalker.survive.world.item.alchemy.SPotions;
import com.stereowalker.survive.world.level.material.PurifiedWaterFluid;
import com.stereowalker.survive.world.level.material.SFluids;
import com.stereowalker.unionlib.event.potionfluid.FluidToPotionEvent;
import com.stereowalker.unionlib.event.potionfluid.PotionToFluidEvent;
import com.stereowalker.unionlib.mod.MinecraftMod;
import com.stereowalker.unionlib.mod.ServerSegment;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Consumer;

@Mod(Survive.MOD_ID)
public class SurviveModForge extends MinecraftMod implements Survive {
    public SurviveModForge() {
        super(MOD_ID, SurviveClientSegment::new, ServerSegment::new);
        SurviveRegistry.setInstance(this);
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(Survive.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        eventBus().addListener(this::clientRegistries);
        eventBus().addListener((Consumer<RegisterGuiOverlaysEvent>) event -> {
            GuiHelper.registerOverlays(event);
        });

        MinecraftForge.EVENT_BUS.addListener((Consumer<PotionToFluidEvent>) event -> {
            if (event.getPotion() == SPotions.PURIFIED_WATER) {
                event.setFluid(SFluids.PURIFIED_WATER);
                event.setFlowingFluid(SFluids.FLOWING_PURIFIED_WATER);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((Consumer<FluidToPotionEvent.FromStateEvent>) event -> {
            if (event.getFluid().getType() instanceof PurifiedWaterFluid) {
                event.setPotion(SPotions.PURIFIED_WATER);
            }
        });
    }

    public void clientRegistries(FMLClientSetupEvent event) {
        event.enqueueWork(SItemProperties::registerAll);
    }
}
