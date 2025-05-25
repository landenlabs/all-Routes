/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package landenlabs.wx_lib_data.wx;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wsi.wxdata.BaseData;
import com.wsi.wxdata.BaseWeatherData;
import com.wsi.wxdata.BaseWxFetcher;


/**
 * Weather Data Query Status.
 * See WxDataHolder and DataUtils.
 *
 *  Similar to:
 *     WxStatusData
 *     WxStatusFetch
 *     WxStatusInit
 */
public class WxStatusData<TT extends BaseWeatherData> {

    public BaseWxFetcher<? extends BaseData> fetcher;       // BaseWxWeatherBasedFetcher
    public TT data;
    public Throwable error;

    @SuppressWarnings("unchecked")
    public WxStatusData(@NonNull BaseWxFetcher<? extends BaseData> fetcher, @Nullable Object data, @Nullable Throwable error) {
        this.fetcher = fetcher;
        this.data = (TT) data;
        this.error = error;
    }

    /*
    public void postIf(TT prevData, LiveQueue<WxStatusData<TT>> loadEvent) {
        if (loadEvent != null) {
            boolean post = false;
            if (prevData == null) {
                post = true;
            } else {
                WxWeatherSample sample = prevData.asSample();
                long prevMilli = (sample != null) ? sample.validTimeLocal.getMillis() : 0;
                long newMilli = (data != null && (sample = data.asSample()) != null) ? sample.validTimeLocal.getMillis() : 0;
                post = (newMilli != prevMilli);
            }
            if (post) {
                loadEvent.postValue(this);
            }
        }
    }
     */
}
