package cn.blockforge.generated.ysmv.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = cn.blockforge.generated.ysmv.GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class HudOverlay {
    private static long firstShow = -1L;
    private static boolean done;

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type() || done) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        if (firstShow == -1L) firstShow = System.currentTimeMillis();
        long elapsed = System.currentTimeMillis() - firstShow;
        if (elapsed > 5000) {
            done = true;
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        String text = "喵❤";
        int x = (event.getWindow().getGuiScaledWidth() - mc.font.width(text)) / 2;
        int y = event.getWindow().getGuiScaledHeight() - 45;

        int alpha = 0xFF;
        if (elapsed > 4000) {
            alpha = (int) (0xFF * (1.0 - (elapsed - 4000) / 1000.0));
            if (alpha <= 0) return;
        }
        g.drawString(mc.font, text, x, y, (alpha << 24) | 0xFFFFFF, false);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        firstShow = -1L;
        done = false;
    }
}