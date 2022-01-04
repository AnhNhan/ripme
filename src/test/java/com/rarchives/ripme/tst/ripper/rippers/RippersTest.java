package com.rarchives.ripme.tst.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractRipper;
import com.rarchives.ripme.ripper.rippers.ChanRipper;
import com.rarchives.ripme.utils.Utils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Contains helper methods for testing rippers.
 */
public class RippersTest {

    private final Logger logger = LogManager.getLogger(RippersTest.class);

    void testRipper(AbstractRipper ripper) {
        try {
            // Turn on Debug logging
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
            loggerConfig.setLevel(Level.DEBUG);
            ctx.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig.

            // Decrease timeout
            Utils.setConfigInteger("page.timeout", 20 * 1000);

            ripper.setup();
            ripper.markAsTest();
            ripper.rip();
            if (logger.isTraceEnabled()) {
                logger.trace("working dir: " + ripper.getWorkingDir());
                File wd = ripper.getWorkingDir().toFile();
                logger.trace("list files: " + wd.listFiles().length);
                for (int i = 0; i < wd.listFiles().length; i++) {
                    logger.trace("   " + wd.listFiles()[i]);
                }
            }
            Assertions.assertFalse(isEmpty(ripper.getWorkingDir()),
                    "Failed to download a single file from " + ripper.getURL());
        } catch (IOException e) {
            if (e.getMessage().contains("Ripping interrupted")) {
                // We expect some rips to get interrupted
            } else {
                e.printStackTrace();
                Assertions.fail("Failed to rip " + ripper.getURL() + " : " + e.getMessage());
            }
        } catch (Exception e) {
             e.printStackTrace();
            Assertions.fail("Failed to rip " + ripper.getURL() + " : " + e.getMessage());
        } finally {
            deleteDir(ripper.getWorkingDir());
        }
    }

    // We have a special test for chan rippers because we can't assume that content
    // will be downloadable, as content
    // is often removed within mere hours of it being posted. So instead of trying
    // to download any content we just check
    // that we found links to it
    void testChanRipper(ChanRipper ripper) {
        try {
            // Decrease timeout
            Utils.setConfigInteger("page.timeout", 20 * 1000);

            ripper.setup();
            ripper.markAsTest();
            List<String> foundUrls = ripper.getURLsFromPage(ripper.getFirstPage());
            Assertions.assertTrue(foundUrls.size() >= 1, "Failed to find single url on page " + ripper.getURL());
        } catch (IOException e) {
            if (e.getMessage().contains("Ripping interrupted")) {
                // We expect some rips to get interrupted
            } else {
                e.printStackTrace();
                Assertions.fail("Failed to rip " + ripper.getURL() + " : " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Failed to rip " + ripper.getURL() + " : " + e.getMessage());
        } finally {
            deleteDir(ripper.getWorkingDir());
        }
    }

    /** File extensions that are safe to delete. */
    private static final String[] SAFE_EXTENSIONS = { "png", "jpg", "jpeg", "gif", "mp4", "webm", "mov", "mpg", "mpeg",
            "txt", "log", "php" };

    /** Recursively deletes a directory */
    void deleteDir(Path dir) {
        if (!dir.getFileName().toString().contains("_")) {
            // All ripped albums contain an underscore
            // Don't delete an album if it doesn't have an underscore
            return;
        }
        try {
            for (File f : dir.toFile().listFiles()) {
                boolean safe = false;
                for (String ext : SAFE_EXTENSIONS) {
                    safe |= f.getAbsolutePath().toLowerCase().endsWith("." + ext);
                }
                if (!safe) {
                    // Found a file we shouldn't delete! Stop deleting immediately.
                    return;
                }
                if (f.isDirectory()) {
                    deleteDir(f.toPath());
                }
                f.delete();
            }
            Files.delete(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void deleteSubdirs(Path workingDir) {
        File wd = workingDir.toFile();
        for (File f : wd.listFiles()) {
            if (f.isDirectory()) {
                for (File sf : f.listFiles()) {
                    logger.debug("Deleting " + sf);
                    sf.delete();
                }
                logger.debug("Deleting " + f);
                f.delete();
            }
        }
    }

    private boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return !entries.findFirst().isPresent();
            }
        }
        return false;
    }
}
