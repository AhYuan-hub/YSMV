package cn.blockforge.generated.ysmv;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class VillagerEvents {
    private record Session(UUID token, UUID target, ResourceKey<Level> dim, long expire) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_OPEN = new HashMap<>();

    public static void openRequested(ServerPlayer player, ModNetwork.Request packet) {
        if (player == null || player.isSpectator()) return;
        Entity target = player.serverLevel().getEntity(packet.entityId());
        if (target == null || !WardrobeTarget.is(target) || !target.isAlive()) return;
        if (player.distanceToSqr(target) > 64 || !player.hasLineOfSight(target)) return;
        open(player, target);
    }

    private static void open(ServerPlayer player, Entity target) {
        long now = player.serverLevel().getGameTime();
        if (now - LAST_OPEN.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) < 10) return;
        LAST_OPEN.put(player.getUUID(), now);

        try {
            if (!YsmAccess.available()) {
                player.displayClientMessage(Component.literal("YSM 还没准备好"), false);
                return;
            }

            Map<?, ?> models = (Map<?, ?>) YsmAccess.staticCall("OoOoOoooO0O00oOoO00OOo00", YsmAccess.FIRST);
            List<String> allowed = new ArrayList<>();
            for (Object k : models.keySet()) {
                String id = k.toString();
                if (id.length() <= 256 && YsmAccess.allowed(player, id)) allowed.add(id);
                if (allowed.size() >= 4096) break;
            }
            Collections.sort(allowed);

            UUID token = UUID.randomUUID();
            SESSIONS.put(player.getUUID(), new Session(token, target.getUUID(),
                    player.level().dimension(), now + 12000));
            ModNetwork.send(player, new ModNetwork.Open(target.getId(), target.getUUID(), token,
                    Appearance.read((LivingEntity) target), List.copyOf(allowed)));
        } catch (Exception e) {
            YsmAccess.report(e);
            player.displayClientMessage(Component.literal("YSM 接口挂了，看看是不是 1.20.1 的 2.6.5"), false);
        }
    }

    public static void apply(ServerPlayer player, ModNetwork.Apply packet) {
        if (player == null) return;
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || !s.token().equals(packet.token())) return;

        Entity entity = player.serverLevel().getEntity(s.target());
        if (!s.dim().equals(player.level().dimension())
                || s.expire() < player.serverLevel().getGameTime()
                || !WardrobeTarget.is(entity) || !entity.isAlive()
                || !player.isAlive() || player.isSpectator()
                || player.distanceToSqr(entity) > 64
                || !player.hasLineOfSight(entity)) {
            ModNetwork.send(player, new ModNetwork.Result(packet.token(), false, "目标跑了或者太远"));
            return;
        }

        Appearance app = packet.appearance();
        try {
            if (!app.isDefault() && !YsmAccess.allowed(player, app.model())) {
                ModNetwork.send(player, new ModNetwork.Result(packet.token(), false, "没这个模型的权限"));
                return;
            }
            if (app.texture().chars().anyMatch(c -> c < 32)) {
                ModNetwork.send(player, new ModNetwork.Result(packet.token(), false, "皮肤名不合法"));
                return;
            }

            LivingEntity living = (LivingEntity) entity;
            (app.isDefault() ? Appearance.DEFAULT : app).save(living);
            ModNetwork.broadcast(living);
            ModNetwork.send(player, ModNetwork.snapshot(living));
            SESSIONS.remove(player.getUUID());
            ModNetwork.send(player, new ModNetwork.Result(packet.token(), true,
                    WardrobeTarget.displayName(living) + " 已保存"));
        } catch (Exception e) {
            YsmAccess.report(e);
            ModNetwork.send(player, new ModNetwork.Result(packet.token(), false, "接口报错，没改"));
        }
    }

    @SubscribeEvent
    public void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer p && WardrobeTarget.is(event.getTarget())) {
            ModNetwork.send(p, ModNetwork.snapshot((LivingEntity) event.getTarget()));
        }
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
        LAST_OPEN.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void stopped(ServerStoppedEvent event) {
        SESSIONS.clear();
        LAST_OPEN.clear();
    }
}
