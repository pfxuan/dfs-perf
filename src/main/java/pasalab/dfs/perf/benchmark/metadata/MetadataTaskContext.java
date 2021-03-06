package pasalab.dfs.perf.benchmark.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pasalab.dfs.perf.basic.PerfThread;
import pasalab.dfs.perf.benchmark.SimpleTaskContext;

public class MetadataTaskContext extends SimpleTaskContext {
  @Override
  public void setFromThread(PerfThread[] threads) {
    mAdditiveStatistics = new HashMap<String, List<Double>>(1);
    List<Double> rates = new ArrayList<Double>(threads.length);
    for (PerfThread thread : threads) {
      if (!((MetadataThread) thread).getSuccess()) {
        mSuccess = false;
      }
      rates.add(((MetadataThread) thread).getRate());
    }
    mAdditiveStatistics.put("ResponseRate(ops/sec)", rates);
  }
}
