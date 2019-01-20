package io.github.mainstringargs.robinhoodDashboard;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
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

public class RobinhoodUtility {


  private static boolean useExtendedHours = false;
  private static String marketAcronym = "NASDAQ";

  public static RobinhoodApi getRobinhoodAPI() {
    RobinhoodApi rApi = null;
    try {
      rApi = new RobinhoodApi(RobinhoodProperties.getProperty("robinhood.user", ""),
          RobinhoodProperties.getProperty("robinhood.pass", ""));
    } catch (RobinhoodApiException e) {
      e.printStackTrace();
    }
    return rApi;
  }

  public static List<SecurityOrder> getOrdersSafe(RobinhoodApi rApi) {
    List<SecurityOrder> orders = null;
    try {
      orders = rApi.getOrders();
    } catch (Exception e1) {
      System.out.println("getOrdersSafe Failed first time");
    }


    for (int i = 0; i < 10; i++) {
      if (orders == null) {

        try {
          Thread.sleep(i * 5000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        try {
          orders = RobinhoodUtility.getRobinhoodAPI().getOrders();
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

  public static TickerQuote getQuoteByTickerSafe(RobinhoodApi rApi, String ticker) {
    TickerQuote currentQuote = null;
    try {
      currentQuote = rApi.getQuoteByTicker(ticker);
    } catch (Exception e1) {
      System.out.println("getQuoteByTickerSafe Failed first times");
    }

    for (int i = 0; i < 10; i++) {
      if (currentQuote == null) {

        try {
          Thread.sleep(i * 5000);
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

  public static String getTickerSafe(RobinhoodApi rApi, Position pos) {
    String ticker = null;

    if (pos != null) {
      Instrument instrument = pos.getInstrumentElement();

      for (int i = 0; i < 10; i++) {
        if (instrument == null) {

          try {
            Thread.sleep(i * 5000);
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

  public static List<Position> getAccountPositionsSafe(RobinhoodApi rApi) {
    List<Position> pList = null;
    try {
      pList = rApi.getAccountPositions();
    } catch (Exception e1) {
      System.out.println("getAccountPositionsSafe Failed first times");
    }

    for (int i = 0; i < 10; i++) {
      if (pList == null) {

        try {
          Thread.sleep(i * 5000);
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

  public static SecurityOrder submitNewStopLoss(RobinhoodApi rApi, String ticker, int quantity,
      double stopLoss) {

    SecurityOrder order = null;
    try {
      order = rApi.makeMarketStopOrder(ticker, quantity, OrderTransactionType.SELL,
          TimeInForce.GOOD_UNTIL_CANCELED, (float) (stopLoss));
    } catch (Exception e) {
      System.out.println("submitNewStopLoss Failed first time");
    }

    for (int i = 0; i < 10; i++) {
      if (order == null) {

        try {
          Thread.sleep(i * 5000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        try {
          order = rApi.makeMarketStopOrder(ticker, quantity, OrderTransactionType.SELL,
              TimeInForce.GOOD_UNTIL_CANCELED, (float) (stopLoss));
        } catch (Exception e) {
          System.out.println("submitNewStopLoss Failed " + i + " times");
        }

        if (order != null) {
          break;
        }
      }
    }



    return order;
  }

  public static boolean stockMarketIsOpen(RobinhoodApi rApi) {
    MarketList mList = rApi.getMarketList();
    MarketHours hours = null;

    for (int i = 0; i < 10; i++) {
      if (mList == null) {

        try {
          Thread.sleep(i * 5000);
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
        hours = rApi.getMarketHoursByURL(nasdaq.getTodaysHours());

        if (hours != null) {
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
    }
    if (mList == null || hours == null) {
      System.err.println("Robinhood is broken!");
    }

    return false;
  }

  public static String getISO8601SDate(Date date) {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    String nowAsString = df.format(date);
    return nowAsString;
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

  public static SecurityOrder cancelExistingStopLossSafe(RobinhoodApi rApi, String ticker,
      SecurityOrder order) {

    try {
      order = rApi.cancelOrder(order);
    } catch (Exception e) {
      System.out.println("cancelOrder Failed first time");
    }

    for (int i = 0; i < 10; i++) {
      if (order == null) {

        try {
          Thread.sleep(i * 5000);
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

  public static Position getPositionForTicker(RobinhoodApi rApi, String ticker) {

    List<Position> positions = getAccountPositionsSafe(rApi);

    if (positions != null) {

      for (Position pos : positions) {

        Instrument inst = pos.getInstrumentElement();

        if (inst == null) {


          for (int i = 0; i < 10; i++) {
            if (inst == null) {

              try {
                Thread.sleep(i * 5000);
              } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }

              try {
                inst = pos.getInstrumentElement();

              } catch (Exception e) {
                System.out.println("getInstrumentElement Failed " + i + " times");
              }

              if (inst != null) {
                break;
              }
            }
          }
        }

        if (inst != null && inst.getSymbol().equals(ticker)) {
          return pos;
        }
      }
    }

    return null;
  }

  public static SecurityOrder makeMarketOrderSafe(RobinhoodApi rApi, String ticker, int quantity,
      OrderTransactionType orderType, TimeInForce time) {
    SecurityOrder order = null;


    try {
      order = rApi.makeMarketOrder(ticker, quantity, orderType, time);
    } catch (Exception e) {
      System.out.println("makeMarketOrder Failed first time");
    }

    for (int i = 0; i < 10; i++) {
      if (order == null) {

        try {
          Thread.sleep(i * 5000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        try {
          order = rApi.makeMarketOrder(ticker, quantity, orderType, time);
        } catch (Exception e) {
          System.out.println("makeMarketOrder Failed " + i + " times");
        }

        if (order != null) {
          break;
        }
      }
    }

    return order;
  }


  public static SecurityOrder findExistingStopLossOrder(RobinhoodApi rApi, String ticker,
      List<SecurityOrder> orders) {

    if (orders == null) {
      orders = getOrdersSafe(rApi);
    }

    if (orders != null) {
      for (SecurityOrder order : orders) {

        if (order.getTrigger().equals("stop") && order.getCancel() != null) {
          Instrument inst = rApi.getInstrumentByURL(order.getInstrument());

          if (inst != null && inst.getSymbol() != null && inst.getSymbol().equals(ticker)) {

            // System.out.println(">>EXISTING " + ticker + " " + order.getRejectReason() + " "
            // + order.getTransactionState() + " " + order.getResponseCategory());
            return order;
          }

        }

      }
    }
    return null;
  }

}
