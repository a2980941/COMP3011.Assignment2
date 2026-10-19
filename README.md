# COMP3011 Assignment 2 - Java Local Project

For more information on this code and how to get started, see https://learn.adelaide.edu.au/courses/30747/assignments/94505


## Threading Design
The original player decodes a frame, applies every selected effect, converts frame, displays it and handles GUI events on the JavaFX thread. This causes late frames and an unresponsive interface when many effects are selected

The new design uses a bounded processing pipeline

```text
JavaFX controls -> Decoder thread-> decoded-frame-queue-> processing-worker-pool

decoded-frame-queue -> prepared-frame-queue -> JavaFX display and audio thread
```
The decoder thread is the only thing that accesses the FFmpeg frame grabber. It reads frames in media order and puts them into a bounded queue
Each processing worker owns its own sequence of frameProcesser instances
A worker applies that sequence to one frame in command-line-order. Effects within a frame must remain sequential because they modify the same pixels.
Different frames can be processed concurrently by different workers

The JavaFX thread remains responsible for buttons, keyboard input, window resizing, audio output and presenting frames. It must never wait for a worker.

Bounded queues prevent unlimited memory use and apply back pressure when one stage is slower than the other
Seeking, stopping, restarting and closing invalidate queued work from the old playback generation. The player clears both queues and starts fresh work for the new playback position

## Concurrency Safety
- Only the decoder thread accesses FFmpegFrameGrabber
- Only one worker uses a particular FrameProcesser instance at a time.
- Thread safe queues transfer frames between pipeline stages
- Only the JavaFX thread updates the view and audio output
- A playback generation identifier prevents old frames from being displayed after seeking or restarting
- Shutdown signals workers, clear queues, waits for worker termination then closes playback resources
