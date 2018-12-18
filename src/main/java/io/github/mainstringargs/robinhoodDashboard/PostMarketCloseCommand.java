package io.github.mainstringargs.robinhoodDashboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class PostMarketCloseCommand {

  private static String commandToRun =
      RobinhoodProperties.getProperty("postMarketClose.command", "");
  private static String workingDirectory =
      RobinhoodProperties.getProperty("postMarketClose.workingDirectory", "");

  public static void runCommand() {

    if (!commandToRun.isEmpty()) {

      ProcessBuilder pb = new ProcessBuilder(commandToRun.split(" "));
      pb.directory(new File(workingDirectory));
      pb.redirectErrorStream(true);

      Process p = null;

      try {
        p = pb.start();
      } catch (IOException e) {
        e.printStackTrace();
      }
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String readline;
      int i = 0;
      try {
        while ((readline = reader.readLine()) != null) {
          System.err.println(++i + " " + readline);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    PostMarketCloseCommand.runCommand();

  }
}
