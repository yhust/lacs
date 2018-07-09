package alluxio.master.file.options;

/**
 * Created by yuyinghao on 6/7/2018.
 */

import alluxio.annotation.PublicApi;
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

    private int userId; // the user requesting a token

    /**
     * @return the default {@link GetLATokenOptions}
     */
    public static GetLATokenOptions defaults() {
        return new GetLATokenOptions();
    }


    public int getUserId() {
        return userId;
    }

    public void setUserId(int id) {
        userId = id;
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
        return Objects.equal(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("userId", userId)
                .toString();
    }

    /**
     * @return thrift representation of the options
     */
    public GetLATokenTOptions toThrift() {
        GetLATokenTOptions options = new GetLATokenTOptions();
        options.setUserId(userId);
        return options;
    }
}