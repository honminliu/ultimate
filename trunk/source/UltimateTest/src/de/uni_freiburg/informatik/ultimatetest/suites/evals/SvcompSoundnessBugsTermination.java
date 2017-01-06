/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 *
 * This file is part of the ULTIMATE Test Library.
 *
 * The ULTIMATE Test Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE Test Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Test Library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Test Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Test Library grant you additional permission
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimatetest.suites.evals;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.CegarLoopStatisticsDefinitions;
import de.uni_freiburg.informatik.ultimate.test.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimate.test.UltimateTestCase;
import de.uni_freiburg.informatik.ultimate.test.decider.ITestResultDecider;
import de.uni_freiburg.informatik.ultimate.test.decider.TerminationAnalysisTestResultDecider;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Triple;
import de.uni_freiburg.informatik.ultimatetest.suites.AbstractEvalTestSuite;
import de.uni_freiburg.informatik.ultimatetest.summaries.ColumnDefinition;
import de.uni_freiburg.informatik.ultimatetest.summaries.ColumnDefinition.Aggregate;
import de.uni_freiburg.informatik.ultimatetest.summaries.ConversionContext;

/**
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 */
public class SvcompSoundnessBugsTermination extends AbstractEvalTestSuite {

	@SuppressWarnings("unchecked")
	private static final Triple<String, String, String>[] INPUTS = new Triple[] {
			new Triple<>("BuchiAutomizerCInline.xml",
					"svcomp2017/automizer/svcomp-Termination-64bit-Automizer_Default.epf",
					"examples/svcomp/termination-memory-linkedlists/cll_search-alloca_true-termination.c.i"),
			new Triple<>("BuchiAutomizerCInline.xml",
					"svcomp2017/automizer/svcomp-Termination-64bit-Automizer_Default.epf",
					"examples/svcomp/termination-memory-linkedlists/nondet_ll_search-alloca_true-termination.c.i"), };

	@Override
	protected ITestResultDecider constructITestResultDecider(final UltimateRunDefinition ultimateRunDefinition) {
		return new TerminationAnalysisTestResultDecider(ultimateRunDefinition, false);
	}

	@Override
	protected long getTimeout() {
		return 90 * 1000;
	}

	@Override
	public Collection<UltimateTestCase> createTestCases() {
		for (final Triple<String, String, String> triple : INPUTS) {
			addTestCase(triple.getFirst(), triple.getSecond(), triple.getThird());
		}
		return super.createTestCases();
	}

	@Override
	protected ColumnDefinition[] getColumnDefinitions() {
		return new ColumnDefinition[] {
				new ColumnDefinition("Runtime (ns)", "Avg. runtime", ConversionContext.Divide(1000000000, 2, " s"),
						Aggregate.Sum, Aggregate.Average),
				new ColumnDefinition("Allocated memory end (bytes)", "Memory",
						ConversionContext.Divide(1048576, 2, " MB"), Aggregate.Max, Aggregate.Average),
				new ColumnDefinition(CegarLoopStatisticsDefinitions.OverallIterations.toString(), "Iter{-}ations",
						ConversionContext.BestFitNumber(), Aggregate.Ignore, Aggregate.Average),
				new ColumnDefinition(CegarLoopStatisticsDefinitions.OverallTime.toString(), "Trace Abstraction Time",
						ConversionContext.Divide(1000000000, 2, " s"), Aggregate.Sum, Aggregate.Average), };
	}
}
