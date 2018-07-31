package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.mapelimination;

import java.util.Map;

import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IcfgUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfg;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgEdgeIterator;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;

/**
 * @author Luca Bruder (luca.bruder@gmx.de)
 * @author Lisa Kleinlein (lisa.kleinlein@web.de)
 */
public class MonniauxMapEliminator {

	public MonniauxMapEliminator(final IIcfg<?> icfg) {
		final IIcfg<?> micfg = icfg;
		final IcfgEdgeIterator iter = new IcfgEdgeIterator(micfg);

		while (iter.hasNext()) {
			final IIcfgTransition<?> transition = iter.next();
			final TransFormula tf = IcfgUtils.getTransformula(transition);
			int step = 0;

			/*
			 * String expr = "ABCD"; String test = "(sfksanohoa (select x y))"; for (String expr1 : test.split(" ")) {
			 * expr = expr1; expr = expr.substring(1); break; }
			 */

			String transformula = tf.toString();
			String expr = null;
			String x = null;
			String y = null;
			int index = 0;

			for (final String expr1 : transformula.split(" (select * *)")) {

				for (int i = expr1.length() - 1; i >= 0; i--) {
					index = expr1.length();
					final char c = expr1.charAt(i);
					if (c == '(') {
						expr = expr1.substring(i + 1);
						i = 0;
					}
				}

				for (int i = index + 1; i < transformula.length(); i++) {
					final char c = transformula.charAt(i);
					int index_left = 0;
					boolean left_found = false;
					boolean x_found = false;
					int index_right = 0;
					if (c == ' ') {
						index_left = i + 1;
						left_found = true;
					}
					if (c == ' ' && left_found) {
						index_right = i - 1;
						x = transformula.substring(index_left, index_right);
						x_found = true;
					}
					if (c == ')' && x_found) {
						y = transformula.substring(index_right + 1, i - 1);
						i = transformula.length() + 1;
					}

				}

				final String sub_transformula = "(and (=> (= y i_step) (= a_step_i x_i)) (expr a_step_i)";
				sub_transformula.replaceAll("y", y);
				sub_transformula.replaceAll("x", x);
				sub_transformula.replaceAll("expr", expr);
				sub_transformula.replaceAll("step", Integer.toString(step));

				// TermVariable t = new TermVariable("f_step", sort, null);

				final Map<IProgramVar, TermVariable> inV = tf.getInVars();
				inV.remove(x);
				transformula = transformula.replaceAll("(* (select x y))", sub_transformula);
				final Map<IProgramVar, TermVariable> outV = tf.getOutVars();
				outV.remove(x);
				// outV.merge(null, , null);
				step++;

			}

			/*
			 * for (true) { //todo }
			 * 
			 * TransFormula(inVars, outVars, auxVars, nonTheoryConst) newTF;
			 */
		}

	}

}
