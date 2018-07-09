package latests;
import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.master.LoadAwareFileWriter;

/**
 * Created by yyuau on 28/6/2018.
 */
public class WriterTest {

  public WriterTest() {}
  public static final String TEST_PATH = "/tests";
  public static void main(String[] args) throws Exception {


    FileSystem fileSystem = FileSystem.Factory.get();
    LoadAwareFileWriter writer;
    if(args.length >=4){
      writer = new LoadAwareFileWriter(args[0], String.format("%s/%s", TEST_PATH, args[1]),
              Integer.parseInt(args[2]), Float.parseFloat(args[3]), fileSystem);
    }else {
      System.out.println("Usage: localFilePath, AlluxioPath, workerId, cacheRatio.");
      return;
    }
    writer.writeFile();
    System.exit(0);
  }

}
