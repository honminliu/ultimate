package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.relational.octagon;

import static de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.relational.octagon.BoolValue.*;

import org.junit.Test;
import org.junit.Assert;

import java.util.function.BiFunction;

public class BoolValueTest {

	private final static BoolValue[] values = {BOT, FALSE, TRUE, TOP};
	
	@Test
	public void testJoin() {
		BoolValue[][] expected = {
			{BOT,   FALSE, TRUE, TOP},
			{FALSE, FALSE, TOP,  TOP},
			{TRUE,  TOP,   TRUE, TOP},
			{TOP,   TOP,   TOP,  TOP},
		};
		testBinaryOperation(expected, BoolValue::union);
	}

	@Test
	public void testMeet() {
		BoolValue[][] expected = {
			{BOT, BOT,   BOT,  BOT},
			{BOT, FALSE, BOT,  FALSE},
			{BOT, BOT,   TRUE, TRUE},
			{BOT, FALSE, TRUE, TOP},
		};
		testBinaryOperation(expected, BoolValue::intersect);
	}
	
	@Test
	public void testAnd() {
		BoolValue[][] expected = {
			{BOT, BOT,   BOT,   BOT},
			{BOT, FALSE, FALSE, FALSE},
			{BOT, FALSE, TRUE,  TOP},
			{BOT, FALSE, TOP,   TOP},
		};
		testBinaryOperation(expected, BoolValue::and);
	}
	
	@Test
	public void testOr() {
		BoolValue[][] expected = {
			{BOT, BOT,   BOT,  BOT},
			{BOT, FALSE, TRUE, TOP},
			{BOT, TRUE,  TRUE, TRUE},
			{BOT, TOP,   TRUE, TOP},
		};
		testBinaryOperation(expected, BoolValue::or);
	}
	
	@Test
	public void testNot() {
		Assert.assertEquals(BOT,   BOT.not());
		Assert.assertEquals(TRUE,  FALSE.not());
		Assert.assertEquals(FALSE, TRUE.not());
		Assert.assertEquals(TOP,   TOP.not());
	}
	
	private void testBinaryOperation(BoolValue[][] expected, BiFunction<BoolValue, BoolValue, BoolValue> op) {
		for (int i = 0; i < values.length; ++i) {
			for (int j = 0; j < values.length; ++j) {
				String msg = values[i] +  " o " + values[j];
				Assert.assertEquals(msg, expected[i][j], op.apply(values[i], values[j]));
			}
		}
	}
	
}
