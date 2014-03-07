package de.uni_freiburg.informatik.ultimate.boogie.preprocessor;

import de.uni_freiburg.informatik.ultimate.boogie.type.BoogieType;
import de.uni_freiburg.informatik.ultimate.model.boogie.DeclarationInformation;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Declaration;

public class VariableInfo {
	private final boolean rigid;
	private final Declaration declaration;
	private final String name;
	private final BoogieType type;
	private final DeclarationInformation declarationInformation;
	
	public boolean isRigid() {
		return rigid;
	}

	public String getName() {
		return name;
	}

	public BoogieType getType() {
		return type;
	}
	
	public Declaration getDeclaration() {
		return declaration;
	}
	
	public DeclarationInformation getDeclarationInformation() {
		return declarationInformation;
	}

	public VariableInfo(boolean rigid, Declaration declaration, String name, 
			BoogieType type, DeclarationInformation declarationInformation) {
		super();
		this.rigid = rigid;
		this.declaration = declaration; 
		this.name = name;
		this.type = type;
		this.declarationInformation = declarationInformation;
	}
	
	public String toString() {
		return name + ":" + type;
	}
}
