package io.github.mainstringargs.robinhoodDashboard;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.LinkedHashMap;
import org.junit.Test;
import com.ampro.robinhood.endpoint.quote.data.TickerQuote;
import io.github.mainstringargs.robinhoodDashboard.QQQPlayground.Trend;

public class QQQPlaygroundTest {

  public static void setLastTradePrice(TickerQuote tq, float tradePrice) {
    try {
      Field f = tq.getClass().getDeclaredField("last_trade_price");
      f.setAccessible(true);

      f.set(tq, tradePrice);
    } catch (NoSuchFieldException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void TestCase_NOT_ENOUGH_DATA() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    assertEquals(Trend.NA, QQQPlayground.findTrend(priceHistory));

  }

  @Test
  public void TestCase_NO_PRICE_CHANGE() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 6000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 4000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 2000), tq);

    assertEquals(Trend.NONE, QQQPlayground.findTrend(priceHistory));

  }

  @Test
  public void TestCase_MIDDLE_PRICE_UP() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 10.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 6000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 6.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 4000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 2000), tq);

    assertEquals(Trend.NONE, QQQPlayground.findTrend(priceHistory));

  }


  @Test
  public void TestCase_MIDDLE_PRICE_DOWN() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 4.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 2.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 6000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 4.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 4000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 2000), tq);

    assertEquals(Trend.NONE, QQQPlayground.findTrend(priceHistory));

  }

  @Test
  public void TestCase_SCATTER_UP() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 10.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 2.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 6000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 3.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 4000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 2000), tq);

    assertEquals(Trend.NONE, QQQPlayground.findTrend(priceHistory));

  }

  @Test
  public void TestCase_SCATTER_DOWN() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 10.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 2.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 6000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 3.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 4000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 2000), tq);

    // overall DOWN because of fail fast behavior
    assertEquals(Trend.DOWN, QQQPlayground.findTrend(priceHistory));

  }


  @Test
  public void TestCase_OVERALL_DOWN() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 6.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 6000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 4.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 4000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 3.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 2000), tq);

    assertEquals(Trend.DOWN, QQQPlayground.findTrend(priceHistory));

  }

  @Test
  public void TestCase_OVERALL_UP() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 8.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 9.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 6000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 10.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 4000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 11.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 2000), tq);

    assertEquals(Trend.UP, QQQPlayground.findTrend(priceHistory));

  }

  @Test
  public void TestCase_OVERALL_LATE_UP() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 6000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 4000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 11.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 2000), tq);

    assertEquals(Trend.UP, QQQPlayground.findTrend(priceHistory));

  }

  @Test
  public void TestCase_OVERALL_LATE_DOWN() {

    LinkedHashMap<Date, TickerQuote> priceHistory = new LinkedHashMap<>();

    TickerQuote tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 10000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 8000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 6000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 7.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 4000), tq);

    tq = new TickerQuote();
    setLastTradePrice(tq, 5.0f);
    priceHistory.put(new Date(System.currentTimeMillis() - 2000), tq);

    assertEquals(Trend.DOWN, QQQPlayground.findTrend(priceHistory));

  }
}
