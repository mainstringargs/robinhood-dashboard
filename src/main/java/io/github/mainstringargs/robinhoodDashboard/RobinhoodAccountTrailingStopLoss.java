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

  private static long stopLossUpdateIntervalMs = 1000;
  private static long cancelStopLossUpdateIntervalMs = 5000;
  private static long stockMarketSleepTime = 1000 * 60 * 10;
  private static double stopLossPercent = .075;
  private static double lockInMultiple = .01;
  private static DecimalFormat df2 = new DecimalFormat("###,###.00");
  private static List<String> tickersToIgnore = new ArrayList<String>(
      Arrays.asList("AMZN", "GOOGL", "NEM", "KGC", "PACB", "SAVE", "BGNE", "NBEV", "CHD", "CTB",
          "HDB", "HCP", "NXTM", "HLF", "ARRS", "AMT", "FNSR", "ACIW", "VZ", "CNP", "EXC"));

  private static List<String> superLongTermStocks = new ArrayList<String>(Arrays.asList());

  private static List<String> longTermStocks =
      new ArrayList<String>(Arrays.asList("MSFT", "SNE", "AMZN", "CRM", "NTDOY", "PRNT", "PG",
          "QQQ", "PYPL", "SBUX", "INFO", "JLL", "GOOGL", "BABA", "NKE", "CRSP", "UNH"));

  private static List<String> shortTermStocks = new ArrayList<String>(
      Arrays.asList("AKTS", "DFFN", "NOK", "CPSI", "CJJD", "VNET", "BIOS", "SSRM", "COLD", "CNP",
          "BEL", "MSEX", "SCG", "SPA", "UPL", "CRMD", "FLO", "MITK", "XBIT", "ATRS"));

  private static List<String> reallyShortTermStocks =
      new ArrayList<String>(Arrays.asList("SQQQ", "TQQQ"));

  private static List<String> halfStopLossPercentStocks = new ArrayList<String>(Arrays.asList());

  private static Map<String, Double> rsiValue = new HashMap<String, Double>();

  private static Map<String, Integer> numStopLossChanges = new HashMap<String, Integer>();
  private static double reallyShortTermCents = .10;
  private static double shortTermCentsLow = .20;
  private static double shortTermCentsHigh = 2.00;

  public static void main(String[] args) {
    // String ticker = args[0];

    RobinhoodApi rApi = RobinhoodUtility.getRobinhoodAPI();

    String apiKey = AlphaVantageAPIKey.getAPIKey();
    int timeout = 3000;
    AlphaVantageConnector apiConnector = new AlphaVantageConnector(apiKey, timeout);
    TechnicalIndicators ti = new TechnicalIndicators(apiConnector);


    List<Position> acctPositions = RobinhoodUtility.getAccountPositionsSafe(rApi);

    Map<String, SecurityOrder> currentStopLosses = new HashMap<>();

    List<SecurityOrder> orders = RobinhoodUtility.getOrdersSafe(rApi);

    for (Position pos : acctPositions) {

      String ticker = pos.getInstrumentElement().getSymbol();

      System.out.println("Searching for " + ticker + " stop loss");
      try {
        SecurityOrder standingStopLoss =
            RobinhoodUtility.findExistingStopLossOrder(rApi, ticker, orders);
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


      rApi = RobinhoodUtility.getRobinhoodAPI(); // refresh on each rotation


      if (!RobinhoodUtility.stockMarketIsOpen(rApi)) {

        // if (false) {

        if (!firstStockMarketClosed) {

          firstStockMarketClosed = true;
          System.out.print(new Date() + ": Stock market is closed");

          numStopLossChanges.clear();

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

        acctPositions = RobinhoodUtility.getAccountPositionsSafe(rApi);


        for (Position pos : acctPositions) {

          String ticker = RobinhoodUtility.getTickerSafe(rApi, pos);

          if (ticker == null || tickersToIgnore.contains(ticker)) {
            continue;
          }

          currentCheckedPositions.add(ticker);

          TickerQuote currentQuote = RobinhoodUtility.getQuoteByTickerSafe(rApi, ticker);

          if (currentQuote != null) {
            float currentValue = currentQuote.getLastTradePrice();

            SecurityOrder latestOrderForTicker =
                RobinhoodUtility.findExistingStopLossOrder(rApi, ticker, null);
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
                SecurityOrder so = RobinhoodUtility.cancelExistingStopLossSafe(rApi, ticker,
                    currentStopLosses.get(ticker));

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


              SecurityOrder newStopLoss = RobinhoodUtility.submitNewStopLoss(rApi, ticker,
                  (int) pos.getQuantity(), calculatedStopLoss);

              if (newStopLoss != null) {


                System.out.println(new Date() + ": New stop loss for " + ticker + " to "
                    + df2.format(calculatedStopLoss) + " " + newStopLoss.getRejectReason() + " "
                    + newStopLoss.getAveragePrice() + " " + newStopLoss.getCumulativeQuantity()
                    + " " + newStopLoss.getResponseCategory() + " "
                    + newStopLoss.getTransactionStateAsString() + " " + newStopLoss.getTrigger()
                    + " numStopLossUpdates: " + numStopLossChanges.get(ticker));

                currentStopLosses.put(ticker, newStopLoss);

                setStopLoss = calculatedStopLoss;


              } else {
                System.out
                    .println(ticker + " submitNewStopLoss Returned order is null.  Not sure why");
              }


            }

            try {
              Thread.sleep(stopLossUpdateIntervalMs);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }

          } else {
            System.err.println("Error reading Quote for " + ticker);
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

      double removedValue = shortTermCentsHigh;
      double diff = shortTermCentsHigh - shortTermCentsLow;

      if (currentValue < 200.0) {
        removedValue = shortTermCentsLow + (currentValue / 200.0) * diff;
        // System.out.println("REMOVED VALUE "+removedValue);
      }


      // short term
      calculatedStopLoss = (float) (currentValue - removedValue);

      isShortTermGain = true;
    }

    System.out.println(">>>>>>>> " + ticker + " " + averageBuyPrice + " " + currentValue + " "
        + setStopLoss + " " + calculatedStopLoss + " " + isShortTermGain);


    return calculatedStopLoss;

  }



  private static void refreshRSI(RobinhoodApi rApi, TechnicalIndicators ti) {

    for (Position pos : RobinhoodUtility.getAccountPositionsSafe(rApi)) {

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



}
