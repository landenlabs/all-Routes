/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.pages.PagesSmartAux;

import static landenlabs.routes.pages.PagesSmartAux.SmartAlert.DDI_NAMES;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import landenlabs.routes.R;
import landenlabs.routes.utils.Ui;

import org.joda.time.DateTime;

import java.util.Locale;

import landenlabs.wx_lib_data.location.WxLocationEx;
import landenlabs.wx_lib_data.logger.ALog;

/**
 * Recycler View holder for SmartAlerts
 */
public class SmartItemHolder extends RecyclerView.ViewHolder {

    @ColorInt
    private static final int COLOR_ORANGE = 0xffffa500;

    TextView nameTv;
    ImageView ddiIconIv;
    TextView alertTv;
    TextView ddiTv;
    TextView rateTv;
    TextView timeTv;
    View detailHolder;
    SmartViewHelper helper;

    public SmartItemHolder(@NonNull View view, SmartViewHelper helper) {
        super(view);
        this.helper = helper;
        nameTv = itemView.findViewById(R.id.name);
        ddiIconIv = itemView.findViewById(R.id.smart_ddi_icon);
        detailHolder = itemView.findViewById(R.id.row_items);
        alertTv = itemView.findViewById(R.id.smart_alert);
        ddiTv = itemView.findViewById(R.id.smart_ddi);
        rateTv = itemView.findViewById(R.id.smart_rate);
        timeTv = itemView.findViewById(R.id.smart_time);
    }

    public void onBindViewHolder(SmartAlert smartAlert, int position) {
        try {
            itemView.setTag(helper.clickTagId, position);
            itemView.setOnClickListener(helper.onClick);
            ddiIconIv.setImageResource(smartAlert.getImageRes(ddiIconIv.getContext()));
            @ColorInt int iconBgColor = Color.WHITE;
            if (smartAlert.alert != null) {
                switch (smartAlert.alert.significance) {
                    case SmartAlert.SIG_WARNING:
                        iconBgColor = Color.RED;
                        break;
                    case SmartAlert.SIG_WATCH:
                        iconBgColor = COLOR_ORANGE;
                        break;
                    case SmartAlert.SIG_ADVISORY:
                        iconBgColor = Color.YELLOW;
                        break;
                }
            }
            ddiIconIv.setBackgroundColor(iconBgColor);
            if (detailHolder != null) {
                nameTv.setText(String.format(Locale.US, "[%d] %s", position, WxLocationEx.fmtLocationName(smartAlert.wxLocation)));
                if (smartAlert.hasValidData) {
                    ddiTv.setText(DDI_NAMES[smartAlert.drivingDifficultyIndex]);
                    rateTv.setText(String.format(Locale.US, "%.1f", Math.max(smartAlert.precipRate, smartAlert.snowRate)));
                    alertTv.setText("no Alert");
                    if (smartAlert.alert != null) {
                        alertTv.setText(smartAlert.alertType());
                    }

                    timeTv.setText(new DateTime(smartAlert.pt.milli).toString("hh:mm a"));
                } else {
                    String none = "--";
                    ddiTv.setText(none);
                    rateTv.setText(none);
                    alertTv.setText(none);
                }
            } else {
                Ui.setVisibleIf(View.GONE, nameTv);
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(this, "Smart Alert presentation error ", ex);
        }
    }

    public static SmartItemHolder createTrackHolderFor(@NonNull ViewGroup parent, int viewType, SmartViewHelper helper) {
        View view = LayoutInflater
                .from(parent.getContext()).inflate(R.layout.list_smart_row, parent, false);
        return new SmartItemHolder(view, helper);
    }
}
