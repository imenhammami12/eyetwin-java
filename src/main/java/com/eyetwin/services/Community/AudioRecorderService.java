package com.eyetwin.services.Community;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioRecorderService {

    private static final AudioFormat RECORDING_FORMAT =
            new AudioFormat(16000f, 16, 1, true, false);

    private TargetDataLine line;
    private Thread recordingThread;
    private volatile boolean recording;
    private File currentOutputFile;

    public synchronized void startRecording() throws Exception {
        if (recording) {
            return;
        }

        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "eyetwin-audio");
        Files.createDirectories(tempDir);

        currentOutputFile = tempDir.resolve("recording-" + System.currentTimeMillis() + ".wav").toFile();

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, RECORDING_FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("Microphone recording is not supported on this device.");
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(RECORDING_FORMAT);
        line.start();

        recording = true;

        recordingThread = new Thread(() -> {
            try (AudioInputStream audioStream = new AudioInputStream(line)) {
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, currentOutputFile);
            } catch (Exception ignored) {
            }
        }, "community-audio-recorder");

        recordingThread.setDaemon(true);
        recordingThread.start();
    }

    public synchronized File stopRecording() throws Exception {
        if (!recording) {
            return currentOutputFile;
        }

        recording = false;

        if (line != null) {
            line.stop();
            line.close();
        }

        if (recordingThread != null) {
            recordingThread.join(1500);
        }

        if (currentOutputFile == null || !currentOutputFile.exists()) {
            throw new IllegalStateException("No audio file was created.");
        }

        return currentOutputFile;
    }

    public boolean isRecording() {
        return recording;
    }
}