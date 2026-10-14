package cn.blockforge.generated.ysmv.client;

import cn.blockforge.generated.ysmv.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private record Key(ResourceLocation dim, UUID uuid) {}

    private static final Map<Key, Appearance> DATA = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Appearance> e) {
            return size() > 8192;
        }
    };

    static LivingEntity previewEntity;
    static Appearance previewAppearance = Appearance.DEFAULT;

    public static final KeyMapping WARDROBE_KEY = new KeyMapping(
            "key.ysmv.wardrobe", InputConstants.Type.KEYSYM, 340, KeyMapping.CATEGORY_GAMEPLAY);

    private static boolean requestSent;

    private static void open(ModNetwork.Open p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(p.entityId());
        if (!(e instanceof LivingEntity living)) return;
        if (!WardrobeTarget.is(living) || !e.getUUID().equals(p.entityUuid()) || !e.isAlive()) return;
        DATA.put(new Key(e.level().dimension().location(), e.getUUID()), p.appearance());
        mc.setScreen(new VillagerScreen(living, p));
    }

    private static void sync(ModNetwork.Sync p) {
        DATA.put(new Key(p.dimension(), p.entityUuid()), p.appearance());
    }

    private static void result(ModNetwork.Result p) {
        if (Minecraft.getInstance().screen instanceof VillagerScreen s) s.result(p);
    }

    @SubscribeEvent
    public static void render(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity living = event.getEntity();
        if (!WardrobeTarget.is(living)) return;
        Appearance app = living == previewEntity ? previewAppearance
                : DATA.getOrDefault(new Key(living.level().dimension().location(), living.getUUID()), Appearance.DEFAULT);
        if (app.isDefault()) return;
        if (YsmClient.draw(living, app, event.getPartialTick(), event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void name(RenderNameTagEvent event) {
        if (!YsmClient.isProxy(event.getEntity())) return;
        if (!event.getEntity().hasCustomName() || previewEntity != null) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        } else {
            event.setContent(event.getEntity().getCustomName());
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof LivingEntity living && WardrobeTarget.is(living)) {
            DATA.remove(new Key(event.getLevel().dimension().location(), living.getUUID()));
            YsmClient.forget(living);
        }
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == 0) {
            requestSent = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (event.getButton() != 1 || event.getAction() != 1 || !WARDROBE_KEY.isDown()
                || requestSent || mc.screen != null || mc.player == null || mc.level == null) return;
        if (!(mc.hitResult instanceof EntityHitResult hit)) return;
        if (!WardrobeTarget.is(hit.getEntity()) || !hit.getEntity().isAlive()) return;
        event.setCanceled(true);
        requestSent = true;
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.Request(hit.getEntity().getId()));
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) clearAll();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearAll();
    }

    private static void clearAll() {
        DATA.clear();
        YsmClient.clear();
        previewEntity = null;
        previewAppearance = Appearance.DEFAULT;
    }

    @Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(WARDROBE_KEY);
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ModNetwork.onOpen = ClientEvents::open;
                ModNetwork.onSync = ClientEvents::sync;
                ModNetwork.onResult = ClientEvents::result;
            });
        }

        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers event) {
            YsmClient.initialize(event.getContext());
        }
    }
}