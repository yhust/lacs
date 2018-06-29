package alluxio.master;

import alluxio.AlluxioURI;
import alluxio.client.WriteType;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.policy.LoadAwarePolicy;
import alluxio.examples.BasicOperations;
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
public class LoadAwareFileWriter {

  protected int mWorkerId;
  protected float mCacheRatio;
  //protected int mFileSize;
  private CreateFileOptions mWriteOptions;
  private FileSystem mFileSystem;
  private AlluxioURI mFilePath;
  private String mLocalFile;
  private static final Logger LOG = LoggerFactory.getLogger(BasicOperations.class);

  public LoadAwareFileWriter(String localFile, AlluxioURI alluxioPath, int workerId,
                             float cacheRatio, FileSystem fileSystem) {
    mWorkerId = workerId;
    mCacheRatio = cacheRatio;
    mFileSystem = fileSystem;
    mLocalFile = localFile;
    mFilePath = alluxioPath;
    //long tBlockSize;
    //if (fileSize % k == 0) {
      //tBlockSize = fileSize / mK;
    //} else {
      //// NOTE: Assume that file size in bytes is much larger than the number of machines
      //tBlockSize = fileSize / mK + 1;
    //}
    mWriteOptions = CreateFileOptions.defaults().setWriteType(WriteType.MUST_CACHE)
            //.setBlockSizeBytes(tBlockSize)
            .setLocationPolicy(new LoadAwarePolicy(mWorkerId));
            //.setKValueForSP(mK);
    mWriteOptions.getWriteTier();
    // LOG.info("k value after set in write is " + mWriteOptions.getKValueForSP());
  }

  public void setWriteOption(CreateFileOptions writeOptions) {
    mWriteOptions = writeOptions;
  }

  public void writeFile() {
    try {
      // the write policy is uniquely random
      FileInputStream is = new FileInputStream(mLocalFile);
      int tFileLength = (int) new File(mLocalFile).length();
      int tCacheLength = (int) (tFileLength * mCacheRatio);
      int tDiskLength = tFileLength - tCacheLength;

      byte[] tCacheBuf = new byte[tCacheLength];
      //Write the cached part
      int tCacheBytes = is.read(tCacheBuf);


      byte[] tDiskBuf = new byte[tDiskLength];
      int tDiskBytes= is.read(tDiskBuf);
      // Then write the file into Alluxio.
      long tStartTimeMs = CommonUtils.getCurrentMs();
      mWriteOptions.setWriteTier(0); // the cache tier
      FileOutStream os = mFileSystem.createFile(mFilePath, mWriteOptions);
      os.write(tCacheBuf);
      LOG.info("Write into memory with bytes:" + tCacheBuf);

      mWriteOptions.setWriteTier(1); // the second tier: disk
      os.write(tDiskBuf);
      LOG.info("Write into disk with bytes:" + tDiskBytes);

      LOG.info(FormatUtils.formatTimeTakenMs(tStartTimeMs,
              "writing " +  mLocalFile + " into Alluxio path " + mFilePath));
      is.close();
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (AlluxioException e) {
      e.printStackTrace();
    }

  }

}
