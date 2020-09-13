import java.util.Arrays;

/**
* Defines a library of selection methods
* on arrays of ints.
*
* @author   Spencer Downey (swd0008@auburn.edu)
* @author   Dean Hendrix (dh@auburn.edu)
* @version  8/24/18
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
    * Selects the minimum value from the array a. This method
    * throws IllegalArgumentException if a is null or has zero
    * length. The array a is not changed by this method.
    */
   public static int min(int[] a) 
                  throws IllegalArgumentException {
                      
      //Throws IllegalArgumentException if array length is insufficient.
      if (a == null || a.length == 0) {
      
         throw new IllegalArgumentException("Array length "
                  + "must be at least one integer");
      }       
      int min = a[0];
      for (int i = 0; i < a.length; i++) {
         if (a[i] < min) {
            min = a[i];
         }
      }
      return min;
   }


   /**
    * Selects the maximum value from the array a. This method
    * throws IllegalArgumentException if a is null or has zero
    * length. The array a is not changed by this method.
    */
   public static int max(int[] a) 
                     throws IllegalArgumentException {
      
      //Throws IllegalArgumentException if array length is insufficient.
      if (a == null || a.length == 0) {
      
         throw new IllegalArgumentException("Array length "
                  + "must be at least one integer");
      }
      int max = a[0];
      for (int i = 0; i < a.length; i++) {
         if (a[i] > max) {
            max = a[i];
         }
      }
      return max;
   }


   /**
    * Selects the kth minimum value from the array a. This method
    * throws IllegalArgumentException if a is null, has zero length,
    * or if there is no kth minimum value. Note that there is no kth
    * minimum value if k < 1, k > a.length, or if k is larger than
    * the number of distinct values in the array. The array a is not
    * changed by this method.
    */
   public static int kmin(int[] a, int k) {
   
      //Throws IllegalArgumentException if array length is insufficient.      
      if (a == null || a.length == 0) {
      
         throw new IllegalArgumentException("Array length "
                  + "must be at least one integer");
      }
      
      if (k < 1 || k > a.length) {
         throw new IllegalArgumentException("There is no kth minimum value");
      }
      
      int[] b = Arrays.copyOf(a, a.length);
      Arrays.sort(b);
      
      //Determine how many duplicates are in the array.
      int duplicates = 0;
      for (int i = 1; i < b.length; i++) {
         if (b[i] == b[i - 1]) {
            duplicates++;
         }
      }
      
      int uniques = b.length - duplicates;
      if (k > uniques) {
         throw new IllegalArgumentException("K is too large");
      }
      
      //Sets up the new array.
      int[] c = new int[uniques];
      c[0] = b[0];
      int j = 1;
      for (int i = 1; i < b.length; i++) {
         if (b[i] != b[i - 1]) {
            c[j] = b[i];
            j++;
         }
      }
      
      return c[k - 1];
   }


   /**
    * Selects the kth maximum value from the array a. This method
    * throws IllegalArgumentException if a is null, has zero length,
    * or if there is no kth maximum value. Note that there is no kth
    * maximum value if k < 1, k > a.length, or if k is larger than
    * the number of distinct values in the array. The array a is not
    * changed by this method.
    */
   public static int kmax(int[] a, int k) {
   
     //Throws IllegalArgumentException if array length is insufficient.      
      if (a == null || a.length == 0) {
      
         throw new IllegalArgumentException("Array length "
                  + "must be at least one integer");
      }
      else if (k < 1 || k > a.length) {
         throw new IllegalArgumentException("There is no kth minimum value");
      }
      
      //Establishes array b that is the same as a.
      int[] b = Arrays.copyOf(a, a.length);
      Arrays.sort(b);
      
      //Determine how many duplicates are in the array.
      int duplicates = 0;
      for (int i = 1; i < b.length; i++) {
         if (b[i] == b[i - 1]) {
            duplicates++;
         }
      }
      
      int uniques = b.length - duplicates;
      if (k > uniques) {
         throw new IllegalArgumentException("K is too large");
      }
      
      //Sets up the new array.
      int[] c = new int[b.length - duplicates];
      c[0] = b[0];
      int j = 1;
      for (int i = 1; i < a.length; i++) {
         if (b[i] != b[i - 1]) {
            c[j] = b[i];
            j++;
         }
      }
      
      return c[c.length - k];
   }


   /**
    * Returns an array containing all the values in a in the
    * range [low..high]; that is, all the values that are greater
    * than or equal to low and less than or equal to high,
    * including duplicate values. The length of the returned array
    * is the same as the number of values in the range [low..high].
    * If there are no qualifying values, this method returns a
    * zero-length array. Note that low and high do not have
    * to be actual values in a. This method throws an
    * IllegalArgumentException if a is null or has zero length.
    * The array a is not changed by this method.
    */
   public static int[] range(int[] a, int low, int high) 
                  throws IllegalArgumentException {
                  
      //Throws IllegalArgumentException if array length is insufficient.      
      if (a == null || a.length == 0) {
      
         throw new IllegalArgumentException("Array length "
                  + "must be at least one integer");
      }
      
      //Counts the amount of numbers within the range.
      int counter = 0;
      for (int i = 0; i < a.length; i++) {
         if ((a[i] <= high) && (a[i] >= low)) {
            counter++;
         }
      }
      
      //Puts the numbers withing the range into the array.
      {
         int[] b = new int[counter];
         int j = 0;
        
         for (int i = 0; i < a.length; i++) {
            if ((a[i] <= high) && (a[i] >= low)) {
               b[j] = a[i];
               j++;
            }
         }
         return b;
      }
   }


   /**
    * Returns the smallest value in a that is greater than or equal to
    * the given key. This method throws an IllegalArgumentException if
    * a is null or has zero length, or if there is no qualifying
    * value. Note that key does not have to be an actual value in a.
    * The array a is not changed by this method.
    */
   public static int ceiling(int[] a, int key) {
   
     //Throws IllegalArgumentException if array length is insufficient.      
      if (a == null || a.length == 0) {
      
         throw new IllegalArgumentException("Array length "
                  + "must be at least one integer");
      }
      
      int ceiling = Selector.max(a);
      
      if (ceiling < key) {
         throw new IllegalArgumentException("No qualifying value found");
      }
      
      for (int i = 0; i < a.length; i++) {
         if (a[i] >= key && a[i] < ceiling) {
            ceiling = a[i];
         }
      }
      return ceiling;
   }


   /**
    * Returns the largest value in a that is less than or equal to
    * the given key. This method throws an IllegalArgumentException if
    * a is null or has zero length, or if there is no qualifying
    * value. Note that key does not have to be an actual value in a.
    * The array a is not changed by this method.
    */
   public static int floor(int[] a, int key) {
   
      //Throws IllegalArgumentException if array length is insufficient.      
      if (a == null || a.length == 0) {
      
         throw new IllegalArgumentException("Array length "
                  + "must be at least one integer");
      }
      
      int floor = Selector.min(a);
      
      if (floor > key) {
         throw new IllegalArgumentException("No qualifying value found");
      }
      
      for (int i = 0; i < a.length; i++) {
         if (a[i] <= key && a[i] > floor) {
            floor = a[i];
         }
      }
      return floor;
   }

}
