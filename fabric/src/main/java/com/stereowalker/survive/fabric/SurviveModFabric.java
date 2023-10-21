package com.stereowalker.survive.fabric;

import com.stereowalker.survive.Survive;
import com.stereowalker.survive.SurviveClientSegment;
import com.stereowalker.survive.SurviveRegistry;
import com.stereowalker.survive.compat.SItemProperties;
import com.stereowalker.unionlib.mod.ClientSegment;
import com.stereowalker.unionlib.mod.MinecraftMod;
import com.stereowalker.unionlib.mod.ServerSegment;
import net.fabricmc.api.ModInitializer;

public class SurviveModFabric extends MinecraftMod implements ModInitializer, Survive {

    public SurviveModFabric() {
        super(MOD_ID, SurviveClientSegment::new, ServerSegment::new);
        SurviveRegistry.setInstance(this);

    }

    @Override
    public void onModStartupInClient() {
        super.onModStartupInClient();
        SItemProperties.registerAll();
    }

    @Override
    public void setupConfigScreen(ClientSegment client) {
        super.setupConfigScreen(client);
    }

}
