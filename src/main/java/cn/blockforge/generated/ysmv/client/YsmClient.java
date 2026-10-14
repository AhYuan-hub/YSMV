package cn.blockforge.generated.ysmv.client;

import cn.blockforge.generated.ysmv.*;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class YsmClient {
    public record Model(String id, String name, List<String> textures) {}

    private static Object renderer;
    private static Method render, setModel, refresh, ready;
    private static String lastError = "";

    private static final Map<LivingEntity, Avatar> AVATARS = new WeakHashMap<>();
    private static final Map<LivingEntity, Avatar> PREVIEWS = new WeakHashMap<>();
    private static final Set<Player> PROXIES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<String> FAILED = new HashSet<>();

    private static final class Avatar {
        final RemotePlayer player;
        final Object anim;
        Appearance appearance = Appearance.DEFAULT;
        int lastTick = Integer.MIN_VALUE;

        Avatar(RemotePlayer player, Object anim) {
            this.player = player;
            this.anim = anim;
        }
    }

    public static void initialize(EntityRendererProvider.Context ctx) {
        clear();
        renderer = null;
        try {
            Class<?> rc = YsmAccess.type("o0ooO0o0ooo000OOO0o0oo0o");
            Class<?> ac = YsmAccess.type("o0O0oOooOo0OoOo0oOo00O00");
            renderer = rc.getConstructor(EntityRendererProvider.Context.class).newInstance(ctx);
            render = YsmAccess.method(rc, YsmAccess.FIRST,
                    ac, float.class, float.class, PoseStack.class, MultiBufferSource.class, int.class);
            setModel = YsmAccess.method(ac, YsmAccess.FIRST, String.class, String.class);
            refresh = YsmAccess.method(ac, "oOo0o0000OOOO0OooooO00oo");
            ready = YsmAccess.method(ac, "oOOo0Ooo0oOoo0O0OOOOo0oo");
            lastError = "";
        } catch (Exception e) {
            fail(e);
        }
    }

    public static String error() { return lastError; }

    public static void clear() {
        AVATARS.clear();
        PREVIEWS.clear();
        PROXIES.clear();
        FAILED.clear();
    }

    public static boolean isProxy(net.minecraft.world.entity.Entity e) { return PROXIES.contains(e); }

    public static void forget(LivingEntity e) {
        AVATARS.remove(e);
        PREVIEWS.remove(e);
    }

    public static void clearPreview(LivingEntity e) { PREVIEWS.remove(e); }

    public static List<Model> models(List<String> allowed) {
        List<Model> out = new ArrayList<>();
        try {
            Map<?, ?> map = (Map<?, ?>) YsmAccess.staticCall("o0OooO00ooo0OO000O0OoOoO", YsmAccess.SECOND);
            for (String id : allowed) {
                Object model = map.get(id);
                if (model == null) continue;
                Object def = YsmAccess.call(model, YsmAccess.FIRST);
                Map<?, ?> tex = (Map<?, ?>) YsmAccess.call(def, "oooooooOOoOOoO00OooOo00O");
                List<String> skins = tex.keySet().stream()
                        .map(Object::toString)
                        .filter(s -> s.length() <= 256)
                        .sorted()
                        .toList();
                Object meta = YsmAccess.call(model, "Ooooo0oooO0oooOOOoO0000O");
                Object info = YsmAccess.call(meta, YsmAccess.FIRST);
                String title = info == null ? id : (String) YsmAccess.call(info, YsmAccess.FIRST);
                if (title == null || title.isBlank()) title = id;
                out.add(new Model(id, title, skins));
            }
        } catch (Exception e) {
            fail(e);
        }
        out.sort(Comparator.comparing(Model::name).thenComparing(Model::id));
        return out;
    }

    private static Avatar avatar(LivingEntity e) throws ReflectiveOperationException {
        Map<LivingEntity, Avatar> cache = e == ClientEvents.previewEntity ? PREVIEWS : AVATARS;
        Avatar a = cache.get(e);
        if (a != null) return a;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        RemotePlayer proxy = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "VillagerModel"));
        Object anim = YsmAccess.capability(proxy, "O0OooOo0oOOoOoOoOooO000o");
        if (anim == null) {
            anim = YsmAccess.type("o0OOO0o0o0OOo000oO00o00O")
                    .getConstructor(Player.class).newInstance(proxy);
        }

        a = new Avatar(proxy, anim);
        cache.put(e, a);
        PROXIES.add(proxy);
        return a;
    }

    public static boolean draw(LivingEntity e, Appearance app, float partialTick,
                               PoseStack poses, MultiBufferSource buffers, int light) {
        if (renderer == null || app.isDefault() || FAILED.contains(app.model())) return false;
        try {
            Map<?, ?> map = (Map<?, ?>) YsmAccess.staticCall("o0OooO00ooo0OO000O0OoOoO", YsmAccess.SECOND);
            if (!map.containsKey(app.model())) return false;

            Avatar a = avatar(e);
            if (a == null) return false;

            if (!a.appearance.equals(app)) {
                setModel.invoke(a.anim, app.model(), app.texture());
                a.appearance = app;
            }

            copyPose(e, a);
            refresh.invoke(a.anim);
            if (!Boolean.TRUE.equals(ready.invoke(a.anim))) return false;

            // 单独开 PoseStack，第三方模型有时候会漏 popMatrix
            PoseStack iso = new PoseStack();
            iso.last().pose().set(poses.last().pose());
            iso.last().normal().set(poses.last().normal());
            if (e.isBaby()) iso.scale(0.5F, 0.5F, 0.5F);

            render.invoke(renderer, a.anim, e.getYRot(), partialTick, iso, buffers, light);
            return true;
        } catch (Exception ex) {
            FAILED.add(app.model());
            fail(ex);
            return false;
        }
    }

    private static void copyPose(LivingEntity src, Avatar a) {
        RemotePlayer t = a.player;

        t.setPos(src.getX(), src.getY(), src.getZ());
        t.xo = src.xo; t.yo = src.yo; t.zo = src.zo;
        t.xOld = src.xOld; t.yOld = src.yOld; t.zOld = src.zOld;

        t.setYRot(src.getYRot()); t.yRotO = src.yRotO;
        t.setXRot(src.getXRot()); t.xRotO = src.xRotO;
        t.yBodyRot = src.yBodyRot; t.yBodyRotO = src.yBodyRotO;
        t.yHeadRot = src.yHeadRot; t.yHeadRotO = src.yHeadRotO;

        t.tickCount = src.tickCount;
        t.setDeltaMovement(src.getDeltaMovement());
        t.setOnGround(src.onGround());
        t.setPose(src.getPose());
        t.setInvisible(src.isInvisible());

        src.getSleepingPos().ifPresentOrElse(t::setSleepingPos, t::clearSleepingPos);

        t.setSprinting(src.isSprinting());
        t.setSwimming(src.isSwimming());
        t.setShiftKeyDown(src.isShiftKeyDown());
        t.walkDist = src.walkDist; t.walkDistO = src.walkDistO;

        t.setCustomName(src.getCustomName());
        t.setCustomNameVisible(src.isCustomNameVisible());

        t.hurtTime = src.hurtTime;
        t.deathTime = src.deathTime;
        t.attackAnim = src.attackAnim; t.oAttackAnim = src.oAttackAnim;
        t.swinging = src.swinging; t.swingTime = src.swingTime; t.swingingArm = src.swingingArm;
        t.setHealth(src.getHealth());

        if (a.lastTick != src.tickCount) {
            t.walkAnimation.update(src.walkAnimation.speed(), 1.0F);
            a.lastTick = src.tickCount;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                t.setItemSlot(slot, src.getItemBySlot(slot).copy());
            }
        }
    }

    private static void fail(Throwable e) {
        lastError = "YSM 接口炸了，先用原版";
        YsmAccess.report(e);
    }
}