package io.github.mainstringargs.robinhoodDashboard;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.patriques.AlphaVantageConnector;
import org.patriques.TechnicalIndicators;
import org.patriques.input.technicalindicators.Interval;
import org.patriques.input.technicalindicators.SeriesType;
import org.patriques.input.technicalindicators.TimePeriod;
import org.patriques.output.technicalindicators.RSI;
import java.util.Set;
import com.ampro.robinhood.RobinhoodApi;
import com.ampro.robinhood.endpoint.account.data.Position;
import com.ampro.robinhood.endpoint.instrument.data.Instrument;
import com.ampro.robinhood.endpoint.markets.data.Market;
import com.ampro.robinhood.endpoint.markets.data.MarketHours;
import com.ampro.robinhood.endpoint.markets.data.MarketList;
import com.ampro.robinhood.endpoint.orders.data.SecurityOrder;
import com.ampro.robinhood.endpoint.orders.enums.OrderTransactionType;
import com.ampro.robinhood.endpoint.orders.enums.TimeInForce;
import com.ampro.robinhood.endpoint.quote.data.TickerQuote;
import com.ampro.robinhood.throwables.RobinhoodApiException;
import com.ampro.robinhood.throwables.TickerNotFoundException;
import io.github.mainstringargs.alphaVantageScraper.AlphaVantageAPIKey;

public class RobinhoodAccountTrailingStopLoss {

  private static boolean useExtendedHours = false;
  private static String marketAcronym = "NASDAQ";
  private static long stopLossUpdateIntervalMs = 1000;
  private static long cancelStopLossUpdateIntervalMs = 5000;
  private static long stockMarketSleepTime = 1000 * 60 * 10;
  private static double stopLossPercent = .075;
  private static double lockInMultiple = .01;
  private static DecimalFormat df2 = new DecimalFormat("###,###.00");
  private static List<String> tickersToIgnore = new ArrayList<String>(Arrays.asList("MSFT","SNE","XBIT","MTLS"));

  private static List<String> superLongTermStocks =
      new ArrayList<String>(Arrays.asList("GOOGL", "AMZN"));

  private static List<String> longTermStocks = new ArrayList<String>(Arrays.asList("MSFT", "SNE",
      "AAPL", "CRM", "NTDOY", "PRNT", "PG", "QQQ", "USB", "SBUX", "INFO", "JLL", "BABA"));

  private static List<String> shortTermStocks = new ArrayList<String>(
      Arrays.asList("AKTS", "DFFN", "NOK", "CPSI", "CJJD", "VNET", "BIOS", "SSRM", "COLD", "CNP",
          "BEL", "MSEX", "SCG", "SPA", "UPL", "CRMD", "FLO", "MITK", "XBIT", "ATRS"));

  private static List<String> reallyShortTermStocks =
      new ArrayList<String>(Arrays.asList("SQQQ", "TQQQ"));

  private static List<String> halfStopLossPercentStocks = new ArrayList<String>(Arrays.asList());

  private static Map<String, Double> rsiValue = new HashMap<String, Double>();

  private static Map<String, Integer> numStopLossChanges = new HashMap<String, Integer>();
  private static double reallyShortTermCents = .05;
  private static double shortTermCents = .15;

  public static void main(String[] args) {
    // String ticker = args[0];

    RobinhoodApi rApi = null;
    try {
      rApi = new RobinhoodApi(RobinhoodProperties.getProperty("robinhood.user", ""),
          RobinhoodProperties.getProperty("robinhood.pass", ""));
    } catch (RobinhoodApiException e) {
      e.printStackTrace();
    }

    String apiKey = AlphaVantageAPIKey.getAPIKey();
    int timeout = 3000;
    AlphaVantageConnector apiConnector = new AlphaVantageConnector(apiKey, timeout);
    TechnicalIndicators ti = new TechnicalIndicators(apiConnector);


    List<Position> acctPositions = getAccountPositionsSafe(rApi);

    Map<String, SecurityOrder> currentStopLosses = new HashMap<>();

    List<SecurityOrder> orders = getOrdersSafe(rApi);

    for (Position pos : acctPositions) {

      String ticker = pos.getInstrumentElement().getSymbol();

      System.out.println("Searching for " + ticker + " stop loss");
      try {
        SecurityOrder standingStopLoss = findExistingStopLossOrder(rApi, ticker, orders);
        float setStopLoss =
            standingStopLoss == null ? 0.0f : (float) standingStopLoss.getStopPrice();

        if (setStopLoss > 0.0) {
          System.out.println("Found existing stopLoss for " + ticker + " @ " + setStopLoss);
          currentStopLosses.put(ticker, standingStopLoss);
        } else {
          System.out.println("!!!!!!!!!!!!! " + ticker + " No existing stoploss");
        }
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println(ticker + "Exception, adding to ignored tickers");
        tickersToIgnore.add(ticker);
      }

    }

    // refreshRSI(rApi, ti);

    boolean firstStockMarketClosed = false;

    while (true) {

      if (!stockMarketIsOpen(rApi)) {

        // if (false) {


        if (!firstStockMarketClosed) {

          firstStockMarketClosed = true;
          System.out.print(new Date() + ": Stock market is closed");

          try {
            Thread.sleep(stockMarketSleepTime);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

          // for post market processing
          PostMarketCloseCommand.runCommand();
        } else {
          System.out.print(".");
        }

        try {
          Thread.sleep(stockMarketSleepTime);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }



      } else {
        if (firstStockMarketClosed) {
          refreshRSI(rApi, ti);
        }

        firstStockMarketClosed = false;

        Set<String> currentCheckedPositions = new HashSet<String>();

        acctPositions = getAccountPositionsSafe(rApi);

        List<SecurityOrder> latestOrders = getOrdersSafe(rApi);

        for (Position pos : acctPositions) {

          String ticker = getTickerSafe(rApi, pos);

          if (ticker == null || tickersToIgnore.contains(ticker)) {
            continue;
          }

          currentCheckedPositions.add(ticker);

          TickerQuote currentQuote = getQuoteByTickerSafe(rApi, ticker);

          float currentValue = currentQuote.getLastTradePrice();

          SecurityOrder latestOrderForTicker =
              findExistingStopLossOrder(rApi, ticker, latestOrders);
          double setStopLoss = 0.0f;
          if (latestOrderForTicker != null) {

            setStopLoss = latestOrderForTicker.getStopPrice();
          } else {
            // setStopLoss = (float) (currentStopLosses.get(ticker) != null
            // ? currentStopLosses.get(ticker).getStopPrice()
            // : 0.0f);

          }

          System.out.println(new Date() + ": " + "Current Val of " + ticker + " is "
              + df2.format(currentValue) + " numStopLossUpdates: "
              + (numStopLossChanges.containsKey(ticker) ? numStopLossChanges.get(ticker) : "N/A")
              + " rsi: " + (rsiValue.containsKey(ticker) ? rsiValue.get(ticker) : "N/A"));


          double calculatedStopLoss = getStopLossCalculatedValue(ticker, pos.getAverageBuyPrice(),
              currentValue, setStopLoss) - .01;

          System.out
              .println(new Date() + ": " + ticker + " Set stop loss " + df2.format(setStopLoss)
                  + " Calculated stop loss " + df2.format(calculatedStopLoss));

          if (calculatedStopLoss > (setStopLoss + .01)) {

            if (setStopLoss > 0 && currentStopLosses.containsKey(ticker)) {
              SecurityOrder so =
                  cancelExistingStopLossSafe(rApi, ticker, currentStopLosses.get(ticker));

            }

            try {
              Thread.sleep(cancelStopLossUpdateIntervalMs);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }

            Integer numStopLosses = numStopLossChanges.get(ticker);

            if (numStopLosses == null) {
              numStopLosses = 0;
            }

            numStopLosses++;
            numStopLossChanges.put(ticker, numStopLosses);


            SecurityOrder newStopLoss =
                submitNewStopLoss(rApi, ticker, (int) pos.getQuantity(), calculatedStopLoss);

            if (newStopLoss != null) {
              currentStopLosses.put(ticker, newStopLoss);

              setStopLoss = calculatedStopLoss;

            }
          }

          try {
            Thread.sleep(stopLossUpdateIntervalMs);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

        }

        Set<String> stopLossesToRemove = new HashSet<>();
        for (Entry<String, SecurityOrder> entry : currentStopLosses.entrySet()) {
          if (!currentCheckedPositions.contains(entry.getKey())) {
            stopLossesToRemove.add(entry.getKey());
          }
        }

        if (!stopLossesToRemove.isEmpty()) {

          System.out.println("Clearing out top losses for " + stopLossesToRemove);

          for (String key : stopLossesToRemove) {
            currentStopLosses.remove(key);
          }
        }

      }
    }

  }

  private static List<SecurityOrder> getOrdersSafe(RobinhoodApi rApi) {
    List<SecurityOrder> orders = null;
    try {
      orders = rApi.getOrders();
    } catch (Exception e1) {
      System.out.println("getOrdersSafe Failed first time");
    }


    for (int i = 0; i < 10; i++) {
      if (orders == null) {

        try {
          Thread.sleep(i * 1000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        try {
          orders = rApi.getOrders();
        } catch (Exception e) {
          System.out.println("getOrdersSafe Failed " + i + " times");
        }

        if (orders != null) {
          break;
        }
      }
    }

    return orders;
  }

  private static double getStopLossCalculatedValue(String ticker, double averageBuyPrice,
      double currentValue, double setStopLoss) {
    double stopLossValue =
        (double) (halfStopLossPercentStocks.contains(ticker) ? stopLossPercent / 2.0f
            : stopLossPercent);

    double calculatedStopLoss = (double) (currentValue - (currentValue * stopLossValue));

    boolean isShortTermGain = false;


    if (longTermStocks.contains(ticker)) {

    } else if (superLongTermStocks.contains(ticker)) {

      stopLossValue = stopLossPercent * 1.5;

      calculatedStopLoss = (double) (currentValue - (currentValue * stopLossValue));


    } else if (reallyShortTermStocks.contains(ticker)) {
      calculatedStopLoss = (float) (currentValue - reallyShortTermCents);
      System.out
          .println("reallyShortTerm " + ticker + " " + currentValue + " " + calculatedStopLoss);
      isShortTermGain = true;
    } else if (currentValue > averageBuyPrice) {

      //short term
      calculatedStopLoss = (float) (currentValue - shortTermCents);

      isShortTermGain = true;
    }

    System.out.println(">>>>>>>> " + ticker + " " + averageBuyPrice + " " + currentValue + " "
        + setStopLoss + " " + calculatedStopLoss + " " + isShortTermGain);


    return calculatedStopLoss;

  }

  private static TickerQuote getQuoteByTickerSafe(RobinhoodApi rApi, String ticker) {
    TickerQuote currentQuote = null;
    try {
      currentQuote = rApi.getQuoteByTicker(ticker);
    } catch (Exception e1) {
      System.out.println("getQuoteByTickerSafe Failed first times");
    }

    for (int i = 0; i < 10; i++) {
      if (currentQuote == null) {

        try {
          Thread.sleep(i * 1000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        try {
          currentQuote = rApi.getQuoteByTicker(ticker);
        } catch (Exception e) {
          System.out.println("getQuoteByTickerSafe Failed " + i + " times");
        }

        if (currentQuote != null) {
          break;
        }
      }
    }

    return currentQuote;
  }

  private static String getTickerSafe(RobinhoodApi rApi, Position pos) {
    String ticker = null;

    if (pos != null) {
      Instrument instrument = pos.getInstrumentElement();

      for (int i = 0; i < 10; i++) {
        if (instrument == null) {

          try {
            Thread.sleep(i * 1000);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

          instrument = pos.getInstrumentElement();

          if (instrument != null) {
            break;
          }
        }
      }

      ticker = instrument.getSymbol();
    }


    return ticker;
  }

  private static List<Position> getAccountPositionsSafe(RobinhoodApi rApi) {
    List<Position> pList = null;
    try {
      pList = rApi.getAccountPositions();
    } catch (Exception e1) {
      System.out.println("getAccountPositionsSafe Failed first times");
    }

    for (int i = 0; i < 10; i++) {
      if (pList == null) {

        try {
          Thread.sleep(i * 1000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        try {
          pList = rApi.getAccountPositions();
        } catch (Exception e) {
          System.out.println("getAccountPositionsSafe Failed " + i + " times");
        }

        if (pList != null) {
          break;
        }
      }
    }

    return pList != null ? pList : new ArrayList<Position>();
  }

  private static void refreshRSI(RobinhoodApi rApi, TechnicalIndicators ti) {

    for (Position pos : rApi.getAccountPositions()) {

      String ticker = pos.getInstrumentElement().getSymbol();

      RSI rsi = null;
      try {
        rsi = ti.rsi(ticker, Interval.FIFTEEN_MIN, TimePeriod.of(5), SeriesType.CLOSE);
      } catch (Exception e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

      if (rsi != null && rsi.getData() != null && !rsi.getData().isEmpty()) {
        rsiValue.put(ticker, rsi.getData().get(rsi.getData().size() - 1).getData());
      }

      System.out.println("Got RSI Data for " + ticker + " " + rsiValue.get(ticker));
      // to get around API limits
      try {
        Thread.sleep(20000L);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

  }

  private static SecurityOrder findExistingStopLossOrder(RobinhoodApi rApi, String ticker,
      List<SecurityOrder> orders) {

    for (SecurityOrder order : orders) {

      if (order.getTrigger().equals("stop") && order.getCancel() != null) {
        Instrument inst = rApi.getInstrumentByURL(order.getInstrument());

        if (inst!=null&& inst.getSymbol()!=null && inst.getSymbol().equals(ticker)) {

          // System.out.println(">>EXISTING " + ticker + " " + order.getRejectReason() + " "
          // + order.getTransactionState() + " " + order.getResponseCategory());
          return order;
        }

      }

    }

    return null;
  }

  private static boolean tickerIsOwnedAtQuanity(RobinhoodApi rApi, String ticker,
      int stockQuantity) {

    List<Position> stockPositions = rApi.getAccountPositions();

    int tickerQuant = 0;

    for (Position position : stockPositions) {
      if (position.getInstrumentElement().getSymbol().equals(ticker)) {
        tickerQuant += position.getQuantity();
      }
    }

    System.out.println(new Date() + ": Own " + tickerQuant + " of " + ticker);

    return tickerQuant >= stockQuantity;
  }

  private static SecurityOrder cancelExistingStopLossSafe(RobinhoodApi rApi, String ticker,
      SecurityOrder order) {

    try {
      order = rApi.cancelOrder(order);
    } catch (Exception e) {
      System.out.println("cancelOrder Failed first time");
    }

    for (int i = 0; i < 10; i++) {
      if (order == null) {

        try {
          Thread.sleep(i * 1000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        try {
          order = rApi.cancelOrder(order);
        } catch (Exception e) {
          System.out.println("cancelOrder Failed " + i + " times");
        }

        if (order != null) {
          break;
        }
      }
    }

    if (order != null) {
      System.out.println(new Date() + ": Cancel stop loss for " + ticker + " "
          + order.getRejectReason() + " " + order.getAveragePrice() + " "
          + order.getCumulativeQuantity() + " " + order.getResponseCategory() + " "
          + order.getTransactionStateAsString() + " " + order.getTrigger());
    } else {
      System.out
          .println(ticker + " cancelExistingStopLossSafe Returned order is null.  Not sure why");
    }

    return order;
  }

  private static SecurityOrder submitNewStopLoss(RobinhoodApi rApi, String ticker, int quantity,
      double stopLoss) {

    SecurityOrder order = null;
    try {
      order = rApi.makeMarketStopOrder(ticker, quantity, OrderTransactionType.SELL,
          TimeInForce.GOOD_UNTIL_CANCELED, (float) (stopLoss));
    } catch (TickerNotFoundException e) {
      System.out.println("submitNewStopLoss Failed first time");
    }

    for (int i = 0; i < 10; i++) {
      if (order == null) {

        try {
          Thread.sleep(i * 1000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        try {
          order = rApi.makeMarketStopOrder(ticker, quantity, OrderTransactionType.SELL,
              TimeInForce.GOOD_UNTIL_CANCELED, (float) (stopLoss));
        } catch (RobinhoodApiException e) {
          System.out.println("submitNewStopLoss Failed " + i + " times");
        }

        if (order != null) {
          break;
        }
      }
    }

    if (order != null) {

      System.out.println(new Date() + ": New stop loss for " + ticker + " to "
          + df2.format(stopLoss) + " " + order.getRejectReason() + " " + order.getAveragePrice()
          + " " + order.getCumulativeQuantity() + " " + order.getResponseCategory() + " "
          + order.getTransactionStateAsString() + " " + order.getTrigger() + " numStopLossUpdates: "
          + numStopLossChanges.get(ticker));
    } else {
      System.out.println(ticker + " submitNewStopLoss Returned order is null.  Not sure why");
    }

    return order;
  }

  private static boolean stockMarketIsOpen(RobinhoodApi rApi) {
    MarketList mList = rApi.getMarketList();

    for (int i = 0; i < 10; i++) {
      if (mList == null) {

        try {
          Thread.sleep(i * 1000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        mList = rApi.getMarketList();

        if (mList != null) {
          break;
        }
      }
    }

    if (mList != null) {
      List<Market> markList = mList.getResults();
      Market nasdaq = null;
      for (Market market : markList) {
        if (market.getAcronym().equals(marketAcronym)) {
          nasdaq = market;
          break;
        }
      }

      if (nasdaq != null) {
        MarketHours hours = rApi.getMarketHoursByURL(nasdaq.getTodaysHours());

        if (!hours.getIsOpen()) {
          return false;
        }

        Date openingTime = null;
        Date closingTime = null;
        if (!useExtendedHours) {
          try {
            openingTime = toCalendar(hours.getOpensAt()).getTime();
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          try {
            closingTime = toCalendar(hours.getClosesAt()).getTime();
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {
          try {
            openingTime = toCalendar(hours.getExtendedOpensAt()).getTime();
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          try {
            closingTime = toCalendar(hours.getExtendedClosesAt()).getTime();
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

        Date currentTime = new Date(System.currentTimeMillis());

        return (openingTime.before(currentTime) && closingTime.after(currentTime));


      }
    }
    if (mList == null) {
      System.err.println("Robinhood is broken!");
    }

    return false;
  }

  /** Transform ISO 8601 string to Calendar. */
  public static Calendar toCalendar(final String iso8601string) throws ParseException {
    Calendar calendar = GregorianCalendar.getInstance();
    String s = iso8601string.replace("Z", "+00:00");
    try {
      s = s.substring(0, 22) + s.substring(23); // to get rid of the ":"
    } catch (IndexOutOfBoundsException e) {
      throw new ParseException("Invalid length", 0);
    }
    Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
    calendar.setTime(date);
    return calendar;
  }
}
