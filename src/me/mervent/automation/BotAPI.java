package me.mervent.automation;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import haven.*;

public class BotAPI {
	private GameUI gui;
	public BotAPI(GameUI gui) {
		this.gui = gui;
	}
	
	// GENERAL
	public WItem getItemAtHand() {
		return gui.vhand;
	}
	
        // INVENTORY
	public  List<WItem> getInventoryItems(Inventory inventory) {
		return inventory.children(WItem.class)
                .stream()
                .collect(Collectors.toList());
	}

	public List<WItem> getInventoryItemsByName(Inventory inventory, String pattern) {
		Pattern pat = Pattern.compile(pattern);
		return inventory.children(WItem.class).stream()
				.filter(item -> {
					String name = getItemName(item);
					return (name != null && pat.matcher(name).matches());
				})
				.collect(Collectors.toList());
	}

	public List<WItem> getInventoryItemsByResName(Inventory inventory, String pattern) {
		Pattern pat = Pattern.compile(pattern);
		return inventory.children(WItem.class).stream()
				.filter(item -> {
					String resname = getItemResName(item);
					return (resname != null && pat.matcher(resname).matches());
				})
				.collect(Collectors.toList());
	}
	public WItem getInventoryItemByLocation(Inventory inventory, int x, int y) {
		for(WItem witem : inventory.children(WItem.class)) {
			if(witem.c.div(UI.scale(33 * Utils.getprefd("uiscale", 1.0))).x == x && witem.c.div(UI.scale(33 * Utils.getprefd("uiscale", 1.0))).y == y) {
				return witem;
			}
		}
		return null;
	}
        public String getItemName(WItem item) {
		while(true) {
			try {
				synchronized(item.ui) {
					for(Object o : item.info().toArray()) {
						if(o instanceof ItemInfo.Name)
							return ((ItemInfo.Name) o).str.text;
					}
					break;
				}
			} catch(Loading l) { }
			sleep(20);
		}
		return null;
	}
        
        public String getItemResName(WItem item) {
		while(true) {
			try {
				Resource res = item.item.getres();
				if(res == null)
					return null;
				else
					return res.name;
			} catch(Loading l) {
			}
		}
	}
	public Double getItemQuality(WItem item) {
		return item.item.quality();
	}
	public Coord getItemLocation(WItem item) {
		return item.c.div(33 * Utils.getprefd("uiscale", 1.0));
	}		
	public Coord getSize(Inventory inventory) {
		return inventory.isz;
	}
	public void takeItem(WItem item, boolean wait) {
		item.take();
		if (wait) {
			while(getItemAtHand() == null) {
				sleep(25);
			}
		}
	}
	public void dropItem(Inventory inventory, int x, int y) {
		inventory.wdgmsg("drop", new Coord(x, y));		
	}
	public Coord getSize(WItem item) {
		try {
			Indir<Resource> res = item.item.getres().indir();
			if(res.get() != null && res.get().layer(Resource.imgc) != null) {
				Tex tex = res.get().layer(Resource.imgc).tex();
				if(tex == null)
					return new Coord(1, 1);
				else
					return UI.unscale(tex.sz()).div(30);
			} else {
				return new Coord(1, 1);
			}
		} catch(Loading l) {

		}
		return new Coord(1, 1);
	}
	
	// UTILITY
	public void msg(String msg) {
		gui.msg(msg);
	}
        public void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
}
