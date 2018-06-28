package alluxio.client.file.policy;

import alluxio.client.block.BlockWorkerInfo;
import alluxio.client.block.policy.BlockLocationPolicy;
import alluxio.client.block.policy.options.GetWorkerOptions;
import alluxio.wire.WorkerNetAddress;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by yyuau on 28/6/2018.
 */
@NotThreadSafe
public final class LoadAwarePolicy implements FileWriteLocationPolicy, BlockLocationPolicy {
  private List<BlockWorkerInfo> mWorkerInfoList;
  private boolean mInitialized = false;
  /** This caches the {@link WorkerNetAddress} for the block IDs.*/
  private final HashMap<Long, WorkerNetAddress> mBlockLocationCache = new HashMap<>();
  private int mWorkerId = 0;
  /**
   * Constructs a new {@link LoadAwarePolicy}.
   */
  public LoadAwarePolicy(int workerId){ mWorkerId = workerId;}

  @Override
  public WorkerNetAddress getWorkerForNextBlock(Iterable<BlockWorkerInfo> workerInfoList, long blockSizeBytes) {
    if (!mInitialized) {
      mWorkerInfoList = Lists.newArrayList(workerInfoList);
      mInitialized = true;
    }
    WorkerNetAddress candidate = mWorkerInfoList.get(mWorkerId).getNetAddress();
    return candidate;
  }

  @Override
  public WorkerNetAddress getWorker(GetWorkerOptions options) {
    WorkerNetAddress address = mBlockLocationCache.get(options.getBlockId());
    if (address != null) {
      return address;
    }
    address = getWorkerForNextBlock(options.getBlockWorkerInfos(), options.getBlockSize());
    mBlockLocationCache.put(options.getBlockId(), address);
    return address;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LoadAwarePolicy)) {
      return false;
    }
    LoadAwarePolicy that = (LoadAwarePolicy) o;
    return Objects.equal(mWorkerInfoList, that.mWorkerInfoList)
            && Objects.equal(mInitialized, that.mInitialized)
            && Objects.equal(mBlockLocationCache, that.mBlockLocationCache)
            && Objects.equal(mWorkerId, that.mWorkerId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mWorkerInfoList, mInitialized, mBlockLocationCache);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
            .add("workerInfoList", mWorkerInfoList)
            .add("initialized", mInitialized)
            .add("blockLocationCache", mBlockLocationCache)
            .toString();
  }

  public void setWorkerId(int workerId){
    mWorkerId = workerId;
  }

}
