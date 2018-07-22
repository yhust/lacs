package latests;

//import alluxio.master.la_master.*;
import alluxio.client.file.FileSystem;
import latests.PrepareTests;
/**
 * Created by yyuau on 10/7/2018.
 */
public class writeFiles {
  public static void main(String[] args){
    //LoadAwareMaster.writeFile();
    //PrepareTests.writeFile();
    FileSystem fileSystem = FileSystem.Factory.get();
    try{
      fileSystem.runLAWrite();
    } catch(Exception e){
      e.printStackTrace();
    }

  }
}
