
package haven.infinity;

import java.util.*;

import haven.*;
import haven.MiniMap.DisplayIcon;

public class Autologout {
	private MiniMap minimap;
	private Set<Long> seen = new HashSet<>();
	
	public Autologout(MiniMap minimap) {
		this.minimap = minimap;
	}
	
	public void check(DisplayIcon icon) {
		if (!CFG.AUTOLOGOUT.get()) {
			return;
		}
		
		if (icon.isPlayer() && icon.kin() == null && !seen.contains(icon.gob.id)) {
			this.seen.add(icon.gob.id);
			this.minimap.gameui().act("lo");
		}
	}	
}
