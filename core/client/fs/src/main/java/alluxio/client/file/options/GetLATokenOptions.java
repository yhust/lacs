package alluxio.client.file.options;

/**
 * Created by yuyinghao on 6/7/2018.
 */

import alluxio.Configuration;
import alluxio.PropertyKey;
import alluxio.annotation.PublicApi;
import alluxio.thrift.GetStatusTOptions;
import alluxio.wire.LoadMetadataType;
import alluxio.thrift.GetLATokenTOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;


import javax.annotation.concurrent.NotThreadSafe;

/**
 * Method options for acquiring the token to access a file.
 */
@PublicApi
@NotThreadSafe
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class GetLATokenOptions {

    private int mUserId; // the user requesting a token

    /**
     * @return the default {@link GetLATokenOptions}
     */
    public static GetLATokenOptions defaults() {
        return new GetLATokenOptions();
    }

    public GetLATokenOptions() {}
    public GetLATokenOptions(int userId) {mUserId =userId;}
    /**
     * Create an instance of {@link GetLATokenOptions} from a {@link GetLATokenTOptions}.
     *
     * @param options the thrift representation of getLAToken options
     */
    public GetLATokenOptions(GetLATokenTOptions options) {
        GetLATokenOptions getLATokenOptions = new GetLATokenOptions();
        getLATokenOptions.setUserId(options.getUserId());
    }

    public int getUserId() {
        return mUserId;
    }

    public void setUserId(int id) {
        mUserId = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetLATokenOptions)) {
            return false;
        }
        GetLATokenOptions that = (GetLATokenOptions) o;
        return Objects.equal(mUserId, that.mUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mUserId);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("mUserId", mUserId)
                .toString();
    }

    /**
     * @return thrift representation of the options
     */
    public GetLATokenTOptions toThrift() {
        GetLATokenTOptions options = new GetLATokenTOptions();
        options.setUserId(mUserId);
        return options;
    }
}