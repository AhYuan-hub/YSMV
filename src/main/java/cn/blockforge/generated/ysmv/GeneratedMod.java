package cn.blockforge.generated.ysmv;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "ysmv";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GeneratedMod() {
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(new VillagerEvents());
    }
}