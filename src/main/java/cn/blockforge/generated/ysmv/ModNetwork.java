package cn.blockforge.generated.ysmv;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GeneratedMod.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static Consumer<Open> onOpen = p -> {};
    public static Consumer<Sync> onSync = p -> {};
    public static Consumer<Result> onResult = p -> {};

    public static void register() {
        CHANNEL.messageBuilder(Open.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Open::encode).decoder(Open::decode)
                .consumerMainThread((p, ctx) -> onOpen.accept(p)).add();
        CHANNEL.messageBuilder(Apply.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Apply::encode).decoder(Apply::decode)
                .consumerMainThread((p, ctx) -> VillagerEvents.apply(ctx.get().getSender(), p)).add();
        CHANNEL.messageBuilder(Sync.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Sync::encode).decoder(Sync::decode)
                .consumerMainThread((p, ctx) -> onSync.accept(p)).add();
        CHANNEL.messageBuilder(Result.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Result::encode).decoder(Result::decode)
                .consumerMainThread((p, ctx) -> onResult.accept(p)).add();
        CHANNEL.messageBuilder(Request.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Request::encode).decoder(Request::decode)
                .consumerMainThread((p, ctx) -> VillagerEvents.openRequested(ctx.get().getSender(), p)).add();
    }

    static Appearance readApp(FriendlyByteBuf buf) {
        return new Appearance(buf.readUtf(256), buf.readUtf(256));
    }

    static void writeApp(FriendlyByteBuf buf, Appearance a) {
        buf.writeUtf(a.model(), 256);
        buf.writeUtf(a.texture(), 256);
    }

    public static void send(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static Sync snapshot(LivingEntity e) {
        return new Sync(e.level().dimension().location(), e.getUUID(), Appearance.read(e));
    }

    public static void broadcast(LivingEntity e) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> e), snapshot(e));
    }

    public record Open(int entityId, UUID entityUuid, UUID token, Appearance appearance, List<String> allowed) {
        void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeUUID(entityUuid);
            buf.writeUUID(token);
            writeApp(buf, appearance);
            buf.writeVarInt(allowed.size());
            for (String s : allowed) buf.writeUtf(s, 256);
        }

        static Open decode(FriendlyByteBuf buf) {
            int id = buf.readVarInt();
            UUID uuid = buf.readUUID();
            UUID token = buf.readUUID();
            Appearance app = readApp(buf);
            int n = buf.readVarInt();
            if (n < 0 || n > 4096) throw new IllegalArgumentException("bad size");
            List<String> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(buf.readUtf(256));
            return new Open(id, uuid, token, app, List.copyOf(list));
        }
    }

    public record Apply(UUID token, Appearance appearance) {
        void encode(FriendlyByteBuf buf) {
            buf.writeUUID(token);
            writeApp(buf, appearance);
        }

        static Apply decode(FriendlyByteBuf buf) {
            return new Apply(buf.readUUID(), readApp(buf));
        }
    }

    public record Sync(ResourceLocation dimension, UUID entityUuid, Appearance appearance) {
        void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(dimension);
            buf.writeUUID(entityUuid);
            writeApp(buf, appearance);
        }

        static Sync decode(FriendlyByteBuf buf) {
            return new Sync(buf.readResourceLocation(), buf.readUUID(), readApp(buf));
        }
    }

    public record Result(UUID token, boolean success, String message) {
        void encode(FriendlyByteBuf buf) {
            buf.writeUUID(token);
            buf.writeBoolean(success);
            buf.writeUtf(message, 256);
        }

        static Result decode(FriendlyByteBuf buf) {
            return new Result(buf.readUUID(), buf.readBoolean(), buf.readUtf(256));
        }
    }

    public record Request(int entityId) {
        void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
        }

        static Request decode(FriendlyByteBuf buf) {
            return new Request(buf.readVarInt());
        }
    }
}