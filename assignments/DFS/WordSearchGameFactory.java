
/**
 * Provides a factory method for creating word search games. 
 *
 * @author Spencer Downey (swd0008@auburn.edu)
 * @author Dean Hendrix (dh@auburn.edu)
 * @version TODAY
 */
public class WordSearchGameFactory {

   /**
    * Returns an instance of a class that implements the WordSearchGame
    * interface.
    */
   public static WordSearchGame createGame() {
      Boggle boggle = new Boggle();
      return boggle;
   }

}
