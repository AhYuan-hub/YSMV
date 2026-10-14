package cn.blockforge.generated.ysmv;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;

public final class YsmAccess {
    public static final String ROOT = "com.elfmcys.yesstevemodel.";
    public static final String FIRST = "Oo0Oo0o00O00Oo0OOoOOoooo";
    public static final String SECOND = "o0OOooo0o0OO00OoOOOo0o0O";
    public static final String THIRD = "O00OOOooOoooOoo0o0o0oO0O";

    private static final Map<String, Method> CACHE = new ConcurrentHashMap<>();
    private static boolean reported;

    private YsmAccess() {}

    public static Class<?> type(String name) throws ReflectiveOperationException {
        return Class.forName(ROOT + name);
    }

    public static Method method(Class<?> type, String name, Class<?>... args) throws ReflectiveOperationException {
        String key = type.getName() + '#' + name + Arrays.toString(args);
        Method m = CACHE.get(key);
        if (m == null) {
            m = type.getMethod(name, args);
            CACHE.put(key, m);
        }
        return m;
    }

    public static Object call(Object target, String name) throws ReflectiveOperationException {
        return method(target.getClass(), name).invoke(target);
    }

    public static Object staticCall(String owner, String name) throws ReflectiveOperationException {
        return method(type(owner), name).invoke(null);
    }

    public static Object capability(Player player, String provider) throws ReflectiveOperationException {
        Capability<?> cap = (Capability<?>) type(provider).getField(FIRST).get(null);
        return player.getCapability(cap).resolve().orElse(null);
    }

    public static boolean available() throws ReflectiveOperationException {
        return Boolean.TRUE.equals(staticCall("YesSteveModel", "isAvailable"));
    }

    public static boolean allowed(Player player, String model) throws ReflectiveOperationException {
        Map<?, ?> models = (Map<?, ?>) staticCall("OoOoOoooO0O00oOoO00OOo00", FIRST);
        if (!models.containsKey(model)) return false;
        Set<?> restricted = (Set<?>) staticCall("OoOoOoooO0O00oOoO00OOo00", SECOND);
        if (!restricted.contains(model)) return true;
        Object auth = capability(player, "OOOOo0O0oO0OOo0O0O0Oo0O0");
        return auth != null && Boolean.TRUE.equals(method(auth.getClass(), THIRD, String.class).invoke(auth, model));
    }

    public static void report(Throwable e) {
        if (!reported) {
            reported = true;
            GeneratedMod.LOGGER.error("YSM 接口炸了，村民保持原版", e);
        }
    }
}