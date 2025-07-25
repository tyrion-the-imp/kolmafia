package net.sourceforge.kolmafia.request;

import static net.sourceforge.kolmafia.utilities.Statics.DateTimeManager;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.TurtleBlessingLevel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.Speculation;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.MultiStringModifier;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DailyLimitDatabase.DailyLimitType;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ConsequenceManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.DreadScrollManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UseSkillRequest extends GenericRequest implements Comparable<UseSkillRequest> {
  private static final HashMap<Integer, UseSkillRequest> ALL_SKILLS = new HashMap<>();
  private static final Pattern SKILLID_PATTERN = Pattern.compile("whichskill=(\\d+)");
  private static final Pattern BOOKID_PATTERN =
      Pattern.compile("preaction=(?:summon|combine)([^&]*)");

  private static final Pattern COUNT_PATTERN = Pattern.compile("quantity=([\\*\\d,]+)");
  private static final Pattern LOVE_POTION_PATTERN = Pattern.compile("<b>Love Potion #(.*?)</b>");

  // <p>1 / 50 casts used today.</td>
  private static final Pattern LIMITED_PATTERN =
      Pattern.compile("<p>(\\d+) / [\\d]+ casts used today\\.</td>", Pattern.DOTALL);

  private static final Pattern SKILLZ_PATTERN =
      Pattern.compile(
          "rel=\\\"(\\d+)\\\".*?<span class=small>(.*?)</font></center></span>", Pattern.DOTALL);

  private static final Pattern SWEAT_PATTERN = Pattern.compile("You get (\\d+)% less Sweaty.");

  public static final String[] BREAKFAST_SKILLS = {
    "Advanced Cocktailcrafting",
    "Advanced Saucecrafting",
    "Canticle of Carboloading",
    "Pastamastery",
    "Summon Crimbo Candy",
    "Lunch Break",
    "Spaghetti Breakfast",
    "Grab a Cold One",
    "Summon Holiday Fun!",
    "Summon Carrot",
    "Summon Kokomo Resort Pass",
    "Generate Irony",
    "Perfect Freeze",
    "Acquire Rhinestones",
    "Prevent Scurvy and Sobriety",
    "Bowl Full of Jelly",
    "Eye and a Twist",
    "Chubby and Plump"
  };

  // These are skills where someone would not care if they are in-run,
  // generally because they do not cost MP
  public static final String[] BREAKFAST_ALWAYS_SKILLS = {
    "Summon Annoyance", "Communism!",
  };

  public static final String[] TOME_SKILLS = {
    "Summon Snowcones",
    "Summon Stickers",
    "Summon Sugar Sheets",
    // Summon Clip Art requires extra parameters
    // "Summon Clip Art",
    "Summon Rad Libs",
    "Summon Smithsness"
  };

  public static final String[] LIBRAM_SKILLS = {
    "Summon Candy Heart",
    "Summon Party Favor",
    "Summon Love Song",
    "Summon BRICKOs",
    "Summon Dice",
    "Summon Resolutions",
    "Summon Taffy",
  };

  public static final String[] GRIMOIRE_SKILLS = {
    "Summon Hilarious Objects",
    "Summon Tasteful Items",
    "Summon Alice's Army Cards",
    "Summon Geeky Gifts",
    "Summon Confiscated Things",
  };

  public static String lastUpdate = "";
  public static int lastSkillUsed = -1;
  public static int lastSkillCount = 0;

  // The following fields are set in an "unmodified" instance
  private final int skillId;
  private final boolean isBuff;
  private final String skillName;
  private final String canonical;
  private String target;
  private long buffCount;

  // The rest of the fields are set in an instance that we actually run
  private String countFieldId;
  private boolean isRunning;
  private int lastReduction = Integer.MAX_VALUE;
  private String lastStringForm = "";

  // if the request we're running requires a certain item to be equipped, store it here
  private AdventureResult forcedItem;
  private AdventureResult forcedNonItem;

  // Tools for casting buffs. The lists are ordered from most bonus buff
  // turns provided by this tool to least.

  public static final BuffTool[] TAMER_TOOLS =
      new BuffTool[] {
        new BuffTool(ItemPool.PRIMITIVE_ALIEN_TOTEM, 25, false, null),
        new BuffTool(ItemPool.FLAIL_OF_THE_SEVEN_ASPECTS, 15, false, null),
        new BuffTool(ItemPool.CHELONIAN_MORNINGSTAR, 10, false, null),
        new BuffTool(ItemPool.MACE_OF_THE_TORTOISE, 5, false, null),
        new BuffTool(ItemPool.OUIJA_BOARD, 2, true, null),
        new BuffTool(ItemPool.TURTLE_TOTEM, 0, false, null),
      };

  public static final BuffTool[] SAUCE_TOOLS =
      new BuffTool[] {
        new BuffTool(ItemPool.WINDSOR_PAN_OF_THE_SOURCE, 15, false, null),
        new BuffTool(ItemPool.FRYING_BRAINPAN, 15, false, null),
        new BuffTool(ItemPool.SAUCEPANIC, 10, false, null),
        new BuffTool(ItemPool.SEVENTEEN_ALARM_SAUCEPAN, 10, false, null),
        new BuffTool(ItemPool.OIL_PAN, 7, true, null),
        new BuffTool(ItemPool.FIVE_ALARM_SAUCEPAN, 5, false, null),
        new BuffTool(ItemPool.SAUCEPAN, 0, false, null),
      };

  public static final BuffTool[] THIEF_TOOLS =
      new BuffTool[] {
        new BuffTool(ItemPool.TRICKSTER_TRIKITIXA, 15, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.ZOMBIE_ACCORDION, 15, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.ALARM_ACCORDION, 15, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.PEACE_ACCORDION, 14, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.ACCORDIONOID_ROCCA, 13, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.PYGMY_CONCERTINETTE, 12, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.GHOST_ACCORDION, 11, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.SQUEEZEBOX_OF_THE_AGES, 10, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(
            ItemPool.SHAKESPEARES_SISTERS_ACCORDION, 10, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.AUTOCALLIOPE, 10, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(
            ItemPool.NON_EUCLIDEAN_NON_ACCORDION, 10, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.ACCORDION_OF_JORDION, 9, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.PENTATONIC_ACCORDION, 7, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.BONE_BANDONEON, 6, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.AEROGEL_ACCORDION, 5, false, null),
        new BuffTool(ItemPool.ANTIQUE_ACCORDION, 5, true, null),
        new BuffTool(ItemPool.ACCORD_ION, 5, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.ACCORDION_FILE, 5, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.BAL_MUSETTE_ACCORDION, 5, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.CAJUN_ACCORDION, 5, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.QUIRKY_ACCORDION, 5, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.SKIPPERS_ACCORDION, 5, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.ROCK_N_ROLL_LEGEND, 5, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.GUANCERTINA, 4, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.MAMAS_SQUEEZEBOX, 3, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.BARITONE_ACCORDION, 2, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.CALAVERA_CONCERTINA, 2, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.BEER_BATTERED_ACCORDION, 1, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.STOLEN_ACCORDION, 0, false, AscensionClass.ACCORDION_THIEF),
        new BuffTool(ItemPool.PARAFFIN_PSEUDOACCORDION, 0, false, null),
        new BuffTool(ItemPool.TOY_ACCORDION, 0, false, null),
      };

  public static final AdventureResult WIZARD_HAT = ItemPool.get(ItemPool.JEWEL_EYED_WIZARD_HAT, 1);
  public static final AdventureResult REPLICA_WIZARD_HAT =
      ItemPool.get(ItemPool.REPLICA_JEWEL_EYED_WIZARD_HAT, 1);

  public static final AdventureResult SHAKESPEARES_SISTERS_ACCORDION =
      ItemPool.get(ItemPool.SHAKESPEARES_SISTERS_ACCORDION, 1);
  public static final AdventureResult BASS_CLARINET = ItemPool.get(ItemPool.BASS_CLARINET, 1);
  public static final AdventureResult PANTOGRAM_PANTS = ItemPool.get(ItemPool.PANTOGRAM_PANTS, 1);
  public static final AdventureResult KREMLIN_BRIEFCASE =
      ItemPool.get(ItemPool.KREMLIN_BRIEFCASE, 1);

  public static final AdventureResult PLEXI_WATCH =
      ItemPool.get(ItemPool.PLEXIGLASS_POCKETWATCH, 1);
  public static final AdventureResult BRIM_BRACELET = ItemPool.get(ItemPool.BRIMSTONE_BRACELET, 1);
  public static final AdventureResult POCKET_SQUARE = ItemPool.get(ItemPool.POCKET_SQUARE, 1);
  public static final AdventureResult SOLITAIRE =
      ItemPool.get(ItemPool.STAINLESS_STEEL_SOLITAIRE, 1);

  public static final AdventureResult NAVEL_RING = ItemPool.get(ItemPool.NAVEL_RING, 1);
  public static final AdventureResult REPLICA_NAVEL_RING =
      ItemPool.get(ItemPool.REPLICA_NAVEL_RING, 1);
  public static final AdventureResult WIRE_BRACELET =
      ItemPool.get(ItemPool.WOVEN_BALING_WIRE_BRACELETS, 1);
  public static final AdventureResult BACON_BRACELET =
      ItemPool.get(ItemPool.BACONSTONE_BRACELET, 1);
  public static final AdventureResult BACON_EARRING = ItemPool.get(ItemPool.BACONSTONE_EARRING, 1);
  public static final AdventureResult SOLID_EARRING =
      ItemPool.get(ItemPool.SOLID_BACONSTONE_EARRING, 1);
  public static final AdventureResult EMBLEM_AKGYXOTH = ItemPool.get(ItemPool.EMBLEM_AKGYXOTH, 1);

  public static final AdventureResult BAYWATCH = ItemPool.get(ItemPool.BAYWATCH, 1);

  private static final AdventureResult[] AVOID_REMOVAL =
      new AdventureResult[] {
        UseSkillRequest.BASS_CLARINET, // -3
        UseSkillRequest.PANTOGRAM_PANTS, // -3
        UseSkillRequest.KREMLIN_BRIEFCASE, // -3
        UseSkillRequest.BRIM_BRACELET, // -3
        UseSkillRequest.PLEXI_WATCH, // -3
        UseSkillRequest.POCKET_SQUARE, // -3
        UseSkillRequest.SOLITAIRE, // -2
        UseSkillRequest.BAYWATCH, // -2
        UseSkillRequest.SHAKESPEARES_SISTERS_ACCORDION, // -1 or -2
        UseSkillRequest.WIZARD_HAT, // -1
        UseSkillRequest.REPLICA_WIZARD_HAT, // -1
        UseSkillRequest.NAVEL_RING, // -1
        UseSkillRequest.REPLICA_NAVEL_RING, // -1
        UseSkillRequest.WIRE_BRACELET, // -1
        UseSkillRequest.BACON_BRACELET, // -1, discontinued item
        UseSkillRequest.BACON_EARRING, // -1
        UseSkillRequest.SOLID_EARRING, // -1
        UseSkillRequest.EMBLEM_AKGYXOTH, // -1
      };

  private enum SkillStatus {
    // The skill was used successfully.
    SUCCESS,
    // The skill was not used successfully, but automation can continue.
    WARNING,
    // We encountered a critical error, and want to halt automation
    ERROR,
  }

  // Other known MP cost items:
  // Vile Vagrant Vestments (-5) - unlikely to be equippable during Ronin.
  // Idol of Ak'gyxoth (-1) - off-hand, would require special handling.

  private UseSkillRequest(final int skillId) {
    super(UseSkillRequest.chooseURL(skillId));

    this.skillId = skillId;
    this.skillName = SkillDatabase.getSkillName(this.skillId);
    this.canonical = StringUtilities.getCanonicalName(this.skillName);
    this.isBuff = SkillDatabase.isBuff(this.skillId);
    this.target = null;

    this.countFieldId = null;
    this.addFormFields();
  }

  private static String chooseURL(final int skillId) {
    return SkillDatabase.isBookshelfSkill(skillId) ? "campground.php" : "runskillz.php";
  }

  private void addFormFields() {
    switch (this.skillId) {
      case SkillPool.SNOWCONE -> this.addFormField("preaction", "summonsnowcone");
      case SkillPool.STICKER -> this.addFormField("preaction", "summonstickers");
      case SkillPool.SUGAR -> this.addFormField("preaction", "summonsugarsheets");
      case SkillPool.CLIP_ART -> this.addFormField("preaction", "combinecliparts");
      case SkillPool.RAD_LIB -> this.addFormField("preaction", "summonradlibs");
      case SkillPool.SMITHSNESS -> this.addFormField("preaction", "summonsmithsness");
      case SkillPool.HILARIOUS -> this.addFormField("preaction", "summonhilariousitems");
      case SkillPool.TASTEFUL -> this.addFormField("preaction", "summonspencersitems");
      case SkillPool.CARDS -> this.addFormField("preaction", "summonaa");
      case SkillPool.GEEKY -> this.addFormField("preaction", "summonthinknerd");
      case SkillPool.CANDY_HEART -> this.addFormField("preaction", "summoncandyheart");
      case SkillPool.PARTY_FAVOR -> this.addFormField("preaction", "summonpartyfavor");
      case SkillPool.LOVE_SONG -> this.addFormField("preaction", "summonlovesongs");
      case SkillPool.BRICKOS -> this.addFormField("preaction", "summonbrickos");
      case SkillPool.DICE -> this.addFormField("preaction", "summongygax");
      case SkillPool.RESOLUTIONS -> this.addFormField("preaction", "summonresolutions");
      case SkillPool.TAFFY -> this.addFormField("preaction", "summontaffy");
      case SkillPool.CONFISCATOR -> this.addFormField("preaction", "summonconfiscators");
      default -> {
        this.addFormField("action", "Skillz");
        this.addFormField("whichskill", String.valueOf(this.skillId));
        this.addFormField("ajax", "1");
      }
    }
  }

  public void setTarget(final String target) {
    if (this.isBuff) {
      if (target == null
          || target.trim().isEmpty()
          || target.equals(KoLCharacter.getPlayerId())
          || target.equals(KoLCharacter.getUserName())) {
        this.target = null;
        this.addFormField("targetplayer", KoLCharacter.getPlayerId());
      } else {
        this.target = ContactManager.getPlayerName(target);
        this.addFormField("targetplayer", ContactManager.getPlayerId(target));
      }
    } else {
      this.target = null;
    }
  }

  public void setBuffCount(long buffCount) {
    this.countFieldId = "quantity";

    long maxPossible = 0;

    if (SkillDatabase.isSoulsauceSkill(skillId)) {
      maxPossible = KoLCharacter.getSoulsauce() / SkillDatabase.getSoulsauceCost(skillId);
    } else if (SkillDatabase.isThunderSkill(skillId)) {
      maxPossible = KoLCharacter.getThunder() / SkillDatabase.getThunderCost(skillId);
    } else if (SkillDatabase.isRainSkill(skillId)) {
      maxPossible = KoLCharacter.getRain() / SkillDatabase.getRainCost(skillId);
    } else if (SkillDatabase.isLightningSkill(skillId)) {
      maxPossible = KoLCharacter.getLightning() / SkillDatabase.getLightningCost(skillId);
    } else if (SkillDatabase.isVampyreSkill(skillId)) {
      maxPossible = KoLCharacter.getCurrentHP() / SkillDatabase.getHPCost(skillId);
    } else {
      long mpCost = SkillDatabase.getMPConsumptionById(this.skillId);
      long availableMP = KoLCharacter.getCurrentMP();
      if (mpCost == 0) {
        maxPossible = this.getMaximumCast();
      } else if (this.skillId == SkillPool.SEEK_OUT_A_BIRD) {
        maxPossible = SkillDatabase.birdSkillCasts(availableMP);
      } else if (SkillDatabase.isLibramSkill(this.skillId)) {
        maxPossible = SkillDatabase.libramSkillCasts(availableMP);
      } else {
        maxPossible = Math.min(this.getMaximumCast(), availableMP / mpCost);
      }
    }

    if (buffCount < 1) {
      buffCount += maxPossible;
    } else if (buffCount == Integer.MAX_VALUE) {
      buffCount = maxPossible;
    }

    this.buffCount = buffCount;
  }

  private record ReplaceEffect(int skill, int item, int baseEffect, int newEffect) {}

  private static final List<ReplaceEffect> replaceEffects =
      List.of(
          new ReplaceEffect(
              SkillPool.SNARL_OF_THE_TIMBERWOLF,
              ItemPool.VELOUR_VOULGE,
              EffectPool.SNARL_OF_THE_TIMBERWOLF,
              EffectPool.SNARL_OF_THREE_TIMBERWOLVES),
          new ReplaceEffect(
              SkillPool.SCARYSAUCE,
              ItemPool.VELOUR_VISCOMETER,
              EffectPool.SCARYSAUCE,
              EffectPool.SCARIERSAUCE),
          new ReplaceEffect(
              SkillPool.DIRGE_OF_DREADFULNESS,
              ItemPool.VELOUR_VAQUEROS,
              EffectPool.DIRGE_OF_DREADFULNESS,
              EffectPool.DIRGE_OF_DREADFULNESS_REMASTERED),
          new ReplaceEffect(
              SkillPool.EMPATHY_OF_THE_NEWT,
              ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD,
              EffectPool.EMPATHY,
              EffectPool.THOUGHTFUL_EMPATHY));

  private record AdditionalEffect(int skill, int item, int newEffect) {}

  private static final List<AdditionalEffect> additionalEffects =
      List.of(
          new AdditionalEffect(
              SkillPool.SEAL_CLUBBING_FRENZY,
              ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD,
              EffectPool.SLIPPERY_AS_A_SEAL),
          new AdditionalEffect(
              SkillPool.PATIENCE_OF_THE_TORTOISE,
              ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD,
              EffectPool.STRENGTH_OF_THE_TORTOISE),
          new AdditionalEffect(
              SkillPool.MANICOTTI_MEDITATION,
              ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD,
              EffectPool.TUBES_OF_UNIVERSAL_MEAT),
          new AdditionalEffect(
              SkillPool.SAUCE_CONTEMPLATION,
              ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD,
              EffectPool.LUBRICATING_SAUCE),
          new AdditionalEffect(
              SkillPool.DISCO_AEROBICS,
              ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD,
              EffectPool.DISCO_OVER_MATTER),
          new AdditionalEffect(
              SkillPool.MOXIE_OF_THE_MARIACHI,
              ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD,
              EffectPool.MARIACHI_MOISTURE));

  public static int requiredItemForSkillEffect(int skillId, int effId) {
    for (var replace : replaceEffects) {
      if (skillId == replace.skill) {
        if (effId == replace.baseEffect) {
          return -1;
        } else if (effId == replace.newEffect) {
          return replace.item;
        }
      }
    }

    for (var add : additionalEffects) {
      if (skillId == add.skill) {
        if (effId == add.newEffect) {
          return add.item;
        }
      }
    }

    return -1;
  }

  private void setDesiredEffect(int effId) {
    if (effId == -1) return;

    for (var replace : replaceEffects) {
      if (skillId == replace.skill) {
        if (effId == replace.baseEffect) {
          this.forcedNonItem = ItemPool.get(replace.item);
          return;
        } else if (effId == replace.newEffect) {
          this.forcedItem = ItemPool.get(replace.item);
          return;
        }
      }
    }

    for (var add : additionalEffects) {
      if (skillId == add.skill) {
        if (effId == add.newEffect) {
          this.forcedItem = ItemPool.get(add.item);
          return;
        }
      }
    }
  }

  @Override
  public int compareTo(final UseSkillRequest o) {
    if (!(o instanceof UseSkillRequest)) {
      return -1;
    }

    long mpDifference =
        SkillDatabase.getMPConsumptionById(this.skillId)
            - SkillDatabase.getMPConsumptionById(o.skillId);

    return mpDifference < 0
        ? -1
        : mpDifference > 0 ? 1 : this.skillName.compareToIgnoreCase(o.skillName);
  }

  public int getSkillId() {
    return this.skillId;
  }

  public String getSkillName() {
    return this.skillName;
  }

  public String getCanonical() {
    return this.canonical;
  }

  private static final AdventureResult TAINTED_LOVE_POTION =
      EffectPool.get(EffectPool.TAINTED_LOVE_POTION);

  public boolean isEffective() {
    return !KoLCharacter.inGLover() || KoLCharacter.hasGs(this.getSkillName());
  }

  public long getMaximumCast() {
    if (!isEffective()) {
      return 0;
    }

    switch (this.skillId) {
        // Rainbow Gravitation can be cast 3 times per day.  Each
        // casting consumes five elemental wads and a twinkly wad

      case SkillPool.RAINBOW_GRAVITATION -> {
        var maximumCast = Math.max(3 - Preferences.getInteger("prismaticSummons"), 0);
        maximumCast = Math.min(InventoryManager.getAccessibleCount(ItemPool.COLD_WAD), maximumCast);
        maximumCast = Math.min(InventoryManager.getAccessibleCount(ItemPool.HOT_WAD), maximumCast);
        maximumCast =
            Math.min(InventoryManager.getAccessibleCount(ItemPool.SLEAZE_WAD), maximumCast);
        maximumCast =
            Math.min(InventoryManager.getAccessibleCount(ItemPool.SPOOKY_WAD), maximumCast);
        maximumCast =
            Math.min(InventoryManager.getAccessibleCount(ItemPool.STENCH_WAD), maximumCast);
        return Math.min(InventoryManager.getAccessibleCount(ItemPool.TWINKLY_WAD), maximumCast);
      }

        // Hobo skills
      case SkillPool.THINGFINDER,
          SkillPool.BENETTONS,
          SkillPool.ELRONS,
          SkillPool.COMPANIONSHIP,
          SkillPool.PRECISION -> {
        // If you can't cast them, return zero
        if (!KoLCharacter.isAccordionThief() || KoLCharacter.getLevel() < 15) {
          return 0;
        }
        // Otherwise let daily limits database handle remaining casts
      }

        // Zombie Master skills
      case SkillPool.SUMMON_MINION -> {
        return KoLCharacter.getAvailableMeat() / 100;
      }
      case SkillPool.SUMMON_HORDE -> {
        return KoLCharacter.getAvailableMeat() / 1000;
      }

        // Avatar of Jarlsberg skills
      case SkillPool.EGGMAN -> {
        boolean haveEgg = InventoryManager.getCount(ItemPool.COSMIC_EGG) > 0;
        boolean eggActive = KoLCharacter.getCompanion() == Companion.EGGMAN;
        return (haveEgg && !eggActive) ? 1 : 0;
      }
      case SkillPool.RADISH_HORSE -> {
        boolean haveVeggie = InventoryManager.getCount(ItemPool.COSMIC_VEGETABLE) > 0;
        boolean radishActive = KoLCharacter.getCompanion() == Companion.RADISH;
        return (haveVeggie && !radishActive) ? 1 : 0;
      }
      case SkillPool.HIPPOTATO -> {
        boolean havePotato = InventoryManager.getCount(ItemPool.COSMIC_POTATO) > 0;
        boolean hippoActive = KoLCharacter.getCompanion() == Companion.HIPPO;
        return (havePotato && !hippoActive) ? 1 : 0;
      }
      case SkillPool.CREAMPUFF -> {
        boolean haveCream = InventoryManager.getCount(ItemPool.COSMIC_CREAM) > 0;
        boolean creampuffActive = KoLCharacter.getCompanion() == Companion.CREAM;
        return (haveCream && !creampuffActive) ? 1 : 0;
      }
      case SkillPool.DEEP_VISIONS -> {
        return KoLCharacter.getMaximumHP() >= 500 ? 1 : 0;
      }
      case SkillPool.WAR_BLESSING, SkillPool.SHE_WHO_WAS_BLESSING, SkillPool.STORM_BLESSING -> {
        return KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.SPIRIT_PARIAH))
            ? 0
            : 1;
      }
      case SkillPool.SPIRIT_BOON -> {
        return KoLCharacter.getBlessingLevel().isBlessing() ? Integer.MAX_VALUE : 0;
      }
      case SkillPool.TURTLE_POWER -> {
        return KoLCharacter.getBlessingLevel() == TurtleBlessingLevel.GLORIOUS_BLESSING
                && !Preferences.getBoolean("_turtlePowerCast")
            ? 1
            : 0;
      }
      case SkillPool.SUMMON_ANNOYANCE -> {
        if (Preferences.getInteger("summonAnnoyanceCost") == 11) {
          // If we made it this far, you should have the skill.
          // Update its cost.
          GenericRequest req =
              new GenericRequest("desc_skill.php?whichskill=" + this.skillId + "&self=true");
          RequestThread.postRequest(req);
        }
        if (Preferences.getBoolean("_summonAnnoyanceUsed")) {
          return 0;
        }
        if (Preferences.getInteger("availableSwagger")
            < Preferences.getInteger("summonAnnoyanceCost")) {
          return 0;
        }
        return 1;
      }
      case SkillPool.CALCULATE_THE_UNIVERSE -> {
        if (KoLCharacter.getAdventuresLeft() == 0) {
          return 0;
        }
        int skillLevel = Preferences.getInteger("skillLevel144");
        if (!KoLCharacter.canInteract()) {
          skillLevel = Math.min(skillLevel, 3);
        }
        int casts = Preferences.getInteger("_universeCalculated");
        return Math.max(skillLevel - casts, 0);
      }
      case SkillPool.ANCESTRAL_RECALL -> {
        return Math.min(
            10 - Preferences.getInteger("_ancestralRecallCasts"),
            InventoryManager.getAccessibleCount(ItemPool.BLUE_MANA));
      }
      case SkillPool.DARK_RITUAL -> {
        return InventoryManager.getAccessibleCount(ItemPool.BLACK_MANA);
      }
      case SkillPool.INTERNAL_SODA_MACHINE -> {
        long meatLimit = KoLCharacter.getAvailableMeat() / 20;
        long mpLimit =
            (int) Math.ceil((KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP()) / 10.0);
        return Math.min(meatLimit, mpLimit);
      }
      case SkillPool.STACK_LUMPS -> {
        return 1;
      }
      case SkillPool.LOVE_MIXOLOGY -> {
        return InventoryManager.getAccessibleCount(ItemPool.LOVE_POTION_XYZ) > 0
                || KoLConstants.activeEffects.contains(UseSkillRequest.TAINTED_LOVE_POTION)
            ? 0
            : 1;
      }
      case SkillPool.SEEK_OUT_A_BIRD -> {
        // On the seventh cast of the day, you are given the
        // option to change your favorite bird.
        //
        // The choice adventure to select this is 1399.
        // Option 1 changes the bird and the cast succeeds
        // Option 2 does not change and the cast fails.
        //
        // Therefore:
        //
        // If you want option 1, casts are limited by MP
        // If you want option 2, you get 6 casts per day
        //
        // If you have already summoned at least 7 birds today,
        // you are past the choice and casts are unlimited

        int option = Preferences.getInteger("choiceAdventure1399");
        int birds = Preferences.getInteger("_birdsSoughtToday");
        return (birds < 7 && option != 1) ? 6 - birds : Long.MAX_VALUE;
      }
      case SkillPool.AUG_1ST_MOUNTAIN_CLIMBING_DAY,
          SkillPool.AUG_2ND_FIND_AN_ELEVENLEAF_CLOVER_DAY,
          SkillPool.AUG_3RD_WATERMELON_DAY,
          SkillPool.AUG_4TH_WATER_BALLOON_DAY,
          SkillPool.AUG_5TH_OYSTER_DAY,
          SkillPool.AUG_6TH_FRESH_BREATH_DAY,
          SkillPool.AUG_7TH_LIGHTHOUSE_DAY,
          SkillPool.AUG_8TH_CAT_DAY,
          SkillPool.AUG_9TH_HAND_HOLDING_DAY,
          SkillPool.AUG_10TH_WORLD_LION_DAY,
          SkillPool.AUG_11TH_PRESIDENTIAL_JOKE_DAY,
          SkillPool.AUG_12TH_ELEPHANT_DAY,
          SkillPool.AUG_13TH_LEFTOFF_HANDERS_DAY,
          SkillPool.AUG_14TH_FINANCIAL_AWARENESS_DAY,
          SkillPool.AUG_15TH_RELAXATION_DAY,
          SkillPool.AUG_16TH_ROLLER_COASTER_DAY,
          SkillPool.AUG_17TH_THRIFTSHOP_DAY,
          SkillPool.AUG_18TH_SERENDIPITY_DAY,
          SkillPool.AUG_19TH_HONEY_BEE_AWARENESS_DAY,
          SkillPool.AUG_20TH_MOSQUITO_DAY,
          SkillPool.AUG_21ST_SPUMONI_DAY,
          SkillPool.AUG_22ND_TOOTH_FAIRY_DAY,
          SkillPool.AUG_23RD_RIDE_THE_WIND_DAY,
          SkillPool.AUG_24TH_WAFFLE_DAY,
          SkillPool.AUG_25TH_BANANA_SPLIT_DAY,
          SkillPool.AUG_26TH_TOILET_PAPER_DAY,
          SkillPool.AUG_27TH_JUST_BECAUSE_DAY,
          SkillPool.AUG_28TH_RACE_YOUR_MOUSE_DAY,
          SkillPool.AUG_29TH_MORE_HERBS_LESS_SALT_DAY,
          SkillPool.AUG_30TH_BEACH_DAY,
          SkillPool.AUG_31ST_CABERNET_SAUVIGNON_DAY -> {
        // if we've already cast it, we can't cast it again
        if (!DailyLimitType.CAST.getDailyLimit(this.skillId).hasUsesRemaining()) {
          return 0;
        }
        // we can cast up to 5 skills normally
        if (Preferences.getInteger("_augSkillsCast") < 5) {
          return 1;
        }
        // outside ronin / hardcore we can also cast today's skill
        if (KoLCharacter.canInteract()) {
          var date = DateTimeManager.getRolloverDateTime().getDayOfMonth();
          var isTodaySkill = skillId == date - 1 + SkillPool.AUG_1ST_MOUNTAIN_CLIMBING_DAY;
          if (isTodaySkill) {
            return 1;
          }
        }
        return 0;
      }
    }

    var dailyLimit = DailyLimitType.CAST.getDailyLimit(this.skillId);
    if (dailyLimit != null) {
      return dailyLimit.getUsesRemaining();
    }

    return Long.MAX_VALUE;
  }

  @Override
  public String toString() {
    if (this.lastReduction == KoLCharacter.getManaCostAdjustment()
        && !SkillDatabase.isLibramSkill(this.skillId)) {
      return this.lastStringForm;
    }

    this.lastReduction = KoLCharacter.getManaCostAdjustment();
    long mpCost = SkillDatabase.getMPConsumptionById(this.skillId);
    int advCost = SkillDatabase.getAdventureCost(this.skillId);
    int soulCost = SkillDatabase.getSoulsauceCost(this.skillId);
    int thunderCost = SkillDatabase.getThunderCost(this.skillId);
    int rainCost = SkillDatabase.getRainCost(skillId);
    int lightningCost = SkillDatabase.getLightningCost(skillId);
    int hpCost = SkillDatabase.getHPCost(skillId);
    int numCosts = 0;
    int itemCost = 0;
    StringBuilder costString = new StringBuilder();
    costString.append(this.skillName);
    costString.append(" (");
    if (advCost > 0) {
      costString.append(advCost);
      costString.append(" adv");
      numCosts++;
    }
    if (soulCost > 0) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append(soulCost);
      costString.append(" soulsauce");
      numCosts++;
    }
    if (this.skillId == SkillPool.SUMMON_ANNOYANCE) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append(Preferences.getInteger("summonAnnoyanceCost"));
      costString.append(" swagger");
      numCosts++;
    }
    if (this.skillId == SkillPool.HEALING_SALVE) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append("1 white mana");
      itemCost++;
      numCosts++;
    } else if (this.skillId == SkillPool.DARK_RITUAL) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append("1 black mana");
      itemCost++;
      numCosts++;
    } else if (this.skillId == SkillPool.LIGHTNING_BOLT_CARD) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append("1 red mana");
      itemCost++;
      numCosts++;
    } else if (this.skillId == SkillPool.GIANT_GROWTH) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append("1 green mana");
      itemCost++;
      numCosts++;
    } else if (this.skillId == SkillPool.ANCESTRAL_RECALL) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append("1 blue mana");
      itemCost++;
      numCosts++;
    }
    if (thunderCost > 0) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append(thunderCost);
      costString.append(" dB of thunder");
      numCosts++;
    }
    if (rainCost > 0) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append(rainCost);
      costString.append(" drops of rain");
      numCosts++;
    }
    if (lightningCost > 0) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append(lightningCost);
      costString.append(" bolts of lightning");
      numCosts++;
    }
    if (hpCost > 0) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append(hpCost);
      costString.append(" hp");
      numCosts++;
    }
    if (mpCost > 0
        || (advCost == 0
            && soulCost == 0
            && this.skillId != SkillPool.SUMMON_ANNOYANCE
            && thunderCost == 0
            && rainCost == 0
            && lightningCost == 0
            && itemCost == 0
            && hpCost == 0)) {
      if (numCosts > 0) {
        costString.append(", ");
      }
      costString.append(mpCost);
      costString.append(" mp");
    }
    costString.append(")");
    this.lastStringForm = costString.toString();
    return this.lastStringForm;
  }

  private static boolean canSwitchToItem(final AdventureResult item) {
    return !KoLCharacter.hasEquipped(item)
        && EquipmentManager.canEquip(item.getName())
        && InventoryManager.hasItem(item, false);
  }

  private void equipForSkill(final int skillId, final AdventureResult item) {
    if (KoLCharacter.hasEquipped(item)) {
      return;
    }

    if (!InventoryManager.retrieveItem(item)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot acquire: " + item.getName() + ".");
      return;
    }

    var slotType =
        EquipmentManager.consumeFilterToEquipmentType(
            ItemDatabase.getConsumptionType(item.getItemId()));

    // if we have a 2/3-handed weapon equipped, we need to remove it
    if (slotType == Slot.OFFHAND && EquipmentManager.getWeaponHandedness() > 1) {
      (new EquipmentRequest(EquipmentRequest.UNEQUIP, Slot.WEAPON)).run();
      (new EquipmentRequest(item, slotType)).run();
      return;
    }

    if (slotType != Slot.ACCESSORY1) {
      if (skillId == SkillPool.REST_UPSIDE_DOWN) {
        // this is a HP restore, so we need to ensure that the current back item doesn't have +HP
        if (!isValidSwitch(Slot.CONTAINER, item, skillId)) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR,
              "Replacing back item with bat wings will reduce max HP and may loop.");
          return;
        }
      }
      (new EquipmentRequest(item, slotType)).run();
      return;
    }

    // Find an accessory slot to equip the accessory
    boolean slot1Allowed = isValidSwitch(Slot.ACCESSORY1, item, skillId);
    boolean slot2Allowed = isValidSwitch(Slot.ACCESSORY2, item, skillId);
    boolean slot3Allowed = isValidSwitch(Slot.ACCESSORY3, item, skillId);

    Slot slot =
        UseSkillRequest.attemptSwitch(skillId, item, slot1Allowed, slot2Allowed, slot3Allowed);

    if (slot == Slot.NONE) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Cannot choose slot to equip " + item.getName() + ".");
    }
  }

  private void unequipForSkill(final AdventureResult item) {
    if (!KoLCharacter.hasEquipped(item)) {
      return;
    }

    var slot = KoLCharacter.equipmentSlot(item);

    (new EquipmentRequest(EquipmentRequest.UNEQUIP, slot)).run();
  }

  public final void optimizeEquipment() {
    boolean isBuff = SkillDatabase.isBuff(skillId);

    if (isBuff) {
      if (SkillDatabase.isTurtleTamerBuff(skillId)) {
        UseSkillRequest.prepareTool(UseSkillRequest.TAMER_TOOLS, skillId);
      } else if (SkillDatabase.isSaucerorBuff(skillId)) {
        UseSkillRequest.prepareTool(UseSkillRequest.SAUCE_TOOLS, skillId);
      } else if (SkillDatabase.isAccordionThiefSong(skillId)) {
        UseSkillRequest.prepareTool(UseSkillRequest.THIEF_TOOLS, skillId);
      }
    }

    if (this.forcedItem != null) {
      equipForSkill(skillId, this.forcedItem);
    }

    if (this.forcedNonItem != null) {
      unequipForSkill(this.forcedNonItem);
    }

    if (SkillDatabase.getSkillTags(skillId).contains(SkillDatabase.SkillTag.NONCOMBAT)) {
      var possibleEquipment =
          ModifierDatabase.getNonCombatSkillProviders().stream()
              .filter(l -> l.getType() == ModifierType.ITEM)
              .filter(
                  l ->
                      ModifierDatabase.getMultiStringModifier(
                              l, MultiStringModifier.CONDITIONAL_SKILL_EQUIPPED)
                          .stream()
                          .mapToInt(SkillDatabase::getSkillId)
                          .anyMatch(i -> i == skillId))
              .map(l -> ItemPool.get(l.getIntKey()))
              .toList();

      if (!possibleEquipment.isEmpty()) {
        possibleEquipment.stream()
            .filter(i -> canSwitchToItem(i) || KoLCharacter.hasEquipped(i))
            .findFirst()
            .ifPresentOrElse(
                i -> {
                  this.forcedItem = i;
                  equipForSkill(skillId, i);
                },
                () ->
                    KoLmafia.updateDisplay(
                        MafiaState.ERROR,
                        "Cannot acquire: "
                            + possibleEquipment.stream()
                                .map(AdventureResult::getName)
                                .collect(Collectors.joining(", "))
                            + "."));
      }
    }

    if (Preferences.getBoolean("switchEquipmentForBuffs")) {
      reduceManaConsumption(skillId);
    }
  }

  private boolean isValidSwitch(
      final Slot slotId, final AdventureResult newItem, final int skillId) {
    if (newItem.equals(this.forcedNonItem)) {
      return false;
    }

    AdventureResult item = EquipmentManager.getEquipment(slotId);
    if (item.equals(EquipmentRequest.UNEQUIP)) return true;

    if (item.equals(this.forcedItem)) {
      return false;
    }

    for (int i = 0; i < UseSkillRequest.AVOID_REMOVAL.length; ++i) {
      if (item.equals(UseSkillRequest.AVOID_REMOVAL[i])) {
        return false;
      }
    }

    var id = item.getItemId();
    // don't lose the Juju mask buff
    if (id == ItemPool.JUJU_MOJO_MASK || id == ItemPool.REPLICA_JUJU_MOJO_MASK) {
      return false;
    }

    Speculation spec_old = new Speculation();
    var predictions_old = spec_old.calculate().predict();

    Speculation spec = new Speculation();
    spec.equip(slotId, newItem);
    var predictions = spec.calculate().predict();

    double MPgap = KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP();
    double deltaMP =
        predictions.get(DerivedModifier.BUFFED_MP) - predictions_old.get(DerivedModifier.BUFFED_MP);
    // Make sure we do not lose mp in the switch
    if (MPgap + deltaMP < 0) {
      return false;
    }
    // Make sure we do not reduce max hp in the switch, to avoid loops when casting a heal
    if (predictions_old.get(DerivedModifier.BUFFED_HP)
        > predictions.get(DerivedModifier.BUFFED_HP)) {
      return false;
    }
    // Don't allow if we'd lose a song in the switch
    Modifiers mods = spec.getModifiers();
    int predictedSongLimit =
        3
            + (int) mods.getDouble(DoubleModifier.ADDITIONAL_SONG)
            + (mods.getBoolean(BooleanModifier.FOUR_SONGS) ? 1 : 0);
    int predictedSongsNeeded =
        UseSkillRequest.songsActive() + (UseSkillRequest.newSong(skillId) ? 1 : 0);
    if (predictedSongsNeeded > predictedSongLimit) {
      return false;
    }
    return true;
  }

  private static Slot attemptSwitch(
      final int skillId,
      final AdventureResult item,
      final boolean slot1Allowed,
      final boolean slot2Allowed,
      final boolean slot3Allowed) {
    if (slot3Allowed) {
      (new EquipmentRequest(item, Slot.ACCESSORY3)).run();
      return Slot.ACCESSORY3;
    }

    if (slot2Allowed) {
      (new EquipmentRequest(item, Slot.ACCESSORY2)).run();
      return Slot.ACCESSORY2;
    }

    if (slot1Allowed) {
      (new EquipmentRequest(item, Slot.ACCESSORY1)).run();
      return Slot.ACCESSORY1;
    }

    return Slot.NONE;
  }

  private void reduceManaConsumption(final int skillId) {
    // Never bother trying to reduce mana consumption when casting
    // expensive skills or a libram skill

    long mpCost = SkillDatabase.getMPConsumptionById(skillId);

    if (mpCost > 50 || SkillDatabase.isLibramSkill(skillId) || mpCost == 0) {
      return;
    }

    // MP is cheap in aftercore, so save server hits
    if (KoLCharacter.canInteract()) {
      return;
    }

    // Try items

    for (int i = 0; i < UseSkillRequest.AVOID_REMOVAL.length; ++i) {
      // If you can't reduce cost further, stop
      if (mpCost == 1 || KoLCharacter.currentNumericModifier(DoubleModifier.MANA_COST) <= -3) {
        return;
      }

      AdventureResult item = UseSkillRequest.AVOID_REMOVAL[i];

      // If you haven't got it or can't wear it, don't evaluate further
      if (!UseSkillRequest.canSwitchToItem(item)) {
        continue;
      }

      // Items in G-Lover don't help unless they have G's
      if (KoLCharacter.inGLover() && !KoLCharacter.hasGs(item.getName())) {
        continue;
      }

      // Some items only sometimes help
      if (item.getItemId() == ItemPool.PANTOGRAM_PANTS) {
        if (!Preferences.getString("_pantogramModifier").contains("Mana Cost")) {
          continue;
        }
      } else if (item.getItemId() == ItemPool.KREMLIN_BRIEFCASE) {
        if (ModifierDatabase.getItemModifiers(ItemPool.KREMLIN_BRIEFCASE)
                .getDouble(DoubleModifier.MANA_COST)
            == 0) {
          continue;
        }
      }

      // If you won't lose max hp, current mp, or songs, use it
      Slot slot = EquipmentManager.itemIdToEquipmentType(item.getItemId());
      if (slot == Slot.ACCESSORY1) {
        // First determine which slots are available for switching in
        // MP reduction items.  This has do be done inside the loop now
        // that max HP/MP prediction is done, since two changes that are
        // individually harmless might add up to a loss of points.
        boolean slot1Allowed = isValidSwitch(Slot.ACCESSORY1, item, skillId);
        boolean slot2Allowed = isValidSwitch(Slot.ACCESSORY2, item, skillId);
        boolean slot3Allowed = isValidSwitch(Slot.ACCESSORY3, item, skillId);

        UseSkillRequest.attemptSwitch(skillId, item, slot1Allowed, slot2Allowed, slot3Allowed);
      } else {
        if (isValidSwitch(slot, item, skillId)) {
          (new EquipmentRequest(item, slot)).run();
        }
      }

      // Cost may have changed
      mpCost = SkillDatabase.getMPConsumptionById(skillId);
    }
  }

  public static final int songLimit() {
    int rv = 3;
    if (KoLCharacter.currentBooleanModifier(BooleanModifier.FOUR_SONGS)) {
      ++rv;
    }

    rv += (int) KoLCharacter.currentNumericModifier(DoubleModifier.ADDITIONAL_SONG);

    return rv;
  }

  private static int songsActive() {
    int count = 0;

    AdventureResult[] effects = new AdventureResult[KoLConstants.activeEffects.size()];
    KoLConstants.activeEffects.toArray(effects);
    for (AdventureResult effect : effects) {
      String skillName = UneffectRequest.effectToSkill(effect.getName());
      if (SkillDatabase.contains(skillName)) {
        int skillId = SkillDatabase.getSkillId(skillName);
        if (SkillDatabase.isAccordionThiefSong(skillId)) {
          count++;
        }
      }
    }
    return count;
  }

  private static Boolean newSong(final int skillId) {
    if (!SkillDatabase.isAccordionThiefSong(skillId)) {
      return false;
    }

    AdventureResult[] effects = new AdventureResult[KoLConstants.activeEffects.size()];
    KoLConstants.activeEffects.toArray(effects);
    for (AdventureResult effect : effects) {
      String skillName = UneffectRequest.effectToSkill(effect.getName());
      if (SkillDatabase.contains(skillName)) {
        int effectSkillId = SkillDatabase.getSkillId(skillName);
        if (effectSkillId == skillId) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public void run() {
    if (this.isRunning) {
      return;
    }

    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    UseSkillRequest.lastUpdate = "";

    if (this.buffCount == 0) {
      // Silently do nothing
      return;
    }

    if (!KoLCharacter.hasSkill(this.skillName)) {
      UseSkillRequest.lastUpdate = "You don't know how to cast " + this.skillName + ".";
      return;
    }

    long available = this.getMaximumCast();
    if (available == 0) {
      // We could print something
      return;
    }

    long desired = this.buffCount;
    if (available < desired) {
      // We SHOULD print something here
      KoLmafia.updateDisplay(
          "(Only " + available + " casts of " + this.skillName + " currently available.)");
    }

    this.setBuffCount(Math.min(desired, available));

    if (this.skillId == SkillPool.SUMMON_MINION || this.skillId == SkillPool.SUMMON_HORDE) {
      ChoiceManager.setSkillUses((int) this.buffCount);
    }

    // Optimizing equipment can involve changing equipment.
    // Save a checkpoint so we can restore previous equipment.
    try (Checkpoint checkpoint = new Checkpoint()) {
      optimizeEquipment();
      if (KoLmafia.refusesContinue()) {
        UseSkillRequest.lastUpdate = "Unable to change equipment to cast skill.";
        return;
      }

      this.isRunning = true;
      this.useSkillLoop();
    } finally {
      this.isRunning = false;
    }
  }

  private static final AdventureResult ONCE_CURSED = EffectPool.get(EffectPool.ONCE_CURSED);
  private static final AdventureResult TWICE_CURSED = EffectPool.get(EffectPool.TWICE_CURSED);
  private static final AdventureResult THRICE_CURSED = EffectPool.get(EffectPool.THRICE_CURSED);

  private void useSkillLoop() {
    if (KoLmafia.refusesContinue()) {
      return;
    }

    long castsRemaining = this.buffCount;
    if (castsRemaining == 0) {
      return;
    }

    if (this.skillId == SkillPool.SHAKE_IT_OFF
        || (this.skillId == SkillPool.BITE_MINION
            && KoLCharacter.hasSkill(SkillPool.DEVOUR_MINIONS))) {
      boolean cursed =
          KoLConstants.activeEffects.contains(UseSkillRequest.ONCE_CURSED)
              || KoLConstants.activeEffects.contains(UseSkillRequest.TWICE_CURSED)
              || KoLConstants.activeEffects.contains(UseSkillRequest.THRICE_CURSED);

      // If on the Hidden Apartment Quest, and have a Curse, and skill will remove it,
      // ask if you are sure you want to lose it.
      if (cursed
          && Preferences.getInteger("hiddenApartmentProgress") < 7
          && !InputFieldUtilities.confirm("That will remove your Cursed effect. Are you sure?")) {
        return;
      }
    }

    if (this.skillId == SkillPool.RAINBOW_GRAVITATION) {
      // Acquire necessary wads
      InventoryManager.retrieveItem(ItemPool.COLD_WAD, (int) castsRemaining);
      InventoryManager.retrieveItem(ItemPool.HOT_WAD, (int) castsRemaining);
      InventoryManager.retrieveItem(ItemPool.SLEAZE_WAD, (int) castsRemaining);
      InventoryManager.retrieveItem(ItemPool.SPOOKY_WAD, (int) castsRemaining);
      InventoryManager.retrieveItem(ItemPool.STENCH_WAD, (int) castsRemaining);
      InventoryManager.retrieveItem(ItemPool.TWINKLY_WAD, (int) castsRemaining);
    }

    long mpPerCast = SkillDatabase.getMPConsumptionById(this.skillId);

    // If the skill doesn't use MP then MP restoring and checking can be skipped
    if (mpPerCast == 0) {
      int soulsauceCost = SkillDatabase.getSoulsauceCost(this.skillId);
      if (soulsauceCost > 0 && KoLCharacter.getSoulsauce() < soulsauceCost) {
        UseSkillRequest.lastUpdate =
            "Your available soulsauce is too low to cast " + this.skillName + ".";
        KoLmafia.updateDisplay(UseSkillRequest.lastUpdate);
        return;
      }

      int thunderCost = SkillDatabase.getThunderCost(this.skillId);
      if (thunderCost > 0 && KoLCharacter.getThunder() < thunderCost) {
        UseSkillRequest.lastUpdate =
            "You don't have enough thunder to cast " + this.skillName + ".";
        KoLmafia.updateDisplay(UseSkillRequest.lastUpdate);
        return;
      }

      int rainCost = SkillDatabase.getRainCost(this.skillId);
      if (rainCost > 0 && KoLCharacter.getRain() < rainCost) {
        UseSkillRequest.lastUpdate =
            "You have insufficient rain drops to cast " + this.skillName + ".";
        KoLmafia.updateDisplay(UseSkillRequest.lastUpdate);
        return;
      }

      int lightningCost = SkillDatabase.getLightningCost(this.skillId);
      if (lightningCost > 0 && KoLCharacter.getLightning() < lightningCost) {
        UseSkillRequest.lastUpdate =
            "You have too few lightning bolts to cast " + this.skillName + ".";
        KoLmafia.updateDisplay(UseSkillRequest.lastUpdate);
        return;
      }

      int hpCost = SkillDatabase.getHPCost(this.skillId);
      if (hpCost > 0 && KoLCharacter.getCurrentHP() < hpCost) {
        UseSkillRequest.lastUpdate = "You have too little HP to cast " + this.skillName + ".";
        KoLmafia.updateDisplay(UseSkillRequest.lastUpdate);
        return;
      }

      AdventureResult mana = SkillDatabase.getManaItemCost(this.skillId);
      if (mana != null) {
        int manaPerCast = mana.getCount();
        long manaNeeded = manaPerCast * castsRemaining;

        // getMaximumCast accounted for the "accessible
        // count" of the appropriate mana before we got
        // here. This should not fail.

        InventoryManager.retrieveItem(mana.getInstance((int) manaNeeded));
      }

      if (this.singleCastSkill()) {
        this.addFormField(this.countFieldId, "1");
      } else {
        this.addFormField(this.countFieldId, String.valueOf(castsRemaining));
        castsRemaining = 1;
      }

      // Run it via GET
      String URLString = this.getFullURLString();

      this.constructURLString(URLString, false);

      while (castsRemaining-- > 0 && !KoLmafia.refusesContinue()) {
        super.run();
      }

      // But keep fields as per POST for easy modification
      this.constructURLString(URLString, true);

      return;
    }

    // Before executing the skill, recover all necessary mana

    long maximumMP = KoLCharacter.getMaximumMP();
    long maximumCast = maximumMP / mpPerCast;

    // Save name so we can guarantee correct target later
    // *** Why, exactly, is this necessary?
    String originalTarget = this.target;

    // libram skills have variable (increasing) mana cost
    boolean hasVariableMpCost = SkillDatabase.hasVariableMpCost(this.skillId);

    // Some skills have to be cast 1 at a time. This might be a KoL bug
    int castsPerIteration = this.singleCastSkill() ? 1 : Integer.MAX_VALUE;

    while (castsRemaining > 0 && !KoLmafia.refusesContinue()) {
      if (hasVariableMpCost) {
        mpPerCast = SkillDatabase.getMPConsumptionById(this.skillId);
      }

      if (maximumMP < mpPerCast) {
        UseSkillRequest.lastUpdate = "Your maximum mana is too low to cast " + this.skillName + ".";
        KoLmafia.updateDisplay(UseSkillRequest.lastUpdate);
        return;
      }

      // Find out how many times we can cast with current MP

      long currentCast =
          Math.min(this.availableCasts(castsRemaining, mpPerCast), castsPerIteration);

      // If none, attempt to recover MP in order to cast;
      // take auto-recovery into account.
      // Also recover MP if an opera mask is worn, to maximize its benefit.
      // (That applies only to AT buffs, but it's unlikely that an opera mask
      // will be worn at any other time than casting one.)
      boolean needExtra =
          currentCast < maximumCast
              && currentCast < castsRemaining
              && EquipmentManager.getEquipment(Slot.HAT).getItemId() == ItemPool.OPERA_MASK;

      if (currentCast == 0 || needExtra) {
        currentCast = Math.min(Math.min(castsRemaining, maximumCast), castsPerIteration);

        long currentMP = KoLCharacter.getCurrentMP();
        long recoverMP = mpPerCast * currentCast;

        if (MoodManager.isExecuting()) {
          recoverMP = Math.min(Math.max(recoverMP, MoodManager.getMaintenanceCost()), maximumMP);
        }

        RecoveryManager.checkpointedRecoverMP(recoverMP);

        // If no change occurred, that means the person
        // was unable to recover MP; abort the process.

        if (currentMP == KoLCharacter.getCurrentMP()) {
          UseSkillRequest.lastUpdate =
              "Could not restore enough mana to cast " + this.skillName + ".";
          KoLmafia.updateDisplay(UseSkillRequest.lastUpdate);
          return;
        }

        currentCast = Math.min(this.availableCasts(castsRemaining, mpPerCast), castsPerIteration);
      }

      if (KoLmafia.refusesContinue()) {
        UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
        return;
      }

      // If this happens to be a health-restorative skill,
      // then there is an effective cap based on how much
      // the skill is able to restore.

      switch (this.skillId) {
        case SkillPool.WALRUS_TONGUE,
            SkillPool.DISCO_NAP,
            SkillPool.BANDAGES,
            SkillPool.COCOON,
            SkillPool.SHAKE_IT_OFF,
            SkillPool.GELATINOUS_RECONSTRUCTION -> {
          int healthRestored = HPRestoreItemList.getHealthRestored(this.skillName);
          long maxPossible =
              Math.max(
                  1, (KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP()) / healthRestored);
          castsRemaining = Math.min(castsRemaining, maxPossible);
          currentCast = Math.min(currentCast, castsRemaining);
        }
      }

      currentCast = Math.min(Math.min(currentCast, maximumCast), castsPerIteration);

      if (currentCast > 0) {
        // Attempt to cast the buff.

        if (this.isBuff) {
          this.setTarget(originalTarget);
        }

        if (this.countFieldId != null) {
          this.addFormField(this.countFieldId, String.valueOf(currentCast), false);
        }

        if (this.target == null || this.target.trim().isEmpty()) {
          KoLmafia.updateDisplay("Casting " + this.skillName + " " + currentCast + " times...");
        } else {
          KoLmafia.updateDisplay(
              "Casting " + this.skillName + " on " + this.target + " " + currentCast + " times...");
        }

        // Run it via GET
        String URLString = this.getFullURLString();

        this.constructURLString(URLString, false);
        super.run();

        if (this.redirectLocation != null) {
          // This skill cast redirected - probably to a choice or a fight.
          // Since we do not handle redirects here, GenericRequest automated.
          // Therefore, our processResults method was not called.
          //
          // If it was a choice, process the result from the choice
          if (this.redirectLocation.startsWith("choice.php")) {
            this.responseText = ChoiceManager.lastResponseText;
            this.processResults();
          }
        }

        // But keep fields as per POST for easy modification
        this.constructURLString(URLString, true);

        // Otherwise, you have completed the correct
        // number of casts.  Deduct it from the number
        // of casts remaining and continue.

        castsRemaining -= currentCast;
      }
    }

    if (KoLmafia.refusesContinue()) {
      UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
    }
  }

  public final boolean singleCastSkill() {
    // Some skills do not work with a "count" field.
    // This might be a KoL bug.
    return switch (this.skillId) {
      case SkillPool.DARK_RITUAL,
          SkillPool.ANCESTRAL_RECALL,
          SkillPool.GIANT_GROWTH,
          SkillPool.LIGHTNING_BOLT_CARD,
          SkillPool.HEALING_SALVE,
          SkillPool.INVISIBLE_AVATAR,
          SkillPool.TRIPLE_SIZE,
          SkillPool.SEEK_OUT_A_BIRD,
          SkillPool.SUMMON_KOKOMO_RESORT_PASS,
          SkillPool.SWEAT_OUT_BOOZE,
          SkillPool.CINCHO_DISPENSE_SALT_AND_LIME,
          SkillPool.CINCHO_PARTY_SOUNDTRACK,
          SkillPool.CINCHO_FIESTA_EXIT -> true;
      default -> false;
    };
  }

  public final long availableCasts(long maxCasts, long mpPerCast) {
    long availableMP = KoLCharacter.getCurrentMP();
    long currentCast = 0;

    if (SkillDatabase.isLibramSkill(this.skillId)) {
      currentCast = SkillDatabase.libramSkillCasts(availableMP);
    } else if (this.skillId == SkillPool.SEEK_OUT_A_BIRD) {
      currentCast = SkillDatabase.birdSkillCasts(availableMP);
    } else if (SkillDatabase.isSoulsauceSkill(this.skillId)) {
      currentCast = KoLCharacter.getSoulsauce() / SkillDatabase.getSoulsauceCost(this.skillId);
    } else if (SkillDatabase.isThunderSkill(this.skillId)) {
      currentCast = KoLCharacter.getThunder() / SkillDatabase.getThunderCost(this.skillId);
    } else if (SkillDatabase.isRainSkill(this.skillId)) {
      currentCast = KoLCharacter.getRain() / SkillDatabase.getRainCost(this.skillId);
    } else if (SkillDatabase.isLightningSkill(this.skillId)) {
      currentCast = KoLCharacter.getLightning() / SkillDatabase.getLightningCost(this.skillId);
    } else if (SkillDatabase.isVampyreSkill(this.skillId)) {
      currentCast = KoLCharacter.getCurrentHP() / SkillDatabase.getHPCost(this.skillId);
    } else {
      currentCast = availableMP / mpPerCast;
      currentCast = Math.min(this.getMaximumCast(), currentCast);
    }

    currentCast = Math.min(maxCasts, currentCast);

    return currentCast;
  }

  private static BuffTool findTool(BuffTool[] tools) {
    for (BuffTool tool : tools) {
      if (tool.hasItem(true)) {
        return tool;
      }
    }
    return null;
  }

  public static final boolean hasAccordion() {
    return KoLCharacter.canInteract()
        || UseSkillRequest.findTool(UseSkillRequest.THIEF_TOOLS) != null;
  }

  public static final boolean hasTotem() {
    return KoLCharacter.canInteract()
        || UseSkillRequest.findTool(UseSkillRequest.TAMER_TOOLS) != null;
  }

  public static final boolean hasSaucepan() {
    return KoLCharacter.canInteract()
        || UseSkillRequest.findTool(UseSkillRequest.SAUCE_TOOLS) != null;
  }

  public static final void prepareTool(final BuffTool[] options, int skillId) {
    if (InventoryManager.canUseMall() || InventoryManager.canUseStorage()) {
      // If we are here, you are out of Hardcore/Ronin and
      // have access to storage and the mall.

      // Iterate over tools. Retrieve the best one you have
      // available. If you have none available that are better
      // than the default tool, retrieve the default, which
      // is determined using these rules:
      //
      // 1) It is not a quest item and is therefore available
      //    to any class.
      // 2) It is not expensive to buy
      // 3) It provides the most bonus turns of any tool that
      //    satisfies the first two conditions

      for (BuffTool tool : options) {
        // If we have the tool, we are good to go
        if (tool.hasItem(false)
            && (!tool.isClassLimited()
                || KoLCharacter.getAscensionClass() == tool.getAscensionClass())) {
          // If it is not equipped, get it into inventory
          if (!tool.hasEquipped()) {
            tool.retrieveItem();
          }
          return;
        }

        // If we don't have it and this is the default
        // tool on this list, acquire it.
        if (tool.isDefault()) {
          if (!tool.retrieveItem()) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR,
                "You are out of Ronin and need a "
                    + tool.getItem()
                    + " to cast that. Check item retrieval settings.");
          }
          return;
        }
      }
    }

    // If we are here, you are in Hardcore/Ronin and have access
    // only to what is in inventory (or closet, if your retrieval
    // settings allow you to use it).

    // Iterate over items and remember the best one you have available.

    BuffTool bestTool = null;

    for (BuffTool tool : options) {
      if (tool.hasItem(false)
          && (!tool.isClassLimited()
              || (KoLCharacter.getAscensionClass() == tool.getAscensionClass()))) {
        bestTool = tool;
        break;
      }
    }

    // If we don't have any of the tools, try to retrieve the
    // weakest one via purchase/sewer fishing.
    if (bestTool == null) {
      BuffTool weakestTool = options[options.length - 1];
      weakestTool.retrieveItem();
      return;
    }

    // if best tool is equipped, cool.
    if (bestTool.hasEquipped()) {
      return;
    }

    // Get best tool into inventory
    bestTool.retrieveItem();
  }

  @Override
  protected boolean retryOnTimeout() {
    return false;
  }

  @Override
  protected boolean processOnFailure() {
    return true;
  }

  @Override
  public void processResults() {
    UseSkillRequest.lastUpdate = "";

    boolean shouldStop = UseSkillRequest.parseResponse(this.getURLString(), this.responseText);

    if (!UseSkillRequest.lastUpdate.isEmpty()) {
      MafiaState state = shouldStop ? MafiaState.ERROR : MafiaState.CONTINUE;
      KoLmafia.updateDisplay(state, UseSkillRequest.lastUpdate);

      if (BuffBotHome.isBuffBotActive()) {
        BuffBotHome.timeStampedLogEntry(BuffBotHome.ERRORCOLOR, UseSkillRequest.lastUpdate);
      }

      return;
    }

    if (this.target == null) {
      KoLmafia.updateDisplay(this.skillName + " was successfully cast.");
    } else {
      KoLmafia.updateDisplay(this.skillName + " was successfully cast on " + this.target + ".");
    }
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof UseSkillRequest
        && this.getSkillName().equals(((UseSkillRequest) o).getSkillName());
  }

  @Override
  public int hashCode() {
    return this.skillId;
  }

  public static final UseSkillRequest getUnmodifiedInstance(final int skillId) {
    if (skillId == -1) {
      return null;
    }

    String skillName = SkillDatabase.getSkillName(skillId);
    if (skillName == null) {
      return null;
    }

    UseSkillRequest request = UseSkillRequest.ALL_SKILLS.get(skillId);
    if (request == null) {
      request = new UseSkillRequest(skillId);
      UseSkillRequest.ALL_SKILLS.put(skillId, request);
    }

    return request;
  }

  public static final UseSkillRequest getUnmodifiedInstance(String skillName) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    return UseSkillRequest.getUnmodifiedInstance(SkillDatabase.getSkillId(skillName));
  }

  public static final UseSkillRequest getInstance(
      final int skillId, final String target, final int buffCount, final int desiredEffect) {
    UseSkillRequest request = new UseSkillRequest(skillId);
    request.setTarget(target == null || target.isEmpty() ? KoLCharacter.getUserName() : target);
    request.setBuffCount(buffCount);
    request.setDesiredEffect(desiredEffect);
    return request;
  }

  public static final UseSkillRequest getInstance(
      final int skillId, final String target, final int buffCount) {
    return UseSkillRequest.getInstance(skillId, target, buffCount, -1);
  }

  public static final UseSkillRequest getInstance(final int skillId) {
    return UseSkillRequest.getInstance(skillId, null, 0);
  }

  public static final UseSkillRequest getInstance(final int skillId, final int buffCount) {
    return UseSkillRequest.getInstance(skillId, null, buffCount);
  }

  public static final UseSkillRequest getInstance(
      final String skillName, final String target, final int buffCount) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    return UseSkillRequest.getInstance(SkillDatabase.getSkillId(skillName), target, buffCount);
  }

  public static final UseSkillRequest getInstance(String skillName) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    return UseSkillRequest.getInstance(skillName, null, 0);
  }

  public static final UseSkillRequest getInstance(final String skillName, final int buffCount) {
    // *** Skills can have ambiguous names. Best to use the methods that deal with skill id
    return UseSkillRequest.getInstance(skillName, null, buffCount);
  }

  public static final boolean parseResponse(final String urlString, final String responseText) {
    int skillId = UseSkillRequest.lastSkillUsed;
    int count = UseSkillRequest.lastSkillCount;

    if (urlString.contains("skillz.php") && !urlString.contains("whichskill")) {
      // This is a skill list, parse skills for consequences.
      Matcher matcher = UseSkillRequest.SKILLZ_PATTERN.matcher(responseText);
      while (matcher.find()) {
        ConsequenceManager.parseSkillDesc(
            StringUtilities.parseInt(matcher.group(1)), matcher.group(2));
      }
    }

    if (skillId == -1) {
      UseSkillRequest.lastUpdate = "Skill ID not saved.";
      return false;
    }

    UseSkillRequest.lastSkillUsed = -1;
    UseSkillRequest.lastSkillCount = 0;

    SkillStatus status = responseStatus(responseText, skillId, count);
    if (status == SkillStatus.SUCCESS) {
      SkillDatabase.registerCasts(skillId, count);
    }
    return status == SkillStatus.ERROR;
  }

  private static SkillStatus responseStatus(final String responseText, int skillId, int count) {
    if (responseText == null || responseText.trim().isEmpty()) {
      long initialMP = KoLCharacter.getCurrentMP();
      ApiRequest.updateStatus();

      if (initialMP == KoLCharacter.getCurrentMP()) {
        UseSkillRequest.lastUpdate = "Encountered lag problems.";
        return SkillStatus.WARNING;
      }

      UseSkillRequest.lastUpdate = "KoL sent back a blank response, but consumed MP.";
      return SkillStatus.ERROR;
    }

    if (responseText.contains("You don't have that skill")
        || responseText.contains("you don't seem to have that skill")) {
      UseSkillRequest.lastUpdate = "That skill is unavailable.";
      return SkillStatus.ERROR;
    }

    // Summon Clip Art cast through the browser has two phases:
    //
    //   campground.php?preaction=summoncliparts
    //   campground.php?preaction=combinecliparts
    //
    // Only the second once consumes MP and only if it is successful.
    // Internally, we use only the second URL.
    //
    // For now, simply ignore any call on either URL that doesn't
    // result in an item, since failures just redisplay the bookshelf

    if (skillId == SkillPool.CLIP_ART && !responseText.contains("You acquire")) {
      return SkillStatus.WARNING;
    }

    // You can't fit anymore songs in your head right now
    // XXX can't fit anymore songs in their head right now.
    if (responseText.contains("can't fit anymore songs")
        || responseText.contains("can't fit any more songs")) {
      UseSkillRequest.lastUpdate = "Selected target has the maximum number of AT buffs already.";
      return SkillStatus.WARNING;
    }

    if (responseText.contains("casts left of the Smile of Mr. A")) {
      UseSkillRequest.lastUpdate = "You cannot cast that many smiles.";
      return SkillStatus.WARNING;
    }

    if (responseText.contains("Invalid target player")) {
      UseSkillRequest.lastUpdate = "Selected target is not a valid target.";
      return SkillStatus.ERROR;
    }

    // You can't cast that spell on persons who are lower than
    // level 15, like <name>, who is level 13.
    if (responseText.contains("lower than level")) {
      UseSkillRequest.lastUpdate = "Selected target is too low level.";
      return SkillStatus.WARNING;
    }

    if (responseText.contains("busy fighting")) {
      UseSkillRequest.lastUpdate = "Selected target is busy fighting.";
      return SkillStatus.WARNING;
    }

    if (responseText.contains("receive buffs")) {
      UseSkillRequest.lastUpdate = "Selected target cannot receive buffs.";
      return SkillStatus.WARNING;
    }

    if (responseText.contains("You need")) {
      UseSkillRequest.lastUpdate = "You need special equipment to cast that buff.";
      return SkillStatus.ERROR;
    }

    if (responseText.contains("You can't remember how to use that skill")) {
      UseSkillRequest.lastUpdate = "That skill is currently unavailable.";
      return SkillStatus.ERROR;
    }

    if (responseText.contains("You can't cast this spell because you are not an Accordion Thief")) {
      UseSkillRequest.lastUpdate = "Only Accordion Thieves can use that skill.";
      return SkillStatus.ERROR;
    }

    if (responseText.contains("You're already blessed")) {
      UseSkillRequest.lastUpdate = "You already have that blessing.";
      return SkillStatus.ERROR;
    }

    if (responseText.contains("not attuned to any particular Turtle Spirit")) {
      UseSkillRequest.lastUpdate = "You haven't got a Blessing, so can't get a Boon.";
      return SkillStatus.ERROR;
    }

    if (responseText.contains("You decide not to commit")) {
      UseSkillRequest.lastUpdate = "You decide to not change your favorite bird.";
      return SkillStatus.ERROR;
    }

    var limit = DailyLimitType.CAST.getDailyLimit(skillId);

    // Determine whether maximum casts reached
    if (limit != null) {
      boolean maxedOut;

      if (responseText.contains("You may only use three Tome summonings each day")) {
        Preferences.setInteger("tomeSummons", 3);
        ConcoctionDatabase.setRefreshNeeded(true);

        // If you are in run, this message doesn't mean that this specific Tome has maxed out,
        // rather that overall maximum Tome usage has been reached
        if (!KoLCharacter.canInteract()) {
          UseSkillRequest.lastUpdate = "You may only use three Tome summonings each day";
          return SkillStatus.ERROR;
        }

        maxedOut = true;
      } else {
        maxedOut =
            // You've already recalled a lot of ancestral memories lately. You should
            // probably give your ancestors the rest of the day off.
            responseText.contains("You've already recalled a lot of ancestral memories lately")
                ||
                // You think your stomach has had enough for one day.
                responseText.contains("enough for one day")
                ||
                // You can't cast that many turns of that skill today. (You've used 5 casts today,
                // and the limit of casts per day you have is 5.)
                responseText.contains("You can't cast that many turns of that skill today")
                || responseText.contains("You can only declare one Employee of the Month per day");
      }

      if (maxedOut) {
        UseSkillRequest.lastUpdate = limit.getMaxMessage();
        limit.setToMax();
        return SkillStatus.ERROR;
      }

      // Handle skills that indicate their current and max casts
      Matcher limitedMatcher = UseSkillRequest.LIMITED_PATTERN.matcher(responseText);
      // limited-use skills
      // "Y / maxCasts casts used today."
      if (limitedMatcher.find()) {
        // parse the number of casts remaining and set the appropriate preference.
        int casts = Integer.parseInt(limitedMatcher.group(1));
        // We will increment this later. For now, just synch with KoL.
        limit.set(casts - count);
      }
    }

    // Request Sandwich has a 50% chance of working until it is succesfully cast for the day.
    // If this fails and we haven't already cast it successfully today, send a warning.
    if (skillId == SkillPool.REQUEST_SANDWICH
        && !responseText.contains("well, since you asked nicely")
        && !Preferences.getBoolean("_requestSandwichSucceeded")) {
      UseSkillRequest.lastUpdate = "You forgot the magic word!";
      return SkillStatus.WARNING;
    }

    if (responseText.contains("You don't have enough")) {
      String skillName = SkillDatabase.getSkillName(skillId);

      UseSkillRequest.lastUpdate = "Not enough mana to cast " + skillName + ".";
      ApiRequest.updateStatus();
      return SkillStatus.ERROR;
    }

    // The skill was successfully cast. Deal with its effects.
    if (responseText.contains("tear the opera mask")) {
      EquipmentManager.breakEquipment(ItemPool.OPERA_MASK, "Your opera mask shattered.");
    }

    long mpCost = SkillDatabase.getMPConsumptionById(skillId) * count;

    if (responseText.contains("You can only conjure")
        || responseText.contains("You can only scrounge up")
        || responseText.contains("You can't use that skill")
        || responseText.contains("You can only summon")) {
      if (skillId == SkillPool.COCOON) {
        // Cannelloni Cocoon says "You can't use that
        // skill" when you are already at full HP.
        UseSkillRequest.lastUpdate = "You are already at full HP.";
      } else if (skillId == SkillPool.ANCESTRAL_RECALL) {
        // Ancestral Recall says "You can't use that
        // skill" if you don't have any blue mana.
        UseSkillRequest.lastUpdate = "You don't have any blue mana.";
        return SkillStatus.ERROR;
      } else {
        UseSkillRequest.lastUpdate = "Summon limit exceeded.";

        // We're out of sync with the actual number of times
        // this skill has been cast.  Adjust the counter by 1
        // at a time.
        count = 1;
      }
      mpCost = 0L;
    }

    // Deal with secondary effects from skill usage (not daily limitations)
    switch (skillId) {
      case SkillPool.ODE_TO_BOOZE -> ConcoctionDatabase.getUsables().sort();
      case SkillPool.WALRUS_TONGUE, SkillPool.DISCO_NAP -> UneffectRequest.removeEffectsWithSkill(
          skillId);
      case SkillPool.RAINBOW_GRAVITATION -> {

        // Each cast of Rainbow Gravitation consumes five
        // elemental wads and a twinkly wad

        ResultProcessor.processResult(ItemPool.get(ItemPool.COLD_WAD, -count));
        ResultProcessor.processResult(ItemPool.get(ItemPool.HOT_WAD, -count));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SLEAZE_WAD, -count));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SPOOKY_WAD, -count));
        ResultProcessor.processResult(ItemPool.get(ItemPool.STENCH_WAD, -count));
        ResultProcessor.processResult(ItemPool.get(ItemPool.TWINKLY_WAD, -count));
      }
      case SkillPool.TURTLE_POWER,
          SkillPool.WAR_BLESSING,
          SkillPool.SHE_WHO_WAS_BLESSING,
          SkillPool.STORM_BLESSING -> Preferences.setInteger("turtleBlessingTurns", 0);
      case SkillPool.CARBOLOADING -> Preferences.increment("carboLoading", 1);
      case SkillPool.SNOWCONE,
          SkillPool.STICKER,
          SkillPool.SUGAR,
          SkillPool.CLIP_ART,
          SkillPool.RAD_LIB,
          SkillPool.SMITHSNESS -> ConcoctionDatabase.setRefreshNeeded(false);
      case SkillPool.EGGMAN -> ResultProcessor.removeItem(ItemPool.COSMIC_EGG);
      case SkillPool.RADISH_HORSE -> ResultProcessor.removeItem(ItemPool.COSMIC_VEGETABLE);
      case SkillPool.HIPPOTATO -> ResultProcessor.removeItem(ItemPool.COSMIC_POTATO);
      case SkillPool.CREAMPUFF -> ResultProcessor.removeItem(ItemPool.COSMIC_CREAM);
      case SkillPool.DEEP_VISIONS -> DreadScrollManager.handleDeepDarkVisions(responseText);
      case SkillPool.BIND_VAMPIEROGHI,
          SkillPool.BIND_VERMINCELLI,
          SkillPool.BIND_ANGEL_HAIR_WISP,
          SkillPool.BIND_UNDEAD_ELBOW_MACARONI,
          SkillPool.BIND_PENNE_DREADFUL,
          SkillPool.BIND_LASAGMBIE,
          SkillPool.BIND_SPICE_GHOST,
          SkillPool.BIND_SPAGHETTI_ELEMENTAL -> PastaThrallData.handleBinding(
          skillId, responseText);
      case SkillPool.DISMISS_PASTA_THRALL -> PastaThrallData.handleDismissal(responseText);
      case SkillPool.SUMMON_ANNOYANCE -> Preferences.decrement(
          "availableSwagger", Preferences.getInteger("summonAnnoyanceCost"));
      case SkillPool.ANCESTRAL_RECALL -> ResultProcessor.processResult(
          ItemPool.get(ItemPool.BLUE_MANA, -count));
      case SkillPool.DARK_RITUAL -> ResultProcessor.processResult(
          ItemPool.get(ItemPool.BLACK_MANA, -count));
      case SkillPool.STACK_LUMPS -> {
        Preferences.increment("_stackLumpsUses");
        ResultProcessor.processResult(ItemPool.get(ItemPool.NEGATIVE_LUMP, -100));
      }
      case SkillPool.LOVE_MIXOLOGY -> {
        Matcher matcher = UseSkillRequest.LOVE_POTION_PATTERN.matcher(responseText);
        if (matcher.find()) {
          Preferences.setString("lovePotion", matcher.group(1).replaceAll(",", ""));
        }
      }
      case SkillPool.CALCULATE_THE_UNIVERSE -> {
        if (Preferences.getInteger("skillLevel144") == 0) {
          // If the skill is being cast, then the limit must be at least 1
          Preferences.setInteger("skillLevel144", 1);
        }
      }
      case SkillPool.SEEK_OUT_A_BIRD -> Preferences.increment("_birdsSoughtToday");
      case SkillPool.MAP_THE_MONSTERS -> Preferences.setBoolean("mappingMonsters", true);
      case SkillPool.MAKE_SWEATADE,
          SkillPool.DRENCH_YOURSELF_IN_SWEAT,
          SkillPool.SIP_SOME_SWEAT -> {
        Matcher sweatCheck = SWEAT_PATTERN.matcher(responseText);
        if (sweatCheck.find()) {
          int sweatCost = Integer.parseInt(sweatCheck.group(1));
          Preferences.decrement("sweat", sweatCost);
        }
      }
      case SkillPool.SWEAT_OUT_BOOZE -> Preferences.decrement("sweat", count * 25);
      case SkillPool.CINCHO_DISPENSE_SALT_AND_LIME -> Preferences.increment("cinchoSaltAndLime");
      case SkillPool.CINCHO_FIESTA_EXIT -> Preferences.setBoolean("noncombatForcerActive", true);
      case SkillPool.AUG_1ST_MOUNTAIN_CLIMBING_DAY,
          SkillPool.AUG_2ND_FIND_AN_ELEVENLEAF_CLOVER_DAY,
          SkillPool.AUG_3RD_WATERMELON_DAY,
          SkillPool.AUG_4TH_WATER_BALLOON_DAY,
          SkillPool.AUG_5TH_OYSTER_DAY,
          SkillPool.AUG_6TH_FRESH_BREATH_DAY,
          SkillPool.AUG_7TH_LIGHTHOUSE_DAY,
          SkillPool.AUG_8TH_CAT_DAY,
          SkillPool.AUG_9TH_HAND_HOLDING_DAY,
          SkillPool.AUG_10TH_WORLD_LION_DAY,
          SkillPool.AUG_11TH_PRESIDENTIAL_JOKE_DAY,
          SkillPool.AUG_12TH_ELEPHANT_DAY,
          SkillPool.AUG_13TH_LEFTOFF_HANDERS_DAY,
          SkillPool.AUG_14TH_FINANCIAL_AWARENESS_DAY,
          SkillPool.AUG_15TH_RELAXATION_DAY,
          SkillPool.AUG_16TH_ROLLER_COASTER_DAY,
          SkillPool.AUG_17TH_THRIFTSHOP_DAY,
          SkillPool.AUG_18TH_SERENDIPITY_DAY,
          SkillPool.AUG_19TH_HONEY_BEE_AWARENESS_DAY,
          SkillPool.AUG_20TH_MOSQUITO_DAY,
          SkillPool.AUG_21ST_SPUMONI_DAY,
          SkillPool.AUG_22ND_TOOTH_FAIRY_DAY,
          SkillPool.AUG_23RD_RIDE_THE_WIND_DAY,
          SkillPool.AUG_24TH_WAFFLE_DAY,
          SkillPool.AUG_25TH_BANANA_SPLIT_DAY,
          SkillPool.AUG_26TH_TOILET_PAPER_DAY,
          SkillPool.AUG_27TH_JUST_BECAUSE_DAY,
          SkillPool.AUG_28TH_RACE_YOUR_MOUSE_DAY,
          SkillPool.AUG_29TH_MORE_HERBS_LESS_SALT_DAY,
          SkillPool.AUG_30TH_BEACH_DAY,
          SkillPool.AUG_31ST_CABERNET_SAUVIGNON_DAY -> {
        // is this our "today" skill?
        var date = DateTimeManager.getRolloverDateTime().getDayOfMonth();
        var isTodaySkill = skillId == date - 1 + SkillPool.AUG_1ST_MOUNTAIN_CLIMBING_DAY;
        // "today" skill does not apply in-run
        if (isTodaySkill && KoLCharacter.canInteract()) {
          Preferences.setBoolean("_augTodayCast", true);
        } else {
          Preferences.increment("_augSkillsCast", 1, 5, false);
        }
      }
    }

    if (KoLCharacter.hasEquipped(ItemPool.APRIL_SHOWER_THOUGHTS_SHIELD)) {
      switch (skillId) {
        case SkillPool.SIMMER -> Preferences.setBoolean("_aprilShowerSimmer", true);
        case SkillPool.DISCO_NAP -> Preferences.increment("_aprilShowerDiscoNap", 1, 5, false);
      }
    }

    // Now apply daily limits
    if (limit != null) {
      limit.incrementByCost(count);
    }

    if (SkillDatabase.isLibramSkill(skillId)) {
      int cast = Preferences.getInteger("libramSummons");
      mpCost = SkillDatabase.libramSkillMPConsumption(cast + 1, count);
      Preferences.increment("libramSummons", count);
      LockableListFactory.sort(KoLConstants.summoningSkills);
      LockableListFactory.sort(KoLConstants.usableSkills);
    } else if (SkillDatabase.isSoulsauceSkill(skillId)) {
      KoLCharacter.decrementSoulsauce(SkillDatabase.getSoulsauceCost(skillId) * count);
    } else if (SkillDatabase.isThunderSkill(skillId)) {
      KoLCharacter.decrementThunder(SkillDatabase.getThunderCost(skillId) * count);
    } else if (SkillDatabase.isRainSkill(skillId)) {
      KoLCharacter.decrementRain(SkillDatabase.getRainCost(skillId) * count);
    } else if (SkillDatabase.isLightningSkill(skillId)) {
      KoLCharacter.decrementLightning(SkillDatabase.getLightningCost(skillId) * count);
    } else if (SkillDatabase.isVampyreSkill(skillId)) {
      ResultProcessor.processResult(
          new AdventureLongCountResult(
              AdventureResult.HP, (long) -SkillDatabase.getHPCost(skillId) * count));
    }

    if (mpCost > 0) {
      ResultProcessor.processResult(new AdventureLongCountResult(AdventureResult.MP, -mpCost));
    }

    return SkillStatus.SUCCESS;
  }

  public static int getSkillId(final String urlString) {
    Matcher skillMatcher = UseSkillRequest.SKILLID_PATTERN.matcher(urlString);
    if (skillMatcher.find()) {
      return StringUtilities.parseInt(skillMatcher.group(1));
    }

    skillMatcher = UseSkillRequest.BOOKID_PATTERN.matcher(urlString);
    if (!skillMatcher.find()) {
      return -1;
    }

    String action = skillMatcher.group(1);

    if (action.equals("snowcone")) {
      return SkillPool.SNOWCONE;
    }

    if (action.equals("stickers")) {
      return SkillPool.STICKER;
    }

    if (action.equals("sugarsheets")) {
      return SkillPool.SUGAR;
    }

    if (action.equals("cliparts")) {
      if (!urlString.contains("clip3=")) {
        return -1;
      }

      return SkillPool.CLIP_ART;
    }

    if (action.equals("radlibs")) {
      return SkillPool.RAD_LIB;
    }

    if (action.equals("smithsness")) {
      return SkillPool.SMITHSNESS;
    }

    if (action.equals("hilariousitems")) {
      return SkillPool.HILARIOUS;
    }

    if (action.equals("spencersitems")) {
      return SkillPool.TASTEFUL;
    }

    if (action.equals("aa")) {
      return SkillPool.CARDS;
    }

    if (action.equals("thinknerd")) {
      return SkillPool.GEEKY;
    }

    if (action.equals("candyheart")) {
      return SkillPool.CANDY_HEART;
    }

    if (action.equals("partyfavor")) {
      return SkillPool.PARTY_FAVOR;
    }

    if (action.equals("lovesongs")) {
      return SkillPool.LOVE_SONG;
    }

    if (action.equals("brickos")) {
      return SkillPool.BRICKOS;
    }

    if (action.equals("gygax")) {
      return SkillPool.DICE;
    }

    if (action.equals("resolutions")) {
      return SkillPool.RESOLUTIONS;
    }

    if (action.equals("taffy")) {
      return SkillPool.TAFFY;
    }

    if (action.equals("confiscators")) {
      return SkillPool.CONFISCATOR;
    }

    return -1;
  }

  private static long getCount(final String urlString, int skillId) {
    Matcher countMatcher = UseSkillRequest.COUNT_PATTERN.matcher(urlString);

    if (!countMatcher.find()) {
      return 1;
    }

    long availableMP = KoLCharacter.getCurrentMP();
    long maxcasts;
    if (SkillDatabase.isLibramSkill(skillId)) {
      maxcasts = SkillDatabase.libramSkillCasts(availableMP);
    } else if (skillId == SkillPool.SEEK_OUT_A_BIRD) {
      maxcasts = SkillDatabase.birdSkillCasts(availableMP);
    } else if (SkillDatabase.isSoulsauceSkill(skillId)) {
      maxcasts = KoLCharacter.getSoulsauce() / SkillDatabase.getSoulsauceCost(skillId);
    } else if (SkillDatabase.isThunderSkill(skillId)) {
      maxcasts = KoLCharacter.getThunder() / SkillDatabase.getThunderCost(skillId);
    } else if (SkillDatabase.isRainSkill(skillId)) {
      maxcasts = KoLCharacter.getRain() / SkillDatabase.getRainCost(skillId);
    } else if (SkillDatabase.isLightningSkill(skillId)) {
      maxcasts = KoLCharacter.getLightning() / SkillDatabase.getLightningCost(skillId);
    } else if (SkillDatabase.isVampyreSkill(skillId)) {
      maxcasts = KoLCharacter.getCurrentHP() / SkillDatabase.getHPCost(skillId);
    } else {
      long MP = SkillDatabase.getMPConsumptionById(skillId);
      maxcasts = SkillDatabase.getMaxCasts(skillId);
      maxcasts = maxcasts == -1 ? Integer.MAX_VALUE : maxcasts;
      if (MP != 0) {
        maxcasts = Math.min(maxcasts, availableMP / MP);
      }
    }

    if (countMatcher.group(1).startsWith("*")) {
      return maxcasts;
    }

    return Math.min(maxcasts, StringUtilities.parseLong(countMatcher.group(1)));
  }

  public static final boolean registerRequest(final String urlString) {
    if (urlString.startsWith("skillz.php")) {
      return true;
    }

    if (!urlString.startsWith("campground.php") && !urlString.startsWith("runskillz.php")) {
      return false;
    }

    int skillId = UseSkillRequest.getSkillId(urlString);
    // Quick skills has (select a skill) with ID = 999
    if (skillId == -1 || skillId == 999) {
      return false;
    }

    // Exceedingly unlikely that count will exceed the capacity of
    // an integer. SkillDatabase can't handle that, at the moment.
    // So, simply cast to an int and possible overflow
    int count = (int) UseSkillRequest.getCount(urlString, skillId);
    String skillName = SkillDatabase.getSkillName(skillId);

    UseSkillRequest.lastSkillUsed = skillId;
    UseSkillRequest.lastSkillCount = count;

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("cast " + count + " " + skillName);

    return true;
  }

  @Override
  public int getAdventuresUsed() {
    return SkillDatabase.getAdventureCost(this.skillId);
  }

  public static int getAdventuresUsed(final String urlString) {
    return SkillDatabase.getAdventureCost(UseSkillRequest.getSkillId(urlString));
  }

  public static class BuffTool {
    final AdventureResult item;
    final int bonusTurns;
    final boolean def;
    final AscensionClass ascensionClass;

    public BuffTool(
        final int itemId,
        final int bonusTurns,
        final boolean def,
        final AscensionClass ascensionClass) {
      this.item = ItemPool.get(itemId, 1);
      this.bonusTurns = bonusTurns;
      this.def = def;
      this.ascensionClass = ascensionClass;
    }

    public final AdventureResult getItem() {
      return this.item;
    }

    public final int getBonusTurns() {
      return this.bonusTurns;
    }

    public final boolean isClassLimited() {
      return this.ascensionClass != null;
    }

    public final AscensionClass getAscensionClass() {
      return this.ascensionClass;
    }

    public final boolean isDefault() {
      return this.def;
    }

    public final boolean hasEquipped() {
      return KoLCharacter.hasEquipped(this.item);
    }

    public final boolean hasItem(final boolean create) {
      return InventoryManager.hasItem(this.item, create);
    }

    public final boolean retrieveItem() {
      return InventoryManager.retrieveItem(this.item);
    }
  }
}
