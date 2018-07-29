package latests;

/**
 * Created by yyuau on 22/7/2018.
 */

import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.DeleteOptions;
import alluxio.exception.AlluxioException;
import alluxio.master.la_master.LoadAwareFileWriter;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;


import org.isomorphism.util.*;
//import com.google.common.util.concurrent.RateLimiter.*;


/**
 * Created by yyuau on 9/7/2018.
 */
public class PrepareTests {
  private static final Logger LOG = LoggerFactory.getLogger(latests.PrepareTests.class);
  private static final String CONF = "config/config.txt"; //  the file to store the config statistics: "bandwidth \n filesize \n cachesize of each worker \n mode"
  private static final String ALLOC = "alloc.txt"; // the file to store the output of the python algorithm
  private static final String ALLUXIODIR = "/tests"; // where to put test files in Alluxio
  private static final String LOCALPATH = "/test_files/local_file"; // local file for copying

  public interface ModeConstants {
    String MaxMinDefault = "MaxMinDefault";
    String LoadAware = "LoadAware";
    String MaxMinOptLatency = "MaxMinOptLatency";
    String Isolation = "Isolation";
  }

  private static String mMode;

  private static Double mBandwidth; // of each worker
  private static Double mFileSize;
  private static Double mCacheSize; // of each worker
  private static int mWorkerCount;
  private static int mFileCount;
  private static Double mDelta; // disk delay

  private static Double mIsolateRate; // allowed TOTAL access rate per second, if a user is isolated // todo: per-worker throttling

  private static List<Integer> mBlockList = new ArrayList<Integer>();
  private static List<Integer> mLocation = new ArrayList<Integer>(); // location of each file. Files are identified by integer ids.
  private static List<Double> mCacheRatio = new ArrayList<Double>(); // of each file
  private static Map<Integer, TokenBucket> mTokenPool = new ConcurrentHashMap<>(); //todo: Bucket4j might provide a more efficient implementation of the token bucket algorithm
  private static int mBucketSize = 1000; // the size of the 'leaky' token bucket

  private static Map<Integer, Integer> mWaitRequestPool = new ConcurrentHashMap<>(); // Record the number of waiting request of each user (after its tokens are used up). Need to be thread-safe.
  private static int mMaxWaitRequestNumber = 20; // The maximum waiting requests for a BLOCKED user.
  private static Map<String, Integer> mLocationMap = new ConcurrentHashMap<>(); // The map storing the actual locations of Alluxio files.

  public PrepareTests() {

    // Get the allocation, and initialize the token buckets and waiting numbers.
    // getAlloction();


    // Write the files based on the allocation results.
    // writeFile();
  }

  public static void setWorkerCount(int workerCount) {
    mWorkerCount = workerCount;
    System.out.println("worker count set in lamaster: " + workerCount);
  }

  public static void setmDelta(Double delta) {
    mDelta = delta;
    System.out.println("Delta is set in lamaster: " + delta);
  }

  public static void getDelta() {
    try{
      BufferedReader br = new BufferedReader(new FileReader("delta.txt"));
      mDelta = Double.parseDouble(br.readLine());
      br.close();
    } catch(Exception e){
      e.printStackTrace();
    }

    System.out.println("Delta is profiled: " + mDelta);
  }
  /**
   * Run the python algorithm to get the cache allocation and block list.
   *
   * The output of the python program should be written to python/alloc.txt
   */

  public static void getAllocation() {
    try (BufferedReader br = new BufferedReader(new FileReader(CONF))) { //todo: check whether the path is correct. //we may need to launch the LoadAwareMaster in the alluxio root folder
      mBandwidth = Double.parseDouble(br.readLine());
      mFileSize = Double.parseDouble(br.readLine());
      mCacheSize = Double.parseDouble(br.readLine());
      mMode = br.readLine();
      mWorkerCount = Integer.parseInt(br.readLine());
      br.close();
    } catch (IOException e) {
      LOG.info("Five parameters in config/config.txt are required: bandwidth, filesize, cachesize, delta(speed difference of memory and disk), and mode, separated in lines.");
      e.printStackTrace();
    }
    mIsolateRate = mBandwidth / mFileSize * mWorkerCount;// ; //total rate per second

    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("python");
    String currentDirectory = System.getProperty("user.dir");
    System.out.println("Current dir: " + currentDirectory);
    switch (mMode) {
      case latests.PrepareTests.ModeConstants.MaxMinDefault:
        cmd.add(currentDirectory + "/python/mm_default.py");
        break;
      case latests.PrepareTests.ModeConstants.LoadAware:
        cmd.add(currentDirectory + "/python/la_fair.py");
        break;
      case latests.PrepareTests.ModeConstants.MaxMinOptLatency:
        cmd.add(currentDirectory + "/python/mm_opt.py");
        break;
      case latests.PrepareTests.ModeConstants.Isolation:
        cmd.add(currentDirectory + "/python/isolation.py");
        break;
      default:
        cmd.add(currentDirectory + "/python/la_fair_allocator.py");
        break;
    }
    cmd.add(mBandwidth.toString());
    cmd.add(Integer.toString(mWorkerCount)); // Note: this is not read from the config
    cmd.add(mFileSize.toString());
    cmd.add(mCacheSize.toString());
    getDelta();
    cmd.add(mDelta.toString());

    // The python algorithm will read the log file of user access frequencies and output the cache allocation (position+ratio)
    // and the block list, all separated by commas. The path of the output file should be python/alloc.txt

    LOG.info("cmd: " + cmd);
    // convert cmd to string[]
    //String[] cmdArray = cmd.stream().toArray(String[]::new); // for java 1.8+
    String[] cmdArray = cmd.toArray(new String[cmd.size()]);

    // Run la_fair_allocator.py and parse the results
    try {
      Process p = Runtime.getRuntime().exec(cmdArray);
      //BufferedReader stdInput = new BufferedReader(new
      //InputStreamReader(p.getInputStream()));
      //String[] locationArray = stdInput.readLine().split(",");

      BufferedReader br = new BufferedReader(new FileReader(ALLOC)); //todo: check whether the path is correct. //we may need to launch the LoadAwareMaster in the alluxio root folder
      String[] locationArray = br.readLine().split(",");
      // mLocation = Arrays.stream(locationArray).mapToInt(Integer::parseInt).toArray(); // for jave 1.8+
      int i = 0;
      for (String loc : locationArray) {
        mLocation.add(Integer.parseInt(loc));//Exception in this line
        i++;
      }
      String[] ratioArray = br.readLine().split(",");
      //mCacheRatio = Arrays.stream(ratioArray).mapToDouble(Double::parseDouble).toArray();
      i = 0;
      for (String ratio : ratioArray) {
        mCacheRatio.add(Double.parseDouble(ratio));//Exception in this line
        i++;
      }

      String blockList = br.readLine();
      System.out.println(blockList);
      if(!blockList.equals("")) {
        String[] blockArray = blockList.split(",");
        //mBlockList = Arrays.stream(blockArray).mapToInt(Integer::parseInt).toArray();
        i = 0;
        // blockArray could be empty.
        for (String id : blockArray) {
          mBlockList.add(Integer.parseInt(id));//Exception in this line
          i++;
        }
        LOG.info("BlockList" + Arrays.toString(mBlockList.toArray()));
      }
      br.close();

      // log the results for debugging
      LOG.info("################Allocation results################");
      LOG.info("Locations" + Arrays.toString(mLocation.toArray())); //Arrays.toString(mLocation));
      LOG.info("Ratios" + Arrays.toString(mCacheRatio.toArray()));

    } catch (Exception e) {
      LOG.info(String.format("%s: %s",e.getStackTrace()[0].getLineNumber(), e));
      return;
    }
    mFileCount = mLocation.size();

    for (Integer useId : mBlockList) {
      TokenBucket tokenBucket = TokenBuckets.builder()
              .withCapacity(mBucketSize)
              .withFixedIntervalRefillStrategy(mIsolateRate.longValue() * 60, 1, TimeUnit.MINUTES) // the isolate rate must be larger than 1/60 accesses per second.
              .build();
      mTokenPool.put(useId, tokenBucket);
      mWaitRequestPool.put(useId, 0);
    }
  }

  public static void writeFile() {

    FileSystem fs = FileSystem.Factory.get();
    //LoadAwareFileWriter fw = new LoadAwareFileWriter(fs);

    AlluxioURI writeDir = new AlluxioURI(ALLUXIODIR);
    try {
      if (fs.exists(writeDir)) { // delete existing dir
        fs.delete(writeDir, DeleteOptions.defaults().setRecursive(true).setUnchecked(true));
      }
      FileInputStream is = new FileInputStream(LOCALPATH);
      int fileLength = (int) new File(LOCALPATH).length();
      byte[] buf = new byte[fileLength];
      is.read(buf);
      is.close();

      // AlluxioURI[] shortcuts = new AlluxioURI[mWorkerCount]; // shortcuts for local copy!
      for (int fileId = 0; fileId < mFileCount; fileId++) {
        LoadAwareFileWriter fw= new LoadAwareFileWriter(fs);
        Thread t = new Thread(fw);
        int workerId = mLocation.get(fileId);
        double cacheRatio = mCacheRatio.get(fileId);
        fw.setmCacheRatio(cacheRatio);
        fw.setmWorkerId(workerId);
        String dstFile = String.format("%s/%s", ALLUXIODIR, fileId);
        fw.setmDstFile(dstFile);
        fw.setBuf(buf);
        t.start();
        mLocationMap.put(dstFile, workerId);
      }
    } catch (IOException | AlluxioException e) {
      e.printStackTrace();
    }


  }
}