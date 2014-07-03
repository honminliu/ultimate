package de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences;

import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceItem;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceItem.PreferenceType;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.TranslationMode;

public class PreferenceInitializer extends UltimatePreferenceInitializer {


	@Override
	protected UltimatePreferenceItem<?>[] initDefaultPreferences() {
		return new UltimatePreferenceItem<?>[] {
				new UltimatePreferenceItem<TranslationMode>(LABEL_MODE,
						TranslationMode.BASE, PreferenceType.Radio,
						TranslationMode.values()),
				new UltimatePreferenceItem<String>(LABEL_MAINPROC, "",
						PreferenceType.String),
				new UltimatePreferenceItem<POINTER_CHECKMODE>(
						LABEL_CHECK_POINTER_VALIDITY, 
							POINTER_CHECKMODE.ASSERTandASSUME,
							PreferenceType.Combo, POINTER_CHECKMODE.values()),
				new UltimatePreferenceItem<POINTER_CHECKMODE>(
							LABEL_CHECK_POINTER_ALLOC, 
							POINTER_CHECKMODE.ASSERTandASSUME,
							PreferenceType.Combo, POINTER_CHECKMODE.values()),
				new UltimatePreferenceItem<Boolean>(
						LABEL_CHECK_FREE_VALID, true,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(
						LABEL_CHECK_MemoryLeakInMain, false,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(
						LABEL_CHECK_MallocNonNegative, false,
						PreferenceType.Boolean), 
				new UltimatePreferenceItem<Boolean>(
					    LABEL_REPORT_UNSOUNDNESS_WARNING, false,
							PreferenceType.Boolean),
				new UltimatePreferenceItem<POINTER_CHECKMODE>(
						LABEL_CHECK_POINTER_SUBTRACTION_AND_COMPARISON_VALIDITY, 
				  	    POINTER_CHECKMODE.ASSERTandASSUME, 
				  	    PreferenceType.Combo, POINTER_CHECKMODE.values()),
				new UltimatePreferenceItem<Boolean>(
						LABEL_USE_EXPLICIT_TYPESIZES, true,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_BOOL, 1,
						PreferenceType.Integer) ,
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_CHAR, 1,
						PreferenceType.Integer) ,
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_SHORT, 2,
						PreferenceType.Integer) ,
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_INT, 4,
						PreferenceType.Integer) ,
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_LONG, 8,
						PreferenceType.Integer) ,
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_FLOAT, 4,
						PreferenceType.Integer) ,
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_DOUBLE, 8,
						PreferenceType.Integer) ,
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_POINTER, 8,
						PreferenceType.Integer)
		};
	}

	@Override
	protected String getPlugID() {
		return Activator.s_PLUGIN_ID;
	}

	@Override
	public String getPreferencePageTitle() {
		return "C+ACSL to Boogie Translator";
	}

	public enum POINTER_CHECKMODE { IGNORE, ASSUME, ASSERTandASSUME }

	public static final String LABEL_MODE = "Translation Mode:";
	public static final String LABEL_MAINPROC = "Checked method. Library mode if empty.";
	public static final String LABEL_CHECK_POINTER_VALIDITY = "Pointer base address is valid at dereference";
	public static final String LABEL_CHECK_POINTER_ALLOC = "Pointer to allocated memory at dereference";
	public static final String LABEL_CHECK_FREE_VALID = "Check if freed pointer was valid";
	public static final String LABEL_CHECK_MemoryLeakInMain = "Check for the main procedure if all allocated memory was freed";
	public static final String LABEL_CHECK_MallocNonNegative = "Check if the input of malloc is non-negative";
	public static final String LABEL_REPORT_UNSOUNDNESS_WARNING = "Report unsoundness warnings";
	public static final String LABEL_CHECK_POINTER_SUBTRACTION_AND_COMPARISON_VALIDITY = 
			"If two pointers are subtracted or compared they have the same base address";
	public static final String LABEL_USE_EXPLICIT_TYPESIZES = "Use the constants given below as storage sizes for the correponding types";
	public static final String LABEL_EXPLICIT_TYPESIZE_BOOL = "Size of bool (in bytes)";
	public static final String LABEL_EXPLICIT_TYPESIZE_CHAR = "Size of char (in bytes)";
	public static final String LABEL_EXPLICIT_TYPESIZE_SHORT = "Size of short (in bytes)";
	public static final String LABEL_EXPLICIT_TYPESIZE_INT = "Size of int (in bytes)";
	public static final String LABEL_EXPLICIT_TYPESIZE_LONG = "Size of long (in bytes)";
	public static final String LABEL_EXPLICIT_TYPESIZE_FLOAT = "Size of float (in bytes)";
	public static final String LABEL_EXPLICIT_TYPESIZE_DOUBLE = "Size of double (in bytes)";
	public static final String LABEL_EXPLICIT_TYPESIZE_POINTER = "Size of pointer (in bytes)";

}
