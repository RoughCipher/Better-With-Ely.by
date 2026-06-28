package ru.roughcipher.better_with_elyby.config;


public final class BWEBUrls {

    private BWEBUrls() {}

    public static final String AUTH_SERVER = "https://authserver.ely.by";

    /** URL UUID NICKNAME */
    public static final String UUID_LOOKUP_URL = AUTH_SERVER + "/api/users/profiles/minecraft/%s";

    /** Client join */
    public static final String SESSION_JOIN_URL = AUTH_SERVER + "/session/legacy/join?user=";

    /** Server hasJoined */
    public static final String SESSION_HAS_JOINED_URL = AUTH_SERVER + "/session/legacy/hasJoined?user=";

    public static final String SKIN_PROFILE_URL = "https://skinsystem.ely.by/profile/";

    /** MOJANG */
    public static final String OLD_UUID_LOOKUP = "https://api.minecraftservices.com/minecraft/profile/lookup/name/%s";

    public static final String OLD_SESSION = "http://session.minecraft.net/game/joinserver.jsp?user=";
}
