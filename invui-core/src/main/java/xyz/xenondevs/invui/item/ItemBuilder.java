package xyz.xenondevs.invui.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import xyz.xenondevs.inventoryaccess.InventoryAccess;
import xyz.xenondevs.inventoryaccess.component.BungeeComponentWrapper;
import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
import xyz.xenondevs.inventoryaccess.util.ReflectionRegistry;
import xyz.xenondevs.inventoryaccess.util.ReflectionUtils;
import xyz.xenondevs.invui.util.MojangApiUtils;
import xyz.xenondevs.invui.util.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract base class for item builders.
 */
public class ItemBuilder implements ItemProvider {

    /**
     * The {@link ItemStack} to use as a base.
     */
    protected ItemStack base;
    /**
     * The {@link Material} of the {@link ItemStack}.
     */
    protected Material material;
    /**
     * The amount of the {@link ItemStack}.
     */
    protected int amount = 1;
    /**
     * The damage value of the {@link ItemStack}
     */
    protected Integer damage;
    /**
     * The custom model data value of the {@link ItemStack}.
     */
    protected Integer customModelData;
    /**
     * The unbreakable state of the {@link ItemStack}.
     */
    protected Boolean unbreakable;
    /**
     * The display name of the {@link ItemStack}.
     */
    protected String displayName;
    /**
     * The lore of the {@link ItemStack}.
     */
    protected List<String> lore;
    /**
     * The selected {@link ItemFlag ItemFlags} of the {@link ItemStack}.
     */
    protected List<ItemFlag> itemFlags;
    /**
     * The enchantments of the {@link ItemStack}.
     */
    protected HashMap<Enchantment, Pair<Integer, Boolean>> enchantments;
    /**
     * Additional modifier functions to be run after building the {@link ItemStack}.
     */
    protected List<Function<ItemStack, ItemStack>> modifiers;

    private List<Pattern> patterns = new ArrayList<>();

    private Integer fireworkPower;
    private List<FireworkEffect> fireworkEffects = new ArrayList<>();
    private FireworkEffect fireworkEffect;

    private List<PotionEffect> potionEffects = new ArrayList<>();
    private Color potionColor;
    private PotionType potionType;
    private PotionData basePotionData;

    private GameProfile gameProfile;
    private String skin;

    /**
     * Constructs a new {@link ItemBuilder} based on the given {@link Material}.
     *
     * @param material The {@link Material}
     */
    public ItemBuilder(@NotNull Material material) {
        this.material = material;
    }

    /**
     * Constructs a new {@link ItemBuilder} based on the given {@link Material} and amount.
     *
     * @param material The {@link Material}
     * @param amount   The amount
     */
    public ItemBuilder(@NotNull Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    /**
     * Constructs a new {@link ItemBuilder} based on the give {@link ItemStack}.
     * This will keep the {@link ItemStack} and uses it's {@link ItemMeta}
     *
     * @param base The {@link ItemStack to use as a base}
     */
    public ItemBuilder(@NotNull ItemStack base) {
        this.base = base.clone();
        this.amount = base.getAmount();
        this.material = base.getType();
    }

    /**
     * Builds the {@link ItemStack}
     *
     * @return The {@link ItemStack}
     */
    @Contract(value = "_ -> new", pure = true)
    @Override
    public @NotNull ItemStack get(@Nullable String lang) {
        ItemStack itemStack;
        if (base != null) {
            itemStack = base;
            itemStack.setAmount(amount);
        } else {
            itemStack = new ItemStack(material, amount);
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            // display name
            if (displayName != null) {
                itemMeta.setDisplayName(displayName);
            }

            // lore
            if (lore != null) {
                itemMeta.setLore(lore);
            }

            // damage
            if (itemMeta instanceof Damageable)
                if (damage != null)
                    ((Damageable) itemMeta).setDamage(damage);

            // custom model data
            if (customModelData != null)
                itemMeta.setCustomModelData(customModelData);

            // unbreakable
            if (unbreakable != null)
                itemMeta.setUnbreakable(unbreakable);

            // enchantments
            if (enchantments != null) {
                if (base != null)
                    itemMeta.getEnchants().forEach((enchantment, level) -> itemMeta.removeEnchant(enchantment));

                enchantments.forEach((enchantment, pair) -> itemMeta.addEnchant(enchantment, pair.getFirst(), pair.getSecond()));
            }

            // item flags
            if (itemFlags != null) {
                if (base != null)
                    itemMeta.removeItemFlags(itemMeta.getItemFlags().toArray(new ItemFlag[0]));

                itemMeta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));
            }

            // banner meta
            if (itemMeta instanceof BannerMeta bannerMeta) {
                bannerMeta.setPatterns(patterns);
            }

            // firework meta
            if (itemMeta instanceof FireworkMeta fireworkMeta) {
                if (fireworkPower != null) fireworkMeta.setPower(fireworkPower);
                fireworkMeta.clearEffects();
                fireworkMeta.addEffects(fireworkEffects);
            }

            if (itemMeta instanceof FireworkEffectMeta fireworkEffectMeta) {
                if (fireworkEffect != null) fireworkEffectMeta.setEffect(fireworkEffect);
            }

            // potion meta
            if (itemMeta instanceof PotionMeta potionMeta) {
                potionMeta.clearCustomEffects();
                if (potionColor != null) potionMeta.setColor(potionColor);
                if (potionType != null) potionMeta.setBasePotionType(potionType);
                if (basePotionData != null) potionMeta.setBasePotionData(basePotionData);
                potionEffects.forEach(effect -> potionMeta.addCustomEffect(effect, true));
            }

            // skull texture
            if (itemMeta instanceof SkullMeta) {
                if (gameProfile != null) {
                    if (ReflectionRegistry.CB_CRAFT_META_SKULL_SET_PROFILE_METHOD != null) {
                        ReflectionUtils.invokeMethod(ReflectionRegistry.CB_CRAFT_META_SKULL_SET_PROFILE_METHOD, itemMeta, gameProfile);
                    } else {
                        ReflectionUtils.setFieldValue(ReflectionRegistry.CB_CRAFT_META_SKULL_PROFILE_FIELD, itemMeta, gameProfile);
                    }
                }
            }

            // apply to the item stack
            itemStack.setItemMeta(itemMeta);
        }

        // run modifiers
        if (modifiers != null) {
            for (Function<ItemStack, ItemStack> modifier : modifiers)
                itemStack = modifier.apply(itemStack);
        }

        return itemStack;
    }

    /**
     * Removes a lore line at the given index.
     *
     * @param index The index of the lore line to remove
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder removeLoreLine(int index) {
        if (lore != null) lore.remove(index);
        return this;
    }

    /**
     * Clears the lore.
     *
     * @return The builder instance
     */
    @Contract("-> this")
    public @NotNull ItemBuilder clearLore() {
        if (lore != null) lore.clear();
        return this;
    }

    /**
     * Gets the base {@link ItemStack} of this builder.
     *
     * @return The base {@link ItemStack}
     */
    public @Nullable ItemStack getBase() {
        return base;
    }

    /**
     * Gets the {@link Material} of this builder.
     *
     * @return The {@link Material}
     */
    public @NotNull Material getMaterial() {
        return material;
    }

    /**
     * Sets the {@link Material} of this builder.
     *
     * @param material The {@link Material}
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder setMaterial(@NotNull Material material) {
        this.material = material;
        return this;
    }

    /**
     * Gets the amount.
     *
     * @return The amount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Sets the amount.
     *
     * @param amount The amount
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Gets the damage value.
     *
     * @return The damage value
     */
    public @Nullable Integer getDamage() {
        return damage;
    }

    /**
     * Sets the damage value.
     *
     * @param damage The damage value
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder setDamage(int damage) {
        this.damage = damage;
        return this;
    }

    /**
     * Gets the custom model data value.
     *
     * @return The custom model data value
     */
    public @Nullable Integer getCustomModelData() {
        return customModelData;
    }

    /**
     * Sets the custom model data value.
     *
     * @param customModelData The custom model data value
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    /**
     * Gets the unbreakable state, null for default.
     *
     * @return The unbreakable state
     */
    public @Nullable Boolean isUnbreakable() {
        return unbreakable;
    }

    /**
     * Sets the unbreakable state.
     *
     * @param unbreakable The unbreakable state
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    /**
     * Gets the display name.
     *
     * @return The display name
     */
    public @Nullable PotionType getBasePotionType() {
        return potionType;
    }

    /**
     * Gets the display name.
     *
     * @return The display name
     */
    public @Nullable Color getPotionColor() {
        return potionColor;
    }

    /**
     * Gets the display name.
     *
     * @return The display name
     */
    public @Nullable GameProfile getGameProfile() {
        return gameProfile;
    }

    /**
     * Gets the skin.
     *
     * @return The skin
     */
    public @Nullable String getSkin() {
        return skin;
    }

    /**
     * Gets the display name.
     *
     * @return The display name
     */
    public @Nullable String getDisplayName() {
        return displayName;
    }

    public @Nullable Integer getFireworkPower() {
        return fireworkPower;
    }

    /**
     * Gets the display name.
     *
     * @return The display name
     */
    public @Nullable List<Pattern> getBannerPatterns() {
        return patterns;
    }

    /**
     * Sets the display name.
     *
     * @param displayName The display name
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    //<editor-fold desc="lore">

    /**
     * Gets the lore.
     *
     * @return The lore
     */
    public @Nullable List<String> getLore() {
        return lore;
    }

    /**
     * Sets the lore using the legacy text format.
     *
     * @param lore The lore
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder setLore(@NotNull List<@NotNull String> lore) {
        this.lore = lore;
        return this;
    }

    /**
     * Adds lore lindes using the legacy text format.
     *
     * @param lines The lore lines
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder addLoreLines(@NotNull String... lines) {
        if (lore == null) lore = new ArrayList<>();
        Collections.addAll(lore, lines);
        return this;
    }
    //</editor-fold>

    //<editor-fold desc="item flags">

    /**
     * Gets the configured {@link ItemFlag ItemFlags}.
     *
     * @return The {@link ItemFlag ItemFlags}
     */
    public @Nullable List<ItemFlag> getItemFlags() {
        return itemFlags;
    }

    /**
     * Sets the {@link ItemFlag ItemFlags}.
     *
     * @param itemFlags The {@link ItemFlag ItemFlags}
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder setItemFlags(@NotNull List<ItemFlag> itemFlags) {
        this.itemFlags = itemFlags;
        return this;
    }

    /**
     * Adds {@link ItemFlag ItemFlags}.
     *
     * @param itemFlags The {@link ItemFlag ItemFlags}
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder addItemFlags(@NotNull ItemFlag... itemFlags) {
        if (this.itemFlags == null) this.itemFlags = new ArrayList<>();
        this.itemFlags.addAll(Arrays.asList(itemFlags));
        return this;
    }

    /**
     * Adds {@link ItemFlag ItemFlags}.
     *
     * @param itemFlags The {@link ItemFlag ItemFlags}
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder addItemFlags(@NotNull List<ItemFlag> itemFlags) {
        if (this.itemFlags == null) this.itemFlags = new ArrayList<>();
        this.itemFlags.addAll(itemFlags);
        return this;
    }

    /**
     * Add {@link ItemFlag ItemFlag}.
     *
     * @param itemFlag The {@link ItemFlag ItemFlag}
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder addItemFlag(@NotNull ItemFlag itemFlag) {
        if (this.itemFlags == null) this.itemFlags = new ArrayList<>();
        this.itemFlags.add(itemFlag);
        return this;
    }

    /**
     * Adds all existing {@link ItemFlag ItemFlags}.
     *
     * @return The builder instance
     */
    @Contract("-> this")
    public @NotNull ItemBuilder addAllItemFlags() {
        this.itemFlags = new ArrayList<>(Arrays.asList(ItemFlag.values()));
        return this;
    }

    /**
     * Removes the specified {@link ItemFlag ItemFlags}.
     *
     * @param itemFlags The {@link ItemFlag ItemFlags} to remove
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder removeItemFlags(@NotNull ItemFlag... itemFlags) {
        if (this.itemFlags != null)
            this.itemFlags.removeAll(Arrays.asList(itemFlags));
        return this;
    }

    /**
     * Removes all {@link ItemFlag ItemFlags}.
     *
     * @return The builder instance
     */
    @Contract("-> this")
    public @NotNull ItemBuilder clearItemFlags() {
        if (itemFlags != null) itemFlags.clear();
        return this;
    }
    //</editor-fold>

    //<editor-fold desc="enchantments">

    /**
     * Gets the enchantments.
     *
     * @return The enchantments
     */
    public @Nullable HashMap<Enchantment, Pair<Integer, Boolean>> getEnchantments() {
        return enchantments;
    }

    /**
     * Sets the enchantments.
     *
     * @param enchantments The enchantments
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder setEnchantments(@NotNull HashMap<Enchantment, Pair<Integer, Boolean>> enchantments) {
        this.enchantments = enchantments;
        return this;
    }

    /**
     * Adds an enchantment.
     *
     * @param enchantment            The enchantment
     * @param level                  The level
     * @param ignoreLevelRestriction Whether to ignore the level restriction
     * @return The builder instance
     */
    @Contract("_, _, _ -> this")
    public @NotNull ItemBuilder addEnchantment(Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        if (enchantments == null) enchantments = new HashMap<>();
        enchantments.put(enchantment, new Pair<>(level, ignoreLevelRestriction));
        return this;
    }

    /**
     * Adds an enchantment.
     *
     * @param enchantment The enchantment
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder removeEnchantment(Enchantment enchantment) {
        if (enchantments != null) enchantments.remove(enchantment);
        return this;
    }

    /**
     * Removes all enchantments.
     *
     * @return The builder instance
     */
    @Contract("-> this")
    public @NotNull ItemBuilder clearEnchantments() {
        if (enchantments != null) enchantments.clear();
        return this;
    }
    //</editor-fold>

    //<editor-fold desc="modifiers">

    /**
     * Gets the configured modifier functions.
     *
     * @return The modifier functions
     */
    public @Nullable List<Function<ItemStack, ItemStack>> getModifiers() {
        return modifiers;
    }

    /**
     * Adds a modifier function, which will be run after building the {@link ItemStack}.
     *
     * @param modifier The modifier function
     * @return The builder instance
     */
    @Contract("_ -> this")
    public @NotNull ItemBuilder addModifier(Function<ItemStack, ItemStack> modifier) {
        if (modifiers == null) modifiers = new ArrayList<>();
        modifiers.add(modifier);
        return this;
    }

    /**
     * Removes all modifier functions.
     *
     * @return The builder instance
     */
    @Contract("-> this")
    public @NotNull ItemBuilder clearModifiers() {
        if (modifiers != null) modifiers.clear();
        return this;
    }
    //</editor-fold>

    @Contract("_ -> this")
    public @NotNull ItemBuilder addPattern(@NotNull Pattern pattern) {
        patterns.add(pattern);
        return this;
    }

    @Contract("_, _ -> this")
    public @NotNull ItemBuilder addPattern(@NotNull DyeColor color, @NotNull PatternType type) {
        patterns.add(new Pattern(color, type));
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setBannerPatterns(@NotNull List<@NotNull Pattern> patterns) {
        this.patterns = patterns;
        return this;
    }

    @Contract("-> this")
    public @NotNull ItemBuilder clearPatterns() {
        patterns.clear();
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setFireworkPower(@Range(from = 0, to = 127) int power) {
        this.fireworkPower = power;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder addFireworkEffect(@NotNull FireworkEffect effect) {
        fireworkEffects.add(effect);
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder addFireworkEffect(@NotNull FireworkEffect.Builder builder) {
        fireworkEffects.add(builder.build());
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setFireworkEffects(@NotNull List<@NotNull FireworkEffect> effects) {
        this.fireworkEffects = effects;
        return this;
    }

    @Contract("-> this")
    public @NotNull ItemBuilder clearFireworkEffects() {
        fireworkEffects.clear();
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setFireworkEffect(@NotNull FireworkEffect effect) {
        this.fireworkEffect = effect;
        return this;
    }

    public @Nullable FireworkEffect getFireworkEffect() {
        return fireworkEffect;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setFireworkEffect(@NotNull FireworkEffect.Builder effect) {
        this.fireworkEffect = effect.build();
        return this;
    }

    public @Nullable List<FireworkEffect> getFireworkEffects() {
        return this.fireworkEffects;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setBasePotionType(@NotNull PotionType potionType) {
        this.potionType = potionType;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setPotionColor(@NotNull Color potionColor) {
        this.potionColor = potionColor;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setPotionColor(@NotNull java.awt.Color color) {
        this.potionColor = Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue());
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setBasePotionData(@NotNull PotionData basePotionData) {
        this.basePotionData = basePotionData;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder addPotionEffect(@NotNull PotionEffect effect) {
        potionEffects.add(effect);
        return this;
    }

    @Contract("_, _, _ -> this")
    public @NotNull ItemBuilder addPotionEffect(@NotNull PotionEffectType type, int duration, int amplifier) {
        potionEffects.add(new PotionEffect(type, duration, amplifier));
        return this;
    }

    public @NotNull ItemBuilder setSkin(String skin) {
        this.skin = skin;
        gameProfile = new GameProfile(UUID.randomUUID(), "InvUI");
        PropertyMap propertyMap = gameProfile.getProperties();
        propertyMap.put("textures", new Property("textures", this.skin));
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setGameProfile(@NotNull HeadTexture texture) {
        skin = texture.getTextureValue();
        gameProfile = new GameProfile(UUID.randomUUID(), "InvUI");
        PropertyMap propertyMap = gameProfile.getProperties();
        propertyMap.put("textures", new Property("textures", texture.getTextureValue()));
        return this;
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setGameProfile(@NotNull String name) throws MojangApiUtils.MojangApiException, IOException {
        return this.setGameProfile(HeadTexture.of(name));
    }

    @Contract("_ -> this")
    public @NotNull ItemBuilder setGameProfile(@NotNull UUID uuid) throws MojangApiUtils.MojangApiException, IOException {
        return this.setGameProfile(HeadTexture.of(uuid));
    }

    /**
     * Clones this builder.
     *
     * @return The cloned builder
     */
    @Contract(value = "-> new", pure = true)
    @Override
    public @NotNull ItemBuilder clone() {
        try {
            ItemBuilder clone = ((ItemBuilder) super.clone());
            if (base != null) clone.base = base.clone();
            if (lore != null) clone.lore = new ArrayList<>(lore);
            if (itemFlags != null) clone.itemFlags = new ArrayList<>(itemFlags);
            if (enchantments != null) clone.enchantments = new HashMap<>(enchantments);
            if (modifiers != null) clone.modifiers = new ArrayList<>(modifiers);
            if (patterns != null) clone.patterns = new ArrayList<>(patterns);
            if (fireworkEffects != null) clone.fireworkEffects = new ArrayList<>(fireworkEffects);
            if (potionEffects != null) clone.potionEffects = new ArrayList<>(potionEffects);
            if (fireworkEffect != null) clone.fireworkEffect = fireworkEffect;
            if (gameProfile != null) clone.gameProfile = gameProfile;
            if (potionType != null) clone.potionType = potionType;
            if (potionColor != null) clone.potionColor = potionColor;
            if (basePotionData != null) clone.basePotionData = basePotionData;
            if (skin != null) clone.skin = skin;


            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Contains the texture value for a player head.
     */
    public static class HeadTexture implements Serializable {

        private static final Cache<UUID, String> textureCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build();

        private static final Cache<String, UUID> uuidCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build();

        private final String textureValue;

        /**
         * Creates a new {@link HeadTexture} from the raw texture value.
         *
         * @param textureValue The texture value of this {@link HeadTexture}
         * @see HeadTexture#of(OfflinePlayer)
         * @see HeadTexture#of(UUID)
         * @see HeadTexture#of(String)
         */
        public HeadTexture(@NotNull String textureValue) {
            this.textureValue = textureValue;
        }

        /**
         * Retrieves the {@link HeadTexture} from this {@link OfflinePlayer}
         * Please note that this data might not be pulled from the Mojang API as it might already be cached.
         * Use {@link HeadTexture#invalidateCache()} to invalidate the cache.
         *
         * @param offlinePlayer The skull owner.
         * @return The {@link HeadTexture} of that player.
         * @throws MojangApiUtils.MojangApiException If the Mojang API returns an error.
         * @throws IOException                       If an I/O error occurs.
         * @see HeadTexture#of(UUID)
         */
        public static @NotNull HeadTexture of(@NotNull OfflinePlayer offlinePlayer) throws MojangApiUtils.MojangApiException, IOException {
            return of(offlinePlayer.getUniqueId());
        }

        /**
         * Retrieves the {@link HeadTexture} from the username of the skull owner.
         * This will first retrieve the {@link UUID} of the player from either Bukkit's usercache.json file
         * (if the server is in only mode) or from the Mojang API (if the server is in offline mode).
         * <p>
         * Please note that this data might not be pulled from the Mojang API as it might already be cached.
         * Use {@link HeadTexture#invalidateCache()} to invalidate the cache.
         *
         * @param playerName The username of the player.
         * @return The {@link HeadTexture} of that player.
         * @throws MojangApiUtils.MojangApiException If the Mojang API returns an error.
         * @throws IOException                       If an I/O error occurs.
         * @see HeadTexture#of(UUID)
         */
        @SuppressWarnings("deprecation")
        public static @NotNull HeadTexture of(@NotNull String playerName) throws MojangApiUtils.MojangApiException, IOException {
            if (Bukkit.getServer().getOnlineMode()) {
                // if the server is in online mode, the Minecraft UUID cache (usercache.json) can be used
                return of(Bukkit.getOfflinePlayer(playerName).getUniqueId());
            } else {
                // the server isn't in online mode - the UUID has to be retrieved from the Mojang API
                try {
                    return of(uuidCache.get(playerName, () -> MojangApiUtils.getCurrentUuid(playerName)));
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof MojangApiUtils.MojangApiException) {
                        throw (MojangApiUtils.MojangApiException) cause;
                    } else if (cause instanceof IOException) {
                        throw (IOException) cause;
                    } else {
                        throw new RuntimeException(cause);
                    }
                }
            }
        }

        /**
         * Retrieves the {@link HeadTexture} from the {@link UUID} of the skull owner.
         * Please note that this data might not be pulled from the Mojang API as it might already be cached.
         * Use {@link HeadTexture#invalidateCache()} to invalidate the cache.
         *
         * @param uuid The {@link UUID} of the skull owner.
         * @return The {@link HeadTexture} of that player.
         * @throws MojangApiUtils.MojangApiException If the Mojang API returns an error.
         * @throws IOException                       If an I/O error occurs.
         */
        public static @NotNull HeadTexture of(@NotNull UUID uuid) throws MojangApiUtils.MojangApiException, IOException {
            try {
                return new HeadTexture(textureCache.get(uuid, () -> MojangApiUtils.getSkinData(uuid, false)[0]));
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof MojangApiUtils.MojangApiException) {
                    throw (MojangApiUtils.MojangApiException) cause;
                } else if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else {
                    throw new RuntimeException(cause);
                }
            }
        }

        /**
         * Invalidates the uuid and texture value cache.
         * This means that when {@link HeadTexture#of(OfflinePlayer)}, {@link HeadTexture#of(UUID)}
         * and {@link HeadTexture#of(String)} are called, these values will be pulled from the
         * Mojang API again.
         */
        public static void invalidateCache() {
            uuidCache.invalidateAll();
            textureCache.invalidateAll();
        }

        /**
         * Gets the stored texture value.
         *
         * @return The stored texture value.
         */
        public @NotNull String getTextureValue() {
            return textureValue;
        }

    }

}
