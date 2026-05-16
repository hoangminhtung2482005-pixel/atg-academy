package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MySqlEsportsDatabaseBackupService implements EsportsDatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(MySqlEsportsDatabaseBackupService.class);
    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final List<String> MYSQL_DUMP_CANDIDATES = List.of(
            "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
            "C:\\Program Files\\MySQL\\MySQL Workbench 8.0\\mysqldump.exe",
            "C:\\xampp\\mysql\\bin\\mysqldump.exe",
            "/usr/bin/mysqldump",
            "/usr/local/bin/mysqldump",
            "mysqldump"
    );

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${app.admin.esports-reset.mysqldump-path:}")
    private String configuredMySqlDumpPath;

    @Override
    public String backupBeforeResetEsportsData() {
        MySqlConnectionInfo connectionInfo = parseConnectionInfo(datasourceUrl);
        Path backupDirectory = Path.of("sql", "backups");
        String fileName = connectionInfo.databaseName() + "_before_reset_esports_data_"
                + LocalDateTime.now().format(BACKUP_TIMESTAMP) + ".sql";
        Path absoluteBackupPath = backupDirectory.toAbsolutePath().normalize().resolve(fileName);

        try {
            Files.createDirectories(absoluteBackupPath.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể backup DB, reset bị hủy. Không tạo được thư mục backup.", exception);
        }

        String executable = resolveMySqlDumpExecutable();
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("--host=" + connectionInfo.host());
        command.add("--port=" + connectionInfo.port());
        command.add("--user=" + datasourceUsername);
        command.add("--default-character-set=utf8mb4");
        command.add("--single-transaction");
        command.add("--quick");
        command.add("--set-gtid-purged=OFF");
        command.add("--routines");
        command.add("--events");
        command.add("--triggers");
        command.add("--databases");
        command.add(connectionInfo.databaseName());
        command.add("--result-file=" + absoluteBackupPath);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        if (StringUtils.hasText(datasourcePassword)) {
            processBuilder.environment().put("MYSQL_PWD", datasourcePassword);
        }

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Không thể backup DB, reset bị hủy. Lệnh mysqldump bị timeout.");
            }
            if (process.exitValue() != 0) {
                deleteIfExists(absoluteBackupPath);
                throw new IllegalStateException(buildBackupFailureMessage(output));
            }
            if (!Files.isRegularFile(absoluteBackupPath) || Files.size(absoluteBackupPath) <= 0) {
                deleteIfExists(absoluteBackupPath);
                throw new IllegalStateException("Không thể backup DB, reset bị hủy. File backup không hợp lệ hoặc đang rỗng.");
            }
        } catch (IOException exception) {
            deleteIfExists(absoluteBackupPath);
            throw new IllegalStateException("Không thể backup DB, reset bị hủy. Không mở được mysqldump.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            deleteIfExists(absoluteBackupPath);
            throw new IllegalStateException("Không thể backup DB, reset bị hủy. Backup bị gián đoạn.", exception);
        }

        String relativeBackupPath = backupDirectory.resolve(fileName).toString().replace('\\', '/');
        log.info(">> [Admin] Da tao backup DB truoc reset esports data tai {}", absoluteBackupPath);
        return relativeBackupPath;
    }

    private String resolveMySqlDumpExecutable() {
        if (StringUtils.hasText(configuredMySqlDumpPath)) {
            Path configuredPath = Path.of(configuredMySqlDumpPath.trim());
            if (Files.isRegularFile(configuredPath)) {
                return configuredPath.toString();
            }
        }

        for (String candidate : MYSQL_DUMP_CANDIDATES) {
            if (!candidate.contains("\\") && !candidate.contains("/")) {
                return candidate;
            }
            Path candidatePath = Path.of(candidate);
            if (Files.isRegularFile(candidatePath)) {
                return candidatePath.toString();
            }
        }

        throw new IllegalStateException("Không thể backup DB, reset bị hủy. Không tìm thấy mysqldump.");
    }

    private MySqlConnectionInfo parseConnectionInfo(String jdbcUrl) {
        if (!StringUtils.hasText(jdbcUrl) || !jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException("Không thể backup DB, reset bị hủy. JDBC URL hiện tại không phải MySQL hợp lệ.");
        }

        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 3306;
        String databaseName = uri.getPath();
        if (databaseName != null && databaseName.startsWith("/")) {
            databaseName = databaseName.substring(1);
        }

        if (!StringUtils.hasText(host) || !StringUtils.hasText(databaseName)) {
            throw new IllegalStateException("Không thể backup DB, reset bị hủy. Không parse được host/database từ JDBC URL.");
        }

        return new MySqlConnectionInfo(host, port, databaseName);
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup for a failed backup file.
        }
    }

    private String buildBackupFailureMessage(String output) {
        if (StringUtils.hasText(output)) {
            return "Không thể backup DB, reset bị hủy. Mysqldump lỗi: " + output;
        }
        return "Không thể backup DB, reset bị hủy.";
    }

    private record MySqlConnectionInfo(String host, int port, String databaseName) {
    }
}
