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

    private final static FileWriter mTimeLog = createLogWriter("logs/readLatency.txt"); // user_id \t latency (-1 if rejected)\n
    private final static FileWriter mCacheHitLog = createLogWriter("logs/cacheHit.txt"); // user_id \t cache bytes \t disk bytes \n


    public LoadAwareFileReader() throws IOException{ //
    }

    private static FileWriter createLogWriter(final String name) {
        try {
            return new FileWriter(name, true); //append);
        } catch (final IOException exc) {
            throw new Error(exc);
        }
    }

    /**
     *
     * @param  args 1. file name 2.  user id
     */
    public static void main(String[] args){
        String fileName = args[0];
        int userId = Integer.parseInt(args[1]);

        OpenFileOptions readOptions = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
        AlluxioURI cacheURI = new AlluxioURI(String.format("/tests/%s-0", fileName));
        AlluxioURI diskURI = new AlluxioURI(String.format("/tests/s-1", fileName));


        try {
            boolean token = mFileSystem.getLAToken(fileName, new GetLATokenOptions(userId));
            if (token) { // get the token
                long startTimeMs = CommonUtils.getCurrentMs();
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
                    Thread.sleep(isDisk.mFileLength/1024/1024);// 1ms per MB
                    diskBytes = isDisk.mFileLength;
                }
                synchronized (mCacheHitLog){
                    mCacheHitLog.write(String.format("%s:\t %s\t %s \n", userId, cacheBytes, diskBytes));
                    mCacheHitLog.close();
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
                //LOG.info("");
                synchronized (mTimeLog) {mTimeLog.write("" + userId + "\t" + latency + "\n");mTimeLog.close();}
            } else {
                synchronized (mTimeLog) {mTimeLog.write("" + userId + "\t" + "-1\n");mTimeLog.close();}
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
