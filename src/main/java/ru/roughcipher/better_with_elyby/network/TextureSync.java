package ru.roughcipher.better_with_elyby.network;

import net.minecraft.core.net.packet.PacketCustomPayload;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.PlayerList;
import ru.roughcipher.better_with_elyby.auth.AuthSource;
import ru.roughcipher.better_with_elyby.skin.PlayerTextures;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class TextureSync {
	public static final String CHANNEL = "bweb:tex";

	private static final int MAGIC = 0x42575458;
	private static final short PROTO = 1;
	private static final byte KIND_PUT = 0x10;
	private static final byte KIND_DROP = 0x11;
	private static final int FLAG_SKIN = 1;
	private static final int FLAG_CAPE = 2;
	private static final int FLAG_SLIM = 4;
	private static final int MAX_NAME = 64;
	private static final int MAX_UUID = 36;
	private static final int MAX_URL = 2048;

	private TextureSync() {}

	public static void pushToAll(PlayerList list, int entityId, String name, String uuid, PlayerTextures textures) {
		if (list == null || textures == null || textures.isEmpty() || uuid == null) return;
		list.sendPacketToAllPlayers(new PacketCustomPayload(CHANNEL, encodePut(entityId, name, uuid, textures)));
	}

	public static void pushTo(PlayerServer player, int entityId, String name, String uuid, PlayerTextures textures) {
		if (player == null || textures == null || textures.isEmpty() || uuid == null) return;
		player.playerNetServerHandler.sendPacket(new PacketCustomPayload(CHANNEL, encodePut(entityId, name, uuid, textures)));
	}

	public static byte[] encodePut(int entityId, String name, String uuid, PlayerTextures textures) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
			 DataOutputStream out = new DataOutputStream(bos)) {
			out.writeInt(MAGIC);
			out.writeShort(PROTO);
			out.writeByte(KIND_PUT);
			writeStr(out, uuid, MAX_UUID);
			out.writeInt(entityId);
			out.writeByte(textures.getSource().id());
			int flags = 0;
			if (textures.getSkinUrl() != null) flags |= FLAG_SKIN;
			if (textures.getCapeUrl() != null) flags |= FLAG_CAPE;
			if (textures.isSlim()) flags |= FLAG_SLIM;
			out.writeByte(flags);
			if ((flags & FLAG_SKIN) != 0) writeStr(out, textures.getSkinUrl(), MAX_URL);
			if ((flags & FLAG_CAPE) != 0) writeStr(out, textures.getCapeUrl(), MAX_URL);
			writeStr(out, name == null ? "" : name, MAX_NAME);
			return bos.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Snapshot decode(byte[] data) throws IOException {
		if (data == null || data.length < 8) throw new IOException("short payload");
		try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
			if (in.readInt() != MAGIC) throw new IOException("bad magic");
			if (in.readShort() != PROTO) throw new IOException("bad proto");
			int kind = in.readUnsignedByte();
			String uuid = readStr(in, MAX_UUID);
			int entityId = in.readInt();
			if (kind == KIND_DROP) {
				String name = in.available() > 0 ? readStr(in, MAX_NAME) : "";
				return Snapshot.drop(entityId, name, uuid.isEmpty() ? null : uuid);
			}
			if (kind != KIND_PUT) throw new IOException("unknown kind " + kind);
			AuthSource source = AuthSource.fromId(in.readUnsignedByte());
			int flags = in.readUnsignedByte();
			String skin = (flags & FLAG_SKIN) != 0 ? requireHttp(readStr(in, MAX_URL)) : null;
			String cape = (flags & FLAG_CAPE) != 0 ? requireHttp(readStr(in, MAX_URL)) : null;
			boolean slim = (flags & FLAG_SLIM) != 0;
			String name = readStr(in, MAX_NAME);
			PlayerTextures textures = PlayerTextures.of(skin, cape, slim ? "slim" : "default", source);
			if (textures.isEmpty()) throw new IOException("empty textures");
			return Snapshot.put(entityId, name, uuid.isEmpty() ? null : uuid, textures);
		}
	}

	private static void writeStr(DataOutputStream out, String value, int max) throws IOException {
		byte[] raw = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
		if (raw.length > max) throw new IllegalArgumentException("field too long");
		out.writeShort(raw.length);
		out.write(raw);
	}

	private static String readStr(DataInputStream in, int max) throws IOException {
		int len = in.readUnsignedShort();
		if (len > max) throw new IOException("field too long");
		byte[] raw = new byte[len];
		in.readFully(raw);
		return new String(raw, StandardCharsets.UTF_8);
	}

	private static String requireHttp(String url) throws IOException {
		if (url == null || url.isEmpty()) return null;
		try {
			URI uri = new URI(url);
			String scheme = uri.getScheme();
			if (uri.getHost() == null
				|| (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
				throw new IOException("non-http texture url");
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("bad texture url", e);
		}
		return url;
	}

	public static final class Snapshot {
		private final int entityId;
		private final String name;
		private final String uuid;
		private final PlayerTextures textures;

		private Snapshot(int entityId, String name, String uuid, PlayerTextures textures) {
			this.entityId = entityId;
			this.name = name;
			this.uuid = uuid;
			this.textures = textures;
		}

		public static Snapshot put(int entityId, String name, String uuid, PlayerTextures textures) {
			return new Snapshot(entityId, name, uuid, textures);
		}

		public static Snapshot drop(int entityId, String name, String uuid) {
			return new Snapshot(entityId, name, uuid, null);
		}

		public int entityId() { return entityId; }
		public String name() { return name; }
		public String uuid() { return uuid; }
		public PlayerTextures textures() { return textures; }
		public boolean isDrop() { return textures == null; }
	}
}
