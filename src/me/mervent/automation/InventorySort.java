package me.mervent.automation;

import java.util.*;
import java.util.regex.Pattern;

import haven.*;

public class InventorySort {
        private GameUI gui;
        private BotAPI api;
        private Pattern pattern = Pattern.compile("Cupboard|Chest|Crate|Woodbox|Basket");
        private Comparator<WItem> ITEM_COMPARATOR = new Comparator<WItem>() {
                @Override
                public int compare(WItem o1, WItem o2) {
                        if (api.getItemName(o1).compareTo(api.getItemName(o2)) == 0) {
                                QualityList ql1 = o1.itemq.get();
                                double q1 = (ql1 != null && !ql1.isEmpty()) ? ql1.single().value : 0;

                                QualityList ql2 = o2.itemq.get();
                                double q2 = (ql2 != null && !ql2.isEmpty()) ? ql2.single().value : 0;

                                return Double.compare(q1, q2);
                        }
                        return api.getItemName(o1).compareTo(api.getItemName(o2));

                }
        };

        public InventorySort(GameUI gui) {
                this.gui = gui;
                this.api = new BotAPI(gui);
        }

        public void run() {
                new Thread(() -> {
                        new InventorySort(gui)._run();
                }, "Sort").start();
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
                        Boolean[][] grid = new Boolean[api.getSize(inv).x][api.getSize(inv).y];

                        List<WItem> items = api.getInventoryItems(inv);
                        Coord inventorySize = api.getSize(inv);
                        items.sort(ITEM_COMPARATOR);
                        int x = 0;
                        int y = 0;
                        api.msg(Integer.toString(inventorySize.x));
                        api.msg(Integer.toString(inventorySize.y));
                        for (WItem item : items) {

                                // Coord location = api.getItemLocation(item);

                                Coord location = api.getItemLocation(item);
                                api.msg(api.getItemName(item) + ": take at " + Integer.toString(location.x) + ","
                                                + Integer.toString(location.y));
                                api.takeItem(item, true);
                                api.sleep(100);

                                api.msg(api.getItemName(item) + ": drop to " + Integer.toString(x) + ","
                                                + Integer.toString(y));
                                api.dropItem(inv, x, y, true);
                                api.sleep(100);

                                api.msg(api.getItemName(item) + ": back to " + Integer.toString(location.x) + ","
                                                + Integer.toString(location.y));
                                api.dropItem(inv, location.x, location.y, true);
                                api.sleep(100);

                                // api.msg(api.getItemName(item) + " back to " + Integer.toString(x) + "," +
                                // Integer.toString(y));
                                // if (api.getItemAtHand() != null) {
                                // api.msg(api.getItemName(api.getItemAtHand()) + " move to " +
                                // Integer.toString(x) + "," + Integer.toString(y));
                                // api.drop(inv, prevLocation.x, prevLocation.y);
                                // }

                                x += 1;
                                if (x == inventorySize.x) {
                                        x = 0;
                                        y += 1;
                                }

                        }
                }

        }
}
