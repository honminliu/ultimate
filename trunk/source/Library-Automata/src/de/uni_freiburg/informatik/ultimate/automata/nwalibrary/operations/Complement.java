/*
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2009-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Automata Library.
 * 
 * The ULTIMATE Automata Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Automata Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automata Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Automata Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations;

import de.uni_freiburg.informatik.ultimate.core.services.model.ILogger;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.ResultChecker;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.ComplementDD;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.IntersectDD;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.reachableStatesAutomaton.NestedWordAutomatonReachableStates;


public class Complement<LETTER,STATE> implements IOperation<LETTER,STATE> {

	private final AutomataLibraryServices m_Services;
	private final ILogger m_Logger;
	
	private final INestedWordAutomatonSimple<LETTER,STATE> m_Operand;
	private DeterminizeNwa<LETTER,STATE> m_Determinized;
	private ComplementDeterministicNwa<LETTER,STATE> m_Complement;
	private NestedWordAutomatonReachableStates<LETTER,STATE> m_Result;
	private final IStateDeterminizer<LETTER, STATE> m_StateDeterminizer;
	private final StateFactory<STATE> m_StateFactory;
	
	
	@Override
	public String operationName() {
		return "complement";
	}
	
	
	@Override
	public String startMessage() {
		return "Start " + operationName() + " Operand " + 
			m_Operand.sizeInformation();
	}
	
	
	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result " + 
				m_Result.sizeInformation();
	}
	
	
	public Complement(AutomataLibraryServices services,
			INestedWordAutomatonSimple<LETTER,STATE> operand, 
			IStateDeterminizer<LETTER,STATE> stateDeterminizer, 
			StateFactory<STATE> sf) throws AutomataLibraryException {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);
		m_Operand = operand;
		m_StateDeterminizer = stateDeterminizer;
		m_StateFactory = sf;
		m_Logger.info(startMessage());
		computeComplement();
		m_Logger.info(exitMessage());
	}
	
	public Complement(AutomataLibraryServices services,
			StateFactory<STATE> stateFactory, 
			INestedWordAutomatonSimple<LETTER,STATE> operand) throws AutomataLibraryException {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);
		m_Operand = operand;
		m_StateDeterminizer = new PowersetDeterminizer<LETTER, STATE>(operand, true, stateFactory);
		m_StateFactory = stateFactory;
		m_Logger.info(startMessage());
		computeComplement();
		m_Logger.info(exitMessage());
	}
	
	private void computeComplement() throws AutomataLibraryException {
		if (m_Operand instanceof DeterminizeNwa) {
			m_Determinized = (DeterminizeNwa<LETTER, STATE>) m_Operand;
			m_Complement = new ComplementDeterministicNwa<LETTER, STATE>(m_Determinized);
			m_Result = new NestedWordAutomatonReachableStates<LETTER, STATE>(m_Services, m_Complement);
			return;
		} 
		if (m_StateDeterminizer instanceof PowersetDeterminizer) {
			boolean success = tryWithoutDeterminization();
			if (success) {
				return;
			}
		}
		m_Determinized = new DeterminizeNwa<LETTER, STATE>(m_Services, m_Operand, m_StateDeterminizer, m_StateFactory);
		m_Complement = new ComplementDeterministicNwa<LETTER, STATE>(m_Determinized);
		m_Result = new NestedWordAutomatonReachableStates<LETTER, STATE>(m_Services, m_Complement);
	}
	
	private boolean tryWithoutDeterminization() throws AutomataLibraryException {
		assert (m_StateDeterminizer instanceof PowersetDeterminizer);
		TotalizeNwa<LETTER, STATE> totalized = new TotalizeNwa<LETTER, STATE>(m_Operand, m_StateFactory);
		ComplementDeterministicNwa<LETTER,STATE> complemented = new ComplementDeterministicNwa<LETTER, STATE>(totalized);
		NestedWordAutomatonReachableStates<LETTER, STATE> result = 
				new NestedWordAutomatonReachableStates<LETTER, STATE>(m_Services, complemented);
		if (!totalized.nonDeterminismInInputDetected()) {
			m_Complement = complemented;
			m_Result = result;
			m_Logger.info("Operand was deterministic. Have not used determinization.");
			return true;
		} else {
			m_Logger.info("Operand was not deterministic. Recomputing result with determinization.");
			return false;
		}
	}
	



	@Override
	public INestedWordAutomatonOldApi<LETTER, STATE> getResult()
			throws AutomataLibraryException {
		return m_Result;
	}
	
	
	
	public boolean checkResult(StateFactory<STATE> sf) throws AutomataLibraryException {
		boolean correct = true;
		if (m_StateDeterminizer instanceof PowersetDeterminizer) {
			m_Logger.info("Start testing correctness of " + operationName());
			INestedWordAutomatonOldApi<LETTER, STATE> operandOldApi = ResultChecker.getOldApiNwa(m_Services, m_Operand);

			// intersection of operand and result should be empty
			INestedWordAutomatonOldApi<LETTER, STATE> intersectionOperandResult = 
					(new IntersectDD<LETTER, STATE>(m_Services, operandOldApi, m_Result)).getResult();
			correct &= (new IsEmpty<LETTER, STATE>(m_Services, intersectionOperandResult)).getResult();
			INestedWordAutomatonOldApi<LETTER, STATE> resultDD = 
					(new ComplementDD<LETTER, STATE>(m_Services, sf, operandOldApi)).getResult();
			// should have same number of states as old complementation
			// does not hold, resultDD sometimes has additional sink state
			//		correct &= (resultDD.size() == m_Result.size());
			// should recognize same language as old computation
			correct &= (ResultChecker.nwaLanguageInclusion(m_Services, resultDD, m_Result, sf) == null);
			correct &= (ResultChecker.nwaLanguageInclusion(m_Services, m_Result, resultDD, sf) == null);
			if (!correct) {
				ResultChecker.writeToFileIfPreferred(m_Services, operationName() + "Failed", "", m_Operand);
			}
			m_Logger.info("Finished testing correctness of " + operationName());
		} else {
			m_Logger.warn("operation not tested");
		}
		return correct;
	}
	
}
