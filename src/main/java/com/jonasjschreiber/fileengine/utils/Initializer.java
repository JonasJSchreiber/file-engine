package com.jonasjschreiber.fileengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Configuration
@RequiredArgsConstructor
public class Initializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
    }
}
