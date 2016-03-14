/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE TraceAbstraction plug-in.
 * 
 * The ULTIMATE TraceAbstraction plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE TraceAbstraction plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstraction plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstraction plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE TraceAbstraction plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import de.uni_freiburg.informatik.ultimate.automata.InCaReCounter;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.benchmark.IStatisticsDataProvider;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.benchmark.IStatisticsType;
import de.uni_freiburg.informatik.ultimate.util.Benchmark;

public class HoareTripleCheckerBenchmarkGenerator implements IStatisticsDataProvider {
	
	protected final InCaReCounter m_SDtfsCounter;
	protected final InCaReCounter m_SDsluCounter;
	protected final InCaReCounter m_SDsCounter;
	protected final InCaReCounter m_SdLazyCounter;
	protected final InCaReCounter m_SolverCounterSat;
	protected final InCaReCounter m_SolverCounterUnsat;
	protected final InCaReCounter m_SolverCounterUnknown;
	protected final InCaReCounter m_SolverCounterNotChecked;
	protected final Benchmark m_Benchmark;

	protected boolean m_Running = false;

	public HoareTripleCheckerBenchmarkGenerator() {
		m_SDtfsCounter = new InCaReCounter();
		m_SDsluCounter = new InCaReCounter();
		m_SDsCounter = new InCaReCounter();
		m_SdLazyCounter = new InCaReCounter();
		m_SolverCounterSat = new InCaReCounter();
		m_SolverCounterUnsat = new InCaReCounter();
		m_SolverCounterUnknown = new InCaReCounter();
		m_SolverCounterNotChecked= new InCaReCounter();
		m_Benchmark = new Benchmark();
		m_Benchmark.register(HoareTripleCheckerBenchmarkType.s_EdgeCheckerTime);
	}

	public InCaReCounter getSDtfsCounter() {
		return m_SDtfsCounter;
	}
	public InCaReCounter getSDsluCounter() {
		return m_SDsluCounter;
	}
	public InCaReCounter getSDsCounter() {
		return m_SDsCounter;
	}
	public InCaReCounter getSdLazyCounter() {
		return m_SdLazyCounter;
	}
	public InCaReCounter getSolverCounterSat() {
		return m_SolverCounterSat;
	}
	public InCaReCounter getSolverCounterUnsat() {
		return m_SolverCounterUnsat;
	}
	public InCaReCounter getSolverCounterUnknown() {
		return m_SolverCounterUnknown;
	}
	public InCaReCounter getSolverCounterNotChecked() {
		return m_SolverCounterNotChecked;
	}
	public long getEdgeCheckerTime() {
		return (long) m_Benchmark.getElapsedTime(HoareTripleCheckerBenchmarkType.s_EdgeCheckerTime, TimeUnit.NANOSECONDS);
	}
	public void continueEdgeCheckerTime() {
		assert m_Running == false : "Timing already running";
		m_Running = true;
		m_Benchmark.unpause(HoareTripleCheckerBenchmarkType.s_EdgeCheckerTime);
	}
	public void stopEdgeCheckerTime() {
		assert m_Running == true : "Timing not running";
		m_Running = false;
		m_Benchmark.pause(HoareTripleCheckerBenchmarkType.s_EdgeCheckerTime);
	}
	@Override
	public Collection<String> getKeys() {
		return HoareTripleCheckerBenchmarkType.getInstance().getKeys();
	}
	public Object getValue(String key) {
		switch (key) {
		case HoareTripleCheckerBenchmarkType.s_SDtfs:
			return m_SDtfsCounter;
		case HoareTripleCheckerBenchmarkType.s_SDslu:
			return m_SDsluCounter;
		case HoareTripleCheckerBenchmarkType.s_SDs:
			return m_SDsCounter;
		case HoareTripleCheckerBenchmarkType.s_SdLazyCounter:
			return m_SdLazyCounter;
		case HoareTripleCheckerBenchmarkType.s_SolverCounterSat: 
			return m_SolverCounterSat;
		case HoareTripleCheckerBenchmarkType.s_SolverCounterUnsat:
			return m_SolverCounterUnsat;
		case HoareTripleCheckerBenchmarkType.s_SolverCounterUnknown:
			return m_SolverCounterUnknown;
		case HoareTripleCheckerBenchmarkType.s_SolverCounterNotchecked:
			return m_SolverCounterNotChecked;
		case HoareTripleCheckerBenchmarkType.s_EdgeCheckerTime:
			return getEdgeCheckerTime();
		default:
			throw new AssertionError("unknown key");
		}
	}

	@Override
	public IStatisticsType getBenchmarkType() {
		return HoareTripleCheckerBenchmarkType.getInstance();
	}

}
