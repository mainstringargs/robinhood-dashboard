package io.github.mainstringargs.robinhoodDashboard;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.ampro.robinhood.RobinhoodApi;
import com.ampro.robinhood.endpoint.account.data.Position;
import com.ampro.robinhood.endpoint.orders.data.SecurityOrder;
import com.ampro.robinhood.endpoint.orders.enums.OrderTransactionType;
import com.ampro.robinhood.endpoint.orders.enums.TimeInForce;
import com.ampro.robinhood.endpoint.quote.data.TickerQuote;

public class RobinhoodThresholds {

  private static long stockMarketSleepTime = 1000 * 60 * 10;

  private static final List<Threshold> thresholds =
      Arrays.asList(Threshold.createTreshold("NEM", 6, 35.01f, 34.16f, 35.35f),
          Threshold.createTreshold("KGC", 30, 3.32f, 3.13f, 3.63f, 3.7, 3.8),
          Threshold.createTreshold("PACB", 5, 7.39f, 7.2f, 7.83f),
          Threshold.createTreshold("SAVE", 3, 61.10f, 55.00f, 62.00f),
          Threshold.createTreshold("BGNE", 1, 153.33f, 131.64f, 163.22f),
          Threshold.createTreshold("NBEV", 15, 6.67f, 5.44f, 6.75f, 6.9, 7.1, 8, 9, 10, 11),
          Threshold.createTreshold("CHD", 1, 69.19f, 64.59f, 69.87f),
          Threshold.createTreshold("CTB", 2, 33.39f, 31.47f, 34.21f, 34.77f, 35.49f),
          Threshold.createTreshold("HDB", 1, 103.62, 102.16, 103.61, 104.65),
          Threshold.createTreshold("HCP", 3, 30.24, 28.12, 30.23, 30.53),
          Threshold.createTreshold("NXTM", 3, 29.13, 28.37, 29.41, 30, 31, 32),
          Threshold.createTreshold("HLF", 2, 56.93, 56.46, 57.92, 59.41, 60),
          Threshold.createTreshold("ARRS", 3, 30.99, 30.33, 30.68, 30.7, 30.98, 31.29),
          Threshold.createTreshold("AMT", 1, 167.65, 156.67, 169.32, 171, 172, 173, 174, 175),
          Threshold.createTreshold("FNSR", 4, 21.93, 20.65, 22.62, 22.96, 23.35, 23.44, 23.67),
          Threshold.createTreshold("ACIW", 4, 28.27, 26.98, 30, 30.12, 30.42, 32, 33, 34),
          Threshold.createTreshold("VZ", 3, 58.31f, 54.89f, 60.66f),
          Threshold.createTreshold("CNP", 4, 29.52, 28.16, 29.81, 30, 31, 32),
          Threshold.createTreshold("EXC", 3, 47.08, 44.92, 47.54, 48, 49, 50),
          Threshold.createTreshold("ORBK", 3, 58.48, 56.99, 58.75,61, 62, 65.01, 65.45),
          Threshold.createTreshold("MA", 1, 201.65, 187.7, 222.62, 223.78)
      );

  public static void main(String[] args) {

    boolean firstStockMarketClosed = false;

    while (true) {


      RobinhoodApi rApi = RobinhoodUtility.getRobinhoodAPI(); // refresh on each rotation


      if (!RobinhoodUtility.stockMarketIsOpen(rApi)) {

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

          // // for post market processing
          // PostMarketCloseCommand.runCommand();
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


        for (Threshold thresh : thresholds) {


          TickerQuote tq = RobinhoodUtility.getQuoteByTickerSafe(rApi, thresh.getTicker());



          if (tq != null) {

            Position pos = RobinhoodUtility.getPositionForTicker(rApi, thresh.getTicker());

            if (pos == null) {
              System.out.println(new Date() + ": pos is null, double checking");
              rApi = RobinhoodUtility.getRobinhoodAPI();
              pos = RobinhoodUtility.getPositionForTicker(rApi, thresh.getTicker());
            }

            if (pos != null && (pos.getQuantity() >= thresh.getNumStocks())) {
              System.out.println(new Date() + " Already own at least " + thresh.getNumStocks()
                  + " of " + thresh.getTicker());

              float currVal = tq.getLastTradePrice();

              Double[] resistanceBands = thresh.getResistanceBands();

              SecurityOrder standingStopLoss =
                  RobinhoodUtility.findExistingStopLossOrder(rApi, thresh.getTicker(), null);

              float setStopLoss =
                  standingStopLoss == null ? 0.0f : (float) standingStopLoss.getStopPrice();

              for (int i = resistanceBands.length - 1; i >= 0; i--) {

                System.out.println(new Date() + " checking stop loss thresholds i=" + i
                    + " resistanceBands=" + resistanceBands[i] + " currVal=" + currVal
                    + " setStopLoss=" + setStopLoss);
                if (currVal > resistanceBands[i] && setStopLoss < resistanceBands[i]) {

                  if (standingStopLoss != null) {
                    SecurityOrder order = RobinhoodUtility.cancelExistingStopLossSafe(rApi,
                        thresh.getTicker(), standingStopLoss);

                    if (order != null) {
                      System.out.println(new Date() + " canceled stop loss @ " + standingStopLoss
                          + " for " + thresh.getTicker());
                    }


                    SecurityOrder newStopLoss = RobinhoodUtility.submitNewStopLoss(rApi,
                        thresh.getTicker(), thresh.getNumStocks(), resistanceBands[i]);

                    if (newStopLoss != null) {


                      System.out
                          .println(new Date() + ": New stop loss for " + thresh.getTicker() + " "
                              + newStopLoss.getRejectReason() + " " + newStopLoss.getAveragePrice()
                              + " " + newStopLoss.getCumulativeQuantity() + " "
                              + newStopLoss.getResponseCategory() + " "
                              + newStopLoss.getTransactionStateAsString() + " "
                              + newStopLoss.getTrigger());



                    } else {
                      System.out.println(thresh.getTicker()
                          + " submitNewStopLoss Returned order is null.  Not sure why");
                    }
                  }
                  break;

                }

              }


            } else {

              float currVal = tq.getLastTradePrice();

              System.out.println(new Date() + " ticker " + thresh.getTicker() + " currVal "
                  + currVal + " buy " + thresh.getBuyPrice());

              if (currVal >= thresh.getBuyPrice()) {

                SecurityOrder order = RobinhoodUtility.makeMarketOrderSafe(rApi, thresh.getTicker(),
                    thresh.getNumStocks(), OrderTransactionType.BUY, TimeInForce.GOOD_FOR_DAY);

                if (order != null) {
                  System.out.println(new Date() + " submit buy order " + order.getRejectReason()
                      + " " + order.getAveragePrice() + " " + order.getCumulativeQuantity() + " "
                      + order.getResponseCategory() + " " + order.getTransactionStateAsString()
                      + " " + order.getTrigger());

                  try {
                    Thread.sleep(10000L);
                  } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }

                  SecurityOrder newStopLoss = RobinhoodUtility.submitNewStopLoss(rApi,
                      thresh.getTicker(), thresh.getNumStocks(), thresh.getStopLossExit());

                  if (newStopLoss != null) {


                    System.out.println(new Date() + ": New stop loss for " + thresh.getTicker()
                        + " " + newStopLoss.getRejectReason() + " " + newStopLoss.getAveragePrice()
                        + " " + newStopLoss.getCumulativeQuantity() + " "
                        + newStopLoss.getResponseCategory() + " "
                        + newStopLoss.getTransactionStateAsString() + " "
                        + newStopLoss.getTrigger());



                  } else {
                    System.out.println(thresh.getTicker()
                        + " submitNewStopLoss Returned order is null.  Not sure why");
                  }

                } else {
                  System.out
                      .println(new Date() + " Buy Order for " + thresh.getTicker() + " is null");
                }

              }


            }
          } else {
            System.out.println(new Date() + " ticker for " + thresh.getTicker() + " is null");
          }

        }

        try {
          Thread.sleep(stockMarketSleepTime);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }



  }

}
