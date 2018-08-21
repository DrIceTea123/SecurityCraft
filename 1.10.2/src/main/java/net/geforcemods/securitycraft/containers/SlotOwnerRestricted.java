package net.geforcemods.securitycraft.containers;

import net.geforcemods.securitycraft.api.IOwnable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotOwnerRestricted extends Slot {

	private final IOwnable tileEntity;
	private final boolean isGhostSlot;

	public SlotOwnerRestricted(IInventory par1iInventory, IOwnable tileEntity, int par2, int par3, int par4, boolean ghostSlot) {
		super(par1iInventory, par2, par3, par4);
		this.tileEntity = tileEntity;
		isGhostSlot = ghostSlot;
	}

	/**
	 * Return whether this slot's stack can be taken from this slot.
	 */
	@Override
	public boolean canTakeStack(EntityPlayer par1EntityPlayer){
		return tileEntity.getOwner().isOwner(par1EntityPlayer) && !isGhostSlot; //the !isGhostSlot check helps to prevent double clicking a stack to pull all items towards the stack
	}

	@Override
	public boolean isItemValid(ItemStack stack)
	{
		return !isGhostSlot; //prevents shift clicking into ghost slot
	}

	@Override
	public void putStack(ItemStack p_75215_1_)
	{
		if(isItemValid(p_75215_1_))
		{
			inventory.setInventorySlotContents(getSlotIndex(), p_75215_1_);
			onSlotChanged();
		}
	}

	@Override
	public int getSlotStackLimit(){
		return 1;
	}

	public boolean isGhostSlot()
	{
		return isGhostSlot;
	}
}
