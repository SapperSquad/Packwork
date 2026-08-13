package com.sappersquad.packwork.sort;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Component/class predicates that recognise an item by what it <em>is</em> rather
 * than by a tag it happens to carry. These catch modded items for free - any mod's
 * pickaxe is a tool, any mod's food is food - without anyone maintaining a tag list.
 *
 * <p><b>1.21.11 port note:</b> the item classes these leaned on (SwordItem, DiggerItem,
 * ArmorItem) were dissolved into data components in 1.21.5, so the tests now read
 * components - tuned to keep the 1.21.1 routing: axes carry WEAPON now but stay
 * TOOLS (SapperSquad's tab order routed them there), swords carry TOOL now but stay COMBAT
 * (the #minecraft:swords tag is the tiebreak), and armor means "worn in an armor slot
 * AND carries attribute modifiers" so a carved pumpkin or elytra doesn't file as plate.
 */
public enum PredicateKind {
    IS_FOOD {
        @Override
        public boolean test(ItemStack stack) {
            return stack.has(DataComponents.FOOD) || stack.has(DataComponents.CONSUMABLE);
        }
    },
    IS_TOOL {
        @Override
        public boolean test(ItemStack stack) {
            // Mining tools: TOOL component, minus swords (which carry TOOL for cobwebs).
            return stack.has(DataComponents.TOOL) && !stack.is(ItemTags.SWORDS);
        }
    },
    IS_WEAPON {
        @Override
        public boolean test(ItemStack stack) {
            if (stack.is(ItemTags.SWORDS)) return true;                       // swords, incl. modded tagged ones
            if (stack.getItem() instanceof ProjectileWeaponItem) return true; // bows, crossbows
            // Maces, tridents, modded melee: WEAPON without a mining TOOL side
            // (an axe has both and belongs to the Tools tab, as it always did).
            return stack.has(DataComponents.WEAPON) && !stack.has(DataComponents.TOOL);
        }
    },
    IS_ARMOR {
        @Override
        public boolean test(ItemStack stack) {
            Equippable eq = stack.get(DataComponents.EQUIPPABLE);
            if (eq == null || eq.slot().getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return false;
            // Real armor protects: it carries attribute modifiers (armor/toughness).
            // A carved pumpkin or an elytra is equippable but carries none.
            var mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            return mods != null && !mods.modifiers().isEmpty();
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
