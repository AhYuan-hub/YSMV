package cn.blockforge.generated.ysmv;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

public record Appearance(String model, String texture) {
    public static final String KEY = GeneratedMod.MOD_ID + ":appearance";
    public static final Appearance DEFAULT = new Appearance("", "");

    public boolean isDefault() {
        return model.isEmpty();
    }

    public static Appearance read(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData().getCompound(KEY);
        return new Appearance(tag.getString("Model"), tag.getString("Texture"));
    }

    public void save(LivingEntity entity) {
        if (isDefault()) {
            entity.getPersistentData().remove(KEY);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("Model", model);
        tag.putString("Texture", texture);
        entity.getPersistentData().put(KEY, tag);
    }
}