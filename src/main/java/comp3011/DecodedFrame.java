// Comp3011 Assignment 2 by Ishan Soni -- A2980941

package comp3011;
import org.bytedeco.javacv.Frame;

//one decoded video frame waiting for a processing worker
// The frame pixels are mutable but after this object enters the processing queue exactly one worker owns and modifies that frame.

public record DecodedFrame (
	long playbackGeneration,
	Frame frame,
	InfoFrame info) {
	public static DecodedFrame endOfStream(long generation) {
		return new DecodedFrame(generation,null,null);
	}
	public boolean isEndOfStream() {
		return frame ==null;
	}
}
