package org.ccjmne.orca.api.rest;

import static org.ccjmne.orca.jooq.classes.Tables.USERS;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.ccjmne.orca.api.demo.DemoBareWorkingState;
import org.ccjmne.orca.api.utils.Constants;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

@Path("init")
@Singleton
public class InitEndpoint {

	private static final String SECRET = System.getProperty("init.secret");

	private final DSLContext ctx;

	@Inject
	public InitEndpoint(final DSLContext ctx) {
		this.ctx = ctx;
	}

	@POST
	public void init(final String secret) {
		if ((SECRET == null) || SECRET.isEmpty() || !SECRET.equals(secret)) {
			throw new IllegalStateException("The instance is not set up for (re)initilisation or your password is invalid.");
		}

		if (null == this.ctx.selectFrom(USERS).where(USERS.USER_ID.eq(Constants.USER_ROOT)).fetchOne()) {
			throw new IllegalStateException("The database has already been initialised.");
		}

		this.ctx.transaction(config -> {
			try (final DSLContext transactionCtx = DSL.using(config)) {
				DemoBareWorkingState.restore(transactionCtx);
			}
		});
	}
}
