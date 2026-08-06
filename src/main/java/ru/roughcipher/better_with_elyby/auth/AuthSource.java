package ru.roughcipher.better_with_elyby.auth;

public enum AuthSource {
	ELY,
	MOJANG,
	OFFLINE;

	public static AuthSource fromId(int id) {
		if (id == 1) return MOJANG;
		if (id == 2) return OFFLINE;
		return ELY;
	}

	public int id() {
		if (this == MOJANG) return 1;
		if (this == OFFLINE) return 2;
		return 0;
	}
}
