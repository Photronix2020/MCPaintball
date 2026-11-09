package com.splatball;

import com.splatball.item.MarkerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Splatball.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, Action.class, Action::encode, Action::decode, Action::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    /** Client asks to fire (or reload) the marker in its main hand. The server validates everything. */
    public record Action(boolean reload) {
        static void encode(Action msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.reload);
        }

        static Action decode(FriendlyByteBuf buf) {
            return new Action(buf.readBoolean());
        }

        static void handle(Action msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof MarkerItem marker) {
                    if (msg.reload) marker.reload(player, stack);
                    else marker.tryFire(player, stack);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private Net() {}
}
