/*
 * Copyright (C) 2013-2015 Christian Schilling (schillic@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.minimization;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.Determinize;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;

/**
 * This class implements Brzozowski's minimization algorithm.
 * 
 * The key idea is to reverse and determinize the automaton twice.
 * After each reversal the resulting DFA is minimal wrt. its language
 * (i.e., the reversed DFA minimally accepts the reverse language and the
 * twice reversed DFA minimally accepts the original language).
 * 
 * Reversal means that
 * - the transitions are turned around,
 * - the final states become the initial states,
 * - the initial states become the final states.
 * 
 * NOTE: The implementation is naive in the sense that both a new automaton is
 * created after each operation and the reversal and determinization do not
 * know each other (potentially they may .
 * 
 * @author Christian Schilling <schillic@informatik.uni-freiburg.de>
 * @param <LETTER> letter type
 * @param <STATE> state type
 */
public class MinimizeBrzozowski<LETTER, STATE>
		extends AMinimizeNwa<LETTER, STATE>
		implements IOperation<LETTER, STATE>{
	/**
	 * Constructor.
	 * 
	 * @param services Ultimate services
	 * @param stateFactory state factory
	 * @param operand input (finite, possibly nondeterministic) automaton
	 * @throws AutomataOperationCanceledException when execution is cancelled
	 */
	public MinimizeBrzozowski(final AutomataLibraryServices services,
			final StateFactory<STATE> stateFactory, 
			final INestedWordAutomaton<LETTER, STATE> operand)
					throws AutomataOperationCanceledException {
		super(services, stateFactory, "MinimizeBrzozowski", operand);
		
		assert super.isFiniteAutomaton() :
			"The input automaton contains call or return transitions.";
		
		minimize();
		mLogger.info(exitMessage());
	}
	
	/**
	 * This method simply reverses and determinizes the automaton twice, which
	 * results in the minimal DFA.
	 * 
	 * @throws AutomataOperationCanceledException when execution is cancelled
	 */
	private void minimize() throws AutomataOperationCanceledException {
		INestedWordAutomaton<LETTER, STATE> automaton = mOperand;
		for (int i = 0; i < 2; ++i) {
			super.checkForContinuation();
			automaton = reverse(automaton);
			
			super.checkForContinuation();
			automaton = determinize(automaton);
		}
		directResultConstruction(automaton);
	}
	
	/**
	 * This method reverses the automaton.
	 * 
	 * Reversal means that
     * - the transitions are turned around,
     * - the final states become the initial states,
     * - the initial states become the final states.
     * 
	 * @param automaton automaton
	 * @return the reversed automaton
	 */
	private INestedWordAutomaton<LETTER, STATE> reverse(
			final INestedWordAutomaton<LETTER, STATE> automaton) {
		final NestedWordAutomaton<LETTER, STATE> reversed =
				new NestedWordAutomaton<LETTER, STATE>(mServices, 
						automaton.getInternalAlphabet(),
						automaton.getCallAlphabet(),
						automaton.getReturnAlphabet(),
						automaton.getStateFactory());
		
		// add states
		for (final STATE state : automaton.getStates()) {
			reversed.addState(automaton.isFinal(state),
					automaton.isInitial(state), state);
		}
		// add (only internal) transitions
		for (final STATE state : automaton.getStates()) {
			for (final OutgoingInternalTransition<LETTER, STATE> trans :
					automaton.internalSuccessors(state)) {
				reversed.addInternalTransition(
						trans.getSucc(), trans.getLetter(), state);
			}
		}
		
		return reversed;
	}
	
	/**
	 * This method determinizes the automaton.
	 * 
	 * @param automaton automaton
	 * @return the determinized automaton
	 * @throws AutomataOperationCanceledException when execution is cancelled
	 */
	private INestedWordAutomaton<LETTER, STATE> determinize(
			final INestedWordAutomaton<LETTER, STATE> automaton)
					throws AutomataOperationCanceledException {
		return new Determinize<LETTER, STATE>(mServices, mStateFactory, automaton).getResult();
	}
}
