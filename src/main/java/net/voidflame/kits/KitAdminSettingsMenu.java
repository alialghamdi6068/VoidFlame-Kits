package net.voidflame.kits;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.Locale;

public final class KitAdminSettingsMenu implements Listener {
    private final VoidFlameKitsPlugin plugin;
    public KitAdminSettingsMenu(VoidFlameKitsPlugin plugin){this.plugin=plugin;}
    public void open(Player p,String kit){
        Inventory inv=Bukkit.createInventory(new Holder(kit),54,"§8VoidFlame §7• §5Kit • "+plugin.catalog().displayName(kit));
        for(int i=0;i<54;i++) inv.setItem(i,item(Material.BLACK_STAINED_GLASS_PANE," "));
        button(inv,10,Material.CHEST,"§d§lEDIT LAYOUT","§7Open the full inventory editor.");
        button(inv,11,Material.NAME_TAG,"§d§lRENAME","§7Rename this kit.");
        button(inv,12,Material.WRITABLE_BOOK,"§d§lLORE","§7Set lore using | between lines.");
        button(inv,13,plugin.catalog().icon(kit),"§d§lICON","§7Choose a new icon.");
        button(inv,14,Material.COMPARATOR,"§d§lORDER","§7Change ordering number.");
        button(inv,15,Material.CHEST_MINECART,"§b§lDUPLICATE","§7Create a disabled copy.");
        button(inv,16,plugin.catalog().enabled(kit)?Material.LIME_DYE:Material.GRAY_DYE,plugin.catalog().enabled(kit)?"§a§lENABLED":"§c§lDISABLED","§7Toggle availability.");
        button(inv,19,Material.GOLDEN_APPLE,"§6§lDEFAULT","§7Set as default for new players.");
        button(inv,20,Material.ENDER_EYE,"§e§lPREVIEW","§7Preview metadata.");
        button(inv,21,Material.REDSTONE_BLOCK,"§c§lDELETE","§7Delete custom kit.");
        button(inv,49,Material.ARROW,"§7§lBACK"); button(inv,53,Material.BARRIER,"§c§lCLOSE"); p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p)||!(e.getView().getTopInventory().getHolder() instanceof Holder h))return;
        e.setCancelled(true); if(e.getClickedInventory()!=e.getView().getTopInventory())return; String k=h.kit();
        switch(e.getRawSlot()){
            case 10->plugin.editor().openAdmin(p,k);
            case 11->p.performCommand("kit admin");
            case 12->p.sendMessage("§dKit Lore §7» §fUse the catalog configuration for lore lines: "+String.join(" | ",plugin.catalog().lore(k)));
            case 13->p.sendMessage("§dKit Icon §7» §fCurrent: "+plugin.catalog().icon(k).name());
            case 14->p.sendMessage("§dKit Order §7» §fCurrent: "+plugin.catalog().order(k));
            case 15->{String id=k+"_copy"; if(plugin.catalog().add(id)){plugin.catalog().setDisplayName(id,plugin.catalog().displayName(k)+" Copy");plugin.catalog().setIcon(id,plugin.catalog().icon(k));plugin.catalog().setLore(id,plugin.catalog().lore(k));plugin.catalog().setOrder(id,plugin.catalog().order(k)+1);plugin.catalog().setEnabled(id,false);plugin.saveCatalog();open(p,id);} }
            case 16->{plugin.catalog().setEnabled(k,!plugin.catalog().enabled(k));plugin.saveCatalog();open(p,k);}
            case 19->{plugin.setDefaultKit(k);open(p,k);}
            case 20->p.sendMessage("§5VOIDFLAME §8» §f"+plugin.catalog().displayName(k)+" §7| "+String.join(" §8/ ",plugin.catalog().lore(k)));
            case 21->{if(KitCatalog.KITS.contains(k)){p.sendMessage("§cBuilt-in kits cannot be deleted.");}else if(plugin.catalog().remove(k)){plugin.saveCatalog();p.performCommand("kit admin");}}
            case 49->p.performCommand("kit admin"); case 53->p.closeInventory(); default->{}
        }
    }
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Holder)e.setCancelled(true);}
    private void button(Inventory i,int s,Material m,String n,String...l){i.setItem(s,item(m,n,l));}
    private ItemStack item(Material m,String n,String...l){ItemStack x=new ItemStack(m);ItemMeta z=x.getItemMeta();if(z!=null){z.setDisplayName(n);z.setLore(List.of(l));x.setItemMeta(z);}return x;}
    private record Holder(String kit) implements InventoryHolder{public Inventory getInventory(){return null;}}
}
