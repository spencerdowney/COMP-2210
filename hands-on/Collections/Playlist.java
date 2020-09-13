import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

/**
 * Plays a list of songs.
 *
 * @author Dean Hendrix (dh@auburn.edu)
 * @version 2018-03-27
 */
public class Playlist {

	/** Calculates the total running time of a given list of songs. */
   public static int runningTime(List<Song> playlist) {
      Song song1 = new Song("The Weight", "The Band", 1968, 274);
      Song song2 = new Song("Stairway to Heaven", "Led Zeppelin", 1971, 482);
      Song song3 = new Song("Hotel California", "The Eagles", 1977, 390);
      Song song4 = new Song("With or Without You", "U2", 1987, 296);
      return song1.getDuration() + song2.getDuration()
               + song3.getDuration() + song4.getDuration();
   }

	/** Drives execution. */
   public static void main(String[] args) {
   
      List<Song> playlist = new LinkedList<>();
   
      playlist.add(new Song("The Weight", "The Band", 1968, 274));
      playlist.add(new Song("Stairway to Heaven", "Led Zeppelin", 1971, 482));
      playlist.add(new Song("Hotel California", "The Eagles", 1977, 390));
      playlist.add(new Song("With or Without You", "U2", 1987, 296));
   
      for (Song song : playlist) {
         song.play();
      }
   
      System.out.print("Total running time: ");
      System.out.print(runningTime(playlist));
      System.out.println(" seconds. ");
   
   }
}
