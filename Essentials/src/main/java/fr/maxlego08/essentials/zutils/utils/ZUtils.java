package fr.maxlego08.essentials.zutils.utils;

import fr.maxlego08.essentials.api.commands.Permission;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;

public abstract class ZUtils extends MessageUtils {

    protected boolean hasPermission(CommandSender sender, Permission permission) {
        return sender.hasPermission(permission.asPermission());
    }

    protected String name(String string) {
        String name = string.replace("_", " ").toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    protected Optional<Location> topLocation(Location location, int step, int y) {

        if (step > location.getWorld().getMaxHeight()) {
            return Optional.empty();
        }

        location.setY(y);
        if (!location.getBlock().getType().isSolid() && !location.getBlock().getRelative(BlockFace.UP).getType().isSolid() && location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
            return Optional.of(location);
        }
        return this.topLocation(location.getBlock().getRelative(BlockFace.UP).getLocation(), step + 1, y - 1);

    }

    protected boolean same(Location l1, Location l2) {
        return (l1.getBlockX() == l2.getBlockX()) && (l1.getBlockY() == l2.getBlockY()) && (l1.getBlockZ() == l2.getBlockZ()) && l1.getWorld().getName().equals(l2.getWorld().getName());
    }

    protected Location toSafeLocation(Location location) {

        Location defaultLocation = location.clone();

        if (isValid(defaultLocation)) {
            return defaultLocation;
        }

        location = findMeSafeLocation(defaultLocation, BlockFace.UP, 1);

        return location;
    }

    protected Location findMeSafeLocation(Location location, BlockFace blockFace, int distance) {

        if (distance > location.getWorld().getMaxHeight() * 2) {
            return null;
        }

        Location location2 = relative(location, blockFace, distance);
        if (isValid(location2)) {
            return location2;
        }

        return findMeSafeLocation(location2, blockFace.equals(BlockFace.UP) ? BlockFace.DOWN : BlockFace.UP,
                distance + 1);
    }

    protected boolean isValid(Location location) {
        return !location.getBlock().getType().isSolid()
                && !relative(location, BlockFace.UP).getBlock().getType().isSolid()
                && relative(location, BlockFace.DOWN).getBlock().getType().isSolid();
    }

    protected Location relative(Location location, BlockFace face) {
        return relative(location, face, 1.0d);
    }

    protected Location relative(Location location, BlockFace face, double distance) {

        Location cloneLocation = location.clone();
        switch (face) {
            case UP -> cloneLocation.setY(cloneLocation.getY() + distance);
            case DOWN -> cloneLocation.setY(cloneLocation.getY() - distance);
            default -> {
            }
        }

        return cloneLocation;
    }

    protected int count(Inventory inventory, Material material) {
        return Arrays.stream(inventory.getContents()).filter(itemStack -> itemStack != null && itemStack.isSimilar(new ItemStack(material))).mapToInt(ItemStack::getAmount).sum();
    }


    protected void removeItems(org.bukkit.inventory.Inventory inventory, ItemStack removeItemStack, int amount) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && itemStack.isSimilar(removeItemStack) && amount > 0) {
                int currentAmount = itemStack.getAmount() - amount;
                amount -= itemStack.getAmount();
                if (currentAmount <= 0) {
                    inventory.removeItem(itemStack);
                } else {
                    itemStack.setAmount(currentAmount);
                }
            }
        }
    }

    protected void give(Player player, ItemStack itemStack) {
        /*if (!player.isOnline() || hasInventoryFull(player)) {
            MailManager.getInstance().addItems(player, itemStack);
        } else {
        }*/
        player.getInventory().addItem(itemStack);
    }

}