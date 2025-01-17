package pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.neigeitems.utils;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.neige.neigeitems.EnumHand;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.internal.annotation.CbVersion;
import pers.neige.neigeitems.libs.bot.inker.bukkit.nbt.neigeitems.animation.AnimationType;
import pers.neige.neigeitems.ref.argument.RefAnchor;
import pers.neige.neigeitems.ref.block.RefBlockPos;
import pers.neige.neigeitems.ref.block.sign.RefSignBlockEntity;
import pers.neige.neigeitems.ref.chat.RefEnumTitleAction;
import pers.neige.neigeitems.ref.entity.*;
import pers.neige.neigeitems.ref.nbt.RefNmsItemStack;
import pers.neige.neigeitems.ref.network.*;
import pers.neige.neigeitems.ref.network.syncher.RefEntityDataAccessor;
import pers.neige.neigeitems.ref.network.syncher.RefSynchedEntityData;
import pers.neige.neigeitems.ref.network.syncher.RefSynchedEntityData$DataItem;
import pers.neige.neigeitems.ref.network.syncher.RefSynchedEntityData$DataValue;
import pers.neige.neigeitems.ref.server.level.RefServerEntity;
import pers.neige.neigeitems.ref.server.level.RefWorldServer;
import pers.neige.neigeitems.ref.world.RefCraftWorld;
import pers.neige.neigeitems.ref.world.RefVec3;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EntityPlayerUtils {
    /**
     * 1.15+ 版本起, EntityLiving 内部 swing 方法可以传入一个布尔量.
     * 该布尔量为 true 时, 向周围生物及该实体本身发送摆动对应手臂的动作数据包.
     * 该布尔量为 false 时, 仅向周围生物发送数据包, 不向该实体本身发送.
     * 以往版本中默认不向该实体本身发送.
     */
    private static final boolean SWING_AND_SEND_SUPPORT = CbVersion.v1_15_R1.isSupport();

    /**
     * 1.16.2+ 版本起, Damageable 接口下添加 getAbsorptionAmount 及 setAbsorptionAmount 方法, 用于操作伤害吸收的黄心数值.
     * 高版本的 getAbsorptionAmount 返回的是 double, 而 NMS 内部返回的实质上是 float, 是 OBC 将 float 强转为 double 并返回.
     * 总觉得有一种小脑发育不完全的美, 所以本工具类的 getAbsorptionAmount 干脆全盘使用 NMS 操作, 不在 1.16.2+ 版本调用 BukkitAPI 了.
     */
    private static final boolean ABSORPTION_SUPPORT = CbVersion.v1_16_R2.isSupport();

    /**
     * 1.15.2+ 版本起, HumanEntity 接口下添加 getAttackCooldown 方法, 用于获取玩家当前攻击冷却.
     * 但是 1.15 - 1.15.2 版本的 CraftBukkit 版本均为 v1_15_R1.
     * 为防止出现问题, 直接看作 v1_16_R1 起步.
     */
    private static final boolean ATTACK_COOLDOWN_SUPPORT = CbVersion.v1_16_R1.isSupport();

    /**
     * 1.14+ 版本起, Entity 接口添加 setRotation 方法, 用于设置实体朝向.
     */
    private static final boolean SET_ROTATION_SUPPORT = CbVersion.v1_14_R1.isSupport();

    /**
     * 1.17+ 版本起, Entity 内部的添加 lookAt 方法, 用于使玩家看向指定位置.
     */
    private static final boolean LOOK_AT_SUPPORT = CbVersion.v1_17_R1.isSupport();

    /**
     * 1.13+ 版本起, 添加 PacketPlayOutLookAt 数据包, 用于使玩家看向指定位置.
     */
    private static final boolean PACKET_LOOK_AT_SUPPORT = CbVersion.v1_13_R1.isSupport();

    /**
     * 1.17+ 版本起, SignBlockEntity 构造函数发生变化.
     */
    private static final boolean NEW_SIGN_ENTITY_CONSTRUCTOR = CbVersion.v1_17_R1.isSupport();

    /**
     * 1.20+ 版本起, 告示牌开始出现正反面区别.
     */
    private static final boolean SIGN_SIDE_SUPPORT = CbVersion.v1_20_R1.isSupport();

    /**
     * 1.17+ 版本起, Title 相关数据包发生变化.
     */
    private static final boolean TITLE_PACKET_CHANGED = CbVersion.v1_17_R1.isSupport();
    /**
     * 1.14+ 版本起, Entity 类添加 addEntityPacket 方法, 返回对应的添加实体数据包.
     */
    private static final boolean ADD_ENTITY_PACKET_SUPPORT = CbVersion.v1_14_R1.isSupport();
    /**
     * 1.17+ 版本起, spawnEntity 方法添加 boolean 类型 randomizeData 参数, 用于随机生成实体数据.
     */
    private static final boolean RANDOMIZE_DATA_SUPPORT = CbVersion.v1_17_R1.isSupport();
    /**
     * 1.19.4+ 版本起, spawnEntity 方法添加 boolean 类型 randomizeData 参数, 用于随机生成实体数据.
     */
    private static final boolean METADATA_NEED_VALUE_LIST = CbVersion.v1_19_R3.isSupport();
    /**
     * 1.13+ 版本起, Entity 类 setCustomName 方法由接收 String 改为接收 IChatBaseComponent.
     */
    private static final boolean COMPONENT_NAME_SUPPORT = CbVersion.v1_13_R1.isSupport();
    /**
     * 1.19.4+ 版本起, SynchedEntityData 下 set 方法添加 boolean 类型 force 参数, 用于强制设置.
     */
    private static final boolean FORCE_DATA_SET = CbVersion.v1_19_R3.isSupport();
    /**
     * 1.14+ 版本起, WorldServer 类移除 tracker 字段.
     */
    private static final boolean REMOVED_TRACKER = CbVersion.v1_14_R1.isSupport();

    /**
     * 让指定玩家攻击指定实体.
     *
     * @param player 攻击者.
     * @param entity 防御者.
     */
    public static void attack(
            @NotNull Player player,
            @NotNull Entity entity
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            RefEntityPlayer attacker = ((RefCraftPlayer) (Object) player).getHandle();
            if (entity instanceof RefCraftEntity) {
                RefEntity defender = ((RefCraftEntity) entity).getHandle();
                attacker.attack(defender);
            }
        }
    }

    /**
     * 获取指定实体的攻击冷却.
     *
     * @param entity 待获取实体.
     */
    public static float getAttackCooldown(
            @NotNull HumanEntity entity
    ) {
        if (ATTACK_COOLDOWN_SUPPORT) {
            return entity.getAttackCooldown();
        } else {
            return getAttackCooldown((LivingEntity) entity);
        }
    }

    /**
     * 获取指定实体的攻击冷却.
     *
     * @param entity 待获取实体.
     */
    public static float getAttackCooldown(
            @NotNull LivingEntity entity
    ) {
        if (entity instanceof RefCraftLivingEntity) {
            RefEntityLiving livingEntity = ((RefCraftLivingEntity) entity).getHandle();
            int attackStrengthTicker = livingEntity.attackStrengthTicker;
            AttributeInstance attackSpeedAttribute = entity.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            double attackSpeed;
            if (attackSpeedAttribute != null) {
                attackSpeed = attackSpeedAttribute.getValue();
            } else {
                attackSpeed = 4;
            }
            float value = (float) ((attackStrengthTicker + 0.5) / (1.0 / attackSpeed * 20.0));
            return value < 0 ? 0 : Math.min(value, 1);
        }
        return 0;
    }

    /**
     * 设置指定实体的攻击冷却.
     *
     * @param entity         待获取实体.
     * @param attackCooldown 攻击冷却.
     */
    public static void setAttackCooldown(
            @NotNull LivingEntity entity,
            float attackCooldown
    ) {
        if (entity instanceof RefCraftLivingEntity) {
            RefEntityLiving livingEntity = ((RefCraftLivingEntity) entity).getHandle();
            AttributeInstance attackSpeedAttribute = entity.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            double attackSpeed;
            if (attackSpeedAttribute != null) {
                attackSpeed = attackSpeedAttribute.getValue();
            } else {
                attackSpeed = 4;
            }
            livingEntity.attackStrengthTicker = Math.max(0, (int) (attackCooldown * (1.0 / attackSpeed * 20.0) - 0.5));
        }
    }

    /**
     * 获取指定实体距上次攻击行为有多久(tick).
     *
     * @param entity 待获取实体.
     */
    public static int getAttackStrengthTicker(
            @NotNull LivingEntity entity
    ) {
        if (entity instanceof RefCraftLivingEntity) {
            RefEntityLiving livingEntity = ((RefCraftLivingEntity) entity).getHandle();
            return livingEntity.attackStrengthTicker;
        }
        return 0;
    }

    /**
     * 设置指定实体距上次攻击行为有多久(tick).
     *
     * @param entity               待获取实体.
     * @param attackStrengthTicker 实体距上次攻击行为的时间(tick).
     */
    public static void setAttackStrengthTicker(
            @NotNull LivingEntity entity,
            int attackStrengthTicker
    ) {
        if (entity instanceof RefCraftLivingEntity) {
            RefEntityLiving livingEntity = ((RefCraftLivingEntity) entity).getHandle();
            livingEntity.attackStrengthTicker = attackStrengthTicker;
        }
    }

    /**
     * 让指定玩家使用对应手持物品.
     *
     * @param player 待操作玩家.
     * @param hand   待使用物品槽位.
     */
    public static void useItem(
            @NotNull Player player,
            @NotNull EnumHand hand
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            RefEntityPlayer entityPlayer = ((RefCraftPlayer) (Object) player).getHandle();
            RefEnumHand enumHand = toRefEnumHand(hand);
            RefWorldServer world = ((RefCraftWorld) (Object) player.getWorld()).getHandle();
            RefNmsItemStack itemStack = entityPlayer.getItemInHand(enumHand);
            entityPlayer.playerInteractManager.useItem(entityPlayer, world, itemStack, enumHand);
        }
    }

    /**
     * 摆动指定玩家的指定手臂.
     *
     * @param player 待操作玩家.
     * @param hand   待摆动手臂.
     */
    public static void swing(
            @NotNull Player player,
            @NotNull EnumHand hand
    ) {
        swing(player, hand, true);
    }

    /**
     * 摆动指定玩家的指定手臂.
     *
     * @param player           待操作玩家.
     * @param hand             待摆动手臂.
     * @param fromServerPlayer 本人是否可见摆动动作.
     */
    public static void swing(
            @NotNull Player player,
            @NotNull EnumHand hand,
            boolean fromServerPlayer
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            RefEntityPlayer entityPlayer = ((RefCraftPlayer) (Object) player).getHandle();
            RefEnumHand enumHand = toRefEnumHand(hand);
            swingByNms(entityPlayer, enumHand, fromServerPlayer);
        }
    }

    /**
     * 给玩家播放指定动画(仅玩家本人可见).
     *
     * @param player 待操作玩家.
     * @param type   动画类型.
     */
    public static void sendAnimationPacket(
            @NotNull Player player,
            @NotNull AnimationType type
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            RefEntityPlayer entityPlayer = ((RefCraftPlayer) (Object) player).getHandle();
            entityPlayer.playerConnection.sendPacket(new RefPacketPlayOutAnimation(entityPlayer, type.getValue()));
        }
    }

    /**
     * 让指定玩家破坏对应位置的方块.
     *
     * @param player 待操作玩家.
     * @param block  待破坏方块.
     * @return 是否破坏成功
     */
    public static boolean breakBlock(
            @NotNull Player player,
            Block block
    ) {
        Preconditions.checkArgument(block != null, "Block cannot be null");
        Preconditions.checkArgument(block.getWorld().equals(player.getWorld()), "Cannot break blocks across worlds");
        return breakBlock(player, block.getX(), block.getY(), block.getZ());
    }

    /**
     * 让指定玩家破坏对应位置的方块.
     *
     * @param player 待操作玩家.
     * @param x      目标方块 x 坐标.
     * @param y      目标方块 y 坐标.
     * @param z      目标方块 z 坐标.
     * @return 是否破坏成功
     */
    public static boolean breakBlock(
            @NotNull Player player,
            int x,
            int y,
            int z
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            RefEntityPlayer entityPlayer = ((RefCraftPlayer) (Object) player).getHandle();
            return entityPlayer.playerInteractManager.breakBlock(new RefBlockPos(x, y, z));
        }
        return false;
    }

    /**
     * 获取玩家的伤害吸收数值.
     *
     * @param player 待获取玩家.
     */
    public static float getAbsorptionAmount(
            @NotNull Player player
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            return ((RefCraftPlayer) (Object) player).getHandle().getAbsorptionAmount();
        }
        return 0;
    }

    /**
     * 设置玩家的伤害吸收数值.
     *
     * @param player 待操作玩家.
     * @param amount 伤害吸收数值.
     */
    public static void setAbsorptionAmount(
            @NotNull Player player,
            float amount
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            ((RefCraftPlayer) (Object) player).getHandle().setAbsorptionAmount(amount);
        }
    }

    /**
     * 强制实体从地上跳起(浮空时不生效).
     *
     * @param humanEntity 待操作实体.
     */
    public static void jumpFromGround(
            @NotNull HumanEntity humanEntity
    ) {
        if (humanEntity.isOnGround() && humanEntity instanceof RefCraftHumanEntity) {
            ((RefCraftHumanEntity) humanEntity).getHandle().jumpFromGround();
        }
    }

    /**
     * 强制实体从地上跳起(浮空时不生效).
     *
     * @param entity 待操作实体.
     */
    public static void jumpFromGround(
            @NotNull LivingEntity entity
    ) {
        if (entity.isOnGround() && entity instanceof RefCraftLivingEntity) {
            ((RefCraftLivingEntity) entity).getHandle().jumpFromGround();
        }
    }

    /**
     * 设置玩家朝向(仅适用于1.13+).
     *
     * @param player 待设置玩家.
     * @param yaw    实体偏航角.
     * @param pitch  实体俯仰角.
     */
    public static void setRotation(
            @NotNull Player player,
            float yaw,
            float pitch
    ) {
        if (SET_ROTATION_SUPPORT) {
            player.setRotation(yaw, pitch);
        } else {
            if ((Object) player instanceof RefCraftPlayer) {
                Location targetLocation = player.getEyeLocation();
                targetLocation.setYaw(yaw);
                targetLocation.setPitch(pitch);
                Vector direction = targetLocation.getDirection();
                direction.multiply(9999999);
                targetLocation.add(direction);
                lookAtByNms(player, targetLocation.getX(), targetLocation.getY(), targetLocation.getZ());
            }
        }
    }

    /**
     * 令玩家看向指定坐标(仅适用于1.13+).
     *
     * @param player 待操作玩家.
     * @param target 目标坐标.
     */
    public static void lookAt(
            @NotNull Player player,
            @NotNull Location target
    ) {
        lookAt(player, target.getX(), target.getY(), target.getZ());
    }

    /**
     * 令玩家看向指定坐标(仅适用于1.13+).
     *
     * @param player 待操作玩家.
     * @param x      目标 x 坐标.
     * @param y      目标 y 坐标.
     * @param z      目标 z 坐标.
     */
    public static void lookAt(
            @NotNull Player player,
            double x,
            double y,
            double z
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            if (LOOK_AT_SUPPORT) {
                ((RefCraftPlayer) (Object) player).getHandle().lookAt(RefAnchor.EYES, new RefVec3(x, y, z));
            } else {
                lookAtByNms(player, x, y, z);
            }
        }
    }

    /**
     * 获取玩家对应的 Channel.
     *
     * @param player 待操作玩家.
     * @return 玩家对应的 Channel.
     */
    @Nullable
    public static Channel getChannel(
            @NotNull Player player
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            return ((RefCraftPlayer) (Object) player).getHandle().playerConnection.networkManager.channel;
        }
        return null;
    }

    /**
     * 为玩家打开一个虚拟告示牌.
     *
     * @param player 待操作玩家.
     */
    public static void openSign(
            @NotNull Player player
    ) {
        if ((Object) player instanceof RefCraftPlayer) {
            RefEntityPlayer nmsPlayer = ((RefCraftPlayer) (Object) player).getHandle();
            RefSignBlockEntity sign;
            if (NEW_SIGN_ENTITY_CONSTRUCTOR) {
                Location location = player.getLocation();
                sign = new RefSignBlockEntity(new RefBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), null);
            } else {
                sign = new RefSignBlockEntity();
            }
            if (SIGN_SIDE_SUPPORT) {
                nmsPlayer.openSign(sign, true);
            } else {
                nmsPlayer.openSign(sign);
            }
        }
    }

    /**
     * 向玩家发送 BaseComponent 形式的 Title.
     *
     * @param player   待接收玩家.
     * @param title    Title.
     * @param subtitle Subtitle.
     */
    public static void sendTitle(
            @NotNull Player player,
            @Nullable BaseComponent title,
            @Nullable BaseComponent subtitle
    ) {
        sendTitle(player, title, subtitle, 10, 70, 20);
    }

    /**
     * 向玩家发送 BaseComponent 形式的 Title.
     *
     * @param player   待接收玩家.
     * @param title    Title.
     * @param subtitle Subtitle.
     * @param fadeIn   渐入时间(tick).
     * @param stay     停留时间(tick).
     * @param fadeOut  渐出时间(tick).
     */
    public static void sendTitle(
            @NotNull Player player,
            @Nullable BaseComponent title,
            @Nullable BaseComponent subtitle,
            int fadeIn,
            int stay,
            int fadeOut
    ) {
        RefEntityPlayer nmsPlayer = ((RefCraftPlayer) (Object) player).getHandle();
        if (TITLE_PACKET_CHANGED) {
            RefClientboundSetTitlesAnimationPacket times = new RefClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);
            nmsPlayer.playerConnection.sendPacket(times);
            if (title != null) {
                RefClientboundSetTitleTextPacket packetTitle = new RefClientboundSetTitleTextPacket(EntityUtils.toNms(title));
                nmsPlayer.playerConnection.sendPacket(packetTitle);
            }
            if (subtitle != null) {
                RefClientboundSetSubtitleTextPacket packetSubtitle = new RefClientboundSetSubtitleTextPacket(EntityUtils.toNms(subtitle));
                nmsPlayer.playerConnection.sendPacket(packetSubtitle);
            }
        } else {
            RefPacketPlayOutTitle times = new RefPacketPlayOutTitle(fadeIn, stay, fadeOut);
            nmsPlayer.playerConnection.sendPacket(times);
            RefPacketPlayOutTitle packetSubtitle;
            if (title != null) {
                packetSubtitle = new RefPacketPlayOutTitle(RefEnumTitleAction.TITLE, EntityUtils.toNms(title));
                nmsPlayer.playerConnection.sendPacket(packetSubtitle);
            }
            if (subtitle != null) {
                packetSubtitle = new RefPacketPlayOutTitle(RefEnumTitleAction.SUBTITLE, EntityUtils.toNms(subtitle));
                nmsPlayer.playerConnection.sendPacket(packetSubtitle);
            }
        }
    }

    /**
     * 向玩家发送一个虚拟的实体数据包.
     *
     * @param location 待生成坐标.
     * @param type     实体类型.
     * @param player   待接收玩家.
     */
    public static void sendFakeEntity(
            @NotNull Location location,
            @NotNull EntityType type,
            @NotNull Player player
    ) {
        sendFakeEntity(location, type, true, null, player);
    }

    /**
     * 向玩家发送一个虚拟的实体数据包.
     *
     * @param location      待生成坐标.
     * @param type          实体类型.
     * @param randomizeData 是否随机实体数据(仅在1.17+版本生效).
     * @param player        待接收玩家.
     */
    public static void sendFakeEntity(
            @NotNull Location location,
            @NotNull EntityType type,
            boolean randomizeData,
            @NotNull Player player
    ) {
        sendFakeEntity(location, type, randomizeData, null, player);
    }

    /**
     * 向玩家发送一个虚拟的实体数据包.
     *
     * @param location      待生成坐标.
     * @param type          实体类型.
     * @param randomizeData 是否随机实体数据(仅在1.17+版本生效).
     * @param function      对实体执行的操作.
     * @param player        待接收玩家.
     * @return 生成的实体.
     */
    @Nullable
    public static Entity sendFakeEntity(
            @NotNull Location location,
            @NotNull EntityType type,
            boolean randomizeData,
            @Nullable Consumer<Entity> function,
            @NotNull Player player
    ) {
        World world = location.getWorld();
        if (world == null) return null;
        RefCraftWorld craftWorld = (RefCraftWorld) (Object) world;

        RefEntity entity;
        if (RANDOMIZE_DATA_SUPPORT) {
            entity = craftWorld.createEntity(location, type.getEntityClass(), randomizeData);
        } else {
            entity = craftWorld.createEntity(location, type.getEntityClass());
        }
        Entity bukkitEntity = entity.getBukkitEntity();

        if ((Object) player instanceof RefCraftPlayer) {
            RefEntityPlayer nmsPlayer = ((RefCraftPlayer) (Object) player).getHandle();

            if (ADD_ENTITY_PACKET_SUPPORT) {
                nmsPlayer.playerConnection.sendPacket(entity.getAddEntityPacket());
            } else {
                RefServerEntity serverEntity = new RefServerEntity(entity, 0, 0, 0, false);
                nmsPlayer.playerConnection.sendPacket(serverEntity.createPacket());
            }
            if (function != null) {
                function.accept(bukkitEntity);
                refreshFakeEntity(bukkitEntity, player);
            }
        }
        return bukkitEntity;
    }

    /**
     * 刷新虚拟实体数据.
     *
     * @param entity 待刷新实体.
     * @param player 待接收玩家.
     */
    public static void refreshFakeEntity(
            @NotNull Entity entity,
            @NotNull Player player
    ) {
        if (entity instanceof RefCraftEntity && (Object) player instanceof RefCraftPlayer) {
            RefEntity nmsEntity = ((RefCraftEntity) entity).getHandle();
            RefEntityPlayer nmsPlayer = ((RefCraftPlayer) (Object) player).getHandle();

            RefPacketPlayOutEntityMetadata packet;
            if (METADATA_NEED_VALUE_LIST) {
                RefSynchedEntityData entityData = nmsEntity.getEntityData();
                List<RefSynchedEntityData$DataValue> dataList = entityData.packDirty();
                if (dataList == null) {
                    dataList = entityData.getNonDefaultValues();
                }
                packet = new RefPacketPlayOutEntityMetadata(nmsEntity.getId(), dataList);
            } else {
                packet = new RefPacketPlayOutEntityMetadata(nmsEntity.getId(), nmsEntity.getEntityData(), true);
            }
            nmsPlayer.playerConnection.sendPacket(packet);
        }
    }

    /**
     * 移除实体虚拟显示名.
     *
     * @param entity 待设置实体.
     * @param player 待接收玩家.
     */
    public static void removeFakeCustomName(
            @NotNull Entity entity,
            @NotNull Player player
    ) {
        if (entity instanceof RefCraftEntity && (Object) player instanceof RefCraftPlayer) {
            RefEntity nmsEntity = ((RefCraftEntity) entity).getHandle();
            RefEntityPlayer nmsPlayer = ((RefCraftPlayer) (Object) player).getHandle();

            RefPacketPlayOutEntityMetadata packet;
            RefSynchedEntityData entityData = new RefSynchedEntityData(nmsEntity);
            defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_VISIBLE, entity.isCustomNameVisible());
            if (COMPONENT_NAME_SUPPORT) {
                defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_COMPONENT, Optional.of(nmsEntity.getCustomName()));
            } else {
                defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_STRING, entity.getCustomName());
            }
            if (METADATA_NEED_VALUE_LIST) {
                List<RefSynchedEntityData$DataValue> dataList = entityData.packDirty();
                if (dataList == null) {
                    dataList = entityData.getNonDefaultValues();
                }
                packet = new RefPacketPlayOutEntityMetadata(nmsEntity.getId(), dataList);
            } else {
                packet = new RefPacketPlayOutEntityMetadata(nmsEntity.getId(), entityData, true);
            }

            nmsPlayer.playerConnection.sendPacket(packet);
        }
    }

    /**
     * 为实体设置虚拟显示名.
     *
     * @param entity 待设置实体.
     * @param player 待接收玩家.
     * @param name   显示名.
     */
    public static void setFakeCustomName(
            @NotNull Entity entity,
            @NotNull Player player,
            @NotNull String name
    ) {
        if (entity instanceof RefCraftEntity && (Object) player instanceof RefCraftPlayer) {
            RefEntity nmsEntity = ((RefCraftEntity) entity).getHandle();
            RefEntityPlayer nmsPlayer = ((RefCraftPlayer) (Object) player).getHandle();

            RefPacketPlayOutEntityMetadata packet;
            RefSynchedEntityData entityData = new RefSynchedEntityData(nmsEntity);
            defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_VISIBLE, true);
            if (COMPONENT_NAME_SUPPORT) {
                defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_COMPONENT, Optional.of(EntityUtils.toNms(new TextComponent(name))));
            } else {
                defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_STRING, name);
            }
            if (METADATA_NEED_VALUE_LIST) {
                List<RefSynchedEntityData$DataValue> dataList = entityData.packDirty();
                if (dataList == null) {
                    dataList = entityData.getNonDefaultValues();
                }
                packet = new RefPacketPlayOutEntityMetadata(nmsEntity.getId(), dataList);
            } else {
                packet = new RefPacketPlayOutEntityMetadata(nmsEntity.getId(), entityData, true);
            }

            nmsPlayer.playerConnection.sendPacket(packet);
        }
    }

    /**
     * 为实体设置虚拟显示名.
     *
     * @param entity 待设置实体.
     * @param player 待接收玩家.
     * @param name   显示名.
     */
    public static void setFakeCustomName(
            @NotNull Entity entity,
            @NotNull Player player,
            @NotNull BaseComponent name
    ) {
        if (entity instanceof RefCraftEntity && (Object) player instanceof RefCraftPlayer) {
            RefEntity nmsEntity = ((RefCraftEntity) entity).getHandle();
            RefEntityPlayer nmsPlayer = ((RefCraftPlayer) (Object) player).getHandle();

            RefPacketPlayOutEntityMetadata packet;
            RefSynchedEntityData entityData = new RefSynchedEntityData(nmsEntity);
            defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_VISIBLE, true);
            if (COMPONENT_NAME_SUPPORT) {
                defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_COMPONENT, Optional.of(EntityUtils.toNms(name)));
            } else {
                defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_STRING, name.toLegacyText());
            }
            if (METADATA_NEED_VALUE_LIST) {
                List<RefSynchedEntityData$DataValue> dataList = entityData.packDirty();
                if (dataList == null) {
                    dataList = entityData.getNonDefaultValues();
                }
                packet = new RefPacketPlayOutEntityMetadata(nmsEntity.getId(), dataList);
            } else {
                packet = new RefPacketPlayOutEntityMetadata(nmsEntity.getId(), entityData, true);
            }

            nmsPlayer.playerConnection.sendPacket(packet);
        }
    }

    public static void setFakeCustomName(
            @NotNull World world,
            @NotNull Object packetObject,
            @NotNull BaseComponent name
    ) {
        if (packetObject instanceof RefPacketPlayOutEntityMetadata) {
            RefPacketPlayOutEntityMetadata packet = (RefPacketPlayOutEntityMetadata) packetObject;

            RefWorldServer worldServer = ((RefCraftWorld) (Object) world).getHandle();
            int entityId = packet.id;
            RefEntity entity = WorldUtils.getEntityFromIDByNms(worldServer, entityId);

            RefSynchedEntityData entityData = new RefSynchedEntityData(entity);
            if (COMPONENT_NAME_SUPPORT) {
                defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_COMPONENT, Optional.of(EntityUtils.toNms(name)));
            } else {
                defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_STRING, name.toLegacyText());
            }
            if (METADATA_NEED_VALUE_LIST) {
                boolean setCustomVisible = true;
                List<RefSynchedEntityData$DataValue> result = new CopyOnWriteArrayList<>();
                for (RefSynchedEntityData$DataValue next : packet.packedItems1) {
                    if (next.id() == RefEntity.DATA_CUSTOM_NAME_VISIBLE.getId()) {
                        setCustomVisible = false;
                        result.add(next);
                    } else {
                        if (COMPONENT_NAME_SUPPORT) {
                            if (next.id() != RefEntity.DATA_CUSTOM_NAME_COMPONENT.getId()) {
                                result.add(next);
                            }
                        } else {
                            if (next.id() != RefEntity.DATA_CUSTOM_NAME_STRING.getId()) {
                                result.add(next);
                            }
                        }
                    }
                }
                if (setCustomVisible) {
                    defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_VISIBLE, true);
                }
                List<RefSynchedEntityData$DataValue> dataList = entityData.packDirty();
                if (dataList == null) {
                    dataList = entityData.getNonDefaultValues();
                }
                result.addAll(new RefPacketPlayOutEntityMetadata(entityId, dataList).packedItems1);
                packet.packedItems1 = result;
            } else {
                boolean setCustomVisible = true;
                List<RefSynchedEntityData$DataItem> result = new CopyOnWriteArrayList<>();
                for (RefSynchedEntityData$DataItem next : packet.packedItems0) {
                    if (next.getAccessor() == RefEntity.DATA_CUSTOM_NAME_VISIBLE) {
                        setCustomVisible = false;
                        result.add(next);
                    } else {
                        if (COMPONENT_NAME_SUPPORT) {
                            if (next.getAccessor() != RefEntity.DATA_CUSTOM_NAME_COMPONENT) {
                                result.add(next);
                            }
                        } else {
                            if (next.getAccessor() != RefEntity.DATA_CUSTOM_NAME_STRING) {
                                result.add(next);
                            }
                        }
                    }
                }
                if (setCustomVisible) {
                    defineAndForceSet(entityData, RefEntity.DATA_CUSTOM_NAME_VISIBLE, true);
                }
                result.addAll(new RefPacketPlayOutEntityMetadata(entityId, entityData, true).packedItems0);
                packet.packedItems0 = result;
            }
        }
    }

    private static <T> void defineAndForceSet(
            @NotNull RefSynchedEntityData entityData,
            @NotNull RefEntityDataAccessor<T> key,
            @NotNull T value
    ) {
        entityData.define(key, value);
        if (FORCE_DATA_SET) {
            entityData.set(key, value, true);
        } else {
            entityData.set(key, value);
        }
    }

    private static void lookAtByNms(
            @NotNull Player player,
            double x,
            double y,
            double z
    ) {
        RefEntityPlayer nmsPlayer = ((RefCraftPlayer) (Object) player).getHandle();
        if (PACKET_LOOK_AT_SUPPORT) {
            EntityUtils.lookAtByNms(player, x, y, z);
            nmsPlayer.playerConnection.sendPacket(new RefPacketPlayOutLookAt(RefAnchor.EYES, x, y, z));
        } else {
            player.teleport(player.getLocation().setDirection(player.getEyeLocation().multiply(-1).add(x, y, z).toVector()));
        }
    }

    private static void swingByNms(
            @NotNull RefEntityPlayer player,
            @NotNull RefEnumHand hand
    ) {
        swingByNms(player, hand, true);
    }

    private static void swingByNms(
            @NotNull RefEntityPlayer player,
            @NotNull RefEnumHand hand,
            boolean fromServerPlayer
    ) {
        if (SWING_AND_SEND_SUPPORT) {
            player.swing(hand, fromServerPlayer);
        } else {
            if (fromServerPlayer) {
                player.playerConnection.sendPacket(new RefPacketPlayOutAnimation(player, hand == RefEnumHand.MAIN_HAND ? 0 : 3));
            }
            player.swing(hand);
        }
    }

    @NotNull
    private static RefEnumHand toRefEnumHand(
            @NotNull EnumHand hand
    ) {
        if (hand == EnumHand.MAIN_HAND) {
            return RefEnumHand.MAIN_HAND;
        } else {
            return RefEnumHand.OFF_HAND;
        }
    }
}
