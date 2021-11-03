package me.mervent.automation;

import java.util.*;

import haven.*;

public class InventorySorter {
        private Inventory inventory;
        private BotAPI bot;
        private Comparator<WItem> ITEM_COMPARATOR = new Comparator<WItem>() {
                @Override
                public int compare(WItem o1, WItem o2) {
                        if (bot.getItemName(o1).compareTo(bot.getItemName(o2)) == 0) {
                                QualityList ql1 = o1.itemq.get();
                                double q1 = (ql1 != null && !ql1.isEmpty()) ? ql1.single().value : 0;

                                QualityList ql2 = o2.itemq.get();
                                double q2 = (ql2 != null && !ql2.isEmpty()) ? ql2.single().value : 0;

                                return Double.compare(q2, q1);
                        }
                        return bot.getItemName(o1).compareTo(bot.getItemName(o2));

                }
        };

        public InventorySorter(GameUI gui, Inventory inventory) {
                this.inventory = inventory;
                this.bot = new BotAPI(gui);
        }

        public void sort() {                
                int sizeX = bot.getSize(this.inventory).x;
                int sizeY = bot.getSize(this.inventory).y;
                Boolean[][] grid = new Boolean[sizeY][sizeX];

                bot.msg(Integer.toString(sizeX));
                bot.msg(Integer.toString(sizeY));

                List<WItem> items = bot.getInventoryItems(this.inventory);
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
                                bot.dropItem(this.inventory, col, row);
                                bot.dropItem(this.inventory, location.x, location.y);
                        }
                }

        }
}
