package alluxio.client;

import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.master.LoadAwareFileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yuyinghao on 30/6/2018.
 */
public class LoadAwareFileReader {

    //protected int mWorkerId;
    protected int mUserId;
    //protected float mCacheRatio;
    //protected int mFileSize;
    private FileSystem mFileSystem;
    private OpenFileOptions mReadOptions;
    private AlluxioURI mDiskURI; // the cached part and on-disk part as two different files
    private AlluxioURI mCacheURI;
    private String mLocalFile;
    private static final Logger LOG = LoggerFactory.getLogger(LoadAwareFileWriter.class);
    public LoadAwareFileReader(String alluxioPath, int userId){ //
        mReadOptions = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
        mCacheURI = new AlluxioURI(String.format("%s-1", alluxioPath));
        mDiskURI = new AlluxioURI(String.format("%s-2", alluxioPath));
        mUserId = userId;
    }

}
