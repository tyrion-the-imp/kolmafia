package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.*;
import net.sourceforge.kolmafia.request.StorageRequest.StorageRequestType;
import net.sourceforge.kolmafia.request.coinmaster.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.coinmaster.BURTRequest;
import net.sourceforge.kolmafia.request.coinmaster.BigBrotherRequest;
import net.sourceforge.kolmafia.request.coinmaster.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.coinmaster.CRIMBCOGiftShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.Crimbo11Request;
import net.sourceforge.kolmafia.request.coinmaster.CrimboCartelRequest;
import net.sourceforge.kolmafia.request.coinmaster.DimemasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.FreeSnackRequest;
import net.sourceforge.kolmafia.request.coinmaster.FudgeWandRequest;
import net.sourceforge.kolmafia.request.coinmaster.GameShoppeRequest;
import net.sourceforge.kolmafia.request.coinmaster.HermitRequest;
import net.sourceforge.kolmafia.request.coinmaster.MrStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.QuartersmasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.SwaggerShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.AlliedHqRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.AppleStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ArmoryAndLeggeryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ArmoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BatFabricatorRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BlackMarketRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BoutiqueRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BrogurtRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.BuffJimmyRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.CanteenRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ChemiCorpRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.CosmicRaysBazaarRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo14Request;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo17Request;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo20BoozeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo20CandyRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo20FoodRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfArmoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfBarRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfCafeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfFactoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23PirateArmoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23PirateBarRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23PirateCafeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23PirateFactoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo24BarRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo24CafeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo24FactoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DedigitizerRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DinostaurRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DinseyCompanyStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DiscoGiftCoRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DollHawkerRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DripArmoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.EdShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FDKOLRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FancyDanRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FishboneryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.FunALogRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GMartRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GeneticFiddlingRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GotporkOrphanageRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GotporkPDRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GuzzlrRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.IsotopeSmitheryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.KiwiKwikiMartRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.LTTRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.LunarLunchRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.MemeShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.MerchTableRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.MrStore2002Request;
import net.sourceforge.kolmafia.request.coinmaster.shop.NeandermallRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.NinjaStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.NuggletCraftingRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PlumberGearRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PlumberItemRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PokemporiumRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PrecinctRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.PrimordialSoupKitchenRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ReplicaMrStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.RubeeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.SHAWARMARequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.SeptEmberCenserRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ShoeRepairRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ShoreGiftShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.SpacegateFabricationRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.SpinMasterLatheRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.TacoDanRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.TerrifiedEagleInnRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ThankShopRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.TicketCounterRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.ToxicChemistryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.TrapperRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.UsingYourShowerThoughtsRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.VendingMachineRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.WalMartRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.WarbearBoxRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.YeNeweSouvenirShoppeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.YourCampfireRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.shop.ShopRow;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemListManagePanel;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmastersFrame extends GenericFrame implements ChangeListener {
  private CardLayoutSelectorPanel selectorPanel = null;

  private CoinmasterPanel BURTPanel = null;
  private CoinmasterPanel CRIMBCOGiftShopPanel = null;
  private CoinmasterPanel SHAWARMAPanel = null;
  private CoinmasterPanel alliedHqPanel = null;
  private CoinmasterPanel altarOfBonesPanel = null;
  private CoinmasterPanel applePanel = null;
  private CoinmasterPanel arcadePanel = null;
  private CoinmasterPanel armoryPanel = null;
  private CoinmasterPanel armoryAndLeggeryPanel = null;
  private CoinmasterPanel awolPanel = null;
  private CoinmasterPanel baconPanel = null;
  private CoinmasterPanel batFabricatorPanel = null;
  private CoinmasterPanel bhhPanel = null;
  private CoinmasterPanel bigBrotherPanel = null;
  private CoinmasterPanel blackMarketPanel = null;
  private CoinmasterPanel boutiquePanel = null;
  private CoinmasterPanel brogurtPanel = null;
  private CoinmasterPanel buffJimmyPanel = null;
  private CoinmasterPanel canteenPanel = null;
  private CoinmasterPanel cashewPanel = null;
  private CoinmasterPanel chemCorpPanel = null;
  private CoinmasterPanel cosmicRaysBazaarPanel = null;
  private CoinmasterPanel crimbo11Panel = null;
  private CoinmasterPanel crimbo14Panel = null;
  private CoinmasterPanel crimbo17Panel = null;
  private CoinmasterPanel crimbo20boozePanel = null;
  private CoinmasterPanel crimbo20candyPanel = null;
  private CoinmasterPanel crimbo20foodPanel = null;
  private CoinmasterPanel crimbo23ElfBarPanel = null;
  private CoinmasterPanel crimbo23ElfCafePanel = null;
  private CoinmasterPanel crimbo23ElfArmoryPanel = null;
  private CoinmasterPanel crimbo23ElfFactoryPanel = null;
  private CoinmasterPanel crimbo23PirateBarPanel = null;
  private CoinmasterPanel crimbo23PirateCafePanel = null;
  private CoinmasterPanel crimbo23PirateArmoryPanel = null;
  private CoinmasterPanel crimbo23PirateFactoryPanel = null;
  private CoinmasterPanel crimbo24BarPanel = null;
  private CoinmasterPanel crimbo24CafePanel = null;
  private CoinmasterPanel crimbo24FactoryPanel = null;
  private CoinmasterPanel crimboCartelPanel = null;
  private CoinmasterPanel dedigitizerPanel = null;
  private CoinmasterPanel dimemasterPanel = null;
  private CoinmasterPanel dinostaurPanel = null;
  private CoinmasterPanel dinseyCompanyStorePanel = null;
  private CoinmasterPanel discoGiftCoPanel = null;
  private CoinmasterPanel dollhawkerPanel = null;
  private CoinmasterPanel dripArmoryPanel = null;
  private CoinmasterPanel edshopPanel = null;
  private CoinmasterPanel fancyDanPanel = null;
  private CoinmasterPanel fdkolPanel = null;
  private CoinmasterPanel fishboneryPanel = null;
  private CoinmasterPanel freeSnackPanel = null;
  private CoinmasterPanel fudgeWandPanel = null;
  private CoinmasterPanel funALogPanel = null;
  private CoinmasterPanel gameShoppePanel = null;
  private CoinmasterPanel geneticFiddlingPanel = null;
  private CoinmasterPanel gmartPanel = null;
  private CoinmasterPanel gotporkOrphanagePanel = null;
  private CoinmasterPanel gotporkPDPanel = null;
  private CoinmasterPanel guzzlrPanel = null;
  private CoinmasterPanel hermitPanel = null;
  private CoinmasterPanel isotopeSmitheryPanel = null;
  private CoinmasterPanel kiwiKwikiMartPanel = null;
  private CoinmasterPanel lttPanel = null;
  private CoinmasterPanel lunarLunchPanel = null;
  private CoinmasterPanel merchTablePanel = null;
  private CoinmasterPanel mrStorePanel = null;
  private CoinmasterPanel mrStore2002Panel = null;
  private CoinmasterPanel neandermallPanel = null;
  private CoinmasterPanel ninjaPanel = null;
  private CoinmasterPanel nuggletcraftingPanel = null;
  private CoinmasterPanel plumberGearPanel = null;
  private CoinmasterPanel plumberItemPanel = null;
  private CoinmasterPanel pokemporiumPanel = null;
  private CoinmasterPanel precinctPanel = null;
  private CoinmasterPanel quartersmasterPanel = null;
  private CoinmasterPanel replicaMrStorePanel = null;
  private CoinmasterPanel rubeePanel = null;
  private CoinmasterPanel septEmberPanel = null;
  private CoinmasterPanel shakeShopPanel = null;
  private CoinmasterPanel shoeRepairPanel = null;
  private CoinmasterPanel shoreGiftShopPanel = null;
  private CoinmasterPanel showerThoughtsPanel = null;
  private CoinmasterPanel spacegateFabricationPanel = null;
  private CoinmasterPanel spinMasterLathePanel = null;
  private CoinmasterPanel swaggerShopPanel = null;
  private CoinmasterPanel tacoDanPanel = null;
  private CoinmasterPanel terrifiedEagleInnPanel = null;
  private CoinmasterPanel toxicChemistryPanel = null;
  private CoinmasterPanel trapperPanel = null;
  private CoinmasterPanel travelerPanel = null;
  private CoinmasterPanel twitchSoupPanel = null;
  private CoinmasterPanel vendingMachinePanel = null;
  private CoinmasterPanel walmartPanel = null;
  private CoinmasterPanel warbearBoxPanel = null;
  private CoinmasterPanel yourCampfirePanel = null;

  public CoinmastersFrame() {
    super("Coin Masters");

    this.selectorPanel =
        new CardLayoutSelectorPanel("coinMasterIndex", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    JPanel panel;

    // Always available coinmasters
    this.selectorPanel.addCategory("Always Available");

    panel = new JPanel(new BorderLayout());
    bhhPanel = new BountyHunterHunterPanel();
    panel.add(bhhPanel);
    this.selectorPanel.addPanel(bhhPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    mrStorePanel = new MrStorePanel();
    panel.add(mrStorePanel);
    this.selectorPanel.addPanel(mrStorePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    armoryAndLeggeryPanel = new ArmoryAndLeggeryPanel();
    panel.add(armoryAndLeggeryPanel);
    this.selectorPanel.addPanel(armoryAndLeggeryPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    blackMarketPanel = new BlackMarketPanel();
    panel.add(blackMarketPanel);
    this.selectorPanel.addPanel(blackMarketPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    hermitPanel = new HermitPanel();
    panel.add(hermitPanel);
    this.selectorPanel.addPanel(hermitPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    shoreGiftShopPanel = new ShoreGiftShopPanel();
    panel.add(shoreGiftShopPanel);
    this.selectorPanel.addPanel(shoreGiftShopPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    trapperPanel = new TrapperPanel();
    panel.add(trapperPanel);
    this.selectorPanel.addPanel(trapperPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    vendingMachinePanel = new VendingMachinePanel();
    panel.add(vendingMachinePanel);
    this.selectorPanel.addPanel(vendingMachinePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    swaggerShopPanel = new SwaggerShopPanel();
    panel.add(swaggerShopPanel);
    this.selectorPanel.addPanel(swaggerShopPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    nuggletcraftingPanel = new NuggletCraftingPanel();
    panel.add(nuggletcraftingPanel);
    this.selectorPanel.addPanel(nuggletcraftingPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    dripArmoryPanel = new DripArmoryPanel();
    panel.add(dripArmoryPanel);
    this.selectorPanel.addPanel(dripArmoryPanel.getPanelSelector(), panel);

    // Ascension coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Ascension");

    panel = new JPanel(new BorderLayout());
    dimemasterPanel = new DimemasterPanel();
    panel.add(dimemasterPanel);
    this.selectorPanel.addPanel(dimemasterPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    quartersmasterPanel = new QuartersmasterPanel();
    panel.add(quartersmasterPanel);
    this.selectorPanel.addPanel(quartersmasterPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    BURTPanel = new BURTPanel();
    panel.add(BURTPanel);
    this.selectorPanel.addPanel(BURTPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    fishboneryPanel = new FishboneryPanel();
    panel.add(fishboneryPanel);
    this.selectorPanel.addPanel(fishboneryPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    edshopPanel = new EdShopPanel();
    panel.add(edshopPanel);
    this.selectorPanel.addPanel(edshopPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    geneticFiddlingPanel = new GeneticFiddlingPanel();
    panel.add(geneticFiddlingPanel);
    this.selectorPanel.addPanel(geneticFiddlingPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    pokemporiumPanel = new PokemporiumPanel();
    panel.add(pokemporiumPanel);
    this.selectorPanel.addPanel(pokemporiumPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    gmartPanel = new GMartPanel();
    panel.add(gmartPanel);
    this.selectorPanel.addPanel(gmartPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    cosmicRaysBazaarPanel = new CosmicRaysBazaarPanel();
    panel.add(cosmicRaysBazaarPanel);
    this.selectorPanel.addPanel(cosmicRaysBazaarPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    plumberGearPanel = new PlumberGearPanel();
    panel.add(plumberGearPanel);
    this.selectorPanel.addPanel(plumberGearPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    plumberItemPanel = new PlumberItemPanel();
    panel.add(plumberItemPanel);
    this.selectorPanel.addPanel(plumberItemPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    dinostaurPanel = new DinostaurPanel();
    panel.add(dinostaurPanel);
    this.selectorPanel.addPanel(dinostaurPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    replicaMrStorePanel = new ReplicaMrStorePanel();
    panel.add(replicaMrStorePanel);
    this.selectorPanel.addPanel(replicaMrStorePanel.getPanelSelector(), panel);

    // Aftercore coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Aftercore");

    panel = new JPanel(new BorderLayout());
    bigBrotherPanel = new BigBrotherPanel();
    panel.add(bigBrotherPanel);
    this.selectorPanel.addPanel(bigBrotherPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    dedigitizerPanel = new DedigitizerPanel();
    panel.add(dedigitizerPanel);
    this.selectorPanel.addPanel(dedigitizerPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    terrifiedEagleInnPanel = new TerrifiedEagleInnPanel();
    panel.add(terrifiedEagleInnPanel);
    this.selectorPanel.addPanel(terrifiedEagleInnPanel.getPanelSelector(), panel);

    // IOTM coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Item of the Month");

    panel = new JPanel(new BorderLayout());
    arcadePanel = new TicketCounterPanel();
    panel.add(arcadePanel);
    this.selectorPanel.addPanel(arcadePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    gameShoppePanel = new GameShoppePanel();
    panel.add(gameShoppePanel);
    this.selectorPanel.addPanel(gameShoppePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    freeSnackPanel = new SnackVoucherPanel();
    panel.add(freeSnackPanel);
    this.selectorPanel.addPanel(freeSnackPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    isotopeSmitheryPanel = new IsotopeSmitheryPanel();
    panel.add(isotopeSmitheryPanel);
    this.selectorPanel.addPanel(isotopeSmitheryPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    dollhawkerPanel = new DollHawkerPanel();
    panel.add(dollhawkerPanel);
    this.selectorPanel.addPanel(dollhawkerPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    lunarLunchPanel = new LunarLunchPanel();
    panel.add(lunarLunchPanel);
    this.selectorPanel.addPanel(lunarLunchPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    boutiquePanel = new BoutiquePanel();
    panel.add(boutiquePanel);
    this.selectorPanel.addPanel(boutiquePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    brogurtPanel = new BrogurtPanel();
    panel.add(brogurtPanel);
    this.selectorPanel.addPanel(brogurtPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    buffJimmyPanel = new BuffJimmyPanel();
    panel.add(buffJimmyPanel);
    this.selectorPanel.addPanel(buffJimmyPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    tacoDanPanel = new TacoDanPanel();
    panel.add(tacoDanPanel);
    this.selectorPanel.addPanel(tacoDanPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    SHAWARMAPanel = new SHAWARMAPanel();
    panel.add(SHAWARMAPanel);
    this.selectorPanel.addPanel(SHAWARMAPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    canteenPanel = new CanteenPanel();
    panel.add(canteenPanel);
    this.selectorPanel.addPanel(canteenPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    armoryPanel = new ArmoryPanel();
    panel.add(armoryPanel);
    this.selectorPanel.addPanel(armoryPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    dinseyCompanyStorePanel = new DinseyCompanyStorePanel();
    panel.add(dinseyCompanyStorePanel);
    this.selectorPanel.addPanel(dinseyCompanyStorePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    toxicChemistryPanel = new ToxicChemistryPanel();
    panel.add(toxicChemistryPanel);
    this.selectorPanel.addPanel(toxicChemistryPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    discoGiftCoPanel = new DiscoGiftCoPanel();
    panel.add(discoGiftCoPanel);
    this.selectorPanel.addPanel(discoGiftCoPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    walmartPanel = new WalmartPanel();
    panel.add(walmartPanel);
    this.selectorPanel.addPanel(walmartPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    batFabricatorPanel = new BatFabricatorPanel();
    panel.add(batFabricatorPanel);
    this.selectorPanel.addPanel(batFabricatorPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    chemCorpPanel = new ChemiCorpPanel();
    panel.add(chemCorpPanel);
    this.selectorPanel.addPanel(chemCorpPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    gotporkOrphanagePanel = new GotporkOrphanagePanel();
    panel.add(gotporkOrphanagePanel);
    this.selectorPanel.addPanel(gotporkOrphanagePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    gotporkPDPanel = new GotporkPDPanel();
    panel.add(gotporkPDPanel);
    this.selectorPanel.addPanel(gotporkPDPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    lttPanel = new LTTPanel();
    panel.add(lttPanel);
    this.selectorPanel.addPanel(lttPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    baconPanel = new BaconPanel();
    panel.add(baconPanel);
    this.selectorPanel.addPanel(baconPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    precinctPanel = new PrecinctPanel();
    panel.add(precinctPanel);
    this.selectorPanel.addPanel(precinctPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    cashewPanel = new CashewPanel();
    panel.add(cashewPanel);
    this.selectorPanel.addPanel(cashewPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    spacegateFabricationPanel = new SpacegateFabricationPanel();
    panel.add(spacegateFabricationPanel);
    this.selectorPanel.addPanel(spacegateFabricationPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    rubeePanel = new RubeePanel();
    panel.add(rubeePanel);
    this.selectorPanel.addPanel(rubeePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    funALogPanel = new FunALogPanel();
    panel.add(funALogPanel);
    this.selectorPanel.addPanel(funALogPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    yourCampfirePanel = new YourCampfirePanel();
    panel.add(yourCampfirePanel);
    this.selectorPanel.addPanel(yourCampfirePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    guzzlrPanel = new GuzzlrPanel();
    panel.add(guzzlrPanel);
    this.selectorPanel.addPanel(guzzlrPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    spinMasterLathePanel = new SpinMasterLathePanel();
    panel.add(spinMasterLathePanel);
    this.selectorPanel.addPanel(spinMasterLathePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    fancyDanPanel = new FancyDanPanel();
    panel.add(fancyDanPanel);
    this.selectorPanel.addPanel(fancyDanPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    mrStore2002Panel = new MrStore2002Panel();
    panel.add(mrStore2002Panel);
    this.selectorPanel.addPanel(mrStore2002Panel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    kiwiKwikiMartPanel = new KiwiKwikiMartPanel();
    panel.add(kiwiKwikiMartPanel);
    this.selectorPanel.addPanel(kiwiKwikiMartPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    septEmberPanel = new SeptEmberPanel();
    panel.add(septEmberPanel);
    this.selectorPanel.addPanel(septEmberPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    showerThoughtsPanel = new ShowerThoughtsPanel();
    panel.add(showerThoughtsPanel);
    this.selectorPanel.addPanel(showerThoughtsPanel.getPanelSelector(), panel);

    // Twitch coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Twitch");

    panel = new JPanel(new BorderLayout());
    neandermallPanel = new NeandermallPanel();
    panel.add(neandermallPanel);
    this.selectorPanel.addPanel(neandermallPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    shoeRepairPanel = new ShoeRepairPanel();
    panel.add(shoeRepairPanel);
    this.selectorPanel.addPanel(shoeRepairPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    applePanel = new ApplePanel();
    panel.add(applePanel);
    this.selectorPanel.addPanel(applePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    ninjaPanel = new NinjaPanel();
    panel.add(ninjaPanel);
    this.selectorPanel.addPanel(ninjaPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    shakeShopPanel = new ShakeShopPanel();
    panel.add(shakeShopPanel);
    this.selectorPanel.addPanel(shakeShopPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    merchTablePanel = new MerchTablePanel();
    panel.add(merchTablePanel);
    this.selectorPanel.addPanel(merchTablePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    twitchSoupPanel = new TwitchSoupPanel();
    panel.add(twitchSoupPanel);
    this.selectorPanel.addPanel(twitchSoupPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    alliedHqPanel = new AlliedHqPanel();
    panel.add(alliedHqPanel);
    this.selectorPanel.addPanel(alliedHqPanel.getPanelSelector(), panel);

    // Events coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Special Events");

    panel = new JPanel(new BorderLayout());
    awolPanel = new CommendationPanel();
    panel.add(awolPanel);
    this.selectorPanel.addPanel(awolPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    fudgeWandPanel = new FudgeWandPanel();
    panel.add(fudgeWandPanel);
    this.selectorPanel.addPanel(fudgeWandPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    travelerPanel = new TravelingTraderPanel();
    panel.add(travelerPanel);
    this.selectorPanel.addPanel(travelerPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    fdkolPanel = new fdkolPanel();
    panel.add(fdkolPanel);
    this.selectorPanel.addPanel(fdkolPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    warbearBoxPanel = new WarbearBoxPanel();
    panel.add(warbearBoxPanel);
    this.selectorPanel.addPanel(warbearBoxPanel.getPanelSelector(), panel);

    // Removed coinmasters
    this.selectorPanel.addSeparator();
    this.selectorPanel.addCategory("Removed");

    panel = new JPanel(new BorderLayout());
    altarOfBonesPanel = new AltarOfBonesPanel();
    panel.add(altarOfBonesPanel);
    this.selectorPanel.addPanel(altarOfBonesPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimboCartelPanel = new CrimboCartelPanel();
    panel.add(crimboCartelPanel);
    this.selectorPanel.addPanel(crimboCartelPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    CRIMBCOGiftShopPanel = new CRIMBCOGiftShopPanel();
    panel.add(CRIMBCOGiftShopPanel);
    this.selectorPanel.addPanel(CRIMBCOGiftShopPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo11Panel = new Crimbo11Panel();
    panel.add(crimbo11Panel);
    this.selectorPanel.addPanel(crimbo11Panel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo14Panel = new Crimbo14Panel();
    panel.add(crimbo14Panel);
    this.selectorPanel.addPanel(crimbo14Panel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo17Panel = new Crimbo17Panel();
    panel.add(crimbo17Panel);
    this.selectorPanel.addPanel(crimbo17Panel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo20boozePanel = new Crimbo20BoozePanel();
    panel.add(crimbo20boozePanel);
    this.selectorPanel.addPanel(crimbo20boozePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo20candyPanel = new Crimbo20CandyPanel();
    panel.add(crimbo20candyPanel);
    this.selectorPanel.addPanel(crimbo20candyPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo20foodPanel = new Crimbo20FoodPanel();
    panel.add(crimbo20foodPanel);
    this.selectorPanel.addPanel(crimbo20foodPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo23ElfBarPanel = new Crimbo23ElfBarPanel();
    panel.add(crimbo23ElfBarPanel);
    this.selectorPanel.addPanel(crimbo23ElfBarPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo23ElfCafePanel = new Crimbo23ElfCafePanel();
    panel.add(crimbo23ElfCafePanel);
    this.selectorPanel.addPanel(crimbo23ElfCafePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo23ElfArmoryPanel = new Crimbo23ElfArmoryPanel();
    panel.add(crimbo23ElfArmoryPanel);
    this.selectorPanel.addPanel(crimbo23ElfArmoryPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo23ElfFactoryPanel = new Crimbo23ElfFactoryPanel();
    panel.add(crimbo23ElfFactoryPanel);
    this.selectorPanel.addPanel(crimbo23ElfFactoryPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo23PirateBarPanel = new Crimbo23PirateBarPanel();
    panel.add(crimbo23PirateBarPanel);
    this.selectorPanel.addPanel(crimbo23PirateBarPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo23PirateCafePanel = new Crimbo23PirateCafePanel();
    panel.add(crimbo23PirateCafePanel);
    this.selectorPanel.addPanel(crimbo23PirateCafePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo23PirateArmoryPanel = new Crimbo23PirateArmoryPanel();
    panel.add(crimbo23PirateArmoryPanel);
    this.selectorPanel.addPanel(crimbo23PirateArmoryPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo23PirateFactoryPanel = new Crimbo23PirateFactoryPanel();
    panel.add(crimbo23PirateFactoryPanel);
    this.selectorPanel.addPanel(crimbo23PirateFactoryPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo24BarPanel = new Crimbo24BarPanel();
    panel.add(crimbo24BarPanel);
    this.selectorPanel.addPanel(crimbo24BarPanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo24CafePanel = new Crimbo24CafePanel();
    panel.add(crimbo24CafePanel);
    this.selectorPanel.addPanel(crimbo24CafePanel.getPanelSelector(), panel);

    panel = new JPanel(new BorderLayout());
    crimbo24FactoryPanel = new Crimbo24FactoryPanel();
    panel.add(crimbo24FactoryPanel);
    this.selectorPanel.addPanel(crimbo24FactoryPanel.getPanelSelector(), panel);

    this.selectorPanel.addChangeListener(this);
    this.selectorPanel.setSelectedIndex(Preferences.getInteger("coinMasterIndex"));

    JPanel wrapperPanel = new JPanel(new BorderLayout());
    wrapperPanel.add(this.selectorPanel, BorderLayout.CENTER);
    wrapperPanel.add(new StatusPanel(), BorderLayout.SOUTH);

    this.setCenterComponent(wrapperPanel);
  }

  private CoinmasterPanel currentPanel() {
    JComponent panel = this.selectorPanel.currentPanel();
    Component cm = (panel instanceof JPanel) ? panel.getComponent(0) : null;
    return (cm instanceof CoinmasterPanel) ? ((CoinmasterPanel) cm) : null;
  }

  /**
   * Whenever the tab changes, this method is used to change the title to count the coins of the new
   * tab
   */
  @Override
  public void stateChanged(final ChangeEvent e) {
    CoinmasterPanel current = this.currentPanel();
    if (current != null) {
      current.setTitle();
    }
  }

  private class DimemasterPanel extends WarMasterPanel {
    public DimemasterPanel() {
      super(DimemasterRequest.HIPPY);
    }
  }

  private class QuartersmasterPanel extends WarMasterPanel {
    public QuartersmasterPanel() {
      super(QuartersmasterRequest.FRATBOY);
    }
  }

  private class BountyHunterHunterPanel extends CoinmasterPanel {
    public BountyHunterHunterPanel() {
      super(BountyHunterHunterRequest.BHH);
    }
  }

  public class MrStorePanel extends CoinmasterPanel {
    private static final StorageRequest PULL_MR_A_REQUEST =
        new StorageRequest(
            StorageRequestType.STORAGE_TO_INVENTORY, new AdventureResult[] {MrStoreRequest.MR_A});
    private static final StorageRequest PULL_UNCLE_B_REQUEST =
        new StorageRequest(
            StorageRequestType.STORAGE_TO_INVENTORY,
            new AdventureResult[] {MrStoreRequest.UNCLE_B});

    private final JButton pullA = new InvocationButton("pull Mr. A", this, "pullA");
    private final JButton pullB = new InvocationButton("pull Uncle B", this, "pullB");
    private final JButton AToB = new InvocationButton("1 A -> 10 B", this, "AToB");
    private final JButton BToA = new InvocationButton("10 B -> 1 A", this, "BToA");
    private int ACountStorage = 0;
    private int BCountStorage = 0;
    private int ACount = 0;
    private int BCount = 0;

    public MrStorePanel() {
      super(MrStoreRequest.MR_STORE);
      this.buyPanel.addButton(pullA, false);
      this.buyPanel.addButton(pullB, false);
      this.buyPanel.addButton(AToB, false);
      this.buyPanel.addButton(BToA, false);
      this.storageInTitle = true;
      this.setPullsInTitle();
      this.update();
    }

    @Override
    public final void update() {
      this.ACount = MrStoreRequest.MR_A.getCount(KoLConstants.inventory);
      this.BCount = MrStoreRequest.UNCLE_B.getCount(KoLConstants.inventory);
      this.ACountStorage = MrStoreRequest.MR_A.getCount(KoLConstants.storage);
      this.BCountStorage = MrStoreRequest.UNCLE_B.getCount(KoLConstants.storage);
      boolean canPull = KoLCharacter.isHardcore() || ConcoctionDatabase.getPullsRemaining() != 0;
      this.pullA.setEnabled(canPull && this.ACountStorage > 0);
      this.pullB.setEnabled(canPull && this.BCountStorage > 0);
      this.AToB.setEnabled(this.ACount > 0);
      this.BToA.setEnabled(this.BCount >= 10);
      super.update();
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      buffer.append(" (");
      buffer.append(this.BCount);
      buffer.append(" ");
      buffer.append("Uncle B");
      if (this.BCount != 1) {
        buffer.append("s");
      }
      buffer.append(", ");
      buffer.append(this.BCountStorage);
      buffer.append(" in storage");
      buffer.append(")");
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);
      this.pullA.setEnabled(isEnabled && this.ACountStorage > 0);
      this.pullB.setEnabled(isEnabled && this.BCountStorage > 0);
      this.AToB.setEnabled(isEnabled && this.ACount > 0);
      this.BToA.setEnabled(isEnabled && this.BCount >= 10);
    }

    public void pullA() {
      GenericRequest request =
          KoLCharacter.isHardcore() ? new MrStoreRequest("pullmras") : PULL_MR_A_REQUEST;
      RequestThread.postRequest(request);
    }

    public void pullB() {
      GenericRequest request =
          KoLCharacter.isHardcore() ? new MrStoreRequest("pullunclebs") : PULL_UNCLE_B_REQUEST;
      RequestThread.postRequest(request);
    }

    public void AToB() {
      RequestThread.postRequest(new MrStoreRequest("a_to_b"));
    }

    public void BToA() {
      RequestThread.postRequest(new MrStoreRequest("b_to_a"));
    }
  }

  public class ArmoryAndLeggeryPanel extends CoinmasterPanel {
    public ArmoryAndLeggeryPanel() {
      super(ArmoryAndLeggeryRequest.ARMORY_AND_LEGGERY);
      NamedListenerRegistry.registerNamedListener("(armoryandleggery)", this);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        // There are two currencies for every year of Standard equipment.
        // That is far too many to show all of them in the title.
        // Show only the ones you have in inventory right now.
        if (InventoryManager.getCount(currency.getItemId()) > 0) {
          buffer.append(" (");
          buffer.append(InventoryManager.getCount(currency));
          buffer.append(" ");
          buffer.append(currency.getName());
          buffer.append(")");
        }
      }
    }
  }

  public class BlackMarketPanel extends CoinmasterPanel {
    public BlackMarketPanel() {
      super(BlackMarketRequest.BLACK_MARKET);
    }
  }

  public class HermitPanel extends CoinmasterPanel {
    private final JButton fish = new InvocationButton("go fish", this, "fish");

    public HermitPanel() {
      super(HermitRequest.HERMIT);
      this.buyPanel.addButton(fish, true);
    }

    public void fish() {
      int available = HermitRequest.getWorthlessItemCount();
      AdventureResult item = HermitRequest.WORTHLESS_ITEM.getInstance(available + 1);
      InventoryManager.retrieveItem(item, false);
    }
  }

  public class TrapperPanel extends CoinmasterPanel {
    public TrapperPanel() {
      super(TrapperRequest.TRAPPER);
    }
  }

  public class SwaggerShopPanel extends CoinmasterPanel {
    public SwaggerShopPanel() {
      super(SwaggerShopRequest.SWAGGER_SHOP);
      PreferenceListenerRegistry.registerPreferenceListener("blackBartsBootyAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("holidayHalsBookAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener(
          "antagonisticSnowmanKitAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("mapToKokomoAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("essenceOfBearAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("manualOfNumberologyAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("ROMOfOptimalityAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener(
          "schoolOfHardKnocksDiplomaAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("guideToSafariAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("glitchItemAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("lawOfAveragesAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("universalSeasoningAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("bookOfIronyAvailable", this);
      PreferenceListenerRegistry.registerPreferenceListener("essenceOfAnnoyanceAvailable", this);
    }
  }

  public class BURTPanel extends CoinmasterPanel {
    public BURTPanel() {
      super(BURTRequest.BURT);
    }
  }

  public class FishboneryPanel extends CoinmasterPanel {
    public FishboneryPanel() {
      super(FishboneryRequest.FISHBONERY);
    }
  }

  public class EdShopPanel extends CoinmasterPanel {
    public EdShopPanel() {
      super(EdShopRequest.EDSHOP);
    }
  }

  public class NuggletCraftingPanel extends CoinmasterPanel {
    public NuggletCraftingPanel() {
      super(NuggletCraftingRequest.NUGGLETCRAFTING);
    }
  }

  private abstract class TwitchPanel extends CoinmasterPanel {
    public TwitchPanel(CoinmasterData data) {
      super(data);
      PreferenceListenerRegistry.registerPreferenceListener("timeTowerAvailable", this);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.setEnabled(Preferences.getBoolean("timeTowerAvailable"));
    }
  }

  public class NeandermallPanel extends TwitchPanel {
    public NeandermallPanel() {
      super(NeandermallRequest.NEANDERMALL);
    }
  }

  public class ShoeRepairPanel extends TwitchPanel {
    public ShoeRepairPanel() {
      super(ShoeRepairRequest.SHOE_REPAIR);
    }
  }

  public class ApplePanel extends TwitchPanel {
    public ApplePanel() {
      super(AppleStoreRequest.APPLE_STORE);
    }
  }

  public class NinjaPanel extends TwitchPanel {
    public NinjaPanel() {
      super(NinjaStoreRequest.NINJA_STORE);
    }
  }

  public class ShakeShopPanel extends TwitchPanel {
    public ShakeShopPanel() {
      super(YeNeweSouvenirShoppeRequest.SHAKE_SHOP);
    }
  }

  public class MerchTablePanel extends TwitchPanel {
    public MerchTablePanel() {
      super(MerchTableRequest.MERCH_TABLE);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      buffer.append(" (");
      buffer.append(InventoryManager.getCount(MerchTableRequest.CHRONER));
      buffer.append(" Chroner)");
    }
  }

  public class TwitchSoupPanel extends TwitchPanel {
    public TwitchSoupPanel() {
      super(PrimordialSoupKitchenRequest.DATA);
    }

    static List<Map.Entry<String, String>> currencies = new ArrayList<>();

    static {
      currencies.add(Map.entry("Chroner", "Chroner"));
      currencies.add(Map.entry("bacteria bisque", "bisque"));
      currencies.add(Map.entry("ciliophora chowder", "chowder"));
      currencies.add(Map.entry("cream of chloroplasts", "cream"));
      currencies.add(Map.entry("protogenetic chunklet (elbow)", "elbow"));
      currencies.add(Map.entry("protogenetic chunklet (flagellum)", "flagellum"));
      currencies.add(Map.entry("protogenetic chunklet (lips)", "lips"));
      currencies.add(Map.entry("protogenetic chunklet (muscle)", "muscle"));
      currencies.add(Map.entry("protogenetic chunklet (synapse)", "synapse"));
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (var entry : currencies) {
        int itemId = ItemDatabase.getItemId(entry.getKey());
        int count = InventoryManager.getCount(itemId);
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(entry.getValue());
        buffer.append(")");
      }
    }
  }

  public class AlliedHqPanel extends TwitchPanel {
    public AlliedHqPanel() {
      super(AlliedHqRequest.DATA);
    }
  }

  public class ShoreGiftShopPanel extends CoinmasterPanel {
    public ShoreGiftShopPanel() {
      super(ShoreGiftShopRequest.SHORE_GIFT_SHOP);
      PreferenceListenerRegistry.registerPreferenceListener("itemBoughtPerAscension637", this);
    }
  }

  public class SpacegateFabricationPanel extends CoinmasterPanel {
    public SpacegateFabricationPanel() {
      super(SpacegateFabricationRequest.SPACEGATE_STORE);
    }
  }

  public class VendingMachinePanel extends CoinmasterPanel {
    public VendingMachinePanel() {
      super(VendingMachineRequest.VENDING_MACHINE);
    }
  }

  private class BigBrotherPanel extends CoinmasterPanel {
    public BigBrotherPanel() {
      super(BigBrotherRequest.BIG_BROTHER);
    }
  }

  private class DedigitizerPanel extends CoinmasterPanel {
    private static AdventureResult ONE = ItemPool.get(ItemPool.ONE);
    private static AdventureResult ZERO = ItemPool.get(ItemPool.ZERO);

    public DedigitizerPanel() {
      super(DedigitizerRequest.DATA);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);

      // Only show 0's and 1's. All but 5 also require a schematic,
      // but there are 22 of them and the title will be cluttered. Rows
      // will be greyed out if you don't have the required schematic

      int count1 = InventoryManager.getCount(ONE);
      buffer.append(" (");
      buffer.append(count1);
      buffer.append(" ");
      buffer.append(ONE.getPluralName(count1));
      buffer.append(")");
      int count0 = InventoryManager.getCount(ZERO);
      buffer.append(" (");
      buffer.append(count0);
      buffer.append(" ");
      buffer.append(ZERO.getPluralName(count0));
      buffer.append(")");
    }
  }

  private class Crimbo11Panel extends CoinmasterPanel {
    public Crimbo11Panel() {
      super();

      this.setData(Crimbo11Request.CRIMBO11);

      this.sellPanel = new SellPanel();
      this.add(this.sellPanel, BorderLayout.NORTH);

      ActionListener[] listeners = new ActionListener[2];
      listeners[0] = new GiftListener();
      listeners[1] = new DonateListener();

      this.buyPanel = new BuyPanel(listeners);
      this.add(this.buyPanel, BorderLayout.CENTER);
    }

    public AdventureResult[] getDesiredItems() {
      AdventureResult[] items = this.buyPanel.getSelectedValues().toArray(new AdventureResult[0]);
      return this.getDesiredBuyItems(items, false);
    }

    public class GiftListener extends ThreadedListener {
      @Override
      protected void execute() {
        CoinmasterData data = Crimbo11Panel.this.data;
        String reason = data.canBuy();
        if (reason != null) {
          KoLmafia.updateDisplay(MafiaState.ERROR, reason);
          return;
        }

        AdventureResult[] items = Crimbo11Panel.this.getDesiredItems();
        if (items == null) {
          return;
        }

        String victim = InputFieldUtilities.input("Send a gift to whom?");
        if (victim == null) {
          return;
        }

        Crimbo11Panel.this.execute(true, items, "towho=" + victim);
      }

      @Override
      public String toString() {
        return "gift";
      }
    }

    public class DonateListener extends ThreadedListener {
      @Override
      protected void execute() {
        CoinmasterData data = Crimbo11Panel.this.data;
        String reason = data.canBuy();
        if (reason != null) {
          KoLmafia.updateDisplay(MafiaState.ERROR, reason);
          return;
        }

        AdventureResult[] items = Crimbo11Panel.this.getDesiredItems();
        if (items == null) {
          return;
        }

        Crimbo11Panel.this.execute(true, items, "towho=0");
      }

      @Override
      public String toString() {
        return "donate";
      }
    }
  }

  private class CrimboCartelPanel extends CoinmasterPanel {
    public CrimboCartelPanel() {
      super(CrimboCartelRequest.CRIMBO_CARTEL);
    }
  }

  private class Crimbo14Panel extends CoinmasterPanel {
    public Crimbo14Panel() {
      super();

      this.setData(Crimbo14Request.CRIMBO14);

      this.sellPanel = new SellPanel();
      this.add(this.sellPanel, BorderLayout.NORTH);
      this.buyPanel = new BuyPanel();
      this.add(this.buyPanel, BorderLayout.CENTER);
    }
  }

  private class Crimbo17Panel extends CoinmasterPanel {
    public Crimbo17Panel() {
      super(Crimbo17Request.CRIMBO17);
    }
  }

  private class Crimbo20BoozePanel extends CoinmasterPanel {
    public Crimbo20BoozePanel() {
      super(Crimbo20BoozeRequest.CRIMBO20BOOZE);
    }
  }

  private class Crimbo20CandyPanel extends CoinmasterPanel {
    public Crimbo20CandyPanel() {
      super(Crimbo20CandyRequest.CRIMBO20CANDY);
    }
  }

  private class Crimbo20FoodPanel extends CoinmasterPanel {
    public Crimbo20FoodPanel() {
      super(Crimbo20FoodRequest.CRIMBO20FOOD);
    }
  }

  private class PokemporiumPanel extends CoinmasterPanel {
    public PokemporiumPanel() {
      super(PokemporiumRequest.POKEMPORIUM);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      String title = buffer.toString();
      buffer.setLength(0);
      buffer.append(StringUtilities.getEntityDecode(title));
    }
  }

  public class TicketCounterPanel extends CoinmasterPanel {
    private final JButton skeeball = new InvocationButton("skeeball", this, "skeeball");
    private int gameGridTokens = 0;

    public TicketCounterPanel() {
      super(TicketCounterRequest.TICKET_COUNTER);
      this.buyPanel.addButton(skeeball, false);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.gameGridTokens = ArcadeRequest.TOKEN.getCount(KoLConstants.inventory);
      this.skeeball.setEnabled(this.gameGridTokens > 0);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);
      this.skeeball.setEnabled(isEnabled && this.gameGridTokens > 0);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      buffer.append(" (");
      buffer.append(this.gameGridTokens);
      buffer.append(" Game Grid tokens)");
    }

    public void skeeball() {
      RequestThread.postRequest(new ArcadeRequest("arcade_skeeball"));
    }
  }

  private class GameShoppePanel extends CoinmasterPanel {
    public GameShoppePanel() {
      super(GameShoppeRequest.GAMESHOPPE);
    }
  }

  private class SnackVoucherPanel extends CoinmasterPanel {
    public SnackVoucherPanel() {
      super(FreeSnackRequest.FREESNACKS);
    }
  }

  private class AltarOfBonesPanel extends CoinmasterPanel {
    public AltarOfBonesPanel() {
      super(AltarOfBonesRequest.ALTAR_OF_BONES);
    }
  }

  private class CRIMBCOGiftShopPanel extends CoinmasterPanel {
    public CRIMBCOGiftShopPanel() {
      super(CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP);
    }
  }

  public class DripArmoryPanel extends CoinmasterPanel {
    public DripArmoryPanel() {
      super(DripArmoryRequest.DRIP_ARMORY);
    }
  }

  private class CommendationPanel extends CoinmasterPanel {
    public CommendationPanel() {
      super(AWOLQuartermasterRequest.AWOL);
    }
  }

  private class FudgeWandPanel extends CoinmasterPanel {
    public FudgeWandPanel() {
      super(FudgeWandRequest.FUDGEWAND);
    }
  }

  private class TravelingTraderPanel extends CoinmasterPanel {
    public TravelingTraderPanel() {
      super(TravelingTraderRequest.TRAVELER);
    }
  }

  private class fdkolPanel extends CoinmasterPanel {
    public fdkolPanel() {
      super(FDKOLRequest.FDKOL);
    }
  }

  private class IsotopeSmitheryPanel extends CoinmasterPanel {
    public IsotopeSmitheryPanel() {
      super(IsotopeSmitheryRequest.ISOTOPE_SMITHERY);
    }

    @Override
    public boolean enabled() {
      return SpaaaceRequest.immediatelyAccessible();
    }
  }

  private class DollHawkerPanel extends CoinmasterPanel {
    public DollHawkerPanel() {
      super(DollHawkerRequest.DOLLHAWKER);
    }

    @Override
    public boolean enabled() {
      return SpaaaceRequest.immediatelyAccessible();
    }
  }

  private class LunarLunchPanel extends CoinmasterPanel {
    public LunarLunchPanel() {
      super(LunarLunchRequest.LUNAR_LUNCH);
    }

    @Override
    public boolean enabled() {
      return SpaaaceRequest.immediatelyAccessible();
    }
  }

  private class BrogurtPanel extends CoinmasterPanel {
    public BrogurtPanel() {
      super(BrogurtRequest.BROGURT);
    }
  }

  private class BuffJimmyPanel extends CoinmasterPanel {
    public BuffJimmyPanel() {
      super(BuffJimmyRequest.BUFF_JIMMY);
    }
  }

  private class TacoDanPanel extends CoinmasterPanel {
    public TacoDanPanel() {
      super(TacoDanRequest.TACO_DAN);
    }
  }

  private class SHAWARMAPanel extends CoinmasterPanel {
    public SHAWARMAPanel() {
      super(SHAWARMARequest.SHAWARMA);
    }
  }

  private class CanteenPanel extends CoinmasterPanel {
    public CanteenPanel() {
      super(CanteenRequest.CANTEEN);
    }
  }

  private class ArmoryPanel extends CoinmasterPanel {
    public ArmoryPanel() {
      super(ArmoryRequest.ARMORY);
    }
  }

  private class DinseyCompanyStorePanel extends CoinmasterPanel {
    public DinseyCompanyStorePanel() {
      super(DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      String title = buffer.toString();
      buffer.setLength(0);
      buffer.append(StringUtilities.getEntityDecode(title));
    }
  }

  private class DiscoGiftCoPanel extends CoinmasterPanel {
    public DiscoGiftCoPanel() {
      super(DiscoGiftCoRequest.DISCO_GIFTCO);
    }
  }

  private class WalmartPanel extends CoinmasterPanel {
    public WalmartPanel() {
      super(WalMartRequest.WALMART);
    }
  }

  private class BatFabricatorPanel extends BatFellowPanel {
    public BatFabricatorPanel() {
      super(BatFabricatorRequest.BAT_FABRICATOR);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        buffer.append(" (");
        buffer.append(InventoryManager.getCount(currency));
        buffer.append(" ");
        buffer.append(currency.getName());
        buffer.append(")");
      }
    }
  }

  private class ChemiCorpPanel extends BatFellowPanel {
    public ChemiCorpPanel() {
      super(ChemiCorpRequest.CHEMICORP);
    }
  }

  private class GotporkOrphanagePanel extends BatFellowPanel {
    public GotporkOrphanagePanel() {
      super(GotporkOrphanageRequest.GOTPORK_ORPHANAGE);
    }
  }

  private class GotporkPDPanel extends BatFellowPanel {
    public GotporkPDPanel() {
      super(GotporkPDRequest.GOTPORK_PD);
    }
  }

  private class LTTPanel extends CoinmasterPanel {
    public LTTPanel() {
      super(LTTRequest.LTT);
    }
  }

  private class PrecinctPanel extends CoinmasterPanel {
    public PrecinctPanel() {
      super(PrecinctRequest.PRECINCT);
    }
  }

  private class RubeePanel extends CoinmasterPanel {
    public RubeePanel() {
      super(RubeeRequest.RUBEE);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      String title = buffer.toString();
      buffer.setLength(0);
      buffer.append(StringUtilities.getEntityDecode(title));
    }
  }

  private class FunALogPanel extends CoinmasterPanel {
    public FunALogPanel() {
      super(FunALogRequest.FUN_A_LOG);
    }
  }

  private class YourCampfirePanel extends CoinmasterPanel {
    public YourCampfirePanel() {
      super(YourCampfireRequest.YOUR_CAMPFIRE);
    }
  }

  private class GuzzlrPanel extends CoinmasterPanel {
    public GuzzlrPanel() {
      super(GuzzlrRequest.GUZZLR);
    }
  }

  private class SpinMasterLathePanel extends CoinmasterPanel {
    public SpinMasterLathePanel() {
      super(SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        buffer.append(" (");
        buffer.append(InventoryManager.getCount(currency));
        buffer.append(" ");
        buffer.append(currency.getName());
        buffer.append(")");
      }
    }
  }

  public class ToxicChemistryPanel extends CoinmasterPanel {
    public ToxicChemistryPanel() {
      super(ToxicChemistryRequest.TOXIC_CHEMISTRY);
    }
  }

  private class TerrifiedEagleInnPanel extends CoinmasterPanel {
    public TerrifiedEagleInnPanel() {
      super(TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN);
      PreferenceListenerRegistry.registerPreferenceListener("itemBoughtPerCharacter6423", this);
      PreferenceListenerRegistry.registerPreferenceListener("itemBoughtPerCharacter6428", this);
      PreferenceListenerRegistry.registerPreferenceListener("itemBoughtPerCharacter6429", this);
    }

    @Override
    public int buyMax(final AdventureResult item, final int max) {
      return switch (item.getItemId()) {
        case ItemPool.TALES_OF_DREAD, ItemPool.BRASS_DREAD_FLASK, ItemPool.SILVER_DREAD_FLASK -> 1;
        default -> max;
      };
    }
  }

  private class WarbearBoxPanel extends CoinmasterPanel {
    public WarbearBoxPanel() {
      super(WarbearBoxRequest.WARBEARBOX);
    }
  }

  private class BoutiquePanel extends CoinmasterPanel {
    public BoutiquePanel() {
      super(BoutiqueRequest.BOUTIQUE);
    }
  }

  private class BaconPanel extends CoinmasterPanel {
    public BaconPanel() {
      super(MemeShopRequest.BACON_STORE);
      PreferenceListenerRegistry.registerPreferenceListener("_internetViralVideoBought", this);
      PreferenceListenerRegistry.registerPreferenceListener("_internetPlusOneBought", this);
      PreferenceListenerRegistry.registerPreferenceListener("_internetGallonOfMilkBought", this);
      PreferenceListenerRegistry.registerPreferenceListener(
          "_internetPrintScreenButtonBought", this);
      PreferenceListenerRegistry.registerPreferenceListener(
          "_internetDailyDungeonMalwareBought", this);
    }

    @Override
    public int buyMax(final AdventureResult item, final int max) {
      return switch (item.getItemId()) {
        case ItemPool.VIRAL_VIDEO,
            ItemPool.PLUS_ONE,
            ItemPool.GALLON_OF_MILK,
            ItemPool.PRINT_SCREEN,
            ItemPool.DAILY_DUNGEON_MALWARE -> 1;
        default -> max;
      };
    }
  }

  private class CashewPanel extends CoinmasterPanel {
    public CashewPanel() {
      super(ThankShopRequest.CASHEW_STORE);
    }
  }

  private abstract class BatFellowPanel extends CoinmasterPanel {
    public BatFellowPanel(CoinmasterData data) {
      super(data);
      NamedListenerRegistry.registerNamedListener("(batfellow)", this);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.setEnabled(this.data.isAccessible());
    }

    @Override
    public int buyDefault(final int max) {
      return max;
    }
  }

  private abstract class WarMasterPanel extends CoinmasterPanel {
    public WarMasterPanel(CoinmasterData data) {
      super(data);
      this.buyPanel.filterItems();
      NamedListenerRegistry.registerNamedListener("(outfit)", this);
      PreferenceListenerRegistry.registerPreferenceListener("warProgress", this);
      PreferenceListenerRegistry.registerPreferenceListener("sidequestLighthouseCompleted", this);
    }

    @Override
    public int buyDefault(final int max) {
      return max;
    }
  }

  private class GeneticFiddlingPanel extends CoinmasterPanel {
    public GeneticFiddlingPanel() {
      super(GeneticFiddlingRequest.DATA);
    }

    @Override
    public int buyMax(final AdventureResult item, final int max) {
      return 1;
    }
  }

  private class GMartPanel extends CoinmasterPanel {
    public GMartPanel() {
      super(GMartRequest.GMART);
    }
  }

  private class PlumberGearPanel extends CoinmasterPanel {
    public PlumberGearPanel() {
      super(PlumberGearRequest.PLUMBER_GEAR);
    }
  }

  private class PlumberItemPanel extends CoinmasterPanel {
    public PlumberItemPanel() {
      super(PlumberItemRequest.PLUMBER_ITEMS);
    }
  }

  private class CosmicRaysBazaarPanel extends CoinmasterPanel {
    public CosmicRaysBazaarPanel() {
      super(CosmicRaysBazaarRequest.COSMIC_RAYS_BAZAAR);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.setEnabled(this.data.isAccessible());
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        int count =
            currency.isMeat() ? Concoction.getAvailableMeat() : InventoryManager.getCount(currency);
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(currency.getPluralName(count));
        buffer.append(")");
      }
    }
  }

  private class FancyDanPanel extends CoinmasterPanel {
    public FancyDanPanel() {
      super(FancyDanRequest.FANCY_DAN);
      this.update();
    }

    @Override
    public final void update() {
      super.update();
      this.setEnabled(this.data.isAccessible());
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        int count = InventoryManager.getCount(currency);
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(currency.getPluralName(count));
        buffer.append(")");
      }
    }
  }

  private class DinostaurPanel extends CoinmasterPanel {
    public DinostaurPanel() {
      super(DinostaurRequest.DINOSTAUR);
    }
  }

  private class ReplicaMrStorePanel extends CoinmasterPanel {
    public ReplicaMrStorePanel() {
      super(ReplicaMrStoreRequest.REPLICA_MR_STORE);
    }
  }

  private class MrStore2002Panel extends CoinmasterPanel {
    public MrStore2002Panel() {
      super(MrStore2002Request.MR_STORE_2002);
    }
  }

  private class Crimbo23ElfBarPanel extends CoinmasterPanel {
    public Crimbo23ElfBarPanel() {
      super(Crimbo23ElfBarRequest.DATA);
    }
  }

  private class Crimbo23ElfCafePanel extends CoinmasterPanel {
    public Crimbo23ElfCafePanel() {
      super(Crimbo23ElfCafeRequest.DATA);
    }
  }

  private class Crimbo23ElfArmoryPanel extends CoinmasterPanel {
    public Crimbo23ElfArmoryPanel() {
      super(Crimbo23ElfArmoryRequest.DATA);
    }
  }

  private class Crimbo23ElfFactoryPanel extends CoinmasterPanel {
    public Crimbo23ElfFactoryPanel() {
      super(Crimbo23ElfFactoryRequest.DATA);
    }
  }

  private class Crimbo23PirateBarPanel extends CoinmasterPanel {
    public Crimbo23PirateBarPanel() {
      super(Crimbo23PirateBarRequest.DATA);
    }
  }

  private class Crimbo23PirateCafePanel extends CoinmasterPanel {
    public Crimbo23PirateCafePanel() {
      super(Crimbo23PirateCafeRequest.DATA);
    }
  }

  private class Crimbo23PirateArmoryPanel extends CoinmasterPanel {
    public Crimbo23PirateArmoryPanel() {
      super(Crimbo23PirateArmoryRequest.DATA);
    }
  }

  private class Crimbo23PirateFactoryPanel extends CoinmasterPanel {
    public Crimbo23PirateFactoryPanel() {
      super(Crimbo23PirateFactoryRequest.DATA);
    }
  }

  private class Crimbo24Panel extends CoinmasterPanel {
    public Crimbo24Panel(CoinmasterData data) {
      super(data);
    }

    @Override
    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
      for (AdventureResult currency : this.data.currencies()) {
        int count = InventoryManager.getCount(currency);
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(currency.getPluralName(count));
        buffer.append(")");
      }
    }
  }

  private class Crimbo24BarPanel extends Crimbo24Panel {
    public Crimbo24BarPanel() {
      super(Crimbo24BarRequest.DATA);
    }
  }

  private class Crimbo24CafePanel extends Crimbo24Panel {
    public Crimbo24CafePanel() {
      super(Crimbo24CafeRequest.DATA);
    }
  }

  private class Crimbo24FactoryPanel extends Crimbo24Panel {
    public Crimbo24FactoryPanel() {
      super(Crimbo24FactoryRequest.DATA);
    }
  }

  private class KiwiKwikiMartPanel extends CoinmasterPanel {
    public KiwiKwikiMartPanel() {
      super(KiwiKwikiMartRequest.DATA);
    }

    @Override
    public int buyMax(final AdventureResult item, final int max) {
      return switch (item.getItemId()) {
        case ItemPool.MINI_KIWI_INTOXICATING_SPIRITS -> 1;
        default -> max;
      };
    }
  }

  private class SeptEmberPanel extends CoinmasterPanel {
    public SeptEmberPanel() {
      super(SeptEmberCenserRequest.SEPTEMBER_CENSER);
    }
  }

  private class ShowerThoughtsPanel extends CoinmasterPanel {
    public ShowerThoughtsPanel() {
      super(UsingYourShowerThoughtsRequest.DATA);
    }
  }

  public abstract class CoinmasterPanel extends JPanel implements Listener {
    protected CoinmasterData data;
    protected boolean storageInTitle = false;
    protected boolean pullsInTitle = false;

    protected ShopRowPanel shopRowPanel = null;
    protected SellPanel sellPanel = null;
    protected BuyPanel buyPanel = null;

    public CoinmasterPanel() {
      super(new BorderLayout());
      NamedListenerRegistry.registerNamedListener("(coinmaster)", this);
    }

    protected void setData(final CoinmasterData data) {
      this.data = data;

      String property = data.getProperty();
      if (property != null) {
        PreferenceListenerRegistry.registerPreferenceListener(property, this);
      }
    }

    protected void setPullsInTitle() {
      this.pullsInTitle = true;
      NamedListenerRegistry.registerNamedListener("(pullsremaining)", this);
    }

    public CoinmasterPanel(final CoinmasterData data) {
      this();

      this.setData(data);

      if (data.getShopRows() != null) {
        this.shopRowPanel = new ShopRowPanel();
        this.add(shopRowPanel, BorderLayout.CENTER);
      } else {
        if (data.getSellPrices() != null) {
          this.sellPanel = new SellPanel();
          this.add(sellPanel, BorderLayout.NORTH);
        }

        if (data.getBuyPrices() != null) {
          this.buyPanel = new BuyPanel();
          this.add(buyPanel, BorderLayout.CENTER);
        }
      }

      this.storageInTitle = this.data.getStorageAction() != null;
    }

    @Override
    public void update() {
      // (coinmaster) is fired when tokens change
      this.setTitle();
      if (this.shopRowPanel != null) {
        this.shopRowPanel.filterItems();
      }
      if (this.buyPanel != null) {
        this.buyPanel.filterItems();
      }
    }

    public CoinMasterRequest getRequest() {
      return this.data.getRequest();
    }

    public CoinMasterRequest getRequest(final boolean buying, final AdventureResult[] items) {
      return this.data.getRequest(buying, items);
    }

    public final void setTitle() {
      if (this == CoinmastersFrame.this.currentPanel()) {
        StringBuffer buffer = new StringBuffer();
        this.setTitle(buffer);
        CoinmastersFrame.this.setTitle(buffer.toString());
      }
    }

    public void setTitle(final StringBuffer buffer) {
      this.standardTitle(buffer);
    }

    public final void standardTitle(final StringBuffer buffer) {
      buffer.append("Coin Masters");
      String token = this.data.getToken();
      if (token != null) {
        AdventureResult item = this.data.getItem();
        int count = this.data.availableTokens();
        String name = (count != 1) ? this.data.getPluralToken() : token;
        buffer.append(" (");
        buffer.append(count);
        buffer.append(" ");
        buffer.append(name);

        // Makes no sense to show storage except for real items
        if (storageInTitle && item != null) {
          int count1 = item.getCount(KoLConstants.storage);
          buffer.append(", ");
          buffer.append(count1);
          buffer.append(" in storage");

          // Only show pulls if we actually have the item in storage
          if (pullsInTitle && count1 > 0 && !KoLCharacter.isHardcore()) {
            int pulls = ConcoctionDatabase.getPullsRemaining();
            buffer.append(", ");
            buffer.append(KoLCharacter.inRonin() ? String.valueOf(pulls) : "unlimited");
            buffer.append(" pull");
            buffer.append(pulls != 1 ? "s" : "");
            buffer.append(" available");
          }
        }

        buffer.append(")");
      }
    }

    public void actionConfirmed() {}

    public void actionCancelled() {}

    public boolean addSellMovers() {
      return true;
    }

    public String getPanelSelector() {
      return "<html>- " + this.data.getMaster() + "</html>";
    }

    public boolean enabled() {
      return this.data.isAccessible();
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);
      if (this.shopRowPanel != null) {
        this.shopRowPanel.setEnabled(isEnabled);
      }
      if (this.buyPanel != null) {
        this.buyPanel.setEnabled(isEnabled);
      }
      if (this.sellPanel != null) {
        this.sellPanel.setEnabled(isEnabled);
      }
    }

    public int buyMax(final AdventureResult item, final int max) {
      return max;
    }

    public int buyDefault(final int max) {
      return 1;
    }

    public void check() {
      RequestThread.postRequest(this.getRequest());
      if (this.shopRowPanel != null) {
        this.shopRowPanel.filterItems();
      }
      if (this.buyPanel != null) {
        this.buyPanel.filterItems();
      }
    }

    protected void execute(final boolean buying, final AdventureResult[] items) {
      this.execute(buying, items, null);
    }

    protected void execute(
        final boolean buying, final AdventureResult[] items, final String extraAction) {
      if (items.length == 0) {
        return;
      }

      CoinMasterRequest request = this.getRequest(buying, items);
      if (extraAction != null) {
        request.addFormField(extraAction);
      }

      RequestThread.postRequest(request);

      if (this.buyPanel != null) {
        this.buyPanel.filterItems();
      }
    }

    public AdventureResult[] getDesiredBuyItems(
        final AdventureResult[] items, final boolean fromStorage) {
      if (items.length == 0) {
        return null;
      }

      CoinmasterData data = this.data;
      Map<Integer, Integer> originalBalances = new TreeMap<>();
      Map<Integer, Integer> balances = new TreeMap<>();
      int neededSize = items.length;

      for (int i = 0; i < items.length; ++i) {
        AdventureResult item = items[i];
        int itemId = item.getItemId();

        if (!data.availableItem(itemId)) {
          // This was shown but was grayed out.
          items[i] = null;
          --neededSize;
          continue;
        }

        AdventureResult cost = data.itemBuyPrice(itemId);
        Integer currency = cost.getItemId();
        int price = cost.getCount();

        Integer value = originalBalances.get(currency);
        if (value == null) {
          int newValue =
              fromStorage ? data.availableStorageTokens(cost) : data.availableTokens(cost);
          value = newValue;
          originalBalances.put(currency, value);
          balances.put(currency, value);
        }

        int originalBalance = value.intValue();
        int balance = balances.get(currency).intValue();

        if (price > originalBalance) {
          // This was grayed out.
          items[i] = null;
          --neededSize;
          continue;
        }

        int max = CoinmasterPanel.this.buyMax(item, balance / price);
        int quantity = max;

        if (max > 1) {
          int def = CoinmasterPanel.this.buyDefault(max);
          String val =
              InputFieldUtilities.input(
                  "Buying " + item.getName() + "...", KoLConstants.COMMA_FORMAT.format(def));
          if (val == null) {
            // He hit cancel
            return null;
          }

          quantity = StringUtilities.parseInt(val);
        }

        if (quantity > max) {
          quantity = max;
        }

        if (quantity <= 0) {
          items[i] = null;
          --neededSize;
          continue;
        }

        items[i] = item.getInstance(quantity);
        balance -= quantity * price;
        balances.put(currency, balance);
      }

      // Shrink the array which will be returned so
      // that it removes any nulled values.

      if (neededSize == 0) {
        return null;
      }

      AdventureResult[] desiredItems = new AdventureResult[neededSize];
      neededSize = 0;

      for (int i = 0; i < items.length; ++i) {
        if (items[i] != null) {
          desiredItems[neededSize++] = items[i];
        }
      }

      return desiredItems;
    }

    public boolean canBuyItem(AdventureResult item) {
      return this.data.canBuyItem(item.getItemId());
    }

    public class SellPanel extends ItemListManagePanel<AdventureResult> {
      public SellPanel() {
        super((SortedListModel<AdventureResult>) KoLConstants.inventory);
        this.setButtons(
            true,
            new ActionListener[] {
              new SellListener(),
            });

        this.getElementList()
            .setCellRenderer(getCoinmasterRenderer(CoinmasterPanel.this.data, false));
        this.setEnabled(true);
        this.filterItems();
      }

      @Override
      public final void setEnabled(final boolean isEnabled) {
        super.setEnabled(isEnabled);
        this.buttons[0].setEnabled(CoinmasterPanel.this.enabled());
      }

      @Override
      public void addFilters() {}

      @Override
      public void addMovers() {
        if (CoinmasterPanel.this.addSellMovers()) {
          super.addMovers();
        }
      }

      @Override
      public AutoFilterTextField<AdventureResult> getWordFilter() {
        return new SellableFilterField();
      }

      @Override
      public void actionConfirmed() {}

      @Override
      public void actionCancelled() {}

      public class SellListener extends ThreadedListener {
        @Override
        protected void execute() {
          CoinmasterData data = CoinmasterPanel.this.data;
          String reason = data.canSell();
          if (reason != null) {
            KoLmafia.updateDisplay(MafiaState.ERROR, reason);
            return;
          }

          if (!InputFieldUtilities.confirm(
              "Are you sure you would like to trade in the selected items?")) {
            return;
          }

          AdventureResult[] items = SellPanel.this.getDesiredItems("Selling");
          if (items == null) {
            return;
          }

          CoinmasterPanel.this.execute(false, items);
        }

        @Override
        public String toString() {
          return "sell";
        }
      }

      private class SellableFilterField extends FilterItemField {
        @Override
        public boolean isVisible(final Object element) {
          if (!(element instanceof AdventureResult ar)) {
            return false;
          }
          int price =
              CoinmastersDatabase.getPrice(
                  ar.getItemId(), CoinmasterPanel.this.data.getSellPrices());
          return (price > 0) && super.isVisible(element);
        }
      }
    }

    public class BuyPanel extends ItemListManagePanel<AdventureResult> {
      public BuyPanel(ActionListener[] listeners) {
        super((LockableListModel<AdventureResult>) CoinmasterPanel.this.data.getBuyItems());

        this.eastPanel.add(
            new InvocationButton("visit", CoinmasterPanel.this, "check"), BorderLayout.SOUTH);
        this.getElementList()
            .setCellRenderer(getCoinmasterRenderer(CoinmasterPanel.this.data, true));
        this.getElementList().setVisibleRowCount(6);

        if (listeners != null) {
          this.setButtons(true, listeners);
          this.setEnabled(true);
          this.filterItems();
        }
      }

      public BuyPanel() {
        this(null);

        boolean storage = CoinmasterPanel.this.data.getStorageAction() != null;
        int count = storage ? 2 : 1;
        ActionListener[] listeners = new ActionListener[count];
        listeners[0] = new BuyListener();
        if (count > 1) {
          listeners[1] = new BuyUsingStorageListener();
        }

        this.setButtons(true, listeners);
        this.setEnabled(true);
        this.filterItems();
      }

      public void addButton(final JButton button, final boolean save) {
        JButton[] buttons = new JButton[1];
        buttons[0] = button;
        this.addButtons(buttons, save);
      }

      @Override
      public void addButtons(final JButton[] buttons, final boolean save) {
        super.addButtons(buttons, save);
      }

      @Override
      public final void setEnabled(final boolean isEnabled) {
        super.setEnabled(isEnabled);
        for (int i = 0; this.buttons != null && i < this.buttons.length; ++i) {
          this.buttons[i].setEnabled(CoinmasterPanel.this.enabled());
        }
      }

      @Override
      public void addFilters() {}

      @Override
      public void addMovers() {}

      @Override
      public AutoFilterTextField<AdventureResult> getWordFilter() {
        return new BuyableFilterField();
      }

      public AdventureResult[] getDesiredItems(final boolean fromStorage) {
        AdventureResult[] items = this.getSelectedValues().toArray(new AdventureResult[0]);
        return CoinmasterPanel.this.getDesiredBuyItems(items, fromStorage);
      }

      public class BuyListener extends ThreadedListener {
        @Override
        protected void execute() {
          CoinmasterData data = CoinmasterPanel.this.data;
          String reason = data.canBuy();
          if (reason != null) {
            KoLmafia.updateDisplay(MafiaState.ERROR, reason);
            return;
          }

          AdventureResult[] items = BuyPanel.this.getDesiredItems(false);
          if (items == null) {
            return;
          }

          CoinmasterPanel.this.execute(true, items);
        }

        @Override
        public String toString() {
          return "buy";
        }
      }

      public class BuyUsingStorageListener extends ThreadedListener {
        @Override
        protected void execute() {
          AdventureResult[] items = BuyPanel.this.getDesiredItems(true);
          if (items == null) {
            return;
          }

          CoinmasterPanel.this.execute(true, items, CoinmasterPanel.this.data.getStorageAction());
        }

        @Override
        public String toString() {
          return "from storage";
        }
      }

      private class BuyableFilterField extends FilterItemField {
        @Override
        public boolean isVisible(final Object element) {
          if (!(element instanceof AdventureResult ar)) {
            return false;
          }
          return CoinmasterPanel.this.canBuyItem(ar) && super.isVisible(element);
        }
      }
    }

    public class ShopRowPanel extends ItemListManagePanel<ShopRow> {
      // Unlike an AdventureResult, a ShopRow doesn't come with a count field.
      private record ShopRowSelection(ShopRow row, int count) {}

      public ShopRowPanel(ActionListener[] listeners) {
        super((LockableListModel<ShopRow>) CoinmasterPanel.this.data.getShopRows());
        this.eastPanel.add(
            new InvocationButton("visit", CoinmasterPanel.this, "check"), BorderLayout.SOUTH);
        this.getElementList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getElementList()
            .setCellRenderer(getCoinmasterRenderer(CoinmasterPanel.this.data, true));
        this.getElementList().setVisibleRowCount(6);

        if (listeners != null) {
          this.setButtons(true, listeners);
          this.setEnabled(true);
          this.filterItems();
        }
      }

      public ShopRowPanel() {
        this(null);

        ActionListener[] listeners = new ActionListener[1];
        listeners[0] = new BuyListener();

        this.setButtons(true, listeners);
        this.setEnabled(true);
        this.filterItems();
      }

      public void addButton(final JButton button, final boolean save) {
        JButton[] buttons = new JButton[1];
        buttons[0] = button;
        this.addButtons(buttons, save);
      }

      @Override
      public void addButtons(final JButton[] buttons, final boolean save) {
        super.addButtons(buttons, save);
      }

      @Override
      public final void setEnabled(final boolean isEnabled) {
        super.setEnabled(isEnabled);
        for (int i = 0; this.buttons != null && i < this.buttons.length; ++i) {
          this.buttons[i].setEnabled(CoinmasterPanel.this.enabled());
        }
      }

      @Override
      public void addFilters() {}

      @Override
      public void addMovers() {}

      @Override
      public AutoFilterTextField<ShopRow> getWordFilter() {
        return new BuyableFilterField();
      }

      public ShopRowSelection getDesiredRow() {
        ShopRow row = this.getSelectedValue();
        return getDesiredRow(row);
      }

      public ShopRowSelection getDesiredRow(final ShopRow row) {
        int max = row.getAffordableCount();

        if (max <= 0) {
          return null;
        }

        AdventureResult item = row.getItem();
        int quantity = 1;

        if (max > 1) {
          int def = CoinmasterPanel.this.buyDefault(max);
          String val =
              InputFieldUtilities.input(
                  "Buying " + item.getName() + "...", KoLConstants.COMMA_FORMAT.format(def));
          if (val == null) {
            // He hit cancel
            return null;
          }

          quantity = StringUtilities.parseInt(val);
        }

        if (quantity > max) {
          quantity = max;
        }

        if (quantity <= 0) {
          return null;
        }

        return new ShopRowSelection(row, quantity);
      }

      public CoinMasterRequest getRequest(final ShopRow row, int quantity) {
        return CoinmasterPanel.this.data.getRequest(row, quantity);
      }

      protected void execute(final ShopRow row, int quantity) {
        CoinMasterRequest request = getRequest(row, quantity);
        RequestThread.postRequest(request);
        filterItems();
      }

      public class BuyListener extends ThreadedListener {
        @Override
        protected void execute() {
          CoinmasterData data = CoinmasterPanel.this.data;
          String reason = data.canBuy();
          if (reason != null) {
            KoLmafia.updateDisplay(MafiaState.ERROR, reason);
            return;
          }

          ShopRowSelection selection = ShopRowPanel.this.getDesiredRow();
          if (selection == null) {
            return;
          }

          ShopRowPanel.this.execute(selection.row(), selection.count());
        }

        @Override
        public String toString() {
          return "buy";
        }
      }

      private class BuyableFilterField extends FilterItemField {
        @Override
        public boolean isVisible(final Object element) {
          if (!(element instanceof ShopRow sr)) {
            return false;
          }
          return super.isVisible(element);
        }
      }
    }
  }

  public static final DefaultListCellRenderer getCoinmasterRenderer(
      CoinmasterData data, final boolean buying) {
    return new CoinmasterRenderer(data, buying);
  }

  private static class CoinmasterRenderer extends DefaultListCellRenderer {
    private final CoinmasterData data;
    private final boolean buying;

    public CoinmasterRenderer(CoinmasterData data, final boolean buying) {
      this.setOpaque(true);
      this.data = data;
      this.buying = buying;
    }

    public boolean allowHighlight() {
      return true;
    }

    @Override
    public Component getListCellRendererComponent(
        final JList<?> list,
        final Object value,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      Component defaultComponent =
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      if (value == null) {
        return defaultComponent;
      }

      if (value instanceof AdventureResult) {
        return this.getRenderer(defaultComponent, (AdventureResult) value);
      }

      if (value instanceof ShopRow) {
        return this.getRenderer(defaultComponent, (ShopRow) value);
      }

      return defaultComponent;
    }

    public Component getRenderer(final Component defaultComponent, final AdventureResult ar) {
      boolean show = true;
      AdventureResult cost = null;

      if (ar.isSkill()) {
        int skillId = ar.getSkillId();
        cost = this.data.skillBuyPrice(skillId);
        show = data.availableSkill(skillId);
      } else if (ar.isItem()) {
        int itemId = ar.getItemId();
        cost = this.buying ? this.data.itemBuyPrice(itemId) : this.data.itemSellPrice(itemId);
        show = !this.buying || data.availableItem(itemId);
      } else {
        return defaultComponent;
      }

      if (cost == null) {
        return defaultComponent;
      }

      int price = cost.getCount();

      if (cost.isMeat()) {
        price = NPCPurchaseRequest.currentDiscountedPrice(price);
      }

      if (show && this.buying) {
        int balance1 = this.data.availableTokens(cost);
        int balance2 = this.data.availableStorageTokens(cost);
        if (price > balance1 && price > balance2) {
          show = false;
        }
      }

      StringBuilder stringForm = new StringBuilder();
      stringForm.append("<html>");
      if (!show) {
        stringForm.append("<font color=gray>");
      }
      stringForm.append(ar.getName());
      stringForm.append(" (");
      stringForm.append(price);
      stringForm.append(" ");
      stringForm.append(cost.getPluralName(price));
      stringForm.append(")");
      int count = ar.getCount();
      if (count == -1) {
        stringForm.append(" (unknown)");
      } else if (count != PurchaseRequest.MAX_QUANTITY) {
        stringForm.append(" (");
        stringForm.append(KoLConstants.COMMA_FORMAT.format(count));
        stringForm.append(")");
      }
      if (!show) {
        stringForm.append("</font>");
      }
      stringForm.append("</html>");

      ((JLabel) defaultComponent).setText(stringForm.toString());
      return defaultComponent;
    }

    public Component getRenderer(final Component defaultComponent, final ShopRow sr) {
      AdventureResult[] costs = sr.getCosts();

      if (costs == null) {
        return defaultComponent;
      }

      AdventureResult ar = sr.getItem();
      boolean show = true;

      if (ar.isSkill()) {
        int skillId = ar.getSkillId();
        show = data.availableSkill(skillId);
      } else if (ar.isItem()) {
        int itemId = ar.getItemId();
        show = sr.getAffordableCount() > 0;
      } else {
        return defaultComponent;
      }

      String costString = sr.costString();

      StringBuilder stringForm = new StringBuilder();
      stringForm.append("<html>");
      if (!show) {
        stringForm.append("<font color=gray>");
      }
      stringForm.append(ar.getName());
      int count = ar.getCount();
      if (count == -1) {
        stringForm.append(" (unknown)");
      } else if (count > 1 && count != PurchaseRequest.MAX_QUANTITY) {
        stringForm.append(" (");
        stringForm.append(KoLConstants.COMMA_FORMAT.format(count));
        stringForm.append(")");
      }
      stringForm.append(" ");
      stringForm.append(costString);
      if (!show) {
        stringForm.append("</font>");
      }
      stringForm.append("</html>");

      ((JLabel) defaultComponent).setText(stringForm.toString());
      return defaultComponent;
    }
  }
}
