package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.AffineRelation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryEqualityRelation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryRelation.NoRelationOfThisKindException;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.NotAffineException;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.normalForms.Cnf;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.normalForms.Dnf;
import de.uni_freiburg.informatik.ultimate.util.DebugMessage;

/**
 * Try to eliminate existentially quantified variables in terms. Therefore we
 * use that the term ∃v.v=c∧φ[v] is equivalent to term φ[c]. Resp. we use that
 * the term ∀v.v!=c∨φ[v] is equivalent to term φ[c].
 */
public class PartialQuantifierElimination {

	static final boolean USE_UPD = true;
	static final boolean USE_IRD = true;
	static final boolean USE_SOS = true;

	/**
	 * Returns equivalent formula. Quantifier is dropped if quantified variable
	 * not in formula. Quantifier is eliminated if this can be done by using a
	 * naive "destructive equality resolution".
	 * 
	 * @param services
	 * @param logger
	 */
	public static Term quantifier(IUltimateServiceProvider services, Logger logger, Script script, int quantifier,
			TermVariable[] vars, Term body, Term[]... patterns) {
		Set<TermVariable> varSet = new HashSet<TermVariable>(Arrays.asList(vars));
		body = elim(script, quantifier, varSet, body, services, logger);
		if (varSet.isEmpty()) {
			return body;
		} else {
			return script.quantifier(quantifier, varSet.toArray(new TermVariable[varSet.size()]), body, patterns);
		}

	}

	public static Term elim(Script script, int quantifier, final Set<TermVariable> eliminatees, final Term term,
			IUltimateServiceProvider services, Logger logger) {
		Set<TermVariable> occuringVars = new HashSet<TermVariable>(Arrays.asList(term.getFreeVars()));
		Iterator<TermVariable> it = eliminatees.iterator();
		while (it.hasNext()) {
			TermVariable tv = it.next();
			if (!occuringVars.contains(tv)) {
				it.remove();
			}
		}
		if (eliminatees.isEmpty()) {
			return term;
		}
		Term result;

		// transform to DNF (resp. CNF)
		if (quantifier == QuantifiedFormula.EXISTS) {
			result = (new Dnf(script, services)).transform(term);
		} else if (quantifier == QuantifiedFormula.FORALL) {
			result = (new Cnf(script, services)).transform(term);
		} else {
			throw new AssertionError("unknown quantifier");
		}
		if (result instanceof QuantifiedFormula) {
			QuantifiedFormula qf = (QuantifiedFormula) result;
			if (qf.getQuantifier() != quantifier) {
				throw new UnsupportedOperationException("quantifier alternation unsupported");
			}
			eliminatees.addAll(Arrays.asList(qf.getVariables()));
			result = qf.getSubformula();
		}

		// apply Destructive Equality Resolution
		Term termAfterDER;
		{

			Term[] oldParams;
			if (quantifier == QuantifiedFormula.EXISTS) {
				oldParams = SmtUtils.getDisjuncts(result);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				oldParams = SmtUtils.getConjuncts(result);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			Term[] newParams = new Term[oldParams.length];
			for (int i = 0; i < oldParams.length; i++) {
				Set<TermVariable> eliminateesDER = new HashSet<TermVariable>(eliminatees);
				newParams[i] = derSimple(script, quantifier, oldParams[i], eliminateesDER, logger);
			}
			if (quantifier == QuantifiedFormula.EXISTS) {
				termAfterDER = Util.or(script, newParams);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				termAfterDER = Util.and(script, newParams);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			result = termAfterDER;
			Set<TermVariable> remainingAfterDER = new HashSet<TermVariable>(eliminatees);
			remainingAfterDER.retainAll(Arrays.asList(result.getFreeVars()));
			eliminatees.retainAll(remainingAfterDER);
		}

		if (eliminatees.isEmpty()) {
			return result;
		}

		// apply Infinity Restrictor Drop
		Term termAfterIRD;
		if (USE_IRD) {
			Term[] oldParams;
			if (quantifier == QuantifiedFormula.EXISTS) {
				oldParams = SmtUtils.getDisjuncts(result);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				oldParams = SmtUtils.getConjuncts(result);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			Term[] newParams = new Term[oldParams.length];
			for (int i = 0; i < oldParams.length; i++) {
				Set<TermVariable> eliminateesIRD = new HashSet<TermVariable>(eliminatees);
				newParams[i] = irdSimple(script, quantifier, oldParams[i], eliminateesIRD, logger);
			}
			if (quantifier == QuantifiedFormula.EXISTS) {
				termAfterIRD = Util.or(script, newParams);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				termAfterIRD = Util.and(script, newParams);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			result = termAfterIRD;
			Set<TermVariable> remainingAfterIRD = new HashSet<TermVariable>(eliminatees);
			remainingAfterIRD.retainAll(Arrays.asList(result.getFreeVars()));
			eliminatees.retainAll(remainingAfterIRD);
		}

		if (eliminatees.isEmpty()) {
			return result;
		}

		// apply Unconnected Parameter Deletion
		Term termAfterUPD = null;
		if (USE_UPD) {
			Term[] oldParams;
			if (quantifier == QuantifiedFormula.EXISTS) {
				oldParams = SmtUtils.getDisjuncts(result);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				oldParams = SmtUtils.getConjuncts(result);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			Term[] newParams = new Term[oldParams.length];
			for (int i = 0; i < oldParams.length; i++) {
				Set<TermVariable> eliminateesUPD = new HashSet<TermVariable>(eliminatees);
				newParams[i] = updSimple(script, quantifier, oldParams[i], eliminateesUPD, logger);
			}
			if (quantifier == QuantifiedFormula.EXISTS) {
				termAfterUPD = Util.or(script, newParams);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				termAfterUPD = Util.and(script, newParams);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			result = termAfterUPD;
			Set<TermVariable> remainingAfterUPD = new HashSet<TermVariable>(eliminatees);
			remainingAfterUPD.retainAll(Arrays.asList(result.getFreeVars()));
			eliminatees.retainAll(remainingAfterUPD);
		}

		if (eliminatees.isEmpty()) {
			return result;
		}
		final boolean sosChangedTerm;
		// apply Store Over Select
		if (USE_SOS) {
			Set<TermVariable> remainingAndNewAfterSOS = new HashSet<TermVariable>();
			Term termAfterSOS;
			Term[] oldParams;
			if (quantifier == QuantifiedFormula.EXISTS) {
				oldParams = SmtUtils.getDisjuncts(result);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				oldParams = SmtUtils.getConjuncts(result);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			Term[] newParams = new Term[oldParams.length];
			for (int i = 0; i < oldParams.length; i++) {
				Set<TermVariable> eliminateesSOS = new HashSet<TermVariable>(eliminatees);
				newParams[i] = sos(script, quantifier, oldParams[i], eliminateesSOS, logger, services);
				remainingAndNewAfterSOS.addAll(eliminateesSOS);
			}
			if (quantifier == QuantifiedFormula.EXISTS) {
				termAfterSOS = Util.or(script, newParams);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				termAfterSOS = Util.and(script, newParams);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			sosChangedTerm = (result != termAfterSOS);
			result = termAfterSOS;
			eliminatees.retainAll(remainingAndNewAfterSOS);
			eliminatees.addAll(remainingAndNewAfterSOS);
		} else {
			sosChangedTerm = false;
		}

		if (eliminatees.isEmpty()) {
			return result;
		}

		// simplification
		result = SmtUtils.simplify(script, result, services);

		// (new SimplifyDDA(script)).getSimplifiedTerm(result);
		eliminatees.retainAll(Arrays.asList(result.getFreeVars()));

		// if (!eliminateesBeforeSOS.containsAll(eliminatees)) {
		// SOS introduced new variables that should be eliminated
		if (sosChangedTerm) {
			// if term was changed by SOS new elimination might be possible
			// Before the implementation of IRD we only retried elimination
			// if SOS introduced more quantified variables.
			result = elim(script, quantifier, eliminatees, result, services, logger);
		}
		assert Arrays.asList(result.getFreeVars()).containsAll(eliminatees) : "superficial variables";
		return result;
	}

	public static Term sos(Script script, int quantifier, Term term, Set<TermVariable> eliminatees, Logger logger,
			IUltimateServiceProvider services) {
		Term result = term;
		Set<TermVariable> overallAuxVars = new HashSet<TermVariable>();
		Iterator<TermVariable> it = eliminatees.iterator();
		while (it.hasNext()) {
			TermVariable tv = it.next();
			if (tv.getSort().isArraySort()) {
				// if (quantifier != QuantifiedFormula.EXISTS) {
				// // return term;
				// throw new
				// UnsupportedOperationException("QE for universal quantified arrays not implemented yet.");
				// }
				Set<TermVariable> thisIterationAuxVars = new HashSet<TermVariable>();
				Term elim = (new ElimStore3(script, services)).elim(quantifier, tv, result, thisIterationAuxVars);
				logger.debug(new DebugMessage("eliminated quantifier via SOS for {0}, additionally introduced {1}", tv,
						thisIterationAuxVars));
				overallAuxVars.addAll(thisIterationAuxVars);
				// if (Arrays.asList(elim.getFreeVars()).contains(tv)) {
				// elim = (new ElimStore3(script)).elim(tv, result,
				// thisIterationAuxVars);
				// }
				assert !Arrays.asList(elim.getFreeVars()).contains(tv) : "var is still there";
				result = elim;
			}
		}
		eliminatees.addAll(overallAuxVars);
		return result;
	}

	/**
	 * Try to eliminate the variables vars = {x_1,...,x_n} in term φ_1∧...∧φ_m.
	 * Therefore we use the following approach, which we call Unconnected
	 * Parameter Drop. If X is a subset of {x_1,...,x_n} and Φ is a subset
	 * {φ_1,...,φ_m} such that - variables in X occur only in term of Φ, and -
	 * terms in Φ contain only variables of X, and - the conjunction of all term
	 * in Φ is satisfiable. Then we can remove the conjuncts Φ and the
	 * quantified variables X from φ_1∧...∧φ_m and obtain an equivalent formula.
	 * 
	 * Is only sound if there are no uninterpreted function symbols in the term
	 * TODO: extend this to uninterpreted function symbols (for soundness)
	 * 
	 * @param logger
	 */
	public static Term updSimple(Script script, int quantifier, Term term, Set<TermVariable> vars, Logger logger) {
		Set<TermVariable> occuringVars = new HashSet<TermVariable>(Arrays.asList(term.getFreeVars()));
		vars.retainAll(occuringVars);
		Set<Term> parameters;
		if (quantifier == QuantifiedFormula.EXISTS) {
			parameters = new HashSet<Term>(Arrays.asList(SmtUtils.getConjuncts(term)));
		} else if (quantifier == QuantifiedFormula.FORALL) {
			parameters = new HashSet<Term>(Arrays.asList(SmtUtils.getDisjuncts(term)));
		} else {
			throw new AssertionError("unknown quantifier");
		}
		ConnectionPartition connection = new ConnectionPartition(parameters);
		List<TermVariable> removeableTvs = new ArrayList<TermVariable>();
		List<TermVariable> unremoveableTvs = new ArrayList<TermVariable>();
		List<Term> removeableTerms = new ArrayList<Term>();
		List<Term> unremoveableTerms = new ArrayList<Term>();
		for (Set<Term> connectedTerms : connection.getConnectedVariables()) {
			Set<TermVariable> connectedVars = SmtUtils.getFreeVars(connectedTerms);
			boolean isSuperfluous;
			if (quantifier == QuantifiedFormula.EXISTS) {
				isSuperfluous = isSuperfluousConjunction(script, connectedTerms, connectedVars, vars);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				isSuperfluous = isSuperfluousDisjunction(script, connectedTerms, connectedVars, vars);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			if (isSuperfluous) {
				removeableTvs.addAll(connectedVars);
				removeableTerms.addAll(connectedTerms);
			} else {
				unremoveableTvs.addAll(connectedVars);
				unremoveableTerms.addAll(connectedTerms);
			}
		}
		List<Term> termsWithoutTvs = connection.getTermsWithOutTvs();
		assert occuringVars.size() == removeableTvs.size() + unremoveableTvs.size();
		assert parameters.size() == removeableTerms.size() + unremoveableTerms.size() + termsWithoutTvs.size();
		for (Term termWithoutTvs : termsWithoutTvs) {
			LBool sat = Util.checkSat(script, termWithoutTvs);
			if (sat == LBool.UNSAT) {
				if (quantifier == QuantifiedFormula.EXISTS) {
					vars.clear();
					return script.term("false");
				} else if (quantifier == QuantifiedFormula.FORALL) {
					// we drop this term its equivalent to false
				} else {
					throw new AssertionError("unknown quantifier");
				}
			} else if (sat == LBool.SAT) {
				if (quantifier == QuantifiedFormula.EXISTS) {
					// we drop this term its equivalent to true
				} else if (quantifier == QuantifiedFormula.FORALL) {
					vars.clear();
					return script.term("true");
				} else {
					throw new AssertionError("unknown quantifier");
				}
			} else {
				throw new AssertionError("expecting sat or unsat");
			}
		}
		if (removeableTerms.isEmpty()) {
			logger.debug(new DebugMessage("not eliminated quantifier via UPD for {0}", occuringVars));
			return term;
		} else {
			vars.removeAll(removeableTvs);
			logger.debug(new DebugMessage("eliminated quantifier via UPD for {0}", removeableTvs));
			Term result;
			if (quantifier == QuantifiedFormula.EXISTS) {
				result = Util.and(script, unremoveableTerms.toArray(new Term[0]));
			} else if (quantifier == QuantifiedFormula.FORALL) {
				result = Util.or(script, unremoveableTerms.toArray(new Term[0]));
			} else {
				throw new AssertionError("unknown quantifier");
			}
			return result;
		}
	}

	/**
	 * Return true if connectedVars is a subset of quantifiedVars and the
	 * conjunction of terms is satisfiable.
	 */
	public static boolean isSuperfluousConjunction(Script script, Set<Term> terms, Set<TermVariable> connectedVars,
			Set<TermVariable> quantifiedVars) {
		if (quantifiedVars.containsAll(connectedVars)) {
			Term conjunction = Util.and(script, terms.toArray(new Term[terms.size()]));
			if (Util.checkSat(script, conjunction) == LBool.SAT) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return true if connectedVars is a subset of quantifiedVars and the
	 * disjunction of terms is not valid.
	 */
	public static boolean isSuperfluousDisjunction(Script script, Set<Term> terms, Set<TermVariable> connectedVars,
			Set<TermVariable> quantifiedVars) {
		if (quantifiedVars.containsAll(connectedVars)) {
			Term disjunction = Util.or(script, terms.toArray(new Term[terms.size()]));
			if (Util.checkSat(script, Util.not(script, disjunction)) == LBool.SAT) {
				return true;
			}
		}
		return false;
	}

	public static Term irdSimple(Script script, int quantifier, Term term, Collection<TermVariable> vars, Logger logger) {
		Iterator<TermVariable> it = vars.iterator();
		Term result = term;
		while (it.hasNext()) {
			TermVariable tv = it.next();
			if (!Arrays.asList(result.getFreeVars()).contains(tv)) {
				// case where var does not occur
				it.remove();
				continue;
			} else {
				if (tv.getSort().isNumericSort()) {
					Term withoutTv = irdSimple(script, quantifier, result, tv, logger);
					if (withoutTv != null) {
						logger.debug(new DebugMessage("eliminated quantifier via IRD for {0}", tv));
						result = withoutTv;
						it.remove();
					} else {
						logger.debug(new DebugMessage("not eliminated quantifier via IRD for {0}", tv));
					}
				} else {
					// ird is only applicable to variables of numeric sort
					logger.debug(new DebugMessage("not eliminated quantifier via IRD for {0}", tv));
				}
			}
		}
		return result;
	}

	/**
	 * If the application term contains only parameters param such that for each
	 * param one of the following holds and the third case applies at most once,
	 * we return all params that do not contain tv. 1. param does not contain tv
	 * 2. param is an AffineRelation such that tv is a variable of the
	 * AffineRelation and the function symbol is "distinct" and quantifier is ∃
	 * or the function symbol is "=" and the quantifier is ∀ 3. param is an
	 * inequality
	 * 
	 * @param logger
	 */
	public static Term irdSimple(Script script, int quantifier, Term term, TermVariable tv, Logger logger) {
		assert tv.getSort().isNumericSort() : "only applicable for numeric sorts";
		Term[] oldParams;
		if (quantifier == QuantifiedFormula.EXISTS) {
			oldParams = SmtUtils.getConjuncts(term);
		} else if (quantifier == QuantifiedFormula.FORALL) {
			oldParams = SmtUtils.getDisjuncts(term);
		} else {
			throw new AssertionError("unknown quantifier");
		}
		ArrayList<Term> paramsWithoutTv = new ArrayList<Term>();
		short inequalitiesWithTv = 0;
		for (Term oldParam : oldParams) {
			if (!Arrays.asList(oldParam.getFreeVars()).contains(tv)) {
				paramsWithoutTv.add(oldParam);
			} else {
				AffineRelation affineRelation;
				try {
					affineRelation = new AffineRelation(oldParam, logger);
				} catch (NotAffineException e) {
					// unable to eliminate quantifier
					return null;
				}
				if (!affineRelation.isVariable(tv)) {
					// unable to eliminate quantifier
					// tv occurs in affine relation but not as affine variable
					// it might occur inside a function or array.
					return null;
				}
				try {
					affineRelation.onLeftHandSideOnly(script, tv);
				} catch (NotAffineException e) {
					// unable to eliminate quantifier
					return null;
				}
				String functionSymbol = affineRelation.getFunctionSymbolName();
				switch (functionSymbol) {
				case "=":
					if (quantifier == QuantifiedFormula.EXISTS) {
						// unable to eliminate quantifier
						return null;
					} else if (quantifier == QuantifiedFormula.FORALL) {
						// we may drop this parameter
					} else {
						throw new AssertionError("unknown quantifier");
					}
					break;
				case "distinct":
					if (quantifier == QuantifiedFormula.EXISTS) {
						// we may drop this parameter
					} else if (quantifier == QuantifiedFormula.FORALL) {
						// unable to eliminate quantifier
						return null;
					} else {
						throw new AssertionError("unknown quantifier");
					}
					break;
				case ">":
				case ">=":
				case "<":
				case "<=":
					if (inequalitiesWithTv > 0) {
						// unable to eliminate quantifier, we may drop at most
						// one inequality
						return null;
					} else {
						inequalitiesWithTv++;
						// we may drop this parameter (but it has to be the
						// only dropped inequality
					}
					break;
				default:
					throw new AssertionError("unknown functionSymbol");
				}
			}
		}
		Term result;
		if (quantifier == QuantifiedFormula.EXISTS) {
			result = Util.and(script, paramsWithoutTv.toArray(new Term[paramsWithoutTv.size()]));
		} else if (quantifier == QuantifiedFormula.FORALL) {
			result = Util.or(script, paramsWithoutTv.toArray(new Term[paramsWithoutTv.size()]));
		} else {
			throw new AssertionError("unknown quantifier");
		}
		return result;
	}

	public static Term derSimple(Script script, int quantifier, Term term, Collection<TermVariable> vars, Logger logger) {
		Iterator<TermVariable> it = vars.iterator();
		Term result = term;
		while (it.hasNext()) {
			TermVariable tv = it.next();
			if (!Arrays.asList(result.getFreeVars()).contains(tv)) {
				// case where var does not occur
				it.remove();
				continue;
			} else {
				Term withoutTv = derSimple(script, quantifier, result, tv, logger);
				if (withoutTv != null) {
					result = withoutTv;
					it.remove();
				}
			}
		}
		return result;
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
	public static Term derSimple(Script script, int quantifier, Term term, TermVariable tv, Logger logger) {
		Term[] oldParams;
		if (quantifier == QuantifiedFormula.EXISTS) {
			oldParams = SmtUtils.getConjuncts(term);
		} else if (quantifier == QuantifiedFormula.FORALL) {
			oldParams = SmtUtils.getDisjuncts(term);
		} else {
			throw new AssertionError("unknown quantifier");
		}
		Term result;
		EqualityInformation eqInfo = getEqinfo(script, tv, oldParams, null, quantifier, logger);
		if (eqInfo == null) {
			logger.debug(new DebugMessage("not eliminated quantifier via DER for {0}", tv));
			result = null;
		} else {
			logger.debug(new DebugMessage("eliminated quantifier via DER for {0}", tv));
			Term[] newParams = new Term[oldParams.length - 1];
			Map<Term, Term> substitutionMapping = Collections.singletonMap(eqInfo.getVariable(), eqInfo.getTerm());
			SafeSubstitution substitution = new SafeSubstitution(script, substitutionMapping);
			for (int i = 0; i < eqInfo.getIndex(); i++) {
				newParams[i] = substitution.transform(oldParams[i]);
			}
			for (int i = eqInfo.getIndex() + 1; i < oldParams.length; i++) {
				newParams[i - 1] = substitution.transform(oldParams[i]);
			}
			if (quantifier == QuantifiedFormula.EXISTS) {
				result = Util.and(script, newParams);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				result = Util.or(script, newParams);
			} else {
				throw new AssertionError("unknown quantifier");
			}
		}
		return result;
	}

	/**
	 * Check all terms in context if they are an equality of the form givenTerm
	 * == t, such that t does not contain the subterm forbiddenTerm. If this is
	 * the case return corresponding equality information, otherwise return
	 * null. If forbiddenTerm is null all subterms in t are allowed.
	 * 
	 * @param logger
	 */
	public static EqualityInformation getEqinfo(Script script, Term givenTerm, Term[] context, Term forbiddenTerm,
			int quantifier, Logger logger) {
		BinaryEqualityRelation[] binaryRelations = new BinaryEqualityRelation[context.length];

		// stage 1: check if there is an "=" or "distinct" term where the
		// givenTerm is on one hand sie of the relation.
		for (int i = 0; i < context.length; i++) {
			if (!isSubterm(givenTerm, context[i])) {
				continue;
			}
			try {
				binaryRelations[i] = new BinaryEqualityRelation(context[i]);
			} catch (NoRelationOfThisKindException e2) {
				continue;
			}

			if (binaryRelations[i].getRelationSymbol().toString().equals("=") && quantifier == QuantifiedFormula.FORALL) {
				binaryRelations[i] = null;
				continue;
			} else if (binaryRelations[i].getRelationSymbol().toString().equals("distinct")
					&& quantifier == QuantifiedFormula.EXISTS) {
				binaryRelations[i] = null;
				continue;
			}

			Term lhs = binaryRelations[i].getLhs();
			Term rhs = binaryRelations[i].getRhs();

			if (lhs.equals(givenTerm) && !isSubterm(givenTerm, rhs)) {
				if (forbiddenTerm == null || !isSubterm(forbiddenTerm, rhs)) {
					return new EqualityInformation(i, givenTerm, rhs);
				}
			}
			if (rhs.equals(givenTerm) && !isSubterm(givenTerm, lhs)) {
				if (forbiddenTerm == null || !isSubterm(forbiddenTerm, lhs)) {
					return new EqualityInformation(i, givenTerm, lhs);
				}
			}
		}
		// stage 2: also rewrite linear terms if necessary to get givenTerm
		// to one hand side of the binary relation.
		for (int i = 0; i < context.length; i++) {
			if (binaryRelations[i] == null) {
				// not even binary equality relation that contains givenTerm
				continue;
			} else {
				AffineRelation affRel;
				try {
					affRel = new AffineRelation(context[i], logger);
				} catch (NotAffineException e1) {
					continue;
				}
				if (affRel.isVariable(givenTerm)) {
					Term equalTerm;
					try {
						ApplicationTerm equality = affRel.onLeftHandSideOnly(script, givenTerm);
						equalTerm = equality.getParameters()[1];
					} catch (NotAffineException e) {
						// no representation where var is on lhs
						continue;
					}
					if (forbiddenTerm != null && isSubterm(forbiddenTerm, equalTerm)) {
						continue;
					} else {
						return new EqualityInformation(i, givenTerm, equalTerm);
					}
				}
			}
		}
		// no equality information found
		return null;
	}

	/**
	 * Returns true if subterm is a subterm of term.
	 */
	private static boolean isSubterm(Term subterm, Term term) {
		return (new ContainsSubterm(subterm)).containsSubterm(term);
	}

	/**
	 * A given term, an equal term and the index at which this equality occurred
	 */
	public static class EqualityInformation {
		private final int m_Index;
		private final Term m_GivenTerm;
		private final Term m_EqualTerm;

		public EqualityInformation(int index, Term givenTerm, Term equalTerm) {
			m_Index = index;
			m_GivenTerm = givenTerm;
			m_EqualTerm = equalTerm;
		}

		public int getIndex() {
			return m_Index;
		}

		public Term getVariable() {
			return m_GivenTerm;
		}

		public Term getTerm() {
			return m_EqualTerm;
		}

	}

	/**
	 * Find term φ such that term implies tv == φ.
	 * 
	 * @param logger
	 */
	private static Term findEqualTermExists(TermVariable tv, Term term, Logger logger) {
		if (term instanceof ApplicationTerm) {
			ApplicationTerm appTerm = (ApplicationTerm) term;
			FunctionSymbol sym = appTerm.getFunction();
			Term[] params = appTerm.getParameters();
			if (sym.getName().equals("=")) {
				int tvOnOneSideOfEquality = tvOnOneSideOfEquality(tv, params, logger);
				if (tvOnOneSideOfEquality == -1) {
					return null;
				} else {
					assert (tvOnOneSideOfEquality == 0 || tvOnOneSideOfEquality == 1);
					return params[tvOnOneSideOfEquality];
				}
			} else if (sym.getName().equals("and")) {
				for (Term param : params) {
					Term equalTerm = findEqualTermExists(tv, param, logger);
					if (equalTerm != null) {
						return equalTerm;
					}
				}
				return null;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Find term φ such that tv != φ implies term
	 * 
	 * @param logger
	 */
	private static Term findEqualTermForall(TermVariable tv, Term term, Logger logger) {
		if (term instanceof ApplicationTerm) {
			ApplicationTerm appTerm = (ApplicationTerm) term;
			FunctionSymbol sym = appTerm.getFunction();
			Term[] params = appTerm.getParameters();
			if (sym.getName().equals("not")) {
				assert params.length == 1;
				if (params[0] instanceof ApplicationTerm) {
					appTerm = (ApplicationTerm) params[0];
					sym = appTerm.getFunction();
					params = appTerm.getParameters();
					if (sym.getName().equals("=")) {
						int tvOnOneSideOfEquality = tvOnOneSideOfEquality(tv, params, logger);
						if (tvOnOneSideOfEquality == -1) {
							return null;
						} else {
							assert (tvOnOneSideOfEquality == 0 || tvOnOneSideOfEquality == 1);
							return params[tvOnOneSideOfEquality];
						}
					} else {
						return null;
					}
				} else {
					return null;
				}
			} else if (sym.getName().equals("or")) {
				for (Term param : params) {
					Term equalTerm = findEqualTermForall(tv, param, logger);
					if (equalTerm != null) {
						return equalTerm;
					}
				}
				return null;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * return
	 * <ul>
	 * <li>0 if right hand side of equality is tv and left hand side does not
	 * contain tv
	 * <li>1 if left hand side of equality is tv and right hand side does not
	 * contain tv
	 * <li>-1 otherwise
	 * </ul>
	 * 
	 * @param logger
	 * 
	 */
	private static int tvOnOneSideOfEquality(TermVariable tv, Term[] params, Logger logger) {
		if (params.length != 2) {
			logger.warn("Equality of length " + params.length);
		}
		if (params[0] == tv) {
			final boolean rightHandSideContainsTv = Arrays.asList(params[1].getFreeVars()).contains(tv);
			if (rightHandSideContainsTv) {
				return -1;
			} else {
				return 1;
			}
		} else if (params[1] == tv) {
			final boolean leftHandSideContainsTv = Arrays.asList(params[0].getFreeVars()).contains(tv);
			if (leftHandSideContainsTv) {
				return -1;
			} else {
				return 0;
			}
		}
		return -1;
	}

}
