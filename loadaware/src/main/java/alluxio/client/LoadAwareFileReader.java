package alluxio.client;

import alluxio.AlluxioURI;
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
import java.util.concurrent.Callable;

/**
 * Created by yuyinghao on 30/6/2018.
 */
public class LoadAwareFileReader{

    private static final Logger LOG = LoggerFactory.getLogger(LoadAwareFileReader.class);

    protected int mUserId;
    private static FileSystem mFileSystem = FileSystem.Factory.get(); // todo: will using a static fs slow down the performance?
    private OpenFileOptions mReadOptions;
    private AlluxioURI mDiskURI; // the cached part and on-disk part as two different files
    private AlluxioURI mCacheURI;

    private static FileWriter mTimeLog;
    //private static FileWriter mBlockLog;

    public LoadAwareFileReader(){ //
    }

    public static void main(String[] args){
        String fileName = args[0];
        int userId = Integer.parseInt(args[1]);

        OpenFileOptions readOptions = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
        AlluxioURI cacheURI = new AlluxioURI(String.format("%s-0", fileName));
        AlluxioURI diskURI = new AlluxioURI(String.format("%s-1", fileName));


        try {
            mTimeLog= new FileWriter("logs/readLatency.txt", true); //append
            boolean token = mFileSystem.getLAToken(fileName, new GetLATokenOptions(userId));
            synchronized (mTimeLog) {
                if (token) { // get the token
                    long startTimeMs = CommonUtils.getCurrentMs();
                    int cacheBytes = 0;
                    int diskBytes = 0;
                    int totalBytes = 0;
                    FileInStream isCache = null;
                    FileInStream isDisk = null;
                    if(mFileSystem.exists(cacheURI)){
                        isCache = mFileSystem.openFile(cacheURI, readOptions);
                        totalBytes += isCache.mFileLength;
                    }
                    if(mFileSystem.exists(diskURI)){
                        isDisk = mFileSystem.openFile(diskURI, readOptions);
                        totalBytes += isDisk.mFileLength;
                    }
                    byte[] buf = new byte[totalBytes];
                    if(mFileSystem.exists(cacheURI)){
                        cacheBytes = isCache.read(buf);
                    }
                    if(mFileSystem.exists(diskURI)){
                        diskBytes = isDisk.read(buf);
                    }
                    long endTimeMs = CommonUtils.getCurrentMs();
                    long latency =  endTimeMs - startTimeMs;
                    LOG.info("");
                    mTimeLog.write("" + latency + "\n");
                } else {
                    mTimeLog.write("-1\n");
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
