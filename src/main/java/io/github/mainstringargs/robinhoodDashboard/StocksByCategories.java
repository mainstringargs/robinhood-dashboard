package io.github.mainstringargs.robinhoodDashboard;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ampro.robinhood.RobinhoodApi;
import com.ampro.robinhood.endpoint.collection.data.InstrumentCollectionList;
import com.ampro.robinhood.endpoint.instrument.data.Instrument;
import com.ampro.robinhood.throwables.RobinhoodApiException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.github.mainstringargs.robinhoodDashboard.domain.StockCategories;

public class StocksByCategories {


  public static void main(String[] args) {

    RobinhoodApi api = null;
    try {
      api = new RobinhoodApi(RobinhoodProperties.getProperty("robinhood.user", ""),
          RobinhoodProperties.getProperty("robinhood.pass", ""));
    } catch (RobinhoodApiException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }


    Map<String, StockCategories> stockCatMap = new HashMap<>();

    List<String> categories = Arrays.asList("new-on-robinhood", "etf", "conglomerate",
        "manufacturing", "health", "100-most-popular", "private-equity", "healthcare", "technology",
        "payment", "electronics", "telecommunications", "consumer-product", "north-america", "us",
        "software-service", "rental-and-lease", "shipping", "material", "building-material",
        "banking", "investment-banking", "finance", "loan", "retail", "packaging", "material",
        "biotechnology", "etf", "most-popular-under-25", "road-transportation", "construction",
        "energy", "engineering", "oil", "gas", "transportation");

    for (String category : categories) {

      getInstruments(category, api, stockCatMap);
    }

    List<StockCategories> stockCategories = new ArrayList<>(stockCatMap.values());

    stockCategories.sort(new Comparator<StockCategories>() {

      @Override
      public int compare(StockCategories o1, StockCategories o2) {
        return o1.getTicker().compareTo(o2.getTicker());
      }
    });

    try (Writer writer = new FileWriter("src/main/resources/StocksByCategories.json")) {

      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      gson.toJson(stockCategories, writer);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private static void getInstruments(String catName, RobinhoodApi api,
      Map<String, StockCategories> stockCatMap) {


    try {
      System.out.println("Getting.... " + catName);

      InstrumentCollectionList instruments = null;
      instruments = (api.getCollectionData(catName));

      List<Instrument> instrumentsCollection = null;
      try {
        instrumentsCollection = instruments.getInstruments();
      } catch (RobinhoodApiException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

      System.out.println("got " + instrumentsCollection.size() + " instruments");

      for (Instrument inst : instrumentsCollection) {
        if (!stockCatMap.containsKey(inst.getSymbol())) {
          StockCategories stockCat = new StockCategories();
          stockCat.setTicker(inst.getSymbol());
          stockCat.setName(inst.getName());
          stockCatMap.put(inst.getSymbol(), stockCat);

        }

        stockCatMap.get(inst.getSymbol()).getCategories().add(catName);

        System.out.println(stockCatMap.get(inst.getSymbol()));
      }


      System.out.println("Finished Getting.... " + catName);
    } catch (Exception e) {
      System.out.println("Error getting " + catName);
    }

  }

  public static List<StockCategories> getStockCategories() {

    JsonReader jsonReader = null;
    try {
      jsonReader = new JsonReader(new FileReader("src/main/resources/StocksByCategories.json"));
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    Type listType = new TypeToken<List<StockCategories>>() {}.getType();

    List<StockCategories> jsonList = new Gson().fromJson(jsonReader, listType);

    return jsonList;

  }
}
