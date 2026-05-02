package ru.blps.lab_1.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.quartz.autoconfigure.QuartzDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.blps.lab_1.jobs.DailyReportJob;
import ru.blps.lab_1.jobs.HourlyActiveOrdersJob;
import ru.blps.lab_1.jobs.StuckOrdersJob;

import javax.sql.DataSource;

@Configuration
public class QuartzConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String jdbcUser;

    @Value("${spring.datasource.password}")
    private String jdbcPassword;

    @Bean
    @QuartzDataSource
    public DataSource quartzDataSource() {
        return DataSourceBuilder.create()
            .url(jdbcUrl)
            .username(jdbcUser)
            .password(jdbcPassword)
            .driverClassName("org.postgresql.Driver")
            .build();
    }

    @Value("${eis.reports.daily-cron:0 55 23 * * ?}")
    private String dailyCron;

    @Value("${eis.reports.hourly-cron:0 0 * * * ?}")
    private String hourlyCron;

    @Value("${eis.reports.stuck-cron:0 0/15 * * * ?}")
    private String stuckCron;

    @Bean
    public JobDetail dailyJobDetail() {
        return JobBuilder.newJob(DailyReportJob.class)
            .withIdentity("dailyReportJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger dailyJobTrigger(JobDetail dailyJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(dailyJobDetail)
            .withIdentity("dailyReportTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule(dailyCron))
            .build();
    }

    @Bean
    public JobDetail hourlyJobDetail() {
        return JobBuilder.newJob(HourlyActiveOrdersJob.class)
            .withIdentity("hourlyActiveOrdersJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger hourlyJobTrigger(JobDetail hourlyJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(hourlyJobDetail)
            .withIdentity("hourlyActiveOrdersTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule(hourlyCron))
            .build();
    }

    @Bean
    public JobDetail stuckJobDetail() {
        return JobBuilder.newJob(StuckOrdersJob.class)
            .withIdentity("stuckOrdersJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger stuckJobTrigger(JobDetail stuckJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(stuckJobDetail)
            .withIdentity("stuckOrdersTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule(stuckCron))
            .build();
    }
}
