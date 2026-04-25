package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.config.StartupConfig;
import dglabmc.platform.PlatformServices;
import dglabmc.platform.forge.ForgeRuleEventBridge;
import dglabmc.rule.RuleEventContext;
import dglabmc.rule.TriggerRegistry;
import dglabmc.security.EncryptedChatSignal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber(modid = dglabmc.DgLabMcMod.MODID, value = Dist.CLIENT)
public final class ClientHooks {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static KeyMapping openMenuKey;
    private static boolean menuKeyLatch;
    private static boolean suppressNextScreenChar;
    private static boolean sprinting;
    private static boolean crouching;
    private static boolean lowHealthLatched;
    private static boolean lowFoodLatched;
    private static boolean lowArmorLatched;
    private static boolean totemLatched;
    private static int equippedTotemCount;
    private static long clientTickCounter;
    private static float lastObservedHealth = -1.0F;
    private static int lastObservedHurtTime;
    private static int lastObservedDeathTime;
    private static final Map<Integer, PendingAttack> PENDING_ATTACKS = new LinkedHashMap<Integer, PendingAttack>();
    private static final Queue<String> PENDING_SIGNAL_TRIGGERS = new ConcurrentLinkedQueue<String>();
    private static final Map<String, Long> RECENT_SENT_SIGNAL_PAYLOADS = new LinkedHashMap<String, Long>();
    private static final long SIGNAL_ECHO_DEDUP_TICKS = 40L;

    private ClientHooks() {
    }

    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        if (openMenuKey == null) {
            openMenuKey = new KeyMapping(
                "key.dglabmc.open",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_O),
                "key.categories.dglabmc"
            );
        }
        event.register(openMenuKey);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (openMenuKey == null) {
            return;
        }
        boolean down = openMenuKey.isDown();
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
        queueSyntheticSignalFromSentChat(message);
        if (("dgDebug".equals(message) || message.startsWith("/dglab")) && ClientCommandRouter.tryHandle(message)) {
            if (message.startsWith("/dglab") && MINECRAFT.gui != null && MINECRAFT.gui.getChat() != null) {
                MINECRAFT.gui.getChat().addRecentChat(message);
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientChatReceived(ClientChatReceivedEvent event) {
        LocalPlayer player = MINECRAFT.player;
        if (player == null || event.getMessage() == null) {
            return;
        }
        queueSyntheticSignalFromReceivedChat(event, player);
    }

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        PlatformServices.client().showPlayerMessage("DG-LAB 控制中心已就绪，按 O 打开。");
        if (StartupConfig.OPEN_MENU_ON_LOGIN.get()) {
            PlatformServices.client().openControlCenter();
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
        LocalPlayer player = MINECRAFT.player;
        if (player == null) {
            sprinting = false;
            crouching = false;
            lowHealthLatched = false;
            lowFoodLatched = false;
            lowArmorLatched = false;
            totemLatched = false;
            equippedTotemCount = 0;
            lastObservedHealth = -1.0F;
            lastObservedHurtTime = 0;
            lastObservedDeathTime = 0;
            PENDING_ATTACKS.clear();
            PENDING_SIGNAL_TRIGGERS.clear();
            RECENT_SENT_SIGNAL_PAYLOADS.clear();
            AppServices.get().getRuleEngine().reset();
            return;
        }
        clientTickCounter++;
        pruneRecentSentSignalPayloads();
        drainPendingSignals(player);
        processPendingAttacks(player);
        pollLocalPlayerEvents(player);

        AppServices.get().getRuleEngine().tick(ForgeRuleEventBridge.createContext(player, ""));

        boolean sprintNow = player.isSprinting();
        if (sprintNow && !sprinting) {
            fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.SPRINT_START));
        }
        boolean crouchNow = player.isCrouching();
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

        int foodLevel = player.getFoodData().getFoodLevel();
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
        if (event.getEntity().level().isClientSide && ForgeRuleEventBridge.isLocalPlayer(event.getEntity())) {
            fireTrigger(ForgeRuleEventBridge.createContext((Player) event.getEntity(), TriggerRegistry.JUMP));
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        LocalPlayer player = MINECRAFT.player;
        if (player == null || event.getEntity() == null || !player.getUUID().equals(event.getEntity().getUUID())) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity && isClientSideEntity(event.getTarget())) {
            trackOutgoingAttack((LivingEntity) event.getTarget(), false);
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        LocalPlayer player = MINECRAFT.player;
        if (player != null
            && event.getEntity() != null
            && event.getEntity().level().isClientSide
            && event.getEntity().getUUID().equals(player.getUUID())
            && event.isVanillaCritical()
            && event.getTarget() instanceof LivingEntity) {
            trackOutgoingAttack((LivingEntity) event.getTarget(), true);
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        LocalPlayer player = MINECRAFT.player;
        if (player != null
            && event.getEntity() != null
            && event.getEntity().level().isClientSide
            && event.getEntity().getUUID().equals(player.getUUID())) {
            fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.BOW_RELEASE));
        }
    }

    private static void fireTrigger(RuleEventContext context) {
        if (context == null) {
            return;
        }
        ClientCommandRouter.reportTrigger(context.triggerId);
        AppServices.get().getRuleEngine().fire(context);
    }

    private static void queueSyntheticSignalFromSentChat(String message) {
        LocalPlayer player = MINECRAFT.player;
        if (player == null) {
            return;
        }
        String payload = EncryptedChatSignal.extractEncodedPayload(message);
        if (payload.isEmpty()) {
            return;
        }
        String triggerId = extractSyntheticSignalTrigger(message, player);
        if (triggerId.isEmpty()) {
            return;
        }
        RECENT_SENT_SIGNAL_PAYLOADS.put(payload, Long.valueOf(clientTickCounter));
        PENDING_SIGNAL_TRIGGERS.offer(triggerId);
    }

    private static void queueSyntheticSignalFromReceivedChat(ClientChatReceivedEvent event, LocalPlayer player) {
        SignalMatch match = extractSyntheticSignalMatch(event, player);
        if (match == null) {
            return;
        }
        Long sentTick = RECENT_SENT_SIGNAL_PAYLOADS.get(match.payload);
        if (sentTick != null && (clientTickCounter - sentTick.longValue()) <= SIGNAL_ECHO_DEDUP_TICKS) {
            RECENT_SENT_SIGNAL_PAYLOADS.remove(match.payload);
            return;
        }
        PENDING_SIGNAL_TRIGGERS.offer(match.triggerId);
    }

    private static SignalMatch extractSyntheticSignalMatch(ClientChatReceivedEvent event, LocalPlayer player) {
        if (event instanceof ClientChatReceivedEvent.Player) {
            ClientChatReceivedEvent.Player playerEvent = (ClientChatReceivedEvent.Player) event;
            SignalMatch signedMatch = extractSyntheticSignalMatch(playerEvent.getPlayerChatMessage().signedContent(), player);
            if (signedMatch != null) {
                return signedMatch;
            }
            SignalMatch decoratedMatch = extractSyntheticSignalMatch(playerEvent.getPlayerChatMessage().decoratedContent().getString(), player);
            if (decoratedMatch != null) {
                return decoratedMatch;
            }
        }
        return extractSyntheticSignalMatch(event.getMessage().getString(), player);
    }

    private static SignalMatch extractSyntheticSignalMatch(String message, LocalPlayer player) {
        if (player == null || message == null || message.isEmpty()) {
            return null;
        }
        String payload = EncryptedChatSignal.extractEncodedPayload(message);
        if (payload.isEmpty()) {
            return null;
        }
        String triggerId = extractSyntheticSignalTrigger(message, player);
        if (triggerId.isEmpty()) {
            return null;
        }
        return new SignalMatch(payload, triggerId);
    }

    private static String extractSyntheticSignalTrigger(String message, LocalPlayer player) {
        if (player == null || message == null || message.isEmpty()) {
            return "";
        }
        String playerName = player.getGameProfile() == null ? "" : player.getGameProfile().getName();
        if (playerName.isEmpty()) {
            return "";
        }
        return EncryptedChatSignal.extractSignalTrigger(message, playerName);
    }

    private static void pruneRecentSentSignalPayloads() {
        Iterator<Map.Entry<String, Long>> iterator = RECENT_SENT_SIGNAL_PAYLOADS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if ((clientTickCounter - entry.getValue().longValue()) > SIGNAL_ECHO_DEDUP_TICKS) {
                iterator.remove();
            }
        }
    }

    private static void drainPendingSignals(LocalPlayer player) {
        String triggerId;
        while ((triggerId = PENDING_SIGNAL_TRIGGERS.poll()) != null) {
            dispatchSyntheticSignal(player, triggerId);
        }
    }

    private static void dispatchSyntheticSignal(LocalPlayer player, String triggerId) {
        RuleEventContext context = ForgeRuleEventBridge.createSignalContext(player, triggerId);
        if (context == null) {
            return;
        }
        ClientCommandRouter.reportTrigger(context.triggerId);
        AppServices.get().getRuleEngine().fireSynthetic(context);
    }

    private static void pollLocalPlayerEvents(LocalPlayer player) {
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

        if (hurtAnimationStarted && player.isBlocking()) {
            RuleEventContext blockContext = ForgeRuleEventBridge.createContext(player, TriggerRegistry.SHIELD_BLOCK);
            blockContext.damage = damageAmount;
            fireTrigger(blockContext);
        }

        boolean deadNow = currentHealth <= 0.0F || !player.isAlive() || currentDeathTime > 0;
        if (deadNow && lastObservedDeathTime <= 0 && lastObservedHealth > 0.0F) {
            fireTrigger(ForgeRuleEventBridge.createContext(player, TriggerRegistry.PLAYER_DEATH));
            if (lastDamageSource != null && lastDamageSource.getEntity() != null && !player.getUUID().equals(lastDamageSource.getEntity().getUUID())) {
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
        return source != null && (source.is(DamageTypes.FALL) || "fall".equals(source.getMsgId()));
    }

    private static void fireAttackDamageTriggers(Player player, LivingEntity target, float damage) {
        fireAttackCategory(player, target instanceof Player, TriggerRegistry.ATTACK_DAMAGE_ALL, TriggerRegistry.ATTACK_DAMAGE_PLAYER, TriggerRegistry.ATTACK_DAMAGE_NON_PLAYER, damage);
    }

    private static void fireAttackCriticalTriggers(Player player, LivingEntity target) {
        fireAttackCategory(player, target instanceof Player, TriggerRegistry.ATTACK_CRITICAL_ALL, TriggerRegistry.ATTACK_CRITICAL_PLAYER, TriggerRegistry.ATTACK_CRITICAL_NON_PLAYER, 0.0F);
    }

    private static void fireAttackKillTriggers(Player player, LivingEntity target) {
        fireAttackCategory(player, target instanceof Player, TriggerRegistry.ATTACK_KILL_ALL, TriggerRegistry.ATTACK_KILL_PLAYER, TriggerRegistry.ATTACK_KILL_NON_PLAYER, 0.0F);
    }

    private static void fireAttackKillTriggers(Player player, boolean playerTarget) {
        fireAttackCategory(player, playerTarget, TriggerRegistry.ATTACK_KILL_ALL, TriggerRegistry.ATTACK_KILL_PLAYER, TriggerRegistry.ATTACK_KILL_NON_PLAYER, 0.0F);
    }

    private static void fireAttackCategory(Player player, boolean playerTarget, String allTrigger, String playerTrigger, String nonPlayerTrigger, float damage) {
        RuleEventContext allContext = ForgeRuleEventBridge.createContext(player, allTrigger);
        allContext.damage = damage;
        fireTrigger(allContext);

        RuleEventContext typedContext = ForgeRuleEventBridge.createContext(player, playerTarget ? playerTrigger : nonPlayerTrigger);
        typedContext.damage = damage;
        fireTrigger(typedContext);
    }

    private static boolean isClientSideEntity(Entity entity) {
        return entity != null && entity.level().isClientSide;
    }

    private static void trackOutgoingAttack(LivingEntity target, boolean critical) {
        PendingAttack pending = PENDING_ATTACKS.get(Integer.valueOf(target.getId()));
        if (pending == null) {
            pending = new PendingAttack(target.getId());
            PENDING_ATTACKS.put(Integer.valueOf(target.getId()), pending);
        }
        pending.playerTarget = target instanceof Player;
        pending.lastObservedHealth = Math.max(0.0F, target.getHealth());
        pending.critical = pending.critical || critical;
        pending.damageTriggered = false;
        pending.killTriggered = false;
        pending.createdTick = clientTickCounter;
        pending.lastDamageTick = -1L;
    }

    private static void processPendingAttacks(LocalPlayer player) {
        if (MINECRAFT.level == null || PENDING_ATTACKS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, PendingAttack>> iterator = PENDING_ATTACKS.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingAttack pending = iterator.next().getValue();
            Entity entity = MINECRAFT.level.getEntity(pending.entityId);
            if (!(entity instanceof LivingEntity)) {
                if (pending.damageTriggered && !pending.killTriggered && pending.lastDamageTick >= 0L && (clientTickCounter - pending.lastDamageTick) <= 8L) {
                    fireAttackKillTriggers(player, pending.playerTarget);
                    pending.killTriggered = true;
                }
                if (pending.killTriggered || (clientTickCounter - pending.createdTick) > 20L) {
                    iterator.remove();
                }
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
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

            if (!pending.killTriggered && (!target.isAlive() || health <= 0.0F)) {
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

    private static final class SignalMatch {
        final String payload;
        final String triggerId;

        private SignalMatch(String payload, String triggerId) {
            this.payload = payload;
            this.triggerId = triggerId;
        }
    }

    private static int countEquippedTotems(LocalPlayer player) {
        int count = 0;
        if (player.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING) {
            count += player.getMainHandItem().getCount();
        }
        if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
            count += player.getOffhandItem().getCount();
        }
        return count;
    }

    private static boolean hasTotemActivationState(LocalPlayer player) {
        return player.getHealth() <= 2.0F
            && player.hasEffect(MobEffects.REGENERATION)
            && player.hasEffect(MobEffects.ABSORPTION)
            && player.hasEffect(MobEffects.FIRE_RESISTANCE);
    }
}
