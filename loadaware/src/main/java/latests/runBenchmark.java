package latests;

import alluxio.Configuration;
import alluxio.PropertyKey;
import alluxio.client.file.FileSystem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yyuau on 11/7/2018.
 *
 * * Run on client
 * Pref and writes should be done on master node
 */
public class runBenchmark {

  private static ReadTest mReadTest;
  private static long mFileSize; // in MB
  private static int mFileNumber;
  private static double mAccessRate;
  private static int mRepeat;
  private static int mCacheSize;
  private static FileSystem mFS = FileSystem.Factory.get();
  private static FileWriter mTimeLog;
  private static FileWriter mHitLog;
  private static String TestType;



  /**
   *
   * parameters: Testtype; cache size (MB, of each worker); filesize (MB); filenumber; (base) accessrate;
     */
  public static void main(String[] args){
    TestType = args[0];
    mCacheSize = Integer.parseInt(args[1]);
    mFileSize = Long.parseLong(args[2]);
    mFileNumber = Integer.parseInt(args[3]);
    mAccessRate = Double.parseDouble(args[4]);
    mRepeat = Integer.parseInt(args[5]);

    System.out.println(String.format("Cache size (per worker) %s \t File size %s \t FileNumber %s \t Access rate %s \t trial %s\t", mCacheSize, mFileSize, mFileNumber, mAccessRate, mRepeat));

//    generatePref();
//    writeFiles();
    try{
      mTimeLog= new FileWriter(String.format("logs/%s-time.txt",TestType),true);
      mHitLog= new FileWriter(String.format("logs/%s-hr.txt",TestType),true);
      mReadTest = new ReadTest(mRepeat, mTimeLog);
      mReadTest.setFileNumber(mFileNumber);
      mReadTest.setRate(mAccessRate);
      mReadTest.setHitLog(mHitLog);

      mReadTest.readFiles();
      mTimeLog.close();
      mHitLog.close();

    }catch (IOException e){
      e.printStackTrace();
    }
  }

}

//  public interface  TestTypes{
//    String MicroBench = "microbench";
//    String BenchMark = "benchmark";
//    String GoogleTrace = "googletrace";
//  }

//  /**
//   * Generate users' access profile using python/generate_rates.py
//   */
//  private static void generatePref() {
//   // get lacs folder
//    boolean isCluster= Configuration.getBoolean(PropertyKey.IS_CLUSTER);
//    String curDir =  System.getProperty("user.dir");
//    if(isCluster)  // cluster mode
//      curDir = System.getProperty("user.dir") + "/lacs";
//
//
//    List<String> cmd = new ArrayList<String>();
//    cmd.add("python");
//    cmd.add(curDir + "/python/generate_rates.py");
//    cmd.add(Integer.toString(mFileNumber));
//    cmd.add(Double.toString(mAccessRate));
//
//    String[] cmdArray = cmd.toArray(new String[cmd.size()]);
//
//    // Run python and wait till it ends.
//    try {
//      Process p = Runtime.getRuntime().exec(cmdArray);
//      p.waitFor(); //  block
//    } catch (IOException | InterruptedException e) {
//      e.printStackTrace();
//    }
//
//  }
//  /**
//   *  run the corresponding python file and ask LAMaster to write the file.
//   */
//  private static void writeFiles() {
//    try {
//      mFS.runLAWrite(mCacheSize);
//
//    } catch(Exception e) {
//      e.printStackTrace();
//    }
//  }

