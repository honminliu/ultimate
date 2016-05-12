/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE IRSDependencies plug-in.
 * 
 * The ULTIMATE IRSDependencies plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE IRSDependencies plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE IRSDependencies plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE IRSDependencies plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE IRSDependencies plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.irsdependencies.boogie;

import de.uni_freiburg.informatik.ultimate.core.services.model.ILogger;

import de.uni_freiburg.informatik.ultimate.boogie.BoogieTransformer;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Body;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Unit;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VariableDeclaration;
import de.uni_freiburg.informatik.ultimate.models.IElement;
import de.uni_freiburg.informatik.ultimate.models.structure.WrapperNode;

public class SymbolTableVisitor extends BoogieTransformer
{

	protected final ILogger mLogger;
	protected SymbolTable mSymbolTable;

	protected String mCurrentScopeIdentifier;

	public SymbolTableVisitor(ILogger logger)
	{
		super();
		mLogger = logger;
		mSymbolTable = new SymbolTable();
		mCurrentScopeIdentifier = "";
	}

	public SymbolTable getSymbolTable()
	{
		return mSymbolTable;
	}

	public boolean process(IElement root)
	{
		if (root instanceof WrapperNode) {
			Unit unit = (Unit) ((WrapperNode) root).getBacking();
			Declaration[] declarations = unit.getDeclarations();

			for (Declaration decl : declarations) {
				processDeclaration(decl);
			}
			return false;
		}
		else {
			return true;
		}
	}

	@Override
	protected Declaration processDeclaration(Declaration decl)
	{
		if (decl instanceof Procedure) {
			Procedure proc = (Procedure) decl;
			mCurrentScopeIdentifier = proc.getIdentifier();
			addVariables(proc.getInParams());
			addVariables(proc.getOutParams());
		}
		else if (decl instanceof VariableDeclaration) {
			mCurrentScopeIdentifier = "global";
			VariableDeclaration vdecl = (VariableDeclaration) decl;
			
			for (VarList vl : vdecl.getVariables()) {
				for (String varName : vl.getIdentifiers()) {
					mSymbolTable.addGlobalVariable(varName, vl.getType()
							.getBoogieType());
				}
			}
		}
		return super.processDeclaration(decl);
	}

	@Override
	protected Body processBody(Body body)
	{
		addVariables(body.getLocalVars());
		return super.processBody(body);
	}

	private void addVariables(VariableDeclaration[] vdecls)
	{
		for (VariableDeclaration vdecl : vdecls) {
			addVariables(vdecl.getVariables());
		}
	}

	private void addVariables(VarList[] vlists)
	{
		for (VarList vl : vlists) {
			for (String varName : vl.getIdentifiers()) {
				mSymbolTable.addLocalVariable(varName, mCurrentScopeIdentifier,
						vl.getType().getBoogieType());
			}
		}
	}

}
