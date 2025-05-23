package net.sourceforge.kolmafia;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.CryptManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.VioletFogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class RequestEditorKitTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("RequestEditorKitTest");
    Preferences.reset("RequestEditorKitTest");
  }

  @Test
  public void willSuppressRedundantCharPaneRefreshes() {
    // Obsolete usage: put script into HTML comment.
    //
    // <script language=Javascript>
    // <!--
    // if (parent.frames.length == 0) location.href="game.php";
    // top.charpane.location.href="charpane.php";
    // //-->
    // </script>
    //
    // Current usage: no HTML comments:
    //
    // <script>top.charpane.location.href="charpane.php";</script>
    // <script>parent.charpane.location.href="charpane.php";</script>
    //
    // Either will force the browser to issue a request for charpane.php.
    // The issue is that KoL will sometimes include BOTH, forcing two requests.

    String refresh = "(?:top|parent).charpane.location.href=\"charpane.php\";";
    Pattern CHARPANE_REFRESH_PATTERN = Pattern.compile(refresh + "\\n?", Pattern.DOTALL);

    // No charpane refresh requested
    String location = "fight.php?ireallymeanit=1652976032";
    String html = html("request/test_feature_rich_html_charpane_refreshes_0.html");
    StringBuffer buffer = new StringBuffer(html);
    Matcher matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(0, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(0, matcher.results().count());

    // Only "obsolete" usage.
    location = "fight.php?action=steal";
    html = html("request/test_feature_rich_html_charpane_refreshes_1.html");
    buffer = new StringBuffer(html);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());

    // Only "current" usage.
    location = "fight.php?action=attack";
    html = html("request/test_feature_rich_html_charpane_refreshes_1a.html");
    buffer = new StringBuffer(html);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());

    // Both "obsolete" and "current" usage.
    location = "choice.php?pwd&whichchoice=28&option=2";
    html = html("request/test_feature_rich_html_charpane_refreshes_2.html");
    buffer = new StringBuffer(html);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(2, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());
  }

  @Test
  void decoratesChaostheticianMessage() {
    var html = html("request/test_combat_chaosthetician.html");
    var buffer = new StringBuffer(html);
    RequestEditorKit.getFeatureRichHTML("fight.php?action=attack", buffer, false);
    assertThat(
        buffer.toString(),
        containsString(
            "<a href=\"place.php?whichplace=dinorf&action=dinorf_chaos\">Chaosthetician at Dino World</a>"));
  }

  @Nested
  class VioletFog {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addsDecorations(final boolean addComplexFeatures) {
      var cleanups =
          new Cleanups(
              withProperty("relayShowSpoilers", true),
              withProperty(
                  "violetFogLayout",
                  "0,0,0,0,0,0,0,0,57,0,53,0,0,0,0,0,0,0,0,0,0,66,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,67,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,50,0,0,0,0,0,0,0,0,0,0,0,0,0,0"),
              withProperty("lastVioletFogMap", KoLCharacter.getAscensions()),
              withProperty("violetFogGoal", 7));

      try (cleanups) {
        VioletFogManager.reset();
        var buffer =
            new StringBuffer(html("request/test_choice_violet_fog_66_that_way_to_51.html"));
        RequestEditorKit.getFeatureRichHTML(
            "choice.php?pwd&whichchoice=66&option=2", buffer, addComplexFeatures);
        var contents = buffer.toString();
        // Go to Goal is rendered
        assertThat(
            contents, containsString("<input class=button type=submit value=\"Go To Goal\">"));
        // Graph is rendered if we are adding complex features to the page
        var graphMatcher = containsString("id=\"violetFogGraph\"");
        assertThat(contents, addComplexFeatures ? graphMatcher : not(graphMatcher));
      }

      VioletFogManager.reset();
    }
  }

  @Nested
  class DwarvishWarUniform {
    private static Stream<Arguments> provideWarUniformArguments() {
      return Stream.of(
          Arguments.of(
              "<p>A small crystal lens flips down out of the helmet, covering your left eye. You hear a *bleep*, and glowing dwarvish runes appear in it, reading:  "
                  + "<img border=\"0\" title=\"Dwarf Digit Rune F\" alt=\"Dwarf Digit Rune F\" src=\"/images/otherimages/mine/runedigit5.gif\">"
                  + "<img border=\"0\" title=\"Dwarf Digit Rune D\" alt=\"Dwarf Digit Rune D\" src=\"/images/otherimages/mine/runedigit3.gif\">"
                  + "<img border=\"0\" title=\"Dwarf Digit Rune B\" alt=\"Dwarf Digit Rune B\" src=\"/images/otherimages/mine/runedigit1.gif\"></p>",
              "(Attack rating = -1)"),
          Arguments.of(
              "<p>A little light on your sporran lights up orange.</p>",
              "<p>(Defense rating = 1)</p>"),
          Arguments.of(
              "<p>Two little lights light up on your sporran -- a red one and an orange one.</p>",
              "<p>(Defense rating = 1)</p>"),
          Arguments.of(
              "<p>Three little lights light up red, red, and orange on your sporran.</p>",
              "<p>(Defense rating = 1)</p>"),
          Arguments.of(
              "<p>Your sporran lights up with a series of four little lights: red, red, red, and orange.</p>",
              "<p>(Defense rating = 1)</p>"),
          Arguments.of(
              "<p>A bunch of little lights on your sporran start flashing random colors like there's a rave on your crotch.</p>",
              "<p>(Defense rating = 99999)</p>"),
          Arguments.of(
              "<p>Your mattock glows really really really bright blue.</p>",
              "<p>(Hit Points = 21)</p>"));
    }

    @ParameterizedTest
    @MethodSource("provideWarUniformArguments")
    void addsWarUniformTextIfEquipped(String nativeText, String addedText) {
      // TODO: Test rune computation for attack.
      var cleanups =
          new Cleanups(
              withEquipped(Slot.HAT, ItemPool.DWARVISH_WAR_HELMET),
              withEquipped(Slot.PANTS, ItemPool.DWARVISH_WAR_KILT),
              withEquipped(Slot.WEAPON, ItemPool.DWARVISH_WAR_MATTOCK));

      try (cleanups) {
        var buffer = new StringBuffer(nativeText);
        RequestEditorKit.getFeatureRichHTML("fight.php", buffer, false);
        assertThat(buffer.toString(), containsString(addedText));
      }
    }

    @ParameterizedTest
    @MethodSource("provideWarUniformArguments")
    void doesNotAddWarUniformTextIfNotEquipped(String nativeText, String addedText) {
      var buffer = new StringBuffer(nativeText);
      RequestEditorKit.getFeatureRichHTML("fight.php", buffer, false);
      assertThat(buffer.toString(), not(containsString(addedText)));
    }
  }

  @ParameterizedTest
  @CsvSource({
    // Test regular drops
    "fluffy bunny, 'bunny liver (75)'",
    "skeleton with a mop, 'beer-soaked mop (10), ice-cold Willer (30), ice-cold Willer (30)'",
    // Test mix of pp and no pp
    "Dr. Awkward, 'Drowsy Sword (100 no pp), Staff of Fats (100 no pp), fumble formula (5 pp only)'",
    // Test mix of item drops and bounty drops
    "novelty tropical skeleton, 'cherry (0), cherry (0), grapefruit (0), grapefruit (0), orange (0), orange (0), strawberry (0), strawberry (0), lemon (0), lemon (0), novelty fruit hat (0 cond), cherry stem (bounty)'",
    // Test fractional drops
    "stench zombie, 'Dreadsylvanian Almanac page (1 no mod), Freddy Kruegerand (5 no mod), muddy skirt (0.1 cond)'"
  })
  public void addsSimpleItemDrops(final String monsterName, final String itemDropString) {
    var cleanups = new Cleanups(withNextMonster(monsterName));

    try (cleanups) {
      var buffer = new StringBuffer("<span id='monname'>" + monsterName + "</span>");
      RequestEditorKit.getFeatureRichHTML("fight.php", buffer, false);
      assertThat(buffer.toString(), containsString("Drops: " + itemDropString));
    }
  }

  @Nested
  class Events {
    private void loadEvents() {
      var html = html("request/test_main_loads_of_iotm_events.html");
      EventManager.checkForNewEvents(html);
    }

    @ParameterizedTest
    @ValueSource(strings = {"main_replace_events", "main_island"})
    void addsEventsToMain(final String source) {
      loadEvents();

      var html = html("request/test_" + source + ".html");
      var buffer = new StringBuffer(html);
      RequestEditorKit.applyGlobalAdjustments("main.php", buffer, false);
      var output = buffer.toString();

      assertThat(output, containsString(">New Events:</b>"));
      assertThat(
          output,
          containsString("You remember you have a lifetime VIP membership and grab your key!"));

      EventManager.clearEventHistory();
    }
  }

  @Nested
  class Cyrpt {
    @Test
    void decoratesFourCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 200));
      try (cleanups) {
        var html = html("request/test_cyrpt_four_corners.html");
        CryptManager.visitCrypt(html);

        var buffer = new StringBuffer(html);
        RequestEditorKit.getFeatureRichHTML("crypt.php", buffer, true);

        String decorated = buffer.toString();
        assertThat(decorated, containsString("<b>Nook</b> - 50"));
        assertThat(decorated, containsString("<b>Niche</b> - 50"));
        assertThat(decorated, containsString("<b>Cranny</b> - 50"));
        assertThat(decorated, containsString("<b>Alcove</b> - 50"));
      }
    }

    @Test
    void decoratesThreeCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 200));
      try (cleanups) {
        var html = html("request/test_cyrpt_three_corners.html");
        CryptManager.visitCrypt(html);

        var buffer = new StringBuffer(html);
        RequestEditorKit.getFeatureRichHTML("crypt.php", buffer, true);

        String decorated = buffer.toString();
        assertThat(decorated, not(containsString("<b>Nook</b>")));
        assertThat(decorated, containsString("<b>Niche</b> - 50"));
        assertThat(decorated, containsString("<b>Cranny</b> - 50"));
        assertThat(decorated, containsString("<b>Alcove</b> - 50"));
      }
    }

    @Test
    void decoratesTwoCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 200));
      try (cleanups) {
        var html = html("request/test_cyrpt_two_corners.html");
        CryptManager.visitCrypt(html);

        var buffer = new StringBuffer(html);
        RequestEditorKit.getFeatureRichHTML("crypt.php", buffer, true);

        String decorated = buffer.toString();
        assertThat(decorated, not(containsString("<b>Nook</b>")));
        assertThat(decorated, not(containsString("<b>Niche</b>")));
        assertThat(decorated, containsString("<b>Cranny</b> - 50"));
        assertThat(decorated, containsString("<b>Alcove</b> - 50"));
      }
    }

    @Test
    void doesNotDecorateEvilness0Cyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptCrannyEvilness", 0),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptTotalEvilness", 0));
      try (cleanups) {
        var html = html("request/test_cyrpt_haert.html");
        CryptManager.visitCrypt(html);

        var buffer = new StringBuffer(html);
        RequestEditorKit.getFeatureRichHTML("crypt.php", buffer, true);

        String decorated = buffer.toString();
        assertThat(decorated, not(containsString("<b>Nook</b>")));
        assertThat(decorated, not(containsString("<b>Niche</b>")));
        assertThat(decorated, not(containsString("<b>Cranny</b>")));
        assertThat(decorated, not(containsString("<b>Alcove</b>")));
      }
    }

    @Test
    void doesNotDecorateEvilness999Cyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptCrannyEvilness", 0),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptTotalEvilness", 999));
      try (cleanups) {
        var html = html("request/test_cyrpt_haert.html");
        CryptManager.visitCrypt(html);

        var buffer = new StringBuffer(html);

        String decorated = buffer.toString();
        RequestEditorKit.getFeatureRichHTML("crypt.php", buffer, true);
        assertThat(decorated, not(containsString("<b>Nook</b>")));
        assertThat(decorated, not(containsString("<b>Niche</b>")));
        assertThat(decorated, not(containsString("<b>Cranny</b>")));
        assertThat(decorated, not(containsString("<b>Alcove</b>")));
      }
    }

    @Test
    void doesNotDecorateEmptyCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptCrannyEvilness", 0),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptTotalEvilness", 0));
      try (cleanups) {
        var html = html("request/test_cyrpt_empty.html");
        CryptManager.visitCrypt(html);

        var buffer = new StringBuffer(html);
        RequestEditorKit.getFeatureRichHTML("crypt.php", buffer, true);

        String decorated = buffer.toString();
        assertThat(decorated, not(containsString("<b>Nook</b>")));
        assertThat(decorated, not(containsString("<b>Niche</b>")));
        assertThat(decorated, not(containsString("<b>Cranny</b>")));
        assertThat(decorated, not(containsString("<b>Alcove</b>")));
      }
    }

    @Test
    void doesNotDecorateBogusEvilnessCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptCrannyEvilness", 11),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptTotalEvilness", 11));
      try (cleanups) {
        var html = html("request/test_cyrpt_haert.html");
        var buffer = new StringBuffer(html);
        RequestEditorKit.getFeatureRichHTML("crypt.php", buffer, true);

        String decorated = buffer.toString();
        assertThat(decorated, not(containsString("<b>Nook</b>")));
        assertThat(decorated, not(containsString("<b>Niche</b>")));
        assertThat(decorated, not(containsString("<b>Cranny</b>")));
        assertThat(decorated, not(containsString("<b>Alcove</b>")));
      }
    }
  }

  @Nested
  class CosmeticModifiers {
    @Test
    void canAddCustomModifiersToMonsterWithNone() {
      var cleanups = withProperty("extraCosmeticModifiers", "wobble");

      try (cleanups) {
        var text = html("request/test_fight_fleaman.html");
        var buffer = new StringBuffer(text);
        RequestEditorKit.suppressPowerPixellation(buffer);
        var modifiers = RequestEditorKit.parseCosmeticModifiers(buffer.toString());
        assertThat(modifiers, containsInAnyOrder("wobble"));
      }
    }

    @Test
    void canAddCustomModifiersToMonsterWithSome() {
      var cleanups = withProperty("extraCosmeticModifiers", "wobble");

      try (cleanups) {
        var text = html("request/test_fight_mimeograph.html");
        var buffer = new StringBuffer(text);
        RequestEditorKit.suppressPowerPixellation(buffer);
        var modifiers = RequestEditorKit.parseCosmeticModifiers(buffer.toString());
        assertThat(modifiers, containsInAnyOrder("wobble", "mimeo"));
      }
    }

    @Test
    void canSuppressPowerPixellation() {
      var cleanups = withProperty("suppressPowerPixellation", "true");

      try (cleanups) {
        var text = html("request/test_fight_oil_slick.html");
        var buffer = new StringBuffer(text);
        RequestEditorKit.suppressPowerPixellation(buffer);
        var modifiers = RequestEditorKit.parseCosmeticModifiers(buffer.toString());
        assertThat(modifiers, containsInAnyOrder());
      }
    }

    @Test
    void canSuppressPowerPixellationWhileAddingModifier() {
      var cleanups =
          new Cleanups(
              withProperty("suppressPowerPixellation", "true"),
              withProperty("extraCosmeticModifiers", "drunk,floating"));

      try (cleanups) {
        var text = html("request/test_fight_oil_slick.html");
        var buffer = new StringBuffer(text);
        RequestEditorKit.suppressPowerPixellation(buffer);
        var modifiers = RequestEditorKit.parseCosmeticModifiers(buffer.toString());
        assertThat(
            modifiers,
            containsInAnyOrder("appendimg:adventureimages\\/ol_drunk.gif:0:0", "floating"));
      }
    }
  }

  @Nested
  class RelayDecorations {
    @Test
    void decoratesNewAreaUnlocked() {
      var buffer = new StringBuffer(html("request/test_place_mclargehuge_trapper_give_quest.html"));
      RequestEditorKit.getFeatureRichHTML(
          "place.php?whichplace=mclargehuge&action=trappercabin", buffer, true);
      var contents = buffer.toString();
      // This is not the best example of why we add links, since KoL
      // itself shows you the updated Mt. McLargeHuge, but we add links
      // to both locations, anyway.
      assertThat(
          contents,
          containsString(
              "<a class=nounder href=\"adventure.php?snarfblat=271\"><b>The Goatlet</b></a>"));
      assertThat(
          contents,
          containsString(
              "<a class=nounder href=\"adventure.php?snarfblat=270\"><b>Itznotyerzitz Mine</b>"));
    }

    @Test
    void decoratesABooPeakTheHorror() {
      var cleanups =
          new Cleanups(
              withProperty("relayShowSpoilers", true),
              withItem(ItemPool.BOO_CLUE),
              withPasswordHash("BOO"));
      try (cleanups) {
        var buffer = new StringBuffer(html("request/test_aboo_peak_flee_the_horror.html"));
        RequestEditorKit.getFeatureRichHTML("choice.php?whichchoice=611&option=2", buffer, true);
        var contents = buffer.toString();
        var expected =
            "<a href=\"javascript:singleUse('inv_use.php','which=3&whichitem="
                + ItemPool.BOO_CLUE
                + "&pwd=BOO&ajax=1');void(0);\">Use another A-Boo Clue</a>";
        assertThat(contents, containsString(expected));
      }
    }

    @Test
    void decoratesHauntedBallroomRottingMatilda() {
      var cleanups =
          new Cleanups(
              withProperty("relayShowSpoilers", true),
              withItem(ItemPool.DANCE_CARD),
              withPasswordHash("DANCE"));
      try (cleanups) {
        var buffer = new StringBuffer(html("request/test_haunted_ballroom_rotting_matilda.html"));
        RequestEditorKit.getFeatureRichHTML(
            "adventure.php?snarfblat=" + AdventurePool.HAUNTED_BALLROOM, buffer, true);
        var contents = buffer.toString();
        var expected =
            "<a href=\"javascript:singleUse('inv_use.php','which=3&whichitem="
                + ItemPool.DANCE_CARD
                + "&pwd=DANCE&ajax=1');void(0);\">Use another dance card</a>";
        assertThat(contents, containsString(expected));
      }
    }
  }

  @Nested
  class CyberRealm {
    @Test
    void decorationControlledBySettings() {
      var cleanups =
          new Cleanups(
              withProperty("suppressCyberRealmDarkMode", false),
              withProperty("suppressCyberRealmGreenImages", false));
      try (cleanups) {
        var buffer = new StringBuffer(html("request/test_fight_cyberrealm.html"));
        RequestEditorKit.getFeatureRichHTML("fight.php", buffer, true);
        var contents = buffer.toString();
        assertTrue(contents.contains("fixedsys"));
        assertTrue(contents.contains("cyberit"));
      }
    }

    @Test
    void canSuppressDarkMode() {
      var cleanups =
          new Cleanups(
              withProperty("suppressCyberRealmDarkMode", true),
              withProperty("suppressCyberRealmGreenImages", false));
      try (cleanups) {
        var buffer = new StringBuffer(html("request/test_fight_cyberrealm.html"));
        RequestEditorKit.getFeatureRichHTML("fight.php", buffer, true);
        var contents = buffer.toString();
        assertFalse(contents.contains("fixedsys"));
      }
    }

    @Test
    void canSuppressGreenImages() {
      var cleanups =
          new Cleanups(
              withProperty("suppressCyberRealmDarkMode", false),
              withProperty("suppressCyberRealmGreenImages", true));
      try (cleanups) {
        var buffer = new StringBuffer(html("request/test_fight_cyberrealm.html"));
        RequestEditorKit.getFeatureRichHTML("fight.php", buffer, true);
        var contents = buffer.toString();
        assertFalse(contents.contains("cyberit"));
      }
    }

    @Test
    void canDetectExistingReturnToNetworkMap() {
      var buffer = new StringBuffer(html("request/test_adventure_hacked_cyberrealm_zone1.html"));
      var original = buffer.toString();
      RequestEditorKit.getFeatureRichHTML("adventure.php?snarfblat=585", buffer, true);
      var contents = buffer.toString();
      assertTrue(original.contains("Back to the Network Map"));
      assertTrue(contents.contains("Back to the Network Map"));
    }

    @Test
    void canInsertNewReturnToNetworkMap() {
      var buffer = new StringBuffer(html("request/test_adventure_hacked_cyberrealm_zone2.html"));
      var original = buffer.toString();
      RequestEditorKit.getFeatureRichHTML("adventure.php?snarfblat=586", buffer, true);
      var contents = buffer.toString();
      assertFalse(original.contains("Back to the Network Map"));
      assertTrue(contents.contains("Back to the Network Map"));
    }
  }

  @Test
  void decoratesFightDec2024() {
    var html = html("request/test_fight_dec2024.html");
    var buffer = new StringBuffer(html);
    RequestEditorKit.getFeatureRichHTML("fight.php?ireallymeanit=1734262567", buffer, false);
    var str = buffer.toString();
    assertThat(str, containsString("Round 1!"));
    assertThat(str, containsString("Round 2!"));
  }

  @Nested
  class Darkness {
    @Test
    void decoratesDarnessFightWithActualMonsterName() {
      var cleanups =
          new Cleanups(withProperty("relayShowSpoilers", true), withNextMonster("The Bush"));
      try (cleanups) {
        var buffer = new StringBuffer(html("request/test_fight_the_darkness.html"));
        var original = buffer.toString();
        assertThat(original, containsString("<span id='monname'>the darkness</span>"));
        assertThat(original, containsString("<!-- MONSTERID: 537 -->"));

        RequestEditorKit.getFeatureRichHTML("fight.php?ireallymeanit=1737917117", buffer, true);
        var contents = buffer.toString();
        var expected =
            "<span class=\"elementsleaze\" id='monname'>The Bush hiding in the darkness</span>";
        assertThat(contents, containsString(expected));
      }
    }
  }
}
