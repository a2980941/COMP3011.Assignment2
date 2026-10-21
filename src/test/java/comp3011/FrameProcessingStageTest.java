package comp3011;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.bytedeco.javacv.Frame;
import org.junit.jupiter.api.Test;

class FrameProcessingStageTest {

    @Test
    void forwardsEndOfStreamWithoutProcessingIt() throws Exception {
        BlockingQueue<DecodedFrame> inputQueue = new ArrayBlockingQueue<>(1);
        BlockingQueue<DecodedFrame> outputQueue = new ArrayBlockingQueue<>(1);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger processedFrames = new AtomicInteger();
        AtomicReference<Exception> error = new AtomicReference<>();
        FrameProcessor processor = new FrameProcessor() {
            @Override
            public void process(Frame frame, InfoFrame info) {
                processedFrames.incrementAndGet();
            }
        };
        FrameProcessingStage stage = new FrameProcessingStage(processor,inputQueue,outputQueue,running,error::set);
        Thread thread = new Thread(stage);
        thread.start();
        DecodedFrame marker = DecodedFrame.endOfStream(7);
        inputQueue.put(marker);
        DecodedFrame forwarded = outputQueue.poll(1, TimeUnit.SECONDS);
        thread.join(1_000);
        assertSame(marker, forwarded);
        assertTrue(forwarded.isEndOfStream());
        assertEquals(0, processedFrames.get());
        assertNull(error.get());
        assertFalse(thread.isAlive());
    }
    @Test
    void processesFramesBeforeForwardingEndOfStream() throws Exception {
        BlockingQueue<DecodedFrame> inputQueue = new ArrayBlockingQueue<>(2);
        BlockingQueue<DecodedFrame> outputQueue = new ArrayBlockingQueue<>(2);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger processedFrames = new AtomicInteger();
        AtomicReference<Exception> error = new AtomicReference<>();

        FrameProcessor processor = new FrameProcessor() {
            @Override
            public void process(Frame frame, InfoFrame info) {
                processedFrames.incrementAndGet();
            }
        };
        FrameProcessingStage stage = new FrameProcessingStage(processor,inputQueue,outputQueue,running,error::set);

        Thread thread = new Thread(stage);
        thread.start();
        DecodedFrame frame = new DecodedFrame(7, new Frame(), new InfoFrame(1, 1_000));
        DecodedFrame marker = DecodedFrame.endOfStream(7);
        inputQueue.put(frame);
        inputQueue.put(marker);
        DecodedFrame firstResult = outputQueue.poll(1, TimeUnit.SECONDS);
        DecodedFrame secondResult = outputQueue.poll(1, TimeUnit.SECONDS);
        thread.join(1_000);
        assertSame(frame, firstResult);
        assertSame(marker, secondResult);
        assertEquals(1, processedFrames.get());
        assertNull(error.get());
        assertFalse(thread.isAlive());
    }
    @Test
    void stopsAndReportsProcessorFailure() throws Exception {
        BlockingQueue<DecodedFrame> inputQueue = new ArrayBlockingQueue<>(1);
        BlockingQueue<DecodedFrame> outputQueue = new ArrayBlockingQueue<>(1);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Exception> error = new AtomicReference<>();
        RuntimeException expected = new RuntimeException("processor failed");
        FrameProcessor processor = new FrameProcessor() {
            @Override
            public void process(Frame frame, InfoFrame info) {
                throw expected;
            }
        };
        FrameProcessingStage stage = new FrameProcessingStage(processor,inputQueue,outputQueue,running,error::set);
        Thread thread = new Thread(stage);
        thread.start();
        inputQueue.put(new DecodedFrame(7, new Frame(), new InfoFrame(1, 1_000)));
        thread.join(1_000);
        assertSame(expected, error.get());
        assertFalse(running.get());
        assertFalse(thread.isAlive());
        assertNull(outputQueue.poll());
    }
}