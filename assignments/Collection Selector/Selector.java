import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Defines a library of selection methods on Collections.
 *
 * @author  Spencer Downey (swd0008@auburn.edu)
 * @author  Dean Hendrix (dh@auburn.edu)
 * @version 9/10
 *
 */
public final class Selector {

/**
 * Can't instantiate this class.
 *
 * D O   N O T   C H A N G E   T H I S   C O N S T R U C T O R
 *
 */
   private Selector() { }


   /**
    * Returns the minimum value in the Collection coll as defined by the
    * Comparator comp. If either coll or comp is null, this method throws an
    * IllegalArgumentException. If coll is empty, this method throws a
    * NoSuchElementException. This method will not change coll in any way.
    *
    * @param coll    the Collection from which the minimum is selected
    * @param comp    the Comparator that defines the total order on T
    * @return        the minimum value in coll
    * @throws        IllegalArgumentException as per above
    * @throws        NoSuchElementException as per above
    */
   public static <T> T min(Collection<T> coll, Comparator<T> comp) {
   
      if (coll == null || comp == null) {
         throw new IllegalArgumentException("No collection or comparator.");
      }
      if (coll.isEmpty()) {
         throw new NoSuchElementException("Collection is empty");
      }
      
      T target = coll.iterator().next();
      for (T element : coll) {
         if (comp.compare(element, target) < 0) {
            target = element;
         }
      }
      return target;
   }


   /**
    * Selects the maximum value in the Collection coll as defined by the
    * Comparator comp. If either coll or comp is null, this method throws an
    * IllegalArgumentException. If coll is empty, this method throws a
    * NoSuchElementException. This method will not change coll in any way.
    *
    * @param coll    the Collection from which the maximum is selected
    * @param comp    the Comparator that defines the total order on T
    * @return        the maximum value in coll
    * @throws        IllegalArgumentException as per above
    * @throws        NoSuchElementException as per above
    */
   public static <T> T max(Collection<T> coll, Comparator<T> comp) {
     
      if (coll == null || comp == null) {
         throw new IllegalArgumentException("No collection or comparator.");
      }
      
      if (coll.isEmpty()) {
         throw new NoSuchElementException("Collection is empty");
      }
      
      T target = coll.iterator().next();
      for (T element : coll) {
         if (comp.compare(element, target) > 0) {
            target = element;
         }
      }
      return target;
   }


   /**
    * Selects the kth minimum value from the Collection coll as defined by the
    * Comparator comp. If either coll or comp is null, this method throws an
    * IllegalArgumentException. If coll is empty or if there is no kth minimum
    * value, this method throws a NoSuchElementException. This method will not
    * change coll in any way.
    *
    * @param coll    the Collection from which the kth minimum is selected
    * @param k       the k-selection value
    * @param comp    the Comparator that defines the total order on T
    * @return        the kth minimum value in coll
    * @throws        IllegalArgumentException as per above
    * @throws        NoSuchElementException as per above
    */
   public static <T> T kmin(Collection<T> coll, int k, Comparator<T> comp) {
      
      if (coll == null || comp == null) {
         throw new IllegalArgumentException("No collection or comparator.");
      }
      
      if (coll.isEmpty() || coll.size() < k) {
         throw new NoSuchElementException("Collection is empty");
      }
     
      if (coll.size() == 1 && k == 1) {
         return coll.iterator().next();
      }
     
      ArrayList<T> copy = new ArrayList<T>(coll);
      java.util.Collections.sort(copy, comp);
      T previous = copy.iterator().next();
      int uniqueCount = 1;
      ArrayList<T> uniques = new ArrayList<T>(uniqueCount);
      boolean first = true;
      
      for (T current : copy) {
         if (first) {
            previous = current;
            uniques.add(current);
            uniqueCount++;
            first = false;
         }
         if (current != previous) {
            
            uniques.add(current);
            previous = current;
            uniqueCount++;
         }
         
      }
      
      if (uniques.size() < k || k < 1) {
         throw new NoSuchElementException();
      }
      
      return uniques.get(k - 1);
   }


   /**
    * Selects the kth maximum value from the Collection coll as defined by the
    * Comparator comp. If either coll or comp is null, this method throws an
    * IllegalArgumentException. If coll is empty or if there is no kth maximum
    * value, this method throws a NoSuchElementException. This method will not
    * change coll in any way.
    *
    * @param coll    the Collection from which the kth maximum is selected
    * @param k       the k-selection value
    * @param comp    the Comparator that defines the total order on T
    * @return        the kth maximum value in coll
    * @throws        IllegalArgumentException as per above
    * @throws        NoSuchElementException as per above
    */
   public static <T> T kmax(Collection<T> coll, int k, Comparator<T> comp) {
      
      if (coll == null || comp == null) {
         throw new IllegalArgumentException("No collection or comparator.");
      }
      
      if (coll.isEmpty() || coll.size() < k) {
         throw new NoSuchElementException("Collection is empty");
      }
      
      if (coll.size() == 1 && k == 1) {
         return coll.iterator().next();
      }
      
      ArrayList<T> copy = new ArrayList<T>(coll);
      java.util.Collections.sort(copy, java.util.Collections.reverseOrder(comp));
      T previous = copy.iterator().next();
      int uniqueCount = 1;
      ArrayList<T> uniques = new ArrayList<T>(uniqueCount);
      boolean first = true;
      
      for (T current : copy) {
         if (first) {
            previous = current;
            uniques.add(current);
            uniqueCount++;
            first = false;
         }
         if (current != previous) {
            
            uniques.add(current);
            previous = current;
            uniqueCount++;
         }
         
      }
      
      if (uniques.size() < k || k < 1) {
         throw new NoSuchElementException();
      }
      
      return uniques.get(k - 1);
   }


   /**
    * Returns a new Collection containing all the values in the Collection coll
    * that are greater than or equal to low and less than or equal to high, as
    * defined by the Comparator comp. The returned collection must contain only
    * these values and no others. The values low and high themselves do not have
    * to be in coll. Any duplicate values that are in coll must also be in the
    * returned Collection. If no values in coll fall into the specified range or
    * if coll is empty, this method throws a NoSuchElementException. If either
    * coll or comp is null, this method throws an IllegalArgumentException. This
    * method will not change coll in any way.
    *
    * @param coll    the Collection from which the range values are selected
    * @param low     the lower bound of the range
    * @param high    the upper bound of the range
    * @param comp    the Comparator that defines the total order on T
    * @return        a Collection of values between low and high
    * @throws        IllegalArgumentException as per above
    * @throws        NoSuchElementException as per above
    */
   public static <T> Collection<T> range(Collection<T> coll, T low, T high,
                                         Comparator<T> comp) {
      if (coll == null || comp == null) {
         throw new IllegalArgumentException("No collection or comparator.");
      }
      
      if (coll.isEmpty()) {
         throw new NoSuchElementException("Collection is empty");
      }
      
      ArrayList<T> copy = new ArrayList<T>(coll);
      ArrayList<T> range = new ArrayList<T>(0);
      for (T element : copy) {
         if (comp.compare(element, low) >= 0 && comp.compare(element, high) <= 0) {
            range.add(element);
         }
      }
      
      if (range.size() == 0) {
         throw new NoSuchElementException("No elements within the range.");
      }
      
      return range;
   }


   /**
    * Returns the smallest value in the Collection coll that is greater than
    * or equal to key, as defined by the Comparator comp. The value of key
    * does not have to be in coll. If coll or comp is null, this method throws
    * an IllegalArgumentException. If coll is empty or if there is no
    * qualifying value, this method throws a NoSuchElementException. This
    * method will not change coll in any way.
    *
    * @param coll    the Collection from which the ceiling value is selected
    * @param key     the reference value
    * @param comp    the Comparator that defines the total order on T
    * @return        the ceiling value of key in coll
    * @throws        IllegalArgumentException as per above
    * @throws        NoSuchElementException as per above
    */
   public static <T> T ceiling(Collection<T> coll, T key, Comparator<T> comp) {
      
      if (coll == null || comp == null) {
         throw new IllegalArgumentException("No collection or comparator.");
      }
      
      T ceiling = Selector.max(coll, comp);
      
      if (coll.isEmpty() || comp.compare(ceiling, key) < 0) {
         throw new NoSuchElementException("Collection is empty");
      }
      
      if (ceiling == key) {
         return ceiling;
      }
      
      for (T e : coll) {
         if (comp.compare(e, ceiling) <= 0 && comp.compare(e, key) >= 0) {
            ceiling = e;
         }
      }
      return ceiling;
   }


   /**
    * Returns the largest value in the Collection coll that is less than
    * or equal to key, as defined by the Comparator comp. The value of key
    * does not have to be in coll. If coll or comp is null, this method throws
    * an IllegalArgumentException. If coll is empty or if there is no
    * qualifying value, this method throws a NoSuchElementException. This
    * method will not change coll in any way.
    *
    * @param coll    the Collection from which the floor value is selected
    * @param key     the reference value
    * @param comp    the Comparator that defines the total order on T
    * @return        the floor value of key in coll
    * @throws        IllegalArgumentException as per above
    * @throws        NoSuchElementException as per above
    */
   public static <T> T floor(Collection<T> coll, T key, Comparator<T> comp) {
      
      if (coll == null || comp == null) {
         throw new IllegalArgumentException("No collection or comparator.");
      }
      
      T floor = Selector.min(coll, comp);
      
      if (coll.isEmpty() || comp.compare(floor, key) > 0) {
         throw new NoSuchElementException("Collection is empty");
      }
      
      if (floor == key) {
         return floor;
      }
      
      for (T e : coll) {
         if (comp.compare(e, floor) >= 0 && comp.compare(e, key) <= 0) {
            floor = e;
         }
      }
      return floor;
   }
   
}
