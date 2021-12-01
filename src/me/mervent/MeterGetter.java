package me.mervent;

import java.util.List;

import haven.*;

public class MeterGetter {
        private GameUI gui;

        public MeterGetter(GameUI gui) {
                this.gui = gui;
        }
        public List<IMeter.Meter> getMany(String name) {
                for (Widget meter : gui.meters) {
                    if (meter instanceof IMeter) {
                        IMeter im = (IMeter) meter;
                        try {
                            Resource res = im.bg.get();
                            if (res != null && res.basename().equals(name))
                                return im.meters;
                        } catch (Loading l) {
                        }
                    }
                }
                return null;
            }
        
        public IMeter.Meter get(String name, int midx) {
        List<IMeter.Meter> meters = getMany(name);
        if (meters != null && midx < meters.size())
                return meters.get(midx);
        return null;
        }
}
