package io.github.mainstringargs.robinhoodDashboard;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import io.github.mainstringargs.robinhoodDashboard.domain.StockCategories;
import io.github.mainstringargs.stockData.spi.StockDataService;
import io.github.mainstringargs.stockData.spi.StockDataServiceLoader;

public class StockReport {

  public static void main(String[] args) throws IOException {


    List<StockCategories> rhStocks = StocksByCategories.getStockCategories();


    Map<String, StockDataService> services = StockDataServiceLoader.getStockDataServices();

    for (StockDataService service : services.values()) {
      service.init();
    }

    System.out.println(rhStocks.size());

    TreeMap<String, LinkedHashMap<String, Object>> stockData = new TreeMap<>();

    LinkedHashSet<String> keySet = new LinkedHashSet<>();

    keySet.add("Ticker");

    for (StockCategories stock : rhStocks) {

      for (StockDataService service : services.values()) {
        try {
          Map<String, Object> stockDataFromService = (service.getStockData(stock.getTicker()));


          if (stockDataFromService.containsKey(stock.getTicker())) {
            LinkedHashMap<String, Object> map = stockData.get(stock.getTicker());

            if (map == null) {
              map = new LinkedHashMap<>();
              stockData.put(stock.getTicker(), map);
            }

            for (Entry<String, Object> entry : stockDataFromService.entrySet()) {

              String key = service.getShortServiceName() + "-" + entry.getKey();
              keySet.add(key);

              map.put(key, entry.getValue());
            }


            System.out.println(service.getServiceName() + " " + stock.getTicker() + " " + map);

          }
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }


    }


    System.out.println("Producing results for " + stockData.size() + " stocks ");

    System.out.println("\n=============\n");



    String csvFile =
        "rhStockReport-" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date()) + ".csv";


    final FileWriter writer = new FileWriter(csvFile);


    try {
      CSVUtils.writeLine(writer, new ArrayList<String>(keySet));
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }


    stockData.forEach((key, myRating) -> {
      LinkedHashMap<String, Object> myData = stockData.get(key);

      List<String> values = new ArrayList<String>();


      for (String header : keySet) {
        Object data = myData.get(header);

        if (header.equals("Ticker")) {

          values.add(key);
        } else {
          if (data != null) {
            values.add(data.toString());
          } else {
            values.add("");
          }
        }
      }

      try {
        CSVUtils.writeLine(writer, values);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    });

    try {
      writer.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
