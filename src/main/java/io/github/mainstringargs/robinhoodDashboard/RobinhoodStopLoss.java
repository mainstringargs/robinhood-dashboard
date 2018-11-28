package io.github.mainstringargs.robinhoodDashboard;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ampro.robinhood.RobinhoodApi;
import com.ampro.robinhood.endpoint.account.data.Position;
import com.ampro.robinhood.endpoint.instrument.data.Instrument;
import com.ampro.robinhood.endpoint.orders.data.SecurityOrder;
import com.ampro.robinhood.throwables.RobinhoodApiException;
import com.ampro.robinhood.throwables.TickerNotFoundException;
import io.github.mainstringargs.stockData.spi.StockDataService;
import io.github.mainstringargs.stockData.spi.StockDataServiceLoader;

public class RobinhoodStopLoss {

  public static void main(String[] args) {
    RobinhoodApi rApi = null;
    try {
      rApi = new RobinhoodApi(RobinhoodProperties.getProperty("robinhood.user", ""),
          RobinhoodProperties.getProperty("robinhood.pass", ""));
    } catch (RobinhoodApiException e) {
      e.printStackTrace();
    }

    List<SecurityOrder> orders = rApi.getOrders();

    Map<String, SecurityOrder> stopLossMap = new HashMap<>();

    for (SecurityOrder order : orders) {

      if (order.getTrigger().equals("stop") && order.getCancel() != null) {

        Instrument inst = rApi.getInstrumentByURL(order.getInstrument());

        if (stopLossMap.containsKey(inst.getSymbol())) {
          System.out.println("Found duplicate stop loss for " + inst.getSymbol());
        }
        stopLossMap.put(inst.getSymbol(), order);

      }

    }

    Map<String, StockDataService> sdsl = StockDataServiceLoader.getStockDataServices();

    StockDataService rHoodService = sdsl.get("Robinhood");
    StockDataService yahooService = sdsl.get("Yahoo Finance");



    List<Position> acctData = rApi.getAccountPositions();

    String format =
        ("%-5s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n");

    int stopLossFactor = 10;

    System.out.printf(format, "Sym", "Avg Pr", "Quant", "Cost", "Val", "T Val", "Stk R", "T Stk R",
        "Stk R %", "APr-" + stopLossFactor + "%", "Val-" + stopLossFactor + "%", "Rec SL", "Act SL",
        "SL Diff", "Y Rat", "Y Mean", "R Buy");
    double totalCost = 0.0;
    double totalValue = 0.0;
    DecimalFormat df = new DecimalFormat("###,###,##0.00");


    acctData.sort(new Comparator<Position>() {

      @Override
      public int compare(Position o1, Position o2) {
        return o1.getInstrumentElement().getSymbol()
            .compareToIgnoreCase(o2.getInstrumentElement().getSymbol());
      }
    });

    for (Position rPos : acctData) {

      float tradeValue = 0;
      try {
        tradeValue =
            rApi.getQuoteByTicker(rPos.getInstrumentElement().getSymbol()).getLastTradePrice();
      } catch (TickerNotFoundException e) {
        e.printStackTrace();
      }
      Map<String, Object> yahooStockData =
          yahooService.getStockData(rPos.getInstrumentElement().getSymbol());

      BigDecimal yahooRating = null;
      String yahooRecommendation = null;
      if (yahooStockData != null) {
        yahooRating = (BigDecimal) yahooStockData.get("Recommendation Mean");
        yahooRecommendation = (String) yahooStockData.get("Recommendation Key");
      }



      Map<String, Object> rhStockData =
          rHoodService.getStockData(rPos.getInstrumentElement().getSymbol());

      Double robinhoodBuyRating = null;
      if (rhStockData != null) {
        robinhoodBuyRating = (Double) rhStockData.get("Buy Rating Percentage");
      }


      double cost = (rPos.getAverageBuyPrice() * rPos.getQuantity());
      double currentValue = rPos.getQuantity() * tradeValue;

      double averagePricesStopLoss =
          rPos.getAverageBuyPrice() - (rPos.getAverageBuyPrice() * (stopLossFactor / 100.0));

      double valueStopLoss = tradeValue - (tradeValue * (stopLossFactor / 100.0));

      double stockReturn = tradeValue - rPos.getAverageBuyPrice();
      double totalStockReturn = stockReturn * rPos.getQuantity();
      double stockReturnPercent = totalStockReturn / cost;

      double recommendedStopLoss =
          averagePricesStopLoss > valueStopLoss ? averagePricesStopLoss : valueStopLoss;
      totalCost += cost;
      totalValue += currentValue;
      System.out
          .printf(format, rPos.getInstrumentElement().getSymbol(),
              df.format(rPos.getAverageBuyPrice()), rPos.getQuantity(), df.format(cost),
              df.format(tradeValue), df.format(currentValue), df.format(stockReturn),
              df.format(totalStockReturn), df.format(stockReturnPercent * 100.00),
              df.format(averagePricesStopLoss), df.format(valueStopLoss),
              df.format(recommendedStopLoss),
              stopLossMap.containsKey(rPos.getInstrumentElement().getSymbol())
                  ? df.format(
                      stopLossMap.get(rPos.getInstrumentElement().getSymbol()).getStopPrice())
                  : "N/A",
              stopLossMap.containsKey(rPos.getInstrumentElement().getSymbol())
                  ? df.format(
                      stopLossMap.get(rPos.getInstrumentElement().getSymbol()).getStopPrice()
                          - recommendedStopLoss)
                  : "N/A",
              yahooRecommendation == null ? "N/A" : yahooRecommendation,
              yahooRating == null ? "N/A" : df.format(yahooRating.doubleValue()),
              robinhoodBuyRating == null ? "N/A" : df.format(robinhoodBuyRating));
    }

    System.out.printf(format, "TOTAL", "", "", df.format(totalCost), "", df.format(totalValue), "",
        df.format(totalValue - totalCost), df.format(100 * ((totalValue - totalCost) / totalCost)),
        df.format(totalCost - (totalCost * (stopLossFactor / 100.0))),
        df.format(totalValue - (totalValue * (stopLossFactor / 100.0))), "", "", "", "", "", "");

  }

}
