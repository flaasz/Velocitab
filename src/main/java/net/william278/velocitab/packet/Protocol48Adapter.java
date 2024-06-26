/*
 * This file is part of Velocitab, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.velocitab.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Protocol48Adapter extends TeamsPacketAdapter {

    private final LegacyComponentSerializer serializer;

    public Protocol48Adapter(@NotNull Velocitab plugin) {
        super(plugin, Set.of(ProtocolVersion.MINECRAFT_1_8, ProtocolVersion.MINECRAFT_1_12_2));
        serializer = LegacyComponentSerializer.legacySection();
    }

    @Override
    public void encode(@NotNull ByteBuf byteBuf, @NotNull UpdateTeamsPacket packet, @NotNull ProtocolVersion protocolVersion) {
        ProtocolUtils.writeString(byteBuf, shrinkString(packet.teamName()));
        UpdateTeamsPacket.UpdateMode mode = packet.mode();
        byteBuf.writeByte(mode.id());

        if (mode == UpdateTeamsPacket.UpdateMode.REMOVE_TEAM) {
            return;
        }

        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.UPDATE_INFO) {
            writeComponent(byteBuf, packet.displayName() != null ? packet.displayName() : Component.empty());
            writeComponent(byteBuf, packet.prefix() != null ? packet.prefix() : Component.empty());
            writeComponent(byteBuf, packet.suffix() != null ? packet.suffix() : Component.empty());
            byteBuf.writeByte(UpdateTeamsPacket.FriendlyFlag.toBitMask(packet.friendlyFlags()));
            ProtocolUtils.writeString(byteBuf, packet.nametagVisibility() != null ? packet.nametagVisibility().id() : "always");

            if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_12_2) >= 0) {
                ProtocolUtils.writeString(byteBuf, packet.collisionRule() != null ? packet.collisionRule().id() : "always");
            }

            byte color = (byte)(packet.color() & 0xFF);
            if (color < 0 || color > 15) {
                color = 0; 
            }
            byteBuf.writeByte(color);
        }

        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.ADD_PLAYERS || mode == UpdateTeamsPacket.UpdateMode.REMOVE_PLAYERS) {
            List < ? > rawEntities = packet.entities();
            List < String > entities = (List < String > )(List < ? > )(rawEntities != null ? rawEntities : new ArrayList < > ());
            ProtocolUtils.writeVarInt(byteBuf, entities.size());
            for (String entity: entities) {
                ProtocolUtils.writeString(byteBuf, shrinkString(entity));
            }
        }
    }

    @NotNull
    private String shrinkString(@NotNull String string) {
        return string != null ? string.substring(0, Math.min(string.length(), 16)) : "";
    }

    protected void writeComponent(ByteBuf buf, Component component) {
        ProtocolUtils.writeString(buf, shrinkString(serializer.serialize(component != null ? component : Component.empty())));
    }

}