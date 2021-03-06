package pasalab.dfs.perf.tools;

import java.io.File;
import java.io.IOException;

import pasalab.dfs.perf.basic.PerfTaskContext;
import pasalab.dfs.perf.basic.PerfTotalReport;
import pasalab.dfs.perf.basic.TestCase;
import pasalab.dfs.perf.conf.PerfConf;

/**
 * Generate a total report for the specified test.
 */
public class DfsPerfCollector {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("Wrong program arguments. Should be <TestCase> <reports dir>");
      System.exit(-1);
    }

    try {
      PerfTotalReport summaryReport = TestCase.get().getTotalReportClass(args[0]);
      summaryReport.initialSet(args[0]);
      File contextsDir = new File(args[1]);
      File[] contextFiles = contextsDir.listFiles();
      if (contextFiles == null || contextFiles.length == 0) {
        throw new IOException("No task context files exists under " + args[1]);
      }
      PerfTaskContext[] taskContexts = new PerfTaskContext[contextFiles.length];
      for (int i = 0; i < contextFiles.length; i ++) {
        taskContexts[i] = TestCase.get().getTaskContextClass(args[0]);
        taskContexts[i].loadFromFile(contextFiles[i]);
      }
      summaryReport.initialFromTaskContexts(taskContexts);
      String outputFileName = PerfConf.get().OUT_FOLDER + "/DfsPerfReport-" + args[0];
      summaryReport.writeToFile(new File(outputFileName));
      System.out.println("Report generated at " + outputFileName);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Failed to generate Dfs-Perf-Report");
      System.exit(-1);
    }
  }
}
