package me.mervent.automation;

import java.util.*;
import java.util.regex.Pattern;

import haven.*;

public class InventorySort {
        private GameUI gui;
        private BotAPI bot;
        private Pattern pattern = Pattern.compile("Cupboard|Chest|Crate|Woodbox|Basket");
        private Comparator<WItem> ITEM_COMPARATOR = new Comparator<WItem>() {
                @Override
                public int compare(WItem o1, WItem o2) {
                        if (bot.getItemName(o1).compareTo(bot.getItemName(o2)) == 0) {
                                QualityList ql1 = o1.itemq.get();
                                double q1 = (ql1 != null && !ql1.isEmpty()) ? ql1.single().value : 0;

                                QualityList ql2 = o2.itemq.get();
                                double q2 = (ql2 != null && !ql2.isEmpty()) ? ql2.single().value : 0;

                                return Double.compare(q1, q2);
                        }
                        return bot.getItemName(o1).compareTo(bot.getItemName(o2));

                }
        };

        public InventorySort(GameUI gui) {
                this.gui = gui;
                this.bot = new BotAPI(gui);
        }

        public void run() {
                this._run();

                // new Thread(() -> {
                // new InventorySort(gui)._run();
                // }, "Sort").start();
        }

        public void _run() {
                ArrayList<Inventory> inventories = new ArrayList<>();

                for (Window w : gui.children(Window.class)) {
                        if (pattern.matcher(w.cap.text).matches()) {
                                for (Inventory i : w.children(Inventory.class)) {
                                        inventories.add(i);
                                }

                        }
                }

                for (Inventory inv : inventories) {
                        int sizeX = bot.getSize(inv).x;
                        int sizeY = bot.getSize(inv).y;
                        Boolean[][] grid = new Boolean[sizeY][sizeX];

                        bot.msg(Integer.toString(sizeX));
                        bot.msg(Integer.toString(sizeY));

                        List<WItem> items = bot.getInventoryItems(inv);
                        items.sort(ITEM_COMPARATOR);
                        
                        for (int row = 0; row < grid.length; row++) {
                                for (int col = 0; col < grid[row].length; col++) {
                                        int idx = col + sizeX * row;
                                        if (idx >= items.size()) {
                                                return;
                                        }
                                        bot.msg(Integer.toString(row) + "," + Integer.toString(col));
                                        bot.msg("IDX " + Integer.toString(idx));

                                        WItem item = items.get(idx);
                                        Coord location = bot.getItemLocation(item);
                                        bot.msg(bot.getItemName(item) + Double.toString(bot.getItemQuality(item)));

                                        bot.takeItem(item, false);
                                        bot.dropItem(inv, col, row);
                                        bot.dropItem(inv, location.x, location.y);
                                }
                        }

                        // for (WItem item : items) {

                        // // Coord location = api.getItemLocation(item);

                        // Coord location = bot.getItemLocation(item);
                        // bot.msg(bot.getItemName(item) + ": take at " + Integer.toString(location.x) +
                        // ","
                        // + Integer.toString(location.y));
                        // bot.msg(bot.getItemName(item) + ": take at " + Integer.toString(location.x) +
                        // ","
                        // + Integer.toString(location.y));
                        // bot.takeItem(item, false);
                        // // bot.sleep(250);

                        // bot.msg(bot.getItemName(item) + ": drop to " + Integer.toString(x) + ","
                        // + Integer.toString(y));
                        // bot.dropItem(inv, x, y);
                        // // bot.sleep(250);

                        // if (bot.getItemAtHand() != null) {
                        // bot.msg(bot.getItemName(item) + ": back to " + Integer.toString(location.x) +
                        // ","
                        // + Integer.toString(location.y));
                        // bot.dropItem(inv, location.x, location.y);
                        // // bot.sleep(250);
                        // }

                        // // api.msg(api.getItemName(item) + " back to " + Integer.toString(x) + "," +
                        // // Integer.toString(y));
                        // // if (api.getItemAtHand() != null) {
                        // // api.msg(api.getItemName(api.getItemAtHand()) + " move to " +
                        // // Integer.toString(x) + "," + Integer.toString(y));
                        // // api.drop(inv, prevLocation.x, prevLocation.y);
                        // // }

                        // x += 1;
                        // if (x == inventorySize.x) {
                        // x = 0;
                        // y += 1;
                        // }

                        // }
                }

        }
}
