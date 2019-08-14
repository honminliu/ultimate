package de.uni_freiburg.informatik.ultimate.mso;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.VpAlphabet;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.statefactory.StringFactory;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Term;

public abstract class MSODOperations {

	public MSODOperations() {

	}

	/**
	 * Constructs an empty automaton.
	 */
	public static NestedWordAutomaton<MSODAlphabetSymbol, String>
			emptyAutomaton(final AutomataLibraryServices services) {

		final Set<MSODAlphabetSymbol> alphabet = new HashSet<>();
		final VpAlphabet<MSODAlphabetSymbol> vpAlphabet = new VpAlphabet<>(alphabet);
		final StringFactory stringFactory = new StringFactory();

		return new NestedWordAutomaton<>(services, vpAlphabet, stringFactory);
	}

	/**
	 * Constructs an automaton that represents "false".
	 */
	public static NestedWordAutomaton<MSODAlphabetSymbol, String>
			falseAutomaton(final AutomataLibraryServices services) {

		final NestedWordAutomaton<MSODAlphabetSymbol, String> automaton = emptyAutomaton(services);
		final MSODAlphabetSymbol symbol = new MSODAlphabetSymbol();
		automaton.getAlphabet().add(symbol);

		automaton.addState(true, false, "init");
		automaton.addInternalTransition("init", symbol, "init");

		return automaton;
	}

	/**
	 * Constructs an automaton that represents "true".
	 */
	public static NestedWordAutomaton<MSODAlphabetSymbol, String>
			trueAutomaton(final AutomataLibraryServices services) {

		final NestedWordAutomaton<MSODAlphabetSymbol, String> automaton = emptyAutomaton(services);
		final MSODAlphabetSymbol symbol = new MSODAlphabetSymbol();
		automaton.getAlphabet().add(symbol);

		automaton.addState(true, true, "init");
		automaton.addInternalTransition("init", symbol, "init");

		return automaton;
	}

	/**
	 * Constructs an automaton that represents an Int variable.
	 *
	 * @throws IllegalArgumentException
	 *             if x is not of type Int.
	 */
	public static NestedWordAutomaton<MSODAlphabetSymbol, String>
			intVariableAutomaton(final AutomataLibraryServices services, final Term x) {

		if (!MSODUtils.isIntVariable(x)) {
			throw new IllegalArgumentException("Input x must be an Int variable.");
		}

		final NestedWordAutomaton<MSODAlphabetSymbol, String> automaton = emptyAutomaton(services);
		final Map<String, MSODAlphabetSymbol> symbols = MSODUtils.createAlphabet(x);
		automaton.getAlphabet().addAll(symbols.values());

		automaton.addState(true, false, "init");
		automaton.addState(false, true, "final");

		automaton.addInternalTransition("init", symbols.get("x0"), "init");
		automaton.addInternalTransition("init", symbols.get("x1"), "final");
		automaton.addInternalTransition("final", symbols.get("x0"), "final");

		return automaton;
	}

	/**
	 * Returns a {@link NestedWordAutomaton} which is constructed from the given {@link INestedWordAutomaton}.
	 */
	public static NestedWordAutomaton<MSODAlphabetSymbol, String> nwa(final AutomataLibraryServices services,
			final INestedWordAutomaton<MSODAlphabetSymbol, String> automaton) {

		final NestedWordAutomaton<MSODAlphabetSymbol, String> result = emptyAutomaton(services);
		result.getAlphabet().addAll(automaton.getAlphabet());

		for (final String state : automaton.getStates()) {
			result.addState(automaton.isInitial(state), automaton.isFinal(state), state);
		}

		for (final String state : automaton.getStates()) {
			for (final OutgoingInternalTransition<MSODAlphabetSymbol, String> transition : automaton
					.internalSuccessors(state)) {
				result.addInternalTransition(state, transition.getLetter(), transition.getSucc());
			}
		}

		return result;
	}

	/**
	 * Constructs an automaton that represents "x < c".
	 *
	 * @throws IllegalArgumentException
	 *             if x is not of type Int or c is less than 0.
	 */
	public abstract NestedWordAutomaton<MSODAlphabetSymbol, String>
			strictIneqAutomaton(final AutomataLibraryServices services, final Term x, final Rational c);

	/**
	 * Constructs an automaton that represents "x-y < c".
	 *
	 * @throws IllegalArgumentException
	 *             if x, y are not of type Int or c is less than 0.
	 */
	public abstract INestedWordAutomaton<MSODAlphabetSymbol, String>
			strictIneqAutomaton(final AutomataLibraryServices services, final Term x, final Term y, final Rational c)
					throws AutomataLibraryException;

	/**
	 * Constructs an automaton that represents "-x < c".
	 *
	 * @throws IllegalArgumentException
	 *             if x is not of type Int or c is less than 0.
	 */
	public abstract INestedWordAutomaton<MSODAlphabetSymbol, String>
			strictNegIneqAutomaton(final AutomataLibraryServices services, final Term x, final Rational c);

	/**
	 * Constructs an automaton that represents "X strictSubsetInt Y".
	 *
	 * @throws IllegalArgumentException
	 *             if x, y are not of type SetOfInt.
	 */
	public abstract INestedWordAutomaton<MSODAlphabetSymbol, String>
			strictSubsetAutomaton(final AutomataLibraryServices services, final Term x, final Term y);

	/**
	 * Constructs an automaton that represents "X nonStrictSubsetInt Y".
	 *
	 * @throws IllegalArgumentException
	 *             if x, y are not of type SetOfInt.
	 */
	public abstract INestedWordAutomaton<MSODAlphabetSymbol, String>
			subsetAutomaton(final AutomataLibraryServices services, final Term x, final Term y);

	/**
	 * Constructs an automaton that represents "x+c element Y".
	 *
	 * @throws IllegalArgumentException
	 *             if x, y are not of type Int, SetOfInt or c is smaller than 0.
	 */
	public abstract INestedWordAutomaton<MSODAlphabetSymbol, String>
			elementAutomaton(final AutomataLibraryServices services, final Term x, final Rational c, final Term y)
					throws AutomataLibraryException;

	/**
	 * Constructs an automaton that represents "c element X".
	 *
	 * @throws IllegalArgumentException
	 *             if x is not of type SetOfInt or c is smaller than 0.
	 */
	public abstract INestedWordAutomaton<MSODAlphabetSymbol, String>
			constElementAutomaton(final AutomataLibraryServices services, final Rational c, final Term x);

	/**
	 * Constructs a copy of the given automaton with the extended or reduced alphabet.
	 */
	public abstract INestedWordAutomaton<MSODAlphabetSymbol, String> reconstruct(final AutomataLibraryServices services,
			final INestedWordAutomaton<MSODAlphabetSymbol, String> automaton, final Set<MSODAlphabetSymbol> alphabet,
			final boolean isExtended);

}