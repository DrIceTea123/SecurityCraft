package net.geforcemods.securitycraft.util;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import net.geforcemods.securitycraft.entity.SecurityCameraEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;

public class PlayerUtils{

	/**
	 * Gets the PlayerEntity instance of a player (if they're online) using their name. <p>
	 */
	public static Player getPlayerFromName(String name){
		if(EffectiveSide.get() == LogicalSide.CLIENT){
			List<AbstractClientPlayer> players = Minecraft.getInstance().level.players();
			Iterator<?> iterator = players.iterator();

			while(iterator.hasNext()){
				Player tempPlayer = (Player) iterator.next();
				if(tempPlayer.getName().getString().equals(name))
					return tempPlayer;
			}

			return null;
		}else{
			List<?> players = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
			Iterator<?> iterator = players.iterator();

			while(iterator.hasNext()){
				Player tempPlayer = (Player) iterator.next();
				if(tempPlayer.getName().getString().equals(name))
					return tempPlayer;
			}

			return null;
		}
	}

	/**
	 * Returns true if a player with the given name is in the world.
	 */
	public static boolean isPlayerOnline(String name) {
		if(EffectiveSide.get() == LogicalSide.CLIENT){
			for(AbstractClientPlayer player : Minecraft.getInstance().level.players()){
				if(player != null && player.getName().getString().equals(name))
					return true;
			}

			return false;
		}
		else
			return (ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(name) != null);
	}

	public static void sendMessageToPlayer(String playerName, MutableComponent prefix, MutableComponent text, ChatFormatting color){
		Player player = getPlayerFromName(playerName);

		if (player != null)
			sendMessageToPlayer(player, prefix, text, color, false);
	}

	public static void sendMessageToPlayer(Player player, MutableComponent prefix, MutableComponent text, ChatFormatting color) {
		sendMessageToPlayer(player, prefix, text, color, false);
	}

	public static void sendMessageToPlayer(Player player, MutableComponent prefix, MutableComponent text, ChatFormatting color, boolean shouldSendFromClient){
		if (player.level.isClientSide == shouldSendFromClient) {
			player.sendMessage(new TextComponent("[")
					.append(prefix.setStyle(Style.EMPTY.withColor(color)))
					.append(new TextComponent("] ")).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE))
					.append(text), Util.NIL_UUID); //appendSibling
		}
	}

	/**
	 * Sends the given {@link ICommandSource} a chat message, followed by a link prefixed with a colon. <p>
	 */
	public static void sendMessageEndingWithLink(CommandSource sender, MutableComponent prefix, MutableComponent text, String link, ChatFormatting color){
		sender.sendMessage(new TextComponent("[")
				.append(prefix.setStyle(Style.EMPTY.withColor(color)))
				.append(new TextComponent("] ")).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE))
				.append(text)
				.append(new TextComponent(": "))
				.append(ForgeHooks.newChatWithLinks(link)), Util.NIL_UUID); //appendSibling
	}

	/**
	 * Returns true if the player is holding the given item.
	 */
	public static boolean isHoldingItem(Player player, Supplier<Item> item, InteractionHand hand){
		return isHoldingItem(player, item.get(), hand);
	}

	/**
	 * Returns true if the player is holding the given item.
	 * @param player The player that is checked for the item
	 * @param item The item that is checked
	 * @param hand The hand in which the item should be; if hand is null, both hands are checked
	 * @return true if the item was found in the mainhand or offhand, or if no item was found and item was null
	 */
	public static boolean isHoldingItem(Player player, Item item, InteractionHand hand){
		if (hand != InteractionHand.OFF_HAND && !player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == item)
				return true;
		}

		if (hand != InteractionHand.MAIN_HAND && !player.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) {
			if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() == item)
				return true;
		}

		return item == null;
	}

	/**
	 * Returns the ItemStack of the given item the player is currently holding (both hands are checked).
	 * @param player The player holding the item
	 * @param item The item type that should be searched for
	 * @return The item stack if it has been found, ItemStack.EMPTY if not
	 */
	public static ItemStack getSelectedItemStack(Player player, Item item) {
		return getSelectedItemStack(player.inventory, item);
	}

	/**
	 * Returns the ItemStack of the given item the player is currently holding (both hands are checked).
	 * @param inventory The inventory that contains the item
	 * @param item The item type that should be searched for
	 * @return The respective item stack if it has been found, ItemStack.EMPTY if not
	 */
	public static ItemStack getSelectedItemStack(Inventory inventory, Item item) {
		if (!inventory.getSelected().isEmpty()) {
			if (inventory.getSelected().getItem() == item)
				return inventory.getSelected();
		}

		if (!inventory.offhand.get(0).isEmpty()) {
			if (inventory.offhand.get(0).getItem() == item)
				return inventory.offhand.get(0);
		}

		return ItemStack.EMPTY;
	}

	/**
	 * Is the entity mounted on to a security camera?
	 */
	public static boolean isPlayerMountedOnCamera(LivingEntity entity) {
		return entity != null && entity.getVehicle() instanceof SecurityCameraEntity;
	}
}
