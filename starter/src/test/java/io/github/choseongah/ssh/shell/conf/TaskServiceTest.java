package io.github.choseongah.ssh.shell.conf;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class TaskServiceTest {

    @Scheduled(cron = "0 0 0 * * *")
    void test() {
        // test
    }
}
