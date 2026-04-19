package cn.admilk.dglabweb.platform.forge;

import cn.admilk.dglabweb.rule.RuleEventContext;
import cn.admilk.dglabweb.rule.TriggerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;

public final class ForgeRuleEventBridge {
    private ForgeRuleEventBridge() {
    }

    public static boolean isLocalPlayer(LivingEntity entity) {
        PlayerEntity player = Minecraft.getInstance().player;
        return player != null && entity != null && entity.getUUID().equals(player.getUUID());
    }

    public static RuleEventContext createContext(PlayerEntity player, String triggerId) {
        RuleEventContext context = new RuleEventContext();
        context.triggerId = triggerId;
        context.currentHealth = player.getHealth();
        context.maxHealth = player.getMaxHealth();
        context.currentFood = player.getFoodData().getFoodLevel();
        context.lowestArmorRatio = lowestArmorRatio(player);
        return context;
    }

    public static RuleEventContext createSignalContext(PlayerEntity player, String triggerId) {
        RuleEventContext context = createContext(player, triggerId);
        if (TriggerRegistry.PLAYER_HURT.equals(triggerId)
            || TriggerRegistry.FALL_DAMAGE.equals(triggerId)
            || TriggerRegistry.SHIELD_BLOCK.equals(triggerId)
            || TriggerRegistry.ATTACK_DAMAGE_ALL.equals(triggerId)
            || TriggerRegistry.ATTACK_DAMAGE_PLAYER.equals(triggerId)
            || TriggerRegistry.ATTACK_DAMAGE_NON_PLAYER.equals(triggerId)) {
            context.damage = 3.0F;
        } else if (TriggerRegistry.ATTACK_CRITICAL_ALL.equals(triggerId)
            || TriggerRegistry.ATTACK_CRITICAL_PLAYER.equals(triggerId)
            || TriggerRegistry.ATTACK_CRITICAL_NON_PLAYER.equals(triggerId)) {
            context.damage = 4.0F;
        } else if (TriggerRegistry.ATTACK_KILL_ALL.equals(triggerId)
            || TriggerRegistry.ATTACK_KILL_PLAYER.equals(triggerId)
            || TriggerRegistry.ATTACK_KILL_NON_PLAYER.equals(triggerId)
            || TriggerRegistry.PLAYER_DEATH.equals(triggerId)
            || TriggerRegistry.PLAYER_KILLED_BY_OTHER.equals(triggerId)) {
            context.damage = Math.max(6.0F, Math.max(1.0F, player.getHealth()));
            context.currentHealth = 0.0F;
        } else if (TriggerRegistry.PLAYER_HEAL.equals(triggerId)) {
            context.healAmount = 3.0F;
        } else if (TriggerRegistry.LOW_HEALTH.equals(triggerId)) {
            context.currentHealth = Math.min(context.currentHealth, 4.0F);
        } else if (TriggerRegistry.LOW_FOOD.equals(triggerId)) {
            context.currentFood = Math.min(context.currentFood, 5);
        } else if (TriggerRegistry.ARMOR_LOW.equals(triggerId)) {
            context.lowestArmorRatio = Math.min(context.lowestArmorRatio, 0.1D);
        } else if (TriggerRegistry.TOTEM_TRIGGER.equals(triggerId)) {
            context.currentHealth = Math.min(context.currentHealth, 1.0F);
        }
        return context;
    }

    public static double lowestArmorRatio(PlayerEntity player) {
        double lowest = 1.0D;
        for (EquipmentSlotType slot : EquipmentSlotType.values()) {
            if (slot.getType() != EquipmentSlotType.Group.ARMOR) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }
            double remaining = 1.0D - ((double) stack.getDamageValue() / (double) stack.getMaxDamage());
            lowest = Math.min(lowest, remaining);
        }
        return lowest;
    }
}
