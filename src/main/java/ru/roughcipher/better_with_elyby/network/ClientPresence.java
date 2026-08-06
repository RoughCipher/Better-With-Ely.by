package ru.roughcipher.better_with_elyby.network;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPresence {
	public static final String CHANNEL = "BWEB:Hello";
	public static final byte[] HELLO_PAYLOAD = new byte[]{1};
	private static final Set<Integer> MODDED_ENTITIES = ConcurrentHashMap.newKeySet();

	public static void markHasMod(int entityId) { MODDED_ENTITIES.add(entityId); }
	public static boolean hasMod(int entityId) { return MODDED_ENTITIES.contains(entityId); }
	public static void remove(int entityId) { MODDED_ENTITIES.remove(entityId); }
}
