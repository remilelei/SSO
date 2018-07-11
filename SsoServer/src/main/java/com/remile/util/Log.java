package com.remile.util;

import java.util.logging.Logger;

public class Log {
    private static Logger logger = Logger.getGlobal();

    /**
     * 信息级日志
     * @param TAG
     * @param logContent
     */
    public static void info(String TAG, String logContent) {
        logger.info(createLog(TAG, logContent));
    }

    /**
     * 警告级日志
     * @param TAG
     * @param logContent
     */
    public static void warning(String TAG, String logContent) {
        logger.warning(createLog(TAG, logContent));
    }

    /**
     * 严重级日志
     * @param TAG
     * @param logContent
     */
    public static void severe(String TAG, String logContent) {
        logger.severe(createLog(TAG, logContent));
    }

    private static String createLog(String TAG, String logContent) {
        StringBuffer sb = new StringBuffer(
                TAG.length() + 1 + logContent.length());
        sb.append(TAG)
                .append('|')
                .append(logContent);
        return sb.toString();
    }
}
