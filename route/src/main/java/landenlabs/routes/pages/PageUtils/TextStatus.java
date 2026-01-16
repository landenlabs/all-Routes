/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.pages.PageUtils;

import static landenlabs.routes.utils.SysUtils.joinCS;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Helper to manage status and error messages in TextView
 */
public class TextStatus {

    private TextView statusTv;

    public TextStatus(TextView statusTv) {
        this.statusTv = statusTv;
    }

    private void appendError(String msg, Throwable tr) {
        appendStatus(msg + tr.toString(), ALog.ERROR);
    }

    public void appendStatus(CharSequence msg, int level) {
        if (level >= ALog.WARN) {
            ALog.e.tagMsg(this, msg);
            SpannableString ss = new SpannableString(msg);
            ss.setSpan(new ForegroundColorSpan(Color.RED), 0, msg.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            appendMsg(ss);
        } else {
            ALog.i.tagMsg(this, msg);
            appendMsg(msg);
        }
    }

    public void appendMsg(CharSequence msg) {
        if (statusTv != null) {
            statusTv.post(() -> statusTv.setText(joinCS(msg, "\n",  getRows(10, statusTv) )));
        }
    }

    public CharSequence getRows(int maxRows, TextView tv) {
        CharSequence cs = tv.getText();
        for (int idx = 0; idx < cs.length(); idx++) {
            if (cs.charAt(idx) == '\n' && --maxRows == 0) {
                return cs.subSequence(0, idx);
            }
        }
        return cs;
    }
}
