package com.sappersquad.packwork.sort;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * One match rule a tab uses to claim items. Deliberately a flat
 * {@code (type, value)} record rather than a sealed hierarchy so the codec stays
 * a two-field record - the whole rule set of a pack is small and edited by hand,
 * so expressiveness beats cleverness here.
 *
 * <p>{@code value} is read differently per type:
 * <ul>
 *   <li>{@link Type#TAG} - an item tag id, e.g. {@code c:ingots}</li>
 *   <li>{@link Type#MODID} - a namespace, e.g. {@code minecraft}</li>
 *   <li>{@link Type#NAME} - a case-insensitive substring of the item id path</li>
 *   <li>{@link Type#PREDICATE} - a {@link PredicateKind} name, e.g. {@code IS_FOOD}</li>
 * </ul>
 */
public record SortRule(Type type, String value) {

    public enum Type implements StringRepresentable {
        TAG, MODID, NAME, PREDICATE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<SortRule> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            StringRepresentable.fromEnum(Type::values).fieldOf("type").forGetter(SortRule::type),
            Codec.STRING.fieldOf("value").forGetter(SortRule::value)
    ).apply(inst, SortRule::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SortRule> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Type.values()[i], Enum::ordinal), SortRule::type,
            ByteBufCodecs.STRING_UTF8, SortRule::value,
            SortRule::new);

    public static SortRule tag(String tagId) {
        return new SortRule(Type.TAG, tagId);
    }

    public static SortRule tag(TagKey<net.minecraft.world.item.Item> tag) {
        return new SortRule(Type.TAG, tag.location().toString());
    }

    public static SortRule mod(String modid) {
        return new SortRule(Type.MODID, modid);
    }

    public static SortRule name(String substring) {
        return new SortRule(Type.NAME, substring);
    }

    public static SortRule predicate(PredicateKind kind) {
        return new SortRule(Type.PREDICATE, kind.name());
    }

    /** Does this rule claim the given stack? */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return switch (type) {
            case TAG -> {
                Identifier loc = Identifier.tryParse(value);
                if (loc == null) yield false;
                yield stack.is(TagKey.create(Registries.ITEM, loc));
            }
            case MODID -> {
                Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                yield id.getNamespace().equalsIgnoreCase(value);
            }
            case NAME -> {
                Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                yield id.getPath().toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
            }
            case PREDICATE -> {
                PredicateKind kind = PredicateKind.byNameOrNull(value);
                yield kind != null && kind.test(stack);
            }
        };
    }
}
