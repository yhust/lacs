package alluxio.client;

import alluxio.AlluxioURI;
import alluxio.Configuration;
import alluxio.PropertyKey;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.options.GetLATokenOptions;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.exception.AlluxioException;
import alluxio.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by yuyinghao on 30/6/2018.
 */
public class LoadAwareFileReader implements Callable{

    private static final Logger LOG = LoggerFactory.getLogger(LoadAwareFileReader.class);

    private int mUserId;
    private int mFileId;
    private FileSystem mFileSystem = FileSystem.Factory.get();
    private LACSReadResult mResult;
    private boolean mIsRepDefault=false;

    public LoadAwareFileReader(int fileId, int userId){ //
        mFileId = fileId;
        mUserId=userId;
        mResult = new LACSReadResult(mUserId);
        if(Configuration.get(PropertyKey.MODE).equals("RepDefault"))
            mIsRepDefault=true;

    }
    public class LACSReadResult{
        public int userId;
        public boolean blocked;
        public long latency; // in ms
        public long fileSize; // in Bytes
        public double hit; // [0,1]
        //public Map<Integer,Long> loads; // workerId -> bytes read where to read from; to log loads
        LACSReadResult(int id){ userId = id;}
    }
    /**
     *
     *
     */
    public LACSReadResult call(){


        OpenFileOptions readOptions = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
        try {
            if (mIsRepDefault) {

                int copyId = mFileSystem.getLAToken(String.format("%s", mFileId), new GetLATokenOptions(mUserId));
                AlluxioURI alluxioURI = new AlluxioURI(String.format("/tests/%s-copy-%s", mFileId, copyId));
                long startTimeMs = CommonUtils.getCurrentMs();
                FileInStream fis=  mFileSystem.openFile(alluxioURI, readOptions);
                byte[] buf = new byte[(int)fis.mFileLength];
                fis.read(buf);
                if(mFileSystem.getStatus(alluxioURI).getInMemoryPercentage() ==100){
                    System.out.println(String.format("Access /tests/%s-copy-%s in memory", mFileId, copyId));
                    mResult.hit=1.0;
                }else{
                    System.out.println(String.format("Access /tests/%s-copy-%s on disk", mFileId, copyId));
                    mResult.hit=0.0;
                    Thread.sleep(fis.mFileLength / 1024 / 1024);// simulate disk I/O delay, 1ms per MB
                }
                long endTimeMs = CommonUtils.getCurrentMs();
                mResult.latency = endTimeMs - startTimeMs;
                mResult.fileSize = fis.mFileLength;
                fis.close();
            } else {
                AlluxioURI cacheURI = new AlluxioURI(String.format("/tests/%s-0", mFileId));
                AlluxioURI diskURI = new AlluxioURI(String.format("/tests/%s-1", mFileId));


                long startTimeMs = CommonUtils.getCurrentMs();
                int token = mFileSystem.getLAToken(String.format("%s", mFileId), new GetLATokenOptions(mUserId));
                if (token >= 0) { // get the token. The token is the machine id, used for load tracking
                    mResult.blocked = false;
                    long cacheBytes = 0;
                    long diskBytes = 0;
                    int totalBytes = 0;
                    FileInStream isCache = null;
                    FileInStream isDisk = null;
                    if (mFileSystem.exists(cacheURI)) {
                        isCache = mFileSystem.openFile(cacheURI, readOptions);
                        totalBytes += isCache.mFileLength;
                        System.out.println("Cached bytes: " + isCache.mFileLength);
                        cacheBytes = isCache.mFileLength;
                    }
                    if (mFileSystem.exists(diskURI)) {
                        isDisk = mFileSystem.openFile(diskURI, readOptions);
                        totalBytes += isDisk.mFileLength;
                        Thread.sleep(isDisk.mFileLength / 1024 / 1024);// simulate disk I/O delay, 1ms per MB
                        diskBytes = isDisk.mFileLength;
                    }
                    byte[] buf = new byte[totalBytes];
                    if (mFileSystem.exists(cacheURI)) {
                        isCache.read(buf);
                    }
                    if (mFileSystem.exists(diskURI)) {
                        isDisk.read(buf, (int) cacheBytes, (int) diskBytes);
                    }
                    long endTimeMs = CommonUtils.getCurrentMs();
                    mResult.latency = endTimeMs - startTimeMs;
                    mResult.hit = (float) (cacheBytes) / totalBytes;
                    mResult.fileSize = totalBytes;
                    isCache.close();
                    isDisk.close();
                } else {
                    mResult.blocked = true;
                    mResult.latency = -1;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return mResult;
    }

}
