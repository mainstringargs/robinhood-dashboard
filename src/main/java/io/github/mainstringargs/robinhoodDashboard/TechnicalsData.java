package io.github.mainstringargs.robinhoodDashboard;

import java.util.Date;
import io.github.mainstringargs.tradingView.TechnicalRec;

public class TechnicalsData {

  private String timestamp;
  private String ticker;
  private double price;
  private TechnicalRec oneMinute;
  private TechnicalRec fiveMinutes;
  private TechnicalRec fifteenMinutes;


  public TechnicalsData(String ticker, double price, TechnicalRec oneMinute,
      TechnicalRec fiveMinutes, TechnicalRec fifteenMinutes) {
    super();
    this.ticker = ticker;
    this.price = price;
    this.oneMinute = oneMinute;
    this.fiveMinutes = fiveMinutes;
    this.fifteenMinutes = fifteenMinutes;
    this.timestamp = RobinhoodUtility.getISO8601SDate(new Date());
  }

  public String getTicker() {
    return ticker;
  }

  public double getPrice() {
    return price;
  }

  public TechnicalRec getOneMinute() {
    return oneMinute;
  }

  public TechnicalRec getFiveMinutes() {
    return fiveMinutes;
  }

  public TechnicalRec getFifteenMinutes() {
    return fifteenMinutes;
  }

  @Override
  public String toString() {
    return "TechnicalsData [timestamp=" + timestamp + ", ticker=" + ticker + ", price=" + price
        + ", oneMinute=" + oneMinute + ", fiveMinutes=" + fiveMinutes + ", fifteenMinutes="
        + fifteenMinutes + "]";
  }



}
