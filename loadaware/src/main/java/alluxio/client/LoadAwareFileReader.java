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
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by yuyinghao on 30/6/2018.
 */
public class LoadAwareFileReader implements Callable{

    private static final Logger LOG = LoggerFactory.getLogger(LoadAwareFileReader.class);

    private int mUserId;
    private int mFileId;
    private static FileSystem mFileSystem = FileSystem.Factory.get();
    private static LACSReadResult mResult;
    private OpenFileOptions mReadOptions;
    private AlluxioURI mDiskURI; // the cached part and on-disk part as two different files
    private AlluxioURI mCacheURI;

//    private final static FileWriter mTimeLog = createLogWriter("logs/readLatency.txt"); // user_id \t latency (-1 if rejected)\n
//    private final static FileWriter mCacheHitLog = createLogWriter("logs/cacheHit.txt"); // user_id \t cache bytes \t disk bytes \n
//    private final static FileWriter mLoadLog = createLogWriter("logs/workerLoad.txt"); // worker_id \t read bytes \n


    public LoadAwareFileReader(int fileId, int userId){ //
        mFileId = fileId;
        mUserId=userId;
        mResult = new LACSReadResult(mUserId);
    }

//    private static FileWriter createLogWriter(final String name) {
//        try {
//            return new FileWriter(name, true); //append);
//        } catch (final IOException exc) {
//            throw new Error(exc);
//        }
//    }
    public class LACSReadResult{
        public int userId;
        public boolean blocked;
        public long latency; // in ms
        public long fileSize; // in Bytes
        public float hit; // [0,1]
        //public Map<Integer,Long> loads; // workerId -> bytes read where to read from; to log loads
        LACSReadResult(int id){ userId = id;}
    }

    /**
     *
     *
     */
    public LACSReadResult call(){
       // String fileName = args[0];
        //int userId = Integer.parseInt(args[1]);

        OpenFileOptions readOptions = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
        AlluxioURI cacheURI = new AlluxioURI(String.format("/tests/%s-0", mFileId));
        AlluxioURI diskURI = new AlluxioURI(String.format("/tests/%s-1", mFileId));


        try {
            long startTimeMs = CommonUtils.getCurrentMs();
            int token = mFileSystem.getLAToken(String.format("%s",mFileId), new GetLATokenOptions(mUserId));
            if (token >=0) { // get the token. The token is the machine id, used for load tracking
                mResult.blocked=false;
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
                    Thread.sleep(isDisk.mFileLength/1024/1024);// simulate disk I/O delay, 1ms per MB
                    diskBytes = isDisk.mFileLength;
                }
//                synchronized (mCacheHitLog){
//                    mCacheHitLog.write(String.format("%s:\t %s\t %s \n", mUserId, cacheBytes, diskBytes));
//                    mCacheHitLog.close();
//                }
                byte[] buf = new byte[totalBytes];
                if(mFileSystem.exists(cacheURI)){
                    isCache.read(buf);
                }
                if(mFileSystem.exists(diskURI)){
                    isDisk.read(buf,(int)cacheBytes,(int)diskBytes);
                }
                long endTimeMs = CommonUtils.getCurrentMs();
                mResult.latency = endTimeMs - startTimeMs;
                mResult.hit = (float)(cacheBytes)/totalBytes;
                mResult.fileSize =totalBytes;
                //LOG.info("");
//                synchronized (mTimeLog) {mTimeLog.write("" + mUserId + "\t" + latency + "\n");mTimeLog.close();}
//                synchronized (mLoadLog) {mLoadLog.write("" + token + "\t" + totalBytes + "\n");mLoadLog.close();}
            } else {
//                synchronized (mTimeLog) {mTimeLog.write("" + mUserId + "\t" + "-1\n");mTimeLog.close();}
                mResult.blocked=true;
                mResult.latency = -1;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return mResult;
    }

}
