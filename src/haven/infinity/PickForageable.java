package haven.infinity;

import haven.*;

import static haven.OCache.posres;

import java.util.*;

public class PickForageable implements Runnable {
        private GameUI gui;
        private HashSet<String> collectables = new HashSet<String>(Arrays.asList(
            "adder",
            "arrow",
            "blueberry",
            "boarspear",
            "boostspeed",
            "bram",
            "cart",
            "cattail",
            "cavemoth",
            "chantrelle",
            "chick",
            "chicken",
            "chives",
            "clover",
            "coltsfoot",
            "crab",
            "dandelion",
            "dragonfly",
            "duskfern",
            "firefly",
            "forestlizard",
            "forestsnail",
            "frog",
            "frogspawn",
            "grasshopper",
            "grub",
            "hedgehog",
            "irrbloss",
            "jellyfish",
            "ladybug",
            "ladysmantle",
            "lampstalk",
            "lingon",
            "mallard",
            "mistletoe",
            "mole",
            "mussels",
            "opiumdragon",
            "precioussnowflake",
            "rabbit",
            "rat",
            "rowboat",
            "rustroot",
            "sandflea",
            "silkmoth",
            "snapdragon",
            "spindlytaproot",
            "squirrel",
            "stingingnettle",
            "thornythistle",
            "toad",
            "wagon",
            "waterstrider",
            "wball",
            "wheelbarrow",
            "windweed",
            "yellowfoot"
        ));
    
        public PickForageable(GameUI gui) {
            this.gui = gui;
        }
    
        @Override
        public void run() {
            Gob herb = null;
            synchronized (gui.map.glob.oc) {
                for (Gob gob : gui.map.glob.oc) {
                    Resource res = null;
                    try {
                        res = gob.getres();
                    } catch (Loading l) {
                    }
                    if (res != null) {
                        Boolean pick = false;
                        
                        if (res.name.startsWith("gfx/terobjs/herbs") && !res.name.startsWith("gfx/terobjs/vehicle")) {
                            pick = true;
                        }
    
                        if (this.collectables.contains(res.basename())) {
                            pick = true;
                        }
    
                        if (pick) {
                            double distFromPlayer = gob.rc.dist(gui.map.player().rc);
                            if (distFromPlayer <= 20 * 11
                                    && (herb == null || distFromPlayer < herb.rc.dist(gui.map.player().rc)))
                                herb = gob;
                        }
                    }
                }
            }
            if (herb == null)
                return;
    
            gui.map.wdgmsg("click", Coord.z, herb.rc.floor(posres), 3, 1, 0, (int) herb.id, herb.rc.floor(posres), 0, -1);
        }
}
