package alluxio.master.la_master;

import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.DeleteOptions;
import alluxio.exception.AlluxioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;


import org.isomorphism.util.*;
//import com.google.common.util.concurrent.RateLimiter.*;


/**
 * Created by yyuau on 9/7/2018.
 */
public class LoadAwareMaster {
  private static final Logger LOG = LoggerFactory.getLogger(LoadAwareMaster.class);
  private static final String CONF = "config/config.txt"; //  the file to store the config statistics: "bandwidth \n filesize \n cachesize of each worker \n mode"
  private static final String  ALLOC= "python/alloc.txt"; // the file to store the output of the python algorithm
  private static final String  ALLUXIODIR = "/tests"; // where to put test files in Alluxio
  private static final String LOCALPATH = "/test_files/local_file"; // local file for copying


  //public enum MODE {
    //LoadAware,
    //FairRide,
    //Isolation,
  //}
  public interface ModeConstants {
       String LoadAware = "LoadAware";
       String FairRide = "FairRide";
       String Isolation = "Isolation";
  }
  private static String mMode;

  private static Double mBandwidth; // of each worker
  private static Double mFileSize;
  private static Double mCacheSize; // of each worker
  private static int mWorkerCount;
  private static int mFileCount;
  private static Double mIsolateRate; // allowed TOTAL access rate per second, if a user is isolated // todo: per-worker throttling

  private static int[] mBlockList;
  private static int[] mLocation; // location of each file. Files are identified by integer ids.
  private static double[] mCacheRatio; // of each file
  private static Map<Integer, TokenBucket> mTokenPool= new ConcurrentHashMap<>(); //todo: Bucket4j might provide a more efficient implementation of the token bucket algorithm
  private static int mBucketSize = 1000; // the size of the 'leaky' token bucket

  private static Map<Integer, Integer> mWaitRequestPool = new ConcurrentHashMap<>(); // Record the number of waiting request of each user (after its tokens are used up). Need to be thread-safe.
  private static int mMaxWaitRequestNumber = 20; // The maximum waiting requests for a BLOCKED user.


  private static Map<String, Integer> mLocationMap = new ConcurrentHashMap<>(); // The map storing the actual locations of Alluxio files.

  public LoadAwareMaster(int workerCount){

    // Get the allocation, and initialize the token buckets and waiting numbers.
    // getAlloction();


    // Write the files based on the allocation results.
    // writeFile();
  }

  public static void setWorkerCount(int workerCount){
    mWorkerCount = workerCount;
  }

  /**
   * Run the python algorithm to get the cache allocation and block list.
   *
   * The output of the python program should be written to python/alloc.txt
   *
   * The access counts (frequency of the previous period) should be logged to config/pop.txt
   */

  public static void getAlloction() {
    try (BufferedReader br = new BufferedReader(new FileReader(CONF))) { //todo: check whether the path is correct. //we may need to launch the LoadAwareMaster in the alluxio root folder
      mBandwidth = Double.parseDouble(br.readLine());
      mFileSize = Double.parseDouble(br.readLine());
      mCacheSize = Double.parseDouble(br.readLine());
      mMode = br.readLine();
      br.close();
    } catch (IOException e) {
      LOG.info("Four parameters in config/config.txt are required: bandwidth, filesize, cachesize, and mode, separated in lines.");
      e.printStackTrace();
    }
    mIsolateRate = mBandwidth / mFileSize * mWorkerCount;// ; //total rate per second

    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("python");
    String currentDirectory = System.getProperty("user.dir");
    System.out.println("Current dir: " + currentDirectory);
    switch(mMode){
      case ModeConstants.LoadAware: cmd.add(currentDirectory + "/python/la_fair_allocator.py");break;
      case ModeConstants.FairRide: cmd.add(currentDirectory + "/python/fairRide_allocator.py");break;
      case ModeConstants.Isolation: cmd.add(currentDirectory + "/python/isolation_allocator.py");break;
      default: cmd.add(currentDirectory + "/python/la_fair_allocator.py");break;
    }
    cmd.add(mBandwidth.toString());
    cmd.add(mFileSize.toString());
    cmd.add(mCacheSize.toString());

    // The python algorithm will read the log file of user access frequencies and output the cache allocation (position+ratio)
    // and the block list, all separated by commas. The path of the output file should be python/alloc.txt

    LOG.info("cmd: " + cmd);
    // convert cmd to string[]
    String[] cmdArray = cmd.stream().toArray(String[]::new);

    // Run la_fair_allocator.py and parse the results
    try {
      Process p = Runtime.getRuntime().exec(cmdArray);
      //BufferedReader stdInput = new BufferedReader(new
              //InputStreamReader(p.getInputStream()));
      //String[] locationArray = stdInput.readLine().split(",");

      BufferedReader br = new BufferedReader(new FileReader(ALLOC)); //todo: check whether the path is correct. //we may need to launch the LoadAwareMaster in the alluxio root folder
      String[] locationArray = br.readLine().split(",");
      mLocation = Arrays.stream(locationArray).mapToInt(Integer::parseInt).toArray();
      String[] ratioArray = br.readLine().split(",");
      mCacheRatio = Arrays.stream(ratioArray).mapToDouble(Double::parseDouble).toArray();
      String[] blockArray = br.readLine().split(",");
      mBlockList = Arrays.stream(blockArray).mapToInt(Integer::parseInt).toArray();
      br.close();

      // log the results for debugging
      LOG.info("################Allocation results################");
      LOG.info("1. Locations" + Arrays.toString(mLocation));
      LOG.info("2 Ratios" + Arrays.toString(mCacheRatio));
      LOG.info("3. BlockList" + Arrays.toString(mBlockList));

    } catch (Exception e) {
      LOG.info("Wrong Message received: " + e);
      return;
    }
    mFileCount=mLocation.length;

    for(Integer useId: mBlockList){
      TokenBucket tokenBucket = TokenBuckets.builder()
              .withCapacity(mBucketSize)
              .withFixedIntervalRefillStrategy(mIsolateRate.longValue()*60, 1, TimeUnit.MINUTES) // the isolate rate must be larger than 1/60 accesses per second.
              .build();
      mTokenPool.put(useId, tokenBucket);
      mWaitRequestPool.put(useId, 0);
    }
  }

  public static void writeFile(){

    FileSystem fs = FileSystem.Factory.get();
    LoadAwareFileWriter fw = new LoadAwareFileWriter(fs);

    AlluxioURI writeDir = new AlluxioURI(ALLUXIODIR);
    try {
      if (fs.exists(writeDir)) { // delete existing dir
        fs.delete(writeDir, DeleteOptions.defaults().setRecursive(true).setUnchecked(true));
      }
      FileInputStream is = new FileInputStream(LOCALPATH);
      int fileLength =  (int) new File(LOCALPATH).length();
      byte[] buf = new byte[fileLength];
      is.read(buf);
      is.close();

      // AlluxioURI[] shortcuts = new AlluxioURI[mWorkerCount]; // shortcuts for local copy!
      for (int fileId = 0; fileId < mFileCount; fileId++) {
        int workerId = mLocation[fileId];
        double cacheRatio = mCacheRatio[fileId];
        fw.setmCacheRatio(cacheRatio);
        fw.setmWorkerId(workerId);
        String dstFile = String.format("%s/%s", ALLUXIODIR, fileId);
        fw.setmDstFile(dstFile);
        fw.writeFile(buf);
        mLocationMap.put(dstFile,workerId);
      }
    } catch (IOException | AlluxioException e) {
      e.printStackTrace();
    }


  }

  /**
   *  To issue tokens.
   * @param userId
   * @param fileName intended for per-machine access control, together with the locationmap.
   * @return
   */
  public static boolean access(String fileName, int userId) {
    if(!Arrays.asList(mBlockList).contains(userId))
      return true;
    else{
      if(mWaitRequestPool.get(userId) > mMaxWaitRequestNumber) // too many requests waiting
        return false;
      else{ // ask for a token
        TokenBucket tokenBucket = mTokenPool.get(userId);
        mWaitRequestPool.put(userId, mWaitRequestPool.get(userId)+1); // one more waiting
        tokenBucket.consume(1); // block
        mWaitRequestPool.put(userId, mWaitRequestPool.get(userId)-1); // one finishes waiting
        return true;
      }
    }
  }
}


