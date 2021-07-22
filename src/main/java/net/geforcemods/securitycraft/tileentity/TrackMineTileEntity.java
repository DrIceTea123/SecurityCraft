package net.geforcemods.securitycraft.tileentity;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.OwnableTileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class TrackMineTileEntity extends OwnableTileEntity
{
	private boolean active = true;

	public TrackMineTileEntity()
	{
		super(SCContent.teTypeTrackMine);
	}

	public void activate()
	{
		if(!active)
		{
			active = true;
			setChanged();
		}
	}

	public void deactivate()
	{
		if(active)
		{
			active = false;
			setChanged();
		}
	}

	public boolean isActive()
	{
		return active;
	}

	@Override
	public CompoundTag save(CompoundTag tag)
	{
		tag.putBoolean("TrackMineEnabled", active);
		return super.save(tag);
	}

	@Override
	public void load(BlockState state, CompoundTag tag)
	{
		super.load(state, tag);
		active = tag.getBoolean("TrackMineEnabled");
	}
}
