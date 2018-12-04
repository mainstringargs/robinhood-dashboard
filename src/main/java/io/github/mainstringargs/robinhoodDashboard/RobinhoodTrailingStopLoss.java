package io.github.mainstringargs.robinhoodDashboard;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import com.ampro.robinhood.RobinhoodApi;
import com.ampro.robinhood.endpoint.account.data.Position;
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
  private static double stopLossPercent = .05;
  private static int quantity = 4;
  private static DecimalFormat df2 = new DecimalFormat("###,###.00");


  public static void main(String[] args) {
    // String ticker = args[0];
    String ticker = "SQQQ";

    RobinhoodApi rApi = null;
    try {
      rApi = new RobinhoodApi(RobinhoodProperties.getProperty("robinhood.user", ""),
          RobinhoodProperties.getProperty("robinhood.pass", ""));
    } catch (RobinhoodApiException e) {
      e.printStackTrace();
    }

    float lastValue = 0;
    float setStopLoss = 0;
    SecurityOrder standingStopLoss = null;


    while (stockMarketIsOpen(rApi) && tickerIsOwnedAtQuanity(rApi, ticker, quantity)) {

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
    }


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

    System.out.println(new Date() + ": Cancel stop loss for " + ticker + " "
        + order.getRejectReason() + " " + order.getAveragePrice() + " "
        + order.getCumulativeQuantity() + " " + order.getResponseCategory() + " "
        + order.getTransactionStateAsString() + " " + order.getTrigger());

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

    System.out.println(new Date() + ": New stop loss for " + ticker + " to " + df2.format(stopLoss)
        + " " + order.getRejectReason() + " " + order.getAveragePrice() + " "
        + order.getCumulativeQuantity() + " " + order.getResponseCategory() + " "
        + order.getTransactionStateAsString() + " " + order.getTrigger());


    return order;
  }

  private static boolean stockMarketIsOpen(RobinhoodApi rApi) {
    MarketList mList = rApi.getMarketList();

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
