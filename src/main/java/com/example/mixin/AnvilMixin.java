package com.example.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilMixin {

    @Shadow
    private Property levelCost;

    @Shadow
    protected abstract void updateResult();

    private static final int MAX_LEVEL = 6;

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
        ItemStack left = handler.getSlot(0).getStack();
        ItemStack right = handler.getSlot(1).getStack();

        if (left.isEmpty() || right.isEmpty()) return;

        // Получаем чары с обоих предметов, учитывая и ENCHANTMENTS, и STORED_ENCHANTMENTS
        Map<RegistryEntry<Enchantment>, Integer> leftMap = getEnchantments(left);
        Map<RegistryEntry<Enchantment>, Integer> rightMap = getEnchantments(right);

        if (leftMap.isEmpty() && rightMap.isEmpty()) return;

        Map<RegistryEntry<Enchantment>, Integer> newMap = new HashMap<>(leftMap);
        boolean changed = false;

        for (var entry : rightMap.entrySet()) {
            RegistryEntry<Enchantment> enchant = entry.getKey();
            int rightLvl = entry.getValue();
            int leftLvl = leftMap.getOrDefault(enchant, 0);

            int newLvl = Math.max(leftLvl, rightLvl);
            if (leftLvl == rightLvl && leftLvl < MAX_LEVEL) {
                newLvl = leftLvl + 1;
            }
            if (newLvl > leftLvl) {
                newMap.put(enchant, newLvl);
                changed = true;
            }
        }

        if (changed) {
            ItemStack resultStack = left.copy();

            // Если левый предмет – книга, кладём чары в STORED_ENCHANTMENTS, иначе в ENCHANTMENTS
            if (resultStack.isOf(Items.ENCHANTED_BOOK)) {
                resultStack.set(DataComponentTypes.STORED_ENCHANTMENTS, buildEnchantments(newMap));
            } else {
                resultStack.set(DataComponentTypes.ENCHANTMENTS, buildEnchantments(newMap));
            }

            handler.getSlot(2).setStack(resultStack);
            levelCost.set(1);
            handler.sendContentUpdates();   // <-- обязательно!
            ci.cancel();
        }
    }

    // Извлекает все чары из предмета (и ENCHANTMENTS, и STORED_ENCHANTMENTS)
    private Map<RegistryEntry<Enchantment>, Integer> getEnchantments(ItemStack stack) {
        Map<RegistryEntry<Enchantment>, Integer> map = new HashMap<>();
        addEnchantments(map, stack.get(DataComponentTypes.ENCHANTMENTS));
        addEnchantments(map, stack.get(DataComponentTypes.STORED_ENCHANTMENTS));
        return map;
    }

    private void addEnchantments(Map<RegistryEntry<Enchantment>, Integer> map, ItemEnchantmentsComponent comp) {
        if (comp == null) return;
        for (RegistryEntry<Enchantment> ench : comp.getEnchantments()) {
            map.put(ench, comp.getLevel(ench));
        }
    }

    private ItemEnchantmentsComponent buildEnchantments(Map<RegistryEntry<Enchantment>, Integer> map) {
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        for (var entry : map.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}