package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.DgLabMcMod;
import dglabmc.config.StartupConfig;
import dglabmc.platform.PlatformServices;
import dglabmc.platform.forge.ForgeRuleEventBridge;
import dglabmc.core.rule.RuleEventContext;
import dglabmc.core.rule.TriggerRegistry;
import dglabmc.core.security.EncryptedChatSignal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.util.DamageSource;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber(modid = DgLabMcMod.MODID, value = Side.CLIENT)
public final class ClientHooks {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();
    private static KeyBinding openMenuKey;
    private static boolean menuKeyLatch;
    private static boolean suppressNextScreenChar;
    private static boolean sprinting;
    private static boolean crouching;
    private static boolean lowHealthLatched;
    private static boolean lowFoodLatched;
    private static boolean lowArmorLatched;
    private static boolean totemLatched;
    private static boolean loginNoticeShown;
    private static int equippedTotemCount;
    private static long clientTickCounter;
    private static float lastObservedHealth = -1.0F;
    private static int lastObservedHurtTime;
    private static int lastObservedDeathTime;
    private static final Map<Integer, PendingAttack> PENDING_ATTACKS = new LinkedHashMap<Integer, PendingAttack>();
    private static final Queue<String> PENDING_SIGNAL_TRIGGERS = new ConcurrentLinkedQueue<String>();

    public ClientHooks() {
    }

    public static void registerKeyBinding() {
        openMenuKey = new KeyBinding("key.dglabmc.open", Keyboard.KEY_O, "key.categories.dglabmc");
        ClientRegistry.registerKeyBinding(openMenuKey);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (openMenuKey == null) {
            return;
        }
        boolean down = openMenuKey.isKeyDown();
        if (down && !menuKeyLatch) {
            suppressNextScreenChar = true;
            PlatformServices.client().openControlCenter();
        }
        menuKeyLatch = down;
    }

    public static boolean consumePendingScreenCharSuppression() {
        boolean pending = suppressNextScreenChar;
        suppressNextScreenChar = false;
        return pending;
    }

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String message = event.getMessage().trim();
        if ("dgDebug".equals(message) && ClientCommandRouter.tryHandle(message)) {
            if (MINECRAFT.ingameGUI != null && MINECRAFT.ingameGUI.getChatGUI() != null) {
                MINECRAFT.ingameGUI.getChatGUI().addToSentMessages(message);
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientChatReceived(ClientChatReceivedEvent event) {
        EntityPlayerSP player = MINECRAFT.player;
        if (player == null || event.getMessage() == null) {
            return;
        }
        String playerName = player.getGameProfile() == null ? "" : player.getGameProfile().getName();
        if (playerName.isEmpty()) {
            return;
        }
        String triggerId = EncryptedChatSignal.extractSignalTrigger(event.getMessage().getUnformattedText(), playerName);
        if (!triggerId.isEmpty()) {
            PENDING_SIGNAL_TRIGGERS.offer(triggerId);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (ClientCommandRouter.consumePendingControlCenterOpen()) {
            PlatformServices.client().openControlCenter();
        }
        EntityPlayerSP player = MINECRAFT.player;
        if (player == null) {
            resetState();
            return;
        }

        if (!loginNoticeShown) {
            loginNoticeShown = true;
            PlatformServices.client().showPlayerMessage("DG-LAB 控制中心已就绪，按 O 打开。");
            if (StartupConfig.shouldOpenMenuOnLogin()) {
                PlatformServices.client().openControlCenter();
            }
        }

        clientTickCounter++;
        drainPendingSignals(player);
        processPendingAttacks(player);
        pollLocalPlayerEvents(player);

        AppServices.get().getRuleEngine().tick(ForgeRuleEventBridge.createContext(player, ""));

        boolean sprintNow = player.isSprinting();
        if (sprintNow && !sprinting) {
            fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.SPRINT_START));
        }
        boolean crouchNow = player.isSneaking();
        if (crouchNow && !crouching) {
            fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.SNEAK_START));
        }
        sprinting = sprintNow;
        crouching = crouchNow;

        float health = player.getHealth();
        if (health <= 6.0F) {
            if (!lowHealthLatched) {
                fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.LOW_HEALTH));
                lowHealthLatched = true;
            }
        } else if (health >= 8.0F) {
            lowHealthLatched = false;
        }

        int foodLevel = player.getFoodStats().getFoodLevel();
        if (foodLevel <= 6) {
            if (!lowFoodLatched) {
                fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.LOW_FOOD));
                lowFoodLatched = true;
            }
        } else if (foodLevel >= 9) {
            lowFoodLatched = false;
        }

        if (ForgeRuleEventBridge.lowestArmorRatio(player) <= 0.15D) {
            if (!lowArmorLatched) {
                fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.ARMOR_LOW));
                lowArmorLatched = true;
            }
        } else {
            lowArmorLatched = false;
        }

        int currentTotemCount = countEquippedTotems(player);
        boolean totemActivated = hasTotemActivationState(player);
        if (equippedTotemCount > currentTotemCount && totemActivated && !totemLatched) {
            fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.TOTEM_TRIGGER));
            totemLatched = true;
        } else if (!totemActivated) {
            totemLatched = false;
        }
        equippedTotemCount = currentTotemCount;
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntityLiving().world.isRemote && ForgeRuleEventBridge.isLocalPlayer(event.getEntityLiving())) {
            fireTrigger(ForgeRuleEventBridge.createContext((EntityPlayer) event.getEntityLiving(), TriggerRegistry.JUMP));
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        EntityPlayerSP player = MINECRAFT.player;
        if (player == null || event.getEntityPlayer() == null || !player.getUniqueID().equals(event.getEntityPlayer().getUniqueID())) {
            return;
        }
        if (event.getTarget() instanceof EntityLivingBase && isClientSideEntity(event.getTarget())) {
            trackOutgoingAttack((EntityLivingBase) event.getTarget(), false);
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        EntityPlayerSP player = MINECRAFT.player;
        if (player != null
            && event.getEntityPlayer() != null
            && event.getEntityPlayer().world.isRemote
            && event.getEntityPlayer().getUniqueID().equals(player.getUniqueID())
            && event.isVanillaCritical()
            && event.getTarget() instanceof EntityLivingBase) {
            trackOutgoingAttack((EntityLivingBase) event.getTarget(), true);
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        EntityPlayerSP player = MINECRAFT.player;
        if (player != null
            && event.getEntityPlayer() != null
            && event.getEntityPlayer().world.isRemote
            && event.getEntityPlayer().getUniqueID().equals(player.getUniqueID())) {
            fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.BOW_RELEASE));
        }
    }

    private static void resetState() {
        sprinting = false;
        crouching = false;
        lowHealthLatched = false;
        lowFoodLatched = false;
        lowArmorLatched = false;
        totemLatched = false;
        loginNoticeShown = false;
        equippedTotemCount = 0;
        lastObservedHealth = -1.0F;
        lastObservedHurtTime = 0;
        lastObservedDeathTime = 0;
        PENDING_ATTACKS.clear();
        PENDING_SIGNAL_TRIGGERS.clear();
        AppServices.get().getRuleEngine().reset();
    }

    private static void fireTrigger(RuleEventContext context) {
        if (context == null) {
            return;
        }
        ClientCommandRouter.reportTrigger(context.triggerId);
        AppServices.get().getRuleEngine().fire(context);
    }

    private static void drainPendingSignals(EntityPlayerSP player) {
        String triggerId;
        while ((triggerId = PENDING_SIGNAL_TRIGGERS.poll()) != null) {
            dispatchSyntheticSignal(player, triggerId);
        }
    }

    private static void dispatchSyntheticSignal(EntityPlayerSP player, String triggerId) {
        RuleEventContext context = ForgeRuleEventBridge.createSignalContext(player, triggerId);
        if (context == null) {
            return;
        }
        ClientCommandRouter.reportTrigger(context.triggerId);
        AppServices.get().getRuleEngine().fireSynthetic(context);
    }

    private static void pollLocalPlayerEvents(EntityPlayerSP player) {
        float currentHealth = Math.max(0.0F, player.getHealth());
        int currentHurtTime = player.hurtTime;
        int currentDeathTime = player.deathTime;
        if (lastObservedHealth < 0.0F) {
            lastObservedHealth = currentHealth;
            lastObservedHurtTime = currentHurtTime;
            lastObservedDeathTime = currentDeathTime;
            return;
        }

        float damageAmount = Math.max(0.0F, lastObservedHealth - currentHealth);
        float healAmount = Math.max(0.0F, currentHealth - lastObservedHealth);
        boolean hurtAnimationStarted = currentHurtTime > 0 && lastObservedHurtTime <= 0;
        DamageSource lastDamageSource = player.getLastDamageSource();

        if (damageAmount > 0.01F) {
            RuleEventContext hurtContext = ForgeRuleEventBridge.createContext(player, TriggerRegistry.PLAYER_HURT);
            hurtContext.damage = damageAmount;
            fireTrigger(hurtContext);

            if (isFallDamageSource(lastDamageSource)) {
                RuleEventContext fallContext = ForgeRuleEventBridge.createContext(player, TriggerRegistry.FALL_DAMAGE);
                fallContext.damage = damageAmount;
                fireTrigger(fallContext);
            }
        }

        if (hurtAnimationStarted && player.isActiveItemStackBlocking()) {
            RuleEventContext blockContext = ForgeRuleEventBridge.createContext(player, TriggerRegistry.SHIELD_BLOCK);
            blockContext.damage = damageAmount;
            fireTrigger(blockContext);
        }

        boolean deadNow = currentHealth <= 0.0F || !player.isEntityAlive() || currentDeathTime > 0;
        if (deadNow && lastObservedDeathTime <= 0 && lastObservedHealth > 0.0F) {
            fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.PLAYER_DEATH));
            if (lastDamageSource != null && lastDamageSource.getTrueSource() != null && !player.getUniqueID().equals(lastDamageSource.getTrueSource().getUniqueID())) {
                fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.PLAYER_KILLED_BY_OTHER));
            }
        } else if (!deadNow && healAmount > 0.01F && lastObservedHealth > 0.0F) {
            RuleEventContext healContext = ForgeRuleEventBridge.createContext(player, TriggerRegistry.PLAYER_HEAL);
            healContext.healAmount = healAmount;
            fireTrigger(healContext);
        }

        lastObservedHealth = currentHealth;
        lastObservedHurtTime = currentHurtTime;
        lastObservedDeathTime = currentDeathTime;
    }

    private static boolean isFallDamageSource(DamageSource source) {
        return source != null && (source == DamageSource.FALL || "fall".equals(source.getDamageType()));
    }

    private static void fireAttackDamageTriggers(EntityPlayer player, EntityLivingBase target, float damage) {
        fireAttackCategory(player, target instanceof EntityPlayer, TriggerRegistry.ATTACK_DAMAGE_ALL, TriggerRegistry.ATTACK_DAMAGE_PLAYER, TriggerRegistry.ATTACK_DAMAGE_NON_PLAYER, damage);
    }

    private static void fireAttackCriticalTriggers(EntityPlayer player, EntityLivingBase target) {
        fireAttackCategory(player, target instanceof EntityPlayer, TriggerRegistry.ATTACK_CRITICAL_ALL, TriggerRegistry.ATTACK_CRITICAL_PLAYER, TriggerRegistry.ATTACK_CRITICAL_NON_PLAYER, 0.0F);
    }

    private static void fireAttackKillTriggers(EntityPlayer player, EntityLivingBase target) {
        fireAttackCategory(player, target instanceof EntityPlayer, TriggerRegistry.ATTACK_KILL_ALL, TriggerRegistry.ATTACK_KILL_PLAYER, TriggerRegistry.ATTACK_KILL_NON_PLAYER, 0.0F);
    }

    private static void fireAttackKillTriggers(EntityPlayer player, boolean playerTarget) {
        fireAttackCategory(player, playerTarget, TriggerRegistry.ATTACK_KILL_ALL, TriggerRegistry.ATTACK_KILL_PLAYER, TriggerRegistry.ATTACK_KILL_NON_PLAYER, 0.0F);
    }

    private static void fireAttackCategory(EntityPlayer player, boolean playerTarget, String allTrigger, String playerTrigger, String nonPlayerTrigger, float damage) {
        RuleEventContext allContext = ForgeRuleEventBridge.createContext(player, allTrigger);
        allContext.damage = damage;
        fireTrigger(allContext);

        RuleEventContext typedContext = ForgeRuleEventBridge.createContext(player, playerTarget ? playerTrigger : nonPlayerTrigger);
        typedContext.damage = damage;
        fireTrigger(typedContext);
    }

    private static boolean isClientSideEntity(Entity entity) {
        return entity != null && entity.world != null && entity.world.isRemote;
    }

    private static void trackOutgoingAttack(EntityLivingBase target, boolean critical) {
        PendingAttack pending = PENDING_ATTACKS.get(Integer.valueOf(target.getEntityId()));
        if (pending == null) {
            pending = new PendingAttack(target.getEntityId());
            PENDING_ATTACKS.put(Integer.valueOf(target.getEntityId()), pending);
        }
        pending.playerTarget = target instanceof EntityPlayer;
        pending.lastObservedHealth = Math.max(0.0F, target.getHealth());
        pending.critical = pending.critical || critical;
        pending.damageTriggered = false;
        pending.killTriggered = false;
        pending.createdTick = clientTickCounter;
        pending.lastDamageTick = -1L;
    }

    private static void processPendingAttacks(EntityPlayerSP player) {
        if (MINECRAFT.world == null || PENDING_ATTACKS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, PendingAttack>> iterator = PENDING_ATTACKS.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingAttack pending = iterator.next().getValue();
            Entity entity = MINECRAFT.world.getEntityByID(pending.entityId);
            if (!(entity instanceof EntityLivingBase)) {
                if (pending.damageTriggered && !pending.killTriggered && pending.lastDamageTick >= 0L && (clientTickCounter - pending.lastDamageTick) <= 8L) {
                    fireAttackKillTriggers(player, pending.playerTarget);
                    pending.killTriggered = true;
                }
                if (pending.killTriggered || (clientTickCounter - pending.createdTick) > 20L) {
                    iterator.remove();
                }
                continue;
            }

            EntityLivingBase target = (EntityLivingBase) entity;
            float health = Math.max(0.0F, target.getHealth());
            if (!pending.damageTriggered && health + 0.01F < pending.lastObservedHealth) {
                float damage = Math.max(0.1F, pending.lastObservedHealth - health);
                fireAttackDamageTriggers(player, target, damage);
                if (pending.critical) {
                    fireAttackCriticalTriggers(player, target);
                }
                pending.damageTriggered = true;
                pending.lastDamageTick = clientTickCounter;
            }

            if (!pending.killTriggered && (!target.isEntityAlive() || health <= 0.0F)) {
                if (!pending.damageTriggered) {
                    fireAttackDamageTriggers(player, target, Math.max(1.0F, pending.lastObservedHealth));
                    if (pending.critical) {
                        fireAttackCriticalTriggers(player, target);
                    }
                    pending.damageTriggered = true;
                    pending.lastDamageTick = clientTickCounter;
                }
                fireAttackKillTriggers(player, target);
                pending.killTriggered = true;
            }

            pending.lastObservedHealth = health;
            if (pending.killTriggered || (pending.damageTriggered && pending.lastDamageTick >= 0L && (clientTickCounter - pending.lastDamageTick) > 10L) || (clientTickCounter - pending.createdTick) > 20L) {
                iterator.remove();
            }
        }
    }

    private static int countEquippedTotems(EntityPlayerSP player) {
        int count = 0;
        if (player.getHeldItemMainhand().getItem() == Items.TOTEM_OF_UNDYING) {
            count += player.getHeldItemMainhand().getCount();
        }
        if (player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING) {
            count += player.getHeldItemOffhand().getCount();
        }
        return count;
    }

    private static boolean hasTotemActivationState(EntityPlayerSP player) {
        return player.getHealth() <= 2.0F
            && player.isPotionActive(MobEffects.REGENERATION)
            && player.isPotionActive(MobEffects.ABSORPTION)
            && player.isPotionActive(MobEffects.FIRE_RESISTANCE);
    }

    private static final class PendingAttack {
        final int entityId;
        float lastObservedHealth;
        boolean playerTarget;
        boolean critical;
        boolean damageTriggered;
        boolean killTriggered;
        long createdTick;
        long lastDamageTick;

        private PendingAttack(int entityId) {
            this.entityId = entityId;
        }
    }
}
