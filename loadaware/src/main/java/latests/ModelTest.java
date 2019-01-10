package latests;
import alluxio.AlluxioURI;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.options.DeleteOptions;
import alluxio.exception.AlluxioException;

import java.io.*;

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
  private static FileSystem mFS = FileSystem.Factory.get();
  private static String mTestFolder ="/tests";
  private static ReadTest mReadTest;
  private static FileWriter mTimeLog;


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


  /**
   *
   * @param args 1. file size; 2. file number; 3. access count;
   */

  public static void main(String[] args){
    mFileSize = Long.parseLong(args[0]);
    mFileNumber = Integer.parseInt(args[1]);
    int count = Integer.parseInt(args[2]);
    System.out.println(String.format("File size %s \t FileNumber %s \t count %s\t", mFileSize, mFileNumber, count));



    try{
      mTimeLog=new FileWriter("logs/model_test.txt",true);
      mReadTest = new ReadTest(mFileNumber, count,0, mTimeLog);
      int tier = 0;
      writeFiles(tier);

      double[] rates = new double[]{1,2,3,4,5,5.2,5.4,5.6,5.8};

      //mReadTest.setFileNumber();
      for(double rate:rates){
        mTimeLog.write(String.format("\n%s memory\n", rate));
        mReadTest.setRate(rate);
        mReadTest.readFiles();
        mTimeLog.flush();
      }


      tier = 1;
      writeFiles(tier);
      for(double rate:rates){
        mTimeLog.write(String.format("\n%s disk\n", rate));
        mReadTest.setRate(rate);
        mReadTest.readFiles();
        mTimeLog.flush();
      }
      mTimeLog.close();

    }catch (IOException e){
      e.printStackTrace();
    }
  }
}
