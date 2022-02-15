package ru.flinbein.chatmvc.example;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ru.flinbein.chatmvc.controller.MVCController;

public class ExampleInventoryController extends MVCController {

    public String mode = "inventoryView";
    public int invPage = 0;
    public ItemStack selectedItem = null;

    public Player getPlayer() {return (Player) this.commandSender;}
    public int getInvPage() {return invPage;}
    public String getMode() {return mode;}
    public ItemStack getSelectedItem() {return selectedItem;}

    private void rerender() {
        render("templates/controller_example.ftlx");
    }

    public void nextPage(Object[] params, String[] texts) {
        int currentPage = (int) params[0];
        changePage(currentPage+1);
    }
    public void prevPage(Object[] params, String[] texts) {
        int currentPage = (int) params[0];
        changePage(currentPage-1);
    }

    public void selectItem(Object[] params, String[] texts) {
        int itemSlot = (int) params[0];
        var item = getPlayer().getInventory().getItem(itemSlot);
        if (item == null) return;
        mode = "itemView";
        selectedItem = item;
        rerender();
    }

    public void backToInventory(Object[] params, String[] texts) {
        selectedItem = null;
        mode = "inventoryView";
        rerender();
    }

    public void changePage(int page) {
        invPage = page;
        if (invPage < 0) invPage = 0;
        else if (invPage > 3) invPage = 3;
        rerender();
    }

    @Override
    public void register(CommandSender sender, Plugin plugin, String commandPrefixWithId) {
        super.register(sender, plugin, commandPrefixWithId);
    }
}
