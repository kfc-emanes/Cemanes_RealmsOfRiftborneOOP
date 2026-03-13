package com.ror.model.Playable;

import com.ror.model.*;

public class Andrew extends Entity {
    public Andrew() {
        super("Andrew the Timeblade", 115, 115, 18, 5);

        Skill chronoSlash = new Skill("Chrono Slash", 25, "Chrono", 0); // 2-turn cooldown
        Skill timeShield = new Skill("Time Shield", 0, "Shield", 2); // 3-turn cooldown
        Skill reverseFlow = new Skill("Reverse Flow", 0, "Reverse", 3); // 4-turn cooldown

        // set 3 skills
        setSkills(new Skill[] { chronoSlash, timeShield, reverseFlow });
    }
}