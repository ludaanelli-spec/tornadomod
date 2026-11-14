package com.tornadomod.world;

import com.tornadomod.TornadoMod;
import com.tornadomod.entity.TornadoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Natural tornado formation: Overworld thunderstorms occasionally spawn one near a random player. */
public final class TornadoSpawner {
	private static final int MAX_TORNADOES_PER_WORLD = 2;

	private TornadoSpawner() {
	}

	public static void tick(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) return;
		if (!level.isThundering()) return;
		if (level.getGameTime() % 100 != 0) return;
		if (!level.getGameRules().getBoolean(TornadoMod.RULE_SPAWNS)) return;

		RandomSource random = level.getRandom();
		if (random.nextFloat() > 0.12f) return; // roughly one attempt per minute of storm

		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) return;
		if (level.getEntities(EntityTypeTest.forClass(TornadoEntity.class), e -> true).size() >= MAX_TORNADOES_PER_WORLD) return;

		ServerPlayer target = players.get(random.nextInt(players.size()));
		double angle = random.nextDouble() * Math.PI * 2;
		double dist = 48 + random.nextDouble() * 40;
		double x = target.getX() + Math.cos(angle) * dist;
		double z = target.getZ() + Math.sin(angle) * dist;
		if (!TornadoEntity.isLoaded(level, x, z)) return;

		int bx = Mth.floor(x);
		int bz = Mth.floor(z);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
		if (level.getFluidState(new BlockPos(bx, y - 1, bz)).is(FluidTags.WATER)) return; // no waterspouts

		// head roughly towards the player, with some scatter
		float heading = (float) (Math.atan2(target.getZ() - z, target.getX() - x) + (random.nextDouble() - 0.5) * 1.2);
		TornadoEntity.spawn(level, new Vec3(x, y, z), -1, heading);
	}
}
