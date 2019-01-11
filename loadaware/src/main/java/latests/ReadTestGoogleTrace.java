package latests;

import alluxio.client.LoadAwareFileReader;
import alluxio.util.CommonUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by yuyinghao on 1/8/19.
 *
 * Exponentially distributed intervals
 *
 */
public class ReadTestGoogleTrace {

    private static FileWriter mTimeLog;
    private static FileWriter mHitLog=null;
    private static File mPopFile;
    private int mTrial;
    private int mFileNumber;
    private RandomNumberGenerator mRandomNumberGenerator;
    private int mUserId;
    private int mUserType;
    private List<Double> mIntervals=new ArrayList<>();

    public ReadTestGoogleTrace(int fileNumber, int trial, int userId, File intervalFile, File popFile, FileWriter log){
        mFileNumber = fileNumber;
        mTrial = trial;
        mUserId = userId;
        mTimeLog = log;
        mPopFile = popFile;
        mRandomNumberGenerator=new RandomNumberGenerator();
        mUserId = userId;
        // load the preferences
        try{
            String popString = FileUtils.readLines(mPopFile).get(userId-1);
            System.out.println("Pop file:" + popString);
            String[] pops = popString.split(",");

            for(int i=0;i<pops.length;i++) {
                mRandomNumberGenerator.addNumber(i, Double.parseDouble(pops[i]));
            }

            // get the intervals
            List<String> intervals = FileUtils.readLines(intervalFile);
            for(String interval:intervals)
                mIntervals.add(Double.parseDouble(interval));
        }catch(IOException e){
            e.printStackTrace();
        }

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
                results.add(executorService.submit(new LoadAwareFileReader(fileId, 0)));

                // Access interval
                // Double interval = new ExponentialDistribution(1.0 / mRate).sample();
                int randNum = ThreadLocalRandom.current().nextInt(0,mIntervals.size());
                Double interval = mIntervals.get(randNum);
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
