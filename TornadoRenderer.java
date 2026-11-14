package com.tornadomod.client;

import com.tornadomod.entity.TornadoEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The tornado has no model: its funnel is drawn with particles in TornadoEntity#clientTick. */
public class TornadoRenderer extends EntityRenderer<TornadoEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/misc/white.png");

	public TornadoRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(TornadoEntity entity, Frustum frustum, double x, double y, double z) {
		return false;
	}

	@Override
	public ResourceLocation getTextureLocation(TornadoEntity entity) {
		return TEXTURE;
	}
}
