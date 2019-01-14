package io.github.mainstringargs.robinhoodDashboard;

import java.util.ArrayList;
import java.util.List;

public class Threshold {


  private double buyPrice = 0.0f;
  private double stopLossExit = 0.0f;
  private int numStocks;
  private String ticker;
  private Double[] resistanceBands;



  public Threshold(String ticker, int numStocks, double buyPrice, double stopLossExit,
      double... resistanceBands) {
    super();
    this.ticker = ticker;
    this.numStocks = numStocks;
    this.buyPrice = buyPrice;
    this.stopLossExit = stopLossExit;

    List<Double> resistenceList = new ArrayList<Double>();
    resistenceList.add(stopLossExit);
    for(double resistence: resistanceBands) {
      resistenceList.add(resistence);
    }
    
    this.resistanceBands = resistenceList.toArray(new Double[] {});
    
  }


  public String getTicker() {
    return ticker;
  }


  public double getBuyPrice() {
    return buyPrice;
  }

  public double getStopLossExit() {
    return stopLossExit;
  }

  public Double[] getResistanceBands() {
    return resistanceBands;
  }


  public int getNumStocks() {
    return numStocks;
  }


  public static Threshold createTreshold(String ticker, int numStocks, double buyPrice,
      double stopLossExit, double... resistanceBands) {

    Threshold thresh = new Threshold(ticker, numStocks, buyPrice, stopLossExit, resistanceBands);
    return thresh;
  }


}
