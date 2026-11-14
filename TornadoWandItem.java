package com.tornadomod.item;

import com.tornadomod.entity.TornadoEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * Right-click: summon a tornado where you are looking.
 * Sneak + right-click: cycle the strength (EF0 - EF5).
 */
public class TornadoWandItem extends Item {
	private static final String KEY = "TornadoRating";

	public TornadoWandItem(Properties properties) {
		super(properties);
	}

	public static int getRating(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(KEY) ? Math.max(0, Math.min(5, tag.getInt(KEY))) : 1;
	}

	private static void setRating(ItemStack stack, int rating) {
		stack.getOrCreateTag().putInt(KEY, rating);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isShiftKeyDown()) {
			int next = (getRating(stack) + 1) % 6;
			setRating(stack, next);
			if (!level.isClientSide) {
				player.displayClientMessage(Component.literal("Tornado strength: EF" + next), true);
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		}

		if (!level.isClientSide) {
			ServerLevel serverLevel = (ServerLevel) level;
			HitResult hit = player.pick(150.0, 0.0f, false);
			Vec3 target = hit.getType() == HitResult.Type.MISS
					? player.getEyePosition().add(player.getLookAngle().scale(40.0))
					: hit.getLocation();
			float heading = (float) Math.toRadians(player.getYRot() + 90.0f);
			TornadoEntity.spawn(serverLevel, target, getRating(stack), heading);
			player.getCooldowns().addCooldown(this, 60);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Strength: EF" + getRating(stack)).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Right-click: summon a tornado").withStyle(ChatFormatting.DARK_GRAY));
		tooltip.add(Component.literal("Sneak + right-click: change strength").withStyle(ChatFormatting.DARK_GRAY));
	}
}
