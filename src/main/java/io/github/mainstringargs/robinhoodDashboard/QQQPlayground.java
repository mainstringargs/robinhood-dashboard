package io.github.mainstringargs.robinhoodDashboard;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.ampro.robinhood.RobinhoodApi;
import com.ampro.robinhood.endpoint.quote.data.TickerQuote;
import com.ampro.robinhood.throwables.RequestTooLargeException;
import com.ampro.robinhood.throwables.RobinhoodApiException;

public class QQQPlayground {

  public enum Trend {
    UP, DOWN, NONE, NA
  };


  public static int MAX = 20;

  public static void main(String[] args) {

    RobinhoodApi rApi = null;
    try {
      rApi = new RobinhoodApi(RobinhoodProperties.getProperty("robinhood.user", ""),
          RobinhoodProperties.getProperty("robinhood.pass", ""));
    } catch (RobinhoodApiException e) {
      e.printStackTrace();
    }

    String format = "%-10s %-10s %-10s %-10s\n";
    DecimalFormat df = new DecimalFormat("###,###,##0.0000");

    LinkedHashMap<Date, TickerQuote> sqqqPriceHistory = new LinkedHashMap<Date, TickerQuote>() {
      protected boolean removeEldestEntry(Map.Entry<Date, TickerQuote> eldest) {
        return size() > MAX;
      }
    };

    LinkedHashMap<Date, TickerQuote> tqqqPriceHistory = new LinkedHashMap<Date, TickerQuote>() {
      protected boolean removeEldestEntry(Map.Entry<Date, TickerQuote> eldest) {
        return size() > MAX;
      }
    };
    
    int numSQQQUps = 0;
    int numTQQQUps = 0;


    while (true) {


      System.out.println("=== " + new Date() + " ===");

      List<TickerQuote> qqqQuotes = null;
      try {
        qqqQuotes = rApi.getQuoteListByTickers(Arrays.asList("SQQQ", "TQQQ"));
      } catch (RequestTooLargeException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

      TickerQuote sqqqQuote = qqqQuotes.get(0);
      TickerQuote tqqqQuote = qqqQuotes.get(1);

      sqqqPriceHistory.put(new Date(), sqqqQuote);

      tqqqPriceHistory.put(new Date(), tqqqQuote);


      Trend sqqqTrend = findTrend(sqqqPriceHistory);
      
      if(sqqqTrend == Trend.UP) {
        numSQQQUps++;
      }

      Trend tqqqTrend = findTrend(tqqqPriceHistory);
      
      if(tqqqTrend == Trend.UP) {
        numTQQQUps++;
      }


      System.out.printf(format, sqqqQuote.getSymbol(), df.format(sqqqQuote.getLastTradePrice()),
          sqqqTrend, numSQQQUps);

      System.out.printf(format, tqqqQuote.getSymbol(), df.format(tqqqQuote.getLastTradePrice()),
          tqqqTrend, numTQQQUps);



      try {
        Thread.sleep(15000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

  }

  public static Trend findTrend(LinkedHashMap<Date, TickerQuote> priceHistory) {
    if (priceHistory.size() < (MAX / 2.0)) {
      return Trend.NA;
    }


    Trend overallTrend = Trend.NONE;

//    System.out.println("!!!SIZE " + priceHistory.size());
//
//    for (Entry<Date, TickerQuote> entry : priceHistory.entrySet()) {
//      System.out.println("XXXX " + entry.getValue().getSymbol() + " " + entry.getKey() + " "
//          + entry.getValue().getLastTradePrice());
//    }

    TickerQuote[] tickerQuotes = priceHistory.values().toArray(new TickerQuote[] {});

    TickerQuote firstQuote = tickerQuotes[0];
    TickerQuote lastQuote = tickerQuotes[tickerQuotes.length - 1];

    int comparison = Float.compare(firstQuote.getLastTradePrice(), lastQuote.getLastTradePrice());


    if (comparison < 0) {
      overallTrend = Trend.UP;
    } else if (comparison > 0) {
      return Trend.DOWN;
    } else {
      return Trend.NONE;
    }



    for (int i = 0; i < priceHistory.size() - 1; i++) {
      TickerQuote thisQuote = tickerQuotes[i];
      TickerQuote nextQuote = tickerQuotes[i + 1];

      comparison = Float.compare(thisQuote.getLastTradePrice(), nextQuote.getLastTradePrice());

      Trend currentTrend = null;


      if (comparison < 0) {
        currentTrend = Trend.UP;
      } else if (comparison > 0) {
        currentTrend = Trend.DOWN;
      } else {
        currentTrend = Trend.NONE;
      }

      if (currentTrend == Trend.DOWN) {
        return Trend.NONE;
      }


    }

    return overallTrend;
  }

}
