package org.ccjmne.faomaintenance.api.demo;

import javax.inject.Inject;

import org.ccjmne.faomaintenance.api.modules.StatisticsCaches;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class DemoDataManager {

	// Defaults to every SUNDAY at 3:00 AM
	private static final String SCHEDULE_CRON_EXPRESSION = System.getProperty("demo-cronwipe", "0 0 3 ? * SUN");

	private final Scheduler scheduler;
	private final DSLContext ctx;
	private final StatisticsCaches stats;

	@Inject
	public DemoDataManager(final DSLContext ctx, final StatisticsCaches stats) throws SchedulerException {
		final DirectSchedulerFactory schedulerFactory = DirectSchedulerFactory.getInstance();
		schedulerFactory.createVolatileScheduler(1);
		this.scheduler = schedulerFactory.getScheduler();
		this.ctx = ctx;
		this.stats = stats;
	}

	public void start() throws SchedulerException {
		final JobKey jobKey = new JobKey("reset");
		// Schedules reset job in accordance with the CRON expression
		this.scheduler.scheduleJob(
									JobBuilder
											.newJob(DemoDataReset.class)
											.usingJobData(new JobDataMap(ImmutableMap
													.of(DSLContext.class.getName(), this.ctx, StatisticsCaches.class.getName(), this.stats)))
											.withIdentity(jobKey)
											.build(),
									TriggerBuilder
											.newTrigger()
											.withSchedule(CronScheduleBuilder.cronSchedule(SCHEDULE_CRON_EXPRESSION))
											.build());
		this.scheduler.triggerJob(jobKey);
		this.scheduler.start();
	}

	public void shutdown() throws SchedulerException {
		if (this.scheduler.isStarted()) {
			this.scheduler.shutdown(true);
		}
	}

	public static class DemoDataReset implements Job {

		private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataReset.class);

		@Override
		public void execute(final JobExecutionContext context) throws JobExecutionException {
			try (final DSLContext ctx = (DSLContext) context.getMergedJobDataMap().get(DSLContext.class.getName())) {
				LOGGER.info("Restoring demo data...");
				ctx.transaction(config -> {
					try (final DSLContext transactionCtx = DSL.using(config)) {
						DemoBareWorkingState.restore(transactionCtx);
						DemoCommonResources.generate(transactionCtx);
						DemoDataSitesEmployees.generate(transactionCtx);
						DemoDataTrainings.generate(transactionCtx);
						DemoDataUsers.generate(transactionCtx);
					} catch (final Exception e) {
						LOGGER.error("An error occured during demo data restoration.", e);
					}
				});

				// Clear statistics caches
				((StatisticsCaches) context.getMergedJobDataMap().get(StatisticsCaches.class.getName())).invalidateEmployeesStats();
				LOGGER.info("Demo data restoration successfully completed.");
			} catch (final Exception e) {
				LOGGER.error("An error occured during demo data restoration.", e);
			}
		}
	}
}
