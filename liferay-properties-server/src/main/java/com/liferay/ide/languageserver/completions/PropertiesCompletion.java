/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.languageserver.completions;

import com.liferay.ide.languageserver.properties.BladeProperties;
import com.liferay.ide.languageserver.properties.BndBnd;
import com.liferay.ide.languageserver.properties.CoreLanguageProperties;
import com.liferay.ide.languageserver.properties.LiferayPluginPackageProperties;
import com.liferay.ide.languageserver.properties.LiferayWorkspaceGradleProperties;
import com.liferay.ide.languageserver.properties.PortalProperties;
import com.liferay.ide.languageserver.properties.PropertiesFile;
import com.liferay.ide.languageserver.properties.PropertyPair;
import com.liferay.ide.languageserver.services.Service;
import com.liferay.ide.languageserver.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.lang.StringEscapeUtils;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;

/**
 * @author Terry Jia
 */
public class PropertiesCompletion {

	public PropertiesCompletion(CompletionParams completionParams) {
		Position position = completionParams.getPosition();

		_line = position.getLine();

		TextDocumentIdentifier textDocument =
			completionParams.getTextDocument();

		try {
			URI uri = new URI(textDocument.getUri());

			_file = new File(uri);

			List<PropertiesFile> propertiesFiles = _getAllPropertiesFiles(
				_file);

			Stream<PropertiesFile> stream = propertiesFiles.stream();

			_propertiesFile = stream.filter(
				PropertiesFile::match
			).findFirst(
			).get();
		}
		catch (Exception e) {
		}
	}

	public List<CompletionItem> getCompletions(String currentContent) {
		List<CompletionItem> completionItems = new ArrayList<>();

		String[] lines = null;

		if ((currentContent != null) && !currentContent.isEmpty()) {
			lines = FileUtil.readLinesFromString(currentContent);
		}
		else {
			lines = FileUtil.readLinesFromFile(_file);
		}

		if (_propertiesFile != null) {
			String line = "";

			if (_line < lines.length) {
				line = lines[_line].trim();
			}

			List<PropertyPair> properties = _propertiesFile.getProperties();

			if (line.contains("=") || line.contains(":")) {
				int index = line.indexOf("=");

				if (index < 0) {
					index = line.indexOf(":");
				}

				String key = line.substring(0, index);

				String key2 = key.trim();

				for (PropertyPair propertyPair : properties) {
					if (key2.equals(propertyPair.getKey())) {
						Service value = propertyPair.getValue();

						if (value != null) {
							for (String possibleValue :
									value.getPossibleValues()) {

								possibleValue = StringEscapeUtils.escapeJava(
									possibleValue);

								CompletionItem completionItem =
									new CompletionItem(possibleValue);

								completionItem.setKind(
									CompletionItemKind.Property);

								completionItems.add(completionItem);
							}
						}
					}
				}
			}
			else {
				Properties props = new Properties();

				try {
					InputStream imputStream = new ByteArrayInputStream(
						currentContent.getBytes());

					props.load(new InputStreamReader(imputStream));
				}
				catch (IOException ioe) {
				}

				for (PropertyPair propertyPair : properties) {
					String key = propertyPair.getKey();

					if (!props.containsKey(key)) {
						String comment = propertyPair.getCommennt();

						CompletionItem completionItem = new CompletionItem(key);

						completionItem.setKind(CompletionItemKind.Property);
						completionItem.setDetail(comment);

						completionItems.add(completionItem);
					}
				}
			}
		}

		return completionItems;
	}

	private List<PropertiesFile> _getAllPropertiesFiles(File file) {
		List<PropertiesFile> propertiesFiles = new ArrayList<>();

		propertiesFiles.add(new LiferayWorkspaceGradleProperties(file));
		propertiesFiles.add(new BladeProperties(file));
		propertiesFiles.add(new PortalProperties(file));
		propertiesFiles.add(new LiferayPluginPackageProperties(file));
		propertiesFiles.add(new CoreLanguageProperties(file));
		propertiesFiles.add(new BndBnd(file));

		return propertiesFiles;
	}

	private File _file;
	private int _line;
	private PropertiesFile _propertiesFile;

}