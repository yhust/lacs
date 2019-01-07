package latests;
import alluxio.AlluxioURI;
import alluxio.client.LoadAwareFileReader;
import alluxio.client.ReadType;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.options.DeleteOptions;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.exception.AlluxioException;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Created by yyuau on 15/7/2018.
 *
 * To test the accuracy of M/D/1 queue
 * Log the latencies here but not in the read threads.
 *
 */

public class ModelTest {

  private static long mFileSize; // in MB
  private static int mFileNumber;
  private static RandomNumberGenerator mRandomNumberGenerator;
  private static FileSystem mFS = FileSystem.Factory.get();
  private static String mTestFolder ="/tests";
  //private final static FileWriter mTimeLog = createLogWriter("logs/readLatency.txt"); // user_id \t latency (-1 if rejected)\n
  private static FileWriter mTimeLog;


//  private static FileWriter createLogWriter(final String name) {
//    try {
//      return new FileWriter(name, true); //append);
//    } catch (final IOException e) {
//      e.printStackTrace();
//    }
//  }


  private static void writeFiles(int tier){
    // write into Alluxio
    try {
      if (mFS.exists(new AlluxioURI(mTestFolder))) { // delete existing files
        mFS.delete(new AlluxioURI(mTestFolder), DeleteOptions.defaults().setUnchecked(true).setRecursive(true));
      }

      byte[] buf = new byte[(int)mFileSize*1024*1024];

      CreateFileOptions writeOptions = CreateFileOptions.defaults().setWriteTier(tier);
      for(int i=0;i<mFileNumber;i++){
        FileOutStream os = mFS.createFile(new AlluxioURI(mTestFolder + "/" + i + "-" + tier), writeOptions);
        os.write(buf);
        os.close();
      }

    } catch (IOException | AlluxioException e) {
      e.printStackTrace();
    }
  }

  protected static void readFiles(int trial, double rate){

    OpenFileOptions readOptions = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
    long memoryTime = 0L;
    long diskTime = 0L;
    List<Future<LoadAwareFileReader.LACSReadResult>> results = new ArrayList<>();

    ExecutorService executorService = Executors.newCachedThreadPool();

    try{
      for(int i = 0; i< trial ; i++){
        int fileId = mRandomNumberGenerator.getNext();
        results.add(executorService.submit(new LoadAwareFileReader(fileId,0)));

        // Access interval
        Double interval  = new ExponentialDistribution(1.0/rate).sample();
        interval*= 1000; // ms
        System.out.println("Access interval " + interval.longValue() + " ms");
        Thread.sleep(interval.longValue());
      }

      for(Future<LoadAwareFileReader.LACSReadResult> future: results){
        LoadAwareFileReader.LACSReadResult result=future.get();
        mTimeLog.write(String.format("%s\t",result.latency));
      }
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   *
   * @param args 1. file size; 2. file number; 3. access count;
   */

  public static void main(String[] args){
    mFileSize = Long.parseLong(args[0]);
    mFileNumber = Integer.parseInt(args[1]);
    int count = Integer.parseInt(args[2]);
    System.out.println(String.format("File size %s \t FileNumber %s \t count %s\t", mFileSize, mFileNumber, count));

    //float rate = Float.parseFloat(args[3]);
    mRandomNumberGenerator=new RandomNumberGenerator();
    ZipfDistribution zd = new ZipfDistribution(mFileNumber,1.05);
    for(int i=1;i<=mFileNumber;i++) {
      mRandomNumberGenerator.addNumber(i-1, zd.probability(i));
    }

    try{
      mTimeLog=new FileWriter("logs/model_test.txt",true);

      int tier = 0;
      writeFiles(tier);

      double[] rates = new double[]{1,2,3,4,5,5.2,5.4,5.6,5.8};
      for(double rate:rates){
        mTimeLog.write(String.format("\n%s memory\n", rate));
        readFiles(count, rate);
        mTimeLog.flush();
      }


      tier = 1;
      writeFiles(tier);
      for(double rate:rates){
        mTimeLog.write(String.format("\n%s disk\n", rate));
        readFiles(count, rate);
        mTimeLog.flush();
      }
      mTimeLog.close();

    }catch (IOException e){
      e.printStackTrace();
    }
  }
}
