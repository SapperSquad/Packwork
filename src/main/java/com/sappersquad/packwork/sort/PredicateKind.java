package com.sappersquad.packwork.sort;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/**
 * Component/class predicates that recognise an item by what it <em>is</em> rather
 * than by a tag it happens to carry. These catch modded items for free - any mod's
 * pickaxe is a tool, any mod's food is food - without anyone maintaining a tag list.
 */
public enum PredicateKind {
    IS_FOOD {
        @Override
        public boolean test(ItemStack stack) {
            return stack.has(DataComponents.FOOD);
        }
    },
    IS_TOOL {
        @Override
        public boolean test(ItemStack stack) {
            return stack.getItem() instanceof DiggerItem;
        }
    },
    IS_WEAPON {
        @Override
        public boolean test(ItemStack stack) {
            return stack.getItem() instanceof SwordItem
                    || stack.getItem() instanceof TridentItem
                    || stack.getItem() instanceof BowItem
                    || stack.getItem() instanceof CrossbowItem
                    || stack.getItem() instanceof ProjectileWeaponItem;
        }
    },
    IS_ARMOR {
        @Override
        public boolean test(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem;
        }
    },
    IS_BLOCK {
        @Override
        public boolean test(ItemStack stack) {
            return stack.getItem() instanceof BlockItem;
        }
    },
    IS_POTION {
        @Override
        public boolean test(ItemStack stack) {
            return stack.getItem() instanceof PotionItem || stack.has(DataComponents.POTION_CONTENTS);
        }
    };

    public abstract boolean test(ItemStack stack);

    public static PredicateKind byNameOrNull(String name) {
        for (PredicateKind k : values()) {
            if (k.name().equalsIgnoreCase(name)) return k;
        }
        return null;
    }
}
