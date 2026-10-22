package comp3011;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Verifies that end of stream markers preserve thier playback generation and contain no normal frame payload
 * Developed with ai guidance. This work is my own 
 */
public class DecodedFrameTest {
	@Test
	void createsEndOfStreamMarkerForCorrectGeneration() {
		DecodedFrame marker = DecodedFrame.endOfStream(42);
		assertEquals(42,marker.playbackGeneration());
		assertTrue(marker.isEndOfStream());
		assertNull(marker.frame());
		assertNull(marker.info());
	}
}
