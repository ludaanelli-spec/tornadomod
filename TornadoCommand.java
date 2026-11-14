package com.tornadomod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tornadomod.entity.TornadoEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

/**
 * /tornado spawn [rating 0-5] [x y z]
 * /tornado clear
 * /tornado storm
 */
public final class TornadoCommand {
	private TornadoCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tornado")
				.requires(src -> src.hasPermission(2))
				.then(Commands.literal("spawn")
						.executes(ctx -> spawn(ctx.getSource(), -1, ctx.getSource().getPosition()))
						.then(Commands.argument("rating", IntegerArgumentType.integer(0, 5))
								.executes(ctx -> spawn(ctx.getSource(),
										IntegerArgumentType.getInteger(ctx, "rating"),
										ctx.getSource().getPosition()))
								.then(Commands.argument("pos", Vec3Argument.vec3())
										.executes(ctx -> spawn(ctx.getSource(),
												IntegerArgumentType.getInteger(ctx, "rating"),
												Vec3Argument.getVec3(ctx, "pos"))))))
				.then(Commands.literal("clear")
						.executes(ctx -> clear(ctx.getSource())))
				.then(Commands.literal("storm")
						.executes(ctx -> storm(ctx.getSource()))));
	}

	private static int spawn(CommandSourceStack source, int rating, Vec3 pos) {
		ServerLevel level = source.getLevel();
		float heading = level.getRandom().nextFloat() * (float) (Math.PI * 2);
		TornadoEntity tornado = TornadoEntity.spawn(level, pos, rating, heading);
		source.sendSuccess(() -> Component.literal("Spawned an EF" + tornado.getRating() + " tornado."), true);
		return tornado.getRating();
	}

	private static int clear(CommandSourceStack source) {
		int removed = 0;
		for (ServerLevel level : source.getServer().getAllLevels()) {
			for (TornadoEntity tornado : level.getEntities(EntityTypeTest.forClass(TornadoEntity.class), e -> true)) {
				tornado.discard();
				removed++;
			}
		}
		final int count = removed;
		source.sendSuccess(() -> Component.literal("Removed " + count + " tornado(es)."), true);
		return count;
	}

	private static int storm(CommandSourceStack source) {
		source.getLevel().setWeatherParameters(0, 12000, true, true);
		source.sendSuccess(() -> Component.literal("A thunderstorm rolls in. Tornadoes may form."), true);
		return 1;
	}
}
