package me.chiller.deathsign;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathSign extends JavaPlugin implements Listener
{
	private List<String> signLines = new ArrayList<String>();
	private List<String> loreLines = new ArrayList<String>();
	
	public void onEnable()
	{
		saveDefaultConfig();
		
		signLines = getConfig().getStringList("sign");
		loreLines = getConfig().getStringList("sign_inventory_hover");
		
		for (int i = 0; i < signLines.size(); i++) signLines.set(i, ChatColor.translateAlternateColorCodes('&', signLines.get(i)));
		for (int i = 0; i < loreLines.size(); i++) loreLines.set(i, ChatColor.translateAlternateColorCodes('&', loreLines.get(i)));
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		Player dead = event.getEntity();
		Entity killer = dead.getKiller();
		
		if (killer instanceof Player)
		{
			Location loc = dead.getLocation();
			
			dropDeathSign(loc, dead.getName(), killer.getName(), new SimpleDateFormat("MMM dd 'at' h:mm").format(new Date(System.currentTimeMillis())));
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(final BlockPlaceEvent event)
	{
		final ItemStack item = event.getItemInHand();
		
		if (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null && item.getItemMeta().getDisplayName().contains("Death Sign"))
		{
			if (event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN)
			{
				event.setCancelled(true);
				event.getPlayer().closeInventory();
				event.getPlayer().setItemInHand(new ItemStack(Material.AIR, 0));
				
				final Material type = event.getBlock().getType();
				final byte data = event.getBlock().getData();
				
				new BukkitRunnable()
				{
					public void run()
					{
						Block block = event.getBlock().getLocation().getBlock();
						
						createSign(block, item, type, data);
					}
				}.runTaskLater(this, 1);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event)
	{
		if (event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN)
		{
			Sign sign = (Sign) event.getBlock().getState();
			
			if (sign.getLine(1).contains("Slain by:"))
			{
				event.setCancelled(true);
				
				String playerName = ChatColor.stripColor(sign.getLine(0));
				String slainBy = ChatColor.stripColor(sign.getLine(2));
				String date = ChatColor.stripColor(sign.getLine(3));
				
				event.getBlock().setType(Material.AIR);
				
				dropDeathSign(event.getBlock().getLocation(), playerName, slainBy, date);
			}
		}
	}
	
	private void dropDeathSign(Location loc, String playerName, String slainBy, String date)
	{
		ItemStack item = new ItemStack(Material.SIGN, 1);
		
		ItemMeta meta = item.getItemMeta();
		List<String> lore = new ArrayList<String>();
		
		for (String loreLine : loreLines)
		{
			lore.add(loreLine.replace("{player}", playerName).replace("{killer}", slainBy).replace("{date}", date));
		}
		
		meta.setLore(lore);
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Death Sign");
		
		item.setItemMeta(meta);
		
		loc.getWorld().dropItemNaturally(loc, item);
	}
	
	@SuppressWarnings("deprecation")
	private void createSign(Block block, ItemStack item, Material type, byte data)
	{
		block.setType(type);
		block.setData(data);
		
		Sign sign = (Sign) block.getState();
		List<String> lore = item.getItemMeta().getLore();
		
		String playerName = ChatColor.stripColor(lore.get(0)).replace("Player: ", "");
		String slainBy = ChatColor.stripColor(lore.get(1)).replace("Slain by: ", "");
		String date = ChatColor.stripColor(lore.get(2)).replace("Date: ", "");
		
		sign.setLine(0, (signLines.size() > 0 ? signLines.get(0) : "").replace("{player}", playerName).replace("{killer}", slainBy).replace("{date}", date));
		sign.setLine(1, (signLines.size() > 1 ? signLines.get(1) : "").replace("{player}", playerName).replace("{killer}", slainBy).replace("{date}", date));
		sign.setLine(2, (signLines.size() > 2 ? signLines.get(2) : "").replace("{player}", playerName).replace("{killer}", slainBy).replace("{date}", date));
		sign.setLine(3, (signLines.size() > 3 ? signLines.get(3) : "").replace("{player}", playerName).replace("{killer}", slainBy).replace("{date}", date));
		
		sign.update();
	}
}