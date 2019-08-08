/*
 * Copyright (C) 2019 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2019 Claus Schätzle (schaetzc@tf.uni-freiburg.de)
 * Copyright (C) 2019 University of Freiburg
 *
 * This file is part of the ULTIMATE SymbolicInterpretation plug-in.
 *
 * The ULTIMATE SymbolicInterpretation plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE SymbolicInterpretation plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE SymbolicInterpretation plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE SymbolicInterpretation plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE SymbolicInterpretation plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.symbolicinterpretation;

import java.util.Collections;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.core.lib.observers.BaseObserver;
import de.uni_freiburg.informatik.ultimate.core.lib.results.AllSpecificationsHoldResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.PositiveResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.UnprovableResult;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResult;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.symbolicinterpretation.IcfgInterpreter;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IcfgProgramExecution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfg;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;

/**
 * Starts symbolic interpretation on an icfg.
 *
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * @author Claus Schätzle (schaetzc@tf.uni-freiburg.de)
 */
public class SymbolicInterpretationObserver extends BaseObserver {

	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;

	public SymbolicInterpretationObserver(final ILogger logger, final IUltimateServiceProvider services) {
		mLogger = logger;
		mServices = services;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean process(final IElement root) throws Exception {
		if (root instanceof IIcfg<?>) {
			processIcfg((IIcfg<IcfgLocation>) root);
			return false;
		}
		return true;
	}

	private void processIcfg(final IIcfg<IcfgLocation> icfg) {
		final IcfgInterpreter icfgInterpreter = new SifaBuilder(mServices, mLogger).construct(icfg);
		final Map<IcfgLocation, IPredicate> predicates = icfgInterpreter.interpret();
		mLogger.debug("Final results are " + predicates);
		reportResult(predicates);
	}

	private void reportResult(final Map<IcfgLocation, IPredicate> predicates) {
		if (allPredicatesAreFalse(predicates)) {
			final String allUnreach = "All locations of interest are guaranteed to be unreachable.";
			mLogger.info(allUnreach);
			mServices.getResultService().reportResult(Activator.PLUGIN_ID, new AllSpecificationsHoldResult(
					Activator.PLUGIN_ID, allUnreach));
		} else {
			mLogger.warn("Some locations of interest might be reachable, see reported results.");
			predicates.entrySet().forEach(this::reportSingleResult);
		}
	}

	private void reportSingleResult(final Map.Entry<IcfgLocation, IPredicate> loiPred) {
		final IResult result;
		if (isFalse(loiPred.getValue())) {
			result = new PositiveResult<>(Activator.PLUGIN_ID, loiPred.getKey(), mServices.getBacktranslationService());
		} else {
			result = new UnprovableResult<>(Activator.PLUGIN_ID, loiPred.getKey(),
					mServices.getBacktranslationService(),
					IcfgProgramExecution.create(Collections.emptyList(), Collections.emptyMap()),
					"Interpreter reached this location of interest. Location might be reachable.");
		}
		mServices.getResultService().reportResult(Activator.PLUGIN_ID, result);
	}

	private static boolean allPredicatesAreFalse(final Map<IcfgLocation, IPredicate> predicates) {
		return predicates.values().stream().allMatch(SymbolicInterpretationObserver::isFalse);
	}

	private static boolean isFalse(final IPredicate predicate) {
		// TODO use isBottom(alpha(predicate)) from Domain as a fail fast test
		// … or even as the only test (in that case adapt the log messages to point out inaccuracy)
		return SmtUtils.isFalse(predicate.getFormula());
	}
}
