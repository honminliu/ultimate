/*
 * Copyright (C) 2013-2015 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Markus Lindenmann (lindenmm@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Oleksii Saukh (saukho@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Stefan Wissert
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE CACSL2BoogieTranslator plug-in.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CACSL2BoogieTranslator plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CACSL2BoogieTranslator plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE CACSL2BoogieTranslator plug-in grant you additional permission 
 * to convey the resulting work.
 */
/**
 * An example for a ACSL handler implementation.
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.LocationFactory;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.ExpressionTranslation.AExpressionTranslation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.MemoryHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.StructHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.InferredType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.SymbolTableValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.PRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CStruct;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.IncorrectSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.UnsupportedSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ContractResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ExpressionResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.HeapLValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.LRValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.LocalLValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.Result;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.BoogieASTUtil;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.Dispatcher;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.IACSLHandler;
import de.uni_freiburg.informatik.ultimate.model.IType;
import de.uni_freiburg.informatik.ultimate.model.acsl.ACSLNode;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.ACSLResultExpression;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.ArrayAccessExpression;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.Assertion;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.Assigns;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.CodeAnnot;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.CodeAnnotStmt;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.Contract;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.ContractStatement;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.Ensures;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.FieldAccessExpression;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.FreeableExpression;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.LoopAnnot;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.LoopAssigns;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.LoopInvariant;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.LoopStatement;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.LoopVariant;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.MallocableExpression;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.RealLiteral;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.Requires;
import de.uni_freiburg.informatik.ultimate.model.acsl.ast.ValidExpression;
import de.uni_freiburg.informatik.ultimate.model.annotation.IAnnotations;
import de.uni_freiburg.informatik.ultimate.model.annotation.Overapprox;
import de.uni_freiburg.informatik.ultimate.model.boogie.ExpressionFactory;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ArrayType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssertStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BinaryExpression.Operator;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.EnsuresSpecification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.HavocStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.LoopInvariantSpecification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ModifiesSpecification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.RequiresSpecification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Specification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.StructAccessExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.StructLHS;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VariableDeclaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VariableLHS;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.result.Check;

/**
 * @author Markus Lindenmann
 * @author Oleksii Saukh
 * @author Stefan Wissert
 * @date 28.02.2012
 */
public class ACSLHandler implements IACSLHandler {
	
    /**
     * To determine the right names, we need to know where we are in the
     * specification.
     */

    private enum SPEC_TYPE {
        /**
         * Not specified.
         */
        NOT,
        /**
         * ACSL requires statement.
         */
        REQUIRES,
        /**
         * ACSL assigns statement.
         */
        ASSIGNS,
        /**
         * ACSL ensures statement.
         */
        ENSURES
    }

    /**
     * Holds the spec type, which we need later in the code.
     */
    private ACSLHandler.SPEC_TYPE specType = ACSLHandler.SPEC_TYPE.NOT;
    
    

	/**
     * @deprecated is not supported in this handler! Do not use!
     */
    @Override
    public Result visit(Dispatcher main, IASTNode node) {
        throw new UnsupportedOperationException(
                "Implementation Error: Use CHandler for: " + node.getClass());
    }

    @Override
    public Result visit(Dispatcher main, ACSLNode node) {
        String msg = "ACSLHandler: Not yet implemented: " + node.toString();
        ILocation loc = LocationFactory.createACSLLocation(node);
        throw new UnsupportedSyntaxException(loc, msg);
    }

    @Override
    public Result visit(Dispatcher main, CodeAnnot node) {
        if (node instanceof CodeAnnotStmt) {
            /*
            Result formula = main.dispatch(((Assertion) ((CodeAnnotStmt) node)
                    .getCodeStmt()).getFormula());
            Check check = new Check(Check.Spec.ASSERT);
            AssertStatement assertStmt = new AssertStatement(
                    LocationFactory.createACSLLocation(node, check),
                    ((Expression) formula.node));
            check.addToNodeAnnot(assertStmt);
            return new Result(assertStmt);
            */
            Check check = new Check(Check.Spec.ASSERT);
            ILocation loc = LocationFactory.createACSLLocation(node, check);
            ArrayList<Declaration> decl = new ArrayList<Declaration>();
            ArrayList<Statement> stmt = new ArrayList<Statement>();
            List<Overapprox> overappr = new ArrayList<Overapprox>();
            Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();

            ExpressionResult formula = (ExpressionResult) main.dispatch(((Assertion) ((CodeAnnotStmt) node).getCodeStmt()).getFormula());

           formula = formula.switchToRValueIfNecessary(main, ((CHandler) main.cHandler).mMemoryHandler, ((CHandler) main.cHandler).mStructHandler, loc);
           
           formula.rexIntToBoolIfNecessary(loc, ((CHandler) main.cHandler).getExpressionTranslation(), ((CHandler) main.cHandler).mMemoryHandler);

           decl.addAll(formula.decl);
           stmt.addAll(formula.stmt);
           overappr.addAll(formula.overappr);
           auxVars.putAll(formula.auxVars);

            AssertStatement assertStmt = new AssertStatement(loc, ((ExpressionResult) formula).lrVal.getValue());
            // TODO: Handle havoc statements
            Map<String, IAnnotations> annots = assertStmt.getPayload().getAnnotations();
            for (Overapprox overapprItem : overappr) {
                annots.put(Overapprox.getIdentifier(), overapprItem);
            }
            stmt.add(assertStmt);
            List<HavocStatement> havocs = CHandler.createHavocsForAuxVars(((ExpressionResult) formula).auxVars);
            stmt.addAll(havocs);

            check.addToNodeAnnot(assertStmt);
            return new ExpressionResult(stmt, null, decl, auxVars, overappr);
        }
        // TODO : other cases
        String msg = "ACSLHandler: Not yet implemented: " + node.toString();
        ILocation loc = LocationFactory.createACSLLocation(node);
        throw new UnsupportedSyntaxException(loc, msg);
    }

    /**
     * Translates an ACSL binary expression operator into a boogie binary
     * expression operator, iff there is a one to one translation - otherwise
     * null.
     * 
     * @param op
     *            the ACSL binary expression operator
     * @return the translates operator or null.
     */
    private static Operator getBoogieBinaryExprOperator(
            de.uni_freiburg.informatik.ultimate.model.acsl.ast.BinaryExpression.Operator op) {
        switch (op) {
            case ARITHDIV:
                return Operator.ARITHDIV;
            case ARITHMINUS:
                return Operator.ARITHMINUS;
            case ARITHMOD:
                return Operator.ARITHMOD;
            case ARITHMUL:
                return Operator.ARITHMUL;
            case ARITHPLUS:
                return Operator.ARITHPLUS;
            case BITVECCONCAT:
                return Operator.BITVECCONCAT;
            case COMPEQ:
                return Operator.COMPEQ;
            case COMPGEQ:
                return Operator.COMPGEQ;
            case COMPGT:
                return Operator.COMPGT;
            case COMPLEQ:
                return Operator.COMPLEQ;
            case COMPLT:
                return Operator.COMPLT;
            case COMPNEQ:
                return Operator.COMPNEQ;
            case COMPPO:
                return Operator.COMPPO;
            case LOGICAND:
                return Operator.LOGICAND;
            case LOGICIFF:
                return Operator.LOGICIFF;
            case LOGICIMPLIES:
                return Operator.LOGICIMPLIES;
            case LOGICOR:
                return Operator.LOGICOR;
            case BITXOR:
            case BITAND:
            case BITIFF:
            case BITIMPLIES:
            case BITOR:
            case LOGICXOR:
            default:
                return null;
        }
    }
    
    /**
     * Translates operator of ACSL binary expression to operator of binary
     * expression in the C AST.
     */
    private int getCASTBinaryExprOperator(
            de.uni_freiburg.informatik.ultimate.model.acsl.ast.BinaryExpression.Operator op) {
        switch (op) {
		case ARITHDIV:
			return IASTBinaryExpression.op_divide;
		case ARITHMINUS:
			return IASTBinaryExpression.op_minus;
		case ARITHMOD:
			return IASTBinaryExpression.op_modulo;
		case ARITHMUL:
			return IASTBinaryExpression.op_multiply;
		case ARITHPLUS:
			return IASTBinaryExpression.op_plus;
		case BITAND:
			break;
		case BITIFF:
			break;
		case BITIMPLIES:
			break;
		case BITOR:
			break;
		case BITVECCONCAT:
			break;
		case BITXOR:
			break;
		case COMPEQ:
			return IASTBinaryExpression.op_equals;
		case COMPGEQ:
			return IASTBinaryExpression.op_greaterEqual;
		case COMPGT:
			return IASTBinaryExpression.op_greaterThan;
		case COMPLEQ:
			return IASTBinaryExpression.op_lessEqual;
		case COMPLT:
			return IASTBinaryExpression.op_lessThan;
		case COMPNEQ:
			return IASTBinaryExpression.op_notequals;
		case COMPPO:
			break;
		case LOGICAND:
			return IASTBinaryExpression.op_logicalAnd;
		case LOGICIFF:
			break;
		case LOGICIMPLIES:
			break;
		case LOGICOR:
			return IASTBinaryExpression.op_logicalOr;
		case LOGICXOR:
			break;
		case LTLRELEASE:
			break;
		case LTLUNTIL:
			break;
		case LTLWEAKUNTIL:
			break;
		default:
			break;
        }
        throw new IllegalArgumentException("don't know equivalent C operator");
    }

    @Override
    public Result visit(
            Dispatcher main,
            de.uni_freiburg.informatik.ultimate.model.acsl.ast.BinaryExpression node) {
    	ILocation loc = LocationFactory.createACSLLocation(node);
        ExpressionResult left = (ExpressionResult) main.dispatch(node.getLeft());
        ExpressionResult right = (ExpressionResult) main.dispatch(node.getRight());
        
        MemoryHandler memoryHandler = ((CHandler) main.cHandler).mMemoryHandler;
        StructHandler structHandler = ((CHandler) main.cHandler).mStructHandler;
        
        left = left.switchToRValueIfNecessary(main, memoryHandler, structHandler, loc);
        right = right.switchToRValueIfNecessary(main, memoryHandler, structHandler, loc);
        
        AExpressionTranslation expressionTranslation = 
     		   ((CHandler) main.cHandler).getExpressionTranslation();
        
        ArrayList<Declaration> decl = new ArrayList<Declaration>();
        ArrayList<Statement> stmt = new ArrayList<Statement>();
        List<Overapprox> overappr = new ArrayList<Overapprox>();
        Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
        

       
       decl.addAll(left.decl);
       stmt.addAll(left.stmt);
       auxVars.putAll(left.auxVars);
       overappr.addAll(left.overappr);
       
       
       decl.addAll(right.decl);
       stmt.addAll(right.stmt);
       auxVars.putAll(right.auxVars);
       overappr.addAll(right.overappr);

//        if (left.getType() != null && //FIXME: (alex:) commenting this out bc of removal of InferredType -- replace with sth? 
//        		left.getType().equals(new InferredType(InferredType.Type.Boolean))) {
//        	//convert to boolean if neccessary
//            right = ConvExpr.toBoolean(loc, right);
//        }



        switch (node.getOperator()) {
		case ARITHDIV:
		case ARITHMOD:
		case ARITHMUL: {
			left.rexBoolToIntIfNecessary(loc, expressionTranslation);
			right.rexBoolToIntIfNecessary(loc, expressionTranslation);
			int op = getCASTBinaryExprOperator(node.getOperator());
			return ((CHandler) main.cHandler).handleMultiplicativeOperation(main, loc, null, op, left, right);
		}
		case ARITHMINUS:
		case ARITHPLUS: {
			left.rexBoolToIntIfNecessary(loc, expressionTranslation);
			right.rexBoolToIntIfNecessary(loc, expressionTranslation);
			int op = getCASTBinaryExprOperator(node.getOperator());
			return ((CHandler) main.cHandler).handleAdditiveOperation(main, loc, null, op, left, right);
		}
		case COMPEQ:
		case COMPNEQ: {
			left.rexBoolToIntIfNecessary(loc, expressionTranslation);
			right.rexBoolToIntIfNecessary(loc, expressionTranslation);
			int op = getCASTBinaryExprOperator(node.getOperator());
			return ((CHandler) main.cHandler).handleEqualityOperators(main, loc, op, left, right);
		}
		case COMPGEQ:
		case COMPGT:
		case COMPLEQ:
		case COMPLT:
		{
			left.rexBoolToIntIfNecessary(loc, expressionTranslation);
			right.rexBoolToIntIfNecessary(loc, expressionTranslation);
			int op = getCASTBinaryExprOperator(node.getOperator());
			return ((CHandler) main.cHandler).handleRelationalOperators(main, loc, op, left, right);
		}
		case LOGICAND:
		case LOGICIFF:
		case LOGICIMPLIES:
		case LOGICOR:
		{
	        Operator op = getBoogieBinaryExprOperator(node.getOperator());
	        if (op != null) {
	        	left.rexIntToBoolIfNecessary(loc, ((CHandler) main.cHandler).getExpressionTranslation(), memoryHandler);
	        	right.rexIntToBoolIfNecessary(loc, ((CHandler) main.cHandler).getExpressionTranslation(), memoryHandler);
	        	Expression be = ExpressionFactory.newBinaryExpression(loc, op, left.lrVal.getValue(), right.lrVal.getValue());
	        	// TODO: Handle Ctype
	            return new ExpressionResult(stmt, new RValue(be, new CPrimitive(PRIMITIVE.INT), true), decl, auxVars, overappr);
	            //return new Result(ExpressionFactory.newBinaryExpression(loc, op, left, right));
	        }
		}
        
        case LOGICXOR:
        	// translate into (l | r)
        	// where l = left & !right
        	Expression notRight = ExpressionFactory.newUnaryExpression(loc,
        			UnaryExpression.Operator.LOGICNEG, right.lrVal.getValue());
        	Expression l = ExpressionFactory.newBinaryExpression(loc,
        			Operator.LOGICAND, left.lrVal.getValue(), notRight);
        	// and r = !left & right
        	Expression notLeft = ExpressionFactory.newUnaryExpression(loc,
        			UnaryExpression.Operator.LOGICNEG, left.lrVal.getValue());
        	Expression r = ExpressionFactory.newBinaryExpression(loc,
        			Operator.LOGICAND, notLeft, right.lrVal.getValue());
        	return new ExpressionResult(stmt, new RValue(ExpressionFactory.newBinaryExpression(loc, Operator.LOGICOR, l, r), new CPrimitive(PRIMITIVE.INT), true), decl, auxVars, overappr);
        	//return new Result(ExpressionFactory.newBinaryExpression(loc, Operator.LOGICOR, l, r));
        case BITAND:
        case BITIFF:
        case BITIMPLIES:
        case BITOR:
        case BITXOR:
        	
		case BITVECCONCAT:
		case COMPPO:
		
			
		case LTLRELEASE:
		case LTLUNTIL:
		case LTLWEAKUNTIL:
            default:
                String msg = "Unknown or unsupported binary operation: "
                        + node.getOperator();
                throw new UnsupportedSyntaxException(loc, msg);
        }
    }

    @Override
    public Result visit(
            Dispatcher main,
            de.uni_freiburg.informatik.ultimate.model.acsl.ast.UnaryExpression node) {
        ILocation loc = LocationFactory.createACSLLocation(node);
        ExpressionResult res = (ExpressionResult) main.dispatch(node.getExpr());
        
        MemoryHandler memoryHandler = ((CHandler) main.cHandler).mMemoryHandler;
        StructHandler structHandler = ((CHandler) main.cHandler).mStructHandler;
        
        res = res.switchToRValueIfNecessary(main, memoryHandler, structHandler, loc);

        switch (node.getOperator()) {
            case LOGICNEG:
            	return ((CHandler) main.cHandler).handleUnaryArithmeticOperators(main, loc, IASTUnaryExpression.op_not, res);
            case MINUS:
            	return ((CHandler) main.cHandler).handleUnaryArithmeticOperators(main, loc, IASTUnaryExpression.op_minus, res);
            case PLUS:
            	return ((CHandler) main.cHandler).handleUnaryArithmeticOperators(main, loc, IASTUnaryExpression.op_plus, res);
            case POINTER:
            case ADDROF:
            case LOGICCOMPLEMENT:
            default:
                String msg = "Unknown or unsupported unary operation: "
                        + node.getOperator();
                throw new UnsupportedSyntaxException(loc, msg);
        }
    }

    @Override
    public Result visit(Dispatcher main, IntegerLiteral node) {
    	/*
        return new Result(
                new de.uni_freiburg.informatik.ultimate.model.boogie.ast.IntegerLiteral(
                        LocationFactory.createACSLLocation(node), node.getValue()));
        */
    	AExpressionTranslation expressionTranslation = 
    			((CHandler) main.cHandler).getExpressionTranslation();
    	ILocation loc = LocationFactory.createACSLLocation(node);
    	String val = node.getValue();
    	RValue rValue = expressionTranslation.translateIntegerLiteral(loc, val);
     	return new ExpressionResult(rValue);

    }

    @Override
    public Result visit(Dispatcher main, BooleanLiteral node) {
    	/*
        return new Result(
                new de.uni_freiburg.informatik.ultimate.model.boogie.ast.BooleanLiteral(
                        LocationFactory.createACSLLocation(node), node.getValue()));
        */
     	return new ExpressionResult(new RValue(new de.uni_freiburg.informatik.ultimate.model.boogie.ast.BooleanLiteral(
                LocationFactory.createACSLLocation(node), node.getValue()), new CPrimitive(PRIMITIVE.BOOL), true));
    }

    @Override
    public Result visit(Dispatcher main, RealLiteral node) {
    	/*
        return new Result(
                new de.uni_freiburg.informatik.ultimate.model.boogie.ast.RealLiteral(
                        LocationFactory.createACSLLocation(node), node.getValue()));
        */
     	return new ExpressionResult(new RValue(new de.uni_freiburg.informatik.ultimate.model.boogie.ast.RealLiteral(
                LocationFactory.createACSLLocation(node), node.getValue()), new CPrimitive(PRIMITIVE.DOUBLE)));
    }

    @Override
    public Result visit(
            Dispatcher main,
            de.uni_freiburg.informatik.ultimate.model.acsl.ast.IdentifierExpression node) {
        String id = SFO.EMPTY;
        ILocation loc = LocationFactory.createACSLLocation(node);
        switch (specType) {
            case ASSIGNS:
                // modifies case in boogie, should be always global!
                // maybe it is allowed to assign also in parameters?
                // Global variable
                id = node.getIdentifier();
                SymbolTableValue stv = main.cHandler.getSymbolTable().get(id,
                        loc);
                if (stv.isBoogieGlobalVar()) {
                    id = stv.getBoogieName();
                } else {
                    String msg = "It is not allowed to assign to in parameters! Should be global variables! ["
                            + node.getIdentifier() + "]";
                    throw new IncorrectSyntaxException(loc, msg);
                }
                break;
            case ENSURES:
                if (node.getIdentifier().equalsIgnoreCase("\result")) {
                    id = SFO.RES;
                } else {
                	id = node.getIdentifier();
                    stv = main.cHandler.getSymbolTable().get(id, loc);
                    id = stv.getBoogieName();
                }
                break;
            case REQUIRES:
            	id = node.getIdentifier();
                stv = main.cHandler.getSymbolTable().get(id, loc);
                id = stv.getBoogieName();
                break;
            case NOT:
                // We to handle the scope, so that we address here the right
                // variable
                String cId = node.getIdentifier();
                id = main.cHandler.getSymbolTable().get(cId, loc)
                        .getBoogieName();
                break;
            default:
                String msg = "The type of specType should be in some type!";
                throw new IncorrectSyntaxException(loc, msg);
        }
        
        
        IType type = new InferredType(InferredType.Type.Unknown);
        String cId = main.cHandler.getSymbolTable()
                    .getCID4BoogieID(id, loc);
        final CType cType;
        if (specType != ACSLHandler.SPEC_TYPE.REQUIRES
                && specType != ACSLHandler.SPEC_TYPE.ENSURES) {
            // TODO : the translation is sometimes wrong, for requires and
            // ensures! i.e. when referring to inparams in ensures clauses!
            // The ensures clause will refer to the in-parameter listed in the
            // in parameter declaration. However, these variables will not be
            // changed, but only assigned to #in~NAME!
            // This cannot be solved by just appending "#in~" to all
            // identifiers, since the identifier could also refer to a global
            // variable! However, we don't know that at this moment!

            if (main.cHandler.getSymbolTable().containsKey(cId)) {
                ASTType astt = main.cHandler.getSymbolTable()
                        .getTypeOfVariable(cId, loc);
                cType = main.cHandler.getSymbolTable().get(cId, loc).getCVariable();
                type = new InferredType(astt);
            } else {
            	throw new UnsupportedOperationException("not yet implemented: "
            			+ "unable to determine CType for variable " + id);
            }
        } else {
        	throw new UnsupportedOperationException("not yet implemented: "
        			+ "unable to determine CType for variable " + id);
        }
        
        //FIXME: dereferencing does not work for ACSL yet, because we cannot pass 
        // the necessary auxiliary statements on.
        LRValue lrVal;
        if (((CHandler) main.cHandler).isHeapVar(id)) {
            IdentifierExpression idExp = new IdentifierExpression(loc, id);
        	lrVal = new HeapLValue(idExp, cType);
        } else {
            VariableLHS idLhs = new VariableLHS(loc, id);
        	lrVal = new LocalLValue(idLhs, cType);
        }
        
        //for now, to make the error output clearer:
        //if (lrVal instanceof HeapLValue)
        //	throw new UnsupportedOperationException("variables on heap are not supported in ACSL code right now.");
        
        return new ExpressionResult(lrVal);
        //return new Result(lrVal.getValue());
    }

    @Override
    public Result visit(Dispatcher main, Contract node) {
        ILocation loc = LocationFactory.createACSLLocation(node);
        ArrayList<Specification> spec = new ArrayList<Specification>();
        // First we catch the case that a contract is at a FunctionDefinition
        if (node instanceof IASTFunctionDefinition) {
            String msg = "Syntax Error, Contracts on FunctionDefinition are not allowed";
            throw new IncorrectSyntaxException(loc, msg);
        }

        for (ContractStatement stmt : node.getContractStmt()) {
            spec.addAll(Arrays.asList(((ContractResult) main.dispatch(stmt)).specs));
        }
        if (node.getBehaviors() != null && node.getBehaviors().length != 0) {
            String msg = "Not yet implemented: Behaviour";
            throw new UnsupportedSyntaxException(loc, msg);
        }
        // TODO : node.getCompleteness();
        specType = ACSLHandler.SPEC_TYPE.NOT;
        return new ContractResult(spec.toArray(new Specification[0]));
    }

    @Override
    public Result visit(Dispatcher main, Requires node) {
        specType = ACSLHandler.SPEC_TYPE.REQUIRES;
        Expression formula = ((ExpressionResult) main.dispatch(node.getFormula())).lrVal.getValue();
        Check check = new Check(Check.Spec.PRE_CONDITION);
        ILocation reqLoc = LocationFactory.createACSLLocation(node, check);
        RequiresSpecification req = new RequiresSpecification(reqLoc, false,
                formula);
        check.addToNodeAnnot(req);
        return new ContractResult(new Specification[] { req });
    }

    @Override
    public Result visit(Dispatcher main, Ensures node) {
        ILocation loc = LocationFactory.createACSLLocation(node);
        de.uni_freiburg.informatik.ultimate.model.acsl.ast.Expression e = node
                .getFormula();
        if (e instanceof FieldAccessExpression
                || e instanceof ArrayAccessExpression) {
            // variable declaration not yet translated, hence we cannot
            // translate this access expression!
            String msg = "Ensures specification on struct types is not supported!";
            throw new UnsupportedSyntaxException(loc, msg);
        }
        specType = ACSLHandler.SPEC_TYPE.ENSURES;
        Expression formula = ((ExpressionResult) main.dispatch(e)).lrVal.getValue();
        Check check = new Check(Check.Spec.POST_CONDITION);
        ILocation ensLoc = LocationFactory.createACSLLocation(node, check);
        EnsuresSpecification ens = new EnsuresSpecification(ensLoc, false,
                formula);
        check.addToNodeAnnot(ens);
        return new ContractResult(new Specification[] { ens });
    }

    @Override
    public Result visit(Dispatcher main, Assigns node) {
        specType = ACSLHandler.SPEC_TYPE.ASSIGNS;
        ILocation loc = LocationFactory.createACSLLocation(node);
        ArrayList<String> identifiers = new ArrayList<String>();
        for (de.uni_freiburg.informatik.ultimate.model.acsl.ast.Expression e : node
                .getLocations()) {
            if (e instanceof de.uni_freiburg.informatik.ultimate.model.acsl.ast.IdentifierExpression) {
                identifiers.add(((IdentifierExpression) main.dispatch(e).node)
                        .getIdentifier());
            } else {
            	String msg = "Unexpected Expression: " + e.getClass();
                throw new UnsupportedSyntaxException(loc, msg);
            }
        }
        VariableLHS[] identifiersVarLHS = new VariableLHS[identifiers.size()];
        for (int i = 0; i < identifiers.size(); i++)
        	identifiersVarLHS[i] = new VariableLHS(loc, identifiers.get(i));
        	
        ModifiesSpecification req = new ModifiesSpecification(loc, false,
                identifiersVarLHS);
        return new ContractResult(new Specification[] { req });
    }

    @Override
    public Result visit(Dispatcher main, ACSLResultExpression node) {
    	return new ExpressionResult(new RValue(new IdentifierExpression(LocationFactory.createACSLLocation(node), "#res"), new CPrimitive(PRIMITIVE.INT)));     
        //return new Result(new IdentifierExpression(LocationFactory.createACSLLocation(node), "#res"));
    }

    @Override
    public Result visit(Dispatcher main, LoopAnnot node) {
        if (node.getLoopBehavior() != null
                && node.getLoopBehavior().length != 0) {
        	String msg = "Not yet implemented: Behaviour";
        	ILocation loc = LocationFactory.createACSLLocation(node);
            throw new UnsupportedSyntaxException(loc, msg);
        }
        ArrayList<Specification> specs = new ArrayList<Specification>();
        for (LoopStatement lst : node.getLoopStmt()) {
            Result res = main.dispatch(lst);
            assert res != null && res.node != null;
            assert res.node instanceof Specification;
            specs.add((Specification) res.node);
        }
        return new ContractResult(specs.toArray(new Specification[0]));
    }

    @Override
    public Result visit(Dispatcher main, LoopInvariant node) {
        ExpressionResult res = (ExpressionResult) main.dispatch(node.getFormula());

        ArrayList<Declaration> decl = new ArrayList<Declaration>();
        ArrayList<Statement> stmt = new ArrayList<Statement>();
        List<Overapprox> overappr = new ArrayList<Overapprox>();
        Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
       
        assert res != null && res.lrVal.getValue() != null;
        assert res.lrVal.getValue() instanceof Expression;
        Check check = new Check(Check.Spec.INVARIANT);
        ILocation invLoc = LocationFactory.createACSLLocation(node, check);
        LoopInvariantSpecification lis = new LoopInvariantSpecification(invLoc,
                false, (Expression) res.node);
        check.addToNodeAnnot(lis);

        decl.addAll(res.decl);
        stmt.addAll(res.stmt);
        overappr.addAll(res.overappr);
        auxVars.putAll(res.auxVars);
        
//        return new ResultExpression(stmt, new RValue(lis, new CPrimitive(PRIMITIVE.BOOL)), decl, auxVars, overappr);
        return new Result(lis);
    }

    @Override
    public Result visit(Dispatcher main, LoopVariant node) {
    	String msg = "Not yet implemented: LoopVariant";
    	ILocation loc = LocationFactory.createACSLLocation(node);
        throw new UnsupportedSyntaxException(loc, msg);
    }

    @Override
    public Result visit(Dispatcher main, LoopAssigns node) {
    	String msg = "Not yet implemented: LoopAssigns";
    	ILocation loc = LocationFactory.createACSLLocation(node);
        throw new UnsupportedSyntaxException(loc, msg);
    }

    @Override
    public Result visit(Dispatcher main, ArrayAccessExpression node) {
        ILocation loc = LocationFactory.createACSLLocation(node);
        Stack<Expression> args = new Stack<Expression>();

        ArrayList<Declaration> decl = new ArrayList<Declaration>();
        ArrayList<Statement> stmt = new ArrayList<Statement>();
        List<Overapprox> overappr = new ArrayList<Overapprox>();
        Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
       
       
        de.uni_freiburg.informatik.ultimate.model.acsl.ast.Expression arr = node;
        do {
            assert arr instanceof ArrayAccessExpression;
            assert ((ArrayAccessExpression) arr).getIndices().length == 1;
            ExpressionResult arg = (ExpressionResult) main.dispatch(((ArrayAccessExpression) arr)
                    .getIndices()[0]);
            assert arg.getClass() == ExpressionResult.class;
            assert arg.lrVal.getValue() instanceof Expression;
            args.push((Expression) arg.lrVal.getValue());
            arr = ((ArrayAccessExpression) arr).getArray();

            decl.addAll(arg.decl);
            stmt.addAll(arg.stmt);
            overappr.addAll(arg.overappr);
            auxVars.putAll(arg.auxVars);
            

        } while (arr instanceof ArrayAccessExpression);

        Expression[] idx = new Expression[args.size()];
        Collections.reverse(args);
        args.toArray(idx);
        ExpressionResult idExprRes = (ExpressionResult) main.dispatch(arr);
        
        assert idExprRes.getClass() == ExpressionResult.class;
        assert idExprRes.lrVal.getValue() instanceof Expression;
        Expression subExpr = (Expression) idExprRes.lrVal.getValue();

        decl.addAll(idExprRes.decl);
        stmt.addAll(idExprRes.stmt);
        overappr.addAll(idExprRes.overappr);
        auxVars.putAll(idExprRes.auxVars);
        
        //TODO: compute the CType of returned ResultExpression
        // basic idea: same as arrayType (below) except the last args.size() entries of arrayType.getDimensions() have to be removed for the new type
//        CArray arrayType = (CArray) idExprRes.lrVal.cType;
//        CArray arrayType = new CArray(dimensions, idExprRes.lrVal.cType); --> wrong, i think (alex)
//        arrayType.getDimensions().length == args.size()
            
        de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression expr;
        if (subExpr instanceof IdentifierExpression) {
            IdentifierExpression idEx = (IdentifierExpression) subExpr;
            String bId = idEx.getIdentifier();
            String cId = main.cHandler.getSymbolTable().getCID4BoogieID(bId,
                    loc);
            assert main.cHandler.getSymbolTable().containsKey(cId);
            InferredType it = new InferredType(main.cHandler.getSymbolTable()
                    .getTypeOfVariable(cId, loc));
            expr = new de.uni_freiburg.informatik.ultimate.model.boogie.ast.ArrayAccessExpression(
                    loc, it, idEx, idx);
        } else if (subExpr instanceof StructAccessExpression) {
            StructAccessExpression sae = (StructAccessExpression) subExpr;
            StructLHS lhs = (StructLHS) BoogieASTUtil.getLHSforExpression(sae);
            ASTType t = main.typeHandler.getTypeOfStructLHS(
                    main.cHandler.getSymbolTable(), loc, lhs);
            if (!(t instanceof ArrayType)) {
                String msg = "Type mismatch - cannot take index on a not-array element!";
                throw new IncorrectSyntaxException(loc, msg);
            }
            expr = new de.uni_freiburg.informatik.ultimate.model.boogie.ast.ArrayAccessExpression(
                    loc, sae, idx);
        } else {
            String msg = "Unexpected result type on left side of array!";
            throw new UnsupportedSyntaxException(loc, msg);
        }
        // TODO: Ctype
        return new ExpressionResult(stmt, new RValue(expr, new CPrimitive(PRIMITIVE.INT)), decl, auxVars, overappr);
        //return new Result(expr);
    }

    @Override
    public Result visit(Dispatcher main, FieldAccessExpression node) {

        ArrayList<Declaration> decl = new ArrayList<Declaration>();
        ArrayList<Statement> stmt = new ArrayList<Statement>();
        List<Overapprox> overappr = new ArrayList<Overapprox>();
        Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
       
        ExpressionResult r = (ExpressionResult) main.dispatch(node.getStruct());
        assert r.getClass() == ExpressionResult.class;
        assert r.lrVal.getValue() instanceof Expression;
        String field = node.getField();

        decl.addAll(r.decl);
        stmt.addAll(r.stmt);
        overappr.addAll(r.overappr);
        auxVars.putAll(r.auxVars);
        
        // TODO: CType
        return new ExpressionResult(stmt, new RValue(new StructAccessExpression(LocationFactory.createACSLLocation(node),
                (Expression) r.lrVal.getValue(), field), ((CStruct) r.lrVal.getCType().getUnderlyingType()).getFieldType(field)), decl, auxVars, overappr);
    }

    @Override
    public Result visit(Dispatcher main, FreeableExpression node) {
        ILocation loc = LocationFactory.createACSLLocation(node);
        IType it = new InferredType(InferredType.Type.Boolean);

        ArrayList<Declaration> decl = new ArrayList<Declaration>();
        ArrayList<Statement> stmt = new ArrayList<Statement>();
        List<Overapprox> overappr = new ArrayList<Overapprox>();
        Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
       
        ExpressionResult rIdc = (ExpressionResult) main.dispatch(node.getFormula());
        Expression idx = (Expression) rIdc.node;

        decl.addAll(rIdc.decl);
        stmt.addAll(rIdc.stmt);
        overappr.addAll(rIdc.overappr);
        auxVars.putAll(rIdc.auxVars);
        
        idx = new StructAccessExpression(loc, idx, SFO.POINTER_BASE);
        Expression[] idc = new Expression[] { idx };
        Expression arr = new IdentifierExpression(loc, SFO.VALID);
        Expression e = new de.uni_freiburg.informatik.ultimate.model.boogie.ast.ArrayAccessExpression(
                loc, it, arr, idc);
        // TODO: CType
        return new ExpressionResult(stmt, new RValue(e, new CPrimitive(PRIMITIVE.BOOL)), decl, auxVars, overappr);
        //return new Result(e);
    }

    @Override
    public Result visit(Dispatcher main, MallocableExpression node) {
        ILocation loc = LocationFactory.createACSLLocation(node);
        IType it = new InferredType(InferredType.Type.Boolean);

        ArrayList<Declaration> decl = new ArrayList<Declaration>();
        ArrayList<Statement> stmt = new ArrayList<Statement>();
        List<Overapprox> overappr = new ArrayList<Overapprox>();
        Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
       
        ExpressionResult rIdc = (ExpressionResult) main.dispatch(node.getFormula());
        Expression idx = (Expression) rIdc.lrVal.getValue();
        
        decl.addAll(rIdc.decl);
        stmt.addAll(rIdc.stmt);
        overappr.addAll(rIdc.overappr);
        auxVars.putAll(rIdc.auxVars);
        
        idx = new StructAccessExpression(loc, idx, SFO.POINTER_BASE);
        Expression[] idc = new Expression[] { idx };
        Expression arr = new IdentifierExpression(loc, SFO.VALID);
        Expression valid = new de.uni_freiburg.informatik.ultimate.model.boogie.ast.ArrayAccessExpression(
                loc, it, arr, idc);
        Expression e = ExpressionFactory.newUnaryExpression(
                loc,
//                it,
                de.uni_freiburg.informatik.ultimate.model.boogie.ast.UnaryExpression.Operator.LOGICNEG,
                valid);
        
        // TODO: CType
        return new ExpressionResult(stmt, new RValue(e, new CPrimitive(PRIMITIVE.INT)), decl, auxVars, overappr);
        //return new Result(e);
    }

    @Override
    public Result visit(Dispatcher main, ValidExpression node) {
        ILocation loc = LocationFactory.createACSLLocation(node);
        IType it = new InferredType(InferredType.Type.Boolean);

        ArrayList<Declaration> decl = new ArrayList<Declaration>();
        ArrayList<Statement> stmt = new ArrayList<Statement>();
        List<Overapprox> overappr = new ArrayList<Overapprox>();
        Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();

        ExpressionResult rIdc = (ExpressionResult) main.dispatch(node.getFormula());
        Expression idx = (Expression) rIdc.node;

        decl.addAll(rIdc.decl);
        stmt.addAll(rIdc.stmt);
        overappr.addAll(rIdc.overappr);
        auxVars.putAll(rIdc.auxVars);

        idx = new StructAccessExpression(loc, idx, SFO.POINTER_BASE);
        Expression[] idc = new Expression[] { idx };
        Expression arr = new IdentifierExpression(loc, SFO.VALID);
        Expression e = new de.uni_freiburg.informatik.ultimate.model.boogie.ast.ArrayAccessExpression(
                loc, it, arr, idc);
        
        // TODO: CType
        return new ExpressionResult(stmt, new RValue(e, new CPrimitive(PRIMITIVE.INT)), decl, auxVars, overappr);
        //return new Result(e);
    }
}
