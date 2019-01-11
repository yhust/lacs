package latests;

import alluxio.client.LoadAwareFileReader;
import alluxio.util.CommonUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by yuyinghao on 1/8/19.
 *
 * Exponentially distributed intervals
 *
 */
public class ReadTest {

    private static FileWriter mTimeLog;
    private static FileWriter mHitLog=null;
    private static File mPopFile;
    private int mTrial;
    private double mRate;
    private int mFileNumber;
    private RandomNumberGenerator mRandomNumberGenerator;
    private int mUserId;

    public ReadTest(int fileNumber, int trial, int userId, File popFile, FileWriter log){
        mFileNumber = fileNumber;
        mTrial = trial;
        mUserId = userId;
        mTimeLog = log;
        mPopFile = popFile;
        mRandomNumberGenerator=new RandomNumberGenerator();
        // load the preferences
        try{
            String popString = FileUtils.readLines(mPopFile).get(userId-1);
            System.out.println("Pop file:" + popString);
            String[] pops = popString.split(",");

            for(int i=0;i<pops.length;i++) {
                mRandomNumberGenerator.addNumber(i, Double.parseDouble(pops[i]));
            }
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public void setRate(double rate){
        mRate = rate;
    }
    public void setHitLog(FileWriter hitLog) { // not all
        mHitLog = hitLog;
    }

    protected void readFiles() {

        List<Future<LoadAwareFileReader.LACSReadResult>> results = new ArrayList<>();
        List<Long> submitTimes = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(50); //no more than 30 read threads in concurrent //Executors.newCachedThreadPool();
        try{
            for (int i = 0; i < mTrial; i++) {
                int fileId = mRandomNumberGenerator.getNext();
                submitTimes.add(CommonUtils.getCurrentMs());
                results.add(executorService.submit(new LoadAwareFileReader(fileId, mUserId-1)));

                // Access interval
                Double interval = new ExponentialDistribution(1.0 / mRate).sample();
                interval *= 1000; // ms
                System.out.println("Access interval " + interval.longValue() + " ms");
                Thread.sleep(interval.longValue());
            }
            Double avgLatency = 0.0;
            Double avgHR = 0.0;
            for (int i = 0;i< mTrial;i++){
                Future future = results.get(i);
                LoadAwareFileReader.LACSReadResult result = (LoadAwareFileReader.LACSReadResult)future.get();
                //mTimeLog.write(String.format("%s\t", result.latency));
                long latency = result.completeTime - submitTimes.get(i);
                mTimeLog.write(String.format("%s\t", latency));
                //avgLatency += result.latency;
                if(!result.blocked) {
                    avgLatency += latency;
                    avgHR += result.hit;
                    if (mHitLog != null) {
                        mHitLog.write(String.format("%s\t", result.hit));
                    }
                }
            }
            avgLatency /= mTrial;
            avgHR /= mTrial;
            mTimeLog.write("\n\n" + avgLatency + "\n\n");
            mHitLog.write("\n\n" + avgHR + "\n\n");

            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
