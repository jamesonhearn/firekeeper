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
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.untitledgame.animation.AnimationController;
import com.untitledgame.animation.AnimationFactory;
import com.untitledgame.assets.*;
import com.untitledgame.logic.*;
import com.untitledgame.ui.HudUi;
import com.untitledgame.ui.ScreenOverlay;
import com.untitledgame.ui.InventoryOverlay;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.untitledgame.ui.UiFont;
import com.untitledgame.utils.FileUtils;

import com.untitledgame.logic.items.DroppedItem;
import com.untitledgame.logic.items.Inventory;
import com.untitledgame.logic.items.Item;
import com.untitledgame.logic.items.ItemRegistry;
import com.untitledgame.logic.items.ItemStack;
import com.untitledgame.logic.npc.Npc;
import com.untitledgame.logic.npc.NpcManager;
import com.untitledgame.assets.DirectionMode;


public class Engine implements Screen {
    public static final double INVENTORY_ROW_SPACING = 1.5;
    public static final int HEALTH_POTION_MESSAGE_MS = 2000;
    public static final int PLAYER_HEALTH = 50;
    public static final int INVULNERABILITY_FRAMES = 15;
    public static final int ITEM_DROP_RETRIES = 400;
    public static final int LIGHT_SURGE_MESSAGE_MS = 3000;
    public static final double RNG_20_PERCENT = 0.8;
    public static final double RNG_30_PERCENT = 0.7;
    public static final double RNG_95_PERCENT = 0.30;
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
    private int currentLevel;
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
    private final Vector2 dashDirection = new Vector2();

    private UiAssets uiAssets;
    private static final String HB_FULL = "ui/healthbar_full.png";
    private static final String HB_75   = "ui/healthbar_75.png";
    private static final String HB_50   = "ui/healthbar_50.png";
    private static final String HB_25   = "ui/healthbar_25.png";
    private static final String HB_ZERO = "ui/healthbar_empty.png";
    private static final int TICK_MS = 40; // create ticks to create consistent movements
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

    private static final String[] PLAYER_DASH_SOUNDS = new String[]{
            "audio/PlayerDash.wav",
            "audio/PlayerDash2.wav"
    };
    private static final String[] KNIGHT_ATTACK_SOUNDS = new String[]{
            "audio/knightattack1.wav",
            "audio/knightattack2.wav",
            "audio/knightattack3.wav"
    };
    private static final String[] MAGE_ATTACK_SOUNDS = new String[]{
            "audio/mageattack1.wav",
            "audio/mageattack2.wav",
            "audio/mageattack3.wav",
            "audio/mageattack4.wav",
            "audio/mageattack5.wav"
    };
    private static final String ENEMY_DODGE_SOUND = "audio/EnemyDodge.wav";
    private static final String DRINK_SOUND = "audio/DrinkSound.wav";

    // Sound effect volume and pitch modifiers
    private static final float KICK_VOLUME_MULTIPLIER = 1.3f; // 30% louder
    private static final float KICK_PITCH_MULTIPLIER = 0.8f;  // 20% slower

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

    private static final double NPC_WALK_SPEED = 2.5;
    private static final double AVATAR_WALK_SPEED = 5.0;
    private static final double AVATAR_DASH_DISTANCE = 8.0;
    private static final double AVATAR_DASH_DURATION_SECONDS = 0.3;
    private static final double AVATAR_DASH_SPEED = AVATAR_DASH_DISTANCE / AVATAR_DASH_DURATION_SECONDS;
    private static final double MELEE_HALF_WIDTH = 1;
    private static final double MELEE_REACH = 1;
    private static final double COLLISION_EPSILON = 1e-4;
    private static final double VELOCITY_EPSILON = 1e-6;  // Threshold for detecting avatar movement


    //Animation variables
    private static final int AVATAR_WALK_TICKS = Math.max(1, (int) Math.round(40.0 / TICK_MS));
    private static final int AVATAR_RUN_TICKS = Math.max(1, AVATAR_WALK_TICKS - 1);
    private static final int AVATAR_ATTACK_TICKS = Math.max(1, (int) Math.round(60.0 / TICK_MS));
    private static final int AVATAR_ATTACK_DAMAGE = 1;
    private static final int AVATAR_DEATH_TICKS = Math.max(1, (int) Math.round(80.0 / TICK_MS));
    private static final int AVATAR_BLOCK_TICKS = Math.max(1, (int) Math.round(60.0 / TICK_MS));
    private static final double PARRY_WINDOW_MS = 200.0; // 300ms window to parry after activation

    // Pause after death animation completes before showing death screen
    private static final int DEATH_PAUSE_TICKS = Math.max(1, (int) Math.round(500.0 / TICK_MS)); // 500ms pause

    private char lastFacing = 's';
    private boolean attackDown = false;
    private final Set<Entity> damagedEntitiesThisAttack = new HashSet<>();
    private final ArrayDeque<Character> typedKeys = new ArrayDeque<>();
    private final InputState inputState = new InputState();

    // Parry system
    private boolean parryDown = false;
    private boolean prevParryDown = false;

    // Death animation pause tracking
    private int deathPauseTicks = 0;

    // Kick system
    private boolean kickDown = false;
    private boolean prevKickDown = false;
    private final Set<Entity> kickedEntitiesThisKick = new HashSet<>();

    // Mouse tracking
    private float mouseWorldX = 0f;
    private float mouseWorldY = 0f;
    private final Vector3 mouseTempVec = new Vector3();

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

    private enum EnginePhase { MENU, PLAYING }


    // Added smoothing to animations
    private double drawX = 0, drawY = 0;
    private double tickAccumulatorMs = 0.0;
    private EnginePhase phase = EnginePhase.MENU;
    private boolean menuMusicStarted = false;
    private boolean gameplayMusicStarted = false;
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
    private com.badlogic.gdx.graphics.Cursor customCursor;
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
        currentLevel = 1;
        history = new StringBuilder();
        npcManager = null;
        npcSeed = 0L;
        combatService = new CombatService();
        combatService.setDamageListener(this::recordDamageStats);
        combatService.setParryChecker(this::isEntityParrying);
        combatService.setDodgeChecker(this::shouldEntityDodge);
        combatService.setKickCounterChecker(this::shouldEntityKickCounter);
        currentDirection = 0;
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
        kickDown = false;
        prevKickDown = false;
        dashDirection.set(0f, 0f);
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
        kickedEntitiesThisKick.clear();

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
        for (String sound : PLAYER_DASH_SOUNDS) {
            assets.load(sound, Sound.class);
        }
        for (String sound : KNIGHT_ATTACK_SOUNDS) {
            assets.load(sound, Sound.class);
        }
        for (String sound : MAGE_ATTACK_SOUNDS) {
            assets.load(sound, Sound.class);
        }
        assets.load(ENEMY_DODGE_SOUND, Sound.class);
        assets.load(DRINK_SOUND, Sound.class);
        for (String track : MUSIC_TRACKS) {
            assets.load(track, Music.class);
        }
        music.loadEffects(STEP_SOUNDS);
        assetsQueued = true;
    }

    private List<SpriteSheetConfig> createSpriteSheetConfigs() {
        List<SpriteSheetConfig> configs = new ArrayList<>();



        // New 8-directional sprite sheets for player (64x64 frames, 15 frames, 8 rows)
        AnimationSetConfig playerConfig = new AnimationSetConfig("player", "avatars/player", 54, 53, DirectionMode.FOUR_DIRECTIONAL);
        playerConfig.addAnimation("idle", "idle.png", 1);
        playerConfig.addAnimation("walk", "walk.png", 8);
        playerConfig.addAnimation("melee_basic", "attack.png", 6);
        //playerConfig.addAnimation("melee_secondary", "Melee2.png", 15);
        //playerConfig.addAnimation("melee_run", "MeleeRun.png", 15);
        //playerConfig.addAnimation("melee_spin", "MeleeSpin.png", 15);
        //playerConfig.addAnimation("block_end", "ShieldBlockMid.png", 15);
        //playerConfig.addAnimation("block_start", "ShieldBlockStart.png", 15);
        //playerConfig.addAnimation("kick", "Kick.png", 15);
        playerConfig.addAnimation("dash", "dash.png", 6);
        //playerConfig.addAnimation("take_damage", "TakeDamage.png", 15);
        playerConfig.addAnimation("death", "death.png", 18);
        configs.addAll(playerConfig.createSpriteSheetConfigs());

        // New 8-directional sprite sheets for NPC (64x64 frames, 15 frames, 8 rows)
        AnimationSetConfig npcConfig = new AnimationSetConfig("npc", "avatars/NPC", 50, 31, DirectionMode.FOUR_DIRECTIONAL);
        //npcConfig.addAnimation("idle", "Idle.png", 15);
        npcConfig.addAnimation("walk", "walk.png", 6);
        npcConfig.addAnimation("ATTACK", "attack.png", 10);
        //npcConfig.addAnimation("ATTACK", "Attack2.png", 15);
        //npcConfig.addAnimation("rolling", "Rolling.png", 15);
        //npcConfig.addAnimation("slide", "Slide.png", 15);
        //npcConfig.addAnimation("kick", "Kick.png", 15);
        npcConfig.addAnimation("take_damage", "take_damage.png", 2);
        npcConfig.addAnimation("death", "death.png", 8);
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
        screenOverlay.renderCentered("FIREKEEPER", new String[]{"N - New Game", "L - Load", "Q - Quit"});
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
                new String[]{"N - New Game", "L - Load", "Q - Quit"});
    }

    private void drawEndOverlay() {
        if (screenOverlay == null) {
            return;
        }
        screenOverlay.renderCentered("Level " + currentLevel + " Complete!",
                new String[]{
                        "Time: " + formatDuration(finalPlayTimeMs),
                        "Enemies felled: " + enemiesFelled,
                        "Damage taken: " + totalDamageTaken,
                        "N: Next Level",
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

        //renderer.setWorldScale(1.0f);

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
        // Play drink sound
        music.playEffect(DRINK_SOUND);

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
                setHudMessage("Enemy lock enabled", 2000);
                updateCurrentTarget();
            } else {
                setHudMessage("Enemy lock disabled", 2000);
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
        boolean kick = kickDown;
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
        if (kick && !prevKickDown) {
            startKick(actionFacing);
        }
        boolean movedThisFrame;
        if (avatar.isDashing()) {
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
        prevKickDown = kick;
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
        if (command == 'e') {
            pickupAtAvatar();
            return false;
        }
        if (command == 'r') {
            useHealthPotion();
            return false;
        }
//        if (command == 'f') {
//            lightToggle = !lightToggle;
//            return false;
//        }
        if (command == 'w' || command == 'a' || command == 's' || command == 'd') {
            return false;
        }

        applyCommands(String.valueOf(command), record, allowQuit);
        return false;
    }


    // Generator func via seed - drop player
    private void startNewWorld(long seed) {
        worldSeed = seed;
        currentLevel = 1;
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
        npcManager.setAttackSoundCallback(() -> music.playRandomEffect(MAGE_ATTACK_SOUNDS));
        npcManager.spawn(world, avatar.x(), avatar.y());
        // give initial items and random spawn ground loot
        seedInitialInventory();
        seedDroppedItems(new Random(seed));
    }


    // Start a new game with random seed
    private void startNewGame() {
        long randomSeed = new Random().nextLong();
        startNewWorld(randomSeed);
        beginGameplay();
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

                    // Create animation controller for player using AnimationFactory
                    //AnimationController avatarAnimationController = AnimationFactory.createPlayerController(atlas);
                    AnimationController avatarAnimationController = AnimationFactory.createPlayerController(atlas, DirectionMode.THREE_DIRECTIONAL_MIRRORED);

                    avatar = new Avatar(x, y, 3, avatarHealth, avatarAnimationController);
                    avatar.setSpawnPoint(new Entity.Position(x, y));
                    combatService.register(avatar);
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
        if (avatar == null || avatar.isParryInProgress() || avatar.isAttacking()) {
            return;
        }
        Direction resolvedFacing = resolveFacingForAction(facing);
        avatar.startAttack(resolvedFacing);

        damagedEntitiesThisAttack.clear();

        music.playRandomEffect(KNIGHT_ATTACK_SOUNDS);
    }



    private void startParry(Direction facing) {
        if (avatar == null || avatar.isParryInProgress() || avatar.isAttacking() || avatar.isStaggered()) {
            return;
        }
        Direction resolvedFacing = resolveFacingForAction(facing);
        avatar.startParry(resolvedFacing);
    }


    private void startKick(Direction facing) {
        if (avatar == null || avatar.isKicking() || avatar.isParryInProgress() || avatar.isAttacking() || avatar.isStaggered()) {
            return;
        }
        Direction resolvedFacing = resolveFacingForAction(facing);
        avatar.startKick(resolvedFacing);

        kickedEntitiesThisKick.clear();

        // Use the same attack sounds but with modified volume/pitch
        float kickVolume = music.getSoundVolume() * KICK_VOLUME_MULTIPLIER;
        music.playRandomEffect(KNIGHT_ATTACK_SOUNDS, kickVolume, KICK_PITCH_MULTIPLIER);
    }

    private void startDash(Direction preferredFacing) {
        if (avatar == null || avatar.isStaggered()) {
            return;
        }
        Direction dashFacing = preferredFacing != null ? preferredFacing : resolveFacingForAction(null);
        if (avatar != null && preferredFacing != null) {
            avatar.setFacing(dashFacing);
        }
        dashDirection.set(facingVector(dashFacing)).nor();
        avatar.startDash(dashFacing);
        currentDirection = directionToChar(dashFacing);
        // Play random dash sound
        music.playRandomEffect(PLAYER_DASH_SOUNDS);
    }

    private boolean updateDashMovement(double deltaSeconds) {
        if (avatar == null || avatar.isAttacking() || avatar.isStaggered() || !avatar.isDashing()) {
            return false;
        }
        double startX = avatar.posX();
        double startY = avatar.posY();
        double stepSeconds = Math.min(deltaSeconds, avatar.getDashDistanceRemaining() / AVATAR_DASH_SPEED);
        avatar.setVelocity(dashDirection.x * AVATAR_DASH_SPEED, dashDirection.y * AVATAR_DASH_SPEED);
        integrateAvatarMotion(stepSeconds);
        double moved = Math.hypot(avatar.posX() - startX, avatar.posY() - startY);
        avatar.tickDash(moved);
        boolean finished = avatar.getDashDistanceRemaining() <= COLLISION_EPSILON
                || (stepSeconds > 0.0 && moved < COLLISION_EPSILON);
        if (finished) {
            avatar.endDash();
            avatar.setVelocity(0.0, 0.0);
        }
        return moved > 0.0;
    }

    private Direction resolveFacingForAction(Direction movementFacing) {
        // Priority 1: If enemy targeting is enabled, lock onto enemy
        if (targetingEnabled) {
            Direction targetFacing = getTargetFacing();
            if (targetFacing != null) {
                if (avatar != null) {
                    avatar.setFacing(targetFacing);
                }
                return targetFacing;
            }
        }
        // Priority 2: Always track mouse position when not locked to enemy
        Direction mouseFacing = getMouseFacing();
        if (mouseFacing != null) {
            if (avatar != null) {
                avatar.setFacing(mouseFacing);
            }
            return mouseFacing;
        }
        // Priority 3: Use movement direction as fallback
        if (movementFacing != null) {
            if (avatar != null) {
                avatar.setFacing(movementFacing);
            }
            return movementFacing;
        }
        return avatar != null ? avatar.facing() : Direction.DOWN;
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
        if (target instanceof Avatar avatarTarget) {
            return avatarTarget.isParrying();
        }
        return false;
    }


    private boolean shouldEntityDodge(Entity target, Entity source) {
        // Only NPCs can dodge
        if (!(target instanceof Npc npc)) {
            return false;
        }

        // Don't dodge if already dodging or dead
        if (npc.isDodging() || npc.health() == null || npc.health().isDepleted()) {
            return false;
        }

        // Probabilistic dodge check - access package-private constant directly
        if (npc.rng().nextDouble() >= Npc.DODGE_PROBABILITY) {
            return false;
        }

        // Calculate dodge direction (away from source, not toward player)
        Direction dodgeDir = calculateDodgeDirection(npc, source);
        npc.triggerDodge(dodgeDir);

        // Play enemy dodge sound
        music.playEffect(ENEMY_DODGE_SOUND);

        return true;
    }

    private boolean shouldEntityKickCounter(Entity target, Entity source) {
        // Only NPCs can kick counter, and only when attacked by the Avatar
        if (!(target instanceof Npc npc) || !(source instanceof Avatar)) {
            return false;
        }

        // Don't kick if already kicking, dodging, dead, or staggered
        if (npc.isKicking() || npc.isDodging() || npc.isStaggered()
                || npc.health() == null || npc.health().isDepleted()) {
            return false;
        }

        // Probabilistic kick counter check
        if (npc.rng().nextDouble() >= Npc.KICK_COUNTER_PROBABILITY) {
            return false;
        }

        // Calculate kick direction (toward the attacker/player)
        Direction kickDir = calculateKickDirection(npc, source);
        npc.triggerKick(kickDir);

        // Play random mage attack sound with increased volume and slower pitch
        float kickVolume = music.getSoundVolume() * KICK_VOLUME_MULTIPLIER;
        music.playRandomEffect(MAGE_ATTACK_SOUNDS, kickVolume, KICK_PITCH_MULTIPLIER);

        return true;
    }

    private static final double POSITION_EPSILON = 0.01; // Threshold for position comparison

    private Direction calculateKickDirection(Npc npc, Entity source) {
        if (source == null) {
            // If no source, kick in current facing direction
            return npc.facing();
        }

        // Calculate vector toward source (opposite of dodge)
        double dx = source.posX() - npc.posX();
        double dy = source.posY() - npc.posY();

        // If source is exactly on top of NPC, use current facing
        if (Math.abs(dx) < POSITION_EPSILON && Math.abs(dy) < POSITION_EPSILON) {
            return npc.facing();
        }

        // Pick primary direction toward source
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
    }

    private Direction opposite(Direction d) {
        return switch (d) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
            case UP_LEFT -> Direction.DOWN_RIGHT;
            case UP_RIGHT -> Direction.DOWN_LEFT;
            case DOWN_LEFT -> Direction.UP_RIGHT;
            case DOWN_RIGHT -> Direction.UP_LEFT;
        };
    }
    private Direction calculateDodgeDirection(Npc npc, Entity source) {
        Direction toward;

        if (source == null) {
            Direction[] all = Direction.values();
            return all[npc.rng().nextInt(all.length)];
        }

        double dx = source.posX() - npc.posX();
        double dy = source.posY() - npc.posY();

        if (Math.abs(dx) < POSITION_EPSILON && Math.abs(dy) < POSITION_EPSILON) {
            Direction[] all = Direction.values();
            return all[npc.rng().nextInt(all.length)];
        }

        toward = Direction.fromVelocity(dx, dy);
        Direction forbidden = opposite(toward);

        // Collect allowed directions
        Direction[] all = Direction.values();
        Direction[] allowed = new Direction[all.length - 1];
        int idx = 0;

        for (Direction d : all) {
            if (d != forbidden) {
                allowed[idx++] = d;
            }
        }

        return allowed[npc.rng().nextInt(idx)];
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
            if (npc.updateKnockback(deltaSeconds)) {
                integrateEntityMotion(npc, deltaSeconds, others);
                continue;
            }
            Vector2 v = facingVector(npc.facing());
            npc.setVelocity(v.x * NPC_WALK_SPEED, v.y * NPC_WALK_SPEED);
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
                    // Sweeping along X then check Y span
                    int minY = (int) Math.floor(posY - half);
                    int maxY = (int) Math.floor(posY + half);
                    for (int y = minY; y <= maxY; y++) {
                        if (isSolidTile(t, y)) {
                            return (t - half) - COLLISION_EPSILON;
                        }
                    }
                } else {
                    // Sweeping along Y then check X span
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
                    // Sweeping along X then check Y span
                    int minY = (int) Math.floor(posY - half);
                    int maxY = (int) Math.floor(posY + half);
                    for (int y = minY; y <= maxY; y++) {
                        if (isSolidTile(t, y)) {
                            return (t + 1 + half) + COLLISION_EPSILON;                        }
                    }
                } else {
                    // Sweeping along Y then check X span
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
        avatar.endAttack();
        attackDown = false;
        clampLightToDeathRadius();
        deathPauseTicks = 0; // Reset the pause counter
        // Update animation to DEATH - the Avatar's updateAnimation will handle this based on health state
        avatar.updateAnimation(0f);
        avatarSprite = avatar.currentFrame();
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
        if (gameState == GameState.DYING && avatar == null) {
            gameState = GameState.DEAD;
        }
        if (gameState == GameState.DYING && avatar.isAnimationFinished()) {
            // Start the pause counter after animation finishes
            deathPauseTicks++;
            if (deathPauseTicks >= DEATH_PAUSE_TICKS) {
                gameState = GameState.DEAD;
                deathPauseTicks = 0; // Reset for next time
            }
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
                startNextLevel();
                return;
            }
        }
    }

    private void startNewGameFromDeath() {
        reset();
        startNewGame();
    }

    private void startNextLevel() {
        // Preserve state across levels
        Inventory previousInventory = inventory;
        int previousLevel = currentLevel;
        int previousEnemiesFelled = enemiesFelled;
        int previousDamageTaken = totalDamageTaken;
        int previousDamageGiven = totalDamageGiven;
        long previousPlayTime = finalPlayTimeMs;

        // Generate next level with derived seed based on current level
        currentLevel = previousLevel + 1;
        // Use XOR with level number for better randomization between levels
        worldSeed = worldSeed ^ (currentLevel * 0x9e3779b97f4a7c15L);

        // Clear level-specific state but preserve player progress
        world = null;
        avatar = null;
        npcManager = null;
        droppedItems = new ArrayList<>();
        gameState = GameState.PLAYING;
        endFadeStartRadius = BASE_LIGHT_RADIUS;
        endFadeStartMs = -1L;

        // Restore preserved state
        inventory = previousInventory;
        enemiesFelled = previousEnemiesFelled;
        totalDamageTaken = previousDamageTaken;
        totalDamageGiven = previousDamageGiven;
        accumulatedPlayTimeMs = previousPlayTime;
        sessionStartMs = System.currentTimeMillis();

        // Generate new world
        World generator = new World(worldSeed);
        world = generator.generate();
        resetLighting();
        decayingLightRadius = MAX_LIGHT_RADIUS;
        lastDecayTime = System.currentTimeMillis();
        renderer.setLightRadius(decayingLightRadius);
        placeAvatar();

        // Spawn NPCs for new level
        npcSeed = worldSeed ^ NPC_SEED_SALT;
        npcManager = new NpcManager(new Random(npcSeed), combatService, atlas);
        npcManager.setDeathHandler(this::handleNpcDeath);
        npcManager.setAttackSoundCallback(() -> music.playRandomEffect(MAGE_ATTACK_SOUNDS));
        npcManager.spawn(world, avatar.x(), avatar.y());

        // Seed new dropped items
        seedDroppedItems(new Random(worldSeed));

        beginGameplay();
    }


    private class InputState implements InputProcessor {
        @Override
        public boolean keyDown(int keycode) {
            // ESC toggles pause
            if (keycode == Input.Keys.ESCAPE) {
                togglePause();
                return true;
            }

            // Q only works while paused
            if (keycode == Input.Keys.Q && gameState == GameState.PAUSED) {
                exitGame();
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
            } else if (keycode == Input.Keys.F) {
                kickDown = true;
            } else if (keycode == Input.Keys.SHIFT_LEFT) {
                shiftDown = true;
            } else if (keycode == Input.Keys.V) {
                tabDown = true;
            } else if (keycode == Input.Keys.CONTROL_LEFT) {
                tKeyDown = true;
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
                // Dampen background music to 30% volume
                music.dampenMusic(0.3f);
            }
            else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;

                // Restore background music to normal volume
                music.restoreMusicVolume();
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
            } else if (keycode == Input.Keys.F) {
                kickDown = false;
            } else if (keycode == Input.Keys.V) {
                tabDown = false;
            } else if (keycode == Input.Keys.SHIFT_LEFT) {
                shiftDown = false;
            } else if (keycode == Input.Keys.CONTROL_LEFT) {
                tKeyDown = false;
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
            updateMouseWorldPosition(screenX, screenY);
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
            updateMouseWorldPosition(screenX, screenY);
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
            startNewGame();
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
        if (avatar == null) {
            return;
        }
        // Tick down parry window in avatar
        avatar.tickParry(deltaSeconds);

        // Check if attack animation finished
        if (avatar.isAttacking() && avatar.isAnimationFinished()) {
            avatar.endAttack();
            damagedEntitiesThisAttack.clear();
        }

        // Check if kick animation finished
        if (avatar.isKicking() && avatar.isAnimationFinished()) {
            avatar.endKick();
            kickedEntitiesThisKick.clear();
        }

        // Calculate facing direction from velocity for smooth 8-directional animation
        Direction facing;
        if (avatar.isAttacking() || avatar.isKicking()) {
            facing = avatar.getAttackFacing();
        } else if (targetingEnabled) {
            // When enemy targeting is enabled, face the target
            Direction targetFacing = getTargetFacing();
            if (targetFacing != null) {
                facing = targetFacing;
                avatar.setFacing(facing);
            } else {
                // No valid target, fall back to mouse tracking
                Direction mouseFacing = getMouseFacing();
                if (mouseFacing != null) {
                    facing = mouseFacing;
                    avatar.setFacing(facing);
                } else {
                    facing = getMovementBasedFacing();
                }
            }
        } else {
            // Default: Track mouse position
            Direction mouseFacing = getMouseFacing();
            if (mouseFacing != null) {
                facing = mouseFacing;
                avatar.setFacing(facing);
            } else {
                // Fall back to movement-based facing if mouse is too close
                facing = getMovementBasedFacing();
            }
        }

        // Update avatar animation using centralized AnimationController
        avatar.updateAnimation((float) deltaSeconds);
        avatarSprite = avatar.currentFrame();

        // Process attack overlaps if attacking
        if (avatar.isAttacking()) {
            processAvatarAttackOverlaps();
        }

        // Process kick overlaps if kicking
        if (avatar.isKicking()) {
            processAvatarKickOverlaps();
        }

        lastFacing = directionToChar(facing);
    }

    private void processAvatarAttackOverlaps() {
        if (!avatar.isAttacking() || npcManager == null || avatar == null || combatService == null) {
            return;
        }

        AttackBounds bounds = buildAttackBounds(avatar.posX(), avatar.posY(), avatar.getAttackFacing(),
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


    private void processAvatarKickOverlaps() {
        if (!avatar.isKicking() || npcManager == null || avatar == null || combatService == null) {
            return;
        }

        AttackBounds bounds = buildAttackBounds(avatar.posX(), avatar.posY(), avatar.getAttackFacing(),
                Avatar.HITBOX_HALF, MELEE_REACH, MELEE_HALF_WIDTH);

        // Kick does limited damage (half of normal attack)
        int kickDamage = AVATAR_ATTACK_DAMAGE / 2;
        if (kickDamage < 1) {
            kickDamage = 1;
        }

        for (Npc npc : npcManager.npcs()) {
            if (kickedEntitiesThisKick.contains(npc)) {
                continue;
            }
            if (overlaps(bounds, npc.posX(), npc.posY(), Npc.HITBOX_HALF)) {
                // Queue damage
                combatService.queueDamage(npc, avatar, kickDamage);

                // Apply knockback
                applyKickKnockback(npc);

                kickedEntitiesThisKick.add(npc);
            }
        }
    }

    private void applyKickKnockback(Entity target) {
        if (target == null || avatar == null) {
            return;
        }

        double dx = target.posX() - avatar.posX();
        double dy = target.posY() - avatar.posY();
        double dist = Math.hypot(dx, dy);

        if (dist < 1e-6) {
            return;
        }

        // Small knockback: 2 tiles over 0.2 seconds
        double distance = 2.0;
        double duration = 0.2;

        target.startKnockback(dx, dy, distance, duration);
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
                currentLevel,
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

    private void updateMouseWorldPosition(int screenX, int screenY) {
        if (renderer == null) {
            return;
        }
        // Use the renderer's screenToWorld which properly handles viewport unprojection
        Vector2 worldPos = renderer.screenToWorld(screenX, screenY);
        mouseWorldX = worldPos.x;
        mouseWorldY = worldPos.y;
    }

    private Direction getMouseFacing() {
        if (avatar == null) {
            return null;
        }
        double dx = mouseWorldX - avatar.posX();
        double dy = mouseWorldY - avatar.posY();

        // Only use mouse facing if mouse is far enough from avatar
        if (Math.hypot(dx, dy) < 0.5) {
            return null;
        }

        return Direction.fromVelocity(dx, dy);
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

    @Override
    public void show() {
        installInputProcessor();
        createAndSetCustomCursor();
        phase = EnginePhase.MENU;
        menuMusicStarted = false;
        gameplayMusicStarted = false;
    }

    private void createAndSetCustomCursor() {
        // Dispose old cursor if exists
        if (customCursor != null) {
            customCursor.dispose();
        }

        // Create a simple dot cursor (5x5 pixels)
        Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f); // White
        pixmap.fillCircle(2, 2, 2);

        customCursor = Gdx.graphics.newCursor(pixmap, 2, 2);
        Gdx.graphics.setCursor(customCursor);
        pixmap.dispose();

        // Make cursor visible
        Gdx.input.setCursorCatched(false);
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

        updateGameplay(deltaSeconds);
    }

    private void updateMenu() {
        if (!menuMusicStarted) {
            music.play("audio/test.mp3");
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
                    startNewGame();
                } else {
                    beginGameplay();
                }
                return;
            }
            if (c == 'n') {
                startNewGame();
                return;
            }
        }
    }

    private void updateGameplay(double deltaSeconds) {
        if (avatar != null) {
            avatar.tickStagger(deltaSeconds);
            if (avatar.updateKnockback(deltaSeconds)) {
                integrateAvatarMotion(deltaSeconds);
                return;
            }
            if (avatar.isStaggered()) {
                // Cancel avatar actions when staggered - handled in Avatar.onStaggered()
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
        renderWithHud();
        if (gameState == GameState.PAUSED) {
            drawPauseOverlay();
        }
    }
    private void drawPauseOverlay() {
        drawOverlayRect();
        if (screenOverlay == null) return;

        screenOverlay.renderCentered(
                "Paused",
                new String[]{"Press ESC to Resume or Q to Quit"}
        );
    }

    private void beginGameplay() {
        phase = EnginePhase.PLAYING;
        tickAccumulatorMs = 0.0;
        if (!gameplayMusicStarted) {
            music.stop();
//            music.playLoop("audio/loop_dropper.wav");
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

        if (customCursor != null) {
            customCursor.dispose();
            customCursor = null;
        }


        if (uiAssets != null) {
            uiAssets.dispose();
            uiAssets = null;
        }

        disposeHudAssets();
        renderer.dispose();
    }
}
