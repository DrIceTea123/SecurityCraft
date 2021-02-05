package net.geforcemods.securitycraft.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.containers.BlockPocketManagerContainer;
import net.geforcemods.securitycraft.network.server.SyncBlockPocketManager;
import net.geforcemods.securitycraft.screen.components.ClickButton;
import net.geforcemods.securitycraft.screen.components.NamedSlider;
import net.geforcemods.securitycraft.screen.components.StackHoverChecker;
import net.geforcemods.securitycraft.screen.components.TextHoverChecker;
import net.geforcemods.securitycraft.tileentity.BlockPocketManagerTileEntity;
import net.geforcemods.securitycraft.util.ClientUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.widget.Slider;
import net.minecraftforge.fml.network.PacketDistributor;

public class BlockPocketManagerScreen extends ContainerScreen<BlockPocketManagerContainer>
{
	private static final ResourceLocation TEXTURE = new ResourceLocation("securitycraft:textures/gui/container/block_pocket_manager.png");
	private static final ResourceLocation TEXTURE_STORAGE = new ResourceLocation("securitycraft:textures/gui/container/block_pocket_manager_storage.png");
	private static final ItemStack BLOCK_POCKET_WALL = new ItemStack(SCContent.BLOCK_POCKET_WALL.get());
	private static final ItemStack REINFORCED_CHISELED_CRYSTAL_QUARTZ = new ItemStack(SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ.get());
	private static final ItemStack REINFORCED_CRYSTAL_QUARTZ_PILLAR = new ItemStack(SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR.get());
	private final TranslationTextComponent blockPocketManager = ClientUtils.localize(SCContent.BLOCK_POCKET_MANAGER.get().getTranslationKey());
	private final TranslationTextComponent youNeed = ClientUtils.localize("gui.securitycraft:blockPocketManager.youNeed");
	private final boolean storage;
	public BlockPocketManagerTileEntity te;
	private int size = 5;
	private Button toggleButton;
	private Button sizeButton;
	private Button assembleButton;
	private Button outlineButton;
	private Slider offsetSlider;
	private StackHoverChecker[] hoverCheckers = new StackHoverChecker[3];
	private TextHoverChecker assembleHoverChecker;

	public BlockPocketManagerScreen(BlockPocketManagerContainer container, PlayerInventory inv, ITextComponent name)
	{
		super(container, inv, name);

		te = container.te;
		size = te.size;
		storage = container.storage;

		if(storage)
			xSize = 256;

		ySize = !storage ? 194 : 240;
	}

	@Override
	public void init()
	{
		super.init();

		int width = storage ? 123 : xSize;
		int widgetWidth = storage ? 110 : 120;
		int widgetOffset = widgetWidth / 2;
		int[] yOffset = storage ? new int[]{-71, -95, -47, -23, 1} : new int[]{-40, -70, 23, 47, 71};

		addButton(toggleButton = new ClickButton(0, guiLeft + width / 2 - widgetOffset, guiTop + ySize / 2 + yOffset[0], widgetWidth, 20, ClientUtils.localize("gui.securitycraft:blockPocketManager." + (!te.enabled ? "activate" : "deactivate")), this::toggleButtonClicked));
		addButton(sizeButton = new ClickButton(1, guiLeft + width / 2 - widgetOffset, guiTop + ySize / 2 + yOffset[1], widgetWidth, 20, ClientUtils.localize("gui.securitycraft:blockPocketManager.size", size, size, size), this::sizeButtonClicked));
		addButton(assembleButton = new ClickButton(2, guiLeft + width / 2 - widgetOffset, guiTop + ySize / 2 + yOffset[2], widgetWidth, 20, ClientUtils.localize("gui.securitycraft:blockPocketManager.assemble"), this::assembleButtonClicked));
		addButton(outlineButton = new ClickButton(3, guiLeft + width / 2 - widgetOffset, guiTop + ySize / 2 + yOffset[3], widgetWidth, 20, ClientUtils.localize("gui.securitycraft:blockPocketManager.outline." + (!te.showOutline ? "show" : "hide")), this::outlineButtonClicked));
		addButton(offsetSlider = new NamedSlider(ClientUtils.localize("gui.securitycraft:projector.offset", te.autoBuildOffset), StringTextComponent.EMPTY, 4, guiLeft + width / 2 - widgetOffset, guiTop + ySize / 2 + yOffset[4], widgetWidth, 20, ClientUtils.localize("gui.securitycraft:projector.offset", ""), "", (-size + 2) / 2, (size - 2) / 2, te.autoBuildOffset, false, true, null, this::offsetSliderReleased));
		offsetSlider.updateSlider();

		if(!te.getOwner().isOwner(Minecraft.getInstance().player))
			sizeButton.active = toggleButton.active = assembleButton.active = outlineButton.active  = offsetSlider.active = false;
		else
		{
			sizeButton.active = offsetSlider.active = !te.enabled;
			assembleButton.active = minecraft.player.isCreative() || (!te.enabled && storage);
		}

		if(!storage)
		{
			hoverCheckers[0] = new StackHoverChecker(BLOCK_POCKET_WALL, guiTop + 93, guiTop + 113, guiLeft + 23, guiLeft + 43);
			hoverCheckers[1] = new StackHoverChecker(REINFORCED_CRYSTAL_QUARTZ_PILLAR, guiTop + 93, guiTop + 113, guiLeft + 75, guiLeft + 95);
			hoverCheckers[2] = new StackHoverChecker(REINFORCED_CHISELED_CRYSTAL_QUARTZ, guiTop + 93, guiTop + 113, guiLeft + 128, guiLeft + 148);
		}
		else
		{
			hoverCheckers[0] = new StackHoverChecker(BLOCK_POCKET_WALL, guiTop + ySize - 73, guiTop + ySize - 54, guiLeft + 174, guiLeft + 191);
			hoverCheckers[1] = new StackHoverChecker(REINFORCED_CRYSTAL_QUARTZ_PILLAR, guiTop + ySize - 50, guiTop + ySize - 31, guiLeft + 174, guiLeft + 191);
			hoverCheckers[2] = new StackHoverChecker(REINFORCED_CHISELED_CRYSTAL_QUARTZ, guiTop + ySize - 27, guiTop + ySize - 9, guiLeft + 174, guiLeft + 191);
		}

		assembleHoverChecker = new TextHoverChecker(assembleButton, ClientUtils.localize("gui.securitycraft:blockPocketManager.needStorageModule"));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY)
	{
		font.func_243248_b(matrix, blockPocketManager, (storage ? 123 : xSize) / 2 - font.getStringPropertyWidth(blockPocketManager) / 2, 6, 4210752);

		if(storage)
		{
			font.func_243248_b(matrix, playerInventory.getDisplayName(), 8, ySize - 94, 4210752);
			renderHoveredTooltip(matrix, mouseX - guiLeft, mouseY - guiTop);
		}

		if(!te.enabled)
		{
			if(!storage)
			{
				font.func_243248_b(matrix, youNeed, xSize / 2 - font.getStringPropertyWidth(youNeed) / 2, 83, 4210752);

				font.drawString(matrix, (size - 2) * (size - 2) * 6 + "", 42, 100, 4210752);
				minecraft.getItemRenderer().renderItemAndEffectIntoGUI(BLOCK_POCKET_WALL, 25, 96);

				font.drawString(matrix, (size - 2) * 12 - 1 + "", 94, 100, 4210752);
				minecraft.getItemRenderer().renderItemAndEffectIntoGUI(REINFORCED_CRYSTAL_QUARTZ_PILLAR, 77, 96);

				font.drawString(matrix, "8", 147, 100, 4210752);
				minecraft.getItemRenderer().renderItemAndEffectIntoGUI(REINFORCED_CHISELED_CRYSTAL_QUARTZ, 130, 96);
			}
			else
			{
				int[] materialCounts = new int[3];
				int wallsNeeded = (size - 2) * (size - 2) * 6;
				int pillarsNeeded = (size - 2) * 12 - 1;
				int chiseledNeeded = 8;

				//TODO: computing this every tick does not seem like a good idea
				te.getStorageHandler().ifPresent(handler -> {
					for(int i = 0; i < handler.getSlots(); i++)
					{
						ItemStack stack = handler.getStackInSlot(i);

						if(stack.getItem() instanceof BlockItem)
						{
							Block block = ((BlockItem)stack.getItem()).getBlock();

							if(block == SCContent.BLOCK_POCKET_WALL.get())
								materialCounts[0] += stack.getCount();
							else if(block == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR.get())
								materialCounts[1] += stack.getCount();
							else if(block == SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ.get())
								materialCounts[2] += stack.getCount();
						}
					}
				});

				font.func_243248_b(matrix, youNeed, 169 + 87 / 2 - font.getStringPropertyWidth(youNeed) / 2, ySize - 83, 4210752);

				font.drawString(matrix, Math.max(0, wallsNeeded - materialCounts[0]) + "", 192, ySize - 66, 4210752);
				minecraft.getItemRenderer().renderItemAndEffectIntoGUI(BLOCK_POCKET_WALL, 175, ySize - 70);

				font.drawString(matrix, Math.max(0, pillarsNeeded - materialCounts[1]) + "", 192, ySize - 44, 4210752);
				minecraft.getItemRenderer().renderItemAndEffectIntoGUI(REINFORCED_CRYSTAL_QUARTZ_PILLAR, 175, ySize - 48);

				font.drawString(matrix, Math.max(0, chiseledNeeded - materialCounts[2]) + "", 192, ySize - 22, 4210752);
				minecraft.getItemRenderer().renderItemAndEffectIntoGUI(REINFORCED_CHISELED_CRYSTAL_QUARTZ, 175, ySize - 26);
			}

			for(StackHoverChecker shc : hoverCheckers)
			{
				if(shc.checkHover(mouseX, mouseY))
				{
					renderTooltip(matrix, shc.getStack(), mouseX - guiLeft, mouseY - guiTop);
					return;
				}
			}

			if(assembleHoverChecker.checkHover(mouseX, mouseY) && !storage && !assembleButton.active && !te.enabled)
				renderTooltip(matrix, assembleHoverChecker.getName(), mouseX - guiLeft, mouseY - guiTop);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrix, float partialTicks, int mouseX, int mouseY)
	{
		int startX = (width - xSize) / 2;
		int startY = (height - ySize) / 2;

		renderBackground(matrix);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		minecraft.getTextureManager().bindTexture(storage ? TEXTURE_STORAGE : TEXTURE);
		blit(matrix, startX, startY, 0, 0, xSize, ySize);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if(offsetSlider.dragging)
			offsetSlider.mouseReleased(mouseX, mouseY, button);

		return super.mouseReleased(mouseX, mouseY, button);
	}

	public void toggleButtonClicked(ClickButton button)
	{
		if(te.enabled)
			te.disableMultiblock();
		else
		{
			TranslationTextComponent feedback;

			te.size = size;
			feedback = te.enableMultiblock();

			if(feedback != null)
				PlayerUtils.sendMessageToPlayer(Minecraft.getInstance().player, ClientUtils.localize(SCContent.BLOCK_POCKET_MANAGER.get().getTranslationKey()), feedback, TextFormatting.DARK_AQUA);
		}

		Minecraft.getInstance().player.closeScreen();
	}

	public void sizeButtonClicked(ClickButton button)
	{
		int newOffset;
		int newMin;
		int newMax;

		size += 4;

		if(size > 25)
			size = 5;

		newMin = (-size + 2) / 2;
		newMax = (size - 2) / 2;

		if(te.autoBuildOffset > 0)
			newOffset = Math.min(te.autoBuildOffset, newMax);
		else
			newOffset = Math.max(te.autoBuildOffset, newMin);

		te.size = size;
		offsetSlider.minValue = newMin;
		offsetSlider.maxValue = newMax;
		te.autoBuildOffset = newOffset;
		offsetSlider.setValue(newOffset);
		offsetSlider.updateSlider();
		button.setMessage(ClientUtils.localize("gui.securitycraft:blockPocketManager.size", size, size, size));
		sync();
	}

	public void assembleButtonClicked(ClickButton button)
	{
		IFormattableTextComponent feedback;

		te.size = size;
		feedback = te.autoAssembleMultiblock(Minecraft.getInstance().player);

		if(feedback != null)
			PlayerUtils.sendMessageToPlayer(Minecraft.getInstance().player, ClientUtils.localize(SCContent.BLOCK_POCKET_MANAGER.get().getTranslationKey()), feedback, TextFormatting.DARK_AQUA);

		Minecraft.getInstance().player.closeScreen();
	}

	public void outlineButtonClicked(ClickButton button)
	{
		te.toggleOutline();
		outlineButton.setMessage(ClientUtils.localize("gui.securitycraft:blockPocketManager.outline."+ (!te.showOutline ? "show" : "hide")));
		sync();
	}

	public void offsetSliderReleased(Slider slider)
	{
		te.autoBuildOffset = slider.getValueInt();
		sync();
	}

	private void sync()
	{
		SecurityCraft.channel.send(PacketDistributor.SERVER.noArg(), new SyncBlockPocketManager(te.getPos(), te.size, te.showOutline, te.autoBuildOffset));
	}
}
