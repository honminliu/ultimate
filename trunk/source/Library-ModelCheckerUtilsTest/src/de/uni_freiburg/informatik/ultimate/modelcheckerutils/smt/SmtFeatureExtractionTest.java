/*
 * Copyright (C) 2018 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2018 University of Freiburg
 *
 * This file is part of the ULTIMATE ModelCheckerUtilsTest Library.
 *
 * The ULTIMATE ModelCheckerUtilsTest Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE ModelCheckerUtilsTest Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ModelCheckerUtilsTest Library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ModelCheckerUtilsTest Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE ModelCheckerUtilsTest Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger.LogLevel;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SMTFeatureExtractionTermClassifier;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.TermParseUtils;
import de.uni_freiburg.informatik.ultimate.test.mocks.UltimateMocks;

/**
 *
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class SmtFeatureExtractionTest {

	private IUltimateServiceProvider mServices;
	private Script mScript;
	private ILogger mLogger;

	@Before
	public void setUp() {
		mServices = UltimateMocks.createUltimateServiceProviderMock(LogLevel.DEBUG);
		mScript = UltimateMocks.createZ3Script(LogLevel.INFO);
		mLogger = mServices.getLoggingService().getLogger("lol");
		mScript.setLogic(Logics.ALL);
	}
	@Test
	public void CheckSingleTerm() {
		final Sort intSort = SmtSortUtils.getIntSort(mScript);

		final String names = "ABCDE";
		for (int i = 0; i < names.length(); ++i) {
			final Term term = declareVar(String.valueOf(names.charAt(i)), intSort);
		}

		final Term input = TermParseUtils.parseTerm(mScript, "(= A 0)");
		final LBool isSat = SmtUtils.checkSatTerm(mScript, input);
		final SMTFeatureExtractionTermClassifier tc = new SMTFeatureExtractionTermClassifier(mLogger);
		tc.checkTerm(input);

		mLogger.info("Original:               " + input.toStringDirect());
		mLogger.info("Original isSat:         " + isSat);
		mLogger.info("Original equiv classes: " + tc.getEquivalenceClasses());
		mLogger.info("Original #Vars:         " + tc.getNumberOfVariables());
		mLogger.info("Original Functions:         " + tc.getOccuringFunctionNames());
		mLogger.info("Original Sorts:         " + tc.getOccuringSortNames());
		mLogger.info("Original Quantifiers:         " + tc.getNumberOfQuantifiers());
		Assert.assertEquals(tc.getEquivalenceClasses().toString(),"{[A]=A}");
		Assert.assertEquals(tc.getNumberOfVariables(), 1);
		Assert.assertEquals(tc.getOccuringFunctionNames().toString(), "{==1}");
		Assert.assertEquals(tc.getOccuringSortNames().toString(), "{Int=1}");
		Assert.assertEquals(tc.getNumberOfQuantifiers(), 0);
	}


	@Test
	public void CheckAndTerm() {
		final Sort intSort = SmtSortUtils.getIntSort(mScript);

		final String names = "ABCDE";
		for (int i = 0; i < names.length(); ++i) {
			final Term term = declareVar(String.valueOf(names.charAt(i)), intSort);
		}

		final Term input = TermParseUtils.parseTerm(mScript, "(and (= A (+ B 1)) (= C 0))");
		final LBool isSat = SmtUtils.checkSatTerm(mScript, input);
		final SMTFeatureExtractionTermClassifier tc = new SMTFeatureExtractionTermClassifier(mLogger);
		tc.checkTerm(input);

		mLogger.info("Original:               " + input.toStringDirect());
		mLogger.info("Original isSat:         " + isSat);
		mLogger.info("Original equiv classes: " + tc.getEquivalenceClasses());
		mLogger.info("Original #Vars:         " + tc.getNumberOfVariables());
		mLogger.info("Original Functions:         " + tc.getOccuringFunctionNames());
		mLogger.info("Original Sorts:         " + tc.getOccuringSortNames());
		mLogger.info("Original Quantifiers:         " + tc.getNumberOfQuantifiers());
		Assert.assertEquals(tc.getEquivalenceClasses().toString(),"{[A, B, C]=B}");
		Assert.assertEquals(tc.getNumberOfVariables(), 3);
		Assert.assertEquals(tc.getOccuringFunctionNames().toString(), "{and=1, +=1, ==2}");
		Assert.assertEquals(tc.getOccuringSortNames().toString(), "{Int=3}");
		Assert.assertEquals(tc.getNumberOfQuantifiers(), 0);
	}

	@Test
	public void CheckOrTerm() {
		final Sort intSort = SmtSortUtils.getIntSort(mScript);

		final String names = "ABCDE";
		for (int i = 0; i < names.length(); ++i) {
			final Term term = declareVar(String.valueOf(names.charAt(i)), intSort);
		}

		final Term input = TermParseUtils.parseTerm(mScript, "(or (= A (+ B 1)) (= C 0))");
		final LBool isSat = SmtUtils.checkSatTerm(mScript, input);
		final SMTFeatureExtractionTermClassifier tc = new SMTFeatureExtractionTermClassifier(mLogger);
		tc.checkTerm(input);

		mLogger.info("Original:               " + input.toStringDirect());
		mLogger.info("Original isSat:         " + isSat);
		mLogger.info("Original equiv classes: " + tc.getEquivalenceClasses());
		mLogger.info("Original #Vars:         " + tc.getNumberOfVariables());
		mLogger.info("Original Functions:         " + tc.getOccuringFunctionNames());
		mLogger.info("Original Sorts:         " + tc.getOccuringSortNames());
		mLogger.info("Original Quantifiers:         " + tc.getNumberOfQuantifiers());
		Assert.assertEquals(tc.getEquivalenceClasses().toString(),"{[C]=C, [A, B]=B}");
		Assert.assertEquals(tc.getNumberOfVariables(), 3);
		Assert.assertEquals(tc.getOccuringFunctionNames().toString(), "{or=1, +=1, ==2}");
		Assert.assertEquals(tc.getOccuringSortNames().toString(), "{Int=3}");
		Assert.assertEquals(tc.getNumberOfQuantifiers(), 0);
	}

	@Test
	public void CheckOrAndTerm() {
		final Sort intSort = SmtSortUtils.getIntSort(mScript);

		final String names = "ABCDE";
		for (int i = 0; i < names.length(); ++i) {
			final Term term = declareVar(String.valueOf(names.charAt(i)), intSort);
		}

		final Term input = TermParseUtils.parseTerm(mScript, "(or (and (= A (+ B 1)) (= D 1)) (= C 0))");
		final LBool isSat = SmtUtils.checkSatTerm(mScript, input);
		final SMTFeatureExtractionTermClassifier tc = new SMTFeatureExtractionTermClassifier(mLogger);
		tc.checkTerm(input);

		mLogger.info("Original:               " + input.toStringDirect());
		mLogger.info("Original isSat:         " + isSat);
		mLogger.info("Original equiv classes: " + tc.getEquivalenceClasses());
		mLogger.info("Original #Vars:         " + tc.getNumberOfVariables());
		mLogger.info("Original Functions:         " + tc.getOccuringFunctionNames());
		mLogger.info("Original Sorts:         " + tc.getOccuringSortNames());
		mLogger.info("Original Quantifiers:         " + tc.getNumberOfQuantifiers());
		Assert.assertEquals(tc.getEquivalenceClasses().toString(),"{[C]=C, [A, B, D]=B}");
		Assert.assertEquals(tc.getNumberOfVariables(), 4);
		Assert.assertEquals(tc.getOccuringFunctionNames().toString(), "{or=1, and=1, +=1, ==3}");
		Assert.assertEquals(tc.getOccuringSortNames().toString(), "{Int=4}");
		Assert.assertEquals(tc.getNumberOfQuantifiers(), 0);
	}

	@Test
	public void CheckOrOrTerm() {
		final Sort intSort = SmtSortUtils.getIntSort(mScript);

		final String names = "ABCDE";
		for (int i = 0; i < names.length(); ++i) {
			final Term term = declareVar(String.valueOf(names.charAt(i)), intSort);
		}

		final Term input = TermParseUtils.parseTerm(mScript, "(or (or (= A (+ B 1)) (= D 1)) (= C 0))");
		final LBool isSat = SmtUtils.checkSatTerm(mScript, input);
		final SMTFeatureExtractionTermClassifier tc = new SMTFeatureExtractionTermClassifier(mLogger);
		tc.checkTerm(input);

		mLogger.info("Original:               " + input.toStringDirect());
		mLogger.info("Original isSat:         " + isSat);
		mLogger.info("Original equiv classes: " + tc.getEquivalenceClasses());
		mLogger.info("Original #Vars:         " + tc.getNumberOfVariables());
		mLogger.info("Original Functions:         " + tc.getOccuringFunctionNames());
		mLogger.info("Original Sorts:         " + tc.getOccuringSortNames());
		mLogger.info("Original Quantifiers:         " + tc.getNumberOfQuantifiers());
		Assert.assertEquals(tc.getEquivalenceClasses().toString(),"{[C]=C, [A, B]=B, [D]=D}");
		Assert.assertEquals(tc.getNumberOfVariables(), 4);
		Assert.assertEquals(tc.getOccuringFunctionNames().toString(), "{or=2, +=1, ==3}");
		Assert.assertEquals(tc.getOccuringSortNames().toString(), "{Int=4}");
		Assert.assertEquals(tc.getNumberOfQuantifiers(), 0);
	}

	@Test
	public void CheckOrOrAndTerm() {
		final Sort intSort = SmtSortUtils.getIntSort(mScript);

		final String names = "ABCDE";
		for (int i = 0; i < names.length(); ++i) {
			final Term term = declareVar(String.valueOf(names.charAt(i)), intSort);
		}

		final Term input = TermParseUtils.parseTerm(mScript, "(or (or (and (= A 0) (= D 1)) (= E 0)) (= C 0))");
		final LBool isSat = SmtUtils.checkSatTerm(mScript, input);
		final SMTFeatureExtractionTermClassifier tc = new SMTFeatureExtractionTermClassifier(mLogger);
		tc.checkTerm(input);

		mLogger.info("Original:               " + input.toStringDirect());
		mLogger.info("Original isSat:         " + isSat);
		mLogger.info("Original equiv classes: " + tc.getEquivalenceClasses());
		mLogger.info("Original #Vars:         " + tc.getNumberOfVariables());
		mLogger.info("Original Functions:         " + tc.getOccuringFunctionNames());
		mLogger.info("Original Sorts:         " + tc.getOccuringSortNames());
		mLogger.info("Original Quantifiers:         " + tc.getNumberOfQuantifiers());
		Assert.assertEquals(tc.getEquivalenceClasses().toString(),"{[C]=C, [E]=E, [A, D]=D}");
		Assert.assertEquals(tc.getNumberOfVariables(), 4);
		Assert.assertEquals(tc.getOccuringFunctionNames().toString(), "{or=2, and=1, ==4}");
		Assert.assertEquals(tc.getOccuringSortNames().toString(), "{Int=4}");
		Assert.assertEquals(tc.getNumberOfQuantifiers(), 0);
	}

	private Term declareVar(final String name, final Sort sort) {
		mScript.declareFun(name, new Sort[0], sort);
		return mScript.term(name);
	}
}
