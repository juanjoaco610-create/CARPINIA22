package com.carpinchoia;

import com.carpinchoia.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Huevo de Carpincho IA: al usarlo pide coordenadas, radio, nombre y duenos. Solo administradores. */
public class CarpinchoEggItem extends Item {
    public CarpinchoEggItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) {
            BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openCreate(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
