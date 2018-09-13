/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import de.uni_freiburg.informatik.ultimate.cdt.decorator.DecoratedUnit;
import de.uni_freiburg.informatik.ultimate.cdt.decorator.DecoratorNode;
import de.uni_freiburg.informatik.ultimate.cdt.parser.MultiparseSymbolTable;
import de.uni_freiburg.informatik.ultimate.cdt.translation.LineDirectiveMapping;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.FlatSymbolTable;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.LocationFactory;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.chandler.TypeSizes;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.Result;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.IACSLHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.ICHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.INameHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.IPreprocessorHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.ITypeHandler;
import de.uni_freiburg.informatik.ultimate.core.lib.results.SyntaxErrorResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.UnsupportedSyntaxResult;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.model.acsl.ACSLNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.CACSL2BoogieBacktranslator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.TranslationMode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer.PointerCheckMode;

/**
 * @author Markus Lindenmann
 * @author Oleksii Saukh
 * @author Stefan Wissert
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * @author Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 */
public abstract class Dispatcher {

	protected LinkedHashMap<String, Integer> mFunctionToIndex;

	protected final ILogger mLogger;

	private final IPreferenceProvider mPreferences;

	/**
	 * The C+ACSL handler.
	 */
	public ICHandler mCHandler;
	/**
	 * The Type handler.
	 */
	public ITypeHandler mTypeHandler;
	/**
	 * The ACSL handler.
	 */
	public IACSLHandler mAcslHandler;
	/**
	 * The Name handler.
	 */
	public INameHandler mNameHandler;
	/**
	 * Holds the next ACSL node in the decorator tree.
	 */
	public NextACSL mNextAcsl;
	/**
	 * The Preprocessor statement handler.
	 */
	public IPreprocessorHandler mPreprocessorHandler;
	/**
	 * This plugin creates results for warnings if set to true.
	 */
	protected boolean mReportWarnings = true;
	/**
	 * Translation from Boogie to C for traces and expressions.
	 */
	protected final CACSL2BoogieBacktranslator mBacktranslator;
	protected final IUltimateServiceProvider mServices;

	private final TypeSizes mTypeSizes;

	private final TranslationSettings mTranslationSettings;

	private LocationFactory mLocationFactory;

	protected final FlatSymbolTable mFlatTable;
	protected final MultiparseSymbolTable mMultiparseTable;

	private final boolean mUseSvcompSettings;

	protected final Map<String, IASTNode> mFunctionTable;

	protected IASTNode mAcslHook;

	protected final CTranslationState mHandlerHandler;

	public Dispatcher(final CACSL2BoogieBacktranslator backtranslator, final IUltimateServiceProvider services,
			final ILogger logger, final LocationFactory locFac, final Map<String, IASTNode> functionTable,
			final MultiparseSymbolTable mst) {
		mLogger = logger;
		mServices = services;
		mHandlerHandler = new CTranslationState(services, logger);
		mBacktranslator = backtranslator;
		mPreferences = mServices.getPreferenceProvider(Activator.PLUGIN_ID);
		mTypeSizes = new TypeSizes(mPreferences, mHandlerHandler);
		mTranslationSettings = new TranslationSettings(mPreferences);
		mLocationFactory = locFac;
		mMultiparseTable = mst;
		mNameHandler = new NameHandler(mBacktranslator, mHandlerHandler);
		mFlatTable = new FlatSymbolTable(mst, this, mNameHandler);
		mFunctionTable = functionTable;

		mUseSvcompSettings = getSvcompMode();
	}

	private boolean getSvcompMode() {
		final IPreferenceProvider prefs = mServices.getPreferenceProvider(Activator.PLUGIN_ID);
		TranslationMode mode = TranslationMode.BASE;
		try {
			mode = prefs.getEnum(CACSLPreferenceInitializer.LABEL_MODE, TranslationMode.class);
		} catch (final Exception e) {
			throw new IllegalArgumentException("Unable to determine preferred mode.");
		}
		switch (mode) {
		case BASE:
			return false;
		case SV_COMP14:
			return true;
		default:
			throw new IllegalArgumentException("Unknown mode.");
		}
	}

	public boolean isSvcomp() {
		return mUseSvcompSettings;
	}

	/**
	 * Initializes the handler fields.
	 */
	protected abstract void init();

	/**
	 * Dispatch a given node to a specific handler.
	 *
	 * @param node
	 *            the node to dispatch
	 * @return the result for the given node
	 */
	public abstract Result dispatch(final List<DecoratedUnit> nodes);

	/**
	 * Dispatch a given C node to a specific handler.
	 *
	 * @param node
	 *            the node to dispatch
	 * @return the result for the given node
	 */
	public abstract Result dispatch(IASTNode node);

	/**
	 * Dispatch a given C node to a specific handler.
	 *
	 * @param node
	 *            the node to dispatch.
	 * @return the resulting translation.
	 */
	public abstract Result dispatch(IASTPreprocessorStatement node);

	// /**
	// * Dispatch a given IType to a specific handler.
	// *
	// * @param type
	// * the type to dispatch
	// * @return the result for the given type.
	// */
	// public abstract InferredType dispatch(IType type);

	/**
	 * Dispatch a given ACSL node to the specific handler.
	 *
	 * @param node
	 *            the node to dispatch
	 * @param cHook
	 *            the C AST node where this ACSL node has scope access
	 * @return the result for the given node
	 */
	public abstract Result dispatch(ACSLNode node, IASTNode cHook);

	/**
	 * Dispatch a given ACSL node to the specific handler. Shortcut for methods where the hook does not change.
	 *
	 * @param node
	 *            the node to dispatch
	 * @param cHook
	 *            the C AST node where this ACSL node has scope access
	 * @return the result for the given node
	 */
	public abstract Result dispatch(ACSLNode node);

	/**
	 * Entry point for a translation.
	 *
	 * @param nodes
	 *            the root nodes from which the translation should be started
	 * @return the result for the given node
	 */
	public final Result run(final List<DecoratedUnit> nodes) {
		preRun(nodes);
		init();
		return dispatch(nodes);
	}

	/**
	 * The method implementing a pre-run, if required.
	 *
	 * @param node
	 *            the node for which the pre run should be started
	 */
	protected void preRun(final List<DecoratedUnit> nodes) {
		assert !nodes.isEmpty();

		// Line Directive mapping doesn't work with multiple TUs right now
		final DecoratorNode ldmNode = nodes.stream().findFirst().get().getRootNode();
		assert ldmNode.getCNode() != null;
		assert ldmNode.getCNode() instanceof IASTTranslationUnit;

		final IASTTranslationUnit tu = (IASTTranslationUnit) ldmNode.getCNode();
		final LineDirectiveMapping lineDirectiveMapping = new LineDirectiveMapping(tu.getRawSignature());
		mLocationFactory = new LocationFactory(lineDirectiveMapping);
		mBacktranslator.setLocationFactory(mLocationFactory);
	}

	protected <T extends ASTVisitor> void executePreRun(final T preRun, final Collection<DecoratedUnit> units,
			final Consumer<T> callback) {
		for (final DecoratedUnit unit : units) {
			unit.getRootNode().getCNode().accept(preRun);
		}
		callback.accept(preRun);
	}

	protected <T extends ASTVisitor> void executePreRun(final T preRun, final Collection<DecoratedUnit> units) {
		for (final DecoratedUnit unit : units) {
			unit.getRootNode().getCNode().accept(preRun);
		}
	}

	/**
	 * Iterates to the next ACSL statement in the decorator tree and returns a list of ACSL nodes until the next C node
	 * appears.
	 *
	 * @return a list of ACSL nodes until the next C node appears.
	 * @throws ParseException
	 *             if no trailing C node in the tree! The ACSL is in an unexpected and most probably unreachable
	 *             location and should be ignored!
	 */
	public abstract NextACSL nextACSLStatement() throws ParseException;

	/**
	 * Report a syntax error to Ultimate. This will cancel the toolchain.
	 *
	 * @param loc
	 *            where did it happen?
	 * @param type
	 *            why did it happen?
	 * @param msg
	 *            description.
	 */
	public void syntaxError(final ILocation loc, final String msg) {
		final SyntaxErrorResult result = new SyntaxErrorResult(Activator.PLUGIN_NAME, loc, msg);
		mLogger.warn(msg);
		mServices.getResultService().reportResult(Activator.PLUGIN_ID, result);
		mServices.getProgressMonitorService().cancelToolchain();
	}

	/**
	 * Report a unsupported syntax to Ultimate. This will cancel the toolchain.
	 *
	 * @param loc
	 *            where did it happen?
	 * @param type
	 *            why did it happen?
	 * @param msg
	 *            description.
	 */
	public void unsupportedSyntax(final ILocation loc, final String msg) {
		final UnsupportedSyntaxResult<IElement> result = new UnsupportedSyntaxResult<>(Activator.PLUGIN_NAME, loc, msg);
		mLogger.warn(msg);
		mServices.getResultService().reportResult(Activator.PLUGIN_ID, result);
		mServices.getProgressMonitorService().cancelToolchain();
	}

	/**
	 * Getter for the setting: checked method.
	 *
	 * @return the checked method's name.
	 */
	public String getCheckedMethod() {
		final IPreferenceProvider prefs = mServices.getPreferenceProvider(Activator.PLUGIN_ID);
		String checkMethod = SFO.EMPTY;
		try {
			checkMethod = prefs.getString(CACSLPreferenceInitializer.MAINPROC_LABEL);
		} catch (final Exception e) {
			throw new IllegalArgumentException("Unable to determine specified checked method.");
		}
		return checkMethod;
	}

	/**
	 * Getter for the identifier mapping.
	 *
	 * @return the mapping of Boogie identifiers to origin C identifiers.
	 */
	public Map<String, String> getIdentifierMapping() {
		return mCHandler.getSymbolTable().getBoogieCIdentifierMapping();
	}

	public LinkedHashMap<String, Integer> getFunctionToIndex() {
		return mFunctionToIndex;
	}

	public TypeSizes getTypeSizes() {
		return mTypeSizes;
	}

	public TranslationSettings getTranslationSettings() {
		return mTranslationSettings;
	}

	/**
	 * Checks whether a declaration is reachable.
	 *
	 * @param decl
	 *            The declaration
	 * @return Whether it is reachable
	 */
	public abstract boolean isReachable(IASTDeclaration decl);

	public IPreferenceProvider getPreferences() {
		return mPreferences;
	}

	public LocationFactory getLocationFactory() {
		return mLocationFactory;
	}

	public Map<String, IASTNode> getFunctionTable() {
		return Collections.unmodifiableMap(mFunctionTable);
	}

	public static final class TranslationSettings {
		private final PointerCheckMode mDivisionByZeroOfIntegerTypes;
		private final PointerCheckMode mDivisionByZeroOfFloatingTypes;

		public TranslationSettings(final IPreferenceProvider preferences) {
			mDivisionByZeroOfIntegerTypes = preferences.getEnum(
					CACSLPreferenceInitializer.LABEL_CHECK_DIVISION_BY_ZERO_OF_INTEGER_TYPES, PointerCheckMode.class);
			mDivisionByZeroOfFloatingTypes = preferences.getEnum(
					CACSLPreferenceInitializer.LABEL_CHECK_DIVISION_BY_ZERO_OF_FLOATING_TYPES, PointerCheckMode.class);

		}

		public PointerCheckMode getDivisionByZeroOfIntegerTypes() {
			return mDivisionByZeroOfIntegerTypes;
		}

		public PointerCheckMode getDivisionByZeroOfFloatingTypes() {
			return mDivisionByZeroOfFloatingTypes;
		}
	}

	public IASTNode getAcslHook() {
		return mAcslHook;
	}

	public ILogger getLogger() {
		return mLogger;
	}
}