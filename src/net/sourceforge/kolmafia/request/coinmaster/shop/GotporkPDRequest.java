package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;

public class GotporkPDRequest extends CoinMasterRequest {
  public static final String master = "Gotpork P. D.";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>([\\d,]+) incriminating evidence");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.INCRIMINATING_EVIDENCE, 1);

  public static final CoinmasterData GOTPORK_PD =
      new CoinmasterData(master, "Gotpork P. D.", GotporkPDRequest.class)
          .withToken("incriminating evidence")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "batman_pd")
          .withItemBuyPrice(GotporkPDRequest::itemBuyPrice)
          .withAccessible(GotporkPDRequest::accessible);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    int price = GOTPORK_PD.getBuyPrices().get(itemId);
    if (price == 1) {
      return COIN;
    }
    // price increased by 3 each time you buy one
    int count = InventoryManager.getCount(itemId);
    if (count > 0) {
      price = 3 * (count + 1);
    }
    return COIN.getInstance(price);
  }

  public GotporkPDRequest() {
    super(GOTPORK_PD);
  }

  public GotporkPDRequest(final boolean buying, final AdventureResult[] attachments) {
    super(GOTPORK_PD, buying, attachments);
  }

  public GotporkPDRequest(final boolean buying, final AdventureResult attachment) {
    super(GOTPORK_PD, buying, attachment);
  }

  public GotporkPDRequest(final boolean buying, final int itemId, final int quantity) {
    super(GOTPORK_PD, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=batman_pd")) {
      return;
    }

    CoinmasterData data = GOTPORK_PD;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (KoLCharacter.getLimitMode() != LimitMode.BATMAN) {
      return "Only Batfellow can go to the Gotpork P. D.";
    }
    if (BatManager.currentBatZone() != BatManager.DOWNTOWN) {
      return "Batfellow can only visit the Gotpork Police Department while Downtown.";
    }
    return null;
  }
}
