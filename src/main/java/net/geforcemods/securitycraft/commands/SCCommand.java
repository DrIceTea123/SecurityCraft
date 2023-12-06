package net.geforcemods.securitycraft.commands;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

import com.google.common.base.Predicates;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.network.client.SendTip;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SCCommand {
	private static final DynamicCommandExceptionType ERROR_NOT_FOUND = new DynamicCommandExceptionType(registry -> Component.translatable("messages.securitycraft:dump.notFound", registry));
	private static final Map<String, DeferredRegister<?>> REGISTRIES = Util.make(() -> {
		Map<String, DeferredRegister<?>> map = new Object2ObjectArrayMap<>();

		for (Field field : SCContent.class.getFields()) {
			try {
				Object object = field.get(null);

				if (!(object instanceof DeferredRegister))
					return map;

				map.put(field.getName().toLowerCase(Locale.ROOT), (DeferredRegister<?>) object);
			}
			catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		return map;
	});

	private SCCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		//@formatter:off
		dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("sc")
				.requires(Predicates.alwaysTrue())
				.then(dump())
				.then(connect())
				.then(help())
				.then(bug()));
		//@formatter:on
	}

	private static ArgumentBuilder<CommandSourceStack, ?> dump() {
		//@formatter:off
		return Commands.literal("dump")
				.then(Commands.argument("registry", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(REGISTRIES.keySet(), builder))
						.executes(ctx -> { //@formatter:on
							String registry = ctx.getArgument("registry", String.class);

							if (!REGISTRIES.containsKey(registry))
								throw ERROR_NOT_FOUND.create(registry);

							final String lineSeparator = System.lineSeparator();
							final String finalResult;
							final var registryObjects = REGISTRIES.get(registry).getEntries();
							String result = "";

							for (DeferredHolder<?, ?> ro : registryObjects) {
								result += ro.getId().toString() + lineSeparator;
							}

							finalResult = result;
							ctx.getSource().getPlayerOrException().sendSystemMessage(Component.literal("[") //@formatter:off
									.append(Component.literal("SecurityCraft").withStyle(ChatFormatting.GOLD))
									.append(Component.literal("] "))
									.append(Component.translatable("messages.securitycraft:dump.result", registryObjects.size())
											.withStyle(style -> style
													.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(registry)))
													.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, finalResult.substring(0, finalResult.lastIndexOf(lineSeparator)))))));
							//@formatter:on
							return 0;
						}));
	}

	private static ArgumentBuilder<CommandSourceStack, ?> connect() {
		return Commands.literal("connect").executes(ctx -> {
			//@formatter:off
			ctx.getSource().getPlayerOrException().sendSystemMessage(Component.literal("[")
					.append(Component.literal("IRC").withStyle(ChatFormatting.GREEN))
					.append(Component.literal("] "))
					.append(Utils.localize("messages.securitycraft:irc.connected"))
					.append(Component.literal(" "))
					.append(CommonHooks.newChatWithLinks(SendTip.TIPS_WITH_LINK.get("discord"))));
			//@formatter:on
			return 0;
		});
	}

	private static ArgumentBuilder<CommandSourceStack, ?> help() {
		return Commands.literal("help").executes(ctx -> {
			//@formatter:off
			ctx.getSource().getPlayerOrException().sendSystemMessage(Component.translatable("messages.securitycraft:sc_help",
					Component.translatable(Blocks.CRAFTING_TABLE.getDescriptionId()),
					Component.translatable(Items.BOOK.getDescriptionId()),
					Component.translatable(Items.IRON_BARS.getDescriptionId())));
			//@formatter:on
			return 0;
		});
	}

	private static ArgumentBuilder<CommandSourceStack, ?> bug() {
		return Commands.literal("bug").executes(ctx -> {
			PlayerUtils.sendMessageEndingWithLink(ctx.getSource().getPlayerOrException(), Component.literal("SecurityCraft"), Utils.localize("messages.securitycraft:bugReport"), SendTip.TIPS_WITH_LINK.get("discord"), ChatFormatting.GOLD);
			return 0;
		});
	}
}
