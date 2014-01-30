package de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.core.api.UltimateServices;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.preferences.Preferences;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.templates.RankingFunctionTemplate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.TransFormula;


/**
 * This is the synthesizer that generates ranking functions.
 * 
 * @author Jan Leike
 */
class TerminationArgumentSynthesizer {
	
	private static Logger s_Logger =
			UltimateServices.getInstance().getLogger(Activator.s_PLUGIN_ID);
	
	/**
	 * SMT script for the template instance
	 */
	private Script m_script;
	
	// Stem and loop transitions for the linear lasso program
	private TransFormula m_stem_transition;
	private TransFormula m_loop_transition;
	
	// Stem and loop transitions as linear inequalities in DNF
	private LinearTransition m_stem;
	private LinearTransition m_loop;
	
	// Objects resulting from the synthesis process
	private RankingFunction m_ranking_function = null;
	private Collection<SupportingInvariant> m_supporting_invariants = null;
	
	public final Preferences m_preferences;
	
	/**
	 * Constructor for the ranking function synthesizer.
	 * @param stem transition formula for the program's stem
	 * @param loop transition formula for the program's loop
	 * @param script SMT Solver
	 * @throws Exception If something goes wrong ;)
	 */
	public TerminationArgumentSynthesizer(Script script, TransFormula stem_transition,
			TransFormula loop_transition, LinearTransition stem,
			LinearTransition loop, Preferences preferences) {
		m_preferences = preferences;
		m_script = script;
		script.setLogic(Logics.QF_NRA);
		
		m_supporting_invariants = new ArrayList<SupportingInvariant>();
		
		m_stem_transition = stem_transition;
		m_stem = stem;
		m_loop_transition = loop_transition;
		m_loop = loop;
	}
	
	/**
	 * @return Boogie variables that are relevant for supporting invariants.
	 * Variables that occur as outVars of the stem but are not read by the loop
	 * (i.e., do not occur as inVar of the loop) are not relevant for
	 * supporting invariants.
	 */
	private Collection<BoogieVar> getSIVars() { 
		Set<BoogieVar> result = 
				new HashSet<BoogieVar>(m_stem_transition.getOutVars().keySet());
		result.retainAll(m_loop_transition.getInVars().keySet());
		return result;
	}
	
	/**
	 * @return Boogie variables that are relevant for ranking functions
	 */
	private Collection<BoogieVar> getRankVars() {
		Collection<BoogieVar> vars = 
				new HashSet<BoogieVar>(m_loop_transition.getOutVars().keySet());
		vars.retainAll(m_loop_transition.getInVars().keySet());
		return vars;
	}
	
	/**
	 * Use the ranking template to build the constraints whose solution gives
	 * the termination argument
	 * @param template the ranking function template
	 * @param si_generators Output container for the used SI generators
	 * @return List of all conjuncts of the constraints
	 */
	private Collection<Term> buildConstraints(RankingFunctionTemplate template,
			Collection<SupportingInvariantGenerator> si_generators) {
		List<Term> conj = new ArrayList<Term>(); // List of constraints
		
		Collection<BoogieVar> siVars = getSIVars();
		List<List<LinearInequality>> templateConstraints =
				template.constraints(m_loop_transition.getInVars(),
						m_loop_transition.getOutVars());
		List<String> annotations = template.getAnnotations();
		assert annotations.size() == templateConstraints.size();
		
		s_Logger.info("We have " + m_loop.getNumPolyhedra()
				+ " loop disjuncts and " + templateConstraints.size()
				+ " template conjuncts.");
		
		// Negate template inequalities
		for (List<LinearInequality> templateDisj : templateConstraints) {
			for (LinearInequality li : templateDisj) {
				li.negate();
			}
		}
		
		// loop(x, x') /\ si(x) -> template(x, x')
		// Iterate over the loop conjunctions and template disjunctions
		for (List<LinearInequality> loopConj : m_loop.getPolyhedra()) {
			for (int m = 0; m < templateConstraints.size(); ++m) {
				MotzkinTransformation motzkin =
						new MotzkinTransformation(m_script,
								m_preferences.annotate_terms);
				motzkin.annotation = annotations.get(m);
				motzkin.add_inequalities(loopConj);
				motzkin.add_inequalities(templateConstraints.get(m));
				
				// Add supporting invariants
				assert(m_preferences.num_strict_invariants >= 0);
				for (int i = 0; i < m_preferences.num_strict_invariants; ++i) {
					SupportingInvariantGenerator sig =
							new SupportingInvariantGenerator(m_script, siVars,
									true);
					si_generators.add(sig);
					motzkin.add_inequality(sig.generate(
							m_loop_transition.getInVars()));
				}
				assert(m_preferences.num_non_strict_invariants >= 0);
				for (int i = 0; i < m_preferences.num_non_strict_invariants;
						++i) {
					SupportingInvariantGenerator sig =
							new SupportingInvariantGenerator(m_script, siVars,
									false);
					si_generators.add(sig);
					motzkin.add_inequality(sig.generate(
							m_loop_transition.getInVars()));
				}
				s_Logger.debug(motzkin);
				conj.add(motzkin.transform());
			}
		}
		
		// Add constraints for the supporting invariants
		s_Logger.debug("Adding the constraints for " + si_generators.size()
				+ " supporting invariants.");
		for (SupportingInvariantGenerator sig : si_generators) {
			// stem(x0) -> si(x0)
			for (List<LinearInequality> stemConj : m_stem.getPolyhedra()) {
				MotzkinTransformation motzkin =
						new MotzkinTransformation(m_script,
								m_preferences.annotate_terms);
				motzkin.annotation = "invariant initiation";
				motzkin.add_inequalities(stemConj);
				LinearInequality li =
						sig.generate(m_stem_transition.getOutVars());
				li.negate();
				motzkin.add_inequality(li);
//				s_Logger.debug(motzkin);
				conj.add(motzkin.transform());
			}
			// si(x) /\ loop(x, x') -> si(x')
			for (List<LinearInequality> loopConj : m_loop.getPolyhedra()) {
				MotzkinTransformation motzkin =
						new MotzkinTransformation(m_script,
								m_preferences.annotate_terms);
				motzkin.annotation = "invariant consecution";
				motzkin.add_inequalities(loopConj);
				motzkin.add_inequality(sig.generate(
						m_loop_transition.getInVars())); // si(x)
				LinearInequality li = sig.generate(
						m_loop_transition.getOutVars()); // ~si(x')
				li.needs_motzkin_coefficient =
						!m_preferences.only_nondecreasing_invariants;
				li.negate();
				motzkin.add_inequality(li);
//				s_Logger.debug(motzkin);
				conj.add(motzkin.transform());
			}
		}
		return conj;
	}
	
	/**
	 * Ranking function generation for lasso programs
	 * 
	 * This procedure is complete in the sense that if there is a linear ranking
	 * function that can be derived with a linear supporting invariant of the
	 * form si(x) >= 0, then it will be found by this procedure.
	 * 
	 * Iff a ranking function is found, this method returns true and the
	 * resulting ranking function and supporting invariant can be retrieved
	 * using the methods getRankingFunction() and getSupportingInvariant().
	 * 
	 * @param template the ranking template to be used
	 * @return whether a ranking function was found
	 * @throws SMTLIBException error with the SMT solver
	 * @throws TermException if the supplied transitions contain
	 *          non-affine update statements
	 */
	public boolean synthesize(RankingFunctionTemplate template)
			throws SMTLIBException, TermException {
		Collection<BoogieVar> rankVars = getRankVars();
		Collection<BoogieVar> siVars = getSIVars();
		template.init(m_script, rankVars);
		s_Logger.debug("Variables for ranking functions: " + rankVars);
		s_Logger.debug("Variables for supporting invariants: " + siVars);
		if (siVars.isEmpty()) {
			s_Logger.info("There is no variables for invariants; "
					+ "disabling supporting invariant generation.");
			m_preferences.num_strict_invariants = 0;
			m_preferences.num_non_strict_invariants = 0;
		}
		
		// List of all used supporting invariant generators
		Collection<SupportingInvariantGenerator> si_generators =
				new ArrayList<SupportingInvariantGenerator>();
		
		// Assert all conjuncts generated from the template
		Collection<Term> constraints =
				buildConstraints(template, si_generators);
		int num_motzkin = constraints.size();
		s_Logger.info("We have " + num_motzkin
				+ " Motzkin's Theorem applications.");
		s_Logger.info("A total of " + si_generators.size()
				+ " supporting invariants were added.");
		for (Term constraint : constraints) {
			m_script.assertTerm(constraint);
		}
		
		// Check for a model
		boolean success = false;
		LBool sat = m_script.checkSat();
		if (sat == LBool.SAT) {
			s_Logger.debug("Found a model, " +
					"proceeding to extract ranking function.");
			
			// Extract ranking function
			Map<Term, Rational> val_rf =
					AuxiliaryMethods.preprocessValuation(m_script.getValue(
							template.getVariables().toArray(new Term[0])));
			m_ranking_function = template.extractRankingFunction(val_rf);
			
			// Extract supporting invariants
			for (SupportingInvariantGenerator sig : si_generators) {
				Map<Term, Rational> val_si =
						AuxiliaryMethods.preprocessValuation(m_script.getValue(
								sig.getVariables().toArray(new Term[0])));
				m_supporting_invariants.add(sig.extractSupportingInvariant(
						val_si));
			}
			success = true;
		} else if (sat == LBool.UNKNOWN) {
			s_Logger.info("Statistics: template " + 
					template.getClass().getSimpleName() + 
					" with degree " + template.getDegree() + 
					" too complicated for solver");
			// Problem: If we use the following line we can receive the 
			// following response which is not SMTLIB2 compliant.
			// (:reason-unknown canceled)
			// Object reason = m_script.getInfo(":reason-unknown");
		}
		
		return success;
	}
	
	public TerminationArgument getArgument() {
		return new TerminationArgument(m_ranking_function,
				m_supporting_invariants);
	}
}
