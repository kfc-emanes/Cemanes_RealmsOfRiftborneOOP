package com.ror.model.Playable;

import com.ror.model.*;

public class SkyMage extends Entity {

    public SkyMage() {
        // name, maxHealth, currentHealth, atk, def
        super("Flashley the Wind Whisperer", 120, 120, 25, 14);

        Skill tempestGale = new Skill("Tempest Gale", 22, "Attack", 1);
        Skill featherBarrier = new Skill("Feather Barrier", 0, "Heal", 2);
        Skill windwalk = new Skill("Windwalk", 0, "Dodge", 2);

        setSkills(new Skill[] { tempestGale, featherBarrier, windwalk });
    }
}
