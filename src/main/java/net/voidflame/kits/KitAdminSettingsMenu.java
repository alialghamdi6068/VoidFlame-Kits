package net.voidflame.kits;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
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
            case 11->input(p,k,"RENAME",plugin.catalog().displayName(k));
            case 12->input(p,k,"LORE",String.join("|",plugin.catalog().lore(k)));
            case 13->icons(p,k);
            case 14->input(p,k,"ORDER",Integer.toString(plugin.catalog().order(k)));
            case 15->input(p,k,"DUPLICATE",k+"_copy");
            case 16->{plugin.catalog().setEnabled(k,!plugin.catalog().enabled(k));plugin.saveCatalog();open(p,k);}
            case 19->{plugin.setDefaultKit(k);open(p,k);}
            case 20->p.sendMessage("§5VOIDFLAME §8» §f"+plugin.catalog().displayName(k)+" §7| "+String.join(" §8/ ",plugin.catalog().lore(k)));
            case 21->{if(KitCatalog.KITS.contains(k)){p.sendMessage("§cBuilt-in kits cannot be deleted.");}else if(plugin.catalog().remove(k)){plugin.saveCatalog();p.performCommand("kit admin");}}
            case 49->p.performCommand("kit admin"); case 53->p.closeInventory(); default->{}
        }
    }

    private void input(Player p,String kit,String mode,String initial){Inventory i=Bukkit.createInventory(new InputHolder(kit,mode),InventoryType.ANVIL,"§8VoidFlame §7• §5"+mode);ItemStack x=new ItemStack(Material.PAPER);ItemMeta m=x.getItemMeta();if(m!=null){m.setDisplayName(initial);x.setItemMeta(m);}i.setItem(0,x);p.openInventory(i);}
    private void icons(Player p,String kit){Inventory i=Bukkit.createInventory(new IconHolder(kit),54,"§8VoidFlame §7• §5Kit Icon");for(int s=0;s<54;s++)i.setItem(s,item(Material.BLACK_STAINED_GLASS_PANE," "));int s=10;for(Material m:Material.values()){if(!m.isItem())continue;i.setItem(s++,new ItemStack(m));if(s>=44)break;}i.setItem(49,item(Material.ARROW,"§7§lBACK"));p.openInventory(i);}
    @EventHandler public void inputClick(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p)||!(e.getView().getTopInventory().getHolder() instanceof InputHolder h)||e.getRawSlot()!=2)return;e.setCancelled(true);ItemStack x=e.getView().getTopInventory().getItem(2);if(x==null||!x.hasItemMeta())return;String v=org.bukkit.ChatColor.stripColor(x.getItemMeta().getDisplayName()).trim();if(v.isBlank())return;String k=h.kit();switch(h.mode()){case "RENAME"->plugin.catalog().setDisplayName(k,v);case "LORE"->plugin.catalog().setLore(k,java.util.Arrays.stream(v.split("\\|")).map(String::trim).filter(z->!z.isBlank()).limit(8).toList());case "ORDER"->{try{plugin.catalog().setOrder(k,Integer.parseInt(v));}catch(NumberFormatException ex){p.sendMessage("§cOrder must be a number.");return;}}case "DUPLICATE"->{String id=v.toLowerCase(java.util.Locale.ROOT).replace(' ','_');if(!plugin.catalog().add(id)){p.sendMessage("§cInvalid or duplicate kit ID.");return;}plugin.catalog().setDisplayName(id,plugin.catalog().displayName(k)+" Copy");plugin.catalog().setIcon(id,plugin.catalog().icon(k));plugin.catalog().setLore(id,plugin.catalog().lore(k));plugin.catalog().setEnabled(id,false);plugin.saveCatalog();open(p,id);return;}default->{} }plugin.saveCatalog();open(p,k);}
    @EventHandler public void iconClick(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p)||!(e.getView().getTopInventory().getHolder() instanceof IconHolder h))return;e.setCancelled(true);if(e.getRawSlot()==49){open(p,h.kit());return;}ItemStack x=e.getCurrentItem();if(x==null||!x.getType().isItem())return;plugin.catalog().setIcon(h.kit(),x.getType());plugin.saveCatalog();open(p,h.kit());}
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Holder)e.setCancelled(true);}
    private void button(Inventory i,int s,Material m,String n,String...l){i.setItem(s,item(m,n,l));}
    private ItemStack item(Material m,String n,String...l){ItemStack x=new ItemStack(m);ItemMeta z=x.getItemMeta();if(z!=null){z.setDisplayName(n);z.setLore(List.of(l));x.setItemMeta(z);}return x;}
    private record Holder(String kit) implements InventoryHolder{public Inventory getInventory(){return null;}}\n    private record InputHolder(String kit,String mode) implements InventoryHolder{public Inventory getInventory(){return null;}}\n    private record IconHolder(String kit) implements InventoryHolder{public Inventory getInventory(){return null;}}
}
