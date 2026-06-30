package dev.arkic.mixin;

import net.minecraft.client.render.chunk.SectionRenderData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionRenderData.class)
public interface SectionBuilderMixin extends Accessor {
}
