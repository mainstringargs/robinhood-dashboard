package io.github.mainstringargs.robinhoodDashboard.domain;

import java.util.ArrayList;

public class StockCategories {

  private String ticker;
  private String stockName;
  private ArrayList<String> categories = new ArrayList<>();



  public String getTicker() {
    return ticker;
  }

  public ArrayList<String> getCategories() {
    return categories;
  }

  public void setTicker(String symbol) {
    this.ticker = symbol;

  }

  public void setName(String stockName) {
    this.stockName = stockName;
  }

  public String getStockName() {
    return stockName;
  }

  public void setStockName(String stockName) {
    this.stockName = stockName;
  }

  @Override
  public String toString() {
    return "StockCategories [ticker=" + ticker + ", stockName=" + stockName + ", categories="
        + categories + "]";
  }



}
