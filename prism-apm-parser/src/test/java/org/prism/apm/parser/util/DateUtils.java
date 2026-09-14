package org.prism.apm.parser.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

  public static Date strToDate(String strDate) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Date strtodate = null;

    try {
      strtodate = formatter.parse(strDate);
    } catch (ParseException e) {
      return null;
    }

    return strtodate;
  }

  public static void main(String[] args) {
    Date date = DateUtils.strToDate("2021-06-06 01:01:01");
    System.out.println(date);
  }
}
