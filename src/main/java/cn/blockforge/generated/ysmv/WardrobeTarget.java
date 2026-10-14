package cn.blockforge.generated.ysmv;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.registries.ForgeRegistries;

public final class WardrobeTarget {
    private static final ResourceLocation GUARD = new ResourceLocation("guardvillagers", "guard");

    private WardrobeTarget() {}

    public static boolean is(Entity entity) {
        if (entity instanceof Villager) return true;
        if (!(entity instanceof LivingEntity)) return false;
        return GUARD.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    public static String displayName(LivingEntity entity) {
        if (entity instanceof Villager) return "村民";
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (GUARD.equals(type)) return "警卫村民";
        return type.getNamespace() + " / " + type.getPath();
    }
}