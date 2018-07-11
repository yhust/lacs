package latests;
import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.master.la_master.LoadAwareFileWriter;

/**
 * Created by yyuau on 28/6/2018.
 */
public class WriterTest {

  public WriterTest() {}
  public static final String TEST_PATH = "/tests";
  public static void main(String[] args) throws Exception {


    FileSystem fileSystem = FileSystem.Factory.get();
    LoadAwareFileWriter writer;
    String localFile;
    if(args.length >=4){
      writer = new LoadAwareFileWriter(fileSystem);
      localFile= args[0];
      writer.setmDstFile(String.format("%s/%s", TEST_PATH, args[1]));
      writer.setmWorkerId(Integer.parseInt(args[2]));
      writer.setmCacheRatio(Double.parseDouble(args[3]));
    }else {
      System.out.println("Usage: localFilePath, AlluxioPath, workerId, cacheRatio.");
      return;
    }
    writer.writeFile(localFile);
    System.exit(0);
  }

}
