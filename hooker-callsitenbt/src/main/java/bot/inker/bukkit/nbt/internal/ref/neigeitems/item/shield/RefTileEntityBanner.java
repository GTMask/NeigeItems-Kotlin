package pers.neige.neigeitems.internal.ref.item.shield;

import org.inksnow.ankhinvoke.comments.HandleBy;
import bot.inker.bukkit.nbt.internal.ref.RefNmsItemStack;
import pers.neige.neigeitems.internal.ref.item.RefEnumColor;

@HandleBy(reference = "net/minecraft/server/v1_12_R1/TileEntityBanner", predicates = "craftbukkit_version:[v1_12_R1,v1_13_R1)")
public final class RefTileEntityBanner {
    @HandleBy(reference = "Lnet/minecraft/server/v1_12_R1/TileEntityBanner;d(Lnet/minecraft/server/v1_12_R1/ItemStack;)Lnet/minecraft/server/v1_12_R1/EnumColor;", predicates = "craftbukkit_version:[v1_12_R1,v1_13_R1)")
    public static native RefEnumColor getColor(RefNmsItemStack itemStack);
}
