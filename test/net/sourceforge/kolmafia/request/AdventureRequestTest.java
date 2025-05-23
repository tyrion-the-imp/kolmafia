package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresSpent;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.Set;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.JuneCleaverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class AdventureRequestTest {
  @BeforeEach
  public void init() {
    KoLCharacter.reset("AdventureRequestTest");
    Preferences.reset("AdventureRequestTest");
    AdventureQueueDatabase.allowSerializationWrite = false;
  }

  @AfterAll
  static void restore() {
    AdventureQueueDatabase.allowSerializationWrite = true;
  }

  @Test
  public void aboveWaterZonesCostOneAdventure() {
    AdventureRequest request = new AdventureRequest("Noob Cave", "adventure.php", "240");
    assertEquals(1, request.getAdventuresUsed());
  }

  @Test
  public void underwaterZonesCostTwoAdventures() {
    AdventureRequest request = new AdventureRequest("The Ice Hole", "adventure.php", "457");
    assertEquals(2, request.getAdventuresUsed());
  }

  @Test
  public void underwaterZonesCostOneAdventureWithFishy() {
    var cleanups = withEffect("Fishy");

    try (cleanups) {
      AdventureRequest request = new AdventureRequest("The Ice Hole", "adventure.php", "457");
      KoLCharacter.recalculateAdjustments();
      assertEquals(1, request.getAdventuresUsed());
    }
  }

  @Test
  public void recognizesSpookyWheelbarrow() {
    var cleanups =
        new Cleanups(withProperty("lastEncounter"), withLastLocation("The Spooky Gravy Burrow"));

    try (cleanups) {
      var request =
          new GenericRequest("adventure.php?snarfblat=" + AdventurePool.SPOOKY_GRAVY_BURROW);
      request.responseText = html("request/find_spooky_fairy_gravy.html");
      AdventureRequest.registerEncounter(request);
      assertEquals("Spooky Wheelbarrow", Preferences.getString("lastEncounter"));
    }
  }

  @Test
  public void gregariousMonstersAreEnqueued() {
    var cleanups =
        new Cleanups(
            withFight(0),
            withLastLocation("Barf Mountain"),
            withNextMonster("Knob Goblin Embezzler"));

    try (cleanups) {
      AdventureQueueDatabase.resetQueue();
      var req = new GenericRequest("fight.php");
      req.setHasResult(true);
      req.responseText = html("request/test_fight_gregarious_monster.html");
      req.processResponse();

      assertThat(
          AdventureQueueDatabase.getZoneQueue("Barf Mountain"), contains("Knob Goblin Embezzler"));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void rainManMonstersAreNotEnqueued(final boolean isRainMan) {
    var cleanups = new Cleanups(withLastLocation("Barf Mountain"));

    if (isRainMan) cleanups.add(withPath(AscensionPath.Path.HEAVY_RAINS));

    try (cleanups) {
      AdventureQueueDatabase.resetQueue();
      var req = new GenericRequest("fight.php");
      req.setHasResult(true);
      req.responseText = html("request/test_fight_rainman_monster.html");
      req.processResponse();

      var matcher = contains("baseball bat");
      if (isRainMan) matcher = not(matcher);

      assertThat(AdventureQueueDatabase.getZoneQueue("Barf Mountain"), matcher);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void mimeographMonstersAreNotEnqueued(final boolean isMimeograph) {
    var cleanups = new Cleanups(withLastLocation("Barf Mountain"));

    try (cleanups) {
      FightRequest.preFight(false);
      AdventureQueueDatabase.resetQueue();
      var req = new GenericRequest("fight.php");
      req.setHasResult(true);
      req.responseText =
          html(
              "request/test_fight_"
                  + (isMimeograph ? "mimeograph" : "knob_goblin_assistant_chef")
                  + ".html");
      req.processResponse();

      var matcher = contains("Knob Goblin Assistant Chef");
      if (isMimeograph) matcher = not(matcher);

      assertThat(AdventureQueueDatabase.getZoneQueue("Barf Mountain"), matcher);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void rainManWitchessPiecesArentCountedTowardsTotal(final boolean isRainMan) {
    var cleanups = new Cleanups(withProperty("_witchessFights", 0), withFight(0));

    if (isRainMan) cleanups.add(withPath(AscensionPath.Path.HEAVY_RAINS));

    try (cleanups) {
      var req = new GenericRequest("fight.php");
      req.setHasResult(true);
      req.responseText =
          html("request/test_fight_witchess_pawn" + (isRainMan ? "_rain_man" : "") + ".html");
      req.processResponse();

      assertThat("_witchessFights", isSetTo(isRainMan ? 0 : 1));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void relativityMonsterAreNotEnqueued(final boolean isRelativity) {
    var cleanups =
        new Cleanups(
            withLastLocation("Oil Peak"), withProperty("_relativityMonster", isRelativity));

    try (cleanups) {
      AdventureQueueDatabase.resetQueue();
      var req = new GenericRequest("fight.php");
      req.setHasResult(true);
      req.responseText = html("request/test_fight_oil_slick.html");
      req.processResponse();

      var matcher = contains("oil slick");
      if (isRelativity) matcher = not(matcher);

      assertThat(AdventureQueueDatabase.getZoneQueue("Oil Peak"), matcher);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void relativityWitchessPiecesArentCountedTowardsTotal(final boolean isRelativityFight) {
    var cleanups =
        new Cleanups(
            withProperty("_witchessFights", 0),
            withFight(0),
            withProperty("_relativityMonster", isRelativityFight));

    try (cleanups) {
      var req = new GenericRequest("fight.php");
      req.setHasResult(true);
      req.responseText = html("request/test_fight_witchess_pawn.html");
      req.processResponse();

      assertThat("_witchessFights", isSetTo(isRelativityFight ? 0 : 1));
    }
  }

  @Nested
  public class NoncombatQueue {
    @Test
    void addsNoncombatToQueue() {
      var cleanups = withLastLocation(AdventureDatabase.getAdventure(AdventurePool.BLACK_FOREST));
      try (cleanups) {
        AdventureQueueDatabase.resetQueue();
        var req = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.BLACK_FOREST);
        req.setHasResult(true);
        req.responseText = html("request/test_adventure_black_forest_all_over_the_map.html");
        req.processResponse();

        assertThat(
            AdventureQueueDatabase.getZoneNoncombatQueue(
                AdventureDatabase.getAdventure(AdventurePool.BLACK_FOREST)),
            contains("All Over the Map"));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "test_adventure_hallowiener_skeleton_store.html," + AdventurePool.SKELETON_STORE,
      "test_adventure_bat_wings_beanbat.html," + AdventurePool.BEANBAT,
      "test_adventure_lucky_outskirts_knob.html," + AdventurePool.OUTSKIRTS_OF_THE_KNOB,
      "test_june_cleaver_choice.html," + AdventurePool.HIDDEN_APARTMENT,
    })
    void doesntAddSpecialNoncombatToQueue(String htmlFile, int locationId) {
      var cleanups = withLastLocation(AdventureDatabase.getAdventure(locationId));
      try (cleanups) {
        AdventureQueueDatabase.resetQueue();
        var req = new GenericRequest("adventure.php?snarfblat=" + locationId);
        req.setHasResult(true);
        req.responseText = html("request/" + htmlFile);
        req.processResponse();

        assertThat(
            AdventureQueueDatabase.getZoneNoncombatQueue(
                AdventureDatabase.getAdventure(locationId)),
            empty());
      }
    }

    @ParameterizedTest
    @CsvSource({"test_adventure_poop_deck_its_always_swordfish.html," + AdventurePool.POOP_DECK})
    void addsStopNoncombatToQueue(String htmlFile, int locationId) {
      var cleanups = withLastLocation(AdventureDatabase.getAdventure(locationId));
      try (cleanups) {
        AdventureQueueDatabase.resetQueue();
        var req = new GenericRequest("adventure.php?snarfblat=" + locationId);
        req.setHasResult(true);
        req.responseText = html("request/" + htmlFile);
        req.processResponse();

        assertThat(
            AdventureQueueDatabase.getZoneNoncombatQueue(
                AdventureDatabase.getAdventure(locationId)),
            contains("It's Always Swordfish"));
      }
    }
  }

  @Nested
  public class LastNoncombat {
    @BeforeEach
    void beforeEach() {
      AdventureSpentDatabase.resetTurns(false);
    }

    @Test
    void tracksLastNoncombat() {
      KoLAdventure location = AdventureDatabase.getAdventure(AdventurePool.BLACK_FOREST);
      var cleanups =
          new Cleanups(
              withLastLocation(location),
              withProperty("lastNoncombat" + AdventurePool.BLACK_FOREST, 0),
              withAdventuresSpent(location, 5));
      try (cleanups) {
        var req = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.BLACK_FOREST);
        req.setHasResult(true);
        req.responseText = html("request/test_adventure_black_forest_all_over_the_map.html");
        req.processResponse();

        assertThat("lastNoncombat" + AdventurePool.BLACK_FOREST, isSetTo(5));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "test_adventure_hallowiener_skeleton_store.html," + AdventurePool.SKELETON_STORE,
      "test_adventure_bat_wings_beanbat.html," + AdventurePool.BEANBAT,
      "test_adventure_lucky_billiards_room.html," + AdventurePool.HAUNTED_BILLIARDS_ROOM,
      "test_june_cleaver_choice.html," + AdventurePool.HIDDEN_APARTMENT,
    })
    void doesntTrackSpecialNoncombat(String htmlFile, int locationId) {
      KoLAdventure location = AdventureDatabase.getAdventure(locationId);
      var cleanups =
          new Cleanups(
              withLastLocation(location),
              withProperty("lastNoncombat" + locationId, 0),
              withAdventuresSpent(location, 5));
      try (cleanups) {
        var req = new GenericRequest("adventure.php?snarfblat=" + locationId);
        req.setHasResult(true);
        req.responseText = html("request/" + htmlFile);
        req.processResponse();

        assertThat("lastNoncombat" + locationId, isSetTo(0));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "2, test_adventure_upper_chamber_a_wheel_how_fortunate.html",
      "5, test_adventure_upper_chamber_down_dooby_doo_down_down.html"
    })
    void doesntTrackUpperChamberSuperlikelies(int turnsSpent, String htmlFile) {
      KoLAdventure location = AdventureDatabase.getAdventure(AdventurePool.UPPER_CHAMBER);
      var cleanups =
          new Cleanups(
              withLastLocation(location),
              withProperty("lastNoncombat" + AdventurePool.UPPER_CHAMBER, 0),
              withAdventuresSpent(location, turnsSpent));
      try (cleanups) {
        AdventureQueueDatabase.resetQueue();
        var req = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.UPPER_CHAMBER);
        req.setHasResult(true);
        req.responseText = html("request/" + htmlFile);
        req.processResponse();

        assertThat("lastNoncombat" + AdventurePool.UPPER_CHAMBER, isSetTo(0));
      }
    }
  }

  @Test
  public void juneCleaverTrackingTest() {
    // regular encounter
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1467&option=1");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 1);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 6);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 0);

    // Skip
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1468&option=4");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467,1468");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 1);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 2);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);

    // Wrong choice
    JuneCleaverManager.parseChoice("choice.php?whichchoice=7000&option=4");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467,1468");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 1);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 2);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);

    // No option
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1469");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467,1468");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 1);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 2);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);

    // Can load queue
    Preferences.setString("juneCleaverQueue", "1467,1468,1469,1470,1471");
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1472&option=3");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467,1468,1469,1470,1471,1472");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 2);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 10);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);

    // Queue has max length of 6
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1473&option=1");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1468,1469,1470,1471,1472,1473");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 3);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 12);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);
  }

  @Test
  public void devReadoutIsStripped() {
    var cleanups =
        new Cleanups(
            withProperty("useDevServer", true),
            withProperty("lastEncounter"),
            withLastLocation("The Spooky Forest"));

    try (cleanups) {
      var request = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.SPOOKY_FOREST);
      request.responseText = html("request/test_choice_on_dev_server.html");
      AdventureRequest.registerEncounter(request);
      assertThat("lastEncounter", isSetTo("Arboreal Respite"));
    }
  }

  @Nested
  class Dinosaurs {
    @ParameterizedTest
    @CsvSource({
      "glass-shelled, archelon, animated ornate nightstand",
      "hot-blooded, dilophosaur, cosmetics wraith",
      "cold-blooded, dilophosaur, cosmetics wraith",
      "swamp, dilophosaur, cosmetics wraith",
      "carrion-eating, dilophosaur, cosmetics wraith",
      "slimy, dilophosaur, cosmetics wraith",
      "steamy, flatusaurus, Hellion",
      "chilling, flatusaurus, Hellion",
      "foul-smelling, flatusaurus, Hellion",
      "mist-shrouded, flatusaurus, Hellion",
      "sweaty, flatusaurus, Hellion",
      "none, ghostasaurus, cubist bull",
      "none, kachungasaur, malevolent hair clog",
      "primitive, chicken, amateur ninja",
      "high-altitude, pterodactyl, W imp",
      "none, spikolodon, empty suit of armor",
      "supersonic, velociraptor, cubist bull",
    })
    public void canExtractDinosaurFromFight(String modifier, String dinosaur, String prey) {
      var cleanups = new Cleanups(withPath(Path.DINOSAURS), withNextMonster((MonsterData) null));
      try (cleanups) {
        // <modifier> <dinosaur> " recently devoured " <prey>
        // <modifier> <dinosaur> " just ate " <prey>
        // <modifier> <dinosaur> " consumed " <prey>
        // <modifier> <dinosaur> " swallowed the soul of " <prey>
        for (String gluttony : AdventureRequest.dinoGluttony) {
          String encounter = "a " + modifier + " " + dinosaur + " " + gluttony + " " + prey;
          MonsterData swallowed = MonsterDatabase.findMonster(prey);
          int monsterId = swallowed.getId();
          String responseText = "<!-- MONSTERID: " + monsterId + " -->";
          MonsterData extracted = AdventureRequest.extractMonster(encounter, responseText);
          MonsterStatusTracker.setNextMonster(extracted);

          MonsterData monster = MonsterStatusTracker.getLastMonster();
          assertEquals(monster.getName(), prey);
          var modifiers = Set.of(monster.getRandomModifiers());
          if (!modifier.equals("none")) {
            assertTrue(modifiers.contains(modifier));
          }
          assertTrue(modifiers.contains(dinosaur));
        }
      }
    }
  }

  @Nested
  class Small {
    @Test
    public void detectsGrassMonsters() {
      var cleanups =
          new Cleanups(
              withFight(0),
              withProperty("lastEncounter"),
              withLastLocation("Fight in the Tall Grass"));

      try (cleanups) {
        AdventureQueueDatabase.resetQueue();
        var req = new GenericRequest("fight.php?ireallymeanit=16");
        req.responseText = html("request/test_fight_small_grass.html");
        String encounter = AdventureRequest.registerEncounter(req);

        assertThat(encounter, is("kilopede"));
        assertThat("lastEncounter", isSetTo("kilopede"));
      }
    }

    @Test
    public void detectsShrunkMonsters() {
      var cleanups =
          new Cleanups(
              withFight(0),
              withProperty("lastEncounter"),
              withLastLocation("The Outskirts of Cobb's Knob"));

      try (cleanups) {
        AdventureQueueDatabase.resetQueue();
        var req = new GenericRequest("fight.php?ireallymeanit=16");
        req.responseText = html("request/test_fight_small_outskirts.html");
        String encounter = AdventureRequest.registerEncounter(req);

        assertThat(encounter, is("Knob Goblin Assistant Chef"));
        assertThat("lastEncounter", isSetTo("Knob Goblin Assistant Chef"));
      }
    }
  }

  @Nested
  class ParseEncounter {
    @Test
    public void canParseDec2024Choice() {
      var encounter = AdventureRequest.parseEncounter(html("request/test_choice_dec2024.html"));
      assertThat(encounter, equalTo("Dr. Gordon Stuart, a Scientist"));
    }

    @Test
    public void canParseDec2024Noncombat() {
      var encounter = AdventureRequest.parseEncounter(html("request/test_noncombat_dec2024.html"));
      assertThat(encounter, equalTo("Cards with Bards"));
    }

    @Test
    public void canParseCyberRealmChoice() {
      var encounter =
          AdventureRequest.parseEncounter(html("request/test_cyber_zone2_choice1.html"));
      assertThat(encounter, equalTo("I Live, You Live..."));
    }
  }

  @Test
  public void recordsMonsterModifiers() {
    var cleanups =
        new Cleanups(
            withFight(0),
            withProperty("lastEncounter"),
            withLastLocation("The Outskirts of Cobb's Knob"));

    try (cleanups) {
      AdventureQueueDatabase.resetQueue();
      var req = new GenericRequest("fight.php?ireallymeanit=16");
      req.responseText = html("request/test_fight_mimeograph_in_ocrs.html");
      String encounter = AdventureRequest.registerEncounter(req);

      assertThat(encounter, is("Sub-Assistant Knob Mad Scientist"));
      var monster = MonsterStatusTracker.getLastMonster();
      assertThat(
          monster.getRandomModifiers(),
          arrayContainingInAnyOrder("dancin'", "floating", "haunted", "untouchable", "mimeo"));
    }
  }

  @Test
  public void recordsMonsterHats() {
    var cleanups =
        new Cleanups(
            withFight(0),
            withPath(Path.HAT_TRICK),
            withProperty("lastEncounter"),
            withLastLocation("The Black Forest"));

    try (cleanups) {
      AdventureQueueDatabase.resetQueue();
      var req = new GenericRequest("fight.php?ireallymeanit=16");
      req.responseText = html("request/test_fight_hat_trick.html");
      String encounter = AdventureRequest.registerEncounter(req);

      assertThat(encounter, is("black adder"));
      var monster = MonsterStatusTracker.getLastMonster();
      assertThat(
          monster.getRandomModifiers(),
          arrayContainingInAnyOrder(
              "construction hardhat",
              "terrycloth turban",
              "jockey's hat",
              "sturdy pith helmet",
              "construction hardhat",
              "imposing pilgrim's hat"));
    }
  }
}
