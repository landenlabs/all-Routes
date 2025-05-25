/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.logger;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Application top level logger, defines flavors of logging used to group common activities
 * together like  Cache, Http, GetData, etc.
 * <p>
 * Each enumeration can set its default logging enable state. Logging is only possible if
 * both the items logging is enabled and the global Min Level is below the log level.
 *
 * <p>
 * Example usage (where XXXX is your logging enumeration group:
 * <Pre color="red">
 * AppLog.LOG_XXXX.d().tagMsg(this, "mapViewWidth=", mapViewWidth);
 * </Pre>
 *
 * @see ALog
 */

public enum AppLog {

    LOG_HTTP(OutLog.LogAll),       // okHTTP network activity
    LOG_GETDATA(OutLog.LogAll),    // Weather data retrieval
    LOG_MAP(OutLog.LogError),      // Weather data retrieval
    ;

    private OutLog out = OutLog.LogAll;

    AppLog(OutLog outLog) {
        out = outLog;
    }

    // Logging levels.
    //
    public ALog v() {
        return out.v();
    }

    public ALog d() {
        return out.d();
    }

    public ALog i() {
        return out.i();
    }

    public ALog w() {
        return out.w();
    }

    public ALog e() {
        return out.e();
    }

    public ALog a() {
        return out.a();
    }

    enum OutLog {
        LogAll(),

        LogError() {
            ALog v() {
                return ALog.none;
            }

            ALog d() {
                return ALog.none;
            }

            ALog i() {
                return ALog.none;
            }

            ALog w() {
                return ALog.none;
            }

            ALog e() {
                return ALog.e;
            }

            ALog a() {
                return ALog.a;
            }
        };

        ALog v() {
            return ALog.v;
        }

        ALog d() {
            return ALog.d;
        }

        ALog i() {
            return ALog.i;
        }

        ALog w() {
            return ALog.w;
        }

        ALog e() {
            return ALog.e;
        }

        ALog a() {
            return ALog.a;
        }
    }

    // public boolean isLogAll() {  return out == LogAll;  }
}

