package DarkShark02.JustJump;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;


public class JustJumpGame extends ApplicationAdapter {
    OrthographicCamera camera;
    SpriteBatch batch;
    ShapeRenderer shapeRenderer;
    BitmapFont font;

    Animation<TextureRegion> walkAnimation;
    Animation<TextureRegion> jumpAnimation;
    Animation<TextureRegion> slipAnimation;
    Animation<TextureRegion> hitAnimation;
    Animation<TextureRegion> springAnimation;

    float stateTime;

    float x, y;
    float velocityY;
    float velocityX;
    boolean isJumping;
    boolean isMoving;
    boolean isSlipping;
    boolean isHitted;
    boolean isGameOver;

    boolean starCollected;
    boolean heartCollected;

    final float GRAVITY = -1000f;
    final float JUMP_VELOCITY = 450f;
    final float MOVEMENT_VELOCITY = 500f;
    final float GROUND_Y = 50;

    float speedMultiplier = 1f;
    float starEffectTimer = 0f;

    Texture walkA, walkB, jump, slip, spikes, hit, star, heart, snow, spring, springOut;
    Texture grass, dirt, sky;

    Rectangle playerRect;
    Rectangle spikesRect;
    Rectangle starRect, heartRect, snowRect, springRect;
    Rectangle playerFeetRect = new Rectangle();

    final int MAX_HEALTH = 3;
    int currentHealth = MAX_HEALTH;

    float damageCooldown = 0;
    float hitDuration = 0;

    float springEffectTimer = 0f;
    boolean springActive = false;

    float springBoost = 0f;
    final float SPRING_BOOST_VELOCITY = 600f;

    int TILE_SIZE = 64;  // dimensione di ogni pezzo
    int SCREEN_WIDTH = 1600;
    int SCREEN_HEIGHT = 900;

    int rows = SCREEN_HEIGHT / TILE_SIZE;   // es: 900 / 64 = 14 righe circa
    int cols = SCREEN_WIDTH / TILE_SIZE;    // es: 1600 / 64 = 25 colonne circa

    // Una matrice che contiene l'id del tile per ogni cella
    int[][] backgroundTiles = new int[rows][cols];

    Texture beeA, beeB;
    Rectangle beeRect;
    boolean beeHit = false;
    float beeSpeed = 200f;  // velocità orizzontale dell’ape
    float beeSpawnTimer = 0f;
    final float BEE_SPAWN_INTERVAL = 5f;


    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 1600, 900);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();  // font di default
        font.getData().setScale(3f); // scala più grande per testo visibile


        grass = new Texture("background_solid_grass.png");
        dirt = new Texture("background_solid_dirt.png");
        sky = new Texture("background_solid_sky.png");
        beeRect = new Rectangle(-100, 100, 60, 50);  // fuori schermo, dimensioni esempio
        beeHit = false;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r < rows - 3) {
                    backgroundTiles[r][c] = 2; // cielo (sky)
                } else {
                    backgroundTiles[r][c] = 0; // erba (grass)
                }
            }
        }

        loadTextures();
        initializeAnimations();
        resetGame();
    }

    private void loadTextures() {
        walkA = new Texture(Gdx.files.internal("character_yellow_walk_a.png"));
        walkB = new Texture(Gdx.files.internal("character_yellow_walk_b.png"));
        jump = new Texture(Gdx.files.internal("character_yellow_jump.png"));
        slip = new Texture(Gdx.files.internal("character_yellow_duck.png"));
        spikes = new Texture(Gdx.files.internal("spikes.png"));
        hit = new Texture(Gdx.files.internal("character_yellow_hit.png"));
        star = new Texture(Gdx.files.internal("star.png"));
        heart = new Texture(Gdx.files.internal("heart.png"));
        snow = new Texture(Gdx.files.internal("snow.png"));
        spring = new Texture(Gdx.files.internal("spring.png"));
        springOut = new Texture(Gdx.files.internal("spring_out.png"));
        beeA = new Texture(Gdx.files.internal("bee_a.png"));
        beeB = new Texture(Gdx.files.internal("bee_rest.png"));

    }

    private void initializeAnimations() {
        walkAnimation = new Animation<>(0.3f, new TextureRegion(walkA), new TextureRegion(walkB));
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP);
        jumpAnimation = new Animation<>(0.3f, new TextureRegion(jump));
        slipAnimation = new Animation<>(0.3f, new TextureRegion(slip));
        hitAnimation = new Animation<>(0.5f, new TextureRegion(hit));
        springAnimation = new Animation<>(0.5f, new TextureRegion(spring));
    }

    private void resetGame() {
        x = 100;
        y = GROUND_Y;
        velocityY = 0;
        velocityX = 0;
        isJumping = false;
        isMoving = false;
        isSlipping = false;
        isHitted = false;
        isGameOver = false;

        playerRect = new Rectangle();
        spikesRect = new Rectangle(700, GROUND_Y, 50, 50);
        starRect = new Rectangle(900, GROUND_Y, 40, 40);
        heartRect = new Rectangle(400, GROUND_Y, 40, 40);
        snowRect = new Rectangle(1100, GROUND_Y, 100, 50);
        springRect = new Rectangle(200, GROUND_Y, 50, 50);

        currentHealth = MAX_HEALTH;
        starCollected = false;
        heartCollected = false;
        starEffectTimer = 0f;
        speedMultiplier = 1f;

        stateTime = 0f;
        damageCooldown = 0f;
        hitDuration = 0f;
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        stateTime += delta;
        damageCooldown -= delta;
        hitDuration -= delta;

        if (!isGameOver) {
            handleInput(delta);
            updatePhysics(delta);
            checkCollisions(delta);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetGame();
        }

        beeSpawnTimer += delta;
        if (beeSpawnTimer >= BEE_SPAWN_INTERVAL && beeRect.x < 0) {
            spawnBee();
        }
        if (beeRect.x >= -100) {
            updateBee(delta);
        }
        drawGame();
    }

    private void handleInput(float delta) {
    isMoving = false;
    velocityX = 0;

    isSlipping = Gdx.input.isKeyPressed(Input.Keys.DOWN) && !isJumping;

    if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
        velocityX = MOVEMENT_VELOCITY * speedMultiplier;
        isMoving = true;
    } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
        velocityX = -MOVEMENT_VELOCITY * speedMultiplier;
        isMoving = true;
    }

    if (isMoving) {
        x += velocityX * delta;
        float playerWidth = 80;
        if (x < 0) x = 0;
        if (x + playerWidth > camera.viewportWidth) x = camera.viewportWidth - playerWidth;
    }

    // SALTO: non moltiplichiamo più per speedMultiplier
    if (Gdx.input.isKeyJustPressed(Input.Keys.UP) && !isJumping) {
        velocityY = JUMP_VELOCITY;
        isJumping = true;
    }
}

    private void updatePhysics(float delta) {
        velocityY += GRAVITY * delta;

        if (springBoost > 0) {
            velocityY = springBoost; // assegna il boost
            springBoost = 0;         // resetta il boost subito dopo averlo usato
        }

        y += velocityY * delta;

        if (y <= GROUND_Y) {
            y = GROUND_Y;
            velocityY = 0;
            isJumping = false;
        }


        // Aggiorna timer effetto stella
        if (starEffectTimer > 0) {
            starEffectTimer -= delta;
            if (starEffectTimer <= 0) {
                speedMultiplier = 1f;
                starEffectTimer = 0;
            }
        }

        if (springEffectTimer > 0) {
            springEffectTimer -= delta;
            if (springEffectTimer <= 0) {
                springEffectTimer = 0;
                springActive = false;
            }
        }
    }

    private void checkCollisions(float delta) {
        float playerWidth = 80;
        float playerHeight = isSlipping ? 90 : 120;

        playerRect.set(x, y, playerWidth, playerHeight);

        // Hitbox piedi: base stretta e molto bassa, posizione y al suolo del player
        playerFeetRect.set(x + 10, y, playerWidth - 20, 15); // 15px altezza, lascia margine ai lati

        // Hitbox spikes: più bassa (esempio 15 px)
        spikesRect.set(700, GROUND_Y, 50, 15);

        // Collisione piedi con spikes
        if (playerFeetRect.overlaps(spikesRect) && damageCooldown <= 0) {
            currentHealth--;
            if (currentHealth <= 0) {
                currentHealth = 0;
                isGameOver = true;
            }
            isHitted = true;
            hitDuration = 0.5f;
            damageCooldown = 1f;
        } else if (hitDuration <= 0) {
            isHitted = false;
        }

        // Cuore
        if (playerRect.overlaps(heartRect) && !heartCollected) {
            if (currentHealth < MAX_HEALTH) {
                currentHealth++;
                heartCollected = true;
            }
        }

        // Stella
        if (playerRect.overlaps(starRect) && !starCollected) {
            starCollected = true;
            starEffectTimer = 10f;
            speedMultiplier = 1.2f;
        }

        // Neve (malus solo quando sopra)
        if (playerFeetRect.overlaps(snowRect)) {
            speedMultiplier = (starEffectTimer > 0) ? 1.2f : 0.8f;
        } else if (starEffectTimer <= 0) {
            speedMultiplier = 1f;
        }

        // Pedana 
        if (playerRect.overlaps(springRect) && !springActive) {
            springBoost = SPRING_BOOST_VELOCITY;
            isJumping = true;
            springEffectTimer = 0.5f;
            springActive = true;
        }

        // Gestione ape
        if (beeRect.x >= 0) { // se è visibile a schermo
            if (playerRect.overlaps(beeRect)) {
                if (!beeHit) {
                    // Controllo se colpito da sopra (il player deve essere sopra l’ape e stia scendendo)
                    if (y > beeRect.y + beeRect.height - 10 && velocityY < 0) {
                        beeHit = true;
                        velocityY = JUMP_VELOCITY * 1.2f;  // slancio leggero in salto
                    } else {
                        // Colpito da lato o sotto -> perde vita
                        if (damageCooldown <= 0) {
                            currentHealth--;
                            damageCooldown = 1f;
                            if (currentHealth <= 0) {
                                currentHealth = 0;
                                isGameOver = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private void drawBackground() {
        for (int r = 0; r < backgroundTiles.length; r++) {
            for (int c = 0; c < backgroundTiles[r].length; c++) {
                Texture tileTexture = null;
                switch (backgroundTiles[r][c]) {
                    case 0: tileTexture = grass; break;
                    case 1: tileTexture = dirt; break;
                    case 2: tileTexture = sky; break;
                }
                batch.draw(tileTexture, c * TILE_SIZE, SCREEN_HEIGHT - (r + 1) * TILE_SIZE);
            }
        }
    }

    private void drawGame() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();

        drawBackground();

        TextureRegion currentFrame;
        if (isGameOver) {
            currentFrame = hitAnimation.getKeyFrame(0);
        } else if (isHitted) {
            currentFrame = hitAnimation.getKeyFrame(stateTime, true);
        } else if (isJumping) {
            currentFrame = jumpAnimation.getKeyFrame(stateTime);
        } else if (isSlipping) {
            currentFrame = slipAnimation.getKeyFrame(stateTime);
        } else if (isMoving) {
            currentFrame = walkAnimation.getKeyFrame(stateTime);
        } else {
            currentFrame = walkAnimation.getKeyFrame(0);
        }

        float playerWidth = 80;
        float playerHeight = isSlipping ? 90 : 120;
        batch.draw(currentFrame, x, y, playerWidth, playerHeight);

        // Stella sopra la testa se attiva
        if (starEffectTimer > 0) {
            batch.draw(star, x + 20, y + playerHeight, 30, 30);
        }

        if (springActive) {
            batch.draw(springOut, springRect.x, springRect.y, springRect.width, springRect.height);
        } else {
            batch.draw(spring, springRect.x, springRect.y, springRect.width, springRect.height);
        }

        // Oggetti
        batch.draw(spikes, spikesRect.x, spikesRect.y, spikesRect.width, spikesRect.height);
        if (!starCollected) batch.draw(star, starRect.x, starRect.y, starRect.width, starRect.height);
        if (!heartCollected) batch.draw(heart, heartRect.x, heartRect.y, heartRect.width, heartRect.height);
        batch.draw(snow, snowRect.x, snowRect.y, snowRect.width, snowRect.height);

        // Cuori
        for (int i = 0; i < currentHealth; i++) {
            batch.draw(heart, 20 + i * 40, 670, 30, 30);
        }

        // Disegna ape
        if (beeRect.x >= 0) {
            if (beeHit) {
                batch.draw(beeB, beeRect.x, beeRect.y, beeRect.width, beeRect.height);
            } else {
                batch.draw(beeA, beeRect.x, beeRect.y, beeRect.width, beeRect.height);
            }
        }

        batch.end();

        // Messaggio Game Over
        if (isGameOver) {
            batch.begin();
             // disegna rettangolo nero a tutto schermo
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 1);
            shapeRenderer.rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            shapeRenderer.end();

            String gameOverText = "Premi R per riprovare";
            // misura la larghezza del testo per centrarlo orizzontalmente
            float textWidth = font.getRegion().getRegionWidth() * font.getData().scaleX;
            // ma meglio calcolare usando GlyphLayout:
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, gameOverText);
            font.draw(batch, gameOverText, (SCREEN_WIDTH - layout.width) / 2, SCREEN_HEIGHT / 2);

            batch.end();
        }
    }

    private void spawnBee() {
        float randomY = GROUND_Y + 100 + (float) Math.random() * 200; // da 150 a 250 se GROUND_Y=50
        beeRect.set(-60, randomY, 60, 50);
        beeHit = false;
        System.out.println("Bee spawned at X: " + beeRect.x + ", Y: " + beeRect.y);
    }


    private void updateBee(float delta) {
        if (beeRect.x > SCREEN_WIDTH) {
            beeRect.x = -100; // fuori schermo
            beeHit = false;
            beeSpawnTimer = 0f; // reset timer per nuovo spawn!
        } else {
            beeRect.x += beeSpeed * delta;  // si muove verso destra
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        walkA.dispose();
        walkB.dispose();
        jump.dispose();
        slip.dispose();
        spikes.dispose();
        hit.dispose();
        star.dispose();
        heart.dispose();
        snow.dispose();
        font.dispose();
    }
}
