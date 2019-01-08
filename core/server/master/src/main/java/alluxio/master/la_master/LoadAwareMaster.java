package alluxio.master.la_master;

import alluxio.AlluxioURI;
import alluxio.Configuration;
import alluxio.PropertyKey;
import alluxio.client.ReadType;
import alluxio.client.WriteType;
import alluxio.client.block.policy.BlockLocationPolicy;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.options.DeleteOptions;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.client.file.policy.FileWriteLocationPolicy;
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
import java.util.stream.IntStream;


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
  //private static String CONF ; //  the file to store the config statistics: "bandwidth \n filesize \n cachesize of each worker \n mode"
  private static String  ALLOC; // the file to store the output of the python algorithm
  //private static String LOCALPATH; // local file for copying

  private static DefaultFileSystemMaster mFSMaster;

  private interface ModeConstants {

    // allocation settled by corresponding python files
    String MaxMinDefault = "MaxMinDefault"; // max-min fair with round robin; no throttling
    String LoadAware = "LoadAware";
    String Isolation = "Isolation"; // throttle everyone

    // allocation settled in this class; no throttling
    String Default = "RepDefault"; // round robin with replication

    // allocation and write are handled by test classess. No throttling
    String Test = "Test"; // Model test;

    // not in use currently
    String MaxMinOptLatency = "MaxMinOptLatency"; //
  }

  // get the following from alluxio-site.properties
  private static String mMode=ModeConstants.Default;
  private static Double mBandwidth; // of each worker
  private static Double mDelta; // disk delay
  private static int mFileSize; // in MB
  private static int mRepFactor; //for the repDefault mode: replication factor of the top 10% popular files.
  private static boolean mIsCluster; // for different curDir path on local test (Mac OS) and cluster (Cent OS)



  // get prefs, file count, and user count from pop.txt: getPref();
  private static int mFileCount;
  private static int mUserCount;
  private static List<List<Double>> mPrefs = new ArrayList<>();


  // get cache size as a input variable
  private static int mCacheSize; // of each worker, in MB

  // get worker count from the master. Do it before allocation. Don't get it on initialization of LAMaster, as the workers might not have registered by then.
  private static int mWorkerCount;


  // calculate this by with  mBandwidth * mWorkerCount / mUserCount / mFileSize
  private static Double mIsolateRate; // allowed TOTAL access rate per second, if a user is isolated // todo: per-worker throttling

  // allocation results
  private static List<Integer> mBlockList = new ArrayList<>();
  private static List<Double> mCacheRatio = new ArrayList<>(); // of each file
  private static List<Integer> mLocation = new ArrayList<>(); // location of each file. Files are identified by integer ids.


  // for throttling isolated users
  private static Map<Integer, TokenBucket> mTokenPool= new ConcurrentHashMap<>(); //todo: Bucket4j might provide a more efficient implementation of the token bucket algorithm
  private static int mBucketSize; // the size of the 'leaky' token bucket
  private static Map<Integer, Integer> mWaitRequestPool = new ConcurrentHashMap<>(); // Record the number of waiting request of each user (after its tokens are used up). Need to be thread-safe.
  private static int mMaxWaitRequestNumber = 400; // The maximum waiting requests for a BLOCKED user.





  //private static Map<String, Integer> mLocationMap = new ConcurrentHashMap<>(); // The map storing the actual locations of Alluxio files.


  public LoadAwareMaster(DefaultFileSystemMaster master){
    mFSMaster = master;
    mMode=Configuration.get(PropertyKey.MODE);
    mBandwidth =  Configuration.getDouble(PropertyKey.BANDWIDTH); // of each worker. MB per second
    mDelta = Configuration.getDouble(PropertyKey.DELTA); // disk delay
    mFileSize = Configuration.getInt(PropertyKey.FILE_SIZE);
    mRepFactor=Configuration.getInt(PropertyKey.REP_FACTOR);
    mIsCluster=Configuration.getBoolean(PropertyKey.IS_CLUSTER);

    //Check
    System.out.println(String.format("Configurations: %s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t", mMode,
            mBandwidth,mDelta,mFileSize, mUserCount, mFileCount,mRepFactor,mIsCluster));

  }

//  private static FileWriter createLogWriter(final String name) {
//    try {
//      return new FileWriter(name, true); //append;
//    } catch (final IOException exc) {
//      throw new Error(exc);
//    }
//  }

  private static void getPref(){
    if(mIsCluster)  // cluster mode
      curDir = System.getProperty("user.dir") + "/lacs";
    String pop= curDir+"/pop.txt"; // the file to store the output of the python algorithm
    try{
      BufferedReader br = new BufferedReader(new FileReader(pop)); // the first line is locations
      int userNumber = 0;
      mPrefs.clear();
      String s;
      while((s = br.readLine())!=null){
        userNumber++;
        String[] prefArray = s.split(",");
        List<Double> prefList = new ArrayList<>();
        for(String pref:prefArray){
          prefList.add(Double.parseDouble(pref));
        }
        mPrefs.add(prefList);
      }
      mUserCount = userNumber;
      System.out.println("User number:" + mUserCount);
      if(mUserCount>0){
        mFileCount = mPrefs.get(0).size();
        System.out.println("File number:" + mFileCount);
      }
    }catch( IOException e){
      e.printStackTrace();
    }
  }

  private static void getWorkerCount(){
    mWorkerCount = mFSMaster.getWorkerCount();
    System.out.println("worker count get from fs master: "+ mWorkerCount);
  }


  /**
   * Get allocation and write files.
   */

  public static void runWrite(int cacheSize){

    mCacheSize = cacheSize;
    getPref(); //from pop.txt
    if(mMode.equals(ModeConstants.Default)){
      writeFileDefault();
    }
    else{ //if(mMode.equals(ModeConstants.ModelTest)){
      //writeFileModelTest();
    //}else {
      getAllocation(); // get allocation from python scripts
      writeFile();
    }
  }


  /**
   * Run the python algorithm to get the cache allocation and block list.
   *
   * The output of the python program should be written to alloc.txt
   *
   *
   */

  public static void getAllocation() {

    getWorkerCount();
    mIsolateRate = mBandwidth * mWorkerCount / mUserCount / mFileSize;//total rate per second
    System.out.println("Isolate rate: " + mIsolateRate);
    //System.out.println("Worker count : " + mWorkerCount);

    // run python to get alloc.txt
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("python");

    switch(mMode){
      case ModeConstants.MaxMinDefault: cmd.add(curDir + "/python/mm_default.py");break;
      case ModeConstants.LoadAware: cmd.add(curDir + "/python/lacs.py");break;
      case ModeConstants.MaxMinOptLatency: cmd.add(curDir + "/python/mm_opt.py");break;
      case ModeConstants.Isolation: cmd.add(curDir + "/python/isolation.py");break;
      default: cmd.add(curDir + "/python/lacs.py");break;
    }
    cmd.add(mBandwidth.toString());
    cmd.add(Integer.toString(mWorkerCount)); //get worker count
    cmd.add(Integer.toString(mFileSize));
    cmd.add(Integer.toString(mCacheSize));
    cmd.add(mDelta.toString());

    // The python algorithm will read the log file of user access frequencies and output the cache allocation (position+ratio)
    // and the block list, all separated by commas. The path of the output file should be python/alloc.txt


    LOG.info("cmd: " + cmd);
    // convert cmd to string[]
    //String[] cmdArray = cmd.stream().toArray(String[]::new); // for java 1.8+
    String[] cmdArray = cmd.toArray(new String[cmd.size()]);

    // Run python and wait till it ends.
    try {
      Process p = Runtime.getRuntime().exec(cmdArray);
      p.waitFor(); //  block
    } catch (IOException | InterruptedException e) {
      LOG.info("Wrong Message received: " + e);
      return;
    }

    // parse the output alloc.txt to get mLocation, mCacheRatio and mBlockList
    updateAlloc();

  }

  @Deprecated
  /**
   * Read the configurations
   */
  public static void getConfig(){}


  /**
   * Update the mLocation, mCacheRatio and mBlockList from alloc.txt
   */
  public static void updateAlloc(){
    //if(mWorkerCount>1 || mMode.equals(ModeConstants.ModelTest))  //we have only one worker for model test
    if(mIsCluster)  // cluster mode
      curDir = System.getProperty("user.dir") + "/lacs";
    ALLOC= curDir+"/alloc.txt"; // the file to store the output of the python algorithm

    try{
      BufferedReader br = new BufferedReader(new FileReader(ALLOC)); // the first line is locations
      String[] locationArray = br.readLine().split(",");
      // mLocation = Arrays.stream(locationArray).mapToInt(Integer::parseInt).toArray(); // for jave 1.8+
      mLocation.clear();
      for(String loc:locationArray){
        mLocation.add(Integer.parseInt(loc));//Exception in this line
      }
      String[] ratioArray = br.readLine().split(","); // the second line is ratios
      //mCacheRatio = Arrays.stream(ratioArray).mapToDouble(Double::parseDouble).toArray();
      mCacheRatio.clear();
      for(String ratio:ratioArray){
        mCacheRatio.add(Double.parseDouble(ratio));//Exception in this line
      }
      String blockList = br.readLine(); // the last line is block list
      System.out.println(blockList);
      mBlockList.clear();
      if(!blockList.equals("")) {
        String[] blockArray = blockList.split(",");
        //mBlockList = Arrays.stream(blockArray).mapToInt(Integer::parseInt).toArray();
        // blockArray could be empty.
        for (String id : blockArray) {
          mBlockList.add(Integer.parseInt(id));//Exception in this line
        }
      }
      br.close();

      // log the results for debugging
      System.out.println("################Allocation results################");
      System.out.println("Locations " + Arrays.toString(mLocation.toArray()));
      System.out.println("Ratios " + Arrays.toString(mCacheRatio.toArray()));
      System.out.println("Block list " + Arrays.toString(mBlockList.toArray()));

    } catch (IOException e) {
      LOG.info("Wrong Message received: " + e);
      return;
    }

    mFileCount=mLocation.size();

    mBucketSize= (int)(mIsolateRate*3); //mIsolateRate.intValue();
    System.out.print("Refill rate: " +(long)(mIsolateRate*3) + " tokens every 3 seconds");
    mTokenPool.clear();
    for(Integer useId: mBlockList){
      TokenBucket tokenBucket = TokenBuckets.builder()
              .withCapacity(mBucketSize)
              .withFixedIntervalRefillStrategy((long)(mIsolateRate*3), 3, TimeUnit.SECONDS) // the isolate rate must be larger than 1/60 accesses per second.
              .build();
      mTokenPool.put(useId, tokenBucket);
      mWaitRequestPool.put(useId, 0);
    }
  }


  private static void writeFile(){
    FileSystem fs = FileSystem.Factory.get();

    AlluxioURI writeDir = new AlluxioURI(ALLUXIODIR);
    try {
      if (fs.exists(writeDir)) { // delete existing dir
        fs.delete(writeDir, DeleteOptions.defaults().setRecursive(true).setUnchecked(true));
      }
      byte[] buf = new byte[mFileSize*1024*1024]; // dummy buf to write
      for (int fileId = 0; fileId < mFileCount; fileId++) {
        LoadAwareFileWriter fw= new LoadAwareFileWriter(fs);
        int workerId = mLocation.get(fileId);
        double cacheRatio = mCacheRatio.get(fileId);
        fw.setmCacheRatio(cacheRatio);
        fw.setmWorkerId(workerId);
        String dstFile = String.format("%s/%s", ALLUXIODIR, fileId);
        fw.setmDstFile(dstFile);
        fw.setBuf(buf);
        fw.run();
        //mLocationMap.put(dstFile,workerId);
      }
    } catch (IOException | AlluxioException e) {
      e.printStackTrace();
    }
  }


  /**
     *
     *  Cache the top popular (aggregated) files. Top 10% files has mRepFactor replicas.
     */
  private static void writeFileDefault(){

    // get the aggregated preference order
    Double[] totalPref = new Double[mFileCount];
    Arrays.fill(totalPref, 0.0);
    //Systeyym.out.println("File_Number:" + File_Number + "Cache quota:" + Total_QUOTA);
    for(List<Double> prefs: mPrefs){
      for(int i = 0; i< mFileCount;i++)
        totalPref[i] += prefs.get(i);
    }
    int[] sortedIndices = IntStream.range(0, totalPref.length)
            .boxed().sorted((i, j) -> totalPref[j].compareTo(totalPref[i]) ) // descent order
            .mapToInt(ele -> ele).toArray();

    // start to write
    FileSystem fs = FileSystem.Factory.get();
    byte[] buf = new byte[mFileSize*1024*1024];
    int usedQuota = 0;
    int totalCacheQuota  = mCacheSize*mWorkerCount/mFileSize;
    CreateFileOptions writeOptions = CreateFileOptions.defaults().setWriteType(WriteType.MUST_CACHE);
    writeOptions.setWriteTier(0);
    try {
      for (int i=0;i<sortedIndices.length;i++) {
        int fileId = sortedIndices[i];
        int repCount = 1;
        if (i < mFileCount * 0.1)  // top 10% files have replicas
          repCount = mRepFactor;
        for (int replica = 0; replica < repCount; replica++) {
          AlluxioURI alluxioURI = new AlluxioURI(String.format("%s/%s-copy-%s",ALLUXIODIR,fileId,replica));

          if (usedQuota >= totalCacheQuota)
            writeOptions.setWriteTier(1);
          usedQuota ++;
          FileOutStream os = fs.createFile(alluxioURI, writeOptions);
          os.write(buf);
          os.close();
        }
      }
    } catch(IOException | AlluxioException e){
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
    LOG.info("User id received at the lacs master " + userId);

    if(mMode.equals(ModeConstants.Test)){ // no throttling, no need for the correct worker id
      return 0;
    }
    else if(mMode.equals(ModeConstants.Isolation)){
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
        return mLocation.get(Integer.parseInt(fileName));
      }
    }

    if(!mBlockList.contains(userId)){ // not in the list
      System.out.println("Mode: " + mMode);
      System.out.println("User " + userId + " not in the block list");
      return mLocation.get(Integer.parseInt(fileName));
    }

    else{ // must be in the LACS mode
      if(mWaitRequestPool.get(userId) > mMaxWaitRequestNumber) { // too many requests waiting
        LOG.info("LACS mode -- More than" + mMaxWaitRequestNumber + "waiting requests: Reject");
        return -1;
      }
      else{ // ask for a token
        TokenBucket tokenBucket = mTokenPool.get(userId);
        mWaitRequestPool.put(userId, mWaitRequestPool.get(userId)+1); // one more waiting
        LOG.info("LACS mode --  Asking for token");
        tokenBucket.consume(1); // block
        LOG.info("LACS mode --  Token get " + CommonUtils.getCurrentMs());
        mWaitRequestPool.put(userId, mWaitRequestPool.get(userId)-1); // one finishes waiting
        return mLocation.get(Integer.parseInt(fileName));
      }
    }
  }
}


