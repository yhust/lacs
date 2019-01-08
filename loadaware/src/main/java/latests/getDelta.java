package latests;
import alluxio.AlluxioURI;
import alluxio.client.ReadType;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.options.DeleteOptions;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.exception.AlluxioException;
import alluxio.util.CommonUtils;

import java.io.*;


/**
 * Created by yyuau on 15/7/2018.
 *
 * To get the proportional overhead of disk I/O compared with reading from memory.
 */
public class getDelta {

  private static long mFileSize; // in MB
  private static FileSystem mFS = FileSystem.Factory.get();

  private static void writeFiles(){
    String LOCALPATH = "delta_test";
    AlluxioURI memory = new AlluxioURI("/Delta_M");
    AlluxioURI disk = new AlluxioURI("/Delta_D");

    // generate a dummy file of give size
    try{
      RandomAccessFile f = new RandomAccessFile(LOCALPATH, "rw");
      f.setLength(mFileSize * 1024 * 1024);
    } catch (Exception e ){
      e.printStackTrace();
    }

    // write into Alluxio
    try {
      if (mFS.exists(memory)) { // delete existing files
        mFS.delete(memory, DeleteOptions.defaults().setUnchecked(true)); //.setRecursive(true)
      }
      if (mFS.exists(disk)) {
        mFS.delete(disk, DeleteOptions.defaults().setUnchecked(true)); //.setRecursive(true)
      }
      FileInputStream is = new FileInputStream(LOCALPATH);
      int fileLength =  (int) new File(LOCALPATH).length();
      byte[] buf = new byte[fileLength];
      is.read(buf);
      is.close();

      CreateFileOptions writeOptions = CreateFileOptions.defaults().setWriteTier(0);
      FileOutStream os = mFS.createFile(memory, writeOptions);
      os.write(buf);
      os.close();


      writeOptions.setWriteTier(1);
      os = mFS.createFile(disk, writeOptions);
      os.write(buf);
      os.close();

    } catch (IOException | AlluxioException e) {
      e.printStackTrace();
    }

    //delete the dummy test file

//    try{
//      File f = new File(LOCALPATH);
//      f.delete();
//    } catch (Exception e ){
//      e.printStackTrace();
//    }

  }

  protected static void readFiles(int trial){
    AlluxioURI memory = new AlluxioURI("/Delta_M");
    AlluxioURI disk = new AlluxioURI("/Delta_D");

    OpenFileOptions readOptions = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
    long memoryTime = 0L;
    long diskTime = 0L;

    try{
      FileWriter cacheLog = new FileWriter(String.format("cacheLatency_%s.txt",mFileSize), true);
      FileWriter diskLog = new FileWriter(String.format("diskLatency_%s.txt",mFileSize),true);
      for(int i = 0; i< trial ; i++){
        FileInStream isM = mFS.openFile(memory, readOptions);
        FileInStream isD = mFS.openFile(disk, readOptions);
        byte[] buf = new byte[(int)isM.mFileLength];
        long start = CommonUtils.getCurrentMs();
        isM.read(buf);
        long latency = CommonUtils.getCurrentMs() - start;
        memoryTime += latency;
        cacheLog.write(String.format("%s\t", latency));
        System.out.println("Read from memory ("+ i + "): " + latency);
        start = CommonUtils.getCurrentMs();
        isD.read(buf);
        Thread.sleep(isD.mFileLength/1024/1024);// 1ms per MB
        latency = CommonUtils.getCurrentMs() - start;
        diskTime +=latency;
        diskLog.write(String.format("%s\t", latency));
        System.out.println("Slept for "+isD.mFileLength/1024/1024 + " ms.");
        System.out.println("Read from disk ("+ i + "): " + latency);
      }
      diskLog.close();
      cacheLog.close();

      double delta = (double)(diskTime - memoryTime) / (trial * mFileSize);
      System.out.println("### Delta = " + String.format("(%s - %s) / (%s x %s) = ", diskTime, memoryTime, trial, mFileSize) + delta);
      FileWriter fw = new FileWriter("delta.txt",true);
      fw.write(String.format("%s,%s\n",mFileSize,delta));
      fw.close();

    } catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   *
   * @param args 1. file size; 2. trial
   */

  public static void main(String[] args){
    mFileSize = Long.parseLong(args[0]);
    int trial = Integer.parseInt(args[1]);
    writeFiles();
    readFiles(trial);
  }
}
