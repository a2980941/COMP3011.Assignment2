//Assignment 2 COMP3011 -Ishan Soni -- A2980941

package comp3011;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean; 
import java.util.function.Consumer;

// runs one frame processor on its own thread
// frames enter from the input queue and are processed in FIFO order and leave through the output queue
// one stage owns one frameProcesser instance so that processor is never used concurrently by different threads

//Ai guidance was used in developing this. This work is my own

public final class FrameProcessingStage implements Runnable{
	private static final long POLL_TIMEOUT_MS = 100;
    private final FrameProcessor processor;
    private final BlockingQueue<DecodedFrame> inputQueue;
    private final BlockingQueue<DecodedFrame> outputQueue;
    private final AtomicBoolean running;
    private final Consumer<Exception> errorHandler;
    public FrameProcessingStage(
            FrameProcessor processor,
            BlockingQueue<DecodedFrame> inputQueue,
            BlockingQueue<DecodedFrame> outputQueue,
            AtomicBoolean running,
            Consumer<Exception> errorHandler) {
        this.processor = processor;
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.running = running;
        this.errorHandler = errorHandler;
    }
    @Override
    public void run() {
        try {
            while (running.get() || !inputQueue.isEmpty()) {
                DecodedFrame decodedFrame =
                        inputQueue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (decodedFrame == null) {
                    continue;
                }
                if(decodedFrame.isEndOfStream()) {
                	outputQueue.put(decodedFrame);
                	return;
                }
                processor.process(decodedFrame.frame(), decodedFrame.info());
                outputQueue.put(decodedFrame);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            running.set(false);
            errorHandler.accept(exception);
        }
    }
}

