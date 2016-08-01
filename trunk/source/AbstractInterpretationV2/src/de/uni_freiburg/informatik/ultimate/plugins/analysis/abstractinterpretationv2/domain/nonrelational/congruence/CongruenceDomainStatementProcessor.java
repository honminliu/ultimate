/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Marius Greitschus (greitsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
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

package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.congruence;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.uni_freiburg.informatik.ultimate.boogie.ast.ArrayAccessExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ArrayStoreExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.boogie.symboltable.BoogieSymbolTable;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SmtSymbolTable;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.NonrelationalStatementProcessor;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.evaluator.EvaluatorFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.evaluator.ExpressionEvaluator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.evaluator.IEvaluatorFactory;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;

/**
 * Processes Boogie {@link Statement}s and returns a new {@link CongruenceDomainState} for the given statement.
 * 
 * @author Frank Schüssele (schuessf@informatik.uni-freiburg.de)
 * @author Marius Greitschus (greitsch@informatik.uni-freiburg.de)
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class CongruenceDomainStatementProcessor
        extends NonrelationalStatementProcessor<CongruenceDomainState, CongruenceDomainValue> {

	protected CongruenceDomainStatementProcessor(final ILogger logger, final BoogieSymbolTable symbolTable,
	        final Boogie2SmtSymbolTable bpl2smtTable, final int maxParallelStates) {
		super(logger, symbolTable, bpl2smtTable, maxParallelStates);
	}

	@Override
	protected IEvaluatorFactory<CongruenceDomainValue, CongruenceDomainState, CodeBlock> createEvaluatorFactory(
	        int maxParallelStates) {
		final EvaluatorFactory.Function<String, CongruenceDomainValue> singletonValueExpressionEvaluatorCreator = (
		        value, type) -> {
			assert value != null;
			assert type != null;
			if (type == BigInteger.class) {
				return CongruenceDomainValue.createConstant(new BigInteger(value));
			} else {
				assert type == BigDecimal.class;
				return CongruenceDomainValue.createTop();
			}
		};
		return new EvaluatorFactory<>(getLogger(), maxParallelStates, new CongruenceValueFactory(),
		        singletonValueExpressionEvaluatorCreator);
	}

	@Override
	protected void addEvaluators(ExpressionEvaluator<CongruenceDomainValue, CongruenceDomainState, CodeBlock> evaluator,
	        IEvaluatorFactory<CongruenceDomainValue, CongruenceDomainState, CodeBlock> evaluatorFactory,
	        Expression expr) {
		super.addEvaluators(evaluator, evaluatorFactory, expr);
		if (expr instanceof ArrayStoreExpression) {
			evaluator.addEvaluator(evaluatorFactory.createSingletonValueTopEvaluator());
		} else if (expr instanceof ArrayAccessExpression) {
			evaluator.addEvaluator(evaluatorFactory.createSingletonValueTopEvaluator());
		}
	}

	@Override
	protected Expression normalizeExpression(final Expression expr) {
		return ExpressionTransformer.transform(expr);
	}
}
