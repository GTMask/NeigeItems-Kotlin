package pers.neige.neigeitems.internal.ref.network;

import org.inksnow.ankhinvoke.comments.HandleBy;
import pers.neige.neigeitems.internal.ref.chat.RefComponent;

@HandleBy(reference = "net/minecraft/network/protocol/game/ClientboundSetTitleTextPacket", predicates = "craftbukkit_version:[v1_17_R1,)")
public final class RefClientboundSetTitleTextPacket implements RefPacket<RefPacketListenerPlayOut> {
    @HandleBy(reference = "Lnet/minecraft/network/protocol/game/ClientboundSetTitleTextPacket;<init>(Lnet/minecraft/network/chat/Component;)V", predicates = "craftbukkit_version:[v1_17_R1,)")
    public RefClientboundSetTitleTextPacket(RefComponent title) {
        throw new UnsupportedOperationException();
    }
}
