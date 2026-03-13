//BattleEngine is mainly responsible for managing the battle logic, turn order, and interactions between the player and enemy entities. It communicates with the BattlePanel through a Listener interface to update the UI based on game events. The engine handles skill usage, enemy actions, status effects, and progression through different realms and bosses in the game.
package com.ror.engine;

import com.ror.model.*;

public class BattleEngine {

    // Dont move, or else mo taas ang code sa tanan mo access ani
    public interface Listener {
        void onLog(String message);

        void onUpdateHP(Entity player, Entity enemy);

        void onUpdateSkillButtons(Skill[] skills);

        void onShowStory(String text);

        void onShowTutorial(String text);

        void onClearBattleLog();

        void onSetBackground(String path);

        void onPlayMusic(String path, boolean loop);

        void onStopMusic();

        void onPlaySoundThen(String soundPath, Runnable nextAction);

        void onEnableSkillButtons(boolean enabled);

        void onPlayerTurnPrompt();

        void onGameOver();

        void onGameWin();
    }

    private Listener listener;

    private Entity player;
    private Entity enemy;

    private boolean playerTurn = true;

    private boolean playerDodgeActive = false;
    private boolean enemyBlinded = false;
    private int burnDamageToEnemy = 0;
    private int burnTurnsRemaining = 0;
    private int playerShieldTurns = 0;

    private String mode = "Tutorial";

    public BattleEngine(Listener listener) {
        this.listener = listener;
    }

    public boolean isPlayerTurn() {
        return playerTurn;
    }

    public Entity getPlayer() {
        return player;
    }

    public Entity getEnemy() {
        return enemy;
    }

    public void startBattle(Entity chosenPlayer) {
        this.player = chosenPlayer;
        this.enemy = new Goblin();

        playerTurn = true;
        playerDodgeActive = false;
        enemyBlinded = false;
        burnDamageToEnemy = 0;
        burnTurnsRemaining = 0;
        playerShieldTurns = 0;
        mode = "Tutorial";

        listener.onPlayMusic("/com/ror/model/Assets/sfx/Tutorial.ogg", true);
        listener.onSetBackground("/com/ror/model/Assets/Backgrounds/Tutorial.png");
        listener.onClearBattleLog();

        listener.onLog("- The Battle Begins. It's " + player.getName() + " VS " + enemy.getName() + "!");
        listener.onUpdateHP(player, enemy);

        for (Skill sk : player.getSkills()) {
            sk.resetCooldown();
        }

        listener.onUpdateSkillButtons(player.getSkills());
        listener.onShowTutorial(
                " WELCOME TO REALMS OF RIFTBORNE! \n\n" +
                        "I see you have selected " + player.getName() + ". Here's a little let-you-know:\n\n" +
                        "[!] You are pitted against a succession of enemies. Defeat each one of them to get through the levels.\n\n"
                        +
                        "[!] Defeating a miniboss will allow you to proceed to the next realm.\n\n" +
                        "[!] You restore all health after every battle.\n\n" +
                        "[!] Your skills are your main method of attack, and certain skills will go on cooldown for a set amount of turns.\n\n"
                        +
                        "[!] The Back button on the bottom right is disabled until AFTER the Tutorial!\n\n" +
                        "Pick a skill to begin your turn!");

        listener.onLog("\n- Choose a skill to begin your turn.");
        listener.onPlayerTurnPrompt();
        listener.onEnableSkillButtons(true);
    }

    public void playerUseSkill(int index) {
        if (!playerTurn || player == null || enemy == null) {
            return;
        }

        Skill s = player.getSkills()[index];

        if (s.isOnCooldown()) {
            listener.onLog("- " + s.getName() + " is on cooldown for " + s.getCurrentCooldown() + " more turns!");
            return;
        }

        listener.onLog("- " + player.getName() + " uses " + s.getName() + "!");

        switch (s.getType().toLowerCase()) {
            case "chrono":
                int immediate = s.getPower() + player.getAtk();
                enemy.takeDamage(immediate);
                burnDamageToEnemy = Math.max(1, s.getPower() / 3);
                burnTurnsRemaining = 3;
                listener.onLog("- Timeblade strikes for " + immediate + " damage and applies a burn ("
                        + burnDamageToEnemy + " x " + burnTurnsRemaining + " turns)!");
                break;
            case "shield":
                playerShieldTurns = 2;
                listener.onLog("- Time Shield bends time around you! Incoming damage reduced by 50% for 2 turns!");
                break;
            case "dodge":
                playerDodgeActive = true;
                listener.onLog("- WindWalk activated! You'll evade the next attack completely!");
                break;
            case "reverse":
                int lost = player.getMaxHealth() - player.getCurrentHealth();
                int heal = (int) Math.ceil(lost * 0.5);
                if (heal <= 0) {
                    listener.onLog("- Reverse Flow restores 0 HP (you are already at full health).");
                } else {
                    player.setCurrentHealth(Math.min(player.getMaxHealth(), player.getCurrentHealth() + heal));
                    listener.onLog("- Reverse Flow restores " + heal + " HP (50% of lost HP)!");
                }
                break;
            case "heal":
                int lostHP = player.getMaxHealth() - player.getCurrentHealth();
                int healAmount = (int) Math.ceil(lostHP * 0.4);
                if (healAmount <= 0) {
                    listener.onLog("- " + s.getName() + " — you are already at full health!");
                } else {
                    player.setCurrentHealth(Math.min(player.getMaxHealth(), player.getCurrentHealth() + healAmount));
                    listener.onLog("- " + s.getName() + " restores " + healAmount + " HP (40% of lost HP)!");
                }
                break;
            case "blind":
                enemyBlinded = true;
                listener.onLog(
                        "- " + s.getName() + " — " + enemy.getName() + " is blinded and will miss the next attack!");
                break;
            default:
                enemy.takeDamage(s.getPower() + player.getAtk());
                listener.onLog("- " + enemy.getName() + " takes " + (s.getPower() + player.getAtk()) + " damage!");
                break;
        }

        if (s.getCooldown() > 0) {
            s.triggerCooldown();
        }

        for (Skill skill : player.getSkills()) {
            if (skill != s) {
                skill.reduceCooldown();
            }
        }

        listener.onUpdateSkillButtons(player.getSkills());
        listener.onUpdateHP(player, enemy);

        playerTurn = false;
    }

    public void enemyTurn() {
        if (enemy == null || player == null) {
            return;
        }

        if (!enemy.isAlive()) {
            handleEnemyDefeat();
            return;
        }

        if (burnTurnsRemaining > 0 && enemy.isAlive()) {
            enemy.takeDamage(burnDamageToEnemy);
            burnTurnsRemaining--;
            listener.onLog("- Burn deals " + burnDamageToEnemy + " damage to " + enemy.getName() + " ("
                    + burnTurnsRemaining + " turns remaining).");
            listener.onUpdateHP(player, enemy);
            if (!enemy.isAlive()) {
                handleEnemyDefeat();
                return;
            }
        }

        if (enemyBlinded) {
            listener.onLog("- " + enemy.getName() + " is blinded by Shadowveil and misses the attack!");
            enemyBlinded = false;
        } else if (playerDodgeActive) {
            listener.onLog("- You dodge " + enemy.getName() + "'s attack with WindWalk!");
            playerDodgeActive = false;
        } else if (playerShieldTurns > 0) {
            int damage = Math.max(0, enemy.getAtk() - player.getDef());
            int reduced = damage / 2;
            player.setCurrentHealth(player.getCurrentHealth() - reduced);
            listener.onLog("- Time Shield distorts impact! Damage reduced from " + damage + " to " + reduced + ".");
            playerShieldTurns--;
            listener.onUpdateHP(player, enemy);
        } else {
            int damage = Math.max(0, enemy.getAtk() - player.getDef());
            player.setCurrentHealth(player.getCurrentHealth() - damage);
            listener.onLog("- " + enemy.getName() + " attacks! You take " + damage + " damage.");
            listener.onUpdateHP(player, enemy);
        }

        if (burnTurnsRemaining > 0 && enemy.isAlive()) {
            // Already applied summary.
        }

        if (!enemy.isAlive()) {
            handleEnemyDefeat();
            return;
        }

        // Reduce cooldowns at end of enemy turn (complete one full round)
        for (Skill skill : player.getSkills()) {
            skill.reduceCooldown();
        }
        listener.onUpdateSkillButtons(player.getSkills());

        if (player.isAlive()) {
            playerTurn = true;
            listener.onEnableSkillButtons(true);
            listener.onPlayerTurnPrompt();
        } else {
            listener.onLog("- You were defeated...");
            listener.onEnableSkillButtons(false);
            listener.onGameOver();
        }
    }

    private void handleEnemyDefeat() {
        listener.onLog("- You defeated the " + enemy.getName() + "!");
        listener.onEnableSkillButtons(false);

        if ("Tutorial".equals(mode)) {
            if (enemy instanceof Goblin) {
                player.levelUp(0.10, 0.10);
                listener.onShowStory(
                        "The Goblin collapses, dropping a strange sigil...\nFrom the shadows, a hooded Cultist steps forward.");
                enemy = new Cultist();
                listener.onClearBattleLog();
                listener.onUpdateHP(player, enemy);
                listener.onLog("- A new foe approaches: " + enemy.getName() + "!");
                listener.onEnableSkillButtons(true);
                playerTurn = true;
                listener.onUpdateSkillButtons(player.getSkills());
                return;
            }
            if (enemy instanceof Cultist) {
                listener.onStopMusic();
                listener.onShowStory(
                        "The Cultist's whisper fades: 'He... watches from the Rift...'\n\nA surge of energy pulls you through - the Realms shift.");
                mode = "Realm1";
                listener.onPlayMusic("/com/ror/model/Assets/sfx/AetheriaTheme.ogg", true);
                listener.onShowStory(
                        " REALM I: AETHERIA \n\nYou awaken beneath stormy skies - Aetheria.\nSky Serpents circle above, lightning dancing across their scales.");
                listener.onSetBackground("/com/ror/model/Assets/Backgrounds/Aetheria.png");
                enemy = new SkySerpent();
                player.levelUp(0.10, 0.10);
                healBetweenBattles();
                listener.onClearBattleLog();
                listener.onUpdateHP(player, enemy);
                listener.onLog(
                        "- You recall the experience from your fight with tutorial and use it to grow stronger! 💪");
                listener.onLog("- A new foe approaches: " + enemy.getName() + "!");
                listener.onEnableSkillButtons(true);
                playerTurn = true;
                listener.onUpdateSkillButtons(player.getSkills());
                return;
            }
        }

        if ("Realm1".equals(mode)) {
            if (enemy instanceof SkySerpent) {
                listener.onShowStory(
                        "The Sky Serpent bursts into feathers and lightning.\nFrom the thunderclouds above descends General Zephra, Storm Mage of the Rift.");
                enemy = new GeneralZephra();
                player.levelUp(0.15, 0.15);
                healBetweenBattles();
                listener.onClearBattleLog();
                listener.onUpdateHP(player, enemy);
                listener.onLog("- You leveled up!");
                listener.onLog("- A new foe approaches: " + enemy.getName() + "!");
                listener.onEnableSkillButtons(true);
                playerTurn = true;
                listener.onUpdateSkillButtons(player.getSkills());
                return;
            }
            if (enemy instanceof GeneralZephra) {
                listener.onStopMusic();
                listener.onShowStory(
                        "Zephra's thunderbird screeches as lightning fades.\nA fiery rift tears open beneath you...");
                mode = "Realm2";
                listener.onPlayMusic("/com/ror/model/Assets/sfx/IgnaraTheme.ogg", true);
                listener.onSetBackground("/com/ror/model/Assets/Backgrounds/Ignara.png");
                enemy = new MoltenImp();
                healBetweenBattles();
                listener.onUpdateHP(player, enemy);
                listener.onLog("- Realm II: Ignara — molten chaos awaits!");
                listener.onEnableSkillButtons(true);
                playerTurn = true;
                listener.onUpdateSkillButtons(player.getSkills());
                return;
            }
        }

        if ("Realm2".equals(mode)) {
            if (enemy instanceof MoltenImp) {
                player.levelUp(0.10, 0.10);
                listener.onLog("- LEVEL UP!!!");
                listener.onShowStory(
                        "The last Molten Imp bursts into flame...\nFrom the magma rises General Vulkrag, the Infernal Commander!");
                enemy = new GeneralVulkrag();
                healBetweenBattles();
                listener.onUpdateHP(player, enemy);
                listener.onLog("- A new foe approaches: " + enemy.getName() + "!");
                listener.onEnableSkillButtons(true);
                playerTurn = true;
                listener.onUpdateSkillButtons(player.getSkills());
                return;
            }
            if (enemy instanceof GeneralVulkrag) {
                listener.onStopMusic();
                listener.onShowStory(
                        "Vulkrag's molten armor cracks apart.\nDarkness seeps in from the edges of reality...");
                mode = "Realm3";
                listener.onPlayMusic("/com/ror/model/Assets/sfx/NoxterraTheme.ogg", true);
                listener.onSetBackground("/com/ror/model/Assets/Backgrounds/Noxterra.png");
                enemy = new ShadowCreeper();
                player.levelUp(0.15, 0.15);
                healBetweenBattles();
                listener.onUpdateHP(player, enemy);
                listener.onLog("- You noticeable feel stronger after defeating a general! 💪");
                listener.onLog("- Realm III: Noxterra — the shadows hunger...");
                listener.onEnableSkillButtons(true);
                playerTurn = true;
                listener.onUpdateSkillButtons(player.getSkills());
                return;
            }
        }

        if ("Realm3".equals(mode)) {
            if (enemy instanceof ShadowCreeper) {
                listener.onStopMusic();
                listener.onShowStory(
                        "The Shadow Creeper dissolves into mist...\nA dark laughter echoes — the Rift Lord himself descends.");
                enemy = new Vorthnar();
                player.levelUp(0.20, 0.20);
                healBetweenBattles();
                listener.onUpdateHP(player, enemy);
                listener.onLog("- You feel a surge of power course through you!");
                listener.onLog("- The final boss approaches: " + enemy.getName() + "!");
                listener.onEnableSkillButtons(true);
                playerTurn = true;
                listener.onUpdateSkillButtons(player.getSkills());
                listener.onPlaySoundThen("/com/ror/model/Assets/sfx/laugh.wav",
                        () -> listener.onPlayMusic("/com/ror/model/Assets/sfx/Vorthar.ogg", true));
                return;
            }
            if (enemy instanceof Vorthnar) {
                // listener.onShowStory(
                // "Vorthnar collapses — time itself shatters, then reforms.\n\n🏆 CHAPTER III
                // COMPLETE 🏆\nYou have conquered the Realms!");
                listener.onLog("🎉 You defeated Lord Vorthnar! Chapter III complete!");
                listener.onEnableSkillButtons(false);
                listener.onGameWin();
            }
        }
    }

    private void healBetweenBattles() {
        int healAmount = player.getMaxHealth();
        player.setCurrentHealth(Math.min(player.getMaxHealth(), player.getCurrentHealth() + healAmount));
        listener.onUpdateHP(player, enemy);
        listener.onLog("- You have recovered your vitality for the next battle!");
    }
}
