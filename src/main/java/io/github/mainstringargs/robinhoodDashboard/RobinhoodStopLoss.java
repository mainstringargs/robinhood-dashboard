package io.github.mainstringargs.robinhoodDashboard;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import com.ampro.robinhood.RobinhoodApi;
import com.ampro.robinhood.endpoint.account.data.Position;
import com.ampro.robinhood.throwables.RobinhoodApiException;
import com.ampro.robinhood.throwables.TickerNotFoundException;

public class RobinhoodStopLoss {

  public static void main(String[] args) {
    RobinhoodApi rApi = null;
    try {
      rApi = new RobinhoodApi(RobinhoodProperties.getProperty("robinhood.user", ""),
          RobinhoodProperties.getProperty("robinhood.pass", ""));
    } catch (RobinhoodApiException e) {
      e.printStackTrace();
    }

    List<Position> acctData = rApi.getAccountPositions();

    String format = ("%-5s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s  %-10s\n");

    int stopLossFactor = 10;

    System.out.printf(format, "Sym", "Avg Pr", "Quant", "Cost", "Val", "T Val", "Stk R", "T Stk R",
        "APr-" + stopLossFactor + "%", "Val-" + stopLossFactor + "%", "Rec SL");
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

      double cost = (rPos.getAverageBuyPrice() * rPos.getQuantity());
      double currentValue = rPos.getQuantity() * tradeValue;

      double averagePricesStopLoss =
          rPos.getAverageBuyPrice() - (rPos.getAverageBuyPrice() * (stopLossFactor / 100.0));

      double valueStopLoss = tradeValue - (tradeValue * (stopLossFactor / 100.0));

      double stockReturn = tradeValue - rPos.getAverageBuyPrice();
      double totalStockReturn = stockReturn * rPos.getQuantity();

      totalCost += cost;
      totalValue += currentValue;
      System.out.printf(format, rPos.getInstrumentElement().getSymbol(),
          df.format(rPos.getAverageBuyPrice()), rPos.getQuantity(), df.format(cost),
          df.format(tradeValue), df.format(currentValue), df.format(stockReturn),
          df.format(totalStockReturn), df.format(averagePricesStopLoss), df.format(valueStopLoss),
          df.format(averagePricesStopLoss > valueStopLoss ? averagePricesStopLoss : valueStopLoss));
    }

    System.out.printf(format, "TOTAL", "", "", df.format(totalCost), "", df.format(totalValue), "",
        df.format(totalValue - totalCost),
        df.format(totalCost - (totalCost * (stopLossFactor / 100.0))),
        df.format(totalValue - (totalValue * (stopLossFactor / 100.0))), "");

  }

}
