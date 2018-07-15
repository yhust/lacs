package latests;

import latests.getDelta;

/**
 * Created by yyuau on 15/7/2018.
 */
public class repeatDelta {

  /**
   * @param args 1: trial
   */
  public static void main(String[] args){
    int trial = Integer.parseInt(args[0]);
    getDelta.readFiles(trial);
  }
}
