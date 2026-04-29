package com.example.mixin;

import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Enchantment.class)
public class EnchantmentMixin {

    /**
     * @author MiNISO
     * @reason Повышаем максимальный уровень всех чар до 7
     */
    @Overwrite
    public int getMaxLevel() {
        return 7; // или 6, если хотите только до 6
    }
}