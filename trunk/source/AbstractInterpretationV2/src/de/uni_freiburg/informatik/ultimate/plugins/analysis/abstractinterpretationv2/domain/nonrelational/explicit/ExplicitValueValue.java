/*
 * Copyright (C) 2018 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2018 University of Freiburg
 *
 * This file is part of the ULTIMATE AbstractInterpretationV2 plug-in.
 *
 * The ULTIMATE AbstractInterpretationV2 plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE AbstractInterpretationV2 plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AbstractInterpretationV2 plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AbstractInterpretationV2 plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE AbstractInterpretationV2 plug-in grant you additional permission
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.explicit;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.BooleanValue;

/**
 * Representation of an explicit value in the {@link ExplicitValueDomain}
 *
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class ExplicitValueValue extends BaseExplicitValueValue {

	private final Rational mValue;

	public ExplicitValueValue(final Rational value) {
		assert value != null;
		mValue = value;
	}

	@Override
	public BaseExplicitValueValue copy() {
		return this;
	}

	@Override
	public boolean isBottom() {
		return false;
	}

	@Override
	public boolean isTop() {
		return false;
	}

	@Override
	public BaseExplicitValueValue intersect(final BaseExplicitValueValue other) {
		if (other instanceof ExplicitValueValue) {
			final ExplicitValueValue evv = (ExplicitValueValue) other;
			if (evv.mValue.equals(mValue)) {
				return this;
			}
			return ExplicitValueBottom.DEFAULT;
		}
		return other.intersect(this);
	}

	@Override
	public BaseExplicitValueValue merge(final BaseExplicitValueValue other) {
		if (other instanceof ExplicitValueValue) {
			final ExplicitValueValue evv = (ExplicitValueValue) other;
			if (evv.mValue.equals(mValue)) {
				return this;
			}
			return ExplicitValueTop.DEFAULT;
		}
		return other.intersect(this);
	}

	@Override
	public Collection<BaseExplicitValueValue> complement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<BaseExplicitValueValue> complementInteger() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEqualTo(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isContainedIn(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public BaseExplicitValueValue add(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue subtract(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue multiply(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue negate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue divideInteger(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue divide(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue modulo(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue greaterThan(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BooleanValue compareEquality(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BooleanValue compareInequality(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BooleanValue isGreaterThan(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue greaterOrEqual(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BooleanValue isGreaterOrEqual(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue lessThan(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BooleanValue isLessThan(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue lessOrEqual(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BooleanValue isLessOrEqual(final BaseExplicitValueValue other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue inverseModulo(final BaseExplicitValueValue referenceValue,
			final BaseExplicitValueValue oldValue, final boolean isLeft) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue inverseEquality(final BaseExplicitValueValue oldValue,
			final BaseExplicitValueValue referenceValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue inverseLessOrEqual(final BaseExplicitValueValue oldValue, final boolean isLeft) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue inverseLessThan(final BaseExplicitValueValue oldValue, final boolean isLeft) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue inverseGreaterOrEqual(final BaseExplicitValueValue oldValue, final boolean isLeft) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue inverseGreaterThan(final BaseExplicitValueValue oldValue, final boolean isLeft) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseExplicitValueValue inverseNotEqual(final BaseExplicitValueValue oldValue,
			final BaseExplicitValueValue referenceValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Term getTerm(final Script script, final Sort sort, final Term variable) {
		// TODO Auto-generated method stub
		return null;
	}

}
