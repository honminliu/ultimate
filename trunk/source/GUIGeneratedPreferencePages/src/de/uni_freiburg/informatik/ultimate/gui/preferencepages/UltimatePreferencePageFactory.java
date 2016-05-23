/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE GUIGeneratedPreferencePages plug-in.
 * 
 * The ULTIMATE GUIGeneratedPreferencePages plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE GUIGeneratedPreferencePages plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE GUIGeneratedPreferencePages plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE GUIGeneratedPreferencePages plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE GUIGeneratedPreferencePages plug-in grant you additional permission 
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.gui.preferencepages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.IOutput;
import de.uni_freiburg.informatik.ultimate.core.model.ISource;
import de.uni_freiburg.informatik.ultimate.core.model.ITool;
import de.uni_freiburg.informatik.ultimate.core.model.IUltimatePlugin;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.BaseUltimatePreferenceItem;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.BaseUltimatePreferenceItem.PreferenceType;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.UltimatePreferenceItemContainer;

/**
 * 
 * Creates the automatically generated preference pages for Ultimate. Call {@link #createPreferencePages()} e.g. after
 * WorbenchWindow creation (in {@link WorkbenchWindowAdvisor}.
 * 
 * @author dietsch
 * 
 */
public class UltimatePreferencePageFactory {

	private ICore<?> mCore;

	public UltimatePreferencePageFactory(ICore<?> core) {
		mCore = core;
	}

	public void createPreferencePages() {
		IUltimatePlugin[] plugins = mCore.getRegisteredUltimatePlugins();
		for (IUltimatePlugin plugin : plugins) {
			if (plugin.getPreferences() == null) {
				continue;
			}
			final BaseUltimatePreferenceItem[] preferenceItems = plugin.getPreferences().getPreferenceItems();
			if (preferenceItems == null) {
				continue;
			}
			final String parentNodeId = getParentNodeId(plugin);
			createPreferencePage(plugin.getPluginID(), plugin.getPreferences().getPreferenceTitle(),
					filterPreferences(preferenceItems), parentNodeId);
		}
	}

	private String getParentNodeId(IUltimatePlugin plugin) {
		if (plugin instanceof ITool) {
			return "ToolPlugins";
		}
		if (plugin instanceof IOutput) {
			return "OutputPlugins";
		}
		if (plugin instanceof ICore) {
			return "Core";
		}
		if (plugin instanceof ISource) {
			return "SourcePlugins";
		}
		return "GeneratedUltimatePreferences";
	}

	private BaseUltimatePreferenceItem[] filterPreferences(BaseUltimatePreferenceItem[] items) {
		ArrayList<BaseUltimatePreferenceItem> list = new ArrayList<>();
		for (BaseUltimatePreferenceItem item : items) {
			if (!item.getUseCustomPreferencePage()) {
				list.add(item);
			}
		}
		return list.toArray(new BaseUltimatePreferenceItem[list.size()]);
	}

	private void createPreferencePage(String pluginID, String title, BaseUltimatePreferenceItem[] preferenceItems,
			String parentNodeID) {
		BaseUltimatePreferenceItem[] pageItems = Arrays.stream(preferenceItems)
				.filter(p -> p.getType() != PreferenceType.SubItemContainer)
				.toArray(i -> new BaseUltimatePreferenceItem[i]);

		UltimatePreferenceItemContainer[] subContainerItems = Arrays.stream(preferenceItems)
				.filter(p -> p.getType() == PreferenceType.SubItemContainer)
				.map(p -> (UltimatePreferenceItemContainer) p).toArray(i -> new UltimatePreferenceItemContainer[i]);

		UltimateGeneratedPreferencePage page = new UltimateGeneratedPreferencePage(pluginID, title, pageItems);

		final String nodeName = pluginID + "." + parentNodeID + "." + title;

		UltimatePreferenceNode node = new UltimatePreferenceNode(nodeName, page);
		PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();

		IPreferenceNode root = findRootNode(pm, parentNodeID);
		if (root != null) {
			root.remove(pluginID);
			root.add(node);
			for (UltimatePreferenceItemContainer container : subContainerItems) {

				final BaseUltimatePreferenceItem[] containerItems = container.getContainerItems()
						.toArray(new BaseUltimatePreferenceItem[container.getContainerItems().size()]);

				createPreferencePage(pluginID, container.getContainerName(), filterPreferences(containerItems),
						node.getId());
			}
			page.init(PlatformUI.getWorkbench());
		}
	}

	private IPreferenceNode findRootNode(PreferenceManager pm, String nodeID) {
		Queue<IPreferenceNode> toVisit = new LinkedList<IPreferenceNode>();
		for (IPreferenceNode node : pm.getRootSubNodes()) {
			toVisit.add(node);
		}

		while (!toVisit.isEmpty()) {
			IPreferenceNode current = toVisit.poll();
			if (current.getId().equals(nodeID)) {
				return current;
			}
			for (IPreferenceNode node : current.getSubNodes()) {
				toVisit.add(node);
			}
		}
		return null;
	}

}
