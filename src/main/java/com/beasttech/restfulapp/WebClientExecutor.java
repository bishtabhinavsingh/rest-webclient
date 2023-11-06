//package com.beasttech.restfulapp;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//public class WebClientExecutor {
//    @Bean
//    public ThreadPoolTaskExecutor webClientExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(5);
//        executor.setMaxPoolSize(10);
//        executor.setQueueCapacity(100);
//        executor.setThreadNamePrefix("slow-");
//        executor.initialize();
//        return executor;
//    }
//
//}
