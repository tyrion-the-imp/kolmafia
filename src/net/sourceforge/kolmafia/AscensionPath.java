package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.preferences.Preferences;

public class AscensionPath {
  private AscensionPath() {}

  public enum Path {
    // Path Name, Path ID, is Avatar?, image in ascension history, article
    NONE("none", 0, false, "blank", null),
    BOOZETAFARIAN("Boozetafarian", 1, false, "martini", "a", null, 0, false, 0, 14, 15),
    TEETOTALER("Teetotaler", 2, false, "bowl", "a", null, 0, false, 15, 0, 15),
    OXYGENARIAN("Oxygenarian", 3, false, "smalloxy", "an", null, 0, false, 0, 0, 15),
    BEES_HATE_YOU("Bees Hate You", 4, false, "beeicon", "a"),
    SURPRISING_FIST("Way of the Surprising Fist", 6, false, "wosp_fist", "a"),
    TRENDY("Trendy", 7, false, "trendyicon", "a"),
    AVATAR_OF_BORIS("Avatar of Boris", 8, true, "trusty", "an", "borisPoints", 0, false),
    BUGBEAR_INVASION("Bugbear Invasion", 9, false, "familiar39", "a"),
    ZOMBIE_SLAYER("Zombie Slayer", 10, true, "tombstone", "a", "zombiePoints", 0, false),
    CLASS_ACT("Class Act", 11, false, "motorboat", "a"),
    AVATAR_OF_JARLSBERG(
        "Avatar of Jarlsberg", 12, true, "jarlhat", "an", "jarlsbergPoints", 0, true),
    BIG("BIG!", 14, false, "bigicon", "a"),
    KOLHS("KOLHS", 15, false, "kolhsicon", "a"),
    CLASS_ACT_II("Class Act II: A Class For Pigs", 16, false, "motorboat2", "a"),
    AVATAR_OF_SNEAKY_PETE(
        "Avatar of Sneaky Pete", 17, true, "bigglasses", "an", "sneakyPetePoints", 0, true),
    SLOW_AND_STEADY("Slow and Steady", 18, false, "sas", "a"),
    HEAVY_RAINS("Heavy Rains", 19, false, "familiar31", "a"),
    PICKY("Picky", 21, false, "pickypath", "a"),
    STANDARD("Standard", 22, false, "standardicon", "the"),
    ACTUALLY_ED_THE_UNDYING(
        "Actually Ed the Undying", 23, true, "scarab", "an", "edPoints", 0, true),
    CRAZY_RANDOM_SUMMER("One Crazy Random Summer", 24, false, "dice", "the"),
    COMMUNITY_SERVICE("Community Service", 25, false, "csplaquesmall", "a"),
    AVATAR_OF_WEST_OF_LOATHING("Avatar of West of Loathing", 26, false, "badge", "an"),
    THE_SOURCE("The Source", 27, false, "ss_datasiphon", "a", "sourcePoints", 0, false),
    NUCLEAR_AUTUMN(
        "Nuclear Autumn", 28, false, "radiation", "a", "nuclearAutumnPoints", 23, false, 3, 2, 3),
    GELATINOUS_NOOB("Gelatinous Noob", 29, true, "gcube", "a", "noobPoints", 20, true),
    LICENSE_TO_ADVENTURE(
        "License to Adventure", 30, false, "briefcase", "a", "bondPoints", 24, true, 0, 2, 15),
    LIVE_ASCEND_REPEAT("Live. Ascend. Repeat.", 31, false, "watch", "a"),
    POKEFAM("Pocket Familiars", 32, false, "spiritorb", "a", "pokefamPoints", 10, false),
    GLOVER("G-Lover", 33, false, "g-loveheart", "a", "gloverPoints", 10, false),
    DISGUISES_DELIMIT("Disguises Delimit", 34, false, "dd_icon", "a", "masksUnlocked", 25, false),
    DARK_GYFFTE("Dark Gyffte", 35, true, "darkgift", "a", "darkGyfftePoints", 23, true),
    CRAZY_RANDOM_SUMMER_TWO(
        "Two Crazy Random Summer", 36, false, "twocrazydice", "a", "twoCRSPoints", 37, false),
    KINGDOM_OF_EXPLOATHING("Kingdom of Exploathing", 37, false, "puff", "a"),
    PATH_OF_THE_PLUMBER(
        "Path of the Plumber", 38, true, "mario_mushroom1", "a", "plumberPoints", 22, false),
    LOWKEY("Low Key Summer", 39, false, "littlelock", "a"),
    GREY_GOO("Grey Goo", 40, false, "greygooball", "a"),
    YOU_ROBOT("You, Robot", 41, false, "robobattery", "a", "youRobotPoints", 37, false, 0, 0, 0),
    QUANTUM("Quantum Terrarium", 42, false, "quantum", "a", "quantumPoints", 11, false),
    WILDFIRE("Wildfire", 43, false, "fire", "a"),
    GREY_YOU("Grey You", 44, true, "greygooring", "a", "greyYouPoints", 11, false),
    JOURNEYMAN("Journeyman", 45, false, "map", "a"),
    DINOSAURS("Fall of the Dinosaurs", 46, false, "dinostuffy", "a"),
    SHADOWS_OVER_LOATHING("Avatar of Shadows Over Loathing", 47, false, "aosol", "an"),
    LEGACY_OF_LOATHING("Legacy of Loathing", 48, false, "xx", "a", "legacyPoints", 19, true),
    SMALL("A Shrunken Adventurer am I", 49, false, "kiloskull", "an", null, 0, false, 2, 1, 15),
    WEREPROFESSOR(
        "WereProfessor",
        50,
        true,
        "intrinsic_beast",
        "a",
        "wereProfessorPoints",
        23,
        true,
        5,
        4,
        15),
    ELEVEN_THINGS("11 Things I Hate About U", 51, false, "ihatesu", "an"),
    AVANT_GUARD("Avant Guard", 52, false, "radshield", "an", "avantGuardPoints", 11, false),
    Z_IS_FOR_ZOOTOMIST(
        "Z is for Zootomist", 53, true, "zootomist", "a", "zootomistPoints", 10, false),
    HAT_TRICK("Hat Trick", 54, false, "hat_bycocket", "a"),
    // A "sign" rather than a "path" for some reason
    BAD_MOON("Bad Moon", 999, false, "badmoon", null),
    ;

    public final String name;
    public final int id;
    public final boolean isAvatar;
    public final String image;
    public final String article;
    public final String pointsPreference;
    public final int maximumPoints;
    public final boolean bucket;
    public final int stomachCapacity;
    public final int liverCapacity;
    public final int spleenCapacity;

    public static Set<Path> allPaths() {
      return Arrays.stream(values()).filter(a -> a.getId() > 0).collect(Collectors.toSet());
    }

    Path(
        String name,
        int id,
        boolean isAvatar,
        String image,
        String article,
        String pointsPreference,
        int maximumPoints,
        boolean bucket,
        int stomachCapacity,
        int liverCapacity,
        int spleenCapacity) {
      this.name = name;
      this.id = id;
      this.isAvatar = isAvatar;
      this.image = image + ".gif";
      this.article = article;
      this.pointsPreference = pointsPreference;
      this.maximumPoints = maximumPoints;
      this.bucket = bucket;
      this.stomachCapacity = stomachCapacity;
      this.liverCapacity = liverCapacity;
      this.spleenCapacity = spleenCapacity;
    }

    Path(
        String name,
        int id,
        boolean isAvatar,
        String image,
        String article,
        String pointsPreference,
        int maximumPoints,
        boolean bucket) {
      this(name, id, isAvatar, image, article, pointsPreference, maximumPoints, bucket, 15, 14, 15);
    }

    Path(String name, int id, boolean isAvatar, String image, String article) {
      this(name, id, isAvatar, image, article, null, 0, false);
    }

    public String getName() {
      return this.name;
    }

    public int getId() {
      return this.id;
    }

    public boolean isAvatar() {
      return this.isAvatar;
    }

    public String getImage() {
      return this.image;
    }

    public String description() {
      return (this == Path.NONE)
          ? "no path"
          : (this.article == null) ? this.name : (this.article + " " + this.name + " path");
    }

    public String getPointsPreference() {
      return this.pointsPreference;
    }

    public int getPoints() {
      String pref = getPointsPreference();
      return pref == null ? 0 : Preferences.getInteger(pref);
    }

    public void setPoints(int points) {
      String pref = getPointsPreference();
      if (pref == null || (this.bucket && getPoints() > points)) return;
      points = (this.maximumPoints == 0) ? points : Math.min(maximumPoints, points);
      Preferences.setInteger(pref, points);
    }

    public void incrementPoints(int points) {
      setPoints(getPoints() + points);
    }

    public boolean canUseFamiliars() {
      return switch (this) {
        case AVATAR_OF_BORIS,
            AVATAR_OF_JARLSBERG,
            AVATAR_OF_SNEAKY_PETE,
            ACTUALLY_ED_THE_UNDYING,
            LICENSE_TO_ADVENTURE,
            DARK_GYFFTE -> false;
        default -> true;
      };
    }

    public int getStomachCapacity() {
      return this.stomachCapacity;
    }

    public int getLiverCapacity() {
      return this.liverCapacity;
    }

    public int getSpleenCapacity() {
      return this.spleenCapacity;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private static final Map<String, Path> pathByName = new HashMap<>();
  private static final Map<Integer, Path> pathById = new HashMap<>();
  private static final Map<String, Path> pathByImage = new HashMap<>();

  static {
    for (Path path : Path.values()) {
      pathByName.put(path.name.toLowerCase(), path);
      pathById.put(path.id, path);
      if (path.image != null) {
        pathByImage.put(path.image, path);
      }
    }
  }

  public static Path nameToPath(String name) {
    return pathByName.getOrDefault(name.toLowerCase(), Path.NONE);
  }

  public static Path idToPath(int id) {
    return pathById.getOrDefault(id, Path.NONE);
  }

  public static Path imageToPath(String image) {
    return pathByImage.get(image);
  }
}
