package ua.mvcor.mivex.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.lang.management.ManagementFactory;
import java.util.Random;

public class MivexCommand implements CommandExecutor {

    private final Random random = new Random();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Невідома команда.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("about")) {
            sender.sendMessage(ChatColor.RED + "Використання: /mivex about");
            return true;
        }

        String status = ChatColor.GREEN + "Running ✔";

        if (random.nextInt(100) == 0) {
            String[] easterEggs = {
                    ChatColor.GOLD + "Coffee required ☕",
                    ChatColor.YELLOW + "TODO: Fix bugs...",
                    ChatColor.AQUA + "Works on my machine ✔",
                    ChatColor.LIGHT_PURPLE + "Powered by pizza 🍕"
            };

            status = easterEggs[random.nextInt(easterEggs.length)];
        }

        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        long seconds = uptime / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        sender.sendMessage(ChatColor.DARK_GRAY + "§m----------------------------------------");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "              Mivex Core");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.WHITE + "1.0.0");
        sender.sendMessage(ChatColor.GRAY + "Author: " + ChatColor.LIGHT_PURPLE + "MV CoR");
        sender.sendMessage(ChatColor.GRAY + "API: " + ChatColor.WHITE + "Paper 1.19.4");
        sender.sendMessage(ChatColor.GRAY + "Java: " + ChatColor.WHITE + System.getProperty("java.version"));
        sender.sendMessage(ChatColor.GRAY + "Server: " + ChatColor.WHITE + sender.getServer().getName());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Status: " + status);
        sender.sendMessage(ChatColor.GRAY + "Uptime: " + ChatColor.WHITE + hours + "h " + minutes + "m " + secs + "s");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.DARK_GRAY + "Made with ❤ by MV CoR");
        sender.sendMessage(ChatColor.DARK_GRAY + "Powered by Mivish");
        sender.sendMessage(ChatColor.DARK_GRAY + "§m----------------------------------------");

        return true;
    }
}