package com.tornadomod.entity;

import com.tornadomod.TornadoMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * A wandering tornado. Strength is an Enhanced Fujita rating from 0 (EF0) to 5 (EF5).
 * <p>
 * Server side: moves across the terrain, drags entities into a spiral, lifts them, hurts them at the core
 * (EF2+) and tears blocks out of the world, flinging them as falling-block debris.
 * Client side: draws the funnel with particles (there is no model).
 */
public class TornadoEntity extends Entity {
	private static final EntityDataAccessor<Integer> RATING = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);

	/** Hardest block (by destroy speed / hardness) each rating can tear loose: EF0 .. EF5. */
	private static final float[] MAX_HARDNESS = {0.4f, 1.0f, 2.5f, 4.0f, 8.0f, 60.0f};

	private int lifeTicks;
	private int maxLife;
	private float heading;

	public TornadoEntity(EntityType<? extends TornadoEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	// ------------------------------------------------------------------ creation helpers

	/** Spawns a tornado on the ground at the given x/z (y is snapped to the terrain when the chunk is loaded). */
	public static TornadoEntity spawn(ServerLevel level, Vec3 pos, int rating, float heading) {
		TornadoEntity t = new TornadoEntity(TornadoMod.TORNADO.get(), level);
		double y = pos.y;
		if (isLoaded(level, pos.x, pos.z)) {
			y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(pos.x), Mth.floor(pos.z));
		}
		t.moveTo(pos.x, y, pos.z, 0.0f, 0.0f);
		t.setRating(rating < 0 ? randomRating(level.getRandom()) : rating);
		t.heading = heading;
		level.addFreshEntity(t);
		return t;
	}

	public static int randomRating(RandomSource random) {
		int roll = random.nextInt(100);
		if (roll < 35) return 0;
		if (roll < 65) return 1;
		if (roll < 85) return 2;
		if (roll < 95) return 3;
		if (roll < 99) return 4;
		return 5;
	}

	public static boolean isLoaded(ServerLevel level, double x, double z) {
		return level.hasChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4);
	}

	// ------------------------------------------------------------------ data

	@Override
	protected void defineSynchedData() {
		this.entityData.define(RATING, 1);
	}

	public int getRating() {
		return this.entityData.get(RATING);
	}

	public void setRating(int rating) {
		this.entityData.set(RATING, Mth.clamp(rating, 0, 5));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		setRating(tag.getInt("Rating"));
		lifeTicks = tag.getInt("Life");
		maxLife = tag.getInt("MaxLife");
		heading = tag.getFloat("Heading");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("Rating", getRating());
		tag.putInt("Life", lifeTicks);
		tag.putInt("MaxLife", maxLife);
		tag.putFloat("Heading", heading);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	// ------------------------------------------------------------------ geometry

	private double baseRadius() {
		return 2.5 + 1.6 * getRating();
	}

	private double funnelHeight() {
		return 30 + 8 * getRating();
	}

	/** Funnel radius at a height above the ground: narrow at the base, wide at the top. */
	private double radiusAt(double h) {
		return baseRadius() * (0.8 + 1.2 * Math.min(1.0, h / funnelHeight()));
	}

	// ------------------------------------------------------------------ tick

	@Override
	public void tick() {
		super.tick();
		lifeTicks++;
		if (level().isClientSide) {
			clientTick();
		} else {
			serverTick((ServerLevel) level());
		}
	}

	private void serverTick(ServerLevel level) {
		if (maxLife == 0) {
			maxLife = 2400 + 600 * getRating() + random.nextInt(1200);
		}
		if (lifeTicks >= maxLife) {
			level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 3, getZ(), 80, 3.0, 4.0, 3.0, 0.05);
			discard();
			return;
		}

		wander(level);
		pullEntities(level);
		if (level.getGameRules().getBoolean(TornadoMod.RULE_DESTRUCTION)) {
			tearBlocks(level);
		}

		if (lifeTicks % 20 == 0) {
			level.playSound(null, getX(), getY() + 5, getZ(), SoundEvents.ENDER_DRAGON_FLAP,
					SoundSource.WEATHER, 5.0f, 0.5f);
		}
		if (lifeTicks % 100 == 1) {
			warnPlayers(level);
		}
	}

	private void wander(ServerLevel level) {
		heading += (random.nextFloat() - 0.5f) * 0.08f;
		double speed = 0.10 + 0.02 * getRating();
		double nx = getX() + Math.cos(heading) * speed;
		double nz = getZ() + Math.sin(heading) * speed;
		if (!isLoaded(level, nx, nz)) {
			// reached the edge of the loaded world: turn around
			heading += (float) Math.PI * (0.5f + random.nextFloat() * 0.5f);
			return;
		}
		double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(nx), Mth.floor(nz));
		double ny = getY() + (ground - getY()) * 0.15;
		setPos(nx, ny, nz);
	}

	private void pullEntities(ServerLevel level) {
		int r = getRating();
		double reach = baseRadius() * 2.4;
		double funnel = funnelHeight();
		double liftCap = 4 + 3 * r;
		double wind = 0.10 + 0.06 * r;

		AABB box = new AABB(getX() - reach, getY() - 2, getZ() - reach, getX() + reach, getY() + funnel * 0.7, getZ() + reach);
		List<Entity> targets = level.getEntities(this, box,
				e -> !(e instanceof TornadoEntity) && !e.isSpectator() && !(e instanceof Player p && p.isCreative()));

		for (Entity e : targets) {
			double dx = getX() - e.getX();
			double dz = getZ() - e.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			double h = Math.max(0, e.getY() - getY());
			double rad = radiusAt(h) * 1.8;
			if (dist > rad) continue;

			// unit vector towards the centre, and the tangent around it
			double ux = dx / Math.max(dist, 0.5);
			double uz = dz / Math.max(dist, 0.5);
			double f = Mth.clamp(1.0 - dist / rad, 0.25, 1.0);
			double tx = (0.35 * ux - uz) * wind * f;
			double tz = (0.35 * uz + ux) * wind * f;
			boolean canLift = h < liftCap;

			if (e instanceof ServerPlayer) {
				// the server does not track a player's real velocity, so set it outright every tick
				e.setDeltaMovement(tx, canLift ? 0.10 + 0.05 * r : 0.0, tz);
			} else {
				Vec3 v = e.getDeltaMovement();
				double vy = v.y;
				if (canLift) {
					double ty = 0.25 + 0.06 * r;
					if (vy < ty) vy += (ty - vy) * 0.5;
				}
				e.setDeltaMovement(v.x + (tx - v.x) * 0.2, vy, v.z + (tz - v.z) * 0.2);
			}
			e.hurtMarked = true;

			if (r >= 2 && lifeTicks % 10 == 0 && dist < baseRadius() * 0.6 && h < funnel * 0.4
					&& e instanceof LivingEntity living) {
				living.hurt(level.damageSources().generic(), (float) (r - 1));
			}
		}
	}

	private void tearBlocks(ServerLevel level) {
		if (lifeTicks % 2 != 0) return;
		int r = getRating();
		float maxHardness = MAX_HARDNESS[r];
		double reach = 6 + 4 * r;
		int attempts = 6 + 6 * r;
		int budget = 1 + r; // max blocks converted per pass, keeps lag in check

		for (int i = 0; i < attempts && budget > 0; i++) {
			double h = random.nextDouble() * reach;
			double rad = radiusAt(h) * Math.sqrt(random.nextDouble());
			double a = random.nextDouble() * Math.PI * 2;
			double bx = getX() + Math.cos(a) * rad;
			double bz = getZ() + Math.sin(a) * rad;
			if (!isLoaded(level, bx, bz)) continue;

			BlockPos pos = BlockPos.containing(bx, getY() + h, bz);
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) continue;

			float hardness = state.getDestroySpeed(level, pos);
			if (hardness < 0 || hardness > maxHardness) continue;
			if (r < 4 && isTerrain(state)) continue; // only EF4+ scours the ground itself
			if (!isExposed(level, pos)) continue;

			launch(level, pos, state, r);
			budget--;
		}
	}

	private static boolean isTerrain(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD);
	}

	private static boolean isExposed(ServerLevel level, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (level.getBlockState(pos.relative(d)).isAir()) return true;
		}
		return false;
	}

	private void launch(ServerLevel level, BlockPos pos, BlockState state, int r) {
		FallingBlockEntity debris = FallingBlockEntity.fall(level, pos, state);
		double ox = pos.getX() + 0.5 - getX();
		double oz = pos.getZ() + 0.5 - getZ();
		double d = Math.max(0.5, Math.sqrt(ox * ox + oz * oz));
		ox /= d;
		oz /= d;
		double spin = 0.30 + 0.07 * r;
		debris.setDeltaMovement(
				oz * spin + ox * 0.08,
				0.30 + random.nextDouble() * (0.15 + 0.08 * r),
				-ox * spin + oz * 0.08);
		debris.hurtMarked = true;
		if (random.nextFloat() < 0.75f) {
			debris.disableDrop(); // most debris just vanishes instead of littering the map
		}
	}

	private void warnPlayers(ServerLevel level) {
		Component msg = Component.literal("Tornado warning: EF" + getRating() + " tornado nearby!").withStyle(ChatFormatting.RED);
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(this) < 100.0 * 100.0) {
				p.displayClientMessage(msg, true);
			}
		}
	}

	// ------------------------------------------------------------------ client visuals

	private void clientTick() {
		Level lvl = level();
		int r = getRating();
		double funnel = funnelHeight();
		int count = 14 + 6 * r;

		for (int i = 0; i < count; i++) {
			double h = random.nextDouble() * funnel;
			double rad = radiusAt(h) * (0.55 + 0.45 * random.nextDouble());
			double ang = -lifeTicks * 0.30 + h * 0.20 + random.nextDouble() * 0.6;
			double px = getX() + Math.cos(ang) * rad;
			double pz = getZ() + Math.sin(ang) * rad;
			double py = getY() + h;
			ParticleOptions type = (h < funnel * 0.15 && random.nextInt(3) == 0)
					? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD;
			lvl.addParticle(type, true, px, py, pz, Math.sin(ang) * 0.25, 0.04, -Math.cos(ang) * 0.25);
		}
	}
}
