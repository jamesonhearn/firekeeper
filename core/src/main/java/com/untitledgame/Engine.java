package com.untitledgame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.badlogic.gdx.math.Vector2;
import com.untitledgame.assets.*;
import com.untitledgame.logic.*;
import com.untitledgame.ui.HudUi;
import com.untitledgame.ui.ScreenOverlay;
import com.untitledgame.ui.InventoryOverlay;

import java.io.*;
import java.util.*;

import com.untitledgame.ui.UiFont;
import com.untitledgame.utils.FileUtils;

import com.untitledgame.logic.items.DroppedItem;
import com.untitledgame.logic.items.Inventory;
import com.untitledgame.logic.items.Item;
import com.untitledgame.logic.items.ItemRegistry;
import com.untitledgame.logic.items.ItemStack;
import com.untitledgame.logic.npc.Npc;
import com.untitledgame.logic.npc.NpcManager;



public class Engine implements Screen {
    public static final double INVENTORY_ROW_SPACING = 1.5;
    public static final int HEALTH_POTION_MESSAGE_MS = 2000;
    public static final int PLAYER_HEALTH = 50;
    public static final int INVULNERABILITY_FRAMES = 15;
    public static final int ITEM_DROP_RETRIES = 400;
    public static final int LIGHT_SURGE_MESSAGE_MS = 3000;
    public static final double RNG_20_PERCENT = 0.8;
    public static final double RNG_30_PERCENT = 0.7;
    public static final double RNG_95_PERCENT = 0.05;
    public static final int MS_PER_S = 1000;
    public static final int SEC_PER_MIN = 60;
    public static final int ATTACK_OFFSET = -2;
    public static final int DEFAULT_ALPHA = 170;

    public static final int WORLD_WIDTH = World.WIDTH;
    public static final int WORLD_HEIGHT = World.HEIGHT;

    private final int VIEW_WIDTH = 50; //screenWidth / 24;
    private final int VIEW_HEIGHT = 35; //screenHeight / 24;
    public static final int HUD_HEIGHT = 3;
    public static final String SAVE_FILE = "save.txt";

    private final Renderer renderer = new Renderer();
    private TileType[][] world;
    private long worldSeed;
    private Avatar avatar;
    private TextureRegion avatarSprite;
    private StringBuilder history;
    private NpcManager npcManager;
    private long npcSeed;
    private CombatService combatService;
    private long sessionStartMs;
    private long accumulatedPlayTimeMs;
    private long finalPlayTimeMs;
    private int enemiesFelled;
    private int totalDamageTaken;
    private int totalDamageGiven;

    // Inventory system stuffs
    private Inventory inventory;
    private List<DroppedItem> droppedItems;
    private boolean inventoryVisible;
    private String hudMessage;
    private boolean tabDown = false;
    private static final int DEFAULT_SLOT_COUNT = 16;

    // Lighting variables

    // Light decay system
    private static final double MAX_LIGHT_RADIUS = 20.0;      // starting radius
    private static final double MIN_LIGHT_RADIUS = 0.0;      // when reached, avatar dies
    private static final long LIGHT_DECAY_INTERVAL_MS = 10_000; // shrink every 30 seconds

    private long lastDecayTime = -1L;
    private double decayingLightRadius = MAX_LIGHT_RADIUS;

    private static final double BASE_LIGHT_RADIUS = MAX_LIGHT_RADIUS;
    private static final double SURGE_LIGHT_RADIUS = 12.0;
    private static final long LIGHT_SURGE_DURATION_MS = 10_000L;
    private static final long LIGHT_FADE_DURATION_MS = 3_000L;
    private static final long END_FADE_DURATION_MS = 3_000L;
    private long lightSurgeStartMs = -1L;

    private static final long FOOTSTEP_INTERVAL_MS = 400; // 0.5 seconds
    private long lastFootstepTime = 0;


    private boolean lightToggle = true;

    // AUDIO STUFF
    private final AssetManager assets;
    private final AudioPlayer music;

    private long hudMessageExpireMs = 0;

    // Movement variables
    private boolean wDown, aDown, sDown, dDown;
    private char currentDirection = 0;
    private boolean shiftDown = false;
    private boolean prevShiftDown = false;
    private boolean prevWDown, prevADown, prevSDown, prevDDown;
    private boolean prevAttackDown;
    private boolean prevTabDown;
    private boolean dashInProgress = false;
    private double dashDistanceRemaining = 0.0;
    private final Vector2 dashDirection = new Vector2();

    private UiAssets uiAssets;
    private static final String HB_FULL = "ui/healthbar_full.png";
    private static final String HB_75   = "ui/healthbar_75.png";
    private static final String HB_50   = "ui/healthbar_50.png";
    private static final String HB_25   = "ui/healthbar_25.png";
    private static final String HB_ZERO = "ui/healthbar_empty.png";
    private static final int TICK_MS = 50; // create ticks to create consistent movements
    private static final String[] STEP_SOUNDS = new String[]{
            "audio/step1.wav",
            "audio/step2.wav",
            "audio/step3.wav",
            "audio/step4.wav",
            "audio/step5.wav",
            "audio/step6.wav",
            "audio/step7.wav",
            "audio/step8.wav",
            "audio/step9.wav",
            "audio/step10.wav",
            "audio/step11.wav",
            "audio/step12.wav",
            "audio/step13.wav"
    };
    private static final String[] MUSIC_TRACKS = new String[]{
            "audio/cavegame.wav",
            "audio/main_menu.wav",
            "audio/friendlycave2loopable.wav",
            "audio/elevatormovement.wav",
            "audio/loop_dropper.wav"
    };
    private static final String[] UI_TEXTURES = new String[]{
            HB_FULL,
            HB_75,
            HB_50,
            HB_25,
            HB_ZERO
    };
    private static final String[] BASE_TILE_TEXTURES = new String[]{
            "tiles/cave_floor_6.png",
            "tiles/elevator.png",
            "tiles/cave_wall_top.png",
            "tiles/cave_wall_base.png",
            "tiles/cave_wall_left.png",
            "tiles/cave_wall_right.png",
            "tiles/test.png",
            "tiles/cave_floor_1.png",
            "tiles/Collorpalletfloortest32.png",
            "tiles/skull_floor.png",
            "tiles/top_skull.png",
            "tiles/side_skull.png"

    };


    private static final double AVATAR_WALK_SPEED = 5.0;
    private static final double AVATAR_DASH_DISTANCE = 3.0;
    private static final double AVATAR_DASH_DURATION_SECONDS = 0.1;
    private static final double AVATAR_DASH_SPEED = AVATAR_DASH_DISTANCE / AVATAR_DASH_DURATION_SECONDS;
    private static final double MELEE_HALF_WIDTH = 0.45;
    private static final double MELEE_REACH = 0.70;
    private static final double COLLISION_EPSILON = 1e-4;
    private static final double VELOCITY_EPSILON = 1e-6;  // Threshold for detecting avatar movement


    //Animation variables
    private static final int AVATAR_WALK_TICKS = Math.max(1, (int) Math.round(40.0 / TICK_MS));
    private static final int AVATAR_RUN_TICKS = Math.max(1, AVATAR_WALK_TICKS - 1);
    private static final int AVATAR_ATTACK_TICKS = Math.max(1, (int) Math.round(60.0 / TICK_MS));
    private static final int AVATAR_ATTACK_DAMAGE = 1;
    private static final int AVATAR_DEATH_TICKS = Math.max(1, (int) Math.round(80.0 / TICK_MS));
    private static final int AVATAR_BLOCK_TICKS = Math.max(1, (int) Math.round(60.0 / TICK_MS));
    private static final double PARRY_WINDOW_MS = 300.0; // 300ms window to parry after activation

    private final EnumMap<AvatarAction, EnumMap<Direction, Animation<TextureRegion>>> avatarAnimations =
            new EnumMap<>(AvatarAction.class);
    private Animation<TextureRegion> avatarAnimation;
    private float avatarStateTime = 0f;
    private AvatarAction avatarAction = AvatarAction.IDLE;
    private char lastFacing = 's';
    private boolean attackDown = false;
    private boolean attackInProgress = false;
    private boolean attackQueued = false;
    private Direction attackFacing = Direction.DOWN;
    private final Set<Entity> damagedEntitiesThisAttack = new HashSet<>();
    private final ArrayDeque<Character> typedKeys = new ArrayDeque<>();
    private final InputState inputState = new InputState();



    // Parry system
    private boolean parryDown = false;
    private boolean prevParryDown = false;
    private boolean parryInProgress = false;
    private double parryWindowRemainingMs = 0.0;



    // Targeting system
    private boolean targetingEnabled = false;
    private Npc currentTarget = null;
    private boolean tKeyDown = false;
    private boolean prevTKeyDown = false;

    private enum GameState { PLAYING, DYING, DEAD, ENDING, PAUSED, ENDED }
    private GameState gameState = GameState.PLAYING;
    private double endFadeStartRadius = BASE_LIGHT_RADIUS;
    private long endFadeStartMs = -1L;


    private static final long NPC_SEED_SALT = 0x9e3779b97f4a7c15L;

    private enum EnginePhase { MENU, SEED_ENTRY, PLAYING }


    // Added smoothing to animations
    private double drawX = 0, drawY = 0;
    private double tickAccumulatorMs = 0.0;
    private EnginePhase phase = EnginePhase.MENU;
    private final StringBuilder seedBuilder = new StringBuilder();
    private boolean menuMusicStarted = false;
    private boolean gameplayMusicStarted = false;
    private boolean awaitingQuitCommand = false;
    private BitmapFont titleFont;
    private BitmapFont menuFont;
    private BitmapFont hudFont;
    private GlyphLayout glyphLayout;
    private Texture hbFullTexture;
    private Texture hb75Texture;
    private Texture hb50Texture;
    private Texture hb25Texture;
    private Texture hbZeroTexture;
    private TextureAtlas atlas;
    private boolean assetsQueued;
    private boolean assetsReady;
    private SpriteBatch loadingBatch;
    private BitmapFont loadingFont;
    private HudUi hudUi;
    private ScreenOverlay screenOverlay;
    private InventoryOverlay inventoryOverlay;

    public Engine(AssetManager assets) {
        this.assets = assets;
        this.music = new AudioPlayer(assets);
        queueAssetLoads();
        reset();
        renderer.configureView(WORLD_WIDTH, WORLD_HEIGHT, VIEW_WIDTH, VIEW_HEIGHT, HUD_HEIGHT);
    }


    public void interactWithInputString(String input) {
        reset();
        applyCommands(input.toLowerCase(Locale.ROOT), true, false);
    }

    private void reset() {
        world = null;
        worldSeed = 0L;
        avatar = null;
        history = new StringBuilder();
        npcManager = null;
        npcSeed = 0L;
        combatService = new CombatService();
        combatService.setDamageListener(this::recordDamageStats);
        combatService.setParryChecker(this::isEntityParrying);
        avatarAnimations.clear();
        avatarAnimation = null;
        avatarAction = AvatarAction.IDLE;
        currentDirection = 0;
        attackInProgress = false;
        attackQueued = false;
        attackDown = false;
        wDown = false;
        aDown = false;
        sDown = false;
        dDown = false;
        shiftDown = false;
        prevShiftDown = false;
        tabDown = false;
        prevWDown = false;
        prevADown = false;
        prevSDown = false;
        prevDDown = false;
        prevAttackDown = false;
        prevTabDown = false;
        parryDown = false;
        prevParryDown = false;
        parryInProgress = false;
        parryWindowRemainingMs = 0.0;
        dashInProgress = false;
        dashDistanceRemaining = 0.0;
        dashDirection.set(0f, 0f);
        awaitingQuitCommand = false;
        typedKeys.clear();
        gameState = GameState.PLAYING;
        sessionStartMs = 0L;
        accumulatedPlayTimeMs = 0L;
        finalPlayTimeMs = 0L;
        enemiesFelled = 0;
        totalDamageTaken = 0;
        totalDamageGiven = 0;
        endFadeStartRadius = BASE_LIGHT_RADIUS;
        endFadeStartMs = -1L;
        drawX = 0.0;
        drawY = 0.0;
        //Reset inventory
        inventory = new Inventory(DEFAULT_SLOT_COUNT);
        droppedItems = new ArrayList<>();
        inventoryVisible = false;
        clearHudMessage();
        // Reset targeting system
        targetingEnabled = false;
        currentTarget = null;
        tKeyDown = false;
        prevTKeyDown = false;
        resetLighting();
        damagedEntitiesThisAttack.clear();

    }

    private void installInputProcessor() {
        if (Gdx.input != null) {
            Gdx.input.setInputProcessor(inputState);
        }
    }

    private boolean hasNextKeyTyped() {
        return !typedKeys.isEmpty();
    }

    private char nextKeyTyped() {
        return typedKeys.removeFirst();
    }

    private void resetLighting() {
        lightSurgeStartMs = -1L;
        renderer.setLightRadius(BASE_LIGHT_RADIUS);
    }

    private void triggerLightSurge() {
        lightSurgeStartMs = System.currentTimeMillis();
        renderer.setLightRadius(SURGE_LIGHT_RADIUS);
    }

    private void updateLightingRadius() {
        if (lightSurgeStartMs < 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lightSurgeStartMs;

        if (elapsed <= LIGHT_SURGE_DURATION_MS) {
            renderer.setLightRadius(SURGE_LIGHT_RADIUS);
            return;
        }

        if (elapsed <= LIGHT_SURGE_DURATION_MS + LIGHT_FADE_DURATION_MS) {
            double fadeProgress = (double) (elapsed - LIGHT_SURGE_DURATION_MS) / LIGHT_FADE_DURATION_MS;
            double radius = SURGE_LIGHT_RADIUS - (SURGE_LIGHT_RADIUS - BASE_LIGHT_RADIUS) * fadeProgress;
            renderer.setLightRadius(radius);
            return;
        }

        lightSurgeStartMs = -1L;

        // Snap rendering radius back to base
        renderer.setLightRadius(BASE_LIGHT_RADIUS);

        //  Correct the decaying state
        decayingLightRadius = BASE_LIGHT_RADIUS;
        lastDecayTime = System.currentTimeMillis();
    }

    private void queueAssetLoads() {
        if (assetsQueued) {
            return;
        }
        for (String texture : atlasTexturePaths()) {
            assets.load(texture, Texture.class);
        }
        for (String uiTexture : UI_TEXTURES) {
            assets.load(uiTexture, Texture.class);
        }
        for (String sound : STEP_SOUNDS) {
            assets.load(sound, Sound.class);
        }
        for (String track : MUSIC_TRACKS) {
            assets.load(track, Music.class);
        }
        music.loadEffects(STEP_SOUNDS);
        assetsQueued = true;
    }

    private List<SpriteSheetConfig> createSpriteSheetConfigs() {
        List<SpriteSheetConfig> configs = new ArrayList<>();



        // New 8-directional sprite sheets for player (64x64 frames, 15 frames, 8 rows)
        AnimationSetConfig playerConfig = new AnimationSetConfig("player", "avatars/player", 64, 64);
        playerConfig.addAnimation("idle", "Idle.png", 15);
        playerConfig.addAnimation("walk", "Walk.png", 15);
        playerConfig.addAnimation("melee", "Melee.png", 15);
        playerConfig.addAnimation("block", "ShieldBlockMid.png", 15);
        playerConfig.addAnimation("melee2", "Melee2.png", 15);
        playerConfig.addAnimation("takedamage", "TakeDamage.png", 15);
        playerConfig.addAnimation("die", "Die.png", 15);
        configs.addAll(playerConfig.createSpriteSheetConfigs());

        // New 8-directional sprite sheets for NPC (64x64 frames, 15 frames, 8 rows)
        AnimationSetConfig npcConfig = new AnimationSetConfig("npc", "avatars/NPC", 64, 64);
        npcConfig.addAnimation("idle", "Idle.png", 15);
        npcConfig.addAnimation("walk", "Walk.png", 15);
        npcConfig.addAnimation("attack1", "Attack1.png", 15);
        npcConfig.addAnimation("attack2", "Attack2.png", 15);
        npcConfig.addAnimation("takedamage", "TakeDamage.png", 15);
        npcConfig.addAnimation("die", "Die.png", 15);
        configs.addAll(npcConfig.createSpriteSheetConfigs());


        return configs;
    }

    private List<String> atlasTexturePaths() {
        List<String> textures = new ArrayList<>();
        textures.addAll(List.of(BASE_TILE_TEXTURES));

        // Add sprite sheet paths
        List<SpriteSheetConfig> spriteSheets = createSpriteSheetConfigs();
        textures.addAll(SpriteSheetLoader.getSpriteSheetPaths(spriteSheets));
        return textures;
    }

    private void showMainMenu() {
        renderer.clearScreen();
        if (screenOverlay == null) {
            return;
        }
        screenOverlay.renderCentered("FIREKEEPER", new String[]{"N - New World", "L - Load", "Q - Quit"});
    }

    private void renderSeedPrompt() {
        renderer.clearScreen();
        if (screenOverlay == null) {
            return;
        }
        screenOverlay.renderCentered("Enter Seed, then press S", new String[]{seedBuilder.toString()});
    }


    //primary method for overlaying world
    private void renderWithHud() {
        renderer.clearScreen();
        if (lightSurgeStartMs >= 0) {
            updateLightingRadius();
        }
        renderer.setAvatarPosition(avatar.posX(), avatar.posY());
        renderer.updateCamera();
        Renderer.AvatarDraw avatarDraw = buildAvatarDraw();
        renderer.updateLights(droppedItems, npcManager, avatarDraw);
        Renderer.RenderContext context = renderer.buildContext(world);
        renderer.beginBatch();
        renderer.drawWorld(world, npcManager == null ? null : npcManager.corpses(), droppedItems,
                npcManager, context, avatarDraw);
        renderer.endBatch();
        if (lightToggle) {
            renderer.beginBatch();
            renderer.applyFullLightingPass(world, context);
            renderer.endBatch();
        }
        renderHudLayer();

        if (inventoryVisible) {
            drawOverlayRect();
            drawInventoryOverlay();
        }
        if (gameState == GameState.DEAD) {
            drawOverlayRect();
            drawDeathOverlay();
        }
        if (gameState == GameState.ENDED) {
            drawOverlayRect();
            drawEndOverlay();
        }
    }

    private void renderHudLayer() {
        if (hudUi == null) {
            return;
        }
        int currentHealth = 0;
        int maxHealth = 1;
        if (avatar != null && avatar.health() != null) {
            currentHealth = avatar.health().current();
            maxHealth = avatar.health().max();
        }
        hudUi.update(currentHealth, maxHealth, Gdx.graphics.getDeltaTime());
        hudUi.draw();
    }



    private void drawDeathOverlay() {
        if (screenOverlay == null) {
            return;
        }
        screenOverlay.renderCentered("Your light has been extinguished",
                new String[]{"N - New World", "L - Load", "Q - Quit"});
    }

    private void drawEndOverlay() {
        if (screenOverlay == null) {
            return;
        }
        screenOverlay.renderCentered("The lightkeeper has escaped.",
                new String[]{
                        "Escape time: " + formatDuration(finalPlayTimeMs),
                        "Enemies felled: " + enemiesFelled,
                        "Damage taken: " + totalDamageTaken,
                        "N: Play Again",
                        "Q: Quit"
                });
    }



    private void drawInventoryOverlay() {
        if (!inventoryVisible || inventoryOverlay == null || inventory == null) {
            return;
        }
        inventoryOverlay.render(inventory);
    }

    private void drawOverlayRect() {
        ShapeRenderer shapeRenderer = renderer.getShapeRenderer();
        if (shapeRenderer == null || renderer.getCamera() == null) {
            return;
        }
        shapeRenderer.setProjectionMatrix(renderer.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, DEFAULT_ALPHA / 255f);
        shapeRenderer.rect(0f, 0f, VIEW_WIDTH, VIEW_HEIGHT);
        shapeRenderer.end();
    }

    private void drawTextLeft(BitmapFont font, String text, float x, float y) {
        if (font == null || text == null || renderer.getBatch() == null) {
            return;
        }
        font.setColor(Color.WHITE);
        font.draw(renderer.getBatch(), text, x, y);
    }

    private void drawCenteredText(BitmapFont font, String text, float centerX, float centerY) {
        if (font == null || text == null || renderer.getBatch() == null) {
            return;
        }
        ensureGlyphLayout();
        glyphLayout.setText(font, text);
        font.setColor(Color.WHITE);
        float x = centerX - glyphLayout.width / 2f;
        float y = centerY + glyphLayout.height / 2f;
        font.draw(renderer.getBatch(), text, x, y);
    }

    private void ensureGlyphLayout() {
        if (glyphLayout == null) {
            glyphLayout = new GlyphLayout();
        }
    }

    private void onAssetsLoaded() {
        if (assetsReady) return;

        atlas = buildTextureAtlas();
        Tileset.initialize(atlas);
        renderer.initialize(VIEW_WIDTH, VIEW_HEIGHT, atlas);

        // ✅ ADD THIS
        uiAssets = new UiAssets();
        uiAssets.load();

        ensureHudAssetsLoaded();
        initializeHudUi();
        initializeScreenOverlay();
        initializeInventoryOverlay();

        assetsReady = true;
    }
    private TextureAtlas buildTextureAtlas() {
        TextureAtlas built = new TextureAtlas();
        List<SpriteSheetConfig> spriteSheets = createSpriteSheetConfigs();
        SpriteSheetLoader.loadSpriteSheets(assets, built, spriteSheets);
        for (String path : atlasTexturePaths()) {
            if (isSpriteSheetPath(path, spriteSheets)) {
                continue;
            }
            Texture texture = assets.get(path, Texture.class);
            String key = stripExtension(path);
            built.addRegion(key, texture, 0, 0, texture.getWidth(), texture.getHeight());
        }
        return built;
    }

    private boolean isSpriteSheetPath(String path, List<SpriteSheetConfig> configs) {
        for (SpriteSheetConfig config : configs) {
            if (config.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    private void renderLoadingScreen() {
        if (loadingBatch == null) {
            loadingBatch = new SpriteBatch();
        }
        if (loadingFont == null) {
            loadingFont = new BitmapFont();
        }
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        loadingBatch.begin();
        float progress = assets.getProgress() * 100f;
        loadingFont.draw(loadingBatch, "Loading... " + String.format("%.0f%%", progress), 20f, 40f);
        loadingBatch.end();
    }

    private String stripExtension(String path) {
        int dot = path.lastIndexOf('.');
        if (dot <= 0) {
            return path;
        }
        return path.substring(0, dot);
    }

    private void ensureHudAssetsLoaded() {
        if (titleFont != null) {
            return;
        }

        titleFont = uiAssets.font(UiFont.TITLE);
        menuFont  = uiAssets.font(UiFont.BODY);
        hudFont   = uiAssets.font(UiFont.BODY);
        hbFullTexture = assets.get(HB_FULL, Texture.class);
        hb75Texture = assets.get(HB_75, Texture.class);
        hb50Texture = assets.get(HB_50, Texture.class);
        hb25Texture = assets.get(HB_25, Texture.class);
        hbZeroTexture = assets.get(HB_ZERO, Texture.class);
    }

    private void initializeHudUi() {
        if (hudUi != null) {
            return;
        }
        if (hudFont == null || hbFullTexture == null || hb75Texture == null
                || hb50Texture == null || hb25Texture == null || hbZeroTexture == null) {
            return;
        }
        hudUi = new HudUi(hudFont, hbFullTexture, hb75Texture, hb50Texture, hb25Texture, hbZeroTexture);
        pushHudMessageToUi(remainingHudMessageDuration());
    }

    private void initializeScreenOverlay() {
        if (screenOverlay != null || titleFont == null || menuFont == null) {
            return;
        }
        screenOverlay = new ScreenOverlay(titleFont, menuFont);
    }

    private void initializeInventoryOverlay() {
        if (inventoryOverlay != null || menuFont == null) {
            return;
        }
        inventoryOverlay = new InventoryOverlay(titleFont, menuFont);
    }

    private void disposeHudAssets() {
        if (titleFont != null) {
            titleFont = null;
        }
        if (menuFont != null) {
            menuFont = null;
        }
        if (hudFont != null) {
            hudFont = null;
        }
        if (inventoryOverlay != null) {
            inventoryOverlay.dispose();
            inventoryOverlay = null;
        }
        if (screenOverlay != null) {
            screenOverlay.dispose();
            screenOverlay = null;
        }
        if (hbFullTexture != null) {
            hbFullTexture = null;
        }
        if (hb75Texture != null) {
            hb75Texture = null;
        }
        if (hb50Texture != null) {
            hb50Texture = null;
        }
        if (hb25Texture != null) {
            hb25Texture = null;
        }
        if (hbZeroTexture != null) {
            hbZeroTexture = null;
        }
    }


    private boolean inventoryHasItem(Item item) {
        if (inventory == null || item == null) {
            return false;
        }
        for (ItemStack stack : inventory.nonEmptySlots()) {
            if (stack.item().equals(item) && stack.quantity() > 0) {
                return true;
            }
        }
        return false;
    }
    private boolean useHealthPotion() {
        if (gameState != GameState.PLAYING || avatar == null || inventory == null) {
            return false;
        }
        if (!inventory.remove(ItemRegistry.SMALL_POTION, 1)) {
            return false;
        }
        avatar.health().restoreFull();
        setHudMessage("Used Small Potion", HEALTH_POTION_MESSAGE_MS);
        return true;
    }

    private void updateInventoryToggle() {
        boolean tab = tabDown;

        if (tab && !prevTabDown) {
            inventoryVisible = !inventoryVisible;
        }

        prevTabDown = tab;
    }


    private void updateTargetingToggle() {
        boolean t = tKeyDown;

        if (t && !prevTKeyDown) {
            targetingEnabled = !targetingEnabled;
            if (targetingEnabled) {
                setHudMessage("Targeting enabled", 2000);
                updateCurrentTarget();
            } else {
                setHudMessage("Targeting disabled", 2000);
                currentTarget = null;
            }
        }

        prevTKeyDown = t;
    }

    private void updateCurrentTarget() {
        if (!targetingEnabled || npcManager == null || avatar == null) {
            currentTarget = null;
            return;
        }

        // Find nearest living enemy
        Npc nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Npc npc : npcManager.npcs()) {
            if (npc.health() != null && npc.health().current() > 0) {
                double dx = npc.posX() - avatar.posX();
                double dy = npc.posY() - avatar.posY();
                double dist = Math.hypot(dx, dy);

                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = npc;
                }
            }
        }

        currentTarget = nearest;
    }

    private Direction getTargetFacing() {
        if (!targetingEnabled || currentTarget == null || avatar == null) {
            return null;
        }

        // Verify target is still alive
        if (currentTarget.health() == null || currentTarget.health().current() <= 0) {
            updateCurrentTarget();
            if (currentTarget == null) {
                return null;
            }
        }

        // Calculate direction to target
        double dx = currentTarget.posX() - avatar.posX();
        double dy = currentTarget.posY() - avatar.posY();

        return Direction.fromVelocity(dx, dy);
    }

    // applyCommands for loading saves
    private void applyCommands(String input, boolean recordHistory, boolean allowQuit) {
        boolean awaitingQuit = false;
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (awaitingQuit) {
                if (c == 'q') {
                    saveGameState();
                    if (allowQuit) {
                        music.stop();
                        exitGame();
                    }
                    return;
                }
                awaitingQuit = false;
            }


            switch (c) {
                case 'n':
                    int start = i + 1;
                    int end = start;
                    while (end < input.length() && Character.isDigit(input.charAt(end))) {
                        end += 1;
                    }
                    if (end >= input.length() || input.charAt(end) != 's') {
                        return; // seed not of correct form
                    }
                    String seedStr = input.substring(start, end);
                    if (recordHistory) {
                        history.append('n').append(seedStr).append('s');
                    }
                    startNewWorld(parseSeed(seedStr));
                    i = end;
                    break;
                case 'l':
                    loadGame();
                    break;
                case 'w':
                case 'a':
                case 's':
                case 'd':
                    if (world != null) {
                        if (recordHistory) {
                            history.append(c);
                        }
                        boolean moved = applyRecordedMove(c);
                        if (moved) {
                            pickupAtAvatar();
                        }
                    }
                    break;
                case 'e':
                    pickupAtAvatar();
                    break;
                case 'r':
                    if (recordHistory) {
                        history.append(c);
                    }
                    useHealthPotion();
                    break;
                case ':':
                    awaitingQuit = true;
                    break;
                default:
                    break;
            }
            i += 1;
        }
    }

    private boolean applyRecordedMove(char direction) {
        Direction dir = directionFromChar(direction);
        if (avatar == null || dir == null) {
            return false;
        }
        currentDirection = direction;
        Vector2 vec = facingVector(dir);
        avatar.setVelocity(vec.x * AVATAR_WALK_SPEED, vec.y * AVATAR_WALK_SPEED);
        boolean moved = integrateAvatarMotion(0.1);
        avatar.setVelocity(0.0, 0.0);
        if (moved) {
            lastFacing = direction;
        }
        return moved;
    }


    private void tryPlayFootstep() {
        long now = System.currentTimeMillis();
        if (now - lastFootstepTime >= FOOTSTEP_INTERVAL_MS) {
            music.playRandomEffect();
            lastFootstepTime = now;
        }
    }

    private boolean handleMovementRealtime(boolean record, double deltaSeconds) {
        if (avatar == null || world == null || gameState != GameState.PLAYING) {
            return false;
        }
        if (avatar.isStaggered()) {
            avatar.setVelocity(0.0, 0.0);
            currentDirection = 0;
            return false;
        }
        boolean w = wDown;
        boolean a = aDown;
        boolean s = sDown;
        boolean d = dDown;
        boolean attack = attackDown;
        boolean parry = parryDown;
        boolean dashPressed = shiftDown && !prevShiftDown;

        updateDirectionOnPress(w, a, s, d, prevWDown, prevADown, prevSDown, prevDDown);
        updateDirectionOnRelease(w, a, s, d);

        Vector2 desired = new Vector2();
        if (w) desired.y += 1f;
        if (a) desired.x -= 1f;
        if (s) desired.y -= 1f;
        if (d) desired.x += 1f;

        boolean inputActive = desired.len2() > 0;
        boolean freshPress = (w && !prevWDown)
                || (a && !prevADown)
                || (s && !prevSDown)
                || (d && !prevDDown);

        Direction movementFacing = inputActive ? Direction.fromVelocity(desired.x, desired.y) : null;
        if (dashPressed) {
            startDash(movementFacing);
        }
        Direction actionFacing = resolveFacingForAction(movementFacing);
        if (attack && !prevAttackDown) {
            startAttack(actionFacing);
        }
        if (parry && !prevParryDown) {
            startParry(actionFacing);
        }
        boolean movedThisFrame;
        if (dashInProgress) {
            movedThisFrame = updateDashMovement(deltaSeconds);
        } else {
            if (inputActive) {
                desired.nor();
                avatar.setVelocity(desired.x * AVATAR_WALK_SPEED, desired.y * AVATAR_WALK_SPEED);
                if (record && freshPress && currentDirection != 0) {
                    history.append(currentDirection);
                }
            } else {
                avatar.setVelocity(0.0, 0.0);
                currentDirection = 0;
            }
            movedThisFrame = integrateAvatarMotion(deltaSeconds);
        }
        prevWDown = w;
        prevADown = a;
        prevSDown = s;
        prevDDown = d;
        prevAttackDown = attack;
        prevParryDown = parryDown;
        prevShiftDown = shiftDown;

        if (movedThisFrame) {
            tryPlayFootstep();
            pickupAtAvatar();
        }

        return movedThisFrame;
    }

    private void updateDirectionOnPress(boolean w, boolean a, boolean s, boolean d,
                                        boolean prevW, boolean prevA, boolean prevS, boolean prevD) {
        if (w && !prevW) {
            currentDirection = 'w';
        }
        if (a && !prevA) {
            currentDirection = 'a';
        }
        if (s && !prevS) {
            currentDirection = 's';
        }
        if (d && !prevD) {
            currentDirection = 'd';
        }
    }
    private void updateDirectionOnRelease(boolean w, boolean a, boolean s, boolean d) {
        if (!w && currentDirection == 'w') {
            currentDirection = fallbackDirection(false, a, s, d);
        }
        if (!a && currentDirection == 'a') {
            currentDirection = fallbackDirection(w, false, s, d);
        }
        if (!s && currentDirection == 's') {
            currentDirection = fallbackDirection(w, a, false, d);
        }
        if (!d && currentDirection == 'd') {
            currentDirection = fallbackDirection(w, a, s, false);
        }
    }

    // Allow for return to prior direction on multi key movements
    private char fallbackDirection(boolean w, boolean a, boolean s, boolean d) {
        if (w) {
            return 'w';
        }
        if (a) {
            return 'a';
        }
        if (s) {
            return 's';
        }
        if (d) {
            return 'd';
        }
        return 0;
    }

    // Checks for System commands (save/quit)
    private boolean processCommand(char command, boolean record, boolean allowQuit) {
        if (command == ':') {
            awaitingQuitCommand = true;
            return false;
        }
        if (command == 'e') {
            pickupAtAvatar();
            return false;
        }
        if (command == 'r') {
            useHealthPotion();
            return false;
        }
        if (command == 'f') {
            lightToggle = !lightToggle;
        }
        if (command == 'w' || command == 's' || command == 'a' || command == 'd') {
            return false;
        }
        applyCommands(String.valueOf(command), record, allowQuit);
        return false;
    }


    // Generator func via seed - drop player
    private void startNewWorld(long seed) {
        worldSeed = seed;
        sessionStartMs = System.currentTimeMillis();
        accumulatedPlayTimeMs = 0L;
        finalPlayTimeMs = 0L;
        World generator = new World(seed);
        world = generator.generate();
        resetLighting();
        decayingLightRadius = MAX_LIGHT_RADIUS;
        lastDecayTime = System.currentTimeMillis();
        renderer.setLightRadius(decayingLightRadius);
        placeAvatar();
        npcSeed = seed ^ NPC_SEED_SALT; // golden ratio hash
        npcManager = new NpcManager(new Random(npcSeed), combatService, atlas);
        npcManager.setDeathHandler(this::handleNpcDeath);
        npcManager.spawn(world, avatar.x(), avatar.y());
        // give initial items and random spawn ground loot
        seedInitialInventory();
        seedDroppedItems(new Random(seed));
    }

    // Find first coordiate that is valid placement for player on spawn - just seeks from bottom right currently
    // Eventually include ladder/elevator placement
    private void placeAvatar() {
        for (int x = 0; x < WORLD_WIDTH; x += 1) {
            for (int y = 0; y < WORLD_HEIGHT; y += 1) {
                if (world[x][y] == TileType.FLOOR) {
                    HealthComponent avatarHealth = new HealthComponent(PLAYER_HEALTH, PLAYER_HEALTH,
                            1, INVULNERABILITY_FRAMES);
                    avatarHealth.addDeathCallback(this::handleAvatarDeath);
                    avatar = new Avatar(x, y, 3, avatarHealth);
                    avatar.setSpawnPoint(new Entity.Position(x, y));
                    combatService.register(avatar);
                    initializeAvatarAnimations(Direction.DOWN);
                    // Snap the smoothed draw coordinates to the spawn tile so the avatar
                    // doesn't glide in from (0,0) on the first frame.
                    drawX = avatar.posX() - 0.5;
                    drawY = avatar.posY() - 0.5;
                    return;
                }
            }
        }
    }



    // Depending on direction, update avatar position and rotate sprite animation frame
    private void startAttack(Direction facing) {
        if (avatar == null || attackInProgress || parryInProgress) {
            return;
        }
        Direction resolvedFacing = resolveFacingForAction(facing);
        attackInProgress = true;
        attackQueued = true;
        avatarStateTime = 0f;
        attackFacing = resolvedFacing;
        Animation<TextureRegion> attackCycle = avatarAnimations.get(AvatarAction.ATTACK).get(resolvedFacing);
        avatarAnimation = attackCycle;
        avatarAction = AvatarAction.ATTACK;
        avatarSprite = attackCycle.getKeyFrame(avatarStateTime);

        damagedEntitiesThisAttack.clear();
    }


    private void startParry(Direction facing) {
        if (avatar == null || parryInProgress || attackInProgress || avatar.isStaggered()) {
            return;
        }
        Direction resolvedFacing = resolveFacingForAction(facing);

        // Check animations exist before setting state
        EnumMap<Direction, Animation<TextureRegion>> blockAnimations = avatarAnimations.get(AvatarAction.BLOCK);
        if (blockAnimations == null) {
            return;
        }
        Animation<TextureRegion> blockCycle = blockAnimations.get(resolvedFacing);
        if (blockCycle == null) {
            return;
        }

        // Set parry state
        parryInProgress = true;
        parryWindowRemainingMs = PARRY_WINDOW_MS;
        avatarStateTime = 0f;
        avatarAnimation = blockCycle;
        avatarAction = AvatarAction.BLOCK;
        avatarSprite = blockCycle.getKeyFrame(avatarStateTime);
    }



    private void startDash(Direction preferredFacing) {
        if (avatar == null || dashInProgress || avatar.isStaggered()) {
            return;
        }
        Direction dashFacing = preferredFacing != null ? preferredFacing : resolveFacingForAction(null);
        if (avatar != null && preferredFacing != null) {
            avatar.setFacing(dashFacing);
        }
        dashDirection.set(facingVector(dashFacing)).nor();
        dashDistanceRemaining = AVATAR_DASH_DISTANCE;
        dashInProgress = true;
        currentDirection = directionToChar(dashFacing);
    }

    private boolean updateDashMovement(double deltaSeconds) {
        if (avatar == null || attackInProgress || avatar.isStaggered()) {
            return false;
        }
        double startX = avatar.posX();
        double startY = avatar.posY();
        double stepSeconds = Math.min(deltaSeconds, dashDistanceRemaining / AVATAR_DASH_SPEED);
        avatar.setVelocity(dashDirection.x * AVATAR_DASH_SPEED, dashDirection.y * AVATAR_DASH_SPEED);
        integrateAvatarMotion(stepSeconds);
        double moved = Math.hypot(avatar.posX() - startX, avatar.posY() - startY);
        dashDistanceRemaining = Math.max(0.0, dashDistanceRemaining - moved);
        boolean finished = dashDistanceRemaining <= COLLISION_EPSILON
                || (stepSeconds > 0.0 && moved < COLLISION_EPSILON);
        if (finished) {
            dashInProgress = false;
            avatar.setVelocity(0.0, 0.0);
        }
        return moved > 0.0;
    }

    private Direction resolveFacingForAction(Direction movementFacing) {
        if (targetingEnabled) {
            Direction targetFacing = getTargetFacing();
            if (targetFacing != null) {
                if (avatar != null) {
                    avatar.setFacing(targetFacing);
                }
                return targetFacing;
            }
        }
        if (movementFacing != null) {
            if (avatar != null) {
                avatar.setFacing(movementFacing);
            }
            return movementFacing;
        }
        return avatar != null ? avatar.facing() : Direction.DOWN;
    }


    private void initializeAvatarAnimations(Direction facing) {
        avatarAnimations.clear();
        avatarAnimations.put(AvatarAction.IDLE, buildAvatarAnimations8Dir(
                Tileset.AVATAR_WALK_UP[0],
                Tileset.AVATAR_WALK_DOWN[0],
                Tileset.AVATAR_WALK_LEFT[0],
                Tileset.AVATAR_WALK_RIGHT[0],
                Tileset.AVATAR_WALK_UP_RIGHT[0],
                Tileset.AVATAR_WALK_UP_LEFT[0],
                Tileset.AVATAR_WALK_DOWN_RIGHT[0],
                Tileset.AVATAR_WALK_DOWN_LEFT[0],
                frameDurationSeconds(1),
                Animation.PlayMode.LOOP));
        avatarAnimations.put(AvatarAction.WALK, buildAvatarAnimations8Dir(
                Tileset.AVATAR_WALK_UP, Tileset.AVATAR_WALK_DOWN,
                Tileset.AVATAR_WALK_LEFT, Tileset.AVATAR_WALK_RIGHT,
                Tileset.AVATAR_WALK_UP_RIGHT, Tileset.AVATAR_WALK_UP_LEFT,
                Tileset.AVATAR_WALK_DOWN_RIGHT, Tileset.AVATAR_WALK_DOWN_LEFT,
                frameDurationSeconds(AVATAR_WALK_TICKS),
                Animation.PlayMode.LOOP));
        avatarAnimations.put(AvatarAction.RUN, buildAvatarAnimations8Dir(
                Tileset.AVATAR_WALK_UP, Tileset.AVATAR_WALK_DOWN,
                Tileset.AVATAR_WALK_LEFT, Tileset.AVATAR_WALK_RIGHT,
                Tileset.AVATAR_WALK_UP_RIGHT, Tileset.AVATAR_WALK_UP_LEFT,
                Tileset.AVATAR_WALK_DOWN_RIGHT, Tileset.AVATAR_WALK_DOWN_LEFT,
                frameDurationSeconds(AVATAR_RUN_TICKS),
                Animation.PlayMode.LOOP));
        avatarAnimations.put(AvatarAction.ATTACK, buildAvatarAnimations8Dir(
                Tileset.AVATAR_ATTACK_UP, Tileset.AVATAR_ATTACK_DOWN,
                Tileset.AVATAR_ATTACK_LEFT, Tileset.AVATAR_ATTACK_RIGHT,
                Tileset.AVATAR_ATTACK_UP_RIGHT, Tileset.AVATAR_ATTACK_UP_LEFT,
                Tileset.AVATAR_ATTACK_DOWN_RIGHT, Tileset.AVATAR_ATTACK_DOWN_LEFT,
                frameDurationSeconds(AVATAR_ATTACK_TICKS),
                Animation.PlayMode.NORMAL));
        avatarAnimations.put(AvatarAction.DEATH, buildAvatarAnimations8Dir(
                Tileset.AVATAR_DEATH_UP, Tileset.AVATAR_DEATH_DOWN,
                Tileset.AVATAR_DEATH_LEFT, Tileset.AVATAR_DEATH_RIGHT,
                Tileset.AVATAR_DEATH_UP_RIGHT, Tileset.AVATAR_DEATH_UP_LEFT,
                Tileset.AVATAR_DEATH_DOWN_RIGHT, Tileset.AVATAR_DEATH_DOWN_LEFT,
                frameDurationSeconds(AVATAR_DEATH_TICKS),
                Animation.PlayMode.NORMAL));
        avatarAnimations.put(AvatarAction.TAKE_DAMAGE, buildAvatarAnimations8Dir(
                Tileset.AVATAR_TAKE_DAMAGE_UP, Tileset.AVATAR_TAKE_DAMAGE_DOWN,
                Tileset.AVATAR_TAKE_DAMAGE_LEFT, Tileset.AVATAR_TAKE_DAMAGE_RIGHT,
                Tileset.AVATAR_TAKE_DAMAGE_UP_RIGHT, Tileset.AVATAR_TAKE_DAMAGE_UP_LEFT,
                Tileset.AVATAR_TAKE_DAMAGE_DOWN_RIGHT, Tileset.AVATAR_TAKE_DAMAGE_DOWN_LEFT,
                frameDurationSeconds(AVATAR_ATTACK_TICKS),
                Animation.PlayMode.NORMAL));
        avatarAnimations.put(AvatarAction.BLOCK, buildAvatarAnimations8Dir(
                Tileset.AVATAR_BLOCK_UP, Tileset.AVATAR_BLOCK_DOWN,
                Tileset.AVATAR_BLOCK_LEFT, Tileset.AVATAR_BLOCK_RIGHT,
                Tileset.AVATAR_BLOCK_UP_RIGHT, Tileset.AVATAR_BLOCK_UP_LEFT,
                Tileset.AVATAR_BLOCK_DOWN_RIGHT, Tileset.AVATAR_BLOCK_DOWN_LEFT,
                frameDurationSeconds(AVATAR_BLOCK_TICKS),
                Animation.PlayMode.LOOP));

        avatarAction = AvatarAction.IDLE;
        avatarAnimation = avatarAnimations.get(avatarAction).get(facing);
        avatarStateTime = 0f;
        avatarSprite = avatarAnimation.getKeyFrame(avatarStateTime);
        lastFacing = directionToChar(facing);
    }

    private EnumMap<Direction, Animation<TextureRegion>> buildAvatarAnimations8Dir(TextureRegion up, TextureRegion down,
                                                                               TextureRegion left, TextureRegion right,
                                                                               TextureRegion upRight, TextureRegion upLeft,
                                                                               TextureRegion downRight, TextureRegion downLeft,
                                                                               float frameDuration,
                                                                               Animation.PlayMode playMode) {
        return buildAvatarAnimations8Dir(
                new TextureRegion[]{up},
                new TextureRegion[]{down},
                new TextureRegion[]{left},
                new TextureRegion[]{right},
                new TextureRegion[]{upRight},
                new TextureRegion[]{upLeft},
                new TextureRegion[]{downRight},
                new TextureRegion[]{downLeft},
                frameDuration,
                playMode);
    }

    private EnumMap<Direction, Animation<TextureRegion>> buildAvatarAnimations8Dir(TextureRegion[] up, TextureRegion[] down,
                                                                               TextureRegion[] left, TextureRegion[] right,
                                                                               TextureRegion[] upRight, TextureRegion[] upLeft,
                                                                               TextureRegion[] downRight, TextureRegion[] downLeft,
                                                                               float frameDuration,
                                                                               Animation.PlayMode playMode) {
        EnumMap<Direction, Animation<TextureRegion>> map = new EnumMap<>(Direction.class);
        map.put(Direction.UP, animation(frameDuration, playMode, up));
        map.put(Direction.DOWN, animation(frameDuration, playMode, down));
        map.put(Direction.LEFT, animation(frameDuration, playMode, left));
        map.put(Direction.RIGHT, animation(frameDuration, playMode, right));
        map.put(Direction.UP_RIGHT, animation(frameDuration, playMode, upRight));
        map.put(Direction.UP_LEFT, animation(frameDuration, playMode, upLeft));
        map.put(Direction.DOWN_RIGHT, animation(frameDuration, playMode, downRight));
        map.put(Direction.DOWN_LEFT, animation(frameDuration, playMode, downLeft));
        return map;
    }

    private float frameDurationSeconds(int ticksPerFrame) {
        return (float) (ticksPerFrame * (TICK_MS / (double) MS_PER_S));
    }

    private Animation<TextureRegion> animation(float frameDuration, Animation.PlayMode playMode, TextureRegion[] frames) {
        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames);
        animation.setPlayMode(playMode);
        return animation;
    }

    private Direction directionFromChar(char facing) {
        return switch (facing) {
            case 'w' -> Direction.UP;
            case 'a' -> Direction.LEFT;
            case 'd' -> Direction.RIGHT;
            default -> Direction.DOWN;
        };
    }

    private char directionToChar(Direction direction) {
        return switch (direction) {
            case UP -> 'w';
            case LEFT -> 'a';
            case RIGHT -> 'd';
            case DOWN -> 's';
            // For diagonal directions, just use one of the cardinal directions as a fallback
            case UP_RIGHT -> 'w';
            case UP_LEFT -> 'w';
            case DOWN_RIGHT -> 's';
            case DOWN_LEFT -> 's';
        };
    }


    //starting inventory
    private void seedInitialInventory() {
        if (inventory == null) {
            inventory = new Inventory(DEFAULT_SLOT_COUNT);
        }
        inventory.add(ItemRegistry.SMALL_POTION, 2);
        inventory.add(ItemRegistry.TORCH, 1);
    }


    // Randmly place items around the map
    private void seedDroppedItems(Random random) {
        if (world == null || avatar == null) {
            return;
        }
        Item[] candidates = new Item[]{ItemRegistry.LIGHT_SHARD};
        int placed = 0;
        int attempts = 0;
        while (placed < 5 && attempts < ITEM_DROP_RETRIES) {
            int x = random.nextInt(WORLD_WIDTH);
            int y = random.nextInt(WORLD_HEIGHT);
            attempts += 1;
            if (world[x][y] != TileType.FLOOR || (x == avatar.x() && y == avatar.y())) {
                continue;
            }
            Item choice = candidates[placed % candidates.length];
            int qty = 1 + random.nextInt(Math.max(1, choice.getMaxStackSize() / 2));
            droppedItems.add(new DroppedItem(choice, qty, x, y));
            placed += 1;
        }
    }



    // Pickup item in front of avatar if room in inventory
    private void pickupAtAvatar() {
        if (avatar == null || droppedItems == null || inventory == null) {
            return;
        }
        List<DroppedItem> remaining = new ArrayList<>();
        boolean pickedSomething = false;
        for (DroppedItem drop : droppedItems) {
            if (drop.x() == avatar.x() && drop.y() == avatar.y()) {
                if (drop.item() == ItemRegistry.LIGHT_SHARD) {
                    triggerLightSurge();
                    pickedSomething = true;
                    setHudMessage("A burst of light surrounds you", LIGHT_SURGE_MESSAGE_MS);
                    decayingLightRadius = MAX_LIGHT_RADIUS;
                    lastDecayTime = System.currentTimeMillis();

                    // Also update renderer radius immediately
                    renderer.setLightRadius(decayingLightRadius);
                    continue;
                }
                int leftover = inventory.add(drop.item(), drop.quantity());
                pickedSomething = true;
                if (leftover > 0) {
                    drop.setQuantity(leftover);
                    remaining.add(drop);
                    showHudMessage("Inventory full - left " + leftover + " " + drop.item().name());
                } else {
                    showHudMessage("Picked up " + drop.item().name());
                }
            } else {
                remaining.add(drop);
            }
        }
        droppedItems = remaining;
    }


    private void setHudMessage(String msg, long durationMs) {
        hudMessage = msg;
        hudMessageExpireMs = System.currentTimeMillis() + durationMs;
        pushHudMessageToUi(durationMs);
    }

    private void showHudMessage(String msg) {
        hudMessage = msg;
        hudMessageExpireMs = 0L;
        pushHudMessageToUi(0L);
    }

    private void clearHudMessage() {
        hudMessage = "";
        hudMessageExpireMs = 0L;
        pushHudMessageToUi(0L);
    }

    private void pushHudMessageToUi(long durationMs) {
        if (hudUi == null) {
            return;
        }
        if (hudMessage.isEmpty()) {
            hudUi.clearMessage();
            return;
        }
        hudUi.setMessage(hudMessage, durationMs);
    }

    private long remainingHudMessageDuration() {
        if (hudMessageExpireMs == 0L) {
            return 0L;
        }
        return Math.max(0L, hudMessageExpireMs - System.currentTimeMillis());
    }

    private void updateHudMessage() {
        if (!hudMessage.isEmpty() && hudMessageExpireMs > 0 && System.currentTimeMillis() > hudMessageExpireMs) {
            clearHudMessage();
        }
    }

    private void handleAvatarDeath(Entity entity) {
        if (!(entity instanceof Avatar)) {
            return;
        }
        if (gameState != GameState.PLAYING) {
            return;
        }
        beginDeathSequence();
    }

    private void updateLightDecay() {
        if (lastDecayTime < 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lastDecayTime;

        if (elapsed >= LIGHT_DECAY_INTERVAL_MS) {
            lastDecayTime = now;

            // shrink radius
            decayingLightRadius -= 1.0;
            if (decayingLightRadius < MIN_LIGHT_RADIUS) {
                decayingLightRadius = MIN_LIGHT_RADIUS;
            }

            renderer.setLightRadius(decayingLightRadius);

            // Check death condition
            if (decayingLightRadius <= 1.0) {
                decayingLightRadius = 1.0;
                renderer.setLightRadius(decayingLightRadius);
                showHudMessage("Your light has been extinguished");
                handleAvatarDeath(avatar);
            }
        }
    }

    private long currentPlayTimeMs() {
        long base = accumulatedPlayTimeMs;
        if (gameState == GameState.ENDING || gameState == GameState.ENDED) {
            return Math.max(base, finalPlayTimeMs);
        }
        if (sessionStartMs > 0) {
            base += System.currentTimeMillis() - sessionStartMs;
        }
        return base;
    }

    private void recordDamageStats(Entity target, Entity source, int attempted, int applied) {
        if (applied <= 0) {
            return;
        }
        if (target instanceof Avatar) {
            totalDamageTaken += applied;
        }
        if (source instanceof Avatar) {
            totalDamageGiven += applied;
        }
    }

    private boolean isEntityParrying(Entity target) {
        // Only avatar can parry
        if (target instanceof Avatar && avatar == target) {
            return parryWindowRemainingMs > 0;
        }
        return false;
    }



    private void checkForEndgame() {
        if (gameState != GameState.PLAYING || world == null || avatar == null) {
            return;
        }
        if (!inventoryHasItem(ItemRegistry.KEY)) {
            return;
        }
        if (world[avatar.x()][avatar.y()] == TileType.ELEVATOR) {
            beginEndSequence();
        }
    }

    private void handleNpcDeath(Npc npc) {
        Random rng = new Random();
        if (npc == null) {
            return;
        }
        enemiesFelled += 1;
        double r = rng.nextDouble();
        if (r > RNG_20_PERCENT) {
            droppedItems.add(new DroppedItem(ItemRegistry.LIGHT_SHARD, 1, npc.x(), npc.y()));
        }
        if (r <= RNG_20_PERCENT && RNG_30_PERCENT <= r) {
            droppedItems.add(new DroppedItem(ItemRegistry.SMALL_POTION, 1, npc.x(), npc.y()));
        }
        if (r < RNG_95_PERCENT && !inventoryHasItem(ItemRegistry.KEY)) {
            droppedItems.add(new DroppedItem(ItemRegistry.KEY, 1, npc.x(), npc.y()));
        }

    }
    private boolean integrateAvatarMotion(double deltaSeconds) {
        if (avatar == null) {
            return false;
        }
        List<Entity> others = npcManager == null ? List.of() : new ArrayList<>(npcManager.npcs());
        return integrateEntityMotion(avatar, deltaSeconds, others);
    }

    private void updateNpcMovement(double deltaSeconds) {
        if (npcManager == null || world == null) {
            return;
        }
        List<Npc> npcs = npcManager.npcs();
        List<Entity> others = new ArrayList<>();
        if (avatar != null) {
            others.add(avatar);
        }
        others.addAll(npcs);
        for (Npc npc : npcs) {
            npc.tickStagger(deltaSeconds);
            // AI is fully responsible for setting velocity
            npc.updateAnimation((float) deltaSeconds);
            integrateEntityMotion(npc, deltaSeconds, others);
        }
        npcManager.rebuildIndex();
    }

    private boolean integrateEntityMotion(Entity entity, double deltaSeconds, List<? extends Entity> others) {
        double dx = entity.velocityX() * deltaSeconds;
        double dy = entity.velocityY() * deltaSeconds;
        if (Math.abs(dx) < COLLISION_EPSILON && Math.abs(dy) < COLLISION_EPSILON) {
            return false;
        }
        double half = hitboxHalf(entity);
        Vector2 swept = sweepAgainstTiles(entity.posX(), entity.posY(), dx, dy, half);
        Vector2 resolved = resolveEntityOverlaps(swept.x, swept.y, half, entity, others);
        double clampedX = clampToWorld(resolved.x, half, WORLD_WIDTH);
        double clampedY = clampToWorld(resolved.y, half, WORLD_HEIGHT);
        boolean moved = Math.abs(entity.posX() - clampedX) > COLLISION_EPSILON
                || Math.abs(entity.posY() - clampedY) > COLLISION_EPSILON;
        entity.setPosition(clampedX, clampedY);
        return moved;
    }

    private Vector2 sweepAgainstTiles(double posX, double posY, double dx, double dy, double half) {
        double targetX = sweepAxis(posX, posY, dx, dy, half, true);
        double targetY = sweepAxis(targetX, posY, dx, dy, half, false);
        return new Vector2((float) targetX, (float) targetY);
    }

    private double sweepAxis(double posX, double posY, double dx, double dy, double half, boolean axisX) {
        double delta = axisX ? dx : dy;
        if (Math.abs(delta) < COLLISION_EPSILON) {
            return axisX ? posX : posY;
        }
        double start = axisX ? posX : posY;
        double target = start + delta;
        double minAlong = axisX ? posY - half : posX - half;
        double maxAlong = axisX ? posY + half : posX + half;

        if (delta > 0) {
            double edge = start + half;
            int startTile = (int) Math.floor(edge);
            int endTile = (int) Math.floor(target + half);
            for (int t = startTile + 1; t <= endTile; t += 1) {
                if (axisX) {
                    // Sweeping along X → check Y span
                    int minY = (int) Math.floor(posY - half);
                    int maxY = (int) Math.floor(posY + half);
                    for (int y = minY; y <= maxY; y++) {
                        if (isSolidTile(t, y)) {
                            return (t - half) - COLLISION_EPSILON;
                        }
                    }
                } else {
                    // Sweeping along Y → check X span
                    int minX = (int) Math.floor(posX - half);
                    int maxX = (int) Math.floor(posX + half);
                    for (int x = minX; x <= maxX; x++) {
                        if (isSolidTile(x, t)) {
                            return (t - half) - COLLISION_EPSILON;
                        }
                    }
                }
            }
        } else {
            double edge = start - half;
            int startTile = (int) Math.floor(edge);
            int endTile = (int) Math.floor(target - half);
            for (int t = startTile; t >= endTile; t -= 1) {
                if (axisX) {
                    // Sweeping along X → check Y span
                    int minY = (int) Math.floor(posY - half);
                    int maxY = (int) Math.floor(posY + half);
                    for (int y = minY; y <= maxY; y++) {
                        if (isSolidTile(t, y)) {
                            return (t + 1 + half) + COLLISION_EPSILON;                        }
                    }
                } else {
                    // Sweeping along Y → check X span
                    int minX = (int) Math.floor(posX - half);
                    int maxX = (int) Math.floor(posX + half);
                    for (int x = minX; x <= maxX; x++) {
                        if (isSolidTile(x, t)) {
                            return (t + 1 + half) + COLLISION_EPSILON;                        }
                    }
                }
            }
        }
        return target;
    }

    private boolean isSolidTile(int x, int y) {
        if (world == null) {
            return true;
        }
        if (x < 0 || y < 0 || x >= WORLD_WIDTH || y >= WORLD_HEIGHT) {
            return true;
        }
        TileType t = world[x][y];
        return !(t == TileType.FLOOR || t == TileType.ELEVATOR);
    }

    private double clampToWorld(double center, double half, int dimension) {
        double min = half;
        double max = dimension - half;
        return Math.max(min, Math.min(max, center));
    }

    private Vector2 resolveEntityOverlaps(double targetX, double targetY, double half, Entity self,
                                          List<? extends Entity> others) {
        double resolvedX = targetX;
        double resolvedY = targetY;
        for (Entity other : others) {
            if (other == null || other == self) {
                continue;
            }
            double otherHalf = hitboxHalf(other);
            double dx = resolvedX - other.posX();
            double dy = resolvedY - other.posY();
            double dist = Math.hypot(dx, dy);
            double minDist = half + otherHalf;
            if (dist < minDist && dist > COLLISION_EPSILON) {
                double push = (minDist - dist) + COLLISION_EPSILON;
                resolvedX += dx / dist * push;
                resolvedY += dy / dist * push;
            } else if (dist <= COLLISION_EPSILON) {
                resolvedX += minDist;
            }
        }
        return new Vector2((float) resolvedX, (float) resolvedY);
    }

    private double hitboxHalf(Entity entity) {
        if (entity instanceof Avatar) {
            return Avatar.HITBOX_HALF;
        }
        if (entity instanceof Npc) {
            return Npc.HITBOX_HALF;
        }
        return Avatar.HITBOX_HALF;
    }

    private Vector2 facingVector(Direction facing) {
        // Use precise sqrt(2)/2 for diagonal directions instead of approximation
        float diag = (float)(1.0 / Math.sqrt(2.0));
        return switch (facing) {
            case UP -> new Vector2(0f, 1f);
            case DOWN -> new Vector2(0f, -1f);
            case LEFT -> new Vector2(-1f, 0f);
            case RIGHT -> new Vector2(1f, 0f);
            case UP_RIGHT -> new Vector2(diag, diag);
            case UP_LEFT -> new Vector2(-diag, diag);
            case DOWN_RIGHT -> new Vector2(diag, -diag);
            case DOWN_LEFT -> new Vector2(-diag, -diag);
        };
    }

    private void beginDeathSequence() {
        gameState = GameState.DYING;
        currentDirection = 0;
        attackInProgress = false;
        attackQueued = false;
        attackDown = false;
        clampLightToDeathRadius();
        AvatarAction desired = AvatarAction.DEATH;
        Direction facing = directionFromChar(lastFacing);
        EnumMap<Direction, Animation<TextureRegion>> map = avatarAnimations.getOrDefault(desired,
                avatarAnimations.get(AvatarAction.IDLE));
        if (map != null) {
            Animation<TextureRegion> cycle = map.get(facing);
            if (cycle != null) {
                avatarStateTime = 0f;
                avatarAnimation = cycle;
                avatarAction = desired;
                avatarSprite = avatarAnimation.getKeyFrame(avatarStateTime);
            }
        }
    }

    private void clampLightToDeathRadius() {
        if (decayingLightRadius > 1.0) {
            decayingLightRadius = 1.0;
            renderer.setLightRadius(decayingLightRadius);
        }
    }
    private void beginEndSequence() {
        if (gameState != GameState.PLAYING) {
            return;
        }
        music.stop();
        music.play("audio/elevatormovement.wav");
        gameState = GameState.ENDING;
        endFadeStartRadius = Math.max(0.0, renderer.getLightRadius());
        endFadeStartMs = System.currentTimeMillis();
        finalPlayTimeMs = currentPlayTimeMs();
    }

    private void runEndSequence() {
        if (gameState == GameState.ENDING) {
            long elapsed = System.currentTimeMillis() - endFadeStartMs;
            double progress = Math.min(1.0, (double) elapsed / END_FADE_DURATION_MS);
            double radius = Math.max(0.0, endFadeStartRadius * (1.0 - progress));
            decayingLightRadius = radius;
            renderer.setLightRadius(radius);
            if (progress >= 1.0) {
                finalPlayTimeMs = accumulatedPlayTimeMs + (System.currentTimeMillis() - sessionStartMs);
                decayingLightRadius = 0.0;
                renderer.setLightRadius(decayingLightRadius);
                gameState = GameState.ENDED;
            }
        } else {
            handleEndMenuInput();
        }
        tickAvatarAnimation(TICK_MS / (double) MS_PER_S, false);
    }


    private void runDeathSequence() {
        if (gameState == GameState.DYING && avatarAnimation == null) {
            gameState = GameState.DEAD;
        }
        if (gameState == GameState.DYING && avatarAnimation != null
                && avatarAnimation.isAnimationFinished(avatarStateTime)) {
            gameState = GameState.DEAD;
        }
        if (gameState == GameState.DEAD) {
            handleDeathMenuInput();
        }
        tickAvatarAnimation(TICK_MS / (double) MS_PER_S, false);
    }

    private void handleDeathMenuInput() {
        while (hasNextKeyTyped()) {
            char c = Character.toLowerCase(nextKeyTyped());
            if (c == 'q') {
                exitGame();
            }
            if (c == 'n') {
                startNewGameFromDeath();
                return;
            }
            if (c == 'l') {
                restoreSaveFromDeath();
                return;
            }
        }
    }

    private void handleEndMenuInput() {
        while (hasNextKeyTyped()) {
            char c = Character.toLowerCase(nextKeyTyped());
            if (c == 'q') {
                exitGame();
            }
            if (c == 'n') {
                startNewGameFromEnd();
                return;
            }
        }
    }

    private void startNewGameFromDeath() {
        reset();
        beginSeedEntry();
    }
    private void startNewGameFromEnd() {
        reset();
        beginSeedEntry();
    }

    private class InputState implements InputProcessor {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                togglePause();
                return true;
            }

            if (gameState != GameState.PLAYING) return false;

            if (keycode == Input.Keys.W) {
                wDown = true;
            } else if (keycode == Input.Keys.A) {
                aDown = true;
            } else if (keycode == Input.Keys.S) {
                sDown = true;
            } else if (keycode == Input.Keys.D) {
                dDown = true;
            } else if (keycode == Input.Keys.SPACE) {
                attackDown = true;
            } else if (keycode == Input.Keys.V) {
                tabDown = true;
            } else if (keycode == Input.Keys.CONTROL_LEFT) {
                tKeyDown = true;
            } else if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
                shiftDown = true;
            }
            return false;
        }
        private void togglePause() {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;

                // stop motion
                if (avatar != null) {
                    avatar.setVelocity(0, 0);
                }

                // release mouse
                Gdx.input.setCursorCatched(false);
            }
            else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;

                // recapture mouse
                Gdx.input.setCursorCatched(true);
            }
        }

        @Override
        public boolean keyUp(int keycode) {
            if (keycode == Input.Keys.W) {
                wDown = false;
            } else if (keycode == Input.Keys.A) {
                aDown = false;
            } else if (keycode == Input.Keys.S) {
                sDown = false;
            } else if (keycode == Input.Keys.D) {
                dDown = false;
            } else if (keycode == Input.Keys.V) {
                tabDown = false;
            } else if (keycode == Input.Keys.SPACE) {
                attackDown = false;
            } else if (keycode == Input.Keys.CONTROL_LEFT) {
                tKeyDown = false;
            } else if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
                shiftDown = false;
            }
            return false;
        }

        @Override
        public boolean keyTyped(char character) {
            typedKeys.add(character);
            return false;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.LEFT) {
                attackDown = true;
                return true;
            } else if (button == Input.Buttons.RIGHT) {
                parryDown = true;
                return true;
                }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.LEFT) {
                attackDown = false;
                return true;
            } else if (button == Input.Buttons.RIGHT) {
                parryDown = false;
                return true;
            }
            return false;
        }

        @Override
        public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            return false;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            return false;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            return false;
        }
    }

    private void restoreSaveFromDeath() {
        reset();
        boolean loaded = loadGame();
        if (!loaded || world == null) {
            beginSeedEntry();
            return;
        }
        beginGameplay();
    }


    private Direction getMovementBasedFacing() {
        if (avatar != null && (Math.abs(avatar.velocityX()) > VELOCITY_EPSILON || Math.abs(avatar.velocityY()) > VELOCITY_EPSILON)) {
            // Moving - use velocity to determine 8-directional facing
            Direction facing = Direction.fromVelocity(avatar.velocityX(), avatar.velocityY());
            avatar.setFacing(facing);
            return facing;
        } else {
            // Not moving - use last stored facing
            return avatar != null ? avatar.facing() : Direction.DOWN;
        }
    }

    private void tickAvatarAnimation(double deltaSeconds, boolean movedThisTick) {
        if (avatarAnimation == null) {
            return;
        }
// Tick down parry window
        if (parryWindowRemainingMs > 0) {
            double deltaMs = deltaSeconds * 1000.0;
            parryWindowRemainingMs -= deltaMs;
            if (parryWindowRemainingMs <= 0) {
                parryWindowRemainingMs = 0;
                parryInProgress = false;
            }
        }
        if (attackInProgress && avatarAnimation.isAnimationFinished(avatarStateTime)) {
            attackInProgress = false;
            attackQueued = false;
            damagedEntitiesThisAttack.clear();
        }

        // Calculate facing direction from velocity for smooth 8-directional animation
        Direction facing;
        if (attackInProgress) {
            facing = attackFacing;
        } else if (targetingEnabled) {
            // When targeting is enabled, face the target
            Direction targetFacing = getTargetFacing();
            if (targetFacing != null) {
                facing = targetFacing;
                // Store this as the avatar's facing direction
                if (avatar != null) {
                    avatar.setFacing(facing);
                }
            } else {
                // No valid target, fall back to movement-based facing
                facing = getMovementBasedFacing();
            }
        } else {
            // Normal movement-based facing
            facing = getMovementBasedFacing();
        }

        AvatarAction desiredAction;
        if (gameState != GameState.PLAYING) {
            desiredAction = AvatarAction.DEATH;
        } else if (avatar != null && avatar.isStaggered()) {
            desiredAction = AvatarAction.TAKE_DAMAGE;
        } else if (parryInProgress) {
            desiredAction = AvatarAction.BLOCK;
        } else if (attackInProgress) {
            desiredAction = AvatarAction.ATTACK;
        } else if (dashInProgress) {
            desiredAction = AvatarAction.RUN;
        } else if (currentDirection == 0) {
            desiredAction = AvatarAction.IDLE;
        } else {
            desiredAction = AvatarAction.WALK;
        }

        EnumMap<Direction, Animation<TextureRegion>> byDirection = avatarAnimations.get(desiredAction);
        Animation<TextureRegion> selected = byDirection.get(facing);

        boolean actionChanged = desiredAction != avatarAction;
        boolean animationChanged = selected != avatarAnimation;

        if (actionChanged) {
            if (desiredAction == AvatarAction.IDLE) {
                avatarStateTime = 0f;
            } else if (desiredAction == AvatarAction.ATTACK) {
                attackQueued = false;
                attackInProgress = true;
                avatarStateTime = 0f;
            } else if (desiredAction == AvatarAction.TAKE_DAMAGE) {
                attackInProgress = false;
                attackQueued = false;
                avatarStateTime = 0f;
            } else {
                avatarStateTime = carryStateTime(avatarAnimation, selected, avatarStateTime);
            }
        } else if (animationChanged) {
            avatarStateTime = carryStateTime(avatarAnimation, selected, avatarStateTime);
        }
        avatarAction = desiredAction;
        avatarAnimation = selected;

        boolean looping = avatarAction != AvatarAction.ATTACK
                && avatarAction != AvatarAction.DEATH
                && avatarAction != AvatarAction.TAKE_DAMAGE;
        boolean shouldAdvance = looping || movedThisTick || avatarAnimation.getKeyFrames().length > 1;
        if (shouldAdvance) {
            avatarStateTime += deltaSeconds;
        }

        avatarSprite = avatarAnimation.getKeyFrame(avatarStateTime, looping);
        if (attackInProgress) {
            processAvatarAttackOverlaps();
        }
        if (avatarAction == AvatarAction.ATTACK && avatarAnimation.isAnimationFinished(avatarStateTime)) {
            attackInProgress = false;
            attackQueued = false;
            damagedEntitiesThisAttack.clear();
        }
        lastFacing = directionToChar(facing);
    }

    private void processAvatarAttackOverlaps() {
        if (!attackInProgress || npcManager == null || avatar == null || combatService == null) {
            return;
        }

        AttackBounds bounds = buildAttackBounds(avatar.posX(), avatar.posY(), attackFacing,
                Avatar.HITBOX_HALF, MELEE_REACH, MELEE_HALF_WIDTH);

        for (Npc npc : npcManager.npcs()) {
            if (damagedEntitiesThisAttack.contains(npc)) {
                continue;
            }
            if (overlaps(bounds, npc.posX(), npc.posY(), Npc.HITBOX_HALF)) {
                combatService.queueDamage(npc, avatar, AVATAR_ATTACK_DAMAGE);
                damagedEntitiesThisAttack.add(npc);
            }
        }
    }

    private AttackBounds buildAttackBounds(double originX, double originY, Direction facing, double attackerHalf,
                                           double reach, double halfWidth) {
        double along = reach / 2.0;
        double across = halfWidth;

        Vector2 dir = facingVector(facing);
        double halfX = (Math.abs(dir.x) > 0.0) ? along : across;
        double halfY = (Math.abs(dir.y) > 0.0) ? along : across;
        double centerX = originX + dir.x * (attackerHalf + along);
        double centerY = originY + dir.y * (attackerHalf + along);
        return new AttackBounds(centerX, centerY, halfX, halfY);
    }

    private boolean overlaps(AttackBounds bounds, double otherX, double otherY, double otherHalf) {
        double dx = Math.abs(bounds.centerX - otherX);
        double dy = Math.abs(bounds.centerY - otherY);
        return dx <= (bounds.halfX + otherHalf) && dy <= (bounds.halfY + otherHalf);
    }

    private record AttackBounds(double centerX, double centerY, double halfX, double halfY) { }


    private float carryStateTime(Animation<TextureRegion> previous, Animation<TextureRegion> next, float previousStateTime) {
        if (previous == null || next == null) {
            return 0f;
        }
        int frameIndex = previous.getKeyFrameIndex(previousStateTime);
        return frameIndex * next.getFrameDuration();
    }



    //Load game via save file if exists, restores state directly from snapshot
    private boolean loadGame() {
        if (!FileUtils.fileExists(SAVE_FILE)) {
            return false;
        }
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            Object raw = input.readObject();
            if (!(raw instanceof SaveState saved)) {
                return false;
            }
            //restoreFromState(saved);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    private void saveGameState() {
        if (world == null || avatar == null || npcManager == null || inventory == null) {
            return;
        }
        SaveState.SaveSnapshot snap = new SaveState.SaveSnapshot(
                worldSeed,
                npcSeed,
                avatar,
                decayingLightRadius,
                lastDecayTime,
                lightSurgeStartMs,
                currentPlayTimeMs(),
                enemiesFelled,
                totalDamageTaken,
                totalDamageGiven,
                inventory,
                droppedItems,
                npcManager.npcs(),
                npcManager.corpses()
        );

        SaveState state = SaveState.capture(snap);
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            output.writeObject(state);
        } catch (IOException e) {
            // Best-effort; ignore failures to keep gameplay responsive
        }
    }
//
//    private void restoreFromState(SaveState state) {
//        reset();
//        worldSeed = state.worldSeed();
//        npcSeed = state.npcSeed();
//        accumulatedPlayTimeMs = state.playTimeMs();
//        finalPlayTimeMs = 0L;
//        sessionStartMs = System.currentTimeMillis();
//        enemiesFelled = state.enemiesFelled();
//        totalDamageTaken = state.damageTaken();
//        totalDamageGiven = state.damageGiven();
//        World generator = new World(worldSeed);
//        world = generator.generate();
//        decayingLightRadius = state.decayingLightRadius();
//        lastDecayTime = state.lastDecayTime();
//        lightSurgeStartMs = state.lightSurgeStartMs();
//        renderer.setLightRadius(decayingLightRadius);
//
//        avatar = buildAvatar(state.avatar());
//        initializeAvatarAnimations(directionFromChar(lastFacing));
//        drawX = avatar.posX() - 0.5;
//        drawY = avatar.posY() - 0.5;
//
//        inventory = rebuildInventory(state.inventory());
//        droppedItems = rebuildDroppedItems(state.droppedItems());
//
//        npcManager = new NpcManager(new Random(npcSeed), combatService, atlas);
//        npcManager.setDeathHandler(this::handleNpcDeath);
//        npcManager.restoreState(rebuildNpcs(state.npcs()), rebuildCorpses(state.corpses()));
//    }
//
//    private Avatar buildAvatar(SaveState.AvatarState avatarState) {
//        SaveState.HealthState healthState = avatarState.health();
//        HealthComponent health = new HealthComponent(healthState.current(), healthState.max(),
//                healthState.armor(), healthState.invulnerabilityFrames());
//        health.setInvulnerabilityRemaining(healthState.invulnerabilityRemaining());
//        health.addDeathCallback(this::handleAvatarDeath);
//        Avatar built = new Avatar(avatarState.x(), avatarState.y(), avatarState.lives(), health);
//        built.setSpawnPoint(new Entity.Position(avatarState.spawnX(), avatarState.spawnY()));
//        built.setStagger(avatarState.staggerMs());
//        combatService.register(built);
//        lastFacing = directionToChar(Direction.DOWN);
//        return built;
//    }
//
//    private Inventory rebuildInventory(SaveState.InventoryState inventoryState) {
//        Inventory rebuilt = new Inventory(inventoryState.slots());
//        for (SaveState.ItemStackState stack : inventoryState.stacks()) {
//            Item item = ItemRegistry.byId(stack.itemId());
//            if (item != null) {
//                rebuilt.add(item, stack.quantity());
//            }
//        }
//        return rebuilt;
//    }
//
//    private List<DroppedItem> rebuildDroppedItems(List<SaveState.DroppedItemState> states) {
//        List<DroppedItem> drops = new ArrayList<>();
//        for (SaveState.DroppedItemState drop : states) {
//            Item item = ItemRegistry.byId(drop.itemId());
//            if (item != null) {
//                drops.add(new DroppedItem(item, drop.quantity(), drop.x(), drop.y()));
//            }
//        }
//        return drops;
//    }
//
//    private List<Npc> rebuildNpcs(List<SaveState.NpcState> states) {
//        List<Npc> npcs = new ArrayList<>();
//        for (SaveState.NpcState state : states) {
//            SaveState.HealthState healthState = state.health();
//            HealthComponent health = new HealthComponent(healthState.current(), healthState.max(),
//                    healthState.armor(), healthState.invulnerabilityFrames());
//            health.setInvulnerabilityRemaining(healthState.invulnerabilityRemaining());
//
//            // Create animation controller with all animation types
//            com.untitledgame.animation.AnimationController animationController =
//                    com.untitledgame.animation.AnimationFactory.createNpcController(atlas, state.variant());
//
//            Npc npc = new Npc(state.x(), state.y(), new Random(state.rngSeed()), state.rngSeed(), state.variant(),
//                    animationController, health);
//            npc.setDrawX(state.drawX());
//            npc.setDrawY(state.drawY());
//            npc.setStagger(state.staggerMs());
//            npcs.add(npc);
//        }
//        return npcs;
//    }

    private List<com.untitledgame.logic.npc.Corpse> rebuildCorpses(List<SaveState.CorpseState> states) {
        List<com.untitledgame.logic.npc.Corpse> corpses = new ArrayList<>();
        for (SaveState.CorpseState state : states) {
            corpses.add(new com.untitledgame.logic.npc.Corpse(state.x(), state.y(), Tileset.NPC_CORPSE));
        }
        return corpses;
    }




    // seed parser for Menu
    private long parseSeed(String seedDigits) {
        try {
            return Long.parseLong(seedDigits);
        } catch (NumberFormatException e) {
            return 0L; // returns 0 in Long form
        }
    }

    //Avatar now uses smoothing - placement happens instantly but movement is based on frames
    private Renderer.AvatarDraw buildAvatarDraw() {
        if (avatar == null || avatarSprite == null) {
            return null;
        }
        // When movement stops, snap to the target tile to avoid post-input sliding.
        drawX = avatar.posX() - 0.5;
        drawY = avatar.posY() - 0.5;
        double avatarScale = 3.0;   // adjust this number if starts to look laggy
        return new Renderer.AvatarDraw(drawX, drawY, avatarScale, avatarSprite);
    }


    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis) / MS_PER_S;
        long minutes = totalSeconds / SEC_PER_MIN;
        long seconds = totalSeconds % SEC_PER_MIN;
        return String.format("%d:%02d", minutes, seconds);
    }

    private enum AvatarAction {
        IDLE,
        WALK,
        RUN,
        TAKE_DAMAGE,
        ATTACK,
        DEATH,
        BLOCK
    }

    @Override
    public void show() {
        installInputProcessor();
        Gdx.input.setCursorCatched(true);
        phase = EnginePhase.MENU;
        menuMusicStarted = false;
        gameplayMusicStarted = false;
    }

    @Override
    public void render(float delta) {
        if (!assetsReady) {
            if (assets.update()) {
                onAssetsLoaded();
            } else {
                renderLoadingScreen();
                return;
            }
        }
        update(delta);
        renderFrame();
    }

    private void update(double deltaSeconds) {
        if (gameState == GameState.PAUSED) {
            return;
        }

        if (phase == EnginePhase.MENU) {
            updateMenu();
            return;
        }

        if (phase == EnginePhase.SEED_ENTRY) {
            updateSeedEntry();
            return;
        }

        updateGameplay(deltaSeconds);
    }

    private void updateMenu() {
        if (!menuMusicStarted) {
            music.playThenCallback("audio/cavegame.wav", () -> music.playLoop("audio/main_menu.wav"));
            menuMusicStarted = true;
        }
        while (hasNextKeyTyped()) {
            char c = Character.toLowerCase(nextKeyTyped());
            if (c == 'q') {
                exitGame();
                return;
            }
            if (c == 'l') {
                boolean loaded = loadGame();
                if (!loaded || world == null) {
                    beginSeedEntry();
                } else {
                    beginGameplay();
                }
                return;
            }
            if (c == 'n') {
                beginSeedEntry();
                return;
            }
        }
    }

    private void updateSeedEntry() {
        while (hasNextKeyTyped()) {
            char c = nextKeyTyped();
            if (c == 'S' || c == 's') {
                history.append('n').append(seedBuilder).append('s');
                startNewWorld(parseSeed(seedBuilder.toString()));
                beginGameplay();
                return;
            }
            if (Character.isDigit(c)) {
                seedBuilder.append(c);
            }
            if (c == 'q' || c == 'Q') {
                exitGame();
                return;
            }
        }
    }

    private void updateGameplay(double deltaSeconds) {
        if (avatar != null) {
            avatar.tickStagger(deltaSeconds);
            if (avatar.isStaggered()) {
                attackInProgress = false;
                attackQueued = false;
                dashInProgress = false;
                dashDistanceRemaining = 0.0;
                avatar.setVelocity(0.0, 0.0);
            }
        }
        boolean avatarMoved = handleMovementRealtime(true, deltaSeconds);

        tickAccumulatorMs += deltaSeconds * MS_PER_S;
        while (tickAccumulatorMs >= TICK_MS) {
            tickAccumulatorMs -= TICK_MS;
            updateTick();
        }

        updateNpcMovement(deltaSeconds);
        boolean avatarMoving = avatar != null
                && (Math.abs(avatar.velocityX()) > COLLISION_EPSILON || Math.abs(avatar.velocityY()) > COLLISION_EPSILON);
        tickAvatarAnimation(deltaSeconds, avatarMoved || avatarMoving);
    }

    private void updateTick() {
        switch (gameState) {
            case DYING, DEAD -> {
                updateHudMessage();
                runDeathSequence();
                return;
            }
            case ENDING, ENDED -> {
                updateHudMessage();
                runEndSequence();
                return;
            }
            default -> { }
        }
        updateHudMessage();
        while (hasNextKeyTyped()) {
            char raw = nextKeyTyped();
            char c = Character.toLowerCase(raw);
            if (awaitingQuitCommand) {
                awaitingQuitCommand = false;
                if (c == 'q') {
                    saveGameState();
                    exitGame();
                    return;
                }
                continue;
            }
            if (processCommand(c, true, true)) {
                return;
            }
        }
        updateInventoryToggle();
        updateTargetingToggle();
        if (targetingEnabled) {
            updateCurrentTarget();
        }
        if (npcManager != null && avatar != null) {
            npcManager.tick(world, avatar);
        }
        combatService.tick();
        checkForEndgame();
        updateLightDecay();
    }

    private void renderFrame() {
        if (phase == EnginePhase.MENU) {
            showMainMenu();
            return;
        }
        if (phase == EnginePhase.SEED_ENTRY) {
            renderSeedPrompt();
            return;
        }
        renderWithHud();
        if (gameState == GameState.PAUSED) {
            drawPauseOverlay();
        }
    }
    private void drawPauseOverlay() {
        drawOverlayRect();
        if (screenOverlay == null) {
            return;
        }
        screenOverlay.renderCentered("Paused", new String[]{"Press ESC to Resume"});

    }

    private void beginSeedEntry() {
        seedBuilder.setLength(0);
        phase = EnginePhase.SEED_ENTRY;
    }

    private void beginGameplay() {
        phase = EnginePhase.PLAYING;
        tickAccumulatorMs = 0.0;
        if (!gameplayMusicStarted) {
            music.stop();
            music.playLoop("audio/loop_dropper.wav");
            gameplayMusicStarted = true;
        }
    }

    private void exitGame() {
        if (Gdx.app != null) {
            Gdx.app.exit();
            return;
        }
        dispose();
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
        if (hudUi != null) {
            hudUi.resize(width, height);
        }
        if (screenOverlay != null) {
            screenOverlay.resize(width, height);
        }
        if (inventoryOverlay != null) {
            inventoryOverlay.resize(width, height);
        }
    }

    @Override
    public void pause() {
        // no-op
    }

    @Override
    public void resume() {
        installInputProcessor();
    }

    @Override
    public void hide() {
        // no-op
    }

    @Override
    public void dispose() {
        music.stop();

        if (hudUi != null) {
            hudUi.dispose();
            hudUi = null;
        }

        if (uiAssets != null) {
            uiAssets.dispose();
            uiAssets = null;
        }

        disposeHudAssets();
        renderer.dispose();
    }
}
