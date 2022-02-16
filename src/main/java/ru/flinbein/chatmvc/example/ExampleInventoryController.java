package ru.flinbein.chatmvc.example;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.flinbein.chatmvc.controller.Bind;
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

    @Bind()
    public void prevPage(int currentPage) {
        changePage(currentPage-1);
    }

    @Bind()
    public void nextPage(int currentPage) {
        changePage(currentPage+1);
    }

    @Bind()
    public void backToInventory() {
        selectedItem = null;
        mode = "inventoryView";
        rerender();
    }

    @Bind()
    public void selectItem(int itemSlot) {
        var item = getPlayer().getInventory().getItem(itemSlot);
        if (item == null) return;
        mode = "itemView";
        selectedItem = item;
        rerender();
    }

    @Bind()
    private void changePage(int page) {
        invPage = page;
        if (invPage < 0) invPage = 0;
        else if (invPage > 3) invPage = 3;
        rerender();
    }
}
