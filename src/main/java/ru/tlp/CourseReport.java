package ru.tlp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

final class CourseReport {
    private CourseReport() {}

    static Path locate() throws IOException {
        Path workingDirectory = Path.of("report.pdf").toAbsolutePath();
        if (Files.isRegularFile(workingDirectory)) {
            return workingDirectory;
        }
        try {
            Path application = Path.of(CourseReport.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path directory = Files.isDirectory(application) ? application : application.getParent();
            Path report = directory.resolve("report.pdf");
            if (Files.isRegularFile(report)) {
                return report;
            }
        } catch (URISyntaxException exception) {
            throw new IOException("Cannot locate report.pdf", exception);
        }
        throw new IOException("report.pdf: " + workingDirectory);
    }
}
