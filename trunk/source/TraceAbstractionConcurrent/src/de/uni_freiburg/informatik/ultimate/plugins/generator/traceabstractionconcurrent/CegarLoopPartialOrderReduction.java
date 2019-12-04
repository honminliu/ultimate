package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstractionconcurrent;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.reduction.IIndependenceRelation;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.CfgSmtToolkit;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfg;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.debugidentifiers.DebugIdentifier;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.PredicateFactory;
import de.uni_freiburg.informatik.ultimate.lib.tracecheckerutils.singletracecheck.InterpolationTechnique;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.BasicCegarLoop;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstractionconcurrent.reduction.DualPartialOrderInclusionCheck;

public class CegarLoopPartialOrderReduction<LETTER extends IIcfgTransition<?>> extends BasicCegarLoop<LETTER> {

	private final IIndependenceRelation<IPredicate, LETTER> mRelation1;
	private final IIndependenceRelation<IPredicate, LETTER> mRelation2;

	public CegarLoopPartialOrderReduction(final DebugIdentifier name, final IIcfg<?> rootNode,
			final CfgSmtToolkit csToolkit, final PredicateFactory predicateFactory, final TAPreferences taPrefs,
			final Collection<? extends IcfgLocation> errorLocs, final IUltimateServiceProvider services) {
		super(name, rootNode, csToolkit, predicateFactory, taPrefs, errorLocs,
				InterpolationTechnique.Craig_TreeInterpolation, false, services);

		mRelation1 = null; // TODO
		mRelation2 = null; // TODO
	}

	@Override
	protected boolean isAbstractionEmpty() throws AutomataOperationCanceledException {
		final DualPartialOrderInclusionCheck<IPredicate, IPredicate, LETTER> check = new DualPartialOrderInclusionCheck<>(
				mRelation1, mRelation2, (INestedWordAutomaton<LETTER, IPredicate>) mAbstraction, mInterpolAutomaton,
				true);
		if (!check.getResult()) {
			mCounterexample = check.getCounterexample();
		}
		return check.getResult();
	}
}
