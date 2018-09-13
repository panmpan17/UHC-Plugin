package com.gmail.panmpan.MyUHC;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldBorder;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.dumptruckman.minecraft.util.Logging;
import com.onarandombox.MultiverseCore.MVWorld;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVPlugin;

public class MyUHC extends JavaPlugin implements Listener, MVPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");
	private static final String logPrefix = "[MyUHC] ";
	
	Random random = new Random();
	
	private MultiverseCore core;
	
	private BukkitScheduler scheduler = getServer().getScheduler();
	private BukkitTask UHCcountDown = null;
	public boolean UHCStarted = false;
	
	int minimalPlayers = 1;
	int maximalPlayers = 8;
	List<UUID> UHCPlayers = new ArrayList<UUID>();
	Random rand = new Random();
	
	public void onEnable() {
		this.core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
		
		// find Multiverse-Core, if not disable this plugin
		if (this.core == null) {
			this.info("Multiverse-Core not found");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Register this plugin with Multiverse-Core 
		this.info("- Version " + this.getDescription().getVersion() + " Enabled");
		this.core.incrementPluginCount();
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (cmd.getName().equalsIgnoreCase("join")) {
				if (UHCPlayers.contains(player.getUniqueId())) {
					player.sendMessage(ChatColor.DARK_RED + "已經加入 UHC 了!");
				}
				else {
					UHCPlayers.add(player.getUniqueId());
					player.sendMessage(ChatColor.GREEN + "加入 UHC!");

					if (UHCPlayers.size() >= minimalPlayers && this.UHCcountDown == null) {
						startWaiting();
					}
				}
			}
			else if (cmd.getName().equalsIgnoreCase("leave")) {
				if (UHCPlayers.contains(player.getUniqueId())) {
					player.sendMessage(ChatColor.RED + "不在 UHC 了!");
					UHCPlayers.remove(player.getUniqueId());

					if (UHCPlayers.size() < minimalPlayers) {cancelwaitng();}
				}
				else {
					player.sendMessage(ChatColor.DARK_RED + "不在 UHC 了!");
				}
			}
		}
		return true;
	}
	
	private void startWaiting() {
		MVWorld uhcWorld = (MVWorld) this.core.getMVWorldManager().getMVWorld("uhc");
		if (uhcWorld == null) {
			this.info("創建新的 UHC 世界");
			WorldType customized = WorldType.CUSTOMIZED;

			core.getMVWorldManager().addWorld("uhc", Environment.NORMAL, "a", customized, true, "");
		}
		else {
			this.info("UHC 世界已經存在了");
		}
		
		for (UUID playerUUID: UHCPlayers) {
			getServer().getPlayer(playerUUID).sendMessage(ChatColor.GOLD + "UHC 將在 30 後開始");
		}
		
		this.UHCcountDown = this.scheduler.runTaskLater(this, new Runnable() {
			public void run() {
				MVWorld uhcWorld = (MVWorld) core.getMVWorldManager().getMVWorld("uhc");
				setupUHCWorld(uhcWorld);

				UHCStarted = true;
				
				List<Location> randomLocations = randomLocation(uhcWorld);
				int index = 0;
				for (UUID playerUUID: UHCPlayers) {
					getServer().getPlayer(playerUUID).teleport(randomLocations.get(index));
					index++;
				}
			}
		}, 350);
		this.scheduler.runTaskLater(this, new Runnable() {
			public void run() {
				sendCountDown(5, "秒後 UHC 即將開始");
			}
		}, 250);
	}
	
	public void setupUHCWorld(MVWorld uhcWorld) {
		uhcWorld.allowPortalMaking(null);
		uhcWorld.setPVPMode(true);
		uhcWorld.setAllowFlight(false);
		uhcWorld.setAutoHeal(false);
		uhcWorld.setDifficulty(Difficulty.HARD);
		uhcWorld.setGameMode(GameMode.SURVIVAL);
		uhcWorld.setHidden(true);

		World world = uhcWorld.getCBWorld();
		
		Location spawnPoint = uhcWorld.getSpawnLocation();
		WorldBorder worldBorder = world.getWorldBorder();
		worldBorder.setCenter(spawnPoint);
		worldBorder.setSize(450);
		
		world.setGameRuleValue("naturalRegeneration", "false");
	}
	
	public List<Location> randomLocation(MVWorld uhcWorld) {
		List<Location> locations = new ArrayList<Location>();
		
		Location spawnPoint = uhcWorld.getSpawnLocation();
		int borderDistance = (int) uhcWorld.getCBWorld().getWorldBorder().getSize() / 2;
		
		int startX = (int) (spawnPoint.getX() - borderDistance);
		int startZ = (int) (spawnPoint.getZ() - borderDistance);
		
		for (int i=0;i<9;i++) {
			if (i != 5) {
				int randX = rand.nextInt(borderDistance) + startX;
				int randZ = rand.nextInt(borderDistance) + startZ;
				
				locations.add(uhcWorld.getCBWorld().getHighestBlockAt(randX, randZ).getLocation());
				
				startX += borderDistance;
				startZ += borderDistance;
			}
		}
		
		return locations;
	}
	
	public void sendCountDown(int seconds, final String msg) {
		for (int i=0;i<seconds;i++) {
			final int wait = seconds - i;
			this.scheduler.runTaskLater(this, new Runnable() {
				public void run() {
					if (UHCcountDown != null) {
						for (UUID playerUUID: UHCPlayers) {
							getServer().getPlayer(playerUUID).sendTitle(ChatColor.GOLD + String.valueOf(wait), msg, 3, 10, 3);
						}
					}
				}
			}, i * 20);
		}
	}
	
	private void cancelwaitng() {
		for (UUID playerUUID: UHCPlayers) {
			getServer().getPlayer(playerUUID).sendMessage(ChatColor.RED + "UHC 人數不夠，已取消");
		}
		
		this.UHCcountDown.cancel();
		this.UHCcountDown = null;
	}
	
	private void info(String msg) {
		log.info(logPrefix + msg);
	}
	
//	private void infoInt(int msgint) {
//		log.info(String.valueOf(msgint));
//	}
	
	@EventHandler
	public void onPlayerMine(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Material blockType = event.getBlock().getType();
		if (player.getWorld().getName() == "uhc") {
			ItemStack tool = player.getInventory().getItemInMainHand();

			if (blockType == Material.IRON_ORE) {
				if (tool.getType() == Material.STONE_PICKAXE || tool.getType() == Material.IRON_PICKAXE || tool.getType() == Material.DIAMOND_PICKAXE) {
					event.setDropItems(false);
					player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.IRON_INGOT));
				}
			}
			else if (blockType == Material.GOLD_ORE) {
				if (tool.getType() == Material.IRON_PICKAXE || tool.getType() == Material.DIAMOND_PICKAXE) {
					event.setDropItems(false);
					player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.GOLD_INGOT));
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (player.getWorld().getName() == "uhc") {
			this.UHCPlayers.remove(player.getUniqueId());
		}
	}

	public void log(Level level, String msg) {
		Logging.log(level, msg);
	}

	public String dumpVersionInfo(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public MultiverseCore getCore() {
		// TODO Auto-generated method stub
		return this.core;
	}

	public int getProtocolVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setCore(MultiverseCore arg0) {
		// TODO Auto-generated method stub
		
	}

}
