package de.uni_freiburg.informatik.ultimate.srparse.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.uni_freiburg.informatik.ultimate.core.lib.util.MonitoredProcess;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger.LogLevel;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.pea.PhaseEventAutomata;
import de.uni_freiburg.informatik.ultimate.lib.srparse.SrParseScopeAfter;
import de.uni_freiburg.informatik.ultimate.lib.srparse.SrParseScopeAfterUntil;
import de.uni_freiburg.informatik.ultimate.lib.srparse.SrParseScopeBefore;
import de.uni_freiburg.informatik.ultimate.lib.srparse.SrParseScopeBetween;
import de.uni_freiburg.informatik.ultimate.lib.srparse.SrParseScopeGlobally;
import de.uni_freiburg.informatik.ultimate.lib.srparse.pattern.PatternScopeNotImplemented;
import de.uni_freiburg.informatik.ultimate.lib.srparse.pattern.PatternType;
import de.uni_freiburg.informatik.ultimate.test.mocks.UltimateMocks;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;

@RunWith(Parameterized.class)
public class PeaToDotTestSuite {

	private static final String ROOT_DIR = "/media/ubuntu/Daten/Projects/hanfor/documentation/docs/";
	private static final String MARKDOWN_DIR = "usage/patterns/";
	private static final String IMAGE_DIR = "img/patterns/";

	private final IUltimateServiceProvider mServiceProvider;
	private final ILogger mLogger;
	private final String mName;
	private final PatternType mPattern;
	private final Map<String, Integer> mDuration2Bounds;
	private final String mScope;

	public PeaToDotTestSuite(final PatternType pattern, final Map<String, Integer> duration2Bounds, final String name) {
		mServiceProvider = UltimateMocks.createUltimateServiceProviderMock(LogLevel.INFO);
		mLogger = mServiceProvider.getLoggingService().getLogger("");

		mName = name;
		mPattern = pattern;
		mDuration2Bounds = duration2Bounds;

		final Class<?> scope = pattern.getScope().getClass();
		mScope = scope.getSimpleName().replace(scope.getSuperclass().getSimpleName(), "");
	}

	// @Test
	public void testDot() throws IOException, InterruptedException {
		PhaseEventAutomata pea;
		try {
			pea = mPattern.transformToPea(mLogger, mDuration2Bounds);
		} catch (final PatternScopeNotImplemented e) {
			return; // Oops, somebody forgot to implement that sh.. ;-)
		}

		// writeDotToSvg(DotWriterNew.createDotString(pea));
		// writeMarkdown();
	}

	private void writeDotToSvg(final StringBuilder dot) throws IOException, InterruptedException {
		final File file = new File(ROOT_DIR + IMAGE_DIR + mName + "_" + mScope + ".svg");
		final String[] command = new String[] { "dot", "-Tsvg", "-o", file.toString() };

		final MonitoredProcess process = MonitoredProcess.exec(command, null, null, mServiceProvider);
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		writer.write(dot.toString());
		writer.close();

		process.waitfor();
	}

	private void writeMarkdown() throws IOException {
		final String formula = mPattern.toString().replace(mPattern.getId() + ": ", "");

		final StringBuilder markdown = new StringBuilder();
		markdown.append("### " + mName + " " + mScope + "\n\n");
		markdown.append(formula + "\n\n");
		markdown.append("![](/" + IMAGE_DIR + mName + "_" + mScope + ".svg)\n");

		File file = new File(ROOT_DIR + MARKDOWN_DIR + mName + "_" + mScope + ".md");
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(markdown.toString());
		writer.close();

		file = new File(ROOT_DIR + MARKDOWN_DIR + "includes.md");
		writer = new BufferedWriter(new FileWriter(file, true));

		if (mScope.equals("Globally")) {
			writer.write("## " + mName + "\n");
		}

		writer.write("{!" + MARKDOWN_DIR + mName + "_" + mScope + ".md!}");
		writer.newLine();
		writer.close();

	}

	@BeforeClass
	public static void beforeClass() throws IOException {
		// Check if markdown and image directory exist.
		final File image_dir = new File(ROOT_DIR + IMAGE_DIR);
		final File markdown_dir = new File(ROOT_DIR + MARKDOWN_DIR);

		assert (image_dir.isDirectory()) : "Directory not found: '" + image_dir + "'";
		assert (markdown_dir.isDirectory()) : "Directory not found: '" + markdown_dir + "'";

		// Delete auto-generated markdown and image files.
		Stream.of(markdown_dir.listFiles()).filter(a -> a.getName().endsWith(".md")).forEach(a -> a.delete());
		Stream.of(image_dir.listFiles()).filter(a -> a.getName().endsWith(".svg")).forEach(a -> a.delete());
	}

	@AfterClass
	public static void afterClass() {

	}

	@Parameters(name = "{2}")
	public static Collection<Object[]> data() {
		final Pair<List<PatternType>, Map<String, Integer>> pair = PatternUtil.createAllPatterns();

		return pair.getFirst().stream().sorted(new PatternComparator())
				.map(a -> new Object[] { a, pair.getSecond(), a.getClass().getSimpleName() })
				.collect(Collectors.toList());
	}

	private static final class PatternComparator implements Comparator<PatternType> {

		private static final Map<Class<?>, Integer> scopeOrder = new HashMap<Class<?>, Integer>() {
			{
				put(SrParseScopeGlobally.class, 1);
				put(SrParseScopeBefore.class, 2);
				put(SrParseScopeAfter.class, 3);
				put(SrParseScopeBetween.class, 4);
				put(SrParseScopeAfterUntil.class, 5);
			}
		};

		@Override
		public int compare(final PatternType p1, final PatternType p2) {
			int result = p1.getClass().getSimpleName().compareTo(p2.getClass().getSimpleName());

			if (result == 0) {
				result = scopeOrder.get(p1.getScope().getClass()) - scopeOrder.get(p2.getScope().getClass());
			}

			return result;
		}
	}
}
