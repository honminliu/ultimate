/*
 * Copyright (C) 2014-2015 Betim Musa (musab@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE ModelCheckerUtils Library.
 * 
 * The ULTIMATE ModelCheckerUtils Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE ModelCheckerUtils Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ModelCheckerUtils Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ModelCheckerUtils Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE ModelCheckerUtils Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.partialQuantifierElimination;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.services.model.ILogger;

import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.IFreshTermVariableConstructor;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SafeSubstitution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SafeSubstitutionWithLocalSimplification;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.AffineRelation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.NotAffineException;
import de.uni_freiburg.informatik.ultimate.util.DebugMessage;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;

/**
 * Destructive equality resolution (DER) for terms in XNF.
 * @author Matthias Heizmann
 */
public class XnfDer extends XjunctPartialQuantifierElimination {

	private final IFreshTermVariableConstructor m_FreshVarConstructor;

	public XnfDer(Script script, IUltimateServiceProvider services, 
			IFreshTermVariableConstructor freshVarConstructor) {
		super(script, services);
		m_FreshVarConstructor = freshVarConstructor;
	}

	@Override
	public String getName() {
		return "desctructive equality resolution";
	}

	@Override
	public String getAcronym() {
		return "DER";
	}
	
	@Override
	public boolean resultIsXjunction() {
		return true;
	};


	@Override
	public Term[] tryToEliminate(int quantifier, Term[] inputAtoms,
			Set<TermVariable> eliminatees) {
		Term[] resultAtoms = inputAtoms;
		boolean someVariableWasEliminated;
		// an elimination may allow further eliminations
		// repeat the following until no variable was eliminated
		do {
			someVariableWasEliminated = false;
			Iterator<TermVariable> it = eliminatees.iterator();
			while (it.hasNext()) {
				if (!m_Services.getProgressMonitorService().continueProcessing()) {
					throw new ToolchainCanceledException(this.getClass(),
							"eliminating " + eliminatees.size() + 
							" quantified variables from " + inputAtoms.length + " xjuncts");
				}
				TermVariable tv = it.next();
				if (!SmtUtils.getFreeVars(Arrays.asList(resultAtoms)).contains(tv)) {
					// case where var does not occur
					it.remove();
					continue;
				} else {
					Term[] withoutTv = derSimple(m_Script, quantifier, resultAtoms, tv, m_Logger);
					if (withoutTv != null) {
						resultAtoms = withoutTv;
						it.remove();
						someVariableWasEliminated = true;
					}
				}
			}
		} while (someVariableWasEliminated);
		return resultAtoms;
	}

	/**
	 * TODO: revise documentation Try to eliminate the variables vars in term.
	 * Let vars = {x_1,...,x_n} and term = φ. Returns a term that is equivalent
	 * to ∃x_1,...,∃x_n φ, but were variables are removed. Successfully removed
	 * variables are also removed from vars. Analogously for universal
	 * quantification.
	 * 
	 * @param logger
	 */
	public Term[] derSimple(Script script, int quantifier, Term[] inputAtoms, TermVariable tv, ILogger logger) {
		final Term[] resultAtoms;
		EqualityInformation eqInfo = EqualityInformation.getEqinfo(script, tv, inputAtoms, null, quantifier, logger);
		if (eqInfo == null) {
			logger.debug(new DebugMessage("not eliminated quantifier via DER for {0}", tv));
			resultAtoms = null;
		} else {
			logger.debug(new DebugMessage("eliminated quantifier via DER for {0}", tv));
			resultAtoms = new Term[inputAtoms.length - 1];
			Map<Term, Term> substitutionMapping = Collections.singletonMap(eqInfo.getVariable(), eqInfo.getTerm());
			SafeSubstitution substitution = new SafeSubstitutionWithLocalSimplification(
					script, m_FreshVarConstructor, substitutionMapping);
			for (int i = 0; i < eqInfo.getIndex(); i++) {
				resultAtoms[i] = substituteAndNormalize(substitution, inputAtoms[i]);
			}
			for (int i = eqInfo.getIndex() + 1; i < inputAtoms.length; i++) {
				resultAtoms[i - 1] = substituteAndNormalize(substitution, inputAtoms[i]);
			}
		}
		return resultAtoms;
	}
	
	/**
	 * Apply substitution to term and normalize afterwards if the substitution modified the term.
	 */
	private Term substituteAndNormalize(SafeSubstitution substitution, Term term) {
		Term result =  substitution.transform(term);
		if (term != result) {
			try {
				AffineRelation afr = new AffineRelation(m_Script, result);
				result = afr.positiveNormalForm(m_Script);
			} catch (NotAffineException e) {
				// Do nothing - we return result.
			}
		}
		return result;
	}

	


}
