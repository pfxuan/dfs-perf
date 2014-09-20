package pasalab.dfs.perf.benchmark.massive;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import pasalab.dfs.perf.basic.PerfThread;
import pasalab.dfs.perf.basic.TaskConfiguration;
import pasalab.dfs.perf.benchmark.ListGenerator;
import pasalab.dfs.perf.benchmark.Operators;
import pasalab.dfs.perf.conf.PerfConf;
import pasalab.dfs.perf.fs.PerfFileSystem;

public class MassiveThread extends PerfThread {
  private Random mRand;

  private int mBasicFilesNum;
  private int mBlockSize;
  private int mBufferSize;
  private long mFileLength;
  private PerfFileSystem mFileSystem;
  private String mReadType;
  private long mLimitTimeMs;
  private String mWorkDir;
  private String mWriteType;

  private double mBasicWriteThroughput; // in MB/s
  private double mReadThroughput; // in MB/s
  private boolean mSuccess;
  private double mWriteThroughput; // in MB/s

  public double getBasicWriteThroughput() {
    return mBasicWriteThroughput;
  }

  public double getReadThroughput() {
    return mReadThroughput;
  }

  public boolean getSuccess() {
    return mSuccess;
  }

  public double getWriteThroughput() {
    return mWriteThroughput;
  }

  private void initSyncBarrier() throws IOException {
    mFileSystem.createEmptyFile(mWorkDir + "/sync/" + mTaskId + "-" + mId);
  }

  private void syncBarrier() throws IOException {
    String syncDirPath = mWorkDir + "/sync/";
    String syncFileName = mTaskId + "-" + mId;
    mFileSystem.delete(syncDirPath + "/" + syncFileName, false);
    while (!mFileSystem.listFullPath(syncDirPath).isEmpty()) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        LOG.error("Error in Sync Barrier", e);
      }
    }
  }

  @Override
  public void run() {
    long basicBytes = 0;
    long basicTimeMs = 0;
    long readBytes = 0;
    long readTimeMs = 0;
    long writeBytes = 0;
    long writeTimeMs = 0;
    mSuccess = true;
    String dataDir = mWorkDir + "/data";
    String tmpDir = mWorkDir + "/tmp";

    long tTimeMs = System.currentTimeMillis();
    for (int b = 0; b < mBasicFilesNum; b ++) {
      try {
        String fileName = mTaskId + "-" + mId + "-" + b;
        Operators.writeSingleFile(mFileSystem, dataDir + "/" + fileName, mFileLength, mBlockSize,
            mBufferSize, mWriteType);
        basicBytes += mFileLength;
      } catch (IOException e) {
        LOG.error("Failed to write basic file", e);
        mSuccess = false;
        break;
      }
    }
    tTimeMs = System.currentTimeMillis() - tTimeMs;
    basicTimeMs += tTimeMs;

    try {
      syncBarrier();
    } catch (IOException e) {
      LOG.error("Error in Sync Barrier", e);
      mSuccess = false;
    }
    int index = 0;
    long limitTimeMs = System.currentTimeMillis() + mLimitTimeMs;
    while ((tTimeMs = System.currentTimeMillis()) < limitTimeMs) {
      if (mRand.nextBoolean()) { // read
        try {
          List<String> candidates = mFileSystem.listFullPath(dataDir);
          String readFilePath = ListGenerator.generateRandomReadFiles(1, candidates).get(0);
          readBytes += Operators.readSingleFile(mFileSystem, readFilePath, mBufferSize, mReadType);
        } catch (IOException e) {
          LOG.error("Failed to read file", e);
          mSuccess = false;
          break;
        }
        readTimeMs += System.currentTimeMillis() - tTimeMs;
      } else { // write
        try {
          String writeFileName = mTaskId + "-" + mId + "--" + index;
          Operators.writeToTmpAndRename(mFileSystem, tmpDir + "/" + writeFileName, dataDir + "/"
              + writeFileName, mFileLength, mBlockSize, mBufferSize, mWriteType);
          writeBytes += mFileLength;
        } catch (IOException e) {
          LOG.error("Failed to write file", e);
          mSuccess = false;
          break;
        }
        writeTimeMs += System.currentTimeMillis() - tTimeMs;
      }
      index ++;
    }

    mBasicWriteThroughput =
        (basicBytes == 0) ? 0 : (basicBytes / 1024.0 / 1024.0) / (basicTimeMs / 1000.0);
    mReadThroughput = (readBytes == 0) ? 0 : (readBytes / 1024.0 / 1024.0) / (readTimeMs / 1000.0);
    mWriteThroughput =
        (writeBytes == 0) ? 0 : (writeBytes / 1024.0 / 1024.0) / (writeTimeMs / 1000.0);
  }

  @Override
  public boolean setupThread(TaskConfiguration taskConf) {
    mRand = new Random(System.currentTimeMillis() + mTaskId + mId);
    mBufferSize = taskConf.getIntProperty("buffer.size.bytes");
    mBlockSize = taskConf.getIntProperty("block.size.bytes");
    mFileLength = taskConf.getLongProperty("file.length.bytes");
    mLimitTimeMs = taskConf.getLongProperty("time.seconds") * 1000;
    mReadType = taskConf.getProperty("read.type");
    mWorkDir = taskConf.getProperty("work.dir");
    mBasicFilesNum = taskConf.getIntProperty("basic.files.per.thread");
    mWriteType = taskConf.getProperty("write.type");
    try {
      mFileSystem = Operators.connect((PerfConf.get().DFS_ADDRESS));
      initSyncBarrier();
    } catch (IOException e) {
      LOG.error("Failed to setup thread, task " + mTaskId + " - thread " + mId, e);
      return false;
    }
    mSuccess = false;
    mBasicWriteThroughput = 0;
    mReadThroughput = 0;
    mWriteThroughput = 0;
    return true;
  }

  @Override
  public boolean cleanupThread(TaskConfiguration taskConf) {
    try {
      Operators.close(mFileSystem);
    } catch (IOException e) {
      LOG.warn("Error when close file system, task " + mTaskId + " - thread " + mId, e);
    }
    return true;
  }

}
