/*
 * Copyright (C) 2019 Luca Bruder (luca.bruder@gmx.de)
 *
 * This file is part of the ULTIMATE Library-ModelCheckerUtils library.
 *
 * The ULTIMATE Library-ModelCheckerUtils library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * The ULTIMATE Library-ModelCheckerUtils library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Library-ModelCheckerUtils library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Library-ModelCheckerUtils library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Library-ModelCheckerUtils library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.icfgtransformer.mapelim.monniaux;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.models.ModelUtils;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.IBacktranslationTracker;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.IIcfgTransformer;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.ILocationFactory;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.TransformedIcfgBuilder;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.loopacceleration.IdentityTransformer;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.BasicIcfg;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IIcfgSymbolTable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfg;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgInternalTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgEdgeIterator;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormulaBuilder;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.ILocalProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramConst;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramNonOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.ProgramVarUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SubstitutionWithLocalSimplification;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;

/**
 * @author Luca Bruder (luca.bruder@gmx.de)
 * @author Lisa Kleinlein (lisa.kleinlein@web.de)
 */
public class MonniauxMapEliminator implements IIcfgTransformer<IcfgLocation> {

	private final ManagedScript mMgdScript;
	private final IIcfg<IcfgLocation> mIcfg;
	private final IIcfg<IcfgLocation> mResultIcfg;
	private final ILogger mLogger;
	private final IBacktranslationTracker mBacktranslationTracker;
	private final int mCells;

	public MonniauxMapEliminator(final ILogger logger, final IIcfg<IcfgLocation> icfg,
			final IBacktranslationTracker backtranslationTracker, final int cells) {
		mIcfg = Objects.requireNonNull(icfg);
		mMgdScript = Objects.requireNonNull(mIcfg.getCfgSmtToolkit().getManagedScript());
		mLogger = logger;
		mBacktranslationTracker = backtranslationTracker;
		mCells = cells;
		mResultIcfg = eliminateMaps();
	}

	@Override
	public IIcfg<IcfgLocation> getResult() {
		return mResultIcfg;
	}

	private IIcfg<IcfgLocation> eliminateMaps() {

		final BasicIcfg<IcfgLocation> resultIcfg =
				new BasicIcfg<>(mIcfg.getIdentifier() + "ME", mIcfg.getCfgSmtToolkit(), IcfgLocation.class);
		final ILocationFactory<IcfgLocation, IcfgLocation> funLocFac = (oldLocation, debugIdentifier, procedure) -> {
			final IcfgLocation rtr = new IcfgLocation(debugIdentifier, procedure);
			ModelUtils.copyAnnotations(oldLocation, rtr);
			return rtr;
		};

		final TransformedIcfgBuilder<IcfgLocation, IcfgLocation> lst = new TransformedIcfgBuilder<>(mLogger, funLocFac,
				mBacktranslationTracker, new IdentityTransformer(mIcfg.getCfgSmtToolkit()), mIcfg, resultIcfg);
		mMgdScript.lock(this);
		iterate(lst);
		lst.finish();
		mMgdScript.unlock(this);
		return resultIcfg;
	}

	private void iterate(final TransformedIcfgBuilder<IcfgLocation, IcfgLocation> lst) {

		final Script script = mMgdScript.getScript();

		// Create mappings from original ProgramVars to a set of mCells new ProgramVars
		final IIcfgSymbolTable symboltable = mIcfg.getCfgSmtToolkit().getSymbolTable();
		final Set<IProgramNonOldVar> globals = symboltable.getGlobals();
		final Set<ILocalProgramVar> locals = new HashSet<>();
		for (final Entry<String, IcfgLocation> entry : mIcfg.getProcedureEntryNodes().entrySet()) {
			final String proc = entry.getKey();
			final Set<ILocalProgramVar> someLocals = symboltable.getLocals(proc);
			locals.addAll(someLocals);
		}

		final Set<IProgramVar> programArrayVars = new HashSet<>(globals.size() + locals.size());
		globals.stream().filter(a -> a.getSort().isArraySort()).forEach(programArrayVars::add);
		locals.stream().filter(a -> a.getSort().isArraySort()).forEach(programArrayVars::add);

		// Fill the sets idxD and valD with a certain number (mCells) of new program variables for each program
		// variable of the sort array
		final Map<IProgramVar, Set<IProgramVar>> idxVars = new LinkedHashMap<>();
		final Map<IProgramVar, Set<IProgramVar>> valVars = new LinkedHashMap<>();
		for (final IProgramVar var : programArrayVars) {
			assert var.getSort().isArraySort();
			assert var.getSort().getArguments().length == 2 : "Array sort with != 2 arguments";
			final Sort indexSort = var.getSort().getArguments()[0];
			final Sort valueSort = var.getSort().getArguments()[1];

			final Set<IProgramVar> idx = new LinkedHashSet<>();
			final Set<IProgramVar> val = new LinkedHashSet<>();
			for (int i = 0; i < mCells; i++) {
				final String idxName = (var.toString() + "_idx_" + Integer.toString(i));
				final String valName = (var.toString() + "_val_" + Integer.toString(i));

				final IProgramVar varIdx =
						ProgramVarUtils.constructGlobalProgramVarPair(idxName, indexSort, mMgdScript, this);
				final IProgramVar varVal =
						ProgramVarUtils.constructGlobalProgramVarPair(valName, valueSort, mMgdScript, this);
				idx.add(varIdx);
				val.add(varVal);
			}
			idxVars.put(var, idx);
			valVars.put(var, val);
		}

		final IcfgEdgeIterator iter = new IcfgEdgeIterator(mIcfg);
		while (iter.hasNext()) {
			final IIcfgTransition<?> transition = iter.next();

			// Iterate over relevant edges
			if (transition instanceof IIcfgInternalTransition) {

				final IIcfgInternalTransition<?> internalTransition = (IIcfgInternalTransition<?>) transition;
				final UnmodifiableTransFormula tf = internalTransition.getTransformula();

				final Term tfTerm = tf.getFormula();

				final StoreSelectEqualityCollector ssec = new StoreSelectEqualityCollector();
				ssec.transform(tfTerm);

				if (ssec.isEmpty()) {
					// TODO: Do we have to insert this edge somewhere?
					continue;
				}

				final Map<Term, Term> subst = new HashMap<>();

				// Create new in- and outVars, if necessary
				final Set<TermVariable> auxVars = tf.getAuxVars();
				for (final TermVariable aux : auxVars) {
					if (aux.getSort().isArraySort()) {
						throw new UnsupportedOperationException("Arrays in auxVariables");
					}
				}

				final Map<IProgramVar, TermVariable> newInVars = new HashMap<>(tf.getInVars());
				final Map<IProgramVar, TermVariable> newOutVars = new HashMap<>(tf.getOutVars());

				final Map<Term, Set<Term>> hierarchy = new LinkedHashMap<>();
				final Map<Term, Set<Term>> idxTerms = new LinkedHashMap<>();
				final Map<Term, IProgramVar> oldTermToProgramVar = new LinkedHashMap<>();
				for (final IProgramVar arrayVar : programArrayVars) {

					// InVars for values
					final TermVariable arrayInTermVar = newInVars.remove(arrayVar);
					if (arrayInTermVar != null) {
						final Set<Term> valTermVars = new LinkedHashSet<>();
						for (final IProgramVar valVar : valVars.get(arrayVar)) {
							final TermVariable valTermVar = mMgdScript
									.constructFreshTermVariable((valVar.toString() + "_in"), valVar.getSort());
							newInVars.put(valVar, valTermVar);
							valTermVars.add(valTermVar);
						}
						hierarchy.put(arrayInTermVar, valTermVars);
					}

					// OutVars for values
					final TermVariable arrayOutTermVar = newOutVars.remove(arrayVar);
					if (arrayOutTermVar != null) {
						final Set<Term> valTermVars = new LinkedHashSet<>();
						for (final IProgramVar valVar : valVars.get(arrayVar)) {
							final TermVariable valTermVar = mMgdScript
									.constructFreshTermVariable((valVar.toString() + "_in"), valVar.getSort());
							newOutVars.put(valVar, valTermVar);
							valTermVars.add(valTermVar);
							oldTermToProgramVar.put(valTermVar, valVar);
						}
						hierarchy.put(arrayOutTermVar, valTermVars);
					}

					// In- and OutVars for indices
					final Set<Term> idxTermVarSet = new LinkedHashSet<>();
					for (final IProgramVar idxVar : idxVars.get(arrayVar)) {
						final TermVariable idxTermVar =
								mMgdScript.constructFreshTermVariable((idxVar.toString() + "_term"), idxVar.getSort());
						newInVars.put(idxVar, idxTermVar);
						newOutVars.put(idxVar, idxTermVar);
						idxTermVarSet.add(idxTermVar);
					}
					if (arrayInTermVar != null) {
						idxTerms.put(arrayInTermVar, idxTermVarSet);
					}
					if (arrayOutTermVar != null) {
						idxTerms.put(arrayOutTermVar, idxTermVarSet);
					}
				}

				// Eliminate the Select-, Store-, and Equality-Terms
				for (final Term selectTerm : ssec.mSelectTerms) {
					final ApplicationTerm aSelectTerm = (ApplicationTerm) selectTerm;
					final Term substTerm = eliminateSelects(mMgdScript, idxTerms, aSelectTerm, hierarchy);
					subst.put(selectTerm, substTerm);
				}
				for (final Term storeTerm : ssec.mStoreTerms) {
					final ApplicationTerm aStoreTerm = (ApplicationTerm) storeTerm;
					final Term substTerm = eliminateStores(mMgdScript, idxTerms, aStoreTerm, hierarchy, newInVars,
							oldTermToProgramVar);
					subst.put(storeTerm, substTerm);
				}
				for (final Term equalityTerm : ssec.mEqualityTerms) {
					final ApplicationTerm aEqualityTerm = (ApplicationTerm) equalityTerm;
					final Term substTerm = eliminateEqualities(mMgdScript, idxTerms, aEqualityTerm, hierarchy);
					subst.put(equalityTerm, substTerm);
				}

				final Term newTfTerm = new SubstitutionWithLocalSimplification(mMgdScript, subst).transform(tfTerm);
				final UnmodifiableTransFormula newTf =
						buildTransitionFormula(tf, newTfTerm, newInVars, newOutVars, auxVars);

				final IcfgLocation oldSource = internalTransition.getSource();
				final IcfgLocation newSource = lst.createNewLocation(oldSource);
				final IcfgLocation oldTarget = internalTransition.getTarget();
				final IcfgLocation newTarget = lst.createNewLocation(oldTarget);
				lst.createNewInternalTransition(newSource, newTarget, newTf, true);

			} else {
				throw new UnsupportedOperationException("Not yet implemented");
			}
		}
	}

	private UnmodifiableTransFormula buildTransitionFormula(final UnmodifiableTransFormula oldFormula,
			final Term newTfFormula, final Map<IProgramVar, TermVariable> inVars,
			final Map<IProgramVar, TermVariable> outVars, final Collection<TermVariable> auxVars) {
		final Set<IProgramConst> nonTheoryConsts = oldFormula.getNonTheoryConsts();
		final boolean emptyAuxVars = auxVars.isEmpty();
		final Collection<TermVariable> branchEncoders = oldFormula.getBranchEncoders();
		final boolean emptyBranchEncoders = branchEncoders.isEmpty();
		final boolean emptyNonTheoryConsts = nonTheoryConsts.isEmpty();
		final TransFormulaBuilder tfb = new TransFormulaBuilder(inVars, outVars, emptyNonTheoryConsts, nonTheoryConsts,
				emptyBranchEncoders, branchEncoders, emptyAuxVars);

		tfb.setFormula(newTfFormula);
		tfb.setInfeasibility(Infeasibility.NOT_DETERMINED);
		auxVars.stream().forEach(tfb::addAuxVar);
		return tfb.finishConstruction(mMgdScript);
	}

	private Term eliminateSelects(final ManagedScript mMgdScript, final Map<Term, Set<Term>> idxTerms,
			final ApplicationTerm selectTerm, final Map<Term, Set<Term>> hierarchy) {
		final Term[] params = selectTerm.getParameters();
		final Term x = params[0];
		final Term y = params[1];
		final Script script = mMgdScript.getScript();
		// final int j = Integer.parseInt(x.toString().replaceAll("\\D", ""));

		final Sort sort = x.getSort().getArguments()[1];
		final Term placeholder = mMgdScript.constructFreshTermVariable((x.toString() + "_aux"), sort);
		Term substTerm = SmtUtils.and(script, placeholder);
		for (final Term val : hierarchy.get(x)) {
			for (final Term idx : idxTerms.get(x)) {
				substTerm = SmtUtils.and(script, substTerm, SmtUtils.implies(script,
						SmtUtils.binaryEquality(script, y, idx), SmtUtils.binaryEquality(script, val, placeholder)));
			}
		}

		return substTerm;
	}

	private Term eliminateStores(final ManagedScript mMgdScript, final Map<Term, Set<Term>> idxTerms,
			final ApplicationTerm storeTerm, final Map<Term, Set<Term>> hierarchy,
			final Map<IProgramVar, TermVariable> newInVars, final Map<Term, IProgramVar> oldTermToProgramVar) {
		final Term[] params = storeTerm.getParameters();
		final Term x = params[0];
		final Term y = params[1];
		final Term z = params[2];
		final Script script = mMgdScript.getScript();

		final Set<Term> rtr = new LinkedHashSet<>();
		for (final Term val : hierarchy.get(x)) {
			final Term valLow;
			if (newInVars.containsValue(val)) {
				valLow = val;
			} else {
				valLow = newInVars.get(oldTermToProgramVar.get(x));
			}

			for (final Term idx : idxTerms.get(x)) {
				rtr.add(SmtUtils.implies(script, SmtUtils.binaryEquality(script, y, idx),
						SmtUtils.binaryEquality(script, val, z)));
				rtr.add(SmtUtils.implies(script, SmtUtils.distinct(script, idx, y),
						SmtUtils.binaryEquality(script, val, valLow)));
			}
		}
		return SmtUtils.and(script, rtr);
	}

	private Term eliminateEqualities(final ManagedScript mMgdScript, final Map<Term, Set<Term>> idxTerms,
			final ApplicationTerm equalityTerm, final Map<Term, Set<Term>> hierarchy) {
		final Term[] params = equalityTerm.getParameters();
		final Term x = params[0];
		final Term y = params[1];
		final Script script = mMgdScript.getScript();
		// TBD: Actually eliminate the array

		final Set<Term> rtr = new LinkedHashSet<>();
		final Set<Term> xvals = hierarchy.get(x);
		for (final Term xval : xvals) {
			final Set<Term> xidxs = idxTerms.get(x);
			for (final Term xidx : xidxs) {
				for (final Term yval : hierarchy.get(y)) {
					for (final Term yidx : idxTerms.get(y)) {
						rtr.add(SmtUtils.implies(script, SmtUtils.binaryEquality(script, xidx, yidx),
								SmtUtils.binaryEquality(script, xval, yval)));
					}
				}
			}
		}
		return SmtUtils.and(script, rtr);
	}

}
