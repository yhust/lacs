package alluxio.master.la_master;

import alluxio.AlluxioURI;
import alluxio.client.WriteType;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.policy.LoadAwarePolicy;
import alluxio.exception.AlluxioException;
import alluxio.util.CommonUtils;
import alluxio.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by yinghao on 2018/6/28.
 *
 * This class is the writer that writes files into Alluxio, given cache fraction and location to write.
 */
public class LoadAwareFileWriter implements Runnable{

  protected int mWorkerId;
  protected double mCacheRatio;
  private CreateFileOptions mWriteOptions;
  private FileSystem mFileSystem;
  private String mDstFile;
  //private String mSrcFile; // intended for inside-Alluxio file-copy.
  private static final Logger LOG = LoggerFactory.getLogger(LoadAwareFileWriter.class);
  private byte[] buf;

  public LoadAwareFileWriter(FileSystem fileSystem) {
    mFileSystem = fileSystem;
    mWriteOptions = CreateFileOptions.defaults().setWriteType(WriteType.MUST_CACHE);
  }
  public void setmWorkerId(int workerId){
    mWorkerId = workerId;
    mWriteOptions.setLocationPolicy(new LoadAwarePolicy(mWorkerId));
  }

  public void setmDstFile(String dstFile){
    mDstFile = dstFile ;
  }

  public void setmCacheRatio(double cacheRatio){
    mCacheRatio = cacheRatio;
  }
  public void setBuf(byte[] b){buf = b;}
  public void writeFile(String localFile) { // write the local file
    try {

      FileInputStream is = new FileInputStream(localFile);
      int tFileLength =  (int) new File(localFile).length();
      int tCacheLength = (int) (tFileLength * mCacheRatio);
      int tDiskLength = tFileLength - tCacheLength;

      byte[] tCacheBuf = new byte[tCacheLength];
      int tCacheBytes = is.read(tCacheBuf);

      byte[] tDiskBuf = new byte[tDiskLength];
      int tDiskBytes = is.read(tDiskBuf);

      // Write the file into Alluxio; store the cached part and on-disk part as two different files
      AlluxioURI cacheURI = new AlluxioURI(String.format("%s-0", mDstFile));
      AlluxioURI diskURI = new AlluxioURI(String.format("%s-1", mDstFile));

      long tStartTimeMs = CommonUtils.getCurrentMs();
      if(tCacheBytes >0) {
        mWriteOptions.setWriteTier(0); // the cache tier
        FileOutStream os = mFileSystem.createFile(cacheURI, mWriteOptions);
        os.close();
        //System.out.println("Write into memory with bytes:" + tCacheBytes);
        LOG.info("Write into memory with bytes:" + tCacheBytes);
      }

      if(tDiskBytes > 0){
        mWriteOptions.setWriteTier(1); // the second tier: disk
        FileOutStream os = mFileSystem.createFile(diskURI, mWriteOptions);
        os.write(tDiskBuf);
        os.close();
        LOG.info("Write into disk with bytes:" + tDiskBytes);
        //System.out.println("Write into disk with bytes:" + tDiskBytes);
      }
      LOG.info(FormatUtils.formatTimeTakenMs(tStartTimeMs,
              "writing " +  localFile + " into Alluxio."));
      is.close();
    } catch (IOException | AlluxioException e) {
      e.printStackTrace();
    }

  }
  public void run() { // write the buf
    try {


      int tFileLength = buf.length;
      int tCacheLength = (int) (tFileLength * mCacheRatio);
      int tDiskLength = tFileLength - tCacheLength;


      // Write the file into Alluxio; store the cached part and on-disk part as two different files
      AlluxioURI cacheURI = new AlluxioURI(String.format("%s-0", mDstFile));
      AlluxioURI diskURI = new AlluxioURI(String.format("%s-1", mDstFile));

      long tStartTimeMs = CommonUtils.getCurrentMs();
      if(tCacheLength >0){
        mWriteOptions.setWriteTier(0); // the cache tier
        FileOutStream os = mFileSystem.createFile(cacheURI, mWriteOptions);
        os.write(buf,0,tCacheLength);
        os.close();
        //System.out.println("Write into memory with bytes:" + tCacheBytes);
        // LOG.info("Write into memory with bytes:" + tCacheLength);
      }
      if(tDiskLength > 0){
        mWriteOptions.setWriteTier(1); // the second tier: disk
        FileOutStream os = mFileSystem.createFile(diskURI, mWriteOptions);
        os.write(buf,tCacheLength,tDiskLength);
        os.close();
        // LOG.info("Write into disk with bytes:" + tDiskBytes);
        //System.out.println("Write into disk with bytes:" + tDiskBytes);
      }
      LOG.info(FormatUtils.formatTimeTakenMs(tStartTimeMs,
              "write" + mDstFile + " into Alluxio: " + tFileLength + " bytes."));
    } catch (IOException | AlluxioException e) {
      e.printStackTrace();
    }
  }
}
