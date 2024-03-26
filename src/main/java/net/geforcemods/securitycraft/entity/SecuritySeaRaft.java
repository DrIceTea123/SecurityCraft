package net.geforcemods.securitycraft.entity;

import java.util.UUID;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasscodeProtected;
import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.misc.SaltData;
import net.geforcemods.securitycraft.network.client.OpenScreen;
import net.geforcemods.securitycraft.network.client.OpenScreen.DataType;
import net.geforcemods.securitycraft.util.PasscodeUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

// TODO: Change "This block doesn't have a passcode yet" message to not mention block
// TODO: Change all other passcode-related lines to stop talking about blocks
// TODO: Remove mention of "block" in messages.securitycraft:notOwned
public class SecuritySeaRaft extends ChestBoat implements IOwnable, IPasscodeProtected {
	private static final EntityDataAccessor<Owner> OWNER = SynchedEntityData.<Owner>defineId(SecuritySeaRaft.class, Owner.getSerializer());
	private byte[] passcode;
	private UUID saltKey;

	public SecuritySeaRaft(EntityType<? extends Boat> type, Level level) {
		super(SCContent.SECURITY_SEA_RAFT_ENTITY.get(), level);
	}

	public SecuritySeaRaft(Level level, double x, double y, double z) {
		super(SCContent.SECURITY_SEA_RAFT_ENTITY.get(), level);
		setPos(x, y, z);
		xo = y;
		yo = y;
		zo = z;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(OWNER, new Owner());
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (player.isSecondaryUseActive()) {
			ItemStack stack = player.getItemInHand(hand);

			if (player.isHolding(SCContent.CODEBREAKER.get()))
				handleCodebreaking(player, player.getMainHandItem().is(SCContent.CODEBREAKER.get()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
			else if (player.isHolding(SCContent.UNIVERSAL_KEY_CHANGER.get())) {
				if (isOwnedBy(player) || player.isCreative())
					PacketDistributor.PLAYER.with((ServerPlayer) player).send(new OpenScreen(DataType.CHANGE_PASSCODE_FOR_ENTITY, getId()));
				else
					PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.UNIVERSAL_KEY_CHANGER.get().getDescriptionId()), Utils.localize("messages.securitycraft:notOwned", PlayerUtils.getOwnerComponent(getOwner())), ChatFormatting.RED);
			}
			else if (stack.is(SCContent.UNIVERSAL_OWNER_CHANGER.get()) && isOwnedBy(player)) {
				if (!player.level().isClientSide) {
					String newOwner = stack.getHoverName().getString();

					setOwner(PlayerUtils.isPlayerOnline(newOwner) ? PlayerUtils.getPlayerFromName(newOwner).getUUID().toString() : "ownerUUID", newOwner);
					PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.UNIVERSAL_OWNER_CHANGER.get().getDescriptionId()), Utils.localize("messages.securitycraft:universalOwnerChanger.changed", newOwner), ChatFormatting.GREEN);
					return InteractionResult.CONSUME;
				}

				return InteractionResult.SUCCESS;
			}
		}

		return super.interact(player, hand);
	}

	@Override
	public InteractionResult interactWithContainerVehicle(Player player) {
		Level level = level();
		BlockPos pos = blockPosition();

		if (!level.isClientSide && verifyPasscodeSet(level, pos, this, player))
			openPasscodeGUI(level, pos, player);

		return !level.isClientSide ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
	}

	@Override
	public void openCustomInventoryScreen(Player player) {
		interactWithContainerVehicle(player);
	}

	@Override
	public void openPasscodeGUI(Level level, BlockPos pos, Player player) {
		if (!level.isClientSide && getPasscode() != null)
			PacketDistributor.PLAYER.with((ServerPlayer) player).send(new OpenScreen(DataType.CHECK_PASSCODE_FOR_ENTITY, getId()));
	}

	@Override
	public void openSetPasscodeScreen(ServerPlayer player, BlockPos pos) {
		PacketDistributor.PLAYER.with(player).send(new OpenScreen(DataType.SET_PASSCODE_FOR_ENTITY, getId()));
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		Entity entity = source.getEntity();

		if (!(entity instanceof Player player) || isOwnedBy(player) || player.isCreative())
			return super.hurt(source, amount);
		else
			return false;
	}

	@Override
	public void chestVehicleDestroyed(DamageSource damageSource, Level level, Entity entity) {
		super.chestVehicleDestroyed(damageSource, level, entity);
		SaltData.removeSalt(getSaltKey());
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		CompoundTag ownerTag = new CompoundTag();

		super.addAdditionalSaveData(tag);
		getOwner().save(ownerTag, needsValidation());
		tag.put("owner", ownerTag);

		if (saltKey != null)
			tag.putUUID("saltKey", saltKey);

		if (passcode != null)
			tag.putString("passcode", PasscodeUtils.bytesToString(passcode));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(OWNER, Owner.fromCompound(tag.getCompound("owner")));
		loadSaltKey(tag);
		loadPasscode(tag);
	}

	public void setOwner(Player player) {
		setOwner(player.getGameProfile().getId().toString(), player.getName().getString());
	}

	@Override
	public void setOwner(String uuid, String name) {
		entityData.set(OWNER, new Owner(name, uuid));
	}

	@Override
	public Owner getOwner() {
		return entityData.get(OWNER);
	}

	@Override
	public void onOwnerChanged(BlockState state, Level level, BlockPos pos, Player player) {}

	@Override
	public void activate(Player player) {
		//super is necessary here, because the override doesn't open the screen directly and instead opens the passcode screens
		super.openCustomInventoryScreen(player);
	}

	@Override
	public byte[] getPasscode() {
		return passcode == null || passcode.length == 0 ? null : passcode;
	}

	@Override
	public void setPasscode(byte[] passcode) {
		this.passcode = passcode;
	}

	@Override
	public UUID getSaltKey() {
		return saltKey;
	}

	@Override
	public void setSaltKey(UUID saltKey) {
		this.saltKey = saltKey;
	}

	@Override
	public void startCooldown() {}

	@Override
	public boolean isOnCooldown() {
		return false;
	}

	@Override
	public long getCooldownEnd() {
		return 0;
	}

	@Override
	public Item getDropItem() {
		return SCContent.SECURITY_SEA_RAFT_ITEM.get();
	}
}
