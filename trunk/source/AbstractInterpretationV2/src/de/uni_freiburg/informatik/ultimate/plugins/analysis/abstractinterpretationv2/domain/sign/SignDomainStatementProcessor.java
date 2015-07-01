package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.sign;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVisitor;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssertStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssignmentStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.HavocStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.LeftHandSide;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.RealLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VariableLHS;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.ExpressionEvaluator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.IEvaluationResult;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.IEvaluator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.IEvaluatorFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.INAryEvaluator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.sign.SignDomainValue.Values;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;

/**
 * Processes Boogie {@link Statement}s and returns a new {@link SignDomainState}
 * for the given Statement.
 * 
 * @author greitsch@informatik.uni-freiburg.de
 *
 */
public class SignDomainStatementProcessor extends BoogieVisitor {

	private SignDomainState<?, ?> mOldState;
	private SignDomainState<CodeBlock, BoogieVar> mNewState;

	SignEvaluatorFactory mEvaluatorFactory;
	ExpressionEvaluator<Values, CodeBlock, BoogieVar> mExpressionEvaluator;

	private String mLhsVariable;

	protected SignDomainStatementProcessor(SignStateConverter<CodeBlock, BoogieVar> stateConverter) {
		mEvaluatorFactory = new SignEvaluatorFactory(stateConverter);
	}

	protected SignDomainState<CodeBlock, BoogieVar> process(SignDomainState<CodeBlock, BoogieVar> oldState,
	        Statement statement) {
		mOldState = oldState;
		mNewState = (SignDomainState<CodeBlock, BoogieVar>) oldState.copy();

		mLhsVariable = null;

		// Process the current statement and alter mNewState
		processStatement(statement);

		return mNewState;
	}

	@Override
	protected void visit(HavocStatement statement) {

		final VariableLHS[] vars = statement.getIdentifiers();
		for (final VariableLHS var : vars) {
			mNewState.setValue(var.getIdentifier(), new SignDomainValue(Values.TOP));
		}

		super.visit(statement);
	}

	@Override
	protected void visit(AssignmentStatement statement) {
		mExpressionEvaluator = new ExpressionEvaluator<Values, CodeBlock, BoogieVar>();

		// super.visit(statement);

		final LeftHandSide[] lhs = statement.getLhs();
		final Expression[] rhs = statement.getRhs();

		for (int i = 0; i < lhs.length; i++) {
			assert mLhsVariable == null;
			processLeftHandSide(lhs[i]);
			assert mLhsVariable != null;
			final String varname = mLhsVariable;
			mLhsVariable = null;

			processExpression(rhs[i]);
			assert mExpressionEvaluator.isFinished();
			final IEvaluationResult<?> result = mExpressionEvaluator.getRootEvaluator().evaluate(mOldState);

			if (!result.getType().equals(Values.class)) {
				throw new UnsupportedOperationException(
				        "The type of the assignment left hand side evaluation result is not allowed.");
			}

			final SignDomainValue newValue = new SignDomainValue((Values) result.getResult());
			mNewState.setValue(varname, newValue);
		}
	}

	@Override
	protected void visit(AssumeStatement statement) {

		mExpressionEvaluator = new ExpressionEvaluator<Values, CodeBlock, BoogieVar>();

		Expression formula = statement.getFormula();

		if (formula instanceof BooleanLiteral) {
			BooleanLiteral binform = (BooleanLiteral) formula;
			if (!binform.getValue()) {
				mNewState.setToBottom();
			}
			return;
		}

		processExpression(formula);

		System.out.println(mExpressionEvaluator.isFinished() ? "FINISHED" : "UNFINISHED");
		IEvaluationResult<?> result = mExpressionEvaluator.getRootEvaluator().evaluate(mOldState);

	}

	@Override
	protected void visit(AssertStatement statement) {
		// TODO Auto-generated method stub
		super.visit(statement);
	}

	@Override
	protected void visit(BinaryExpression expr) {
		SignBinaryExpressionEvaluator binaryEvaluator = null;
		SignLogicalExpressionEvaluator logicalEvaluator = null;
		INAryEvaluator<?, ?, ?> evaluator;

		switch (expr.getOperator()) {
		case COMPEQ:
			evaluator = (SignLogicalExpressionEvaluator) mEvaluatorFactory.createLogicalBinaryExpressionEvaluator();
			break;
		case ARITHPLUS:
			evaluator = (SignBinaryExpressionEvaluator) mEvaluatorFactory.createNAryExpressionEvaluator(2);
			break;
		default:
			throw new UnsupportedOperationException("The operator " + expr.getOperator().toString()
			        + " is not implemented.");
		}

		// SignBinaryExpressionEvaluator binaryExpressionEvaluator =
		// (SignBinaryExpressionEvaluator) mEvaluatorFactory
		// .createNAryExpressionEvaluator(2);

		// binaryExpressionEvaluator.setOperator(expr.getOperator());

		evaluator.setOperator(expr.getOperator());

		mExpressionEvaluator.addEvaluator(evaluator);

		super.visit(expr);
	}

	@Override
	protected void visit(RealLiteral expr) {
		IEvaluator<Values, CodeBlock, BoogieVar> integerExpressionEvaluator = mEvaluatorFactory
		        .createSingletonValueExpressionEvaluator(expr.getValue(), BigDecimal.class);

		mExpressionEvaluator.addEvaluator(integerExpressionEvaluator);
	}

	@Override
	protected void visit(IntegerLiteral expr) {

		IEvaluator<Values, CodeBlock, BoogieVar> integerExpressionEvaluator = mEvaluatorFactory
		        .createSingletonValueExpressionEvaluator(expr.getValue(), BigInteger.class);

		mExpressionEvaluator.addEvaluator(integerExpressionEvaluator);
	}

	@Override
	protected void visit(UnaryExpression expr) {

		SignUnaryExpressionEvaluator unaryExpressionEvaluator = (SignUnaryExpressionEvaluator) mEvaluatorFactory
		        .createNAryExpressionEvaluator(1);

		unaryExpressionEvaluator.setOperator(expr.getOperator());

		mExpressionEvaluator.addEvaluator(unaryExpressionEvaluator);

		super.visit(expr);
	}

	@Override
	protected void visit(IdentifierExpression expr) {

		final IEvaluator<Values, CodeBlock, BoogieVar> variableExpressionEvaluator = mEvaluatorFactory
		        .createSingletonVariableExpressionEvaluator(expr.getIdentifier());

		mExpressionEvaluator.addEvaluator(variableExpressionEvaluator);

		super.visit(expr);
	}

	@Override
	protected void visit(VariableLHS lhs) {
		mLhsVariable = lhs.getIdentifier();
	}

}
