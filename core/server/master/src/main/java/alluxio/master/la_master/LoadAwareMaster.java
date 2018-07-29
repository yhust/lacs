package alluxio.master.la_master;

import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.DeleteOptions;
import alluxio.exception.AlluxioException;
import alluxio.master.file.DefaultFileSystemMaster;
import alluxio.util.CommonUtils;
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
public class LoadAwareMaster {
  private static final Logger LOG = LoggerFactory.getLogger(LoadAwareMaster.class);
  //private final static FileWriter mCacheHitLog = createLogWriter("logs/cacheHit_master.txt"); // user_id \t cache bytes \t disk bytes \n
  private static final String  ALLUXIODIR = "/tests"; // where to put test files in Alluxio
  private static String curDir = System.getProperty("user.dir");
  private static String CONF ; //  the file to store the config statistics: "bandwidth \n filesize \n cachesize of each worker \n mode"
  private static String  ALLOC; // the file to store the output of the python algorithm
  private static String LOCALPATH; // local file for copying

  //private static final FileWriter WorkerLoads = createLogWriter("logs/workerloads.txt");


  //public enum MODE {
    //LoadAware,
    //MaxMin,
    //Isolation,
  //}
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
  private static int mUserCount;
  private static int mFileCount;
  private static Double mDelta; // disk delay

  private static Double mIsolateRate; // allowed TOTAL access rate per second, if a user is isolated // todo: per-worker throttling

  private static List<Integer> mBlockList = new ArrayList<Integer>();
  private static List<Integer> mLocation = new ArrayList<Integer>(); // location of each file. Files are identified by integer ids.
  private static List<Double> mCacheRatio = new ArrayList<Double>(); // of each file
  private static Map<Integer, TokenBucket> mTokenPool= new ConcurrentHashMap<>(); //todo: Bucket4j might provide a more efficient implementation of the token bucket algorithm
  private static int mBucketSize; // the size of the 'leaky' token bucket

  private static Map<Integer, Integer> mWaitRequestPool = new ConcurrentHashMap<>(); // Record the number of waiting request of each user (after its tokens are used up). Need to be thread-safe.
  private static int mMaxWaitRequestNumber = 400; // The maximum waiting requests for a BLOCKED user.
  private static DefaultFileSystemMaster mFSMaster;


  private static Map<String, Integer> mLocationMap = new ConcurrentHashMap<>(); // The map storing the actual locations of Alluxio files.

  public LoadAwareMaster(DefaultFileSystemMaster master){
    mFSMaster = master;
    // Get the allocation, and initialize the token buckets and waiting numbers.
    // getAlloction();


    // Write the files based on the allocation results.
    // writeFile();
  }

  private static FileWriter createLogWriter(final String name) {
    try {
      return new FileWriter(name, true); //append;
    } catch (final IOException exc) {
      throw new Error(exc);
    }
  }
  public static void setWorkerCount(int workerCount){
    mWorkerCount = workerCount;
    System.out.println("worker count set in lamaster: "+ workerCount);
  }
  public static void getWorkerCount(){
    mWorkerCount = mFSMaster.getWorkerCount();
    System.out.println("worker count get from fs master: "+ mWorkerCount);
  }
  public static void setmDelta(Double delta){
    mDelta = delta;
    System.out.println("Delta is set in lamaster: "+ delta);
  }

  public static void getDelta() {
    try{
      BufferedReader br=new BufferedReader(new FileReader(curDir + "/delta.txt"));
      mDelta = Double.parseDouble(br.readLine());
      br.close();
    } catch(Exception e){
      e.printStackTrace();
    }

    System.out.println("Delta is profiled: " + mDelta);
  }

  /**
   * Get allocation and write files.
   */

  public static void runWrite(){
    getConfig();
    getAllocation();
    writeFile();
  }


  /**
   * Read the configurations
   */
  public static void getConfig(){
    getWorkerCount();
    if(mWorkerCount>1) {
      // cluster mode
      curDir = System.getProperty("user.dir") + "/alluxio-la";
    }
    CONF = curDir+"/config/config.txt"; //  the file to store the config statistics: "bandwidth \n filesize \n cachesize of each worker \n mode"
    ALLOC= curDir+"/alloc.txt"; // the file to store the output of the python algorithm
    LOCALPATH = curDir + "/test_files/local_file"; // local file for copying

    try (BufferedReader br = new BufferedReader(new FileReader(CONF))) {
      mBandwidth = Double.parseDouble(br.readLine());
      mFileSize = Double.parseDouble(br.readLine());
      mCacheSize = Double.parseDouble(br.readLine());
      mUserCount = Integer.parseInt(br.readLine());
      mMode = br.readLine();
      try{
        mMaxWaitRequestNumber = Integer.parseInt(br.readLine());
      }catch(Exception e){}
      br.close();
    } catch (IOException e) {
      LOG.info("Five parameters in config/config.txt are required: bandwidth, filesize, cachesize, usercount, and mode, separated in lines.");
      e.printStackTrace();
    }
    mIsolateRate = mBandwidth / mUserCount / mFileSize * mWorkerCount ;// ; //total rate per second
    System.out.println("Isolate rate: " + mIsolateRate);

  }


  /**
   * Run the python algorithm to get the cache allocation and block list.
   *
   * The output of the python program should be written to alloc.txt
   *
   *
   */

  public static void getAllocation() {
    //LOG.info("Current dir: " + curDir);
    ArrayList<String> cmd = new ArrayList<String>();
    System.out.println("Worker count : " + mWorkerCount);
    if(mWorkerCount>1) {
      cmd.add("/usr/bin/python2.7");
    }
    else{
      cmd.add("python");
    }
    switch(mMode){
      case ModeConstants.MaxMinDefault: cmd.add(curDir + "/python/mm_default.py");break;
      case ModeConstants.LoadAware: cmd.add(curDir + "/python/la_fair.py");break;
      case ModeConstants.MaxMinOptLatency: cmd.add(curDir + "/python/mm_opt.py");break;
      case ModeConstants.Isolation: cmd.add(curDir + "/python/isolation.py");break;
      default: cmd.add(curDir + "/python/la_fair_allocator.py");break;
    }
    cmd.add(mBandwidth.toString());
    cmd.add(Integer.toString(mWorkerCount)); //get worker count
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

      BufferedReader br = new BufferedReader(new FileReader(ALLOC));
      String[] locationArray = br.readLine().split(",");
      // mLocation = Arrays.stream(locationArray).mapToInt(Integer::parseInt).toArray(); // for jave 1.8+
      int i=0;
      mLocation.clear();
      for(String loc:locationArray){
        mLocation.add(Integer.parseInt(loc));//Exception in this line
        i++;
      }
      String[] ratioArray = br.readLine().split(",");
      //mCacheRatio = Arrays.stream(ratioArray).mapToDouble(Double::parseDouble).toArray();
      i=0;
      mCacheRatio.clear();
      for(String ratio:ratioArray){
        mCacheRatio.add(Double.parseDouble(ratio));//Exception in this line
        i++;
      }
      String blockList = br.readLine();
      System.out.println(blockList);
      mBlockList.clear();
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
      LOG.info("Locations" + Arrays.toString(mLocation.toArray()));
      LOG.info("Ratios" + Arrays.toString(mCacheRatio.toArray()));

    } catch (IOException e) {
      LOG.info("Wrong Message received: " + e);
      return;
    }
    mFileCount=mLocation.size();

    mBucketSize= 1; //mIsolateRate.intValue();
    mTokenPool.clear();
    for(Integer useId: mBlockList){
      TokenBucket tokenBucket = TokenBuckets.builder()
              .withCapacity(mBucketSize)
              .withFixedIntervalRefillStrategy(mIsolateRate.longValue(), 1, TimeUnit.SECONDS) // the isolate rate must be larger than 1/60 accesses per second.
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
        int workerId = mLocation.get(fileId);
        double cacheRatio = mCacheRatio.get(fileId);
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
   * @return the worker id. If the request is rejected, return -1
   */
  //todo The access counts (frequency of the previous period) should be logged to python/pop.txt
  public static int access(String fileName, int userId){
    //LOG.info("User id received at the lacs master " + userId);
    System.out.println("Block list: " + Arrays.toString(mBlockList.toArray()));
    if(mMode.equals(ModeConstants.Isolation)){
      System.out.println("Isolation mode");
      if(mWaitRequestPool.get(userId) > mMaxWaitRequestNumber) { // too many requests waiting
        LOG.info("Isolation mode -- More than" + mMaxWaitRequestNumber + "waiting requests: Reject");
        return -1;
      }
      else{ // ask for a token
        TokenBucket tokenBucket = mTokenPool.get(userId);
        mWaitRequestPool.put(userId, mWaitRequestPool.get(userId)+1); // one more waiting
        LOG.info("Isolation mode --  Asking for token");
        tokenBucket.consume(1); // block
        LOG.info("Isolation mode --  Token get " + CommonUtils.getCurrentMs());
        mWaitRequestPool.put(userId, mWaitRequestPool.get(userId)-1); // one finishes waiting
//        try {
//          synchronized (WorkerLoads){
//            WorkerLoads.write(String.format("%s:\t%s\n",fileName, mLocation.get(Integer.parseInt(fileName))));
//            WorkerLoads.flush();
//          }
//        }catch(IOException e){
//          e.printStackTrace();
//        }
        return mLocation.get(Integer.parseInt(fileName));
      }
    }

    if(!mBlockList.contains(userId)){ // not in the list
      System.out.println("User " + userId + " not in the block list");
//      try {
//        synchronized (WorkerLoads) {
//          WorkerLoads.write(String.format("%s:\t%s\n", fileName, mLocation.get(Integer.parseInt(fileName))));
//          WorkerLoads.flush();
//        }
//      }catch(IOException e){
//        e.printStackTrace();
//      }
      return mLocation.get(Integer.parseInt(fileName));
    }

    else{
      if(mWaitRequestPool.get(userId) > mMaxWaitRequestNumber) { // too many requests waiting
        LOG.info("LACS mode -- More than" + mMaxWaitRequestNumber + "waiting requests: Reject");
        return -1;
      }
      else{ // ask for a token
        TokenBucket tokenBucket = mTokenPool.get(userId);
        mWaitRequestPool.put(userId, mWaitRequestPool.get(userId)+1); // one more waiting
        LOG.info("Isolation mode --  Asking for token");
        tokenBucket.consume(1); // block
        LOG.info("Isolation mode --  Token get " + CommonUtils.getCurrentMs());
        mWaitRequestPool.put(userId, mWaitRequestPool.get(userId)-1); // one finishes waiting
//        try {
//          synchronized (WorkerLoads){
//            WorkerLoads.write(String.format("%s:\t%s\n",fileName, mLocation.get(Integer.parseInt(fileName))));
//            WorkerLoads.flush();
//          }
//        }catch(IOException e){
//          e.printStackTrace();
//        }
        return mLocation.get(Integer.parseInt(fileName));
      }
    }
  }

}


