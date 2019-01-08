package latests;

import alluxio.client.LoadAwareFileReader;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by yuyinghao on 1/8/19.
 *
 * Exponentially distributed intervals
 *
 */
public class ReadTest {

    private static FileWriter mTimeLog;
    private static FileWriter mHitLog=null;
    private int mTrial;
    private double mRate;
    private int mFileNumber;
    private RandomNumberGenerator mRandomNumberGenerator;

    public ReadTest(int trial, FileWriter log){
        mTrial = trial;
        mTimeLog = log;
//        try {
//            mTimeLog = new FileWriter("logs/model_test.txt", true);
//        } catch(IOException e){
//            e.printStackTrace();
//        }
        //float rate = Float.parseFloat(args[3]);
        mRandomNumberGenerator=new RandomNumberGenerator();
        ZipfDistribution zd = new ZipfDistribution(mFileNumber,1.05);
        for(int i=1;i<=mFileNumber;i++) {
            mRandomNumberGenerator.addNumber(i-1, zd.probability(i));
        }

    }

    public void setRate(double rate){
        mRate = rate;
    }
    public void setFileNumber(int fileNumber){
        mFileNumber = fileNumber;
    }
    public void setHitLog(FileWriter hitLog) { // not all
        mHitLog = hitLog;
    }

    protected void readFiles() {

        List<Future<LoadAwareFileReader.LACSReadResult>> results = new ArrayList<>();

        ExecutorService executorService = Executors.newCachedThreadPool();

        try {
            for (int i = 0; i < mTrial; i++) {
                int fileId = mRandomNumberGenerator.getNext();
                results.add(executorService.submit(new LoadAwareFileReader(fileId, 0)));

                // Access interval
                Double interval = new ExponentialDistribution(1.0 / mRate).sample();
                interval *= 1000; // ms
                System.out.println("Access interval " + interval.longValue() + " ms");
                Thread.sleep(interval.longValue());
            }
            Double avgLatency = 0.0;
            Double avgHR = 0.0;
            for (Future<LoadAwareFileReader.LACSReadResult> future : results) {
                LoadAwareFileReader.LACSReadResult result = future.get();
                mTimeLog.write(String.format("%s\t", result.latency));
                avgLatency += result.latency;
                avgHR += result.hit;
                if(mHitLog!=null){
                    mHitLog.write(String.format("%s\t",result.hit));
                }
            }
            avgLatency /= mTrial;
            mTimeLog.write("\n\n" + avgLatency + "\t" + avgHR + "\n\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
