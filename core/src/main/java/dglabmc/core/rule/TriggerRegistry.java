package dglabmc.core.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TriggerRegistry {
    public static final String PLAYER_HURT = "player_hurt";
    public static final String PLAYER_HEAL = "player_heal";
    public static final String PLAYER_DEATH = "player_death";
    public static final String PLAYER_KILLED_BY_OTHER = "player_killed_by_other";
    public static final String LOW_HEALTH = "low_health_threshold";
    public static final String LOW_FOOD = "low_food_threshold";
    public static final String JUMP = "jump";
    public static final String FALL_DAMAGE = "fall_damage";
    public static final String SPRINT_START = "sprint_start";
    public static final String SNEAK_START = "sneak_start";
    public static final String SHIELD_BLOCK = "shield_block";
    public static final String ATTACK_DAMAGE_ALL = "attack_damage_all";
    public static final String ATTACK_DAMAGE_PLAYER = "attack_damage_player";
    public static final String ATTACK_DAMAGE_NON_PLAYER = "attack_damage_non_player";
    public static final String ATTACK_CRITICAL_ALL = "attack_critical_all";
    public static final String ATTACK_CRITICAL_PLAYER = "attack_critical_player";
    public static final String ATTACK_CRITICAL_NON_PLAYER = "attack_critical_non_player";
    public static final String ATTACK_KILL_ALL = "attack_kill_all";
    public static final String ATTACK_KILL_PLAYER = "attack_kill_player";
    public static final String ATTACK_KILL_NON_PLAYER = "attack_kill_non_player";
    public static final String BOW_RELEASE = "bow_release";
    public static final String TOTEM_TRIGGER = "totem_trigger";
    public static final String ARMOR_LOW = "armor_low";

    @Deprecated
    public static final String PLAYER_KILL = "player_kill";
    @Deprecated
    public static final String CRITICAL_ATTACK = "critical_attack";

    private static final Map<String, TriggerDefinition> TRIGGERS = new LinkedHashMap<String, TriggerDefinition>();

    static {
        register(new TriggerDefinition(PLAYER_HURT, "受伤", "本地玩家受到伤害时触发"));
        register(new TriggerDefinition(PLAYER_HEAL, "治疗", "本地玩家恢复生命值时触发"));
        register(new TriggerDefinition(PLAYER_DEATH, "死亡", "本地玩家死亡时触发"));
        register(new TriggerDefinition(PLAYER_KILLED_BY_OTHER, "被击杀", "本地玩家被其他实体击杀时触发"));
        register(new TriggerDefinition(LOW_HEALTH, "低血量", "血量低于阈值时触发"));
        register(new TriggerDefinition(LOW_FOOD, "低饥饿", "饥饿值低于阈值时触发"));
        register(new TriggerDefinition(JUMP, "跳跃", "本地玩家起跳时触发"));
        register(new TriggerDefinition(FALL_DAMAGE, "摔落伤害", "因坠落受伤时触发"));
        register(new TriggerDefinition(SPRINT_START, "开始冲刺", "进入冲刺状态时触发"));
        register(new TriggerDefinition(SNEAK_START, "开始潜行", "进入潜行状态时触发"));
        register(new TriggerDefinition(SHIELD_BLOCK, "盾牌格挡", "格挡伤害时触发"));
        register(new TriggerDefinition(ATTACK_DAMAGE_ALL, "造成伤害（全部）", "本地玩家对任意目标造成伤害时触发"));
        register(new TriggerDefinition(ATTACK_DAMAGE_PLAYER, "对玩家造成伤害", "本地玩家对玩家造成伤害时触发"));
        register(new TriggerDefinition(ATTACK_DAMAGE_NON_PLAYER, "造成伤害（不含玩家）", "本地玩家对非玩家目标造成伤害时触发"));
        register(new TriggerDefinition(ATTACK_CRITICAL_ALL, "暴击（全部）", "本地玩家对任意目标造成暴击时触发"));
        register(new TriggerDefinition(ATTACK_CRITICAL_PLAYER, "对玩家暴击", "本地玩家对玩家造成暴击时触发"));
        register(new TriggerDefinition(ATTACK_CRITICAL_NON_PLAYER, "暴击（不含玩家）", "本地玩家对非玩家目标造成暴击时触发"));
        register(new TriggerDefinition(ATTACK_KILL_ALL, "击杀（全部）", "本地玩家击杀任意目标时触发"));
        register(new TriggerDefinition(ATTACK_KILL_PLAYER, "击杀玩家", "本地玩家击杀玩家时触发"));
        register(new TriggerDefinition(ATTACK_KILL_NON_PLAYER, "击杀（不含玩家）", "本地玩家击杀非玩家目标时触发"));
        register(new TriggerDefinition(BOW_RELEASE, "拉弓释放", "释放弓箭时触发"));
        register(new TriggerDefinition(TOTEM_TRIGGER, "图腾触发", "不死图腾生效时触发"));
        register(new TriggerDefinition(ARMOR_LOW, "护甲低耐久", "装备低耐久时触发"));
    }

    private TriggerRegistry() {
    }

    public static void register(TriggerDefinition definition) {
        TRIGGERS.put(definition.id, definition);
    }

    public static List<TriggerDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<TriggerDefinition>(TRIGGERS.values()));
    }

    public static boolean isKnown(String id) {
        return TRIGGERS.containsKey(id);
    }
}

