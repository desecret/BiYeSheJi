package org.example.server.util;

import java.io.IOException;
import java.nio.file.*;

public class FileWatcher {
    private final Path path;
    private WatchService watchService;

    public FileWatcher(String filePath) {
        this.path = Paths.get(filePath);
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addListener(FileChangeListener listener) {
        new Thread(() -> {
            try {
                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.context().toString().equals(path.getFileName().toString())) {
                            listener.onChange();
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }



    public interface FileChangeListener {
        void onChange();
    }
}
