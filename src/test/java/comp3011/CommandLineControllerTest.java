package comp3011;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
class CommandLineControllerTest {
    @Test
    void parsesStackedProcessorsInOrderAndAllowsRepeats() {
        CommandLineController controller =
                new CommandLineController(new String[] {"-nssn", "--help"});
        List<FrameProcessor> processors = controller.getFrameProcessors();

        assertNull(controller.getErrorMessage());
        assertEquals(4, processors.size());
        assertEquals(FrameNumberer.class, processors.get(0).getClass());
        assertEquals(FrameScratcher.class, processors.get(1).getClass());
        assertEquals(FrameScratcher.class, processors.get(2).getClass());
        assertEquals(FrameNumberer.class, processors.get(3).getClass());
    }
    @Test
    void parsesMixedLongShortAndStackedProcessors() {
        CommandLineController controller = new CommandLineController(
                new String[] {"-jf","--dust-frames","-d","--number-frames","--help"});
        List<FrameProcessor> processors = controller.getFrameProcessors();
        assertNull(controller.getErrorMessage());
        assertEquals(5, processors.size());
        assertEquals(FrameJitterer.class, processors.get(0).getClass());
        assertEquals(FrameFlickerer.class, processors.get(1).getClass());
        assertEquals(FrameDuster.class, processors.get(2).getClass());
        assertEquals(FrameDuster.class, processors.get(3).getClass());
        assertEquals(FrameNumberer.class, processors.get(4).getClass());
    }
    @Test
    void rejectsInvalidStackWithoutKeepingPartialProcessors() {
        CommandLineController controller =
                new CommandLineController(new String[] {"-nsq", "--help"});

        assertEquals("Unknown option: -nsq", controller.getErrorMessage());
        assertEquals(0, controller.getFrameProcessors().size());
    }
}