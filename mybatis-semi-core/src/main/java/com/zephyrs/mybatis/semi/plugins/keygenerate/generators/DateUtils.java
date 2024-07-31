package com.zephyrs.mybatis.semi.plugins.keygenerate.generators;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间工具类
 * @author Zephy
 */
public class DateUtils
{
    public static String yyyyMMddHHmmss = "yyyyMMddHHmmss";

    public static String dateTimeNow()
    {
        return dateTimeNow(yyyyMMddHHmmss);
    }
    public static String dateTimeNow(final String format)
    {
        return parseDateToStr(format, new Date());
    }

    public static String parseDateToStr(final String format, final Date date)
    {
        return new SimpleDateFormat(format).format(date);
    }

}
