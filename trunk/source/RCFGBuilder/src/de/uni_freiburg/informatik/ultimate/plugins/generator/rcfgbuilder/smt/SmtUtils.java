package de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.smt;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.core.api.UltimateServices;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.simplification.SimplifyDDA;
import de.uni_freiburg.informatik.ultimate.logic.util.DebugMessage;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.Activator;

public class SmtUtils {
	
	private static Logger s_Logger = 
			UltimateServices.getInstance().getLogger(Activator.PLUGIN_ID);
	
	public static Term simplify(Script script, Term formula) {
		s_Logger.debug(new DebugMessage(
				"simplifying formula of DAG size {0}", 
				new DagSizePrinter(formula)));
		Term simplified = (new SimplifyDDA(script)).getSimplifiedTerm(formula);
		s_Logger.debug(new DebugMessage(
				"DAG size before simplification {0}, DAG size after simplification {1}", 
				new DagSizePrinter(formula), new DagSizePrinter(simplified)));
		return simplified;
	}
	
	/**
	 * Takes an ApplicationTerm with pairwise function symbol and return 
	 * a conjunction of pairwise applications of the function symbol.
	 * E.g. the ternary equality (= a b c) becomes
	 * (and (= a b) (= a c) (= b c)).
	 */
	public static Term binarize(Script script, ApplicationTerm term) {
		FunctionSymbol functionSymbol = term.getFunction();
		if (!functionSymbol.isPairwise()) {
			throw new IllegalArgumentException("can only binarize pairwise terms");
		}
		String functionName = functionSymbol.getApplicationString();
		Term[] params = term.getParameters();
		assert params.length > 1;
		List<Term> conjuncts = new ArrayList<Term>();
		for (int i=0; i<params.length; i++) {
			for (int j=i+1; j<params.length; j++) {
				conjuncts.add(script.term(functionName, params[i], params[j]));
			}
		}
		return script.term("and", conjuncts.toArray(new Term[0]));
	}
	
	public static boolean hasBooleanParams(ApplicationTerm term) {
		Term[] params = term.getParameters();
		boolean result = params[0].getSort().getName().equals("Bool");
		return result;
	}
}
