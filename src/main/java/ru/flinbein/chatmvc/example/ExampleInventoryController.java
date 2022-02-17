package ru.flinbein.chatmvc.example;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.flinbein.chatmvc.controller.Bind;
import ru.flinbein.chatmvc.controller.Hide;
import ru.flinbein.chatmvc.controller.MVCController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Bind()
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

    public void prevPage(int currentPage) {
        changePage(currentPage-1);
    }

    public void nextPage(int currentPage) {
        changePage(currentPage+1);
    }

    public void setPage(String arg) {
        changePage(Integer.parseInt(arg)-1);
    }

    @Hide()
    public List<String> setPageTabComplete(String arg, String[] args) {
        if (arg == null) return null;
        if (args.length > 2) return null;
        int pages = Math.abs(getPlayer().getInventory().getStorageContents().length / 9);
        return IntStream.range(1, pages+1).mapToObj(Integer::toString).collect(Collectors.toList());
    }

    public void backToInventory() {
        selectedItem = null;
        mode = "inventoryView";
        rerender();
    }

    public void selectItem(int itemSlot) {
        var item = getPlayer().getInventory().getItem(itemSlot);
        if (item == null) return;
        mode = "itemView";
        selectedItem = item;
        rerender();
    }

    private void changePage(int page) {
        invPage = page;
        if (invPage < 0) invPage = 0;
        else if (invPage > 3) invPage = 3;
        rerender();
    }
}
