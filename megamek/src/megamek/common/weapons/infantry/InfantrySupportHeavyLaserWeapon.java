/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Ben Grills
 */
public class InfantrySupportHeavyLaserWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportHeavyLaserWeapon() {
        super();
        techLevel.put(3071,TechConstants.T_ALL_IS);
        name = "Support Laser (Heavy)";
        setInternalName(name);
        addLookupName("InfantryHeavyLaser");
        addLookupName("InfantryMediumLaser");
        ammoType = AmmoType.T_NA;
        cost = 40000;
        bv = 17.35;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_LASER).or(F_INF_SUPPORT);
        infantryDamage = 1.47;
        infantryRange = 5;
        crew = 3;
        introDate = 2405;
        availRating = new int[]{RATING_E,RATING_E,RATING_D};
        techLevel.put(2405,techLevel.get(3071));
        techRating = RATING_D;
    }
}