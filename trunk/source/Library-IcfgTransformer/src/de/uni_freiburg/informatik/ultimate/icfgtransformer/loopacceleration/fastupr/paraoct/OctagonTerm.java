/*
 * Copyright (C) 2017 Jill Enke (enkei@informatik.uni-freiburg.de)
 * Copyright (C) 2017 University of Freiburg
 *
 * This file is part of the ULTIMATE IcfgTransformer library.
 *
 * The ULTIMATE IcfgTransformer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE IcfgTransformer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE IcfgTransformer library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE IcfgTransformer library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE IcfgTransformer grant you additional permission
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.icfgtransformer.loopacceleration.fastupr.paraoct;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtSortUtils;

/**
 *
 * @author Jill Enke (enkei@informatik.uni-freiburg.de)
 *
 */
public abstract class OctagonTerm {

	protected BigDecimal mConstant;

	public OctagonTerm(final BigDecimal constant) {
		mConstant = constant;
	}

	public abstract TermVariable getFirstVar();

	public abstract TermVariable getSecondVar();

	public Object getValue() {
		return mConstant;
	}

	public abstract boolean isFirstNegative();

	public abstract boolean isSecondNegative();

	protected abstract Term leftTerm(Script script);

	protected Term rightTerm(final Script script) {
		return (Rational.valueOf(mConstant.toBigInteger(), BigInteger.ONE)).toTerm(SmtSortUtils.getIntSort(script));
	}

	@Override
	public String toString() {
		return " <= " + mConstant.toString();
	}

	public Term toTerm(final Script script) {
		return script.term("<=", leftTerm(script), rightTerm(script));
	}
}
