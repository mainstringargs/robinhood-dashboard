package io.github.mainstringargs.robinhoodDashboard.domain;

import java.util.LinkedHashSet;
import java.util.Set;

public class StockCategories {

  private String ticker;
  private String stockName;
  private Set<String> categories = new LinkedHashSet<>();



  public String getTicker() {
    return ticker;
  }

  public Set<String> getCategories() {
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
