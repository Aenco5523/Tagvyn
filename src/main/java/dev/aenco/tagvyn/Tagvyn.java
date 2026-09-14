package dev.aenco.tagvyn;

import dev.aenco.tagvyn.api.TagvynAPI;
import dev.aenco.tagvyn.config.TagvynConfig;
import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.network.TagvynNetwork;
import dev.aenco.tagvyn.service.NeoForgeTagvynApi;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Tagvyn.MOD_ID)
public final class Tagvyn {
    public static final String MOD_ID = "tagvyn";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Tagvyn(IEventBus modBus, ModContainer modContainer) {
        TagvynAttachments.register(modBus);
        TagvynNetwork.register(modBus);
        TagvynAPI.bootstrap(NeoForgeTagvynApi.INSTANCE);
        modContainer.registerConfig(ModConfig.Type.COMMON, TagvynConfig.SPEC);
    }
}
