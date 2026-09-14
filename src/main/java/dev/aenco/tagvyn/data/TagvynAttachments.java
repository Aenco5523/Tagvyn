package dev.aenco.tagvyn.data;

import dev.aenco.tagvyn.Tagvyn;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class TagvynAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Tagvyn.MOD_ID);

    public static final Supplier<AttachmentType<IdentityData>> IDENTITY = ATTACHMENTS.register(
            "identity",
            () -> AttachmentType.builder(IdentityData::empty)
                    .serialize(IdentityData.CODEC)
                    .copyOnDeath()
                    .sync(IdentityData.STREAM_CODEC)
                    .build()
    );

    private TagvynAttachments() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
