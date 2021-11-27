package me.mervent;

import auto.Bot;
import haven.GameUI;
import haven.IMeter;

public class Drinker {
	private GameUI gui;
	private long lastDrinkTime;
	private double lastTickStamina;

	public Drinker(GameUI gui) {
		this.gui = gui;
	}
	
	public void tick() {
		try {
			IMeter.Meter stam = gui.meterGetter.get("stam", 0);
			
			if (stam.a < 0.6) {
				if (!isDrinking() && System.currentTimeMillis() - lastDrinkTime >= 2 * 1000) {
					lastDrinkTime = System.currentTimeMillis();
					drink();
				}
			}
		} catch (Exception e) {
		}
	}

	private boolean isDrinking() {
		IMeter.Meter stam = gui.meterGetter.get("stam", 0);
		if (stam.a > lastTickStamina) {
			lastTickStamina = stam.a;
			return true;
		}
		lastTickStamina = stam.a;		
		return false;
	}
	private void drink() {
		Bot.drink(gui);
	}
}
