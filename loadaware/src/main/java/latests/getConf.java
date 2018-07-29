package latests;

import alluxio.client.file.FileSystem;

/**
 * Created by yyuau on 29/7/2018.
 */
public class getConf {
  private static FileSystem mFS = FileSystem.Factory.get();

  public static void main(String[] args){
    try {
      mFS.getConf();
    }catch(Exception e){
      e.printStackTrace();
    }
  }

}
