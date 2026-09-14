package org.prism.apm.parser.util;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.*;

/** Compares property values between two objects. https://blog.csdn.net/weixin_43882359/article/details/108576475 */
public class ComparisonUtils {

  /**
   * Compares two entity objects and returns true when their property values have no differences.
   *
   * @param oldObject first object to compare
   * @param newObject second object to compare
   * @return boolean property comparison result
   */
  public static boolean compareObject(Object oldObject, Object newObject) {
    Map<String, List<Object>> resultMap = compareFields(oldObject, newObject, null);

    if (resultMap.size() > 0) {
      return false;
    } else {
      return true;
    }
  }

  public static boolean compareObject(Object oldObject, Object newObject, String[] ignoreArr) {
    Map<String, List<Object>> resultMap = compareFields(oldObject, newObject, ignoreArr);

    if (resultMap.size() > 0) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Compares two entity objects and returns a map keyed by differing property names; each value
   * stores the oldObject and newObject values for that property.
   *
   * @param obj1 first object to compare
   * @param obj2 second object to compare
   * @param ignoreArr fields to ignore during comparison
   * @return map of property comparison differences
   */
  @SuppressWarnings("rawtypes")
  public static Map<String, List<Object>> compareFields(
      Object obj1, Object obj2, String[] ignoreArr) {
    try {
      Map<String, List<Object>> map = new HashMap<String, List<Object>>();
      List<String> ignoreList = null;
      if (ignoreArr != null && ignoreArr.length > 0) {
        // Convert array to list.
        ignoreList = Arrays.asList(ignoreArr);
      }
      if (obj1.getClass() == obj2.getClass()) { // Objects are comparable only when they have the same type.
        Class clazz = obj1.getClass();
        // Get property descriptors for the object.
        PropertyDescriptor[] pds =
            Introspector.getBeanInfo(clazz, Object.class).getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) { // All properties are available here.
          String name = pd.getName(); // Property name.
          if (ignoreList != null && ignoreList.contains(name)) { // Skip ignored properties.
            continue;
          }
          Method readMethod = pd.getReadMethod(); // Getter method.
          // Calling the getter on obj1 obtains the property value for obj1.
          Object o1 = readMethod.invoke(obj1);
          // Calling the getter on obj2 obtains the property value for obj2.
          Object o2 = readMethod.invoke(obj2);
          if (o1 instanceof Timestamp) {
            o1 = new Date(((Timestamp) o1).getTime());
          }
          if (o2 instanceof Timestamp) {
            o2 = new Date(((Timestamp) o2).getTime());
          }
          if (o1 == null && o2 == null) {
            continue;
          } else if (o1 == null && o2 != null) {
            List<Object> list = new ArrayList<Object>();
            list.add(o1);
            list.add(o2);
            map.put(name, list);
            continue;
          }
          if (!o1.equals(o2)) { // Add the values to the map when they differ.
            List<Object> list = new ArrayList<Object>();
            list.add(o1);
            list.add(o2);
            map.put(name, list);
          }
        }
      }
      return map;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
