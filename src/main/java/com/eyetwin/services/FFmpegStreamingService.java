package com.eyetwin.services;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FFmpegStreamingService {

    public enum OS { WINDOWS, LINUX, MAC }

    private static final String RTMP_BASE    = "rtmp://localhost:1935/live/";
    private static final int    STOP_TIMEOUT = 5;

    private static final String[] WINDOWS_FFMPEG_PATHS = {
            "ffmpeg",
            "C:\\ffmpeg-8.1-essentials_build\\bin\\ffmpeg.exe",
            "C:\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\Program Files (x86)\\ffmpeg\\bin\\ffmpeg.exe",
            System.getenv("USERPROFILE") != null
                    ? System.getenv("USERPROFILE") + "\\ffmpeg\\bin\\ffmpeg.exe" : null,
    };

    private static String resolvedFfmpegPath = null;

    private Process         ffmpegProcess;
    private ExecutorService logExecutor;

    private Consumer<String> onLog;
    private Consumer<String> onError;
    private Runnable         onStarted;
    private Runnable         onStopped;

    // ── Configuration ─────────────────────────────────────────────────────────
    private int     frameRate     = 30;
    private String  resolution    = "1920x1080";
    private String  videoBitrate  = "2500k";
    private String  preset        = "ultrafast";
    private boolean captureAudio  = true;
    private boolean captureScreen = true;
    private boolean captureWebcam = false;
    private String  captureFile   = null;

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setOnLog(Consumer<String> v)        { this.onLog         = v; }
    public void setOnError(Consumer<String> v)      { this.onError       = v; }
    public void setOnStarted(Runnable v)            { this.onStarted     = v; }
    public void setOnStopped(Runnable v)            { this.onStopped     = v; }
    public void setFrameRate(int v)                 { this.frameRate     = v; }
    public void setResolution(String v)             { this.resolution    = v; }
    public void setVideoBitrate(String v)           { this.videoBitrate  = v; }
    public void setPreset(String v)                 { this.preset        = v; }
    public void setCaptureAudio(boolean v)          { this.captureAudio  = v; }
    public void setCaptureScreen(boolean v)         { this.captureScreen = v; }
    public void setCaptureWebcam(boolean v)         { this.captureWebcam = v; }
    public void setCaptureFile(String v)            { this.captureFile   = v; }

    // ── Static utilities ──────────────────────────────────────────────────────

    public static String resolveFfmpegPath() {
        if (resolvedFfmpegPath != null) return resolvedFfmpegPath;

        if (detectOSStatic() == OS.WINDOWS) {
            for (String candidate : WINDOWS_FFMPEG_PATHS) {
                if (tryExec(candidate)) return resolvedFfmpegPath;
            }

            String pathEnv = System.getenv("PATH");
            if (pathEnv != null) {
                for (String dir : pathEnv.split(";")) {
                    File f = new File(dir.trim(), "ffmpeg.exe");
                    if (f.exists() && f.canExecute() && tryExec(f.getAbsolutePath())) {
                        resolvedFfmpegPath = f.getAbsolutePath();
                        return resolvedFfmpegPath;
                    }
                }
            }

            try {
                Process whereProc = new ProcessBuilder("where", "ffmpeg")
                        .redirectErrorStream(true).start();
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(whereProc.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (line.toLowerCase().endsWith("ffmpeg.exe") && new File(line).exists()) {
                            if (tryExec(line)) return resolvedFfmpegPath;
                        }
                    }
                }
                whereProc.waitFor(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {}

            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                File wingetPkgs = new File(localAppData, "Microsoft\\WinGet\\Packages");
                String found = walkForFfmpeg(wingetPkgs);
                if (found != null) { resolvedFfmpegPath = found; return resolvedFfmpegPath; }
            }

        } else {
            if (tryExec("ffmpeg")) { resolvedFfmpegPath = "ffmpeg"; return resolvedFfmpegPath; }
        }
        return null;
    }

    private static boolean tryExec(String candidate) {
        if (candidate == null || candidate.contains("null")) return false;
        try {
            Process p = new ProcessBuilder(candidate, "-version")
                    .redirectErrorStream(true).start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            boolean ok = p.waitFor(10, TimeUnit.SECONDS);
            if (ok && p.exitValue() == 0) { resolvedFfmpegPath = candidate; return true; }
        } catch (Exception ignored) {}
        return false;
    }

    private static String walkForFfmpeg(File dir) { return walkForFfmpeg(dir, 0); }
    private static String walkForFfmpeg(File dir, int depth) {
        if (dir == null || !dir.isDirectory() || depth > 5) return null;
        File[] children = dir.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (child.isFile() && child.getName().equalsIgnoreCase("ffmpeg.exe")) {
                if (tryExec(child.getAbsolutePath())) return child.getAbsolutePath();
            } else if (child.isDirectory()) {
                String found = walkForFfmpeg(child, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static boolean isFFmpegAvailable() { return resolveFfmpegPath() != null; }

    public static String detectWindowsAudioDevice() {
        String ffmpeg = resolveFfmpegPath();
        if (ffmpeg == null) return null;
        try {
            Process p = new ProcessBuilder(
                    ffmpeg, "-list_devices", "true", "-f", "dshow", "-i", "dummy")
                    .redirectErrorStream(true).start();
            List<String> audioDevices = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    // ✅ FFmpeg 8.x format: [in#0 @ ...] "Device Name" (audio)
                    if (line.contains("(audio)") && line.contains("\"")) {
                        int s = line.indexOf('"');
                        int e = line.lastIndexOf('"');
                        if (s < e) audioDevices.add(line.substring(s + 1, e));
                    }
                }
            }
            p.waitFor(5, TimeUnit.SECONDS);
            System.out.println("[FFmpeg] Audio devices found: " + audioDevices);
            // Priorité aux devices virtuels
            for (String d : audioDevices) {
                String l = d.toLowerCase();
                if (l.contains("virtual") || l.contains("vb-audio")
                        || l.contains("voicemeeter") || l.contains("cable")) return d;
            }
            return audioDevices.isEmpty() ? null : audioDevices.get(0);
        } catch (Exception e) { return null; }
    }
    public static String detectWindowsVideoDevice() {
        String ffmpeg = resolveFfmpegPath();
        if (ffmpeg == null) return null;
        try {
            Process p = new ProcessBuilder(
                    ffmpeg, "-list_devices", "true", "-f", "dshow", "-i", "dummy")
                    .redirectErrorStream(true).start();
            List<String> videoDevices = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    // ✅ FFmpeg 8.x format: [in#0 @ ...] "Device Name" (video)
                    if (line.contains("(video)") && line.contains("\"")) {
                        int s = line.indexOf('"');
                        int e = line.lastIndexOf('"');
                        if (s < e) videoDevices.add(line.substring(s + 1, e));
                    }
                }
            }
            p.waitFor(5, TimeUnit.SECONDS);
            System.out.println("[FFmpeg] Video devices found: " + videoDevices);
            return videoDevices.isEmpty() ? null : videoDevices.get(0);
        } catch (Exception e) { return null; }
    }
    // ── State ─────────────────────────────────────────────────────────────────

    public boolean isRunning() {
        return ffmpegProcess != null && ffmpegProcess.isAlive();
    }

    // ── Start ─────────────────────────────────────────────────────────────────

    public void start(String streamKey) {
        if (isRunning()) { log("FFmpeg already running."); return; }

        new Thread(() -> {
            String ffmpegExe = resolveFfmpegPath();
            if (ffmpegExe == null) {
                error("FFmpeg not found. Add FFmpeg to PATH and restart the app.");
                return;
            }
            log("FFmpeg: " + ffmpegExe);

            String audioDevice = null;
            String videoDevice = null;

            if (captureWebcam && detectOSStatic() == OS.WINDOWS) {
                log("Detecting webcam…");
                videoDevice = detectWindowsVideoDevice();
                log(videoDevice != null ? "✔ Webcam: " + videoDevice : "⚠ No webcam found.");
                if (captureAudio) {
                    audioDevice = detectWindowsAudioDevice();
                    log(audioDevice != null ? "✔ Audio: " + audioDevice : "⚠ No audio device.");
                }
            } else if (captureScreen && captureAudio && detectOSStatic() == OS.WINDOWS) {
                log("Detecting audio devices…");
                audioDevice = detectWindowsAudioDevice();
                log(audioDevice != null ? "✔ Audio: " + audioDevice
                        : "⚠ No audio device — streaming without audio.");
            }

            List<String> cmd = buildCommand(ffmpegExe, streamKey, audioDevice, videoDevice);
            log("Launching…");

            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                ffmpegProcess = pb.start();

                logExecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "ffmpeg-log");
                    t.setDaemon(true);
                    return t;
                });

                logExecutor.submit(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(ffmpegProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String l = line;
                            Platform.runLater(() -> log(l));
                        }
                    } catch (Exception ignored) {}
                    Platform.runLater(() -> {
                        log("FFmpeg process ended.");
                        if (onStopped != null) onStopped.run();
                    });
                });

                Thread.sleep(1500);
                if (isRunning()) Platform.runLater(() -> {
                    log("Stream is LIVE ✔");
                    if (onStarted != null) onStarted.run();
                });

            } catch (Exception e) {
                error("Failed to launch FFmpeg: " + e.getMessage());
            }
        }, "ffmpeg-launcher").start();
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    public void stop() {
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            try {
                ffmpegProcess.getOutputStream().write('q');
                ffmpegProcess.getOutputStream().flush();
                boolean exited = ffmpegProcess.waitFor(STOP_TIMEOUT, TimeUnit.SECONDS);
                if (!exited) { log("Force-killing FFmpeg."); ffmpegProcess.destroyForcibly(); }
            } catch (Exception e) { ffmpegProcess.destroyForcibly(); }
        }
        if (logExecutor != null) { logExecutor.shutdownNow(); logExecutor = null; }
        ffmpegProcess = null;
        log("Stream stopped.");
        if (onStopped != null) Platform.runLater(onStopped);
    }

    // ── Command builder ───────────────────────────────────────────────────────

    private List<String> buildCommand(String ffmpegExe, String streamKey,
                                      String audioDevice, String videoDevice) {
        OS os = detectOSStatic();
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegExe);
        cmd.add("-y");

        // ── Mode 1: Video file ────────────────────────────────────────────────
        if (captureFile != null) {
            cmd.addAll(List.of("-re", "-i", captureFile));
            cmd.addAll(List.of(
                    "-c:v", "libx264", "-preset", preset, "-tune", "zerolatency",
                    "-b:v", videoBitrate, "-maxrate", videoBitrate, "-bufsize", "5000k",
                    "-pix_fmt", "yuv420p",
                    "-g",            String.valueOf(frameRate * 2),
                    "-keyint_min",   String.valueOf(frameRate),
                    "-sc_threshold", "0",                             // ✅ FIX iOS
                    "-c:a", "aac", "-b:a", "128k", "-ar", "44100",
                    "-f", "flv", RTMP_BASE + streamKey));
            return cmd;
        }

        // ── Mode 2: Webcam ────────────────────────────────────────────────────
        if (captureWebcam) {
            if (os == OS.WINDOWS) {
                if (videoDevice != null) {
                    cmd.addAll(List.of("-f", "dshow", "-i",
                            audioDevice != null
                                    ? "video=" + videoDevice + ":audio=" + audioDevice
                                    : "video=" + videoDevice));
                }
            } else if (os == OS.LINUX) {
                cmd.addAll(List.of("-f", "v4l2", "-i", "/dev/video0"));
                if (captureAudio)
                    cmd.addAll(List.of("-f", "pulse", "-i", "default"));
            } else {
                cmd.addAll(List.of("-f", "avfoundation", "-i", "0:0"));
            }
            cmd.addAll(List.of(
                    "-c:v", "libx264", "-preset", preset, "-tune", "zerolatency",
                    "-b:v", videoBitrate, "-maxrate", videoBitrate, "-bufsize", "5000k",
                    "-pix_fmt", "yuv420p",
                    "-g",            String.valueOf(frameRate * 2),
                    "-keyint_min",   String.valueOf(frameRate),
                    "-sc_threshold", "0",                             // ✅ FIX iOS
                    "-c:a", "aac", "-b:a", "128k", "-ar", "44100",
                    "-f", "flv", RTMP_BASE + streamKey));
            return cmd;
        }

        // ── Mode 3: Screen capture (default) ─────────────────────────────────
        if (captureScreen) {
            switch (os) {
                case WINDOWS -> cmd.addAll(List.of(
                        "-f", "gdigrab", "-framerate", String.valueOf(frameRate),
                        "-video_size", resolution, "-i", "desktop"));
                case LINUX -> {
                    String display = System.getenv("DISPLAY");
                    if (display == null) display = ":0";
                    cmd.addAll(List.of(
                            "-f", "x11grab", "-framerate", String.valueOf(frameRate),
                            "-video_size", resolution, "-i", display + ".0"));
                }
                case MAC -> cmd.addAll(List.of(
                        "-f", "avfoundation", "-framerate", String.valueOf(frameRate),
                        "-i", "1:0"));
            }
        }

        boolean useAudio = captureAudio;
        if (useAudio) {
            switch (os) {
                case WINDOWS -> {
                    if (audioDevice != null)
                        cmd.addAll(List.of("-f", "dshow", "-i", "audio=" + audioDevice));
                    else useAudio = false;
                }
                case LINUX -> cmd.addAll(List.of("-f", "pulse", "-i", "default"));
                case MAC   -> { /* avfoundation already includes audio */ }
            }
        }

        cmd.addAll(List.of(
                "-c:v", "libx264", "-preset", preset, "-tune", "zerolatency",
                "-b:v", videoBitrate, "-maxrate", videoBitrate, "-bufsize", "5000k",
                "-pix_fmt", "yuv420p",
                "-g",            String.valueOf(frameRate * 2),
                "-keyint_min",   String.valueOf(frameRate),
                "-sc_threshold", "0"));                               // ✅ FIX iOS

        if (useAudio)
            cmd.addAll(List.of("-c:a", "aac", "-b:a", "128k", "-ar", "44100"));
        else
            cmd.add("-an");

        cmd.addAll(List.of("-f", "flv", RTMP_BASE + streamKey));
        return cmd;
    }

    // ── OS detection ──────────────────────────────────────────────────────────

    private static OS detectOSStatic() {
        String name = System.getProperty("os.name", "").toLowerCase();
        if (name.contains("win")) return OS.WINDOWS;
        if (name.contains("mac")) return OS.MAC;
        return OS.LINUX;
    }

    // ── Log helpers ───────────────────────────────────────────────────────────

    private void log(String msg) {
        if (onLog != null) onLog.accept(msg);
        else System.out.println("[FFmpeg] " + msg);
    }

    private void error(String msg) {
        if (onError != null) Platform.runLater(() -> onError.accept(msg));
        else System.err.println("[FFmpeg ERROR] " + msg);
    }
}