package com.untitledgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.untitledgame.assets.Tileset;
import com.untitledgame.logic.TileType;
import com.untitledgame.logic.items.DroppedItem;
import com.untitledgame.logic.npc.Corpse;
import com.untitledgame.logic.npc.Npc;
import com.untitledgame.logic.npc.NpcManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Renderer responsible for world and HUD drawing using libGDX.
 * Handles tile rendering, lighting effects, and UI overlay.
 */
public class Renderer implements AutoCloseable {
    static final int TILE_SIZE = 24;
    private static final int MAX_LIGHTS = 16;
    private static final float AMBIENT_LIGHT = 0.0f;
    private static final double NPC_SCALE_TILES = 1.0;
    private static final Color AVATAR_LIGHT_COLOR = new Color(1f, 1f, 1f, 1f);
    private static final Color NPC_LIGHT_COLOR = new Color(0.75f, 0.85f, 1f, 1f);
    private static final Color ITEM_LIGHT_COLOR = new Color(1f, 0.9f, 0.6f, 1f);
    private int width;
    private int height;
    private int xOffset;
    private int yOffset;
    private int viewWidth;
    private int viewHeight;
    private int hudHeight;
    private int worldWidth;
    private int worldHeight;
    private int viewOriginX;
    private int viewOriginY;
    private static final double CAMERA_SMOOTH = 0.20;
    private static final double SMOOTH_SPEED = 0.10;
    private static final float DEFAULT_FALLOFF = 3.0f;
    private static final String LIGHT_VERTEX_SHADER =
            "attribute vec4 a_position;\n" +
                    "attribute vec4 a_color;\n" +
                    "attribute vec2 a_texCoord0;\n" +
                    "uniform mat4 u_projTrans;\n" +
                    "varying vec4 v_color;\n" +
                    "varying vec2 v_texCoords;\n" +
                    "void main(){\n" +
                    "   v_color = a_color;\n" +
                    "   v_color.a = v_color.a * (255.0/254.0);\n" +
                    "   v_texCoords = a_texCoord0;\n" +
                    "   gl_Position =  u_projTrans * a_position;\n" +
                    "}";

    public static final class RenderContext {
        final int startX;
        final int endX;
        final int startY;
        final int endY;
        final LightBounds litBounds;

        RenderContext(int startX, int endX, int startY, int endY, LightBounds litBounds) {
            this.startX = startX;
            this.endX = endX;
            this.startY = startY;
            this.endY = endY;
            this.litBounds = litBounds;
        }

        boolean contains(int x, int y) {
            return x >= startX && x < endX && y >= startY && y < endY;
        }

        boolean withinLightWindow(int x, int y) {
            return x >= litBounds.startX && x < litBounds.endX
                    && y >= litBounds.startY && y < litBounds.endY;
        }
    }

    public record AvatarDraw(double x, double y, double scale, TextureRegion sprite) { }
    private enum DrawLayer {
        BACKGROUND(0),
        ITEM(1),
        ENTITY(2),
        COVER(3);

        final int order;

        DrawLayer(int order) {
            this.order = order;
        }
    }

    private record RenderOp(double sortY, DrawLayer layer, double sortX, Runnable action) { }
    private record LightSource(double x, double y, float radius, float intensity, Color color) { }
    private record ShadowInterval(float start, float end) { }
    private record LightBounds(int startX, int endX, int startY, int endY) { }

    private double camTileX;
    private double camTileY;
    private int avatarX = -1;
    private int avatarY = -1;
    private double lightRadius = 50;

    private ShaderProgram lightShader;
    private Texture lightMaskTexture;
    private Pixmap occlusionPixmap;
    private Texture occlusionTexture;
    private boolean occlusionDirty = true;
    private boolean uniformsDirty = true;
    private int lastOcclusionOriginX = -1;
    private int lastOcclusionOriginY = -1;
    private int lastOcclusionWorldHash = 0;
    private int lastOcclusionLightHash = 0;
    private int lastOcclusionLightCount = 0;
    private int lastUniformOriginX = -1;
    private int lastUniformOriginY = -1;
    private int lastUniformViewWidth = -1;
    private int lastUniformViewHeight = -1;
    private int lastLightHash = 0;
    private int lastLightCount = 0;
    private final List<LightSource> activeLights = new ArrayList<>();

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private TextureAtlas textureAtlas;
    private final Vector2 scratchVector = new Vector2();

    public double getLightRadius() {
        return lightRadius;
    }

    public void setLightRadius(double r) {
        this.lightRadius = r;
        uniformsDirty = true;
        occlusionDirty = true;
    }

    private double avatarFx = -1;
    private double avatarFy = -1;

    public void setAvatarPosition(double x, double y) {
        int tileX = (int) Math.floor(x);
        int tileY = (int) Math.floor(y);

        // Tile-based data for occlusion & lighting
        if (this.avatarX != tileX || this.avatarY != tileY) {
            occlusionDirty = true;
            uniformsDirty = true;
        }

        this.avatarX = tileX;
        this.avatarY = tileY;

        // Continuous position for camera
        this.avatarFx = x;
        this.avatarFy = y;
    }
    private double renderOffsetX;
    private double renderOffsetY;

    public void updateCamera() {
        if (avatarX < 0 || avatarY < 0) return;
        if (worldWidth <= 0 || worldHeight <= 0) return;
        if (viewWidth <= 0 || viewHeight <= 0) return;

        // Desired camera origin in tile coordinates (center on avatar)
        double targetCamTileX = avatarFx - viewWidth / 2.0;
        double targetCamTileY = avatarFy - viewHeight / 2.0;

        // Smooth camera towards target
        camTileX += (targetCamTileX - camTileX) * CAMERA_SMOOTH;
        camTileY += (targetCamTileY - camTileY) * CAMERA_SMOOTH;

        // Clamp in DOUBLE space (important)
        double maxCamX = Math.max(0, worldWidth - viewWidth);
        double maxCamY = Math.max(0, worldHeight - viewHeight);
        camTileX = Math.max(0.0, Math.min(maxCamX, camTileX));
        camTileY = Math.max(0.0, Math.min(maxCamY, camTileY));


// Use floor to prevent flip-flop jitter
        int newOriginX = (int) Math.floor(camTileX);
        int newOriginY = (int) Math.floor(camTileY);

        if (newOriginX != viewOriginX || newOriginY != viewOriginY) {
            viewOriginX = newOriginX;
            viewOriginY = newOriginY;
            occlusionDirty = true;
            uniformsDirty = true;
        }

        renderOffsetX = camTileX - viewOriginX;
        renderOffsetY = camTileY - viewOriginY;
    }
    public int getViewOriginX() {
        return viewOriginX;
    }

    public int getViewOriginY() {
        return viewOriginY;
    }

    public void configureView(int inWorldWidth, int inWorldHeight, int inViewWidth, int inViewHeight, int inHudHeight) {
        this.worldWidth = inWorldWidth;
        this.worldHeight = inWorldHeight;
        this.viewWidth = inViewWidth;
        this.viewHeight = inViewHeight;
        this.hudHeight = inHudHeight;
        occlusionDirty = true;
        uniformsDirty = true;
    }

    public RenderContext buildContext(com.untitledgame.logic.TileType[][] world) {
        int startX = Math.max(0, viewOriginX);
        int endX = Math.min(world.length, viewOriginX + viewWidth);

        int startY = Math.max(0, viewOriginY);
        int endY = Math.min(world[0].length, viewOriginY + viewHeight);

        LightBounds litBounds = litBounds(startX, endX, startY, endY);
        return new RenderContext(startX, endX, startY, endY, litBounds);
    }

    public void updateLights(List<DroppedItem> drops, NpcManager npcManager, AvatarDraw avatarDraw) {
        List<LightSource> sources = new ArrayList<>();
        if (avatarFx >= 0 && avatarFy >= 0) {
            sources.add(new LightSource(avatarFx, avatarFy, (float) lightRadius, 1f, AVATAR_LIGHT_COLOR));
        }
        if (npcManager != null) {
            for (Npc npc : npcManager.npcs()) {
                double lx = npc.x() + 0.5;
                double ly = npc.y() + 0.5;
                sources.add(new LightSource(lx, ly, 5f, 0.6f, NPC_LIGHT_COLOR));
            }
        }
        if (drops != null) {
            for (DroppedItem drop : drops) {
                double lx = drop.x() + 0.5;
                double ly = drop.y() + 0.5;
                sources.add(new LightSource(lx, ly, 3.5f, 0.45f, ITEM_LIGHT_COLOR));
            }
        }

        activeLights.clear();
        for (int i = 0; i < Math.min(sources.size(), MAX_LIGHTS); i++) {
            activeLights.add(sources.get(i));
        }
        int lightHash = computeLightHash(activeLights);
        if (lightHash != lastLightHash || activeLights.size() != lastLightCount) {
            occlusionDirty = true;
            uniformsDirty = true;
        }
        lastLightHash = lightHash;
        lastLightCount = activeLights.size();
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private boolean inView(int x, int y) {
        return x >= viewOriginX && x < viewOriginX + viewWidth
                && y >= viewOriginY && y < viewOriginY + viewHeight;
    }

    public double toScreenX(double worldX) {
        return (worldX - viewOriginX - renderOffsetX) + xOffset;
    }

    public double toScreenY(double worldY) {
        return (worldY - viewOriginY - renderOffsetY) + yOffset;
    }

    public void initialize(int w, int h, int xOff, int yOff, TextureAtlas atlas) {
        this.width = w;
        this.height = h;
        this.xOffset = xOff;
        this.yOffset = yOff;
        this.textureAtlas = atlas;

        if (viewWidth == 0) {
            viewWidth = w;
        }
        if (viewHeight == 0) {
            viewHeight = h - hudHeight;
        }

        if (batch == null) {
            batch = new SpriteBatch();
        }
        if (shapeRenderer == null) {
            shapeRenderer = new ShapeRenderer();
        }
        if (camera == null) {
            camera = new OrthographicCamera();
        }
        if (viewport == null) {
            viewport = new FitViewport(width, height, camera);
        }
        camera.setToOrtho(false, width, height);
        camera.update();
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        TETile.configureRendering(batch, textureAtlas, 1.0f);
    }

    public void initialize(int w, int h, TextureAtlas atlas) {
        initialize(w, h, 0, 0, atlas);
    }

    public void applyFullLightingPass(com.untitledgame.logic.TileType[][] world, RenderContext context) {
        if (batch == null || activeLights.isEmpty() || world == null || context == null) {
            return;
        }
        ensureLightResources();
        if (shouldRefreshOcclusionMap(world, context)) {
            refreshOcclusionMap(world, context);
        }
        updateLightUniforms();

        batch.flush();
        ShaderProgram previousShader = batch.getShader();
        batch.setShader(lightShader);
        lightShader.bind();
        bindOcclusionTexture();
        updateAvatarUniform();

        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(lightMaskTexture, (float) xOffset, (float) yOffset, viewWidth, viewHeight);
        batch.flush();
        batch.setShader(previousShader);
    }


    public void drawWorld(com.untitledgame.logic.TileType[][] world,
                          List<Corpse> corpses,
                          List<DroppedItem> drops,
                          NpcManager npcManager,
                          RenderContext context,
                          AvatarDraw avatarDraw) {
        if (world == null || context == null) {
            return;
        }
        List<RenderOp> ops = new ArrayList<>();
        int coverCutoff = computeCoverCutoff(npcManager, avatarDraw, context);
        addTileDraws(world, context, ops, coverCutoff);
        addCorpseDraws(corpses, context, ops);
        addDroppedItemDraws(drops, context, ops);
        addNpcDraws(npcManager, context, ops);
        addAvatarDraw(avatarDraw, context, ops);

        ops.sort(Comparator
                .comparingInt((RenderOp op) -> op.layer().order)
                .thenComparingDouble(RenderOp::sortY)
                .thenComparingDouble(RenderOp::sortX));
        for (RenderOp op : ops) {
            op.action().run();
        }
    }

    private LightBounds litBounds(int viewStartX, int viewEndX, int viewStartY, int viewEndY) {
        if (activeLights.isEmpty()) {
            return new LightBounds(viewStartX, viewEndX, viewStartY, viewEndY);
        }
        int minX = viewEndX;
        int maxX = viewStartX;
        int minY = viewEndY;
        int maxY = viewStartY;
        for (LightSource light : activeLights) {
            int radius = Math.max(1, (int) Math.ceil(light.radius() + 1.0));
            minX = Math.min(minX, (int) Math.floor(light.x() - radius));
            maxX = Math.max(maxX, (int) Math.ceil(light.x() + radius + 1));
            minY = Math.min(minY, (int) Math.floor(light.y() - radius));
            maxY = Math.max(maxY, (int) Math.ceil(light.y() + radius + 1));
        }

        int startX = Math.max(viewStartX, minX);
        int endX = Math.min(viewEndX, maxX);

        int startY = Math.max(viewStartY, minY);
        int endY = Math.min(viewEndY, maxY);

        return new LightBounds(startX, Math.max(startX, endX), startY, Math.max(startY, endY));
    }

    private void ensureLightResources() {
        ensureLightShader();
        ensureLightMaskTexture();
        ensureOcclusionBuffer();
    }

    private void ensureLightShader() {
        if (lightShader != null && lightShader.isCompiled()) {
            return;
        }
        if (lightShader != null) {
            lightShader.dispose();
        }
        ShaderProgram.pedantic = false;
        String fragment = Gdx.files.internal("shaders/lightmask.frag").readString();
        lightShader = new ShaderProgram(LIGHT_VERTEX_SHADER, fragment);
        if (!lightShader.isCompiled()) {
            throw new IllegalStateException("Failed to compile light shader: " + lightShader.getLog());
        }
        lightShader.bind();
        lightShader.setUniformi("u_texture", 0);
        lightShader.setUniformi("u_occlusionMap", 1);
        lightShader.setUniformi("u_lightCount", 0);
        lightShader.setUniformf("u_ambient", AMBIENT_LIGHT);
        uniformsDirty = true;
    }

    private void ensureLightMaskTexture() {
        if (lightMaskTexture != null) {
            return;
        }
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(0, 0, 0, 0);
        pixel.fill();
        lightMaskTexture = new Texture(pixel);
        lightMaskTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixel.dispose();
    }

    private void ensureOcclusionBuffer() {
        int targetWidth = Math.max(1, viewWidth * MAX_LIGHTS);
        int targetHeight = Math.max(1, viewHeight);

        if (occlusionPixmap != null && occlusionPixmap.getWidth() == targetWidth
                && occlusionPixmap.getHeight() == targetHeight) {
            return;
        }

        if (occlusionPixmap != null) {
            occlusionPixmap.dispose();
        }
        if (occlusionTexture != null) {
            occlusionTexture.dispose();
        }

        occlusionPixmap = new Pixmap(targetWidth, targetHeight, Pixmap.Format.Alpha);
        occlusionTexture = new Texture(occlusionPixmap);
        occlusionTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        occlusionTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        occlusionDirty = true;
    }
    private boolean shouldRefreshOcclusionMap(com.untitledgame.logic.TileType[][] world, RenderContext context) {
        if (world == null || context == null || occlusionTexture == null || occlusionPixmap == null) {
            return false;
        }
        LightBounds bounds = context.litBounds;
        int signature = computeWorldSignature(world, bounds);
        boolean originChanged = viewOriginX != lastOcclusionOriginX || viewOriginY != lastOcclusionOriginY;
        boolean lightsChanged = lastOcclusionLightHash != lastLightHash || lastOcclusionLightCount != lastLightCount;
        return occlusionDirty || originChanged || signature != lastOcclusionWorldHash || lightsChanged;
    }


    private void refreshOcclusionMap(com.untitledgame.logic.TileType[][] world, RenderContext context) {
        occlusionPixmap.setColor(1f, 1f, 1f, 0f);
        occlusionPixmap.fill();
        if (world == null || occlusionTexture == null || occlusionPixmap == null || context == null) {
            return;
        }
        int lightIndex = 0;
        for (LightSource light : activeLights) {
            if (lightIndex >= MAX_LIGHTS) {
                break;
            }
            float[][] visibility = computeVisibilityGrid(world, light);
            int offsetX = viewWidth * lightIndex;
            for (int sx = 0; sx < viewWidth; sx++) {
                int worldX = viewOriginX + sx;
                for (int sy = 0; sy < viewHeight; sy++) {
                    int worldY = viewOriginY + sy;
                    float visibilityValue = 0f;
                    boolean inBounds = worldX >= 0 && worldX < worldWidth && worldY >= 0 && worldY < worldHeight;
                    if (inBounds) {
                        visibilityValue = visibility[sx][sy];
                    }
                    occlusionPixmap.setColor(1f, 1f, 1f, visibilityValue);
                    occlusionPixmap.drawPixel(offsetX + sx, viewHeight - 1 - sy);
                }
            }
            lightIndex++;
        }

        lastOcclusionOriginX = viewOriginX;
        lastOcclusionOriginY = viewOriginY;
        lastOcclusionLightHash = lastLightHash;
        lastOcclusionLightCount = lastLightCount;
        lastOcclusionWorldHash = computeWorldSignature(world, context.litBounds);
        occlusionDirty = false;
        occlusionTexture.draw(occlusionPixmap, 0, 0);
    }


    private float[][] computeVisibilityGrid(com.untitledgame.logic.TileType[][] world, LightSource light) {
        float[][] visibility = new float[viewWidth][viewHeight];
        if (world == null || light == null || light.radius() <= 0.0f) {
            return visibility;
        }
        int lightTileX = (int) Math.floor(light.x());
        int lightTileY = (int) Math.floor(light.y());
        setVisibility(visibility, light, lightTileX, lightTileY, 1f);

        int radius = (int) Math.ceil(light.radius());
        shadowcastOctant(visibility, world, light, radius, 1, 0, 0, 1);
        shadowcastOctant(visibility, world, light, radius, 1, 0, 0, -1);
        shadowcastOctant(visibility, world, light, radius, -1, 0, 0, 1);
        shadowcastOctant(visibility, world, light, radius, -1, 0, 0, -1);
        shadowcastOctant(visibility, world, light, radius, 0, 1, 1, 0);
        shadowcastOctant(visibility, world, light, radius, 0, 1, -1, 0);
        shadowcastOctant(visibility, world, light, radius, 0, -1, 1, 0);
        shadowcastOctant(visibility, world, light, radius, 0, -1, -1, 0);

        return visibility;
    }

    private void shadowcastOctant(float[][] visibility,
                                  com.untitledgame.logic.TileType[][] world,
                                  LightSource light,
                                  int radius,
                                  int xx,
                                  int xy,
                                  int yx,
                                  int yy) {
        int originX = (int) Math.floor(light.x());
        int originY = (int) Math.floor(light.y());
        List<ShadowInterval> shadows = new ArrayList<>();
        for (int row = 1; row <= radius; row++) {
            if (isFullyShadowed(shadows)) {
                break;
            }
            for (int col = 0; col <= row; col++) {
                int deltaX = -col;
                int deltaY = -row;
                float leftSlope = (deltaX - 0.5f) / (deltaY + 0.5f);
                float rightSlope = (deltaX + 0.5f) / (deltaY - 0.5f);
                float startSlope = Math.min(leftSlope, rightSlope);
                float endSlope = Math.max(leftSlope, rightSlope);
                if (isShadowed(shadows, startSlope, endSlope)) {
                    continue;
                }
                int currentX = originX + deltaX * xx + deltaY * xy;
                int currentY = originY + deltaX * yx + deltaY * yy;
                double distanceFromLight = Math.hypot(deltaX, deltaY);
                if (distanceFromLight <= light.radius()) {
                    setVisibility(visibility, light, currentX, currentY, 1f);
                }
                if (isBlocking(world, currentX, currentY)) {
                    addShadow(shadows, startSlope, endSlope);
                }
            }
        }
    }

    private void addShadow(List<ShadowInterval> shadows, float start, float end) {
        int insertAt = 0;
        while (insertAt < shadows.size() && shadows.get(insertAt).start <= start) {
            insertAt++;
        }
        shadows.add(insertAt, new ShadowInterval(start, end));
        mergeShadows(shadows);
    }

    private void mergeShadows(List<ShadowInterval> shadows) {
        if (shadows.size() <= 1) {
            return;
        }
        List<ShadowInterval> merged = new ArrayList<>();
        ShadowInterval current = shadows.get(0);
        for (int i = 1; i < shadows.size(); i++) {
            ShadowInterval next = shadows.get(i);
            if (next.start <= current.end) {
                current = new ShadowInterval(current.start, Math.max(current.end, next.end));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        shadows.clear();
        shadows.addAll(merged);
    }

    private boolean isShadowed(List<ShadowInterval> shadows, float start, float end) {
        for (ShadowInterval interval : shadows) {
            if (start >= interval.start && end <= interval.end) {
                return true;
            }
        }
        return false;
    }

    private boolean isFullyShadowed(List<ShadowInterval> shadows) {
        if (shadows.isEmpty()) {
            return false;
        }
        ShadowInterval interval = shadows.get(0);
        return interval.start <= 0f && interval.end >= 1f;
    }

    private void setVisibility(float[][] visibility, LightSource light, int worldX, int worldY, float amount) {
        int screenX = worldX - viewOriginX;
        int screenY = worldY - viewOriginY;
        if (screenX < 0 || screenX >= viewWidth || screenY < 0 || screenY >= viewHeight) {
            return;
        }
        float distanceFade = distanceFade(light, worldX + 0.5, worldY + 0.5);
        float value = Math.min(1f, amount * distanceFade * light.intensity());
        visibility[screenX][screenY] = Math.max(visibility[screenX][screenY], value);
    }

    private float distanceFade(LightSource light, double worldX, double worldY) {
        double dx = worldX - light.x();
        double dy = worldY - light.y();
        double dist = Math.hypot(dx, dy);

        float radius = light.radius();
        float falloffWidth = 1.5f; // ← TUNE THIS (0.5–2.0 is typical)

        float inner = radius - falloffWidth;

        if (dist <= inner) {
            return 1.0f;
        }

        if (dist >= radius) {
            return 0.0f;
        }

        float t = (float)((radius - dist) / falloffWidth);
        return t;
    }

    private boolean isBlocking(com.untitledgame.logic.TileType[][] world, int worldX, int worldY) {
        if (world == null) {
            return true;
        }
        if (worldX < 0 || worldY < 0 || worldX >= worldWidth || worldY >= worldHeight) {
            return true;
        }
        return isBlockingTile(world[worldX][worldY]);
    }

    private boolean isBlockingTile(com.untitledgame.logic.TileType tile) {
        if (tile == null) {
            return true;
        }
        return tile == TileType.WALL_TOP || tile == TileType.WALL_SIDE
                || tile == TileType.LEFT_WALL || tile == TileType.BACK_WALL;
    }

    private int computeWorldSignature(com.untitledgame.logic.TileType[][] world, LightBounds bounds) {
        if (world == null || bounds == null) {
            return 0;
        }
        int startX = Math.max(0, bounds.startX());
        int endX = Math.min(world.length, bounds.endX());
        int startY = Math.max(0, bounds.startY());
        int endY = Math.min(world[0].length, bounds.endY());
        int hash = 7;
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                TileType tile = world[x][y];
                hash = 31 * hash + (tile == null ? 0 : tile.ordinal() + 1);
            }
        }
        return hash;
    }

    private int computeLightHash(List<LightSource> lights) {
        int hash = 11;
        for (LightSource light : lights) {
            int colorKey = Color.argb8888(light.color());
            hash = 31 * hash + Objects.hash(light.x(), light.y(), light.radius(), light.intensity(), colorKey);
        }
        return hash;
    }

    private void updateLightUniforms() {
        if (lightShader == null) {
            return;
        }
        boolean originChanged = lastUniformOriginX != viewOriginX || lastUniformOriginY != viewOriginY;
        boolean viewChanged = lastUniformViewWidth != viewWidth || lastUniformViewHeight != viewHeight;
        if (uniformsDirty || originChanged || viewChanged) {
            lightShader.bind();
            lightShader.setUniformf("u_viewOrigin", viewOriginX, viewOriginY);
            lightShader.setUniformf("u_viewSize", viewWidth, viewHeight);
            lightShader.setUniformf("u_falloff", DEFAULT_FALLOFF);
            lightShader.setUniformf("u_ambient", AMBIENT_LIGHT);
            lightShader.setUniformi("u_lightCount", activeLights.size());

            float[] lightPositions = new float[MAX_LIGHTS * 2];
            float[] lightColors = new float[MAX_LIGHTS * 3];
            float[] lightIntensities = new float[MAX_LIGHTS];
            float[] lightRadii = new float[MAX_LIGHTS];
            for (int i = 0; i < activeLights.size() && i < MAX_LIGHTS; i++) {
                LightSource light = activeLights.get(i);
                lightPositions[i * 2] = (float) light.x();
                lightPositions[i * 2 + 1] = (float) light.y();
                lightColors[i * 3] = light.color().r;
                lightColors[i * 3 + 1] = light.color().g;
                lightColors[i * 3 + 2] = light.color().b;
                lightIntensities[i] = light.intensity();
                lightRadii[i] = light.radius();
            }

            lightShader.setUniform2fv("u_lightPos", lightPositions, 0, lightPositions.length);
            lightShader.setUniform3fv("u_lightColor", lightColors, 0, lightColors.length);
            lightShader.setUniform1fv("u_lightIntensity", lightIntensities, 0, lightIntensities.length);
            lightShader.setUniform1fv("u_lightRadius", lightRadii, 0, lightRadii.length);

            lastUniformOriginX = viewOriginX;
            lastUniformOriginY = viewOriginY;
            lastUniformViewWidth = viewWidth;
            lastUniformViewHeight = viewHeight;
            uniformsDirty = false;
        }
    }

    private void updateAvatarUniform() {
        if (lightShader == null) {
            return;
        }
        lightShader.bind();
        lightShader.setUniformf(
                "u_cameraOffset",
                (float) renderOffsetX,
                (float) renderOffsetY
        );
    }

    private void bindOcclusionTexture() {
        if (occlusionTexture == null || lightShader == null) {
            return;
        }
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1);
        occlusionTexture.bind();
        lightShader.setUniformi("u_occlusionMap", 1);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
    }

    private void addTileDraws(com.untitledgame.logic.TileType[][] world,
                              RenderContext context,
                              List<RenderOp> ops,
                              int coverCutoff) {
        for (int x = context.startX; x < context.endX; x++) {
            for (int y = context.startY; y < context.endY; y++) {
                com.untitledgame.logic.TileType tile = world[x][y];

                if (tile == null) {
                    throw new IllegalArgumentException("Tile at " + x + "," + y + " is null.");
                }

                TETile teTile = getTETile(tile);
                if (teTile == null) {
                    continue;
                }
                DrawLayer layer = tileLayer(tile, y, coverCutoff);
                double screenX = toScreenX(x);
                double screenY = toScreenY(y);
                ops.add(new RenderOp(y, layer, x, () -> teTile.drawSized(screenX, screenY, 1.0)));
            }
        }
    }

    private void addCorpseDraws(List<Corpse> corpses, RenderContext context, List<RenderOp> ops) {
        if (corpses == null) {
            return;
        }
        for (Corpse corpse : corpses) {
            if (!context.withinLightWindow(corpse.x(), corpse.y())) {
                continue;
            }
            double screenX = toScreenX(corpse.x());
            double screenY = toScreenY(corpse.y());
            ops.add(new RenderOp(corpse.y(), DrawLayer.ITEM, corpse.x(),
                    () -> corpse.tile().drawSized(screenX, screenY, 1.0)));
        }
    }

    private void addDroppedItemDraws(List<DroppedItem> drops, RenderContext context, List<RenderOp> ops) {
        if (drops == null) {
            return;
        }
        for (DroppedItem drop : drops) {
            if (!context.withinLightWindow(drop.x(), drop.y())) {
                continue;
            }
            double screenX = toScreenX(drop.x());
            double screenY = toScreenY(drop.y());
            ops.add(new RenderOp(drop.y(), DrawLayer.ITEM, drop.x(),
                    () -> Tileset.LOOT_BAG.drawSized(screenX, screenY, 1.0)));
        }
    }

    private void addNpcDraws(NpcManager npcManager, RenderContext context, List<RenderOp> ops) {
        if (npcManager == null) {
            return;
        }
        for (Npc npc : npcManager.npcs()) {
            if (!context.withinLightWindow(npc.x(), npc.y())) {
                continue;
            }
            npc.updateSmooth(SMOOTH_SPEED);
            double drawX = npc.drawX();
            double drawY = npc.drawY();
            double screenX = toScreenX(drawX);
            double screenY = toScreenY(drawY);
            double groundY = Math.floor(drawY);

            // Use same scale as avatar for consistency
            double scale = 4.0;
            double offset = (scale - 1.0) / 2.0;

            ops.add(new RenderOp(groundY, DrawLayer.ENTITY, drawX,
                    () -> {
                        com.badlogic.gdx.graphics.g2d.TextureRegion frame = npc.currentFrame();
                        if (frame != null && batch != null) {
                            batch.draw(frame,
                                    (float) (screenX - offset),
                                    (float) (screenY - offset),
                                    (float) scale,
                                    (float) scale);
                        }
                    }));
        }
    }

    private void addAvatarDraw(AvatarDraw avatarDraw, RenderContext context, List<RenderOp> ops) {
        if (avatarDraw == null || avatarDraw.sprite() == null) {
            return;
        }
        int avatarTileX = (int) Math.round(avatarDraw.x());
        int avatarTileY = (int) Math.round(avatarDraw.y());
        if (!context.contains(avatarTileX, avatarTileY)) {
            return;
        }
        double screenX = toScreenX(avatarDraw.x());
        double screenY = toScreenY(avatarDraw.y());
        double groundY = Math.floor(avatarDraw.y());


        // Apply scale and center the sprite on the tile
        // If scale is 3.0, sprite takes 3 tiles, so offset by -1 tile to center it
        double scale = avatarDraw.scale();
        double offset = (scale - 1.0) / 2.0;

        ops.add(new RenderOp(groundY, DrawLayer.ENTITY, avatarDraw.x(),
                () -> batch.draw(avatarDraw.sprite(),
                        (float) (screenX - offset),
                        (float) (screenY - offset),
                        (float) scale,
                        (float) scale)));
    }

    private DrawLayer tileLayer(com.untitledgame.logic.TileType tile, int y, int coverCutoff) {
        if (isFloor(tile)) {
            return DrawLayer.BACKGROUND;
        }
        if ((isSideWall(tile) || isTopWall(tile)) && y < coverCutoff) {
            return DrawLayer.COVER;
        }
        return DrawLayer.BACKGROUND;
    }

    private int computeCoverCutoff(NpcManager npcManager, AvatarDraw avatarDraw, RenderContext context) {
        int cutoff = context.startY;
        if (avatarDraw != null) {
            cutoff = Math.min(cutoff, (int) Math.floor(avatarDraw.y()) - 1);        }
        if (npcManager != null) {
            for (Npc npc : npcManager.npcs()) {
                cutoff = Math.min(cutoff, npc.y() - 1);
            }
        }
        return cutoff;
    }



    private TETile getTETile(com.untitledgame.logic.TileType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case FLOOR -> Tileset.FLOOR;
            case ELEVATOR -> Tileset.ELEVATOR;
            case WALL_TOP -> Tileset.WALL_TOP;
            case WALL_SIDE -> Tileset.WALL_SIDE;
            case LEFT_WALL -> Tileset.LEFT_WALL;
            case BACK_WALL -> Tileset.BACK_WALL;
            case NOTHING -> null;
        };
    }

    private boolean isFloor(com.untitledgame.logic.TileType t) {
        return t == com.untitledgame.logic.TileType.FLOOR || t == com.untitledgame.logic.TileType.ELEVATOR;
    }

    private boolean isSideWall(com.untitledgame.logic.TileType t) {
        return t == com.untitledgame.logic.TileType.LEFT_WALL
                || t == com.untitledgame.logic.TileType.WALL_SIDE;
    }

    private boolean isTopWall(com.untitledgame.logic.TileType t) {
        return t == com.untitledgame.logic.TileType.WALL_TOP
                || t == com.untitledgame.logic.TileType.BACK_WALL;
    }

    public void resetFont() { }

    public void clearScreen() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    public void beginBatch() {
        if (batch == null || camera == null) {
            return;
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
    }

    public void endBatch() {
        if (batch == null) {
            return;
        }
        batch.end();
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public Vector2 screenToWorld(float screenX, float screenY) {
        if (viewport == null) {
            scratchVector.set(screenX, screenY);
            return scratchVector;
        }
        scratchVector.set(screenX, screenY);
        viewport.unproject(scratchVector);
        return scratchVector;
    }

    public void resize(int screenWidth, int screenHeight) {
        if (viewport != null) {
            viewport.update(screenWidth, screenHeight, true);
        }
    }

    @Override
    public void close() {
        dispose();
    }

    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        if (lightMaskTexture != null) {
            lightMaskTexture.dispose();
            lightMaskTexture = null;
        }
        if (occlusionTexture != null) {
            occlusionTexture.dispose();
            occlusionTexture = null;
        }
        if (occlusionPixmap != null) {
            occlusionPixmap.dispose();
            occlusionPixmap = null;
        }
        if (lightShader != null) {
            lightShader.dispose();
            lightShader = null;
        }
        if (textureAtlas != null) {
            textureAtlas = null;
        }
    }
}
