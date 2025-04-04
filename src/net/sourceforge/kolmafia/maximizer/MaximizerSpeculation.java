package net.sourceforge.kolmafia.maximizer;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.Speculation;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.MultiStringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

public class MaximizerSpeculation extends Speculation
    implements Comparable<MaximizerSpeculation>, Cloneable {
  private boolean scored = false;
  private boolean tiebreakered = false;
  private boolean exceeded;
  private double score, tiebreaker;
  private int simplicity;
  private int beeosity;

  public boolean failed = false;
  public CheckedItem attachment;
  private boolean foldables = false;

  @Override
  public MaximizerSpeculation clone() {
    try {
      MaximizerSpeculation copy = (MaximizerSpeculation) super.clone();
      copy.equipment = this.equipment.clone();
      return copy;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  @Override
  public String toString() {
    if (this.attachment != null) {
      return this.attachment.getInstance((int) this.getScore()).toString();
    }
    return super.toString();
  }

  public void setUnscored() {
    this.scored = false;
    this.calculated = false;
  }

  public double getScore() {
    if (this.scored) return this.score;
    if (!this.calculated) this.calculate();
    this.score = Maximizer.eval.getScore(this.mods, this.equipment);
    if (KoLCharacter.inBeecore()) {
      this.beeosity = KoLCharacter.getBeeosity(this.equipment);
    }
    Maximizer.eval.checkEquipment(this.mods, this.equipment, this.beeosity);
    this.failed = Maximizer.eval.failed;
    if ((this.mods.getRawBitmap(BitmapModifier.MUTEX_VIOLATIONS)
            & ~KoLCharacter.currentRawBitmapModifier(BitmapModifier.MUTEX_VIOLATIONS))
        != 0) { // We're speculating about something that would create a
      // mutex problem that the player didn't already have.
      this.failed = true;
    }
    this.exceeded = Maximizer.eval.exceeded;
    this.scored = true;
    return this.score;
  }

  public double getTiebreaker() {
    if (this.tiebreakered) return this.tiebreaker;
    if (!this.calculated) this.calculate();
    this.tiebreaker = Maximizer.eval.getTiebreaker(this.mods);
    this.tiebreakered = true;
    this.simplicity = 0;
    for (var slot : SlotSet.ALL_SLOTS) {
      AdventureResult item = this.equipment.get(slot);
      if (item == null) item = EquipmentRequest.UNEQUIP;
      if (EquipmentManager.getEquipment(slot).equals(item)) {
        this.simplicity += 2;
      } else if (item.equals(EquipmentRequest.UNEQUIP)) {
        this.simplicity += slot == Slot.WEAPON ? -1 : 1;
      }
    }
    return this.tiebreaker;
  }

  @Override
  public int compareTo(MaximizerSpeculation o) {
    if (o == null) return 1;
    MaximizerSpeculation other = o;
    int rv = Double.compare(this.getScore(), other.getScore());
    // Always prefer success to failure
    if (this.failed != other.failed) return this.failed ? -1 : 1;
    // Prefer higher bonus
    if (rv != 0) return rv;
    // In Bees Hate You, prefer lower B count
    rv = other.beeosity - this.beeosity;
    if (rv != 0) return rv;
    // Get other comparisons
    int countThisEffects = 0;
    int countOtherEffects = 0;
    int countThisBreakables = 0;
    int countOtherBreakables = 0;
    int countThisDropsItems = 0;
    int countOtherDropsItems = 0;
    int countThisDropsMeat = 0;
    int countOtherDropsMeat = 0;
    for (var equip : this.equipment.values()) {
      if (equip == null) continue;
      int itemId = equip.getItemId();
      Modifiers mods = ModifierDatabase.getItemModifiers(itemId);
      if (mods == null) continue;
      var rolloverEffects = mods.getStrings(MultiStringModifier.ROLLOVER_EFFECT);
      if (!rolloverEffects.isEmpty()) countThisEffects++;
      if (mods.getBoolean(BooleanModifier.BREAKABLE)) countThisBreakables++;
      if (mods.getBoolean(BooleanModifier.DROPS_ITEMS)) countThisDropsItems++;
      if (mods.getBoolean(BooleanModifier.DROPS_MEAT)) countThisDropsMeat++;
    }
    for (var equip : other.equipment.values()) {
      if (equip == null) continue;
      int itemId = equip.getItemId();
      Modifiers mods = ModifierDatabase.getItemModifiers(itemId);
      if (mods == null) continue;
      var rolloverEffects = mods.getStrings(MultiStringModifier.ROLLOVER_EFFECT);
      if (!rolloverEffects.isEmpty()) countOtherEffects++;
      if (mods.getBoolean(BooleanModifier.BREAKABLE)) countOtherBreakables++;
      if (mods.getBoolean(BooleanModifier.DROPS_ITEMS)) countOtherDropsItems++;
      if (mods.getBoolean(BooleanModifier.DROPS_MEAT)) countOtherDropsMeat++;
    }
    // Prefer item droppers
    if (Maximizer.eval.isUsingTiebreaker() && countThisDropsItems != countOtherDropsItems) {
      return countThisDropsItems > countOtherDropsItems ? 1 : -1;
    }
    // Prefer meat droppers
    if (Maximizer.eval.isUsingTiebreaker() && countThisDropsMeat != countOtherDropsMeat) {
      return countThisDropsMeat > countOtherDropsMeat ? 1 : -1;
    }
    // Prefer higher tiebreaker account (unless -tie used)
    rv = Double.compare(this.getTiebreaker(), other.getTiebreaker());
    if (rv != 0) return rv;
    // Prefer rollover effects
    if (Maximizer.eval.isUsingTiebreaker() && countThisEffects != countOtherEffects) {
      return countThisEffects > countOtherEffects ? 1 : -1;
    }
    // Prefer unbreakables
    if (countThisBreakables != countOtherBreakables) {
      return countThisBreakables < countOtherBreakables ? 1 : -1;
    }
    // Prefer worn
    rv = this.simplicity - other.simplicity;
    if (rv != 0) return rv;
    if (this.attachment != null && other.attachment != null) {
      // prefer items that you don't have to buy
      if (this.attachment.buyableFlag != other.attachment.buyableFlag) {
        return this.attachment.buyableFlag ? -1 : 1;
      }
      if (KoLCharacter.inBeecore()) { // prefer fewer Bs
        rv =
            KoLCharacter.getBeeosity(other.attachment.getName())
                - KoLCharacter.getBeeosity(this.attachment.getName());
      }

      // prefer items that you have
      // doesn't consider wanting multiple of the same item and not having enough
      if ((this.attachment.inventory > 0) != (other.attachment.inventory > 0)) {
        return this.attachment.inventory > 0 ? 1 : -1;
      }
      if ((this.attachment.initial > 0) != (other.attachment.initial > 0)) {
        return this.attachment.initial > 0 ? 1 : -1;
      }
    }
    return rv;
  }

  // Remember which equipment slots were null, so that this
  // state can be restored later.
  public EnumMap<Slot, AdventureResult> mark() {
    return this.equipment.clone();
  }

  public void restore(EnumMap<Slot, AdventureResult> mark) {
    this.equipment.putAll(mark);
  }

  public void tryAll(
      List<FamiliarData> familiars,
      List<FamiliarData> enthronedFamiliars,
      Map<Integer, Boolean> usefulOutfits,
      Map<AdventureResult, AdventureResult> outfitPieces,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    this.foldables = Preferences.getBoolean("maximizerFoldables");
    this.tryOutfits(
        enthronedFamiliars,
        usefulOutfits,
        outfitPieces,
        possibles,
        bestCard,
        useCrownFamiliar,
        useBjornFamiliar);
    for (int i = 0; i < familiars.size(); ++i) {
      this.setFamiliar(familiars.get(i));
      possibles.set(Slot.FAMILIAR, possibles.getFamiliar(i));
      this.tryOutfits(
          enthronedFamiliars,
          usefulOutfits,
          outfitPieces,
          possibles,
          bestCard,
          useCrownFamiliar,
          useBjornFamiliar);
    }
  }

  public void tryOutfits(
      List<FamiliarData> enthronedFamiliars,
      Map<Integer, Boolean> usefulOutfits,
      Map<AdventureResult, AdventureResult> outfitPieces,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    for (Integer outfit : usefulOutfits.keySet()) {
      if (!usefulOutfits.get(outfit)) continue;
      AdventureResult[] pieces = EquipmentDatabase.getOutfit(outfit).getPieces();
      pieceloop:
      for (int idx = pieces.length - 1; ; --idx) {
        if (idx == -1) { // all pieces successfully put on
          this.tryFamiliarItems(
              enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
          break;
        }
        AdventureResult item = outfitPieces.get(pieces[idx]);
        if (item == null) break; // not available
        int count = item.getCount();
        Slot slot = EquipmentManager.itemIdToEquipmentType(item.getItemId());

        switch (slot) {
          case HAT:
          case PANTS:
          case SHIRT:
          case CONTAINER:
            if (item.equals(this.equipment.get(slot))) { // already worn
              continue pieceloop;
            }
            if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
              --count;
            }
            break;
          case WEAPON:
          case OFFHAND:
            if (item.equals(this.equipment.get(Slot.WEAPON))
                || item.equals(this.equipment.get(Slot.OFFHAND))) { // already worn
              continue pieceloop;
            }
            if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
              --count;
            }
            break;
          case ACCESSORY1:
            if (item.equals(this.equipment.get(Slot.ACCESSORY1))
                || item.equals(this.equipment.get(Slot.ACCESSORY2))
                || item.equals(this.equipment.get(Slot.ACCESSORY3))) { // already worn
              continue pieceloop;
            }
            if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
              --count;
            }
            if (this.equipment.get(Slot.ACCESSORY3) == null) {
              slot = Slot.ACCESSORY3;
            } else if (this.equipment.get(Slot.ACCESSORY2) == null) {
              slot = Slot.ACCESSORY2;
            }
            break;
          default:
            break pieceloop; // don't know how to wear that
        }

        if (count <= 0) break; // none available
        if (this.equipment.get(slot) != null) break; // slot taken
        this.equipment.put(slot, item);
      }
      this.restore(mark);
    }

    this.tryFamiliarItems(
        enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
  }

  public void tryFamiliarItems(
      List<FamiliarData> enthronedFamiliars,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.FAMILIAR) == null) {
      List<CheckedItem> possible = possibles.get(Slot.FAMILIAR);
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment.get(Slot.OFFHAND))) {
          --count;
        }
        if (item.equals(this.equipment.get(Slot.WEAPON))) {
          --count;
        }
        if (item.equals(this.equipment.get(Slot.HAT))) {
          --count;
        }
        if (item.equals(this.equipment.get(Slot.PANTS))) {
          --count;
        }
        FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = group.names.get(0);
          for (var slot : SlotSet.SLOTS) {
            if (slot != Slot.FAMILIAR && this.equipment.get(slot) != null) {
              FoldGroup groupEquipped =
                  ItemDatabase.getFoldGroup(this.equipment.get(slot).getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.names.get(0))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment.put(Slot.FAMILIAR, item);
        this.tryContainers(
            enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
        any = true;
        this.restore(mark);
      }

      if (any) return;
      this.equipment.put(Slot.FAMILIAR, EquipmentRequest.UNEQUIP);
    }

    this.tryContainers(enthronedFamiliars, possibles, bestCard, useCrownFamiliar, useBjornFamiliar);
    this.restore(mark);
  }

  public void tryContainers(
      List<FamiliarData> enthronedFamiliars,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar,
      FamiliarData useBjornFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.CONTAINER) == null) {
      List<CheckedItem> possible = possibles.get(Slot.CONTAINER);
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        CheckedItem item = possible.get(pos);
        int count = item.getCount();
        FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = group.names.get(0);
          for (var slot : SlotSet.SLOTS) {
            if (slot != Slot.CONTAINER && this.equipment.get(slot) != null) {
              FoldGroup groupEquipped =
                  ItemDatabase.getFoldGroup(this.equipment.get(slot).getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.names.get(0))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment.put(Slot.CONTAINER, item);
        if (item.getItemId() == ItemPool.BUDDY_BJORN) {
          if (useBjornFamiliar != null) {
            this.setBjorned(useBjornFamiliar);
            this.tryAccessories(enthronedFamiliars, possibles, 0, bestCard, useCrownFamiliar);
            any = true;
            this.restore(mark);
          } else {
            for (FamiliarData f : enthronedFamiliars) {
              this.setBjorned(f);
              this.tryAccessories(enthronedFamiliars, possibles, 0, bestCard, useCrownFamiliar);
              any = true;
              this.restore(mark);
            }
          }
        } else {
          this.tryAccessories(enthronedFamiliars, possibles, 0, bestCard, useCrownFamiliar);
          any = true;
          this.restore(mark);
        }
      }

      if (any) return;
      this.equipment.put(Slot.CONTAINER, EquipmentRequest.UNEQUIP);
    }

    this.tryAccessories(enthronedFamiliars, possibles, 0, bestCard, useCrownFamiliar);
    this.restore(mark);
  }

  public void tryAccessories(
      List<FamiliarData> enthronedFamiliars,
      SlotList<CheckedItem> possibles,
      int pos,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    int free = 0;
    if (this.equipment.get(Slot.ACCESSORY1) == null) ++free;
    if (this.equipment.get(Slot.ACCESSORY2) == null) ++free;
    if (this.equipment.get(Slot.ACCESSORY3) == null) ++free;
    if (free > 0) {
      List<CheckedItem> possible = possibles.get(Slot.ACCESSORY1);
      boolean any = false;
      for (; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment.get(Slot.ACCESSORY1))) {
          --count;
        }
        if (item.equals(this.equipment.get(Slot.ACCESSORY2))) {
          --count;
        }
        if (item.equals(this.equipment.get(Slot.ACCESSORY3))) {
          --count;
        }
        FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = group.names.get(0);
          for (var slot : SlotSet.SLOTS) {
            if (this.equipment.get(slot) != null) {
              FoldGroup groupEquipped =
                  ItemDatabase.getFoldGroup(this.equipment.get(slot).getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.names.get(0))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        for (count = Math.min(free, count); count > 0; --count) {
          if (this.equipment.get(Slot.ACCESSORY1) == null) {
            this.equipment.put(Slot.ACCESSORY1, item);
          } else if (this.equipment.get(Slot.ACCESSORY2) == null) {
            this.equipment.put(Slot.ACCESSORY2, item);
          } else if (this.equipment.get(Slot.ACCESSORY3) == null) {
            this.equipment.put(Slot.ACCESSORY3, item);
          } else {
            System.out.println("no room left???");
            break; // no room left - shouldn't happen
          }

          this.tryAccessories(enthronedFamiliars, possibles, pos + 1, bestCard, useCrownFamiliar);
          any = true;
        }
        this.restore(mark);
      }

      if (any) return;

      if (this.equipment.get(Slot.ACCESSORY1) == null) {
        this.equipment.put(Slot.ACCESSORY1, EquipmentRequest.UNEQUIP);
      }
      if (this.equipment.get(Slot.ACCESSORY2) == null) {
        this.equipment.put(Slot.ACCESSORY2, EquipmentRequest.UNEQUIP);
      }
      if (this.equipment.get(Slot.ACCESSORY3) == null) {
        this.equipment.put(Slot.ACCESSORY3, EquipmentRequest.UNEQUIP);
      }
    }

    this.trySwap(Slot.ACCESSORY1, Slot.ACCESSORY2);
    this.trySwap(Slot.ACCESSORY2, Slot.ACCESSORY3);
    this.trySwap(Slot.ACCESSORY3, Slot.ACCESSORY1);

    this.tryHats(enthronedFamiliars, possibles, bestCard, useCrownFamiliar);
    this.restore(mark);
  }

  public void tryHats(
      List<FamiliarData> enthronedFamiliars,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      FamiliarData useCrownFamiliar)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.HAT) == null) {
      List<CheckedItem> possible = possibles.get(Slot.HAT);
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        CheckedItem item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
          --count;
        }
        FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = group.names.get(0);
          for (var slot : SlotSet.SLOTS) {
            if (slot != Slot.HAT && this.equipment.get(slot) != null) {
              FoldGroup groupEquipped =
                  ItemDatabase.getFoldGroup(this.equipment.get(slot).getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.names.get(0))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment.put(Slot.HAT, item);
        if (item.getItemId() == ItemPool.HATSEAT) {
          if (useCrownFamiliar != null) {
            this.setEnthroned(useCrownFamiliar);
            this.tryShirts(possibles, bestCard);
            any = true;
            this.restore(mark);
          } else {
            for (FamiliarData f : enthronedFamiliars) {
              // Cannot use same familiar for this and Bjorn
              if (f != this.getBjorned() || f == FamiliarData.NO_FAMILIAR) {
                this.setEnthroned(f);
                this.tryShirts(possibles, bestCard);
                any = true;
                this.restore(mark);
              }
            }
          }
        } else {
          this.tryShirts(possibles, bestCard);
          any = true;
          this.restore(mark);
        }
      }

      if (any) return;
      this.equipment.put(Slot.HAT, EquipmentRequest.UNEQUIP);
    }

    this.tryShirts(possibles, bestCard);
    this.restore(mark);
  }

  public void tryShirts(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.SHIRT) == null) {
      boolean any = false;
      if (KoLCharacter.isTorsoAware()) {
        List<CheckedItem> possible = possibles.get(Slot.SHIRT);
        for (int pos = 0; pos < possible.size(); ++pos) {
          AdventureResult item = possible.get(pos);
          int count = item.getCount();
          if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
            --count;
          }
          FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
          if (group != null && this.foldables) {
            String groupName = group.names.get(0);
            for (var slot : SlotSet.SLOTS) {
              if (slot != Slot.SHIRT && this.equipment.get(slot) != null) {
                FoldGroup groupEquipped =
                    ItemDatabase.getFoldGroup(this.equipment.get(slot).getName());
                if (groupEquipped != null && groupName.equals(groupEquipped.names.get(0))) {
                  --count;
                }
              }
            }
          }
          if (count <= 0) continue;
          this.equipment.put(Slot.SHIRT, item);
          this.tryPants(possibles, bestCard);
          any = true;
          this.restore(mark);
        }
      }

      if (any) return;
      this.equipment.put(Slot.SHIRT, EquipmentRequest.UNEQUIP);
    }

    this.tryPants(possibles, bestCard);
    this.restore(mark);
  }

  public void tryPants(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.PANTS) == null) {
      List<CheckedItem> possible = possibles.get(Slot.PANTS);
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
          --count;
        }
        FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = group.names.get(0);
          for (var slot : SlotSet.SLOTS) {
            if (slot != Slot.PANTS && this.equipment.get(slot) != null) {
              FoldGroup groupEquipped =
                  ItemDatabase.getFoldGroup(this.equipment.get(slot).getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.names.get(0))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment.put(Slot.PANTS, item);
        this.trySixguns(possibles, bestCard);
        any = true;
        this.restore(mark);
      }

      if (any) return;
      this.equipment.put(Slot.PANTS, EquipmentRequest.UNEQUIP);
    }

    this.trySixguns(possibles, bestCard);
    this.restore(mark);
  }

  public void trySixguns(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    if (this.equipment.get(Slot.HOLSTER) == null) {
      List<CheckedItem> possible = possibles.get(Slot.HOLSTER);
      boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (count <= 0) continue;
        this.equipment.put(Slot.HOLSTER, item);
        this.tryWeapons(possibles, bestCard);
        any = true;
        this.restore(mark);
      }

      if (any) return;
      this.equipment.put(Slot.HOLSTER, EquipmentRequest.UNEQUIP);
    }

    this.tryWeapons(possibles, bestCard);
    this.restore(mark);
  }

  public void tryWeapons(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    boolean chefstaffable =
        KoLCharacter.hasSkill(SkillPool.SPIRIT_OF_RIGATONI) || KoLCharacter.isJarlsberg();
    if (!chefstaffable && KoLCharacter.isSauceror()) {
      chefstaffable =
          this.equipment.get(Slot.ACCESSORY1).getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE
              || this.equipment.get(Slot.ACCESSORY2).getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE
              || this.equipment.get(Slot.ACCESSORY3).getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE;
    }
    if (this.equipment.get(Slot.WEAPON) == null) {
      List<CheckedItem> possible = possibles.get(Slot.WEAPON);
      // boolean any = false;
      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        if (!chefstaffable && EquipmentDatabase.getItemType(item.getItemId()).equals("chefstaff")) {
          continue;
        }
        int count = item.getCount();
        if (item.equals(this.equipment.get(Slot.OFFHAND))) {
          --count;
        }
        if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
          --count;
        }
        FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = group.names.get(0);
          for (var slot : SlotSet.SLOTS) {
            if (slot != Slot.WEAPON && this.equipment.get(slot) != null) {
              FoldGroup groupEquipped =
                  ItemDatabase.getFoldGroup(this.equipment.get(slot).getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.names.get(0))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        this.equipment.put(Slot.WEAPON, item);
        this.tryOffhands(possibles, bestCard);
        // any = true;
        this.restore(mark);
      }

      // if ( any && <no unarmed items in shortlists> ) return;
      if (Maximizer.eval.melee < -1 || Maximizer.eval.melee > 1) {
        return;
      }
      this.equipment.put(Slot.WEAPON, EquipmentRequest.UNEQUIP);
    } else if (!chefstaffable
        && EquipmentDatabase.getItemType(this.equipment.get(Slot.WEAPON).getItemId())
            .equals("chefstaff")) {
      return;
    }

    this.tryOffhands(possibles, bestCard);
    this.restore(mark);
  }

  public void tryOffhands(SlotList<CheckedItem> possibles, AdventureResult bestCard)
      throws MaximizerInterruptedException {
    var mark = this.mark();
    int weapon = this.equipment.get(Slot.WEAPON).getItemId();
    if (EquipmentDatabase.getHands(weapon) > 1) {
      this.equipment.put(Slot.OFFHAND, EquipmentRequest.UNEQUIP);
    }

    if (this.equipment.get(Slot.OFFHAND) == null) {
      List<CheckedItem> possible;
      WeaponType weaponType = WeaponType.NONE;
      if (KoLCharacter.hasSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING)) {
        weaponType = EquipmentDatabase.getWeaponType(weapon);
      }
      possible =
          switch (weaponType) {
            case MELEE -> possibles.get(Evaluator.OFFHAND_MELEE);
            case RANGED -> possibles.get(Evaluator.OFFHAND_RANGED);
            default -> possibles.get(Slot.OFFHAND);
          };
      boolean any = false;

      for (int pos = 0; pos < possible.size(); ++pos) {
        AdventureResult item = possible.get(pos);
        int count = item.getCount();
        if (item.equals(this.equipment.get(Slot.WEAPON))) {
          --count;
        }
        if (item.equals(this.equipment.get(Slot.FAMILIAR))) {
          --count;
        }
        FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
        if (group != null && this.foldables) {
          String groupName = group.names.get(0);
          for (var slot : SlotSet.SLOTS) {
            if (slot != Slot.OFFHAND && this.equipment.get(slot) != null) {
              FoldGroup groupEquipped =
                  ItemDatabase.getFoldGroup(this.equipment.get(slot).getName());
              if (groupEquipped != null && groupName.equals(groupEquipped.names.get(0))) {
                --count;
              }
            }
          }
        }
        if (count <= 0) continue;
        if (item.getItemId() == ItemPool.CARD_SLEEVE) {
          this.equipment.put(Slot.CARDSLEEVE, bestCard);
        }
        this.equipment.put(Slot.OFFHAND, item);
        this.tryOffhands(possibles, bestCard);
        any = true;
        this.restore(mark);
      }

      if (any && weapon > 0) return;
      this.equipment.put(Slot.OFFHAND, EquipmentRequest.UNEQUIP);
    }

    // doit
    this.calculated = false;
    this.scored = false;
    this.tiebreakered = false;
    if (Maximizer.best == null) {
      RequestLogger.updateSessionLog(
          "Maximizer about to throw LimitExceeded because of null best.");
      // this isn't really what is happening but trying to understand why this is happening, first.
      throw new MaximizerLimitException();
    }
    if (this.compareTo(Maximizer.best) > 0) {
      Maximizer.best = this.clone();
    }
    Maximizer.bestChecked++;
    long t = System.currentTimeMillis();
    if (t > Maximizer.bestUpdate) {
      MaximizerSpeculation.showProgress();
      Maximizer.bestUpdate = t + 5000;
    }
    this.restore(mark);
    if (!KoLmafia.permitsContinue()) {
      throw new MaximizerInterruptedException();
    }
    if (this.exceeded) {
      throw new MaximizerExceededException();
    }
    long comboLimit = Preferences.getLong("maximizerCombinationLimit");
    if (comboLimit != 0 && Maximizer.bestChecked >= comboLimit) {
      throw new MaximizerLimitException();
    }
  }

  private static int getMutex(AdventureResult item) {
    Modifiers mods = ModifierDatabase.getItemModifiers(item.getItemId());
    if (mods == null) {
      return 0;
    }
    return mods.getRawBitmap(BitmapModifier.MUTEX);
  }

  private void trySwap(Slot slot1, Slot slot2) {
    // If we are suggesting an accessory that's already being worn,
    // make sure we suggest the same slot (to minimize server hits).
    AdventureResult item1, item2, eq1, eq2;
    item1 = this.equipment.get(slot1);
    if (item1 == null) item1 = EquipmentRequest.UNEQUIP;
    eq1 = EquipmentManager.getEquipment(slot1);
    if (eq1.equals(item1)) return;
    item2 = this.equipment.get(slot2);
    if (item2 == null) item2 = EquipmentRequest.UNEQUIP;
    eq2 = EquipmentManager.getEquipment(slot2);
    if (eq2.equals(item2)) return;

    // The same thing applies to mutually exclusive accessories -
    // putting the new one in an earlier slot would cause an error
    // when the equipment is being changed.
    int imutex1, imutex2, emutex1, emutex2;
    imutex1 = getMutex(item1);
    emutex1 = getMutex(eq1);
    if ((imutex1 & emutex1) != 0) return;
    imutex2 = getMutex(item2);
    emutex2 = getMutex(eq2);
    if ((imutex2 & emutex2) != 0) return;

    if (eq1.equals(item2)
        || eq2.equals(item1)
        || (imutex1 & emutex2) != 0
        || (imutex2 & emutex1) != 0) {
      this.equipment.put(slot1, item2);
      this.equipment.put(slot2, item1);
    }
  }

  public static void showProgress() {
    StringBuilder msg = new StringBuilder();
    msg.append(Maximizer.bestChecked);
    msg.append(" combinations checked, best score ");
    double score = Maximizer.best.getScore();
    msg.append(KoLConstants.FLOAT_FORMAT.format(score));
    if (Maximizer.best.failed) {
      msg.append(" (FAIL)");
    }
    // if ( MaximizerFrame.best.tiebreakered )
    // {
    //	msg = msg + " / " + MaximizerFrame.best.getTiebreaker() + " / " +
    //		MaximizerFrame.best.simplicity;
    // }
    KoLmafia.updateDisplay(msg.toString());
  }
}
