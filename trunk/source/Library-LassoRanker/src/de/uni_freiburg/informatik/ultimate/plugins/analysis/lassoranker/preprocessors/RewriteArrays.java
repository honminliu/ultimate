/*
 * Copyright (C) 2012-2014 University of Freiburg
 *
 * This file is part of the ULTIMATE LassoRanker Library.
 *
 * The ULTIMATE LassoRanker Library is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The ULTIMATE LassoRanker Library is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE LassoRanker Library. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE LassoRanker Library, or any covered work, by
 * linking or combining it with Eclipse RCP (or a modified version of
 * Eclipse RCP), containing parts covered by the terms of the Eclipse Public
 * License, the licensors of the ULTIMATE LassoRanker Library grant you
 * additional permission to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.preprocessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.core.api.UltimateServices;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.ApplicationTermFinder;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.PartialQuantifierElimination;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SafeSubstitution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayUpdate;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSelect;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSort;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.normalForms.Dnf;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.RankVar;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.ReplacementVar;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.VarCollector;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.VarFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.preprocessors.rewriteArrays.IndexAnalyzer;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.preprocessors.rewriteArrays.SetOfTwoeltons;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker.preprocessors.rewriteArrays.SingleUpdateNormalFormTransformer;
import de.uni_freiburg.informatik.ultimate.util.HashRelation;


/**
 * Replace term with arrays by term without arrays by introducing replacement
 * variables for all "important" array values and equalities that state the
 * constraints between array indices and array values (resp. their replacement
 * variables). 
 * 
 * 
 * @author Matthias Heizmann
 */
public class RewriteArrays implements PreProcessor {
	
	private static Logger s_Logger =
			UltimateServices.getInstance().getLogger(Activator.s_PLUGIN_ID);
	
	private static final String s_RepInPostfix  = "_in_array";
	private static final String s_RepOutPostfix = "_out_array";
	static final String s_AuxArray = "auxArray";
	
	/**
	 * The script used to transform the formula
	 */
	private Script m_Script;
	private Boogie2SMT m_boogie2SMT;
	
	/**
	 * Collection of all generated replacement variables and the terms
	 * that they replace.
	 * These variables are *not* added to in- or outVars.
	 */
	private final Map<TermVariable, Term> m_repVars;
	
	/**
	 * The replacement terms for the replacement variables for the formula.
	 * These terms will be set in conjunction with the whole formula.
	 */
	private final Collection<Term> m_repTerms;
	
	/**
	 * For generating replacement variables
	 */
	private final VarCollector m_VarCollector;
	
	/**
	 * Use assert statement to check if result is equivalent to the conjunction
	 * of input term and definition of replacement variables. 
	 */
	private static final boolean s_CheckResult = true;
	/**
	 * Use assert statement to check if the input is equivalent to the formula
	 * that is obtained by existentially quantifying each replacement variable
	 * in the result term.
	 */
	private static final boolean s_CheckResultWithQuantifiers = false;
	
	
	private HashRelation<TermVariable, List<Term>> m_Array2Indices;
	List<ArrayUpdate>[] m_ArrayUpdates;
	List<MultiDimensionalSelect>[] m_ArrayReads;
	ArrayGenealogy[] m_ArrayGenealogy;
	private Map<TermVariable, Map<List<Term>, TermVariable>> m_ArrayInstance2Index2CellVariable;
	private Term[] sunnf;
	private List<ArrayEquality>[] m_ArrayEqualities;
	private SafeSubstitution m_Select2CellVariable;

	private SetOfTwoeltons<Term> m_EqualTwoeltons;
	private SetOfTwoeltons<Term> m_DistinctTwoeltons;
	private SetOfTwoeltons<Term> m_UnknownTwoeltons;
	
	private final TransFormula m_OriginalStem;
	private final TransFormula m_OriginalLoop;

	private final Set<Term> m_ArrayIndexSupportingInvariants;

	
	public RewriteArrays(VarCollector rankVarCollector, 
			TransFormula originalStem, TransFormula originalLoop, Boogie2SMT 
			boogie2smt, 
			Set<Term> arrayIndexsupportingInvariants) {
		m_VarCollector = rankVarCollector;
		m_repVars = new LinkedHashMap<TermVariable, Term>();
		m_repTerms = new ArrayList<Term>();
		m_OriginalStem = originalStem;
		m_OriginalLoop = originalLoop;
		m_boogie2SMT = boogie2smt;
		m_ArrayIndexSupportingInvariants = arrayIndexsupportingInvariants;
	}
	
	@Override
	public String getDescription() {
		return "Removes arrays by introducing new variables for each " +
				"relevant array cell";
	}
	
	@Override
	public Term process(Script script, Term term) {
		if (!SmtUtils.containsArrayVariables(term)) {
			return term;
		}
		m_Script = script;
		term = SmtUtils.simplify(m_Script, term);
		Term dnf = (new Dnf(script)).transform(term);
		dnf = SmtUtils.simplify(m_Script, dnf);
		Term[] disjuncts = SmtUtils.getDisjuncts(dnf);
		sunnf = new Term[disjuncts.length];
		m_ArrayUpdates = new List[disjuncts.length];
		m_ArrayReads = new List[disjuncts.length];
		m_ArrayEqualities = new List[disjuncts.length];
		m_ArrayGenealogy = new ArrayGenealogy[disjuncts.length];
		for (int i=0; i<disjuncts.length; i++) {
			Term[] conjuncts = SmtUtils.getConjuncts(disjuncts[i]);
			ArrayEqualityExtractor aee = new ArrayEqualityExtractor(conjuncts);
			m_ArrayEqualities[i] = aee.getArrayEqualities();
			SingleUpdateNormalFormTransformer sunnft = 
					new SingleUpdateNormalFormTransformer(Util.and(m_Script,
							aee.getRemainingTerms().toArray(new Term[0])), m_Script, m_VarCollector.getFactory());
			m_ArrayUpdates[i] = sunnft.getArrayUpdates();
			sunnf[i] = sunnft.getRemainderTerm();
			m_ArrayReads[i] = extractArrayReads(sunnft.getArrayUpdates(), sunnft.getRemainderTerm());
			m_ArrayGenealogy[i] = new ArrayGenealogy(m_ArrayEqualities[i], m_ArrayUpdates[i], m_ArrayReads[i]);
		}
		
		new IndexCollector();
		IndexAnalyzer ia = new IndexAnalyzer(term, m_Array2Indices, m_boogie2SMT, m_VarCollector, m_OriginalStem, m_OriginalLoop);
		m_ArrayIndexSupportingInvariants.addAll(ia.getSupportingInvariants());
		m_EqualTwoeltons = ia.getEqualTwoeltons();
		m_DistinctTwoeltons = ia.getDistinctTwoeltons();
		m_UnknownTwoeltons = ia.getUnknownTwoeltons();
		CellVariableBuilder cvb = new CellVariableBuilder();
		m_ArrayInstance2Index2CellVariable = cvb.getArrayInstance2Index2CellVariable();
		m_Select2CellVariable = constructIndex2CellVariableSubstitution();
		Term indexValueConstraints = buildIndexValueConstraints();
		
		Term[] arrayEqualityConstraints = new Term[sunnf.length]; 
		for (int i=0; i<disjuncts.length; i++) {
			arrayEqualityConstraints[i] = buildArrayEqualityConstraints(m_ArrayEqualities[i]); 
		}
		
		Term[] arrayUpdateConstraints = new Term[sunnf.length]; 
		for (int i=0; i<disjuncts.length; i++) {
			arrayUpdateConstraints[i] = buildArrayUpdateConstraints(m_ArrayUpdates[i]); 
		}
		Term[] disjunctsWithUpdateConstraints = new Term[sunnf.length];
		for (int i=0; i<disjunctsWithUpdateConstraints.length; i++) {
			Term removedSelect = m_Select2CellVariable.transform(sunnf[i]);
			disjunctsWithUpdateConstraints[i] = Util.and(m_Script, removedSelect, arrayUpdateConstraints[i], arrayEqualityConstraints[i]);
		}
		Term resultDisjuntion = Util.or(m_Script, disjunctsWithUpdateConstraints);
		Term result = Util.and(m_Script, resultDisjuntion, indexValueConstraints, ia.getAdditionalConjunctsInvariants());
		
		
		result = PartialQuantifierElimination.elim(m_Script, QuantifiedFormula.EXISTS, cvb.getAuxVars(), result);
		
		m_VarCollector.addAuxVars(cvb.getAuxVars());

		
//			assert !s_CheckResult || !isIncorrect(term, result, repTerm) 
//					: "rewrite division unsound";
//			assert !s_CheckResultWithQuantifiers
//					||	!isIncorrectWithQuantifiers(term, result, repTerm) 
//					: "rewrite division unsound";
		SmtUtils.isArrayFree(result);
		result = SmtUtils.simplify(m_Script, result);
		return result;
	}
	
	private List<MultiDimensionalSelect> extractArrayReads(
			List<ArrayUpdate> arrayUpdates, Term remainderTerm) {
		ArrayList<MultiDimensionalSelect> result = new ArrayList<>();
		for (ArrayUpdate au : arrayUpdates) {
			for (Term indexEntry : au.getIndex()) {
				result.addAll(MultiDimensionalSelect.extractSelectDeep(indexEntry, true));
			}
			result.addAll(MultiDimensionalSelect.extractSelectDeep(au.getValue(), true));
		}
		result.addAll(MultiDimensionalSelect.extractSelectDeep(remainderTerm, true));
		return result;
	}

	/**
	 * Return true if we were able to prove that the result is incorrect.
	 * For this check we add to the input term the definition of the replacement
	 * variables.
	 */
	private boolean isIncorrect(Term input, Term result, Term repTerm) {
		Term inputWithDefinitions = m_Script.term("and", input, repTerm);
		return LBool.SAT == Util.checkSat(m_Script,
				m_Script.term("distinct",  inputWithDefinitions, result));
	}
	
	/**
	 * Return true if we were able to prove that the result is incorrect.
	 * For this check we existentially quantify replacement variables in the
	 * result term.
	 */
	private boolean isIncorrectWithQuantifiers(Term input, Term result,
			Term repTerm) {
		Term quantified;
		if (m_repVars.size() > 0) {
			quantified = m_Script.quantifier(Script.EXISTS,
					m_repVars.keySet().toArray(new TermVariable[0]), result);
		} else {
			quantified = m_Script.term("true");
		}
		return Util.checkSat(m_Script, m_Script.term("distinct", 
				input, quantified)) == LBool.SAT;
	}
	

	
//	private List<MultiDimensionalStore> extractArrayStores(Term term) {
//		List<MultiDimensionalStore> foundInThisIteration = new ArrayList<MultiDimensionalStore>();
//		Set<ApplicationTerm> storeTerms = 
//				(new ApplicationTermFinder("store", false)).findMatchingSubterms(term);
//		for (Term storeTerm : storeTerms) {
//			MultiDimensionalStore asd;
//			try {
//				asd = new MultiDimensionalStore(storeTerm);
//			} catch (ArrayStoreException e) {
//				throw new UnsupportedOperationException("unexpected store term");
//			}
//			foundInThisIteration.add(asd);
//		}
//		List<MultiDimensionalStore> result = new LinkedList<MultiDimensionalStore>();
//		while (!foundInThisIteration.isEmpty()) {
//			result.addAll(0, foundInThisIteration);
//			List<MultiDimensionalStore> foundInLastIteration = foundInThisIteration;
//			foundInThisIteration = new ArrayList<MultiDimensionalStore>();
//			for (MultiDimensionalStore asd : foundInLastIteration) {
//				storeTerms = 
//						(new ApplicationTermFinder("store", false)).findMatchingSubterms(asd.getArray());
//				for (Term storeTerm : storeTerms) {
//					MultiDimensionalStore newAsd;
//					try {
//						newAsd = new MultiDimensionalStore(storeTerm);
//					} catch (ArrayStoreException e) {
//						throw new UnsupportedOperationException("unexpected store term");
//					}
//					foundInThisIteration.add(newAsd);
//				}
//			}
//		}
//		return result;
//	}
	
	
	private class ArrayGenealogy {
		Map<TermVariable, TermVariable> m_Instance2OriginalGeneration = new HashMap<TermVariable, TermVariable>();
		
		/**
		 * If array a2 is defined as a2 = ("store", a1, index, value) we call
		 * a1 the parent generation of a2.  
		 */
		Map<TermVariable, TermVariable> m_ParentGeneration = new HashMap<TermVariable, TermVariable>();
		
		ArrayGenealogy(List<ArrayEquality> arrayEqualities, List<ArrayUpdate> arrayUpdates, List<MultiDimensionalSelect> arrayReads) {
			for (ArrayEquality ae : arrayEqualities) {
				putInstance2FirstGeneration(ae.getOutVar(), ae.getInVar());
				putInstance2FirstGeneration(ae.getInVar(), ae.getInVar());
				
			}
			for (ArrayUpdate au : arrayUpdates) {
				putParentGeneration(au.getNewArray(), au.getOldArray());
			}
			for (TermVariable tv : m_ParentGeneration.keySet()) {
				TermVariable fg = getFirstGeneration(tv);
				putInstance2FirstGeneration(tv, fg);
				// we add first generation several times, probably
				// less expensive than checking if already inserted
				putInstance2FirstGeneration(fg, fg);
			}
			for (MultiDimensionalSelect ar : arrayReads) {
				if (m_Instance2OriginalGeneration.get(ar.getArray()) == null) {
					putInstance2FirstGeneration((TermVariable)ar.getArray(), (TermVariable)ar.getArray());
				}
			}
		}
		
		private void putParentGeneration(TermVariable child, TermVariable parent) {
			assert child != null;
			assert parent != null;
			assert child != parent;
			assert child.toString() != null;
			assert parent.toString() != null;
			m_ParentGeneration.put(child, parent);
		}
		
		private void putInstance2FirstGeneration(TermVariable child, TermVariable progenitor) {
			assert child != null;
			assert progenitor != null;
			assert child.toString() != null;
			assert progenitor.toString() != null;
			m_Instance2OriginalGeneration.put(child, progenitor);
		}
		
		private TermVariable getFirstGeneration(TermVariable tv) {
			TermVariable parent = m_ParentGeneration.get(tv);
			if (parent == null) {
				return tv;
			} else {
				return getFirstGeneration(parent);
			}
		}
		
		public TermVariable getProgenitor(TermVariable tv) {
			return m_Instance2OriginalGeneration.get(tv);
		}
		
		public Set<TermVariable> getInstances() {
			return m_Instance2OriginalGeneration.keySet();
		}
	}
	

	
	private class IndexCollector {
		
		SafeSubstitution m_InVars2OutVars;
		SafeSubstitution m_OutVars2InVars;
		
		public IndexCollector() {
			constructSubstitutions();
			m_Array2Indices = new HashRelation<TermVariable, List<Term>>();
			for (int i=0; i<sunnf.length; i++) {
				for(ArrayUpdate au : m_ArrayUpdates[i]) {
					TermVariable firstGeneration = m_ArrayGenealogy[i].getProgenitor(au.getOldArray());
					Term[] index = au.getIndex();
					addFirstGenerationIndexPair(firstGeneration, index);
				}
				for (MultiDimensionalSelect ar : m_ArrayReads[i]) {
					TermVariable firstGeneration = m_ArrayGenealogy[i].getProgenitor((TermVariable) ar.getArray());
					Term[] index = ar.getIndex();
					m_Array2Indices.addPair(firstGeneration, Arrays.asList(index));
				}
			}
		}
		
		private void addFirstGenerationIndexPair(TermVariable firstGeneration, Term[] index) {
			m_Array2Indices.addPair(firstGeneration, Arrays.asList(index));
			//TODO: optimization the following is only necessary if the first
			// generation is no auxiliary variable.
			if (allVariablesAreInVars(Arrays.asList(index), m_VarCollector)) {
				Term[] inReplacedByOut = SmtUtils.substitutionElementwise(index, m_InVars2OutVars);
				m_Array2Indices.addPair(firstGeneration, Arrays.asList(inReplacedByOut));
			}
			if (allVariablesAreOutVars(Arrays.asList(index), m_VarCollector)) {
				Term[] outReplacedByIn = SmtUtils.substitutionElementwise(index, m_OutVars2InVars);
				m_Array2Indices.addPair(firstGeneration, Arrays.asList(outReplacedByIn));
			}
		}
		
		private void constructSubstitutions() {
			Map<Term,Term> in2outMapping = new HashMap<Term,Term>();
			Map<Term,Term> out2inMapping = new HashMap<Term,Term>();
			for (RankVar rv  : m_VarCollector.getInVars().keySet()) {
				Term inVar = m_VarCollector.getInVars().get(rv);
				assert inVar != null;
				Term outVar = m_VarCollector.getOutVars().get(rv);
				assert outVar != null;
				in2outMapping.put(inVar, outVar);
				out2inMapping.put(outVar, inVar);
			}
			m_InVars2OutVars = new SafeSubstitution(m_Script, in2outMapping);
			m_OutVars2InVars = new SafeSubstitution(m_Script, out2inMapping);
		}
	}
	

	
	private class CellVariableBuilder {
		private Map<TermVariable, Map<List<Term>, TermVariable>> m_ArrayInstance2Index2CellVariable;
		private Map<TermVariable, Map<List<Term>, ReplacementVar>> m_Array2Index2RepVar;
		private Set<TermVariable> m_AuxVars = new HashSet<TermVariable>();
		
		
		
		public CellVariableBuilder() {
			m_ArrayInstance2Index2CellVariable = new HashMap<TermVariable, Map<List<Term>,TermVariable>>();
			m_Array2Index2RepVar = new HashMap<TermVariable, Map<List<Term>,ReplacementVar>>();
			dotSomething();
		}

		/**
		 * Returns a getOrConstructReplacementVar that will represent the array
		 * cell array[index].
		 */
		private ReplacementVar getOrConstructReplacementVar(TermVariable array, List<Term> index) {
			List<Term> translatedIndex = Arrays.asList(translateTermVariablesToDefinitions(m_Script, m_VarCollector, index.toArray(new Term[0])));
			Map<List<Term>, ReplacementVar> index2repVar = m_Array2Index2RepVar.get(array);
			if (index2repVar == null) {
				index2repVar = new HashMap<List<Term>, ReplacementVar>();
				m_Array2Index2RepVar.put(array, index2repVar);
			}
			ReplacementVar repVar = index2repVar.get(translatedIndex);
			if (repVar == null) {
				VarFactory fac = m_VarCollector.getFactory();
				String name = getArrayCellName(array, translatedIndex);
				repVar = fac.getRepVar(name);
				if (repVar == null) {
					Term definition = SmtUtils.multiDimensionalSelect(m_Script, array, translatedIndex.toArray(new Term[0]));
					repVar = fac.registerRepVar(name, definition);
				}
				index2repVar.put(translatedIndex, repVar);
			}
			return repVar;
		}
		
	
		/**
		 * Returns a String that we use to refer to the array cell array[index].
		 */
		private String getArrayCellName(TermVariable array, List<Term> index) {
			return "arrayCell_" + SmtUtils.removeSmtQuoteCharacters(array.toString()) + 
					SmtUtils.removeSmtQuoteCharacters(index.toString());
		}
		
		public void dotSomething() {
			for (int i=0; i<sunnf.length; i++) {
				for (TermVariable instance : m_ArrayGenealogy[i].getInstances()) {
					TermVariable originalGeneration = m_ArrayGenealogy[i].getProgenitor(instance);
					Map<List<Term>, TermVariable> index2ArrayCellTv = m_ArrayInstance2Index2CellVariable.get(instance);
					if (index2ArrayCellTv == null) {
						index2ArrayCellTv = new HashMap<List<Term>, TermVariable>();
						m_ArrayInstance2Index2CellVariable.put(instance, index2ArrayCellTv);
					}
					Set<List<Term>> indicesOfOriginalGeneration = m_Array2Indices.getImage(originalGeneration);
					if (indicesOfOriginalGeneration == null) {
						s_Logger.info("Array " + originalGeneration + " is never accessed");
						continue;
					}
					for (List<Term> index : indicesOfOriginalGeneration) {
						TermVariable tv = index2ArrayCellTv.get(index);
						if (tv == null) {
							tv = constructTermVariable(instance, index);
							index2ArrayCellTv.put(index, tv);
						}
						boolean isInVarCell = isInVarCell(instance, index);
						boolean isOutVarCell = isOutVarCell(instance, index);
						if (isInVarCell || isOutVarCell) {
							TermVariable arrayRepresentative = (TermVariable) getDefinition(m_VarCollector, instance);
							ReplacementVar rv = getOrConstructReplacementVar(arrayRepresentative, index);
							if (isInVarCell) {
								if (!m_VarCollector.getInVars().containsKey(rv)) {
									m_VarCollector.addInVar(rv, tv);
								} else {
									assert m_VarCollector.getInVars().get(rv) == tv;
								}
							}
							if (isOutVarCell) {
								if (!m_VarCollector.getOutVars().containsKey(rv)) {
									m_VarCollector.addOutVar(rv, tv);
								} else {
									assert m_VarCollector.getOutVars().get(rv) == tv;
								}
							}
						} else {
							addToAuxVars(tv);
						}
					}
					
				}
			}
		}
		

		private void addToAuxVars(TermVariable tv) {
			m_AuxVars.add(tv);
			//assert false : "not yet implemented";
		}

		private TermVariable constructTermVariable(TermVariable instance, List<Term> index) {
			Sort arraySort = instance.getSort();
			assert arraySort.isArraySort();
			MultiDimensionalSort mdias = new MultiDimensionalSort(arraySort);
			assert mdias.getDimension() == index.size();
			Sort valueSort = mdias.getArrayValueSort();
			String name = getArrayCellName(instance, index);
			TermVariable tv = m_VarCollector.getFactory().getNewTermVariable(name, valueSort);
			return tv;
		}

		
		/**
		 * Is the cellVariable that we construct for arrayInstance[index] is
		 * an inVar. This is the case if arrayInstance and each free variable
		 * of index is an inVar.
		 */
		private boolean isInVarCell(TermVariable arrayInstance, List<Term> index) {
			if (isInvar(arrayInstance, m_VarCollector)) {
				return allVariablesAreInVars(index, m_VarCollector);
			} else {
				return false;
			}
		}
		
		private boolean isOutVarCell(TermVariable arrayInstance, List<Term> index) {
			if (isOutvar(arrayInstance, m_VarCollector)) {
				return allVariablesAreOutVars(index, m_VarCollector);
			} else {
				return false;
			}
		}
		
		


		public Map<TermVariable, Map<List<Term>, TermVariable>> getArrayInstance2Index2CellVariable() {
			return m_ArrayInstance2Index2CellVariable;
		}

		public Set<TermVariable> getAuxVars() {
			return m_AuxVars;
		}
		
	}
	
	private static boolean allVariablesAreInVars(List<Term> terms, VarCollector vc) {
		for (Term term : terms) {
			if (!allVariablesAreInVars(term, vc)) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean allVariablesAreOutVars(List<Term> terms, VarCollector vc) {
		for (Term term : terms) {
			if (!allVariablesAreOutVars(term, vc)) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean allVariablesAreInVars(Term term, VarCollector vc) {
		for (TermVariable tv : term.getFreeVars()) {
			if(!isInvar(tv, vc)) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean allVariablesAreOutVars(Term term, VarCollector vc) {
		for (TermVariable tv : term.getFreeVars()) {
			if(!isOutvar(tv, vc)) {
				return false;
			}
		}
		return true;
	}
	
	public static Term getDefinition(VarCollector vc, TermVariable tv) {
		RankVar rv = vc.getInVarsReverseMapping().get(tv);
		if (rv == null) {
			rv = vc.getOutVarsReverseMapping().get(tv);
		}
		if (rv == null) {
			throw new AssertionError();
		}
		return rv.getDefinition();
	}
	
	public static Term[] translateTermVariablesToDefinitions(Script script, VarCollector vc, Term... terms) {
		Term[] result = new Term[terms.length];
		for (int i=0; i<terms.length; i++) {
			Map<Term, Term> substitutionMapping = new HashMap<Term, Term>();
			for (TermVariable tv : terms[i].getFreeVars()) {
				Term definition = getDefinition(vc, tv);
				substitutionMapping.put(tv, definition);
			}
			result[i] = (new SafeSubstitution(script, substitutionMapping)).transform(terms[i]);
		}
		return result;
	}

	
	private static boolean isInvar(TermVariable tv, VarCollector vc) {
		return vc.getInVarsReverseMapping().keySet().contains(tv);
	}
	
	private static boolean isOutvar(TermVariable tv, VarCollector vc) {
		return vc.getOutVarsReverseMapping().keySet().contains(tv);
	}
	
	private Term buildArrayEqualityConstraints(List<ArrayEquality> arrayEqualities) {
		Term[] conjuncts = new Term[arrayEqualities.size()];
		int offset = 0;
		for (ArrayEquality ae : arrayEqualities) {
			conjuncts[offset] = buildArrayEqualityConstraints(ae.getInVar(), ae.getOutVar());
			offset++;
		}
		return Util.and(m_Script, conjuncts);
	}
	
	private Term buildArrayEqualityConstraints(TermVariable oldArray,
			TermVariable newArray) {
		Map<List<Term>, TermVariable> newInstance2Index2CellVariable = m_ArrayInstance2Index2CellVariable.get(newArray);
		Map<List<Term>, TermVariable> oldInstance2Index2CellVariable = m_ArrayInstance2Index2CellVariable.get(oldArray);
		if (newInstance2Index2CellVariable == null && oldInstance2Index2CellVariable == null) {
			return m_Script.term("true");
		}
		Term[] conjuncts = new Term[newInstance2Index2CellVariable.keySet().size()];
		int offset = 0;
		for (List<Term> index : newInstance2Index2CellVariable.keySet()) {
			Term newCellVariable = newInstance2Index2CellVariable.get(index);
			Term oldCellVariable = oldInstance2Index2CellVariable.get(index);
			conjuncts[offset] = SmtUtils.binaryEquality(m_Script, oldCellVariable, newCellVariable);
			offset++;
		}
		return Util.and(m_Script, conjuncts);
	}

	private Term buildArrayUpdateConstraints(List<ArrayUpdate> arrayUpdates) {
		Term[] conjuncts = new Term[arrayUpdates.size()];
		int offset = 0;
		for (ArrayUpdate au : arrayUpdates) {
			conjuncts[offset] = buildArrayUpdateConstraints(au.getNewArray(), au.getOldArray(), au.getIndex(), au.getValue());
			offset++;
		}
		Term result = Util.and(m_Script, conjuncts);
		assert (new ApplicationTermFinder("select", false)).findMatchingSubterms(result).isEmpty() : "contains select terms";
		return result;
	}
	
	
	private Term buildArrayUpdateConstraints(TermVariable newArray,
			TermVariable oldArray, Term[] updateIndex, Term data) {
		data = m_Select2CellVariable.transform(data);
		Map<List<Term>, TermVariable> newInstance2Index2CellVariable = m_ArrayInstance2Index2CellVariable.get(newArray);
		Map<List<Term>, TermVariable> oldInstance2Index2CellVariable = m_ArrayInstance2Index2CellVariable.get(oldArray);
		Term[] conjuncts = new Term[newInstance2Index2CellVariable.keySet().size()];
		int offset = 0;
		for (List<Term> index : newInstance2Index2CellVariable.keySet()) {
			Term newCellVariable = newInstance2Index2CellVariable.get(index);
			Term oldCellVariable = oldInstance2Index2CellVariable.get(index);
			Term indexIsUpdateIndex = pairwiseEqualityExploitTwoeltons(index.toArray(new Term[index.size()]), updateIndex);
			Term newDataIsUpdateData = SmtUtils.binaryEquality(m_Script, newCellVariable, data);
			Term newDateIsOldData = SmtUtils.binaryEquality(m_Script, newCellVariable, oldCellVariable);
			Term indexIsNotUpdateIndex = Util.not(m_Script, indexIsUpdateIndex);
			Term indexIsUpdateIndexImpliesUpdateData = Util.or(m_Script, indexIsNotUpdateIndex, newDataIsUpdateData);
			Term indexIsNotUpdateIndexImpliesOldData = Util.or(m_Script, indexIsUpdateIndex, newDateIsOldData);
			conjuncts[offset] = Util.and(m_Script, indexIsUpdateIndexImpliesUpdateData, indexIsNotUpdateIndexImpliesOldData);
			offset++;
		}
		return Util.and(m_Script, conjuncts);
	}

	private Term buildIndexValueConstraints() {
		Term[] conjuncts = new Term[m_ArrayInstance2Index2CellVariable.size()];
		int offset = 0;
		for (Entry<TermVariable, Map<List<Term>, TermVariable>> entry : m_ArrayInstance2Index2CellVariable.entrySet()) {
			Map<List<Term>, TermVariable> indices2values = entry.getValue();
			conjuncts[offset] = buildIndexValueConstraints(indices2values);
			offset++;
		}
		return Util.and(m_Script, conjuncts);
	}

	private Term buildIndexValueConstraints(Map<List<Term>, TermVariable> indices2values) {
		List<Term>[] indices = new List[indices2values.size()];
		Term[] values = new Term[indices2values.size()];
		int offset = 0;
		for (Entry<List<Term>, TermVariable> index2value : indices2values.entrySet()) {
			indices[offset] = index2value.getKey();
			values[offset] = index2value.getValue();
			offset++;
		}
		int numberOfPairs = indices2values.size()*(indices2values.size()-1)/2;
		Term[] conjuncts = new Term[numberOfPairs];
		int k = 0;
		for (int i=0; i<indices2values.size(); i++) {
			for (int j=0; j<i; j++) {
				List<Term> index1 = indices[i];
				List<Term> index2 = indices[j];
				Term value1 = values[i];
				Term value2 = values[j];
				conjuncts[k] = indexEqualityImpliesValueEquality(index1.toArray(new Term[0]), index2.toArray(new Term[0]), value1, value2);
				k++;
			}
		}
		Term result = Util.and(m_Script, conjuncts);
		return result;
	}

	private Term indexEqualityImpliesValueEquality(Term[] index1,
			Term[] index2, Term value1, Term value2) {
		Term indexEquality = pairwiseEqualityExploitTwoeltons(index1, index2);
		Term valueEquality = SmtUtils.binaryEquality(m_Script, value1, value2);
		return Util.or(m_Script, Util.not(m_Script, indexEquality), valueEquality);
	}
	
	Term pairwiseEqualityExploitTwoeltons(Term[] index1, Term[] index2) {
		assert index1.length == index2.length;
		Term[] conjuncts = new Term[index1.length];
		for (int i=0; i<index1.length; i++) {
			Term fst = index1[i];
			Term snd = index2[i];
			if (fst == snd || m_EqualTwoeltons.containsTwoelton(fst, snd)) {
				conjuncts[i] = m_Script.term("true");
			} else if (m_DistinctTwoeltons.containsTwoelton(fst, snd)) {
				conjuncts[i] = m_Script.term("false");
			} else if (m_UnknownTwoeltons.containsTwoelton(fst, snd)) {
				conjuncts[i] = SmtUtils.binaryEquality(m_Script, fst, snd);
			} else {
				throw new AssertionError("unknown twoelton");
			}
		}
		return Util.and(m_Script, conjuncts);
	}

	
	/**
	 * Replace all select terms by the corresponding cell variables.
	 */
	private SafeSubstitution constructIndex2CellVariableSubstitution() {
		Map<Term, Term> substitutionMapping = new HashMap<Term, Term>();
		for (int i=0; i<sunnf.length; i++) {
			for (MultiDimensionalSelect ar : m_ArrayReads[i]) {
				Term cellVariable = m_ArrayInstance2Index2CellVariable.get(ar.getArray()).get(Arrays.asList(ar.getIndex()));
				substitutionMapping.put(ar.getSelectTerm(), cellVariable);
			}
		}
		return new SafeSubstitution(m_Script, substitutionMapping);
	}
	

	private class ArrayEquality {
		private final Term m_OriginalTerm;
		private TermVariable m_InVar;
		private TermVariable m_OutVar;
		
		public ArrayEquality(Term term) throws ArrayEqualityException {
			if (!(term instanceof ApplicationTerm)) {
				throw new ArrayEqualityException("no ApplicationTerm");
			}
			ApplicationTerm eqAppTerm = (ApplicationTerm) term;
			if (!eqAppTerm.getFunction().getName().equals("=")) {
				throw new ArrayEqualityException("no equality");
			}
			if (!(eqAppTerm.getParameters().length == 2)) {
				throw new ArrayEqualityException("no binary equality");
			}
			m_OriginalTerm = term;
			Term lhsTerm = eqAppTerm.getParameters()[0];
			Term rhsTerm = eqAppTerm.getParameters()[1];
			if (!(lhsTerm.getSort().isArraySort())) {
				throw new ArrayEqualityException("no array");
			}
			TermVariable lhs;
			if (lhsTerm instanceof TermVariable) {
				lhs = (TermVariable) lhsTerm;
			} else {
				throw new ArrayEqualityException("no tv");
			}
			TermVariable rhs;
			if (rhsTerm instanceof TermVariable) {
				rhs = (TermVariable) rhsTerm;
			} else {
				throw new ArrayEqualityException("no tv");
			}
			if (m_VarCollector.getInVarsReverseMapping().containsKey(lhs)) {
				m_InVar = lhs;
			} else if (m_VarCollector.getOutVarsReverseMapping().containsKey(lhs)) {
				m_OutVar = lhs;
			} else {
				throw new ArrayEqualityException("lhs neither in nor out");
			}
			if (m_VarCollector.getInVarsReverseMapping().containsKey(rhs)) {
				m_InVar = rhs;
			} else if (m_VarCollector.getOutVarsReverseMapping().containsKey(rhs)) {
				m_OutVar = rhs;
			} else {
				throw new ArrayEqualityException("rhs neither in nor out");
			}
		}

		public Term getOriginalTerm() {
			return m_OriginalTerm;
		}

		public TermVariable getInVar() {
			return m_InVar;
		}

		public TermVariable getOutVar() {
			return m_OutVar;
		}
	}
	
	
	private static class ArrayEqualityException extends Exception {

		private static final long serialVersionUID = -5344050289008681972L;

		public ArrayEqualityException(String message) {
			super(message);
		}
	}
	
	/**
	 * Given an array of terms, partition them into terms that are array
	 * equalities and terms that are not array equalities.
	 */
	private class ArrayEqualityExtractor {
		private final List<ArrayEquality> m_ArrayEqualities = 
				new ArrayList<ArrayEquality>();
		private final List<Term> remainingTerms = 
				new ArrayList<Term>();
		
		public ArrayEqualityExtractor(Term[] terms) {
			for (Term term : terms) {
				ArrayEquality au;
				try {
					au = new ArrayEquality(term);
				} catch (ArrayEqualityException e) {
					au = null;
				}
				if (au == null) {
					remainingTerms.add(term);
				} else {
					m_ArrayEqualities.add(au);
				}
			}
		}
		
		public List<ArrayEquality> getArrayEqualities() {
			return m_ArrayEqualities;
		}

		public List<Term> getRemainingTerms() {
			return remainingTerms;
		}
	}
}