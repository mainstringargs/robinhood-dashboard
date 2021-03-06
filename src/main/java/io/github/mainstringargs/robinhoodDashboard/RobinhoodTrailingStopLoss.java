package io.github.mainstringargs.robinhoodDashboard;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class RobinhoodTrailingStopLoss {

  private static boolean useExtendedHours = false;
  private static String marketAcronym = "NASDAQ";
  private static long stopLossUpdateIntervalMs = 1000;
  private static long cancelStopLossUpdateIntervalMs = 5000;
  private static long stockMarketSleepTime = 1000 * 60 * 10;
  private static double stopLossPercent = .01;
  private static int quantity = 1;
  private static DecimalFormat df2 = new DecimalFormat("###,###.00");


  public static void main(String[] args) {
    // String ticker = args[0];
    String ticker = "SQQQ";
    quantity  = 2;
    stopLossPercent = .01;

    RobinhoodApi rApi = null;
    try {
      rApi = new RobinhoodApi(RobinhoodProperties.getProperty("robinhood.user", ""),
          RobinhoodProperties.getProperty("robinhood.pass", ""));
    } catch (RobinhoodApiException e) {
      e.printStackTrace();
    }

    float lastValue = 0;

    SecurityOrder standingStopLoss = findExistingStopLossOrder(rApi, ticker);

    float setStopLoss = standingStopLoss == null ? 0.0f : (float) standingStopLoss.getStopPrice();

    if (setStopLoss > 0.0) {
      System.out.println("Found existing stopLoss @ " + setStopLoss);
    } else {
      System.out.println("No existing stoploss");
    }

    boolean firstStockMarketClosed = false;

    while (true) {

      if (!stockMarketIsOpen(rApi)) {

        if (!firstStockMarketClosed) {
          firstStockMarketClosed = true;
          System.out.print(new Date() + ": Stock market is closed");
        } else {
          System.out.print(".");
        }

        try {
          Thread.sleep(stockMarketSleepTime);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      } else if (tickerIsOwnedAtQuanity(rApi, ticker, quantity)) {

        firstStockMarketClosed = false;
        System.out.println();

        TickerQuote currentQuote = null;
        try {
          currentQuote = rApi.getQuoteByTicker(ticker);
        } catch (TickerNotFoundException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }

        float currentValue = currentQuote.getLastTradePrice();


        System.out.println(
            new Date() + ": " + "Current Val of " + ticker + " is " + df2.format(currentValue));



        float calculatedStopLoss = (float) (currentValue - (currentValue * stopLossPercent));

        System.out.println(new Date() + ": " + "Set stop loss " + df2.format(setStopLoss)
            + " Calculated stop loss " + df2.format(calculatedStopLoss));

        if (calculatedStopLoss > setStopLoss) {

          if (setStopLoss > 0 && standingStopLoss != null) {
            cancelExistingStopLoss(rApi, ticker, standingStopLoss);
          }

          try {
            Thread.sleep(cancelStopLossUpdateIntervalMs);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }


          standingStopLoss = submitNewStopLoss(rApi, ticker, calculatedStopLoss);


          setStopLoss = calculatedStopLoss;
        }



        try {
          Thread.sleep(stopLossUpdateIntervalMs);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        lastValue = currentValue;
      } else {
        System.out.println("clearing out stop loss " + setStopLoss);
        setStopLoss = 0.0f;
        standingStopLoss = null;
      }

    }
  }

  private static SecurityOrder findExistingStopLossOrder(RobinhoodApi rApi, String ticker) {

    List<SecurityOrder> orders = rApi.getOrders();

    for (SecurityOrder order : orders) {

      if (order.getTrigger().equals("stop") && order.getCancel() != null) {

        Instrument inst = rApi.getInstrumentByURL(order.getInstrument());

        if (inst.getSymbol().equals(ticker)) {
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

  private static SecurityOrder cancelExistingStopLoss(RobinhoodApi rApi, String ticker,
      SecurityOrder order) {

    try {
      order = rApi.cancelOrder(order);
    } catch (RobinhoodApiException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    if (order != null) {
      System.out.println(new Date() + ": Cancel stop loss for " + ticker + " "
          + order.getRejectReason() + " " + order.getAveragePrice() + " "
          + order.getCumulativeQuantity() + " " + order.getResponseCategory() + " "
          + order.getTransactionStateAsString() + " " + order.getTrigger());
    } else {
      System.out.println("Returned order is null.  Not sure why");
    }

    return order;
  }

  private static SecurityOrder submitNewStopLoss(RobinhoodApi rApi, String ticker, float stopLoss) {

    SecurityOrder order = null;
    try {
      order = rApi.makeMarketStopOrder(ticker, quantity, OrderTransactionType.SELL,
          TimeInForce.GOOD_UNTIL_CANCELED, stopLoss);
    } catch (TickerNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    if (order != null) {

      System.out.println(new Date() + ": New stop loss for " + ticker + " to "
          + df2.format(stopLoss) + " " + order.getRejectReason() + " " + order.getAveragePrice()
          + " " + order.getCumulativeQuantity() + " " + order.getResponseCategory() + " "
          + order.getTransactionStateAsString() + " " + order.getTrigger());
    } else {
      System.out.println("Returned order is null.  Not sure why");
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
