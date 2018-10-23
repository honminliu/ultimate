package de.uni_freiburg.informatik.ultimate.reqtotest.graphtransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import de.uni_freiburg.informatik.ultimate.boogie.BoogieLocation;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssertStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssignmentStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Attribute;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Body;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.HavocStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IfStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.LeftHandSide;
import de.uni_freiburg.informatik.ultimate.boogie.ast.LoopInvariantSpecification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ModifiesSpecification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.NamedAttribute;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Unit;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VariableDeclaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VariableLHS;
import de.uni_freiburg.informatik.ultimate.boogie.ast.WhileStatement;
import de.uni_freiburg.informatik.ultimate.boogie.type.BoogieType;
import de.uni_freiburg.informatik.ultimate.core.model.models.IBoogieType;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Term2Expression;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TypeSortTranslator;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.reqtotest.req.ReqGuardGraph;
import de.uni_freiburg.informatik.ultimate.reqtotest.req.ReqSymbolTable;
import de.uni_freiburg.informatik.ultimate.reqtotest.req.TimedLabel;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;

public class GraphToBoogie {
	
	private static final Attribute[] EMPTY_ATTRIBUTES = new Attribute[0];
	
	private final ILogger mLogger;
	private final ReqSymbolTable mSymbolTable;
	
	private final BoogieLocation mDummyLocation;
	private final List<ReqGuardGraph> mRequirements;
	private final Unit mUnit;
	
	
	private final Map<ReqGuardGraph, String> mGraphToPc;
	private final Map<ReqGuardGraph, String> mGraphToPrimePc;
	
	private final Term2Expression mTerm2Expression;
	private final Script mScript;
	private final ManagedScript mManagedScript;
	private final ThreeValuedAuxVarGen mThreeValuedAuxVarGen;
	
	public static final String TEST_ORACLE_MARKER = "TEST_ORACLE_MARKER";
	 
	
	public GraphToBoogie(final ILogger logger, final IUltimateServiceProvider services,
			final IToolchainStorage storage,  ReqSymbolTable symbolTable, ThreeValuedAuxVarGen threeValuedAuxVarGen,
			List<ReqGuardGraph> requirements, Script scipt, ManagedScript managedScipt) {
		mLogger = logger;
		mSymbolTable = symbolTable;
		mDummyLocation = generateDummyLocation();
		mRequirements = requirements;
		
		mGraphToPc = new LinkedHashMap<>();
		mGraphToPrimePc = new LinkedHashMap<>();
		
		generatePcVars();

		mScript = scipt;
		mManagedScript = managedScipt;
		mThreeValuedAuxVarGen = threeValuedAuxVarGen;
		
		final HashRelation<Sort, IBoogieType> sortToType = new HashRelation<>();
		sortToType.addPair(mManagedScript.getScript().sort("Int"), BoogieType.TYPE_INT);
		sortToType.addPair(mManagedScript.getScript().sort("Real"), BoogieType.TYPE_REAL);
		sortToType.addPair(mManagedScript.getScript().sort("Bool"), BoogieType.TYPE_BOOL);
		mTerm2Expression = new Term2Expression(new TypeSortTranslator(sortToType, mScript,
				services), symbolTable, mManagedScript);
		
		final List<Declaration> decls = new ArrayList<>();
		decls.addAll(mSymbolTable.constructVariableDeclarations());
		decls.addAll(generateEncodingVarDeclaration());
		decls.add(getMainProcedure());
		mUnit = new Unit(mDummyLocation, decls.toArray(new Declaration[decls.size()]));
		
		
	}
	
	public Unit getAst() {
		return mUnit;
	}
	
	private Declaration getMainProcedure() {
		
		final ModifiesSpecification mod = new ModifiesSpecification(mDummyLocation, false, generateModifiesVariableList());
		final ModifiesSpecification[] modArray = new ModifiesSpecification[1];
		modArray[0] = mod;
		
		final Attribute[] attribute = new Attribute[0];
		final String[] typeParams = new String[0];
		final VarList[] inParams = new VarList[0];
		final VarList[] outParams = new VarList[0];
		
		final VariableDeclaration[] localVars = new VariableDeclaration[0];
		final Body body = new Body(mDummyLocation, localVars, generateProcedureBody());
		
		
		return new Procedure(mDummyLocation, attribute, "testGen", typeParams, inParams, outParams, modArray, body);
	}
	
	private Statement[] generateProcedureBody() {
		final List<Statement> statements = new ArrayList<>();
		statements.addAll(mSymbolTable.constructConstantAssignments());
		statements.addAll(generatePcInitialization());
		statements.addAll(generateClockInitialization());
		statements.add(generateWhileStatement());
		return statements.toArray(new Statement[statements.size()]);
	}
	
	private Statement generateWhileStatement() {
		return new WhileStatement(mDummyLocation, new BooleanLiteral(mDummyLocation, true), 
				new LoopInvariantSpecification[0], generateWhileBody());
	}
	
	private Statement[] generateWhileBody() {
		final List<Statement> statements = new ArrayList<>();
		statements.add(new HavocStatement(new BoogieLocation("", 0, 0, 1, 1), generateHavocVariableList()));
		for(ReqGuardGraph graph: mRequirements) {
			statements.addAll(graphToBoogie(graph));
		}
		statements.addAll(generateDefineUseAssumtions());
		statements.addAll(generateTestOracleAssertion());
		statements.addAll(generateNextLoopStateAssignment());
		statements.addAll(generateClockUpdates());
		return statements.toArray(new Statement[statements.size()]);
	}
	
	private List<Statement> graphToBoogie(ReqGuardGraph req) {

		final HashSet<ReqGuardGraph> visited = new HashSet<>();
		final Queue<ReqGuardGraph> queue = new LinkedList<>();
		queue.add(req);
		Statement[] elsePart = new Statement[0];
		while(queue.size() > 0) {
			ReqGuardGraph pivoth = queue.poll();
			visited.add(pivoth);
			Statement[] innerIf = new Statement[] {new AssumeStatement(mDummyLocation, new BooleanLiteral(mDummyLocation, false))};
			for(ReqGuardGraph successor: pivoth.getOutgoingNodes()) {
				if(!visited.contains(successor) && !queue.contains(successor)) {
					queue.add(successor);
				}
				TimedLabel label = pivoth.getOutgoingEdgeLabel(successor);
				innerIf = generateInnerIf(innerIf, req, successor, label);
			}
			elsePart = new Statement[] {(generateOuterIf(req, pivoth, innerIf, elsePart))};
		}
		//TODO this is ugly
		final List<Statement> statements = new ArrayList<>();
		statements.add(elsePart[0]);
		return statements;
	}
	
	private Statement generateOuterIf(ReqGuardGraph req, ReqGuardGraph pivoth, Statement[] body, Statement[] elsePart) {
		final Expression lhs = new IntegerLiteral(mDummyLocation, Integer.toString(pivoth.getLabel()));
		final Expression rhs = new IdentifierExpression(mDummyLocation, mGraphToPc.get(req));
		final BinaryExpression condition = new BinaryExpression(mDummyLocation, BinaryExpression.Operator.COMPEQ, lhs, rhs);
		return new IfStatement(mDummyLocation, condition, body, elsePart);
	}
	
	private Statement[] generateInnerIf(Statement[] innerIf, ReqGuardGraph graph, ReqGuardGraph successor, TimedLabel label) {
		Statement[] body;
		Statement setPcNextState = createVarIntAssignment(mGraphToPrimePc.get(graph), successor.getLabel());
		if (label.getReset() != null) {
			Statement resetClock = createVarIntAssignment(label.getReset().getName(),0);
			body = new Statement[] {resetClock, setPcNextState};
		} else {
			body = new Statement[] {setPcNextState};
		}
			IfStatement ifStatement = new IfStatement(mDummyLocation, 
					mTerm2Expression.translate(label.getGuard()), body, innerIf);	
		return new Statement[] {ifStatement};
	}
	
	private void generatePcVars() {
		int i = 0;
		for(ReqGuardGraph req: mRequirements) {
			mGraphToPc.put(req, "reqtotest_pc"+ Integer.toString(i));
			mGraphToPrimePc.put(req, "reqtotest_pc"+ Integer.toString(i) + "'");
			i++;
		}
	}
	
	private List<Declaration> generateEncodingVarDeclaration() {
		final List<Declaration> statements = new ArrayList<>();
		Collection<String> values = mGraphToPc.values();
		String[] idents = values.toArray(new String[values.size()]);
		VarList[] varList = new VarList[] { new VarList(mDummyLocation, idents,  BoogieType.TYPE_INT.toASTType(mDummyLocation)) };
		statements.add(new VariableDeclaration(mDummyLocation, EMPTY_ATTRIBUTES, varList));
		Collection<String> valuesPrimed = mGraphToPrimePc.values();
		String[] identsPrimed = valuesPrimed.toArray(new String[valuesPrimed.size()]);
		VarList[] varListPrimed = new VarList[] { new VarList(mDummyLocation, identsPrimed,  BoogieType.TYPE_INT.toASTType(mDummyLocation)) };
		statements.add(new VariableDeclaration(mDummyLocation, EMPTY_ATTRIBUTES, varListPrimed));
		//add encoding variable "delta"
		varList = new VarList[] { new VarList(mDummyLocation, new String[] {"delta"},  BoogieType.TYPE_INT.toASTType(mDummyLocation)) };
		statements.add(new VariableDeclaration(mDummyLocation, EMPTY_ATTRIBUTES, varList));
		return statements;
	}
	
	private List<Statement> generateNextLoopStateAssignment(){
		final List<Statement> statements = new ArrayList<>();
		for(ReqGuardGraph req: mRequirements) {
			statements.add(generateVarVarAssignment(mGraphToPc.get(req), mGraphToPrimePc.get(req)));
		}
		return statements;
	}
	
	private Statement generateVarVarAssignment(String asignee, String asignment) {
		final LeftHandSide[] lhs = new LeftHandSide[] {new VariableLHS(mDummyLocation, asignee)};
		final Expression[] rhs = new Expression[] {new IdentifierExpression(mDummyLocation, asignment)};
		return new AssignmentStatement(mDummyLocation,lhs,rhs );
	}
	
	private Statement createVarIntAssignment(String asignee, int value) {
		final LeftHandSide[] lhs = new LeftHandSide[] {new VariableLHS(mDummyLocation, asignee)};
		final Expression[] rhs = new Expression[] {new IntegerLiteral(mDummyLocation, Integer.toString(value))};
		return new AssignmentStatement(mDummyLocation,lhs,rhs );
	}
	
	private VariableLHS[] generateHavocVariableList(){
		final List<String> modifiedVarsList = new ArrayList<>();

		modifiedVarsList.addAll(mSymbolTable.getInputVars());
		modifiedVarsList.addAll(mSymbolTable.getHiddenVars());
		modifiedVarsList.addAll(mSymbolTable.getOutputVars());
		modifiedVarsList.addAll(mSymbolTable.getAuxVars());
		//modifiedVarsList.addAll(mGraphToPrimePc.values());
		
		final VariableLHS[] modifiedVars = new VariableLHS[modifiedVarsList.size()];
		for (int i = 0; i < modifiedVars.length; i++) {
			modifiedVars[i] = new VariableLHS(mDummyLocation, modifiedVarsList.get(i));
		}
		return modifiedVars;
	}
	
	private VariableLHS[] generateModifiesVariableList(){
		final List<String> modifiedVarsList = new ArrayList<>();

		modifiedVarsList.addAll(mSymbolTable.getInputVars());
		modifiedVarsList.addAll(mSymbolTable.getHiddenVars());
		modifiedVarsList.addAll(mSymbolTable.getOutputVars());
		modifiedVarsList.addAll(mSymbolTable.getConstVars());
		modifiedVarsList.addAll(mSymbolTable.getAuxVars());
		modifiedVarsList.addAll(mSymbolTable.getClockVars());
		modifiedVarsList.addAll(mGraphToPrimePc.values());
		modifiedVarsList.addAll(mGraphToPc.values());
		modifiedVarsList.add("delta");
		
		final VariableLHS[] modifiedVars = new VariableLHS[modifiedVarsList.size()];
		for (int i = 0; i < modifiedVars.length; i++) {
			modifiedVars[i] = new VariableLHS(mDummyLocation, modifiedVarsList.get(i));
		}
		return modifiedVars;
	}
	
	private List<Statement> generatePcInitialization() {
		final List<Statement> statements = new ArrayList<>();
		for(ReqGuardGraph req: mRequirements) {
			statements.add(createVarIntAssignment(mGraphToPc.get(req), req.getLabel()));
		}
		return statements;
	}
	
	private List<Statement> generateClockInitialization() {
		final List<Statement> statements = new ArrayList<>();
		for(String clock: mSymbolTable.getClockVars()) {
			statements.add(createVarIntAssignment(clock, 0));
		}
		statements.add(createVarIntAssignment("delta", 0));
		return statements;
	}
	
	
	private static BoogieLocation generateDummyLocation() {
		return new BoogieLocation("", 1, 1, 1, 1);
	}
	
	private List<Statement> generateDefineUseAssumtions(){
		final List<Statement> defineUseAssumtptions = new ArrayList<>();
		final List<Term> guards = mThreeValuedAuxVarGen.getDefineAssumeGuards();
		for(Term guard: guards) {
			Statement assume = new AssumeStatement(mDummyLocation, mTerm2Expression.translate(guard));
			defineUseAssumtptions.add(assume);
		}
		return defineUseAssumtptions;
	}
	
	private List<Statement> generateTestOracleAssertion(){
		final List<Statement> oracles = new ArrayList<>();	
		final List<Term> guards = mThreeValuedAuxVarGen.getOracleGuards();
		
		// dummyassertion to catch states at this point in the program
		NamedAttribute attribute = new NamedAttribute(mDummyLocation, TEST_ORACLE_MARKER , new Expression[0] );
		Statement assume = new AssertStatement(mDummyLocation, new NamedAttribute[] {attribute}, new BooleanLiteral(mDummyLocation, true));	
		oracles.add(assume);
		
		for(Term guard: guards) {
			assume = new AssertStatement(mDummyLocation, new NamedAttribute[0], mTerm2Expression.translate(guard));
			oracles.add(assume);
		}
		return oracles;
	}
	
	private List<Statement> generateClockUpdates(){
		List<Statement> stmts = new ArrayList<>();
		stmts.add(new HavocStatement(mDummyLocation, 
				new VariableLHS[] {new VariableLHS(mDummyLocation, "delta")} ));
		stmts.add(new AssumeStatement(mDummyLocation,
				new BinaryExpression(mDummyLocation, BinaryExpression.Operator.COMPGT,
						new IdentifierExpression(mDummyLocation, "delta"), new IntegerLiteral(mDummyLocation, "0"))
				));
		for(String clockVar: mSymbolTable.getClockVars()) {
			stmts.add(new AssignmentStatement(mDummyLocation, new VariableLHS[] {new VariableLHS(mDummyLocation, clockVar)},
					new Expression[] {new BinaryExpression(mDummyLocation, BinaryExpression.Operator.ARITHPLUS,
							new IdentifierExpression(mDummyLocation, clockVar),
							new IdentifierExpression(mDummyLocation, "delta")
					)}));
			}
		return stmts;
	}

}



























